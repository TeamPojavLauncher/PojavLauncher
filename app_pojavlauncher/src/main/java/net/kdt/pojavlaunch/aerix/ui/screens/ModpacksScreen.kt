/*
 * Aerix Launcher UI layer for PojavLauncher.
 * "Our modpacks": the Team Aerix packs, each installable with a single press.
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

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpack
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpackCatalog
import net.kdt.pojavlaunch.aerix.modpacks.AerixModpackInstaller
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.aerix.ui.LauncherSection

@Composable
internal fun ModpacksScreen(navigate: (LauncherSection) -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val scope = rememberCoroutineScope()

    // Reading the catalogue on first show, and again only when the user asks for a retry.
    LaunchedEffect(Unit) { AerixModpackCatalog.load() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxWidth(0.94f).fillMaxHeight().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
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
                        "Packs the team builds and plays on. One press downloads the pack and sets up " +
                            "its own instance - Minecraft version, mod loader and mods included.",
                        color = AerixSurface.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when {
                AerixModpackCatalog.isLoading && AerixModpackCatalog.packs.isEmpty() -> item {
                    LoadingCard()
                }
                AerixModpackCatalog.error != null && AerixModpackCatalog.packs.isEmpty() -> item {
                    CatalogueErrorCard(
                        message = AerixModpackCatalog.error ?: "",
                        onRetry = { scope.launch { AerixModpackCatalog.load(force = true) } }
                    )
                }
                AerixModpackCatalog.packs.isEmpty() -> item {
                    Text(
                        "No modpacks have been published yet. Check back soon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AerixSurface.textSecondary
                    )
                }
                else -> items(AerixModpackCatalog.packs, key = { it.id }) { pack ->
                    ModpackCard(pack, navigate)
                }
            }

            item {
                Text(
                    "Packs install into their own instance and never touch your other instances. " +
                        "You can delete one from the library at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AerixSurface.textSecondary
                )
            }
        }
    }
}

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

@Composable
private fun ModpackCard(pack: AerixModpack, navigate: (LauncherSection) -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Only the pack being installed shows progress; every other card stays pressable-looking but
    // disabled, because two installs writing an instance at once is not a state worth supporting.
    val installingThis = AerixModpackInstaller.isInstalling &&
        AerixModpackInstaller.installingName == pack.name
    val liveProgress = if (installingThis) AerixModpackInstaller.progressFraction else 0f

    val versionLine = listOfNotNull(
        pack.mcVersion.takeIf { it.isNotBlank() }?.let { "Minecraft $it" },
        pack.loader.takeIf { it.isNotBlank() }?.let { it.replaceFirstChar { c -> c.uppercase() } }
    ).joinToString(" · ")

    Surface(shape = RoundedCornerShape(22.dp), color = AerixSurface.panel) {
        Column(
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.13f), AerixSurface.panel))
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BoxWithConstraints {
                val artwork: @Composable () -> Unit = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_archive_filled),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(60.dp).padding(16.dp)
                        )
                    }
                }
                val heading: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            pack.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = AerixSurface.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (versionLine.isNotEmpty()) {
                            Text(
                                versionLine,
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                    }
                }
                if (maxWidth < 360.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { artwork(); heading() }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        artwork()
                        Box(Modifier.weight(1f)) { heading() }
                    }
                }
            }

            if (pack.summary.isNotBlank()) {
                Text(
                    pack.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AerixSurface.textSecondary
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

            HorizontalDivider(color = AerixSurface.borderSoft)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // One press: download and install. Failure lands on the card, as a message.
                        scope.launch { AerixModpackInstaller.install(context, pack) }
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
                if (pack.sizeBytes > 0) {
                    Text(
                        formatSize(pack.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = AerixSurface.textSecondary
                    )
                }
            }

            // Once a pack has been installed, jump straight into it: the whole point of one press
            // is that the next step is playing, not hunting the new instance down in the library.
            if (!AerixModpackInstaller.isInstalling &&
                AerixModpackInstaller.installedInstance?.name == pack.name
            ) {
                TextButton(onClick = { navigate(LauncherSection.LIBRARY) }) {
                    Text("Installed - open your library", color = accent)
                }
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
