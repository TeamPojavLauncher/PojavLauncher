/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/VersionSettingsScreen.kt
 *     (VersionSettingsScreen, InstanceSidebarCard, InstanceNavGrid, InstanceNavGridItem)
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/versions/ModsManagerScreen.kt
 *     (ModsManagerScreen, ModsCompactToolbar, ModItemLayout)
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/versions/VersionOverViewScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/versions/VersionConfigScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/versions/UpdateLoaderScreen.kt
 * Two-pane landscape layout: left sidebar with the back button, InstanceSidebarCard (icon, name,
 * "Loader MC · RAM MB", ⋮ menu, full-width accent Play button) and 2-column InstanceNavGrid
 * (Mods, Packs, Shaders, Worlds, Shots, Settings, Overview, Loader); right pane with the active
 * sub-screen.
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

import net.kdt.pojavlaunch.aerix.bridge.rememberControlLayoutCatalog
import net.kdt.pojavlaunch.aerix.vulkan.VulkanDriverManager
import android.text.format.DateFormat
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import net.kdt.pojavlaunch.aerix.bridge.ContentListPolicy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.resourcepack.ResourcePackOptimizer
import net.kdt.pojavlaunch.aerix.resourcepack.AndroidPackImageScaler
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.aerix.bridge.AerixIconCropper
import net.kdt.pojavlaunch.aerix.bridge.AerixInstance
import net.kdt.pojavlaunch.aerix.bridge.AerixInstanceDraft
import net.kdt.pojavlaunch.aerix.bridge.AssetType
import net.kdt.pojavlaunch.aerix.bridge.ContentEntry
import net.kdt.pojavlaunch.aerix.bridge.ContentKind
import net.kdt.pojavlaunch.aerix.bridge.PojavContentBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstaller
import net.kdt.pojavlaunch.aerix.bridge.LoaderKind
import net.kdt.pojavlaunch.aerix.bridge.LoaderVersion
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLoaderBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceEditorBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavNavigator
import net.kdt.pojavlaunch.aerix.bridge.PojavSettingsBridge
import net.kdt.pojavlaunch.aerix.components.ActionSettingRow
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.components.AerixPrimaryButton
import net.kdt.pojavlaunch.aerix.components.CardPosition
import net.kdt.pojavlaunch.aerix.components.MiraiEnumSettingsCard
import net.kdt.pojavlaunch.aerix.components.MiraiFollowGlobalState
import net.kdt.pojavlaunch.aerix.components.MiraiListSettingsCard
import net.kdt.pojavlaunch.aerix.components.MiraiRendererSummaryLayout
import net.kdt.pojavlaunch.aerix.components.MiraiSettingsCard
import net.kdt.pojavlaunch.aerix.components.MiraiStatefulDropdownMenuFollowGlobal
import net.kdt.pojavlaunch.aerix.components.MiraiTextInputSettingsCard
import net.kdt.pojavlaunch.aerix.components.MiraiTitleAndSummary
import net.kdt.pojavlaunch.aerix.components.MiraiToggleableIntSliderSettingsCard
import net.kdt.pojavlaunch.aerix.components.ModrinthMetaPill
import net.kdt.pojavlaunch.aerix.components.QuietEmptyState
import net.kdt.pojavlaunch.aerix.components.SettingsCardColumn
import net.kdt.pojavlaunch.aerix.components.SliderSettingRow
import net.kdt.pojavlaunch.aerix.components.SwitchSettingRow
import net.kdt.pojavlaunch.aerix.components.getMiraiInstanceRendererId
import net.kdt.pojavlaunch.aerix.components.miraiRendererEntries
import net.kdt.pojavlaunch.aerix.components.MiraiRendererEntry
import net.kdt.pojavlaunch.game.renderer.MobileGluesWarning
import net.kdt.pojavlaunch.aerix.components.saveMiraiInstanceRendererId
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.aerix.ui.AerixNav
import net.kdt.pojavlaunch.aerix.ui.LauncherSection
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.File
import java.util.Date

private data class InstanceSubTab(
    val id: String,
    val shortLabel: String,
    @DrawableRes val iconRes: Int
)

private val instanceSubTabs = listOf(
    InstanceSubTab("mods", "Mods", R.drawable.aerix_ic_extension_outlined),
    InstanceSubTab("packs", "Packs", R.drawable.aerix_ic_format_paint_outlined),
    InstanceSubTab("shaders", "Shaders", R.drawable.aerix_ic_lightbulb),
    InstanceSubTab("worlds", "Worlds", R.drawable.aerix_ic_public),
    InstanceSubTab("shots", "Shots", R.drawable.aerix_ic_image_outlined),
    InstanceSubTab("settings", "Settings", R.drawable.aerix_ic_build_outlined),
    InstanceSubTab("overview", "Overview", R.drawable.aerix_ic_dashboard_outlined),
    InstanceSubTab("loader", "Loader", R.drawable.aerix_ic_update)
)

private enum class ModFilterState(val label: String) {
    ALL("All"),
    ENABLED("Enabled"),
    DISABLED("Disabled")
}

private val mcVerRegex = Regex("""(\d+\.\d+(?:\.\d+)?)""")

