/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/LogViewScreen.kt
 * Reads Pojav's latestlog.txt (Tools.shareLog uses the same file) and the crash reports of the
 * selected instance. The 8 MiB tail window of the original is kept; lines are coloured by log
 * level and filtered by a search field. Lines are rendered by the same highlighter the in-game log box
 * uses (net.kdt.pojavlaunch.aerix.log.LogHighlighter, ported from mirai's log_parser package), so both
 * log surfaces share Mirai's colours. Text size is the Aerix "logTextSize" setting.
 * Zalith Launcher 2 Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.log.LogHighlighter
import net.kdt.pojavlaunch.aerix.bridge.PojavNavigator
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.components.PillTabs
import net.kdt.pojavlaunch.aerix.components.QuietEmptyState
import net.kdt.pojavlaunch.aerix.components.SliderSettingRow
import net.kdt.pojavlaunch.aerix.components.SwitchSettingRow
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.instances.Instances
import java.io.File
import java.io.RandomAccessFile

/** Upper bound of the text loaded into the viewer; larger files show their tail. */
private const val MAX_LOG_VIEW_SIZE: Long = 8L * 1024 * 1024

/** Refresh period while auto refresh is on. */
private const val AUTO_REFRESH_MILLIS = 2000L

private class LogSource(val label: String, val file: File)

private fun readLog(file: File): String {
    val size = file.length()
    if (size <= MAX_LOG_VIEW_SIZE) return file.readText()
    RandomAccessFile(file, "r").use { raf ->
        raf.seek(size - MAX_LOG_VIEW_SIZE)
        val bytes = ByteArray(MAX_LOG_VIEW_SIZE.toInt())
        raf.readFully(bytes)
        // The cut may leave a partial character; drop it and start at the first full line.
        val text = String(bytes, Charsets.UTF_8).trimStart('\uFFFD')
        val firstNewline = text.indexOf('\n')
        return if (firstNewline >= 0 && firstNewline < text.length - 1) text.substring(firstNewline + 1) else text
    }
}

/**
 * A readable tab title for an archived launch: `aerix-log-20261115-013045.txt` becomes
 * "Previous launch 15/11 01:30:45". A name this cannot parse is shown as-is rather than dropped,
 * because a title is never worth losing the log behind it.
 */
private fun previousLaunchLabel(file: File): String {
    val stamp = file.name
        .removePrefix(net.kdt.pojavlaunch.aerix.log.PreviousLaunchLog.ARCHIVE_PREFIX)
        .removeSuffix(net.kdt.pojavlaunch.aerix.log.PreviousLaunchLog.ARCHIVE_SUFFIX)
    val parsed = Regex("^(\\d{4})(\\d{2})(\\d{2})-(\\d{2})(\\d{2})(\\d{2})").find(stamp)
        ?: return "Previous launch $stamp"
    val (year, month, day, hour, minute, second) = parsed.destructured
    return "Previous launch $day/$month/${year.takeLast(2)} $hour:$minute:$second"
}

/** latestlog.txt first, then the newest crash reports of the selected instance and the shared folder. */
private fun collectSources(context: Context): List<LogSource> {
    val sources = ArrayList<LogSource>()
    sources.add(LogSource(context.getString(R.string.aerix_logs_latest), File(Tools.DIR_GAME_HOME, "latestlog.txt")))
    // The launches before this one. `Logger.begin` truncates the live file, so without these the
    // retry that every player makes after a startup crash has already destroyed its own evidence.
    net.kdt.pojavlaunch.aerix.log.PreviousLaunchLog.list(File(Tools.DIR_GAME_HOME))
        .forEach { sources.add(LogSource(previousLaunchLabel(it), it)) }
    File(context.filesDir, net.kdt.pojavlaunch.lifecycle.LaunchDiagnostics.FILE_NAME)
        .takeIf { it.isFile }?.let { sources.add(LogSource("Launch status", it)) }
    File(Tools.DIR_GAME_HOME, "latestcrash.txt")
        .takeIf { it.isFile }?.let { sources.add(LogSource("Launcher crash", it)) }
    File(Tools.DIR_DATA, "latestcrash.txt")
        .takeIf { it.isFile }?.let { sources.add(LogSource("Launcher crash (private)", it)) }
    val crashDirs = LinkedHashSet<File>()
    runCatching { Instances.loadSelectedInstance()?.gameDirectory?.let { crashDirs.add(File(it, "crash-reports")) } }
    Tools.DIR_HOME_CRASH?.let { crashDirs.add(File(it)) }
    crashDirs.flatMap { dir -> dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }?.toList().orEmpty() }
        .distinctBy { it.absolutePath }
        .sortedByDescending { it.lastModified() }
        .take(10)
        .forEach { sources.add(LogSource(it.name.removeSuffix(".txt"), it)) }
    return sources
}


