package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.components.GlassPanel
import net.kdt.pojavlaunch.aerix.components.PageHeader
import net.kdt.pojavlaunch.aerix.fastclient.FastClientInstaller
import net.kdt.pojavlaunch.aerix.theme.AerixLayoutDefaults
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.instances.Instances

/** The creator's builds are separate profiles and never receive Aerix Client's own mod stack. */
@Composable
fun FastClientScreen(activity: FragmentActivity, onBack: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var searchPacks by remember { mutableStateOf(false) }
    val instances = remember(refresh, FastClientInstaller.installingVersion) {
        runCatching { Instances.loadAllInstances() }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().padding(AerixLayoutDefaults.pagePadding()), verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 560.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(0.9f)) { FastClientTitle() }
                    FastClientThanks(activity, Modifier.weight(2f))
                    ClientOwnerBackButton(onBack)
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    PageHeader(title = "FastClient", subtitle = "By Mxinspire1 · Fabric", trailing = { ClientOwnerBackButton(onBack) })
                    FastClientThanks(activity, Modifier.fillMaxWidth())
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
            Text("Install a separate FastClient instance. The client, Fabric Loader, Fabric API, Lithium and Entity Culling are downloaded during setup, so nothing is bundled in the app; Minecraft files download when you launch.", color = AerixSurface.textSecondary, fontSize = 12.sp)
            // The same pack search the Aerix Client page has, filtered by the versions FastClient is
            // actually built for. A pack keeps its own mods here: FastClient is an instance of its own,
            // not a mod that can be dropped into somebody else's.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                Button(onClick = { searchPacks = true }) { Text("Search modpacks") }
                Text("Pick a pack, then pick which Minecraft version of it to take.", color = AerixSurface.textSecondary, fontSize = 11.sp)
            }
            if (FastClientInstaller.installingVersion != null) {
                GlassPanel(Modifier.fillMaxWidth(), radius = AerixRadii.card) {
                    Text(FastClientInstaller.status, color = Color.White, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { FastClientInstaller.progress }, modifier = Modifier.fillMaxWidth(), color = AerixSurface.accent)
                }
            }
            FastClientInstaller.lastError?.let { error ->
                GlassPanel(Modifier.fillMaxWidth(), radius = AerixRadii.card, color = AerixSurface.errorDarkContainer.copy(alpha = 0.55f)) {
                    Text(error, color = Color.White, fontSize = 12.sp)
                }
            }
            FastClientInstaller.builds.forEach { build ->
                val installed = FastClientInstaller.findInstalled(build, instances)
                val busy = FastClientInstaller.installingVersion != null
                GlassPanel(Modifier.fillMaxWidth(), radius = AerixRadii.card) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Minecraft ${build.minecraft}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Fabric · Java ${build.java}" + if (installed != null) " · Installed" else "", color = AerixSurface.textSecondary, fontSize = 12.sp)
                        }
                        if (installed == null) {
                            Button(enabled = !busy, onClick = {
                                scope.launch {
                                    FastClientInstaller.install(activity, build).onSuccess {
                                        Toast.makeText(activity, "FastClient ${build.minecraft} ready", Toast.LENGTH_SHORT).show()
                                    }.onFailure { error ->
                                        Toast.makeText(activity, error.message ?: "Installation failed", Toast.LENGTH_LONG).show()
                                    }
                                    refresh++
                                    PojavInstanceBridge.reload()
                                }
                            }) { Text(if (FastClientInstaller.installingVersion == build.minecraft) "Installing…" else "Install") }
                        } else {
                            Button(enabled = !busy, onClick = {
                                Instances.setSelectedInstance(installed)
                                PojavInstanceBridge.reload()
                                PojavLaunchBridge.play(activity)
                            }) { Text("Launch") }
                        }
                    }
                }
            }
            Text("Android compatibility and performance depend on the device and renderer. FastClient is provided by its creator.", color = AerixSurface.textSecondary, fontSize = 12.sp)
        }
    }

        if (searchPacks) {
            // A pack creates its own instance, so the list of FastClient installs below is stale once
            // it finishes; the reload is what makes the new one appear without leaving the page.
            AerixPackSearchDialog(activity, ClientOwnerPage.MX_INSPIRE, onDismiss = { searchPacks = false }) {
                refresh++
                PojavInstanceBridge.reload()
            }
        }
    }

@Composable
private fun FastClientTitle() {
    Text("FastClient", color = AerixSurface.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    Text("By Mxinspire1 · Fabric", color = AerixSurface.textSecondary, fontSize = 12.sp)
}

@Composable
private fun FastClientThanks(activity: FragmentActivity, modifier: Modifier = Modifier) {
    fun openLink(url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(activity, "No app available to open this link", Toast.LENGTH_SHORT).show()
        }
    }
    Column(modifier) {
        Text("Thanks Mx inspire for supporting and giving us permission to use their client",
            color = AerixSurface.textPrimary, fontSize = 12.sp, lineHeight = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { openLink("https://m.youtube.com/channel/UCjC7cPjoCxQycsw0DITzLQA") }) {
                Text("YouTube", color = AerixSurface.accent, fontSize = 12.sp)
            }
            TextButton(onClick = { openLink("https://discord.gg/Z6aCUNfPJH") }) {
                Text("Discord", color = AerixSurface.accent, fontSize = 12.sp)
            }
        }
    }
}