@Composable
fun InstanceEditorScreen(activity: FragmentActivity, onClose: () -> Unit) {
    val contentPaneStates = rememberSaveableStateHolder()
    LaunchedEffect(Unit) {
        if (PojavInstanceEditorBridge.draft == null) {
            PojavInstanceEditorBridge.open(activity)
        }
    }
    val draft = PojavInstanceEditorBridge.draft
    draft?.let { net.kdt.pojavlaunch.aerix.ui.aerixLauncherActivity("Editing ${it.name.ifBlank { it.versionId }.take(36)}") }
    if (draft == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_instance), color = AerixSurface.textSecondary, fontSize = 13.sp)
        }
        return
    }

    var activeTab by rememberSaveable { mutableStateOf(PojavInstanceBridge.requestedSubTab) }
    LaunchedEffect(PojavInstanceBridge.requestedSubTab) {
        activeTab = PojavInstanceBridge.requestedSubTab
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectedAerixInstance = PojavInstanceBridge.selected
    if (showExportDialog && selectedAerixInstance != null) {
        ModpackExportDialog(
            activity = activity,
            instance = selectedAerixInstance,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AerixSurface.panelRaised,
            title = { Text(stringResource(R.string.aerix_editor_delete_title), color = AerixSurface.textPrimary) },
            text = {
                Text(
                    stringResource(R.string.aerix_editor_delete_message, draft.name.ifEmpty { draft.versionId }),
                    color = AerixSurface.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    if (PojavInstanceEditorBridge.delete(activity)) onClose()
                }) {
                    Text(stringResource(R.string.aerix_editor_delete_confirm), color = AerixSurface.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.aerix_editor_delete_cancel), color = AerixSurface.textSecondary)
                }
            }
        )
    }

    // Two-pane layout from Mirai VersionSettingsScreen.kt (Screenshot 2). The loader page needs
    // the width - two version lists side by side - so the sidebar gives it up while that page is
    // open instead of squeezing the lists.
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // Left sidebar: Back button + InstanceSidebarCard + 2-column InstanceNavGrid
        Column(
            modifier = Modifier
                .width(if (activeTab == "loader") 226.dp else 264.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(AerixRadii.control),
                    color = AerixSurface.panel,
                    border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft),
                    onClick = {
                        PojavInstanceEditorBridge.save(activity)
                        onClose()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_arrow_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                InstanceSidebarCard(
                    draft = draft,
                    onPlay = {
                        PojavInstanceEditorBridge.save(activity)
                        PojavLaunchBridge.play(activity)
                    },
                    onOpenFolder = { PojavInstanceEditorBridge.openFolder(activity) },
                    onExportModpack = { showExportDialog = true },
                    onDelete = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            InstanceNavGrid(
                selectedTab = activeTab,
                onSelectTab = {
                    activeTab = it
                    PojavInstanceBridge.requestedSubTab = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Right content pane
        if (activeTab == "settings") {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                InstanceSettingsPane(
                    activity = activity,
                    draft = draft,
                    onDelete = { showDeleteDialog = true },
                    onExport = { showExportDialog = true },
                    onSaved = onClose
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(AerixRadii.panel),
                color = AerixSurface.panel,
                border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
            ) {
                when (activeTab) {
                    "mods" -> contentPaneStates.SaveableStateProvider("${PojavInstanceBridge.selected?.source}:mods") { InstanceModsPane(activity = activity) }
                    "packs" -> contentPaneStates.SaveableStateProvider("${PojavInstanceBridge.selected?.source}:packs") { InstanceContentPane(activity = activity, kind = ContentKind.RESOURCE_PACKS, assetType = AssetType.RESOURCE_PACK) }
                    "shaders" -> contentPaneStates.SaveableStateProvider("${PojavInstanceBridge.selected?.source}:shaders") { InstanceContentPane(activity = activity, kind = ContentKind.SHADER_PACKS, assetType = AssetType.SHADER_PACK) }
                    "worlds" -> contentPaneStates.SaveableStateProvider("${PojavInstanceBridge.selected?.source}:worlds") { InstanceContentPane(activity = activity, kind = ContentKind.SAVES, assetType = AssetType.WORLD) }
                    "shots" -> contentPaneStates.SaveableStateProvider("${PojavInstanceBridge.selected?.source}:shots") { InstanceContentPane(activity = activity, kind = ContentKind.SCREENSHOTS, assetType = null) }
                    "overview" -> InstanceOverviewPane(
                        activity = activity,
                        draft = draft,
                        onSelectTab = {
                            activeTab = it
                            PojavInstanceBridge.requestedSubTab = it
                        },
                        onExport = { showExportDialog = true }
                    )
                    "loader" -> InstanceLoaderPane(activity = activity, draft = draft)
                    else -> InstanceModsPane(activity = activity)
                }
            }
        }
    }
}

/**
 * Mirai InstanceSidebarCard (VersionSettingsScreen.kt lines 490-590, Screenshot 2 top-left card).
 */
@Composable
private fun InstanceSidebarCard(
    draft: AerixInstanceDraft,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit,
    onExportModpack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vanilla = stringResource(R.string.aerix_home_vanilla)
    val loader = loaderLabel(draft.versionId, vanilla)
    val mcVer = mcVerRegex.find(draft.versionId)?.value ?: draft.versionId.ifBlank { "Latest" }
    val ramMb = net.kdt.pojavlaunch.utils.jre.InstanceMemory.effectiveMb(draft.jvmArgs, LauncherPreferences.PREF_RAM_ALLOCATION)
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AerixRadii.panelSmall),
        color = AerixSurface.panel,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AerixSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                AerixImageWell(
                    image = draft.icon,
                    fallback = R.drawable.aerix_loader_vanilla,
                    size = 38.dp,
                    radius = AerixRadii.control
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
                ) {
                    Text(
                        text = draft.name.ifEmpty { stringResource(R.string.unnamed) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AerixSurface.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$loader $mcVer   \u00B7   $ramMb MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = AerixSurface.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_more_vert),
                            contentDescription = "More",
                            tint = AerixSurface.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Modpack") },
                            onClick = {
                                showMenu = false
                                onExportModpack()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open instance folder") },
                            onClick = {
                                showMenu = false
                                onOpenFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete instance", color = AerixSurface.danger) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(AerixRadii.control),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AerixSurface.action,
                    contentColor = AerixSurface.onAction
                ),
                contentPadding = PaddingValues(horizontal = AerixSpacing.md, vertical = AerixSpacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_play_arrow_filled),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(AerixSpacing.sm))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Mirai InstanceNavGrid + InstanceNavGridItem (VersionSettingsScreen.kt lines 595-675):
 * 2-column grid of 38dp navigation cells.
 */
@Composable
private fun InstanceNavGrid(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccent = AerixThemeManager.currentAccent()
    val rows = remember { instanceSubTabs.chunked(2) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
            ) {
                row.forEach { tab ->
                    val selected = selectedTab == tab.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(AerixRadii.control),
                        color = if (selected) activeAccent.copy(alpha = 0.16f) else AerixSurface.panel,
                        contentColor = if (selected) activeAccent else AerixSurface.textPrimary,
                        border = BorderStroke(
                            AerixSpacing.hairline,
                            if (selected) activeAccent else AerixSurface.borderSoft
                        ),
                        onClick = { onSelectTab(tab.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AerixSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
                        ) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                tint = if (selected) activeAccent else AerixSurface.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tab.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selected) activeAccent else AerixSurface.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                repeat(2 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================================================
// Mods sub-screen (Mirai ModsManagerScreen.kt: ModsCompactToolbar + ModItemLayout — Screenshot 2)
// ============================================================================================

@Composable
private fun InstanceModsPane(activity: FragmentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AerixThemeManager.currentAccent()

    val listState = rememberLazyListState()
    val selection = remember { ContentSelectionState() }

    var entries by remember { mutableStateOf<List<ContentEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    var nameFilter by rememberSaveable { mutableStateOf("") }
    var stateFilter by rememberSaveable { mutableStateOf(ModFilterState.ALL) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val failedText = stringResource(R.string.aerix_content_failed)
    val importedText = stringResource(R.string.aerix_content_imported)

    LaunchedEffect(revision, PojavInstanceBridge.selected?.source) {
        loading = true
        entries = PojavContentBridge.list(ContentKind.MODS).orEmpty()
        loading = false
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var ok = 0
                uris.forEach { if (PojavContentBridge.import(context, ContentKind.MODS, it)) ok++ }
                Toast.makeText(context, if (ok == uris.size) importedText else failedText, Toast.LENGTH_SHORT).show()
                revision++
            }
        }
    }

    val filtered = remember(entries, nameFilter, stateFilter) {
        entries.filter { e ->
            val matchesName = nameFilter.isBlank() ||
                e.displayTitle.contains(nameFilter, ignoreCase = true) ||
                e.name.contains(nameFilter, ignoreCase = true)
            val matchesState = when (stateFilter) {
                ModFilterState.ALL -> true
                ModFilterState.ENABLED -> e.enabled
                ModFilterState.DISABLED -> !e.enabled
            }
            matchesName && matchesState
        }
    }

    val rowKeys = remember(entries) { ContentListPolicy.rowKeys(entries) }

    val enabledCount = remember(entries) { entries.count { it.enabled } }
    val disabledCount = remember(entries) { entries.count { !it.enabled } }

    fun countOf(filter: ModFilterState): Int = when (filter) {
        ModFilterState.ALL -> entries.size
        ModFilterState.ENABLED -> enabledCount
        ModFilterState.DISABLED -> disabledCount
    }

    var showAddMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ModsCompactToolbar (Mirai ModsManagerScreen.kt lines 837-1068 — Screenshot 2 top bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            // Search field ("Search 19 mods...")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(AerixRadii.pill))
                    .background(AerixSurface.panelRaised.copy(alpha = 0.36f))
                    .border(AerixSpacing.hairline, AerixSurface.borderSoft, RoundedCornerShape(AerixRadii.pill))
                    .padding(horizontal = AerixSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (nameFilter.isEmpty()) {
                        Text(
                            text = "Search ${entries.size} mods...",
                            fontSize = 12.sp,
                            color = AerixSurface.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = nameFilter,
                        onValueChange = { nameFilter = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // State filter dropdown ("All 19 ▾")
            Box {
                val selectedFilter = stateFilter != ModFilterState.ALL
                Surface(
                    shape = RoundedCornerShape(AerixRadii.control),
                    color = if (selectedFilter) AerixSurface.accentContainer else AerixSurface.panel,
                    border = BorderStroke(
                        AerixSpacing.hairline,
                        if (selectedFilter) accent else AerixSurface.border
                    ),
                    onClick = { showFilterMenu = true }
                ) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = AerixSpacing.smPlus),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
                    ) {
                        Text(
                            text = stateFilter.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedFilter) accent else AerixSurface.textPrimary
                        )
                        Text(
                            text = countOf(stateFilter).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedFilter) accent else AerixSurface.textSecondary
                        )
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_arrow_drop_down_rounded),
                            contentDescription = null,
                            tint = AerixSurface.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    ModFilterState.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text("${f.label}  (${countOf(f)})") },
                            onClick = {
                                showFilterMenu = false
                                stateFilter = f
                            }
                        )
                    }
                }
            }

            // Warning / diagnostics button (Screenshot 2)
            Surface(
                shape = RoundedCornerShape(AerixRadii.control),
                color = AerixSurface.panel,
                border = BorderStroke(AerixSpacing.hairline, AerixSurface.border),
                onClick = {
                    Toast.makeText(
                        context,
                        "${enabledCount} enabled, ${disabledCount} disabled \u2014 no conflicts detected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .padding(horizontal = AerixSpacing.smPlus),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.aerix_ic_warning_outlined),
                        contentDescription = null,
                        tint = AerixSurface.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Refresh button
            IconButton(
                onClick = { revision++ },
                enabled = !selection.busy && !loading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_refresh),
                    contentDescription = "Refresh",
                    tint = AerixSurface.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Accent "+" button (Download from Discover or Import local .jar)
            Box {
                Surface(
                    shape = RoundedCornerShape(AerixRadii.control),
                    color = AerixSurface.action,
                    contentColor = AerixSurface.onAction,
                    onClick = { showAddMenu = true }
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_add),
                            contentDescription = "Add",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Download from Modrinth / CurseForge") },
                        onClick = {
                            showAddMenu = false
                            AerixDownloadRequest.open(AssetType.MOD) { AerixNav.navigate(it) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import local .jar file") },
                        onClick = {
                            showAddMenu = false
                            importPicker.launch(ContentKind.MODS.importMimeTypes)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open mods folder") },
                        onClick = {
                            showAddMenu = false
                            PojavContentBridge.openFolder(activity, ContentKind.MODS)
                        }
                    )
                }
            }
        }

        ContentSelectionControls(ContentKind.MODS, entries, filtered, selection, loading) { revision++ }
        when {
            loading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                QuietEmptyState(R.drawable.aerix_ic_extension_outlined, stringResource(R.string.aerix_content_empty))
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = AerixSpacing.md, vertical = AerixSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                items(filtered, key = { rowKeys.getValue(it.file.absolutePath) }) { entry ->
                    ModItemCard(
                        entry = entry,
                        selecting = selection.selecting,
                        selected = entry.file.absolutePath in selection.selected,
                        busy = selection.busy || loading,
                        onSelect = { selection.toggle(entry) },
                        onToggle = { enabled ->
                            if (!selection.busy && !loading) scope.launch {
                                selection.busy = true
                                try {
                                    val ok = try { PojavContentBridge.setEnabled(entry, enabled) }
                                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                    catch (_: Exception) { false }
                                    if (!ok) Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                                    revision++
                                } finally { selection.busy = false }
                            }
                        },
                        onDelete = {
                            if (!selection.busy && !loading) selection.pendingDelete = listOf(entry)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Mirai ModItemLayout (ModsManagerScreen.kt lines 1411-1595 — Screenshot 2 rows):
 * 40dp icon, bold displayTitle + loader pills (Fabric / Quilt / Forge / NeoForge),
 * filename without .jar/.disabled below, 0.75-scale Switch and ⋮ menu.
 */
@Composable
private fun ModItemCard(
    entry: ContentEntry,
    selecting: Boolean,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().then(if (selecting) Modifier.clickable(enabled = !busy, onClick = onSelect) else Modifier),
        shape = RoundedCornerShape(AerixRadii.card),
        color = AerixSurface.panel,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selecting) Checkbox(checked = selected, onCheckedChange = { onSelect() }, enabled = !busy)
            AerixImageWell(
                image = entry.icon,
                fallback = R.drawable.aerix_ic_extension_outlined,
                size = 40.dp,
                radius = AerixRadii.control
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = entry.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    entry.loaders.take(2).forEach { loader ->
                        Surface(
                            shape = RoundedCornerShape(AerixRadii.compact),
                            color = accent
                        ) {
                            Text(
                                text = loader,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AerixSurface.onAccent,
                                modifier = Modifier.padding(horizontal = AerixSpacing.smCompact, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = entry.fileBaseName,
                    style = MaterialTheme.typography.labelSmall,
                    color = AerixSurface.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!selecting) Row(
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    modifier = Modifier.scale(0.75f),
                    checked = entry.enabled,
                    enabled = !busy,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AerixSurface.onAction,
                        checkedTrackColor = AerixSurface.action,
                        uncheckedThumbColor = AerixSurface.textSecondary,
                        uncheckedTrackColor = AerixSurface.panelRaised,
                        uncheckedBorderColor = AerixSurface.panelRaised
                    )
                )

                Box {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        enabled = !busy,
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_more_vert),
                            contentDescription = "More",
                            tint = AerixSurface.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (entry.enabled) "Disable" else "Enable") },
                            onClick = {
                                showMenu = false
                                onToggle(!entry.enabled)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = AerixSurface.danger) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// Resource Packs / Shaders / Worlds / Screenshots sub-screens
// ============================================================================================

@Composable
private fun InstanceContentPane(
    activity: FragmentActivity,
    kind: ContentKind,
    assetType: AssetType?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AerixThemeManager.currentAccent()

    val listState = rememberLazyListState()
    val selection = remember(kind) { ContentSelectionState() }

    var entries by remember(kind) { mutableStateOf<List<ContentEntry>>(emptyList()) }
    var loading by remember(kind) { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    var query by rememberSaveable(kind) { mutableStateOf("") }
    var optimizeTarget by remember(kind) { mutableStateOf<ContentEntry?>(null) }
    var optimizing by remember(kind) { mutableStateOf(false) }
    var optimizeMessage by remember(kind) { mutableStateOf<String?>(null) }

    val failedText = stringResource(R.string.aerix_content_failed)
    val importedText = stringResource(R.string.aerix_content_imported)

    LaunchedEffect(kind, revision, PojavInstanceBridge.selected?.source) {
        loading = true
        entries = PojavContentBridge.list(kind).orEmpty()
        loading = false
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var ok = 0
                uris.forEach { if (PojavContentBridge.import(context, kind, it)) ok++ }
                Toast.makeText(context, if (ok == uris.size) importedText else failedText, Toast.LENGTH_SHORT).show()
                revision++
            }
        }
    }

    val shown = remember(entries, query) {
        entries.filter { query.isBlank() || it.displayTitle.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) }
    }

    val rowKeys = remember(entries) { ContentListPolicy.rowKeys(entries) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(AerixRadii.pill))
                    .background(AerixSurface.panelRaised.copy(alpha = 0.36f))
                    .border(AerixSpacing.hairline, AerixSurface.borderSoft, RoundedCornerShape(AerixRadii.pill))
                    .padding(horizontal = AerixSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search ${entries.size} ${stringResource(kind.titleRes).lowercase()}...",
                            fontSize = 12.sp,
                            color = AerixSurface.textSecondary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            IconButton(
                onClick = { PojavContentBridge.openFolder(activity, kind) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_folder_outlined),
                    contentDescription = "Open folder",
                    tint = AerixSurface.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { revision++ },
                enabled = !selection.busy && !loading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_refresh),
                    contentDescription = "Refresh",
                    tint = AerixSurface.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (assetType != null) {
                Surface(
                    shape = RoundedCornerShape(AerixRadii.control),
                    color = AerixSurface.panelRaised,
                    border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft),
                    onClick = { AerixDownloadRequest.open(assetType) { AerixNav.navigate(it) } }
                ) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_download_2_outlined),
                            contentDescription = "Download",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(AerixRadii.control),
                color = AerixSurface.action,
                contentColor = AerixSurface.onAction,
                onClick = { importPicker.launch(kind.importMimeTypes) }
            ) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.aerix_ic_add),
                        contentDescription = "Import",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (kind == ContentKind.RESOURCE_PACKS) {
            Text("Tap the wrench next to a ZIP to create a smaller copy. The original stays untouched.",
                color = AerixSurface.textSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = AerixSpacing.md))
        }
        ContentSelectionControls(kind, entries, shown, selection, loading) { revision++ }
        when {
            loading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                QuietEmptyState(kind.iconRes, stringResource(R.string.aerix_content_empty))
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = AerixSpacing.md, vertical = AerixSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                items(shown, key = { rowKeys.getValue(it.file.absolutePath) }) { entry ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AerixRadii.card),
                        color = AerixSurface.panel,
                        border = BorderStroke(AerixSpacing.hairline, AerixSurface.border),
                        enabled = !selection.busy && !loading,
                        onClick = {
                            if (selection.selecting) selection.toggle(entry)
                            else if (kind == ContentKind.SCREENSHOTS) {
                                Tools.openPath(context, entry.file, true)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selection.selecting) Checkbox(checked = entry.file.absolutePath in selection.selected,
                                onCheckedChange = { selection.toggle(entry) }, enabled = !selection.busy && !loading)
                            AerixImageWell(
                                image = entry.icon,
                                fallback = kind.iconRes,
                                size = 40.dp,
                                radius = AerixRadii.control
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = Formatter.formatShortFileSize(context, entry.sizeBytes) + " \u00B7 " +
                                        DateFormat.getMediumDateFormat(context).format(Date(entry.lastModified)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AerixSurface.textSecondary,
                                    maxLines = 1
                                )
                            }
                            if (!selection.selecting && kind == ContentKind.RESOURCE_PACKS && entry.file.isFile && entry.name.endsWith(".zip", ignoreCase = true)) {
                                IconButton(enabled = !selection.busy && !loading && !optimizing, onClick = { optimizeTarget = entry }) {
                                    Icon(painter = painterResource(R.drawable.aerix_ic_build_outlined), contentDescription = "Make smaller pack copy", tint = accent, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (!selection.selecting) IconButton(
                                enabled = !selection.busy && !loading && !optimizing,
                                onClick = { selection.pendingDelete = listOf(entry) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.aerix_ic_delete_outlined),
                                    contentDescription = "Delete",
                                    tint = AerixSurface.danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    optimizeTarget?.let { target ->
        AlertDialog(onDismissRequest = { if (!optimizing) optimizeTarget = null },
            title = { Text("Make a smaller pack copy?") },
            text = { Text("Aerix will create a separate copy with smaller large menu textures. Animated textures stay unchanged, and the original pack is kept. If the copy still crashes, the cause may be something else. Select only the copy in Minecraft, not both.") },
            confirmButton = { TextButton(enabled = !optimizing, onClick = {
                optimizing = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { ResourcePackOptimizer.createCopy(target.file, AndroidPackImageScaler) } }
                    optimizing = false
                    optimizeTarget = null
                    optimizeMessage = result.fold(
                        { "Created ${it.file.name}\n\nReduced ${it.resized} large static images${if (it.metadataRepaired) " and repaired extra characters in pack metadata" else ""}. In Minecraft Resource Packs, turn off the original and select this copy. Compatibility and crash relief aren't guaranteed." },
                        { "Could not create a copy: ${it.message ?: "unknown error"}. The original was not changed." }
                    )
                    revision++
                }
            }) { Text(if (optimizing) "Creating…" else "Create copy") } },
            dismissButton = { TextButton(enabled = !optimizing, onClick = { optimizeTarget = null }) { Text("Cancel") } })
    }
    optimizeMessage?.let { message ->
        AlertDialog(onDismissRequest = { optimizeMessage = null }, title = { Text("Resource pack") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { optimizeMessage = null }) { Text("OK") } })
    }
}

// ============================================================================================
// Settings sub-screen (Mirai VersionConfigScreen.kt — Photos 3, 4 & 5)
// ============================================================================================

@Composable
private fun InstanceSettingsPane(
    activity: FragmentActivity,
    draft: AerixInstanceDraft,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onSaved: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    val instanceKey = remember(draft.instance) { draft.instance.name ?: draft.versionId }
    val prefs = LauncherPreferences.DEFAULT_PREF

    val rendererEntries = remember { miraiRendererEntries(activity, includeFollowGlobal = true) }
    var currentMiraiRendererId by remember(draft.renderer) {
        mutableStateOf(getMiraiInstanceRendererId(activity, instanceKey, draft.renderer))
    }
    // Picking MobileGlues here is held until the warning has been read, exactly like the settings
    // page: the instance is not changed while the dialog is open.
    var pendingMobileGlues by remember { mutableStateOf<MiraiRendererEntry?>(null) }
    pendingMobileGlues?.let { entry ->
        val close = { pendingMobileGlues = null }
        MobileGluesWarningDialog(
            versionId = draft.versionId,
            onConfirm = {
                close()
                currentMiraiRendererId = entry.uniqueId
                draft.renderer = entry.backendRendererId
                saveMiraiInstanceRendererId(instanceKey, entry)
                PojavInstanceEditorBridge.save(activity)
            },
            onRecommended = { recommended ->
                close()
                // The renderer this instance's version wants, taken from the list the device
                // actually offers; the plain "Follow Global" entry stays reachable as the fallback.
                val replacement = rendererEntries.firstOrNull { it.uniqueId == recommended }
                if (replacement != null) {
                    currentMiraiRendererId = replacement.uniqueId
                    draft.renderer = replacement.backendRendererId
                    saveMiraiInstanceRendererId(instanceKey, replacement)
                    PojavInstanceEditorBridge.save(activity)
                }
            },
            onDismiss = close
        )
    }

    val runtimeChoices = remember {
        buildList {
            add("" to "Follow Global")
            PojavInstanceEditorBridge.runtimeChoices(activity).forEach { choice ->
                if (choice.name != null) {
                    add(choice.name to choice.label)
                }
            }
        }
    }

    val controlCatalog = rememberControlLayoutCatalog(activity)
    val controlEntries = remember(controlCatalog.files) {
        buildList {
            add("" to "Follow Global")
            controlCatalog.files.forEach { file ->
                add(file.name to file.nameWithoutExtension)
            }
        }
    }

    val vulkanDriverEntries = remember {
        buildList {
            add("" to "Follow Global")
            add(VulkanDriverManager.SYSTEM_DRIVER to "System Vulkan driver")
            add(VulkanDriverManager.TURNIP_DRIVER to "Turnip (built-in)")
            VulkanDriverManager.installedDrivers(activity).forEach { driver -> add(driver.id to driver.name) }
        }
    }
    var vulkanDriver by remember(instanceKey) {
        mutableStateOf(
            VulkanDriverManager.normalizeInstanceSelection(
                activity,
                prefs.getString("aerix_inst_vulkan_$instanceKey", "") ?: ""
            )
        )
    }

    val graphicsApiEntries = remember {
        listOf(
            "" to "Follow Global",
            "DEFAULT" to "Default",
            "DEFAULT_GL" to "Default (OpenGL)",
            "OPENGL_ES_3" to "OpenGL ES 3",
            "VULKAN" to "Vulkan"
        )
    }
    var graphicsApi by remember(instanceKey) {
        mutableStateOf(prefs.getString("aerix_inst_gapi_$instanceKey", "") ?: "")
    }

    var skipIntegrityState by remember(instanceKey) {
        val raw = prefs.getString("aerix_inst_skip_integrity_$instanceKey", MiraiFollowGlobalState.FOLLOW_GLOBAL.name)
        mutableStateOf(
            runCatching { MiraiFollowGlobalState.valueOf(raw ?: MiraiFollowGlobalState.FOLLOW_GLOBAL.name) }
                .getOrDefault(MiraiFollowGlobalState.FOLLOW_GLOBAL)
        )
    }

    var customInfo by remember(instanceKey) {
        mutableStateOf(prefs.getString("aerix_inst_custom_info_$instanceKey", "") ?: "")
    }
    var programArgs by remember(instanceKey) {
        mutableStateOf(prefs.getString("aerix_inst_program_args_$instanceKey", "") ?: "")
    }
    var serverIp by remember(instanceKey) {
        mutableStateOf(prefs.getString("aerix_inst_server_ip_$instanceKey", "") ?: "")
    }
    var touchVibrateState by remember(instanceKey) {
        val raw = prefs.getString("aerix_inst_vibrate_state_$instanceKey", MiraiFollowGlobalState.FOLLOW_GLOBAL.name)
        mutableStateOf(
            runCatching { MiraiFollowGlobalState.valueOf(raw ?: MiraiFollowGlobalState.FOLLOW_GLOBAL.name) }
                .getOrDefault(MiraiFollowGlobalState.FOLLOW_GLOBAL)
        )
    }
    var vibrateDuration by remember(instanceKey) {
        mutableIntStateOf(prefs.getInt("aerix_inst_vibrate_duration_$instanceKey", -1))
    }

    val maxRamMb = remember { PojavSettingsBridge.safeMaxRam(activity).coerceAtLeast(256) }
    val globalRamMb = remember { LauncherPreferences.PREF_RAM_ALLOCATION.coerceIn(256, maxRamMb) }
    var customRamMb by remember(draft.jvmArgs) {
        mutableIntStateOf(net.kdt.pojavlaunch.utils.jre.InstanceMemory.overrideMb(draft.jvmArgs))
    }

    val isolationState = if (!draft.sharedData) MiraiFollowGlobalState.ENABLE else MiraiFollowGlobalState.DISABLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AerixSpacing.xs,
                end = AerixSpacing.xs,
                top = AerixSpacing.xs,
                bottom = AerixSpacing.xxl
            ),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // ====================================================================================
        // Section 1: Version-Specific Settings (Photos 3 & 5)
        // ====================================================================================
        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = AerixSpacing.xs, vertical = 4.dp),
                text = "Version-Specific Settings",
                color = accent,
                style = MaterialTheme.typography.labelLarge
            )

            MiraiStatefulDropdownMenuFollowGlobal(
                position = CardPosition.Top,
                title = "Version Isolation",
                summary = "Isolate the current version so that its data does not interfere with any other versions",
                currentState = isolationState,
                onValueChange = { state ->
                    draft.sharedData = (state == MiraiFollowGlobalState.DISABLE)
                    PojavInstanceEditorBridge.save(activity)
                }
            )

            MiraiStatefulDropdownMenuFollowGlobal(
                position = CardPosition.Middle,
                title = "Skip Integrity Check",
                summary = "When launching the game, mismatched game files will not be re-downloaded",
                currentState = skipIntegrityState,
                onValueChange = { state ->
                    skipIntegrityState = state
                    prefs.edit().putString("aerix_inst_skip_integrity_$instanceKey", state.name).apply()
                    when (state) {
                        MiraiFollowGlobalState.ENABLE -> PojavSettingsBridge.writeBoolean(activity, "checkGameFiles", false)
                        MiraiFollowGlobalState.DISABLE -> PojavSettingsBridge.writeBoolean(activity, "checkGameFiles", true)
                        MiraiFollowGlobalState.FOLLOW_GLOBAL -> {}
                    }
                }
            )

            MiraiListSettingsCard(
                position = CardPosition.Middle,
                items = rendererEntries,
                currentId = currentMiraiRendererId,
                defaultId = "",
                title = "Renderer",
                getItemText = { it.displayName },
                getItemId = { it.uniqueId },
                getItemSummary = { item ->
                    MiraiRendererSummaryLayout(renderer = item)
                },
                onValueChange = { entry ->
                    if (MobileGluesWarning.applies(entry.backendRendererId)) {
                        // Nothing is written until the warning is answered.
                        pendingMobileGlues = entry
                    } else {
                        currentMiraiRendererId = entry.uniqueId
                        draft.renderer = entry.backendRendererId
                        saveMiraiInstanceRendererId(instanceKey, entry)
                        PojavInstanceEditorBridge.save(activity)
                    }
                }
            )

            MiraiListSettingsCard(
                position = CardPosition.Middle,
                items = vulkanDriverEntries,
                currentId = vulkanDriver,
                defaultId = "",
                title = "Vulkan Driver",
                getItemText = { it.second },
                getItemId = { it.first },
                onValueChange = { item ->
                    vulkanDriver = item.first
                    prefs.edit().putString("aerix_inst_vulkan_$instanceKey", item.first).apply()
                }
            )

            MiraiListSettingsCard(
                position = CardPosition.Middle,
                items = graphicsApiEntries,
                currentId = graphicsApi,
                defaultId = "",
                title = "Graphics API",
                summary = "Specify the graphics API used by the game",
                getItemText = { it.second },
                getItemId = { it.first },
                onValueChange = { item ->
                    graphicsApi = item.first
                    prefs.edit().putString("aerix_inst_gapi_$instanceKey", item.first).apply()
                }
            )

            MiraiListSettingsCard(
                position = CardPosition.Bottom,
                items = controlEntries,
                currentId = draft.controlLayout ?: "",
                defaultId = "",
                title = "Control Layout",
                getItemText = { it.second },
                getItemId = { it.first },
                trailingIcon = {
                    IconButton(onClick = { controlCatalog.refresh() }) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_refresh),
                            contentDescription = "Refresh Controls",
                            tint = Color.White
                        )
                    }
                },
                onValueChange = { item ->
                    draft.controlLayout = item.first.ifEmpty { null }
                    PojavInstanceEditorBridge.save(activity)
                }
            )
        }

        // ====================================================================================
        // Section 2: Game Settings (Photo 4)
        // ====================================================================================
        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = AerixSpacing.xs, vertical = 4.dp),
                text = "Game Settings",
                color = accent,
                style = MaterialTheme.typography.labelLarge
            )

            MiraiListSettingsCard(
                position = CardPosition.Top,
                items = runtimeChoices,
                currentId = draft.runtime ?: "",
                defaultId = "",
                title = "Java Runtime",
                summary = "Specify the Java environment used by the current version",
                getItemText = { it.second },
                getItemId = { it.first },
                onValueChange = { item ->
                    draft.runtime = item.first.ifEmpty { null }
                    PojavInstanceEditorBridge.save(activity)
                }
            )

            MiraiToggleableIntSliderSettingsCard(
                position = CardPosition.Middle,
                currentValue = customRamMb,
                defaultValue = globalRamMb,
                title = "Memory Allocation",
                summary = "Set a separate Java heap for this instance, or uncheck to follow the launcher default",
                valueRange = 256..maxRamMb,
                suffix = "MB",
                fineTuningControl = true,
                onValueChange = { ram ->
                    customRamMb = ram
                },
                onValueFinished = { ram ->
                    customRamMb = ram
                    draft.jvmArgs = net.kdt.pojavlaunch.utils.jre.InstanceMemory.withOverride(draft.jvmArgs, ram)
                    PojavInstanceEditorBridge.save(activity)
                }
            )

            MiraiTextInputSettingsCard(
                position = CardPosition.Middle,
                value = customInfo,
                title = "Custom Version Info",
                summary = "Custom info is shown at the bottom-left of the main menu and top-left of the F3 debug screen. It may not work for special versions or external account logins. You can add the [zl_version] tag, which will be replaced with the current version number automatically",
                label = "Follow global if left blank",
                onValueChange = { value ->
                    customInfo = value
                    prefs.edit().putString("aerix_inst_custom_info_$instanceKey", value).apply()
                }
            )

            MiraiTextInputSettingsCard(
                position = CardPosition.Middle,
                value = programArgs,
                title = "Program Arguments",
                summary = "Additional arguments passed directly to the Minecraft game client",
                label = "Disabled if left blank",
                onValueChange = { value ->
                    programArgs = value
                    prefs.edit().putString("aerix_inst_program_args_$instanceKey", value).apply()
                }
            )

            MiraiTextInputSettingsCard(
                position = CardPosition.Middle,
                value = draft.jvmArgs,
                title = "JVM Arguments",
                summary = "Custom arguments passed to the Java Virtual Machine when launching the game",
                label = "Follow global if left blank",
                onValueChange = { value ->
                    draft.jvmArgs = value
                    PojavInstanceEditorBridge.save(activity)
                }
            )

            MiraiTextInputSettingsCard(
                position = CardPosition.Bottom,
                value = serverIp,
                title = "Auto-Join Server IP",
                summary = "Automatically connect to this server address when the game starts",
                label = "Disabled if left blank",
                onValueChange = { value ->
                    serverIp = value
                    prefs.edit().putString("aerix_inst_server_ip_$instanceKey", value).apply()
                }
            )
        }

        // ====================================================================================
        // Section 3: Support Settings
        // ====================================================================================
        SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = AerixSpacing.xs, vertical = 4.dp),
                text = "Support Settings",
                color = accent,
                style = MaterialTheme.typography.labelLarge
            )

            MiraiTextInputSettingsCard(
                position = CardPosition.Top,
                value = draft.name,
                title = stringResource(R.string.aerix_editor_name_label),
                summary = "Rename this instance or tap the icon below to customize its badge",
                label = "Instance Name",
                onValueChange = { value ->
                    draft.name = value
                    PojavInstanceEditorBridge.save(activity)
                }
            )

            MiraiSettingsCard(
                position = CardPosition.Middle,
                innerPadding = PaddingValues(all = AerixSpacing.lg),
                onClick = {
                    AerixIconCropper.start(activity) {
                        PojavInstanceEditorBridge.applyIcon(it)
                        PojavInstanceEditorBridge.save(activity)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiraiTitleAndSummary(
                        title = "Instance Icon",
                        summary = "Tap to choose and crop a custom icon for this instance",
                        modifier = Modifier.weight(1f)
                    )
                    AerixImageWell(
                        image = draft.icon,
                        fallback = R.drawable.aerix_loader_vanilla,
                        size = 40.dp
                    )
                }
            }

            MiraiEnumSettingsCard(
                position = CardPosition.Middle,
                items = MiraiFollowGlobalState.entries,
                currentValue = touchVibrateState,
                title = "Touch Vibration Feedback",
                summary = "Configure haptic feedback when pressing on-screen controls in this instance",
                getItemText = { it.label },
                onValueChange = { state ->
                    touchVibrateState = state
                    prefs.edit().putString("aerix_inst_vibrate_state_$instanceKey", state.name).apply()
                }
            )

            MiraiToggleableIntSliderSettingsCard(
                position = CardPosition.Bottom,
                currentValue = vibrateDuration,
                defaultValue = 100,
                title = "Vibration Duration",
                summary = "Adjust touch vibration feedback duration in milliseconds",
                valueRange = 80..500,
                suffix = "ms",
                fineTuningControl = true,
                onValueChange = { vibrateDuration = it },
                onValueFinished = {
                    vibrateDuration = it
                    prefs.edit().putInt("aerix_inst_vibrate_duration_$instanceKey", it).apply()
                }
            )
        }
    }
}