@Composable
fun LogScreen(activity: FragmentActivity) {
    val context = LocalContext.current
    var sources by remember { mutableStateOf(emptyList<LogSource>()) }
    var sourceIndex by rememberSaveable { mutableIntStateOf(0) }
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var autoRefresh by rememberSaveable { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val copiedText = stringResource(R.string.aerix_logs_copied)

    LaunchedEffect(revision) {
        sources = withContext(Dispatchers.IO) { collectSources(context) }
    }
    val source = sources.getOrNull(sourceIndex.coerceIn(0, (sources.size - 1).coerceAtLeast(0)))

    LaunchedEffect(source?.file?.absolutePath, revision, autoRefresh) {
        val file = source?.file ?: return@LaunchedEffect
        do {
            val loaded = withContext(Dispatchers.IO) {
                if (file.isFile) runCatching { readLog(file).lines() }.getOrElse { listOf(it.message ?: it.javaClass.simpleName) } else null
            }
            val wasAtEnd = !listState.canScrollForward
            lines = loaded
            if (loaded != null && (wasAtEnd || autoRefresh)) {
                listState.scrollToItem((loaded.size - 1).coerceAtLeast(0))
            }
            if (!autoRefresh) break
            delay(AUTO_REFRESH_MILLIS)
        } while (isActive)
    }

    val shown = remember(lines, query) {
        val all = lines.orEmpty()
        if (query.isBlank()) all else all.filter { it.contains(query, ignoreCase = true) }
    }
    val accent = AerixThemeManager.currentAccent()
    val logHighlighter = remember { LogHighlighter() }

    AerixPage(
        title = stringResource(R.string.aerix_logs_title),
        subtitle = stringResource(R.string.aerix_logs_subtitle),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                AerixGhostButton(stringResource(R.string.aerix_logs_refresh), R.drawable.aerix_ic_refresh, { revision++ })
                AerixGhostButton(stringResource(R.string.aerix_logs_copy), R.drawable.aerix_ic_upload, {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("log", shown.joinToString("\n")))
                    Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                })
                AerixGhostButton(stringResource(R.string.main_share_logs), R.drawable.aerix_ic_share_filled, {
                    val file = source?.file
                    if (file == null || sourceIndex == 0) PojavNavigator.shareLogs(activity) else Tools.openPath(activity, file, true)
                })
            }
        }
    ) {
        if (sources.size > 1) {
            PillTabs(
                tabs = sources.map { it.label },
                selected = sourceIndex.coerceIn(0, sources.size - 1),
                onSelect = { sourceIndex = it },
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.aerix_logs_search), color = AerixSurface.textMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(AerixRadii.control),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AerixSurface.textPrimary,
                    unfocusedTextColor = AerixSurface.textPrimary,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = AerixSurface.borderSoft,
                    cursorColor = accent
                )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                SliderSettingRow(
                    title = stringResource(R.string.aerix_logs_text_size),
                    summary = null,
                    value = AerixPreferences.logTextSize,
                    range = 5..20,
                    step = 1,
                    suffix = " sp",
                    onValueChange = { AerixPreferences.saveLogTextSize(it) }
                )
            }
            Box(Modifier.weight(1f)) {
                SwitchSettingRow(
                    title = stringResource(R.string.aerix_logs_auto_refresh),
                    summary = null,
                    checked = autoRefresh,
                    onCheckedChange = { autoRefresh = it }
                )
            }
        }

        val currentLines = lines
        if (currentLines == null) {
            QuietEmptyState(R.drawable.aerix_ic_terminal_outlined, stringResource(R.string.aerix_logs_empty))
        } else {
            val textSize = AerixPreferences.logTextSize.sp
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AerixSurface.canvas.copy(alpha = 0.72f), RoundedCornerShape(AerixRadii.panelSmall))
                    .border(1.dp, AerixSurface.borderSoft, RoundedCornerShape(AerixRadii.panelSmall))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AerixSpacing.smPlus)
                ) {
                    itemsIndexed(shown) { _, line ->
                        val highlighted = remember(line) { logHighlighter.highlight(line) }
                        Text(
                            text = highlighted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = textSize,
                            lineHeight = textSize * 1.3f
                        )
                    }
                }
            }
        }
    }
}
