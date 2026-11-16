/*
 * Aerix Launcher UI layer for PojavLauncher.
 * "Our modpacks": the Team Aerix packs, each installable with a single press.
 *
 * The tab is two screens in one file, because they are two halves of one journey:
 *   * the folder grid - what packs exist, drawn as folders rather than as rows;
 *   * a pack's own page - its artwork, what it contains, and the build to install.
 *
 * The version choice lives on the pack page rather than on the folder, because a folder is a
 * name and a pack page is a decision: the same pack is normally built for more than one Minecraft
 * version, and installing the wrong build is the failure worth designing out.
 *
 * The catalogue is read over the network (see AerixModpackCatalog) so a pack never has to be
 * shipped inside the APK. When that read fails the page says so and offers to retry instead of
 * showing an empty list, because an empty catalogue is indistinguishable from a broken one.
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

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpack
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpackCatalog
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpackInstaller
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpackVersion
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.aerix.ui.LauncherSection
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.ByteArrayOutputStream

@Composable
internal fun ModpacksScreen(navigate: (LauncherSection) -> Unit) {
    // Null shows the folder grid; a pack shows that pack's page. Keeping the choice in a plain
    // state rather than wiring a second rail destination means Back is a single assignment.
    var openPack by remember { mutableStateOf<AerixModpack?>(null) }

    LaunchedEffect(Unit) { AerixModpackCatalog.load() }

    val current = openPack
    if (current != null) {
        ModpackPage(pack = current, onBack = { openPack = null }, navigate = navigate)
    } else {
        ModpackFolders(onOpen = { openPack = it })
    }
}

/** The folder grid: one tile per pack, plus room reserved for the packs that are still coming. */
@Composable
private fun ModpackFolders(onOpen: (AerixModpack) -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 900.dp).fillMaxWidth(0.94f).fillMaxHeight()) {
            val columns = when {
                maxWidth >= 720.dp -> 3
                maxWidth >= 380.dp -> 2
                else -> 1
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "FROM TEAM AERIX",
                            color = accent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Our modpacks",
                            color = AerixSurface.textPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Packs the team builds and plays on. Open one to pick its Minecraft " +
                                "version, then download it - the instance, loader and mods are set " +
                                "up for you.",
                            color = AerixSurface.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                when {
                    AerixModpackCatalog.isLoading && AerixModpackCatalog.packs.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) { LoadingCard() }

                    AerixModpackCatalog.error != null && AerixModpackCatalog.packs.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogueErrorCard(
                                message = AerixModpackCatalog.error ?: "",
                                onRetry = { scope.launch { AerixModpackCatalog.load(force = true) } }
                            )
                        }

                    AerixModpackCatalog.packs.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "No modpacks have been published yet. Check back soon.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AerixSurface.textSecondary
                            )
                        }

                    else -> {
                        items(AerixModpackCatalog.packs, key = { it.id }) { pack ->
                            ModpackFolder(pack, onClick = { onOpen(pack) })
                        }
                        // Reserved space for the next packs, so the grid reads as a shelf that is
                        // still being filled rather than as a list that happens to be short.
                        item { ComingSoonFolder() }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Packs install into their own instance and never touch your other " +
                            "instances. You can delete one from the library at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AerixSurface.textSecondary
                    )
                }
            }
        }
    }
}