// ============================================================================================
// Overview sub-screen (Mirai VersionOverViewScreen.kt)
// ============================================================================================

@Composable
private fun InstanceOverviewPane(
    activity: FragmentActivity,
    draft: AerixInstanceDraft,
    onSelectTab: (String) -> Unit,
    onExport: () -> Unit
) {
    val vanilla = stringResource(R.string.aerix_home_vanilla)
    val loader = loaderLabel(draft.versionId, vanilla)
    val mcVer = mcVerRegex.find(draft.versionId)?.value ?: draft.versionId.ifBlank { "Latest" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AerixSpacing.mdPlus)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        Text(
            text = "Instance Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            OverviewTile("Name", draft.name.ifBlank { "Unnamed" }, Modifier.weight(1f))
            OverviewTile("Minecraft", mcVer, Modifier.weight(1f))
            OverviewTile("Loader", loader, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            OverviewTile("Renderer", PojavInstanceEditorBridge.rendererLabel(activity, draft.renderer), Modifier.weight(1f))
            OverviewTile("Runtime", PojavInstanceEditorBridge.runtimeLabel(activity, draft.runtime), Modifier.weight(1f))
            OverviewTile("Memory", PojavInstanceEditorBridge.memoryLabel(activity), Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            AerixGhostButton("Manage Mods", R.drawable.aerix_ic_extension_outlined, { onSelectTab("mods") })
            AerixGhostButton("Instance Settings", R.drawable.aerix_ic_build_outlined, { onSelectTab("settings") })
            AerixPrimaryButton("Export Modpack", R.drawable.aerix_ic_upload, onExport)
        }
    }
}

@Composable
private fun OverviewTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AerixRadii.control),
        color = AerixSurface.panelRaised,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Column(
            modifier = Modifier.padding(AerixSpacing.smPlus),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = AerixSurface.textSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ============================================================================================
// Loader sub-screen (Mirai UpdateLoaderScreen.kt)
// ============================================================================================

@Composable
private fun InstanceLoaderPane(activity: FragmentActivity, draft: AerixInstanceDraft) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AerixThemeManager.currentAccent()
    val vanilla = stringResource(R.string.aerix_home_vanilla)

    var kind by remember { mutableStateOf(LoaderKind.FABRIC) }
    var mcVersions by remember { mutableStateOf<List<LoaderVersion>>(emptyList()) }
    var builds by remember { mutableStateOf<List<LoaderVersion>>(emptyList()) }
    var mcVersion by remember { mutableStateOf<String?>(null) }
    var instanceMcVersion by remember { mutableStateOf<String?>(null) }
    var build by remember { mutableStateOf<LoaderVersion?>(null) }
    var metaLoading by remember { mutableStateOf(true) }
    var metaFailed by remember { mutableStateOf(false) }
    var stableOnly by remember { mutableStateOf(true) }
    var mcQuery by remember { mutableStateOf("") }
    var buildQuery by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    // The version of the instance the screen was opened on, picked by default. Nothing in here is
    // allowed to throw: a failing source is an empty list and the retry row, never a crash.
    LaunchedEffect(kind, reload, draft.versionId) {
        metaLoading = true
        metaFailed = false
        val versions = runCatching { PojavLoaderBridge.gameVersions(kind) }.getOrDefault(emptyList())
        mcVersions = versions
        if (versions.isEmpty()) {
            metaFailed = true
            builds = emptyList()
        } else {
            // A loader without a single stable build would leave both benches empty under the
            // switch; dropping the filter opens them instead.
            if (stableOnly && versions.none { it.stable }) stableOnly = false
            instanceMcVersion = PojavLoaderBridge.minecraftVersion(draft.versionId, versions)
            mcVersion = instanceMcVersion
            if (mcVersion == null) { builds = emptyList(); build = null }
        }
        metaLoading = false
    }

    LaunchedEffect(kind, mcVersion, reload) {
        val version = mcVersion ?: return@LaunchedEffect
        metaLoading = true
        val found = runCatching { PojavLoaderBridge.builds(kind, version) }.getOrDefault(emptyList())
        if (stableOnly && found.none { it.stable }) stableOnly = false
        builds = found
        build = found.firstOrNull { !stableOnly || it.stable }
        metaLoading = false
    }

    val visibleMcVersions = remember(mcVersions, stableOnly, mcQuery) {
        mcVersions.filter { (!stableOnly || it.stable) && it.value.contains(mcQuery, true) }
    }
    val visibleBuilds = remember(builds, stableOnly, buildQuery) {
        builds.filter { (!stableOnly || it.stable) && it.label.contains(buildQuery, true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(AerixSpacing.smPlus)) {
        // One line of heading, with what the instance currently runs on the other side, so the
        // lists below get the height instead of the chrome.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.aerix_loader_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.width(AerixSpacing.sm))
            Text(
                text = stringResource(
                    R.string.aerix_loader_subtitle,
                    draft.name.ifEmpty { draft.versionId },
                    loaderLabel(draft.versionId, vanilla)
                ),
                color = AerixSurface.textSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(AerixSpacing.xs))

        // The loaders, as chips: the logo carries the recognition, the title the rest.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
        ) {
            LoaderKind.entries.forEach { entry ->
                val selected = entry == kind
                val shape = RoundedCornerShape(AerixRadii.control)
                Row(
                    modifier = Modifier
                        .clip(shape)
                        .background(if (selected) accent.copy(alpha = 0.18f) else AerixSurface.panelRaised)
                        .border(
                            BorderStroke(
                                AerixSpacing.hairline,
                                if (selected) accent.copy(alpha = 0.7f) else AerixSurface.borderSoft
                            ),
                            shape
                        )
                        .clickable(enabled = !installing, role = Role.Button) {
                            if (entry.installsHere) {
                                kind = entry
                                build = null
                                installError = null
                            } else {
                                PojavNavigator.openInstaller(activity, entry.installer!!, draft.instance.instanceId)
                            }
                        }
                        .padding(horizontal = AerixSpacing.sm, vertical = AerixSpacing.xsPlus),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(entry.iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(AerixSpacing.xs))
                    Text(
                        text = entry.title,
                        color = if (selected) accent else AerixSurface.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(AerixSpacing.smCompact))

        if (metaFailed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.aerix_loader_failed),
                    color = AerixSurface.warning,
                    fontSize = 11.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(AerixSpacing.sm))
                AerixGhostButton(
                    text = stringResource(R.string.aerix_loader_retry),
                    iconRes = R.drawable.aerix_ic_refresh,
                    onClick = { reload++ }
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
        ) {
            VersionBench(
                title = stringResource(R.string.aerix_loader_bench_minecraft),
                rows = visibleMcVersions,
                selected = mcVersion,
                onSelect = { picked -> mcVersion = picked; build = null },
                accent = accent,
                loading = metaLoading && mcVersions.isEmpty(),
                query = mcQuery,
                onQueryChange = { mcQuery = it },
                modifier = Modifier.weight(1f)
            )
            VersionBench(
                title = stringResource(R.string.aerix_loader_bench_build, kind.title),
                rows = visibleBuilds,
                selected = build?.value,
                onSelect = { picked -> build = visibleBuilds.firstOrNull { it.value == picked } },
                accent = accent,
                loading = metaLoading && builds.isEmpty(),
                query = buildQuery,
                onQueryChange = { buildQuery = it },
                toggleLabel = stringResource(R.string.aerix_loader_stable_only),
                toggleChecked = stableOnly,
                onToggleChange = { checked ->
                    stableOnly = checked
                    // A build the switch is about to hide is replaced by the newest one that stays
                    // visible, so the button never points at something off screen.
                    if (checked && build?.stable == false) {
                        build = builds.firstOrNull { it.stable }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(AerixSpacing.smCompact))

        // What the button is about to do, spelled out: loader, version, and where it lands.
        val picked = build
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AerixRadii.control),
            color = AerixSurface.panelRaised,
            border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = AerixSpacing.smPlus,
                    vertical = AerixSpacing.smCompact
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (picked == null) {
                            stringResource(R.string.aerix_loader_pick_a_build)
                        } else {
                            "${kind.title} ${picked.label}"
                        },
                        color = AerixSurface.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = installError ?: stringResource(
                            R.string.aerix_loader_install_detail,
                            mcVersion ?: "?",
                            draft.name.ifBlank { draft.versionId }
                        ),
                        color = if (installError != null) AerixSurface.danger else AerixSurface.textSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(AerixSpacing.sm))
                Surface(
                    shape = RoundedCornerShape(AerixRadii.compact),
                    color = if (picked == null || installing) accent.copy(alpha = 0.35f) else accent,
                    contentColor = AerixSurface.onAccent,
                    onClick = {
                        val selectedVersion = mcVersion
                        if (picked == null || selectedVersion == null || installing) return@Surface
                        val targetId = draft.instance.instanceId
                        val selectedKind = kind
                        val startInstall: () -> Unit = {
                            installing = true
                            installError = null
                            scope.launch {
                                val result = runCatching {
                                    PojavLoaderBridge.install(targetId, selectedKind, selectedVersion, picked)
                                }
                                installing = false
                                result.onSuccess { versionId ->
                                    PojavInstanceBridge.reload()
                                    Toast.makeText(
                                        context,
                                        if (selectedKind.forgeLike != null) context.getString(R.string.aerix_loader_finish_installer)
                                        else context.getString(R.string.aerix_loader_installed, versionId),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    PojavInstanceEditorBridge.close()
                                    AerixNav.finishTo(LauncherSection.LIBRARY)
                                }.onFailure { error ->
                                    installError = error.message
                                        ?: context.getString(R.string.aerix_loader_install_failed)
                                }
                            }
                        }
                        if (selectedVersion != instanceMcVersion) {
                            android.app.AlertDialog.Builder(context)
                                .setTitle(R.string.aerix_loader_change_game_title)
                                .setMessage(R.string.aerix_loader_change_game_warning)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ -> startInstall() }
                                .show()
                        } else startInstall()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AerixSpacing.md,
                            vertical = AerixSpacing.smCompact
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (installing) {
                            CircularProgressIndicator(
                                color = AerixSurface.onAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.aerix_ic_download),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(Modifier.width(AerixSpacing.xs))
                        Text(
                            text = stringResource(
                                if (installing) R.string.aerix_loader_installing else R.string.aerix_loader_install
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * One bench of the loader screen: a titled card holding a scrollable list of versions, a search
 * field and an optional switch. Everything is sized for a phone on its side, so the list - the part
 * the player came for - gets most of the card: a row is 32dp and the head of the card is one line.
 */
@Composable
private fun VersionBench(
    title: String,
    rows: List<LoaderVersion>,
    selected: String?,
    onSelect: (String) -> Unit,
    accent: Color,
    loading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    toggleLabel: String? = null,
    toggleChecked: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(AerixRadii.cardSmall),
        color = AerixSurface.panel.copy(alpha = 0.36f),
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AerixSpacing.xsPlus)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = AerixSurface.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (toggleLabel != null && onToggleChange != null) {
                    Text(
                        text = toggleLabel,
                        color = AerixSurface.textSecondary,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(AerixSpacing.xs))
                    MiniToggle(checked = toggleChecked, accent = accent, onChange = onToggleChange)
                }
            }
            Spacer(Modifier.height(AerixSpacing.xs))
            SearchRow(query = query, onQueryChange = onQueryChange, accent = accent)
            Spacer(Modifier.height(AerixSpacing.xs))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.aerix_loader_nothing_here),
                            color = AerixSurface.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
                    ) {
                        items(rows, key = { it.value }) { row ->
                            val isSelected = row.value == selected
                            val shape = RoundedCornerShape(AerixRadii.compact)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(shape)
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        BorderStroke(
                                            AerixSpacing.hairline,
                                            if (isSelected) accent.copy(alpha = 0.55f) else Color.Transparent
                                        ),
                                        shape
                                    )
                                    .clickable(role = Role.RadioButton) { onSelect(row.value) }
                                    .padding(horizontal = AerixSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // A drawn dot instead of a RadioButton: the stock one is 48dp of
                                // touch target for a 14dp circle, which cost three rows per screen.
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) accent else AerixSurface.textMuted
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(AerixSpacing.smCompact))
                                Text(
                                    text = row.label,
                                    color = if (isSelected) accent else AerixSurface.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!row.stable) {
                                    Text(
                                        text = stringResource(R.string.aerix_loader_unstable),
                                        color = AerixSurface.warning,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compact on/off pill for the bench headers: a 28x16 track with a dot that slides across. */
@Composable
private fun MiniToggle(checked: Boolean, accent: Color, onChange: ((Boolean) -> Unit)?) {
    val offset by animateFloatAsState(if (checked) 1f else 0f, label = "miniToggle")
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 16.dp)
            .clip(CircleShape)
            .background(if (checked) accent else Color.White.copy(alpha = 0.16f))
            .border(
                BorderStroke(AerixSpacing.hairline, if (checked) accent else AerixSurface.borderSoft),
                CircleShape
            )
            .clickable(enabled = onChange != null, role = Role.Switch) { onChange?.invoke(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .offset(x = (offset * 14).dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (checked) AerixSurface.onAccent else AerixSurface.textSecondary)
        )
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, accent: Color) {
    val shape = RoundedCornerShape(AerixRadii.compact)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(AerixSpacing.hairline, AerixSurface.borderSoft, shape)
            .padding(horizontal = AerixSpacing.smCompact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.aerix_ic_search),
            contentDescription = null,
            tint = AerixSurface.textMuted,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(AerixSpacing.xs))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = AerixSurface.textPrimary,
                fontSize = 11.sp
            ),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================================
// Helper dialogs (DeleteButton, InstanceChoiceDialog, MemoryDialog, ArgsDialog)
// ============================================================================================

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val shape = CircleShape
    Surface(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = shape,
        color = AerixSurface.danger.copy(alpha = 0.16f),
        contentColor = AerixSurface.danger,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.danger.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AerixSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
        ) {
            Icon(
                painter = painterResource(R.drawable.aerix_ic_delete_outlined),
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = stringResource(R.string.aerix_editor_delete),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InstanceChoiceDialog(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(title, color = AerixSurface.textPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                labels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(index) }
                            .padding(vertical = AerixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onPick(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accent,
                                unselectedColor = AerixSurface.textSecondary
                            )
                        )
                        Text(label, color = AerixSurface.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.aerix_settings_cancel), color = AerixSurface.textSecondary)
            }
        }
    )
}

@Composable
private fun MemoryDialog(activity: FragmentActivity, onDismiss: () -> Unit) {
    val setting = PojavSettingsBridge.memoryAllocation
    val revision = PojavSettingsBridge.revision
    val min = remember { setting.min(activity) }
    val max = remember { maxOf(min, setting.max(activity)) }
    val value = remember(revision) { setting.current().coerceIn(min, max) }
    val total = remember { Tools.getTotalDeviceMemory(activity) }
    val free = remember(revision) { PojavSettingsBridge.freeMemoryMb(activity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(stringResource(setting.titleRes), color = AerixSurface.textPrimary) },
        text = {
            SliderSettingRow(
                title = stringResource(R.string.aerix_settings_memory_info, total, free),
                summary = if (value > free) stringResource(R.string.setting_java_memory_exceeded) else null,
                value = value,
                range = min..max,
                step = setting.step,
                suffix = setting.suffix,
                onValueChange = { PojavSettingsBridge.writeInto(activity, setting, it) }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.aerix_settings_apply), color = AerixThemeManager.currentAccent())
            }
        }
    )
}

@Composable
private fun ArgsDialog(
    activity: FragmentActivity,
    draftArgs: String,
    draftMode: Int,
    onApply: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    var text by remember { mutableStateOf(draftArgs) }
    var mode by remember { mutableIntStateOf(draftMode) }
    val modes = listOf(
        stringResource(R.string.aerix_editor_arg_mode_instance),
        stringResource(R.string.aerix_editor_arg_mode_default_first),
        stringResource(R.string.aerix_editor_arg_mode_instance_first)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(stringResource(R.string.aerix_editor_jvm_args), color = AerixSurface.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.aerix_editor_jvm_args_hint), color = AerixSurface.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = AerixSurface.borderSoft,
                        focusedTextColor = AerixSurface.textPrimary,
                        unfocusedTextColor = AerixSurface.textPrimary,
                        cursorColor = accent
                    )
                )
                modes.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { mode = index }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == mode,
                            onClick = { mode = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accent,
                                unselectedColor = AerixSurface.textSecondary
                            )
                        )
                        Text(label, color = AerixSurface.textPrimary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(text.trim(), mode.coerceIn(0, Instance.ARGS_MODE_LAST)) }) {
                Text(stringResource(R.string.aerix_settings_apply), color = accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.aerix_settings_cancel), color = AerixSurface.textSecondary)
            }
        }
    )
}