/** One pack, drawn as a folder: the artwork the pack ships, its name, and what runs it. */
@Composable
private fun ModpackFolder(pack: AerixModpack, onClick: () -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val artwork = rememberPackArtwork(pack)
    val versions = pack.offeredVersions
    val versionLine = when {
        versions.size > 1 -> "${versions.size} versions"
        else -> listOfNotNull(
            versions.firstOrNull()?.mcVersion?.takeIf { it.isNotBlank() }?.let { "Minecraft $it" },
            pack.loader.takeIf { it.isNotBlank() }?.let { it.replaceFirstChar { c -> c.uppercase() } }
        ).joinToString(" · ")
    }
    val installingThis = AerixModpackInstaller.isInstalling &&
        AerixModpackInstaller.installingName == pack.name

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AerixSurface.panel,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.14f), AerixSurface.panel)))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box {
                AerixImageWell(
                    image = artwork,
                    fallback = R.drawable.aerix_ic_folder_filled,
                    size = 60.dp,
                    radius = 16.dp
                )
                // A folder tab on top of the artwork: the tile should read as something that
                // opens, not as a wall of cards.
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_folder_filled),
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.BottomEnd).size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    pack.name,
                    color = AerixSurface.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (versionLine.isNotEmpty()) {
                    Text(versionLine, color = accent, fontSize = 11.sp, maxLines = 1)
                }
                if (installingThis) {
                    Text("Installing…", color = AerixSurface.textSecondary, fontSize = 11.sp)
                } else {
                    val bytes = versions.firstOrNull { it.sizeBytes > 0 }?.sizeBytes ?: 0L
                    Text(
                        if (bytes > 0) "${versions.size} pack${if (versions.size == 1) "" else "s"} · ${formatSize(bytes)}"
                        else "Open to install",
                        color = AerixSurface.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/** The reserved tile: space held for the packs that are still being built. */
@Composable
private fun ComingSoonFolder() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AerixSurface.panel.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_package_2_outlined),
                    contentDescription = null,
                    tint = AerixSurface.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "More packs coming",
                    color = AerixSurface.textSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "The next Team Aerix pack lands here.",
                    color = AerixSurface.textSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * A pack's own page: artwork, what it is, and the build to install.
 *
 * The version list is the point of this screen. Every build the catalogue offers is shown, the
 * first one is preselected, and the button names the choice, so pressing it is never a guess.
 */
@Composable
private fun ModpackPage(
    pack: AerixModpack,
    onBack: () -> Unit,
    navigate: (LauncherSection) -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val artwork = rememberPackArtwork(pack)
    val versions = pack.offeredVersions

    var selectedFile by remember(pack.id) { mutableStateOf(versions.first().file) }
    val selected = versions.firstOrNull { it.file == selectedFile } ?: versions.first()

    val installingThis = AerixModpackInstaller.isInstalling &&
        AerixModpackInstaller.installingName == pack.name
    val liveProgress = if (installingThis) AerixModpackInstaller.progressFraction else 0f
    val installedThis = !AerixModpackInstaller.isInstalling &&
        AerixModpackInstaller.installedInstance?.name == pack.name

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight()
                .padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("‹  Our modpacks", color = accent, fontWeight = FontWeight.SemiBold)
                }
            }

            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero: the pack's own artwork, large, because that is what the folder promised.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 400.dp
                    val header: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                pack.name,
                                color = AerixSurface.textPrimary,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (pack.summary.isNotBlank()) {
                                Text(
                                    pack.summary,
                                    color = AerixSurface.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AerixImageWell(artwork, R.drawable.aerix_ic_folder_filled, 76.dp, radius = 18.dp)
                            header()
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AerixImageWell(artwork, R.drawable.aerix_ic_folder_filled, 92.dp, radius = 20.dp)
                            Box(Modifier.weight(1f)) { header() }
                        }
                    }
                }

                HorizontalDivider(color = AerixSurface.borderSoft)

                Text(
                    if (versions.size > 1) "Choose a Minecraft version" else "What you get",
                    color = AerixSurface.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                versions.forEach { version ->
                    VersionRow(
                        version = version,
                        selected = version.file == selected.file,
                        enabled = !AerixModpackInstaller.isInstalling,
                        onSelect = { selectedFile = version.file }
                    )
                }

                if (installingThis) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { liveProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = accent,
                            trackColor = AerixSurface.panelTrack
                        )
                        Text(
                            AerixModpackInstaller.statusHeadline.ifBlank { "Installing…" },
                            style = MaterialTheme.typography.bodySmall,
                            color = AerixSurface.textSecondary
                        )
                    }
                }

                if (installingThis && AerixModpackInstaller.lastError != null) {
                    Text(
                        AerixModpackInstaller.lastError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AerixSurface.danger
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // One press: download the chosen build and install it.
                            scope.launch { AerixModpackInstaller.install(context, pack, selected) }
                        },
                        enabled = !AerixModpackInstaller.isInstalling,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = AerixSurface.onAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        if (installingThis) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AerixSurface.onAccent
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Installing…")
                        } else {
                            Text("Download & play")
                        }
                    }
                    if (selected.sizeBytes > 0) {
                        Text(
                            formatSize(selected.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = AerixSurface.textSecondary
                        )
                    }
                }

                if (installedThis) {
                    TextButton(onClick = { navigate(LauncherSection.LIBRARY) }) {
                        Text("Installed - open your library", color = accent)
                    }
                }

            Text(
                    "The pack installs into its own instance, so it cannot collide with " +
                        "another pack's mods. Deleting the instance from the library removes it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AerixSurface.textSecondary
                )
            }
        }
    }
}

/** One selectable build: the Minecraft version, the loader, the size, and any note about it. */
@Composable
private fun VersionRow(
    version: AerixModpackVersion,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    val title = buildString {
        append(if (version.mcVersion.isNotBlank()) "Minecraft ${version.mcVersion}" else "Latest build")
        if (version.loader.isNotBlank()) {
            append(" · ")
            append(version.loader.replaceFirstChar { it.uppercase() })
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else AerixSurface.panel,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A filled ring for the chosen build; the same affordance as a radio button without
            // borrowing the widget's own colours, which fight the pack artwork behind it.
            Box(
                Modifier
                    .size(18.dp)
                    .background(
                        if (selected) accent else AerixSurface.panelTrack,
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        Modifier.size(7.dp).background(AerixSurface.onAccent, RoundedCornerShape(4.dp))
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = AerixSurface.textPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (version.note.isNotBlank()) {
                    Text(version.note, color = AerixSurface.textSecondary, fontSize = 11.sp)
                }
            }
            if (version.sizeBytes > 0) {
                Text(
                    formatSize(version.sizeBytes),
                    color = AerixSurface.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Decoded pack artwork, keyed by URL; sized in kilobytes like the Discover cache. */
private val packArtworkCache = object : LruCache<String, ImageBitmap>(8 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        (value.width * value.height * 4 / 1024).coerceAtLeast(1)
}

/**
 * The pack's logo, downloaded from the first mirror that answers and kept between visits.
 *
 * Artwork is optional in the catalogue, so a pack without one simply keeps the folder mark - no
 * empty box, no placeholder frame.
 */
@Composable
private fun rememberPackArtwork(pack: AerixModpack): ImageBitmap? {
    val urls = remember(pack.id) { pack.iconUrls }
    val state = produceState(initialValue = urls.firstNotNullOfOrNull { packArtworkCache.get(it) }, urls) {
        if (value == null && urls.isNotEmpty()) value = downloadPackArtwork(urls)
    }
    return state.value
}

/** Fetches the first mirror that answers and decodes it; null when none do. */
private suspend fun downloadPackArtwork(urls: List<String>): ImageBitmap? {
    for (url in urls) {
        val bitmap = withContext(Dispatchers.IO) { decodeImage(url) }
        if (bitmap != null) {
            packArtworkCache.put(url, bitmap)
            return bitmap
        }
    }
    return null
}

private fun decodeImage(url: String): ImageBitmap? = runCatching {
    val bytes = ByteArrayOutputStream().also { DownloadUtils.download(url, it) }.toByteArray()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    // The tile draws at 60-92dp; a phone at 3x wants ~276px, and the mirror holds a 256px image,
    // so it is decoded whole rather than sampled down to mush.
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 256) sample *= 2
    BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample }
    )?.asImageBitmap()
}.getOrNull()

@Composable
private fun LoadingCard() {
    Surface(shape = RoundedCornerShape(22.dp), color = AerixSurface.panel) {
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Text(
                "Reading the modpack catalogue…",
                style = MaterialTheme.typography.bodyMedium,
                color = AerixSurface.textSecondary
            )
        }
    }
}

@Composable
private fun CatalogueErrorCard(message: String, onRetry: () -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    Surface(shape = RoundedCornerShape(22.dp), color = AerixSurface.panel) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_archive_filled),
                    contentDescription = null,
                    tint = AerixSurface.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "The modpack list could not be loaded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AerixSurface.textPrimary
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = AerixSurface.textSecondary
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = AerixSurface.onAccent
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Try again")
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
