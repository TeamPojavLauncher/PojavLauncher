/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/SettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/RendererSettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/GameSettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/JavaManageScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/LauncherSettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/ControlSettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/ControlManageScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/GamepadSettingsScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/AboutInfoScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/layouts/(all .kt files)
 * Top bar is Mirai's AerixPillTabRow with the 9 settings tabs (Renderer, Game, Java, Wallpapers,
 * Launcher, Controls, Controls, Gamepad, About) and no page title above it; settings rows are
 * grouped in Mirai's SettingsCardColumn (32dp outer corners, 4dp inner seams, DefaultSwitch with
 * check/close thumb icons). Every setting writes PojavLauncher preferences or AerixPreferences.
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

import net.kdt.pojavlaunch.BuildConfig
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import net.kdt.pojavlaunch.aerix.bridge.rememberControlLayoutCatalog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.ImportControlActivity
import net.kdt.pojavlaunch.NewJREUtil
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.aerix.bridge.AerixColorTheme
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.bridge.AerixTransitionType
import net.kdt.pojavlaunch.aerix.bridge.AssetPlatform
import net.kdt.pojavlaunch.aerix.bridge.PojavNavigator
import net.kdt.pojavlaunch.aerix.bridge.PojavPluginBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavSetting
import net.kdt.pojavlaunch.aerix.bridge.PojavSettingCategory
import net.kdt.pojavlaunch.aerix.bridge.PojavSettingsBridge
import net.kdt.pojavlaunch.aerix.vulkan.VulkanDriverManager
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.components.AerixPillTab
import net.kdt.pojavlaunch.aerix.components.AerixPillTabRow
import net.kdt.pojavlaunch.aerix.components.AerixPrimaryButton
import net.kdt.pojavlaunch.aerix.components.CardPosition
import net.kdt.pojavlaunch.aerix.components.DefaultSwitch
import net.kdt.pojavlaunch.aerix.components.MiraiIntSliderSettingsCard
import net.kdt.pojavlaunch.aerix.components.MiraiListSettingsCard
import net.kdt.pojavlaunch.aerix.components.SettingsCardColumn
import net.kdt.pojavlaunch.aerix.discord.DiscordPresenceSettingsCard
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.components.rememberSettingsCardShape
import net.kdt.pojavlaunch.aerix.theme.AerixFonts
import net.kdt.pojavlaunch.aerix.theme.AerixMetrics
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.aerix.ui.AerixNav
import net.kdt.pojavlaunch.aerix.ui.LauncherSection
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.game.renderer.def.Renderers
import net.kdt.pojavlaunch.mobileglues.MainActivity as MobileGluesActivity
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DisplayRefreshMode
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.tasks.DataMigrator
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException
import java.io.File
import kotlin.math.roundToInt

private data class SettingTabItem(
    val key: String,
    @DrawableRes val iconRes: Int,
    val label: String
)

private val settingTabItems = listOf(
    SettingTabItem(AerixNav.TAB_STYLES, R.drawable.aerix_ic_format_paint_outlined, "Styles"),
    SettingTabItem(AerixNav.TAB_RENDERER, R.drawable.aerix_ic_video_settings, "Renderer"),
    SettingTabItem(AerixNav.TAB_GAME, R.drawable.aerix_ic_rocket_launch_filled, "Game"),
    SettingTabItem(AerixNav.TAB_JAVA, R.drawable.aerix_ic_java, "Java"),
    SettingTabItem(AerixNav.TAB_WALLPAPERS, R.drawable.aerix_ic_format_paint_outlined, "Wallpapers"),
    SettingTabItem(AerixNav.TAB_LAUNCHER, R.drawable.aerix_ic_setting_launcher, "Launcher"),
    SettingTabItem(AerixNav.TAB_CONTROLS, R.drawable.aerix_ic_videogame_asset_outlined, "Controls"),
    SettingTabItem(AerixNav.TAB_CONTROL_MANAGER, R.drawable.aerix_ic_dashboard_outlined, "Layouts"),
    SettingTabItem(AerixNav.TAB_GAMEPAD, R.drawable.aerix_ic_sports_esports_outlined, "Gamepad"),
    SettingTabItem(AerixNav.TAB_ABOUT, R.drawable.aerix_ic_info_outlined, "About")
)


/**
 * The name of an installed runtime, corrected against the Java version inside it.
 *
 * A build used to install the Java 17 artifact under the name "Internal-8", so the Runtime
 * Manager said "Internal 8" over a 17.0.20.1 version string. The volume is the truth: when the
 * name is one of the built-in "Internal-N" slots and N disagrees with the detected major
 * version, the detected version wins. Imported runtimes keep the name their author gave them.
 */
private fun runtimeLabel(runtime: Runtime): String {
    val pretty = runtime.name.replace(".tar.xz", "").replace("-", " ")
    val match = Regex("^Internal[ -](\\d+)$").find(runtime.name.replace(".tar.xz", ""))
        ?: return pretty
    val declared = match.groupValues[1].toIntOrNull() ?: return pretty
    val actual = runtime.javaVersion
    // The volume is the truth: a slot that holds a different Java than its name claims is shown
    // under the version it really contains, and every slot reads "Java N".
    return "Java " + if (actual > 0 && actual != declared) actual else declared
}


@Composable
fun SettingsScreen(activity: FragmentActivity, tabKey: String, onTabChanged: (String) -> Unit) {
    val activeKey = if (settingTabItems.any { it.key == tabKey }) tabKey else AerixNav.TAB_RENDERER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // Every settings page keeps the tab strip. The Wallpapers page used to be reachable from a
        // rail entry of its own and hid the strip for that reason; that entry is now the modpack
        // page, so Wallpapers is a normal tab like the rest.
        AerixPillTabRow(modifier = Modifier.fillMaxWidth()) {
            settingTabItems.forEach { item ->
                val selected = activeKey == item.key
                AerixPillTab(
                    selected = selected,
                    onClick = { onTabChanged(item.key) }
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeKey) {
                AerixNav.TAB_STYLES -> FontStylesTab(activity)
                AerixNav.TAB_RENDERER -> RendererTabContent(activity)
                AerixNav.TAB_GAME -> GameTabContent(activity)
                AerixNav.TAB_JAVA -> JavaManageTabContent(activity)
                AerixNav.TAB_WALLPAPERS -> WallpapersScreen(modifier = Modifier.fillMaxSize())
                AerixNav.TAB_LAUNCHER -> LauncherTabContent(activity)
                AerixNav.TAB_CONTROLS -> ControlsTabContent(activity)
                AerixNav.TAB_CONTROL_MANAGER -> ControlManagerTabContent(activity)
                AerixNav.TAB_GAMEPAD -> GamepadTabContent(activity)
                AerixNav.TAB_ABOUT -> AboutInfoTabContent(activity)
                else -> RendererTabContent(activity)
            }
        }
    }
}


/** A live, offline preview of the same families used by the launcher's global theme. */
@Composable
private fun FontStylesTab(activity: FragmentActivity) {
    val selected = AerixFonts.selectedId
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
        contentPadding = PaddingValues(bottom = AerixSpacing.xl)
    ) {
        item {
            Text("Font styles", color = AerixSurface.textPrimary, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Choose a font for the launcher. Your selection is saved and works offline.",
                color = AerixSurface.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        items(AerixFonts.choices.size, key = { AerixFonts.choices[it].id }) { index ->
            val choice = AerixFonts.choices[index]
            val active = selected == choice.id
            val previewFamily = remember(choice.id) { choice.family() }
            Surface(
                onClick = { AerixFonts.select(activity, choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AerixRadii.cardSmall),
                color = if (active) AerixSurface.accent.copy(alpha = 0.22f) else AerixSurface.panel,
                border = BorderStroke(1.dp, if (active) AerixSurface.accent else AerixSurface.borderSoft)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(choice.label, fontFamily = previewFamily, fontSize = 16.sp,
                            color = AerixSurface.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Aerix Launcher · Aa 123", fontFamily = previewFamily, fontSize = 12.sp,
                            color = AerixSurface.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    RadioButton(selected = active, onClick = { AerixFonts.select(activity, choice) },
                        colors = RadioButtonDefaults.colors(selectedColor = AerixSurface.accent))
                }
            }
        }
    }
}

// ============================================================================================
// 1. Renderer tab (Mirai RendererSettingsScreen.kt)
// ============================================================================================

@Composable
private fun RendererTabContent(activity: FragmentActivity) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    if (showUpdateDialog) LauncherUpdateDialog(activity) { showUpdateDialog = false }

    val revision = PojavSettingsBridge.revision
    val currentRenderer = remember(revision) { LauncherPreferences.PREF_RENDERER ?: Renderers.GL4ES_RENDERER }
    // The Global Renderer list is gone from this tab: a renderer is chosen per instance, where the
    // choice belongs, and the launcher picks the one its version calls for when an instance has
    // none. What is left here is everything that applies to the device rather than to one instance.
    var globalVulkanDriver by remember(revision) {
        mutableStateOf(VulkanDriverManager.globalDriverId(activity))
    }
    val installedVulkanDrivers = remember(revision) { VulkanDriverManager.installedDrivers(activity) }
    var showVulkanDriverImportWarning by remember { mutableStateOf(false) }
    val vulkanDriverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            PojavApplication.sExecutorService.execute {
                val result = runCatching { VulkanDriverManager.importDriver(activity, uri) }
                Tools.runOnUiThread {
                    result.onSuccess { driver ->
                        LauncherPreferences.DEFAULT_PREF.edit()
                            .putString("aerix_global_vulkan_driver", driver.id)
                            .apply()
                        globalVulkanDriver = driver.id
                        PojavSettingsBridge.writeBoolean(activity, "zinkPreferSystemDriver", false)
                        Toast.makeText(activity, "Added and selected ${driver.name}.", Toast.LENGTH_LONG).show()
                    }.onFailure { error ->
                        Toast.makeText(
                            activity,
                            "Could not add Vulkan driver: ${error.message ?: "unsupported package"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    if (showVulkanDriverImportWarning) {
        AlertDialog(
            onDismissRequest = { showVulkanDriverImportWarning = false },
            title = { Text("Add Vulkan driver") },
            text = {
                Text("Choose a compatible AdrenoTools or Turnip driver APK/ZIP. Only import native drivers from sources you trust; an incompatible driver can make Minecraft crash.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showVulkanDriverImportWarning = false
                    vulkanDriverPicker.launch(arrayOf("*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { showVulkanDriverImportWarning = false }) { Text("Cancel") }
            }
        )
    }
    var globalGraphicsApi by remember(revision) {
        mutableStateOf(
            LauncherPreferences.DEFAULT_PREF.getString("aerix_global_graphics_api", "DEFAULT")
                ?: "DEFAULT"
        )
    }
    var resolutionMode by remember(revision) {
        mutableStateOf(
            LauncherPreferences.DEFAULT_PREF.getString("aerix_resolution_mode", "SCALE")
                ?: "SCALE"
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // 1. Compact Footer Updater Card (Mirai RendererSettingsScreen lines 428-469)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AerixRadii.control),
                color = AerixSurface.panel,
                border = BorderStroke(AerixSpacing.hairline, AerixSurface.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Aerix Launcher v${BuildConfig.VERSION_NAME} \u2022 Built by entitybrian",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AerixSurface.textPrimary
                    )

                    Button(
                        onClick = {
                            showUpdateDialog = true
                        },
                        shape = RoundedCornerShape(AerixRadii.card),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AerixThemeManager.currentAccent(),
                            contentColor = AerixSurface.onAccent
                        ),
                        contentPadding = PaddingValues(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.smCompact)
                    ) {
                        Text(
                            text = "Check for Updates",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 2. Layout transparency — the slot the global renderer picker used to occupy. It lives on
        // this tab because it is a device-wide look, not a per-instance renderer choice, and it is
        // the first card so the effect of dragging it is visible on the cards right below.
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSliderSettingsCard(
                    position = CardPosition.Single,
                    title = stringResource(R.string.aerix_settings_layout_transparency),
                    summary = stringResource(R.string.aerix_settings_layout_transparency_sub),
                    value = AerixPreferences.layoutTransparency,
                    range = 25..100,
                    step = 5,
                    suffix = " %",
                    onValueChange = { AerixPreferences.saveLayoutTransparency(it) }
                )
            }
        }

        // 3. Detailed Renderer, Vulkan Driver, Graphics API, Resolution & Fullscreen SettingsCardColumn (Mirai RendererSettingsScreen — Photos 1 & 2)
        item {
            // With no global picker on this tab, the MobileGlues settings only matter when the
            // renderer in effect is the one chosen on an instance.
            val showMgSettings = currentRenderer == Renderers.MOBILEGLUES_RENDERER
            val vulkanDrivers = remember(installedVulkanDrivers) {
                buildList {
                    add(VulkanDriverManager.SYSTEM_DRIVER to "System Vulkan driver")
                    add(VulkanDriverManager.TURNIP_DRIVER to "Turnip (built-in)")
                    installedVulkanDrivers.forEach { driver -> add(driver.id to driver.name) }
                }
            }
            val graphicsApis = remember {
                listOf(
                    "DEFAULT" to "Default",
                    "DEFAULT_GL" to "Default (OpenGL)",
                    "OPENGL_ES_3" to "OpenGL ES 3",
                    "VULKAN" to "Vulkan"
                )
            }
            val resolutionModes = remember {
                listOf(
                    "SCALE" to "Follow Screen Percentage",
                    "CUSTOM" to "Custom Resolution"
                )
            }

            // State of the renderer plugin APKs, right above the list that is missing them: an
            // installed plugin Android does not expose and one that was never installed look
            // identical from here otherwise.
            val pluginStatus = remember(revision) { PojavPluginBridge.pluginStatus(activity) }
            Text(
                text = "Renderer plugins: $pluginStatus",
                style = MaterialTheme.typography.labelSmall,
                color = if (pluginStatus == "all detected") AerixSurface.textSecondary else Color(0xFFFFC46B)
            )

            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                if (showMgSettings) {
                    // First in the column now that the Global Renderer list is gone, so it takes
                    // the rounded top edge and the Vulkan card below becomes a middle segment.
                    MiraiActionSettingsCard(
                        position = CardPosition.Top,
                        title = stringResource(R.string.mcl_setting_title_renderer_settings),
                        summary = stringResource(R.string.aerix_settings_renderer_settings_sub),
                        value = "Configure",
                        onClick = { activity.startActivity(Intent(activity, MobileGluesActivity::class.java)) }
                    )
                }
                MiraiListSettingsCard(
                    position = if (showMgSettings) CardPosition.Middle else CardPosition.Top,
                    items = vulkanDrivers,
                    currentId = globalVulkanDriver,
                    defaultId = VulkanDriverManager.TURNIP_DRIVER,
                    title = "Global Vulkan Driver",
                    summary = "Choose the Vulkan driver used by Zink and Vulkan mods",
                    getItemText = { it.second },
                    getItemId = { it.first },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                PojavNavigator.openUrl(
                                    activity,
                                    "https://github.com/K11MCH1/AdrenoToolsDrivers/releases"
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.aerix_ic_download_2_outlined),
                                contentDescription = "Download Vulkan Driver",
                                tint = Color.White
                            )
                        }
                    },
                    onValueChange = { item ->
                        globalVulkanDriver = item.first
                        val preferences = LauncherPreferences.DEFAULT_PREF.edit()
                        if (item.first == VulkanDriverManager.SYSTEM_DRIVER || item.first == VulkanDriverManager.TURNIP_DRIVER) {
                            preferences.remove("aerix_global_vulkan_driver")
                        } else {
                            preferences.putString("aerix_global_vulkan_driver", item.first)
                        }
                        preferences.apply()
                        PojavSettingsBridge.writeBoolean(
                            activity,
                            "zinkPreferSystemDriver",
                            item.first == VulkanDriverManager.SYSTEM_DRIVER
                        )
                    }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Middle,
                    title = "Add Vulkan Driver",
                    summary = "Import a compatible AdrenoTools or Turnip APK/ZIP, then select it above",
                    value = "Add",
                    onClick = { showVulkanDriverImportWarning = true }
                )
                MiraiListSettingsCard(
                    position = CardPosition.Middle,
                    items = graphicsApis,
                    currentId = globalGraphicsApi,
                    defaultId = "DEFAULT",
                    title = "Graphics API",
                    summary = "Specify the graphics API used by the game",
                    getItemText = { it.second },
                    getItemId = { it.first },
                    onValueChange = { item ->
                        globalGraphicsApi = item.first
                        LauncherPreferences.DEFAULT_PREF.edit()
                            .putString("aerix_global_graphics_api", item.first)
                            .apply()
                    }
                )
                MiraiListSettingsCard(
                    position = CardPosition.Middle,
                    items = resolutionModes,
                    currentId = resolutionMode,
                    defaultId = "SCALE",
                    title = "Resolution Mode",
                    summary = "Choose whether the game window resolution follows a percentage of the screen size or uses a custom resolution",
                    getItemText = { it.second },
                    getItemId = { it.first },
                    onValueChange = { item ->
                        resolutionMode = item.first
                        LauncherPreferences.DEFAULT_PREF.edit()
                            .putString("aerix_resolution_mode", item.first)
                            .apply()
                    }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_setting_title_resolution_scaler),
                    summary = stringResource(R.string.mcl_setting_subtitle_resolution_scaler),
                    value = PojavSettingsBridge.resolutionPercent(),
                    range = 25..100,
                    step = 5,
                    suffix = "%",
                    onValueChange = { PojavSettingsBridge.writeInt(activity, "resolutionRatio", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.mcl_setting_title_ignore_notch),
                    summary = stringResource(R.string.mcl_setting_subtitle_ignore_notch),
                    checked = LauncherPreferences.PREF_IGNORE_NOTCH,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "ignoreNotch", it) }
                )
            }
        }

        // 3. Switch cards group (Screenshot 4: Sustained Performance Mode, Use System Vulkan Driver, Zink Vsync, Use SurfaceView Rendering, Shader Dump)
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = "Sustained Performance Mode",
                    summary = "When enabled, the system provides more stable performance for the game, suitable for long play sessions, but may slightly reduce device responsiveness to prevent overheating",
                    checked = LauncherPreferences.PREF_SUSTAINED_PERFORMANCE,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "sustainedPerformance", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = "Use System Vulkan Driver",
                    summary = "When enabled, the launcher will force the use of the system's built-in Vulkan driver (for Adreno GPUs) instead of the default Turnip driver. This setting only affects the Zink backend and related mods that use Vulkan rendering",
                    checked = globalVulkanDriver == VulkanDriverManager.SYSTEM_DRIVER,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "zinkPreferSystemDriver", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = "Zink Vsync",
                    summary = "Enable vertical sync for the Zink renderer via the system interface",
                    checked = LauncherPreferences.PREF_FORCE_VSYNC,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "force_vsync", it) }
                )
                var maxRefreshEnabled by remember { mutableStateOf(LauncherPreferences.PREF_MAX_REFRESH_RATE) }
                // Read the panel rather than the preference: this is the line that tells the player
                // whether their screen has a faster mode to ask for at all, and what it is doing now.
                val maxRefreshCeilingHz = Math.round(DisplayRefreshMode.ceilingRefreshHz(activity)).toInt()
                val maxRefreshActiveHz = Math.round(DisplayRefreshMode.activeRefreshHz(activity)).toInt()
                val maxRefreshSummary = when {
                    maxRefreshCeilingHz <= 0 -> activity.getString(R.string.aerix_max_refresh_desc)
                    maxRefreshCeilingHz <= maxRefreshActiveHz ->
                        activity.getString(R.string.aerix_max_refresh_unavailable, maxRefreshActiveHz)
                    maxRefreshEnabled ->
                        activity.getString(R.string.aerix_max_refresh_summary, maxRefreshCeilingHz, maxRefreshActiveHz)
                    else -> activity.getString(R.string.aerix_max_refresh_desc)
                }
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = activity.getString(R.string.aerix_max_refresh_title),
                    summary = maxRefreshSummary,
                    checked = maxRefreshEnabled,
                    onCheckedChange = { isOn ->
                        maxRefreshEnabled = isOn
                        PojavSettingsBridge.writeBoolean(activity, DisplayRefreshMode.PREF_KEY, isOn)
                    }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = "Use SurfaceView Rendering",
                    summary = stringResource(R.string.mcl_setting_subtitle_use_surface_view),
                    checked = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "alternate_surface", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.preference_shader_dump_title),
                    summary = stringResource(R.string.preference_shader_dump_description),
                    checked = LauncherPreferences.PREF_DUMP_SHADERS,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "dump_shaders", it) }
                )
            }
        }
    }
}


// ============================================================================================
// 2. Game tab (Mirai GameSettingsScreen.kt: integrity checks, Java memory/args, log settings, experimental)
// ============================================================================================

@Composable
private fun GameTabContent(activity: FragmentActivity) {
    val revision = PojavSettingsBridge.revision
    val migrateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            android.app.AlertDialog.Builder(activity)
                .setTitle(R.string.migration_progress_warning_title)
                .setMessage(R.string.migration_progress_warning_summary)
                .setPositiveButton(android.R.string.ok) { _, _ -> DataMigrator(activity, uri).migrateData() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.preference_verify_game_files_title),
                    summary = stringResource(R.string.preference_verify_game_files_description),
                    checked = remember(revision) { LauncherPreferences.PREF_VERIFY_FILES },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "checkGameFiles", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_go_vroom_title),
                    summary = stringResource(R.string.preference_go_vroom_description),
                    checked = remember(revision) { LauncherPreferences.PREF_RAPID_START },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "fastStartupCheck", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_verify_manifest_title),
                    summary = stringResource(R.string.preference_verify_manifest_description),
                    checked = remember(revision) { LauncherPreferences.PREF_VERIFY_MANIFEST },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "verifyManifest", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.preference_show_memory_warning_title),
                    summary = stringResource(R.string.preference_show_memory_warning_summary),
                    checked = remember(revision) { LauncherPreferences.PREF_SHOW_MEMORY_WARNING_DIALOG },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "showMemoryWarning", it) }
                )
            }
        }

        item {
            val maxRam = remember { PojavSettingsBridge.safeMaxRam(activity) }
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSliderSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.mcl_memory_allocation),
                    summary = stringResource(R.string.mcl_memory_allocation_subtitle),
                    value = remember(revision) { LauncherPreferences.PREF_RAM_ALLOCATION.coerceIn(256, maxRam) },
                    range = 256..maxRam,
                    step = 8,
                    suffix = " MB",
                    onValueChange = { PojavSettingsBridge.writeInt(activity, "allocation", it) }
                )
                MiraiTextSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_setting_title_javaargs),
                    summary = stringResource(R.string.mcl_setting_subtitle_javaargs),
                    value = remember(revision) { LauncherPreferences.PREF_CUSTOM_JAVA_ARGS ?: "" },
                    onValueChange = { PojavSettingsBridge.writeString(activity, "javaArgs", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.mcl_setting_java_sandbox),
                    summary = stringResource(R.string.mcl_setting_java_sandbox_subtitle),
                    checked = remember(revision) { LauncherPreferences.PREF_JAVA_SANDBOX },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "java_sandbox", it) }
                )
            }
        }

        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSliderSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.aerix_logs_text_size),
                    summary = stringResource(R.string.aerix_logs_text_size_sub),
                    value = AerixPreferences.logTextSize,
                    range = 5..20,
                    step = 1,
                    suffix = " sp",
                    onValueChange = { AerixPreferences.saveLogTextSize(it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_log_flush),
                    summary = stringResource(R.string.aerix_settings_log_flush_sub),
                    value = AerixPreferences.logBufferFlushInterval,
                    range = 100..1000,
                    step = 100,
                    suffix = " ms",
                    onValueChange = { AerixPreferences.saveLogBufferFlushInterval(it) }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.aerix_logs_title),
                    summary = stringResource(R.string.aerix_logs_subtitle),
                    value = "Open",
                    onClick = { AerixNav.navigate(LauncherSection.LOGS) }
                )
            }
        }

        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.preference_force_big_core_title),
                    summary = stringResource(R.string.preference_force_big_core_desc),
                    checked = remember(revision) { LauncherPreferences.PREF_BIG_CORE_AFFINITY },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "bigCoreAffinity", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_sysmem_title),
                    summary = stringResource(R.string.preference_sysmem_summary),
                    checked = remember(revision) { LauncherPreferences.PREF_FREEDRENO_SYSMEM },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "freedrenoSysmem", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_alsoft_opensl_title),
                    summary = stringResource(R.string.preference_alsoft_opensl_summary),
                    checked = remember(revision) { LauncherPreferences.PREF_ALSOFT_FORCE_OPENSL },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "alsoftForceOpenSL", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_ubwc_title),
                    summary = stringResource(R.string.preference_ubwc_summary),
                    checked = remember(revision) { LauncherPreferences.PREF_UBWC_WORKAROUND },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "ubwcWorkaround", it) }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.preference_data_migration_title),
                    summary = stringResource(R.string.preference_data_migration_summary),
                    value = "Import",
                    onClick = {
                        if (ProgressKeeper.getTaskCount() > 0) {
                            Toast.makeText(activity, R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
                        } else {
                            migrateLauncher.launch(null)
                        }
                    }
                )
            }
        }
    }
}

// ============================================================================================
// 3. Java tab (Mirai JavaManageScreen.kt over Pojav's MultiRTUtils / NewJREUtil)
// ============================================================================================

@Composable
private fun JavaManageTabContent(activity: FragmentActivity) {
    var refreshTick by remember { mutableIntStateOf(0) }
    val installed = remember(refreshTick, PojavSettingsBridge.revision) {
        runCatching { MultiRTUtils.getRuntimes() }.getOrDefault(emptyList())
    }
    val downloadable = remember(refreshTick, PojavSettingsBridge.revision) {
        runCatching { MultiRTUtils.getRuntimesToDownload() }.getOrDefault(emptyList())
    }
    var downloadingName by remember { mutableStateOf<String?>(null) }
    val defaultName = remember(refreshTick, PojavSettingsBridge.revision) {
        LauncherPreferences.PREF_DEFAULT_RUNTIME ?: ""
    }

    val importJreLauncher = rememberLauncherForActivityResult(OpenDocumentWithExtension("xz")) { uri ->
        if (uri != null) {
            Tools.installRuntimeFromUri(activity, uri)
            Toast.makeText(activity, R.string.global_installing, Toast.LENGTH_SHORT).show()
            refreshTick++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.multirt_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.multirt_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = AerixSurface.textSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                AerixGhostButton(
                    text = "",
                    iconRes = R.drawable.aerix_ic_refresh,
                    onClick = { refreshTick++ }
                )
                AerixPrimaryButton(
                    text = "Import .tar.xz",
                    iconRes = R.drawable.aerix_ic_add,
                    onClick = { importJreLauncher.launch(null) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            items(installed.size, key = { "inst_${installed[it].name}" }) { index ->
                val runtime: Runtime = installed[index]
                val isDefault = runtime.name == defaultName
                val compatible = runtime.versionString != null &&
                    Tools.deviceArchitecture() == Architecture.archAsInt(runtime.arch)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AerixRadii.card),
                    color = AerixSurface.panel,
                    border = BorderStroke(
                        if (isDefault) 1.5.dp else AerixSpacing.hairline,
                        if (isDefault) AerixThemeManager.currentAccent() else AerixSurface.borderSoft
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.smPlus),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_java),
                            contentDescription = null,
                            tint = if (compatible) AerixThemeManager.currentAccent() else AerixSurface.danger,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = runtimeLabel(runtime),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    runtime.versionString == null -> stringResource(R.string.multirt_runtime_corrupt)
                                    !compatible -> stringResource(R.string.multirt_runtime_incompatiblearch, runtime.arch)
                                    else -> "${runtime.versionString} \u2022 ${runtime.arch}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (compatible) AerixSurface.textSecondary else AerixSurface.danger
                            )
                        }
                        if (compatible) {
                            AerixGhostButton(
                                text = stringResource(
                                    if (isDefault) R.string.multirt_config_setdefault_already
                                    else R.string.multirt_config_setdefault
                                ),
                                iconRes = if (isDefault) R.drawable.aerix_ic_check else null,
                                onClick = {
                                    if (!isDefault) {
                                        LauncherPreferences.PREF_DEFAULT_RUNTIME = runtime.name
                                        PojavSettingsBridge.writeString(activity, "defaultRuntime", runtime.name)
                                        refreshTick++
                                    }
                                }
                            )
                        }
                        if (installed.size > 1) {
                            IconButton(onClick = {
                                PojavApplication.sExecutorService.execute {
                                    runCatching { MultiRTUtils.removeRuntimeNamed(runtime.name) }
                                    Tools.runOnUiThread { refreshTick++ }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.aerix_ic_delete_outlined),
                                    contentDescription = stringResource(R.string.global_remove),
                                    tint = AerixSurface.danger
                                )
                            }
                        }
                    }
                }
            }

            items(downloadable.size, key = { "dl_${downloadable[it].name}" }) { index ->
                val ext: NewJREUtil.ExternalRuntime = downloadable[index]
                val busy = downloadingName != null || ext.isDownloading
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AerixRadii.card),
                    color = AerixSurface.panel,
                    border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.smPlus),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_download),
                            contentDescription = null,
                            tint = AerixSurface.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ext.getLabel(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.global_not_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = AerixSurface.textSecondary
                            )
                        }
                        AerixPrimaryButton(
                            text = stringResource(if (busy) R.string.global_installing else R.string.global_download),
                            iconRes = R.drawable.aerix_ic_download,
                            enabled = !busy,
                            onClick = {
                                downloadingName = ext.name
                                ext.isDownloading = true
                                PojavApplication.sExecutorService.execute {
                                    try {
                                        ext.downloadRuntime(activity.assets)
                                    } catch (e: RuntimeSelectionException) {
                                        Tools.showError(activity, e)
                                    } finally {
                                        ext.isDownloading = false
                                        Tools.runOnUiThread {
                                            downloadingName = null
                                            refreshTick++
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// 4. Launcher tab (Mirai LauncherSettingsScreen.kt)
// ============================================================================================

@Composable
private fun LauncherTabContent(activity: FragmentActivity) {
    val revision = PojavSettingsBridge.revision
    var showColorPicker by remember { mutableStateOf(false) }

    val platformLabels = listOf(
        AssetPlatform.MODRINTH.name to stringResource(R.string.aerix_download_platform_modrinth),
        AssetPlatform.CURSEFORGE.name to stringResource(R.string.aerix_download_platform_curseforge)
    )
    val transitionLabels = listOf(
        AerixTransitionType.CLOSE.name to stringResource(R.string.aerix_settings_transition_close),
        AerixTransitionType.JELLY_BOUNCE.name to stringResource(R.string.aerix_settings_transition_jelly),
        AerixTransitionType.BOUNCE.name to stringResource(R.string.aerix_settings_transition_bounce),
        AerixTransitionType.SLICE_IN.name to stringResource(R.string.aerix_settings_transition_slice)
    )
    val aerixLabel = stringResource(R.string.aerix_settings_theme_aerix)
    val customLabel = stringResource(R.string.aerix_settings_theme_custom)
    val themeEntries = listOf(
        AerixColorTheme.AERIX.name to aerixLabel,
        AerixColorTheme.CUSTOM.name to customLabel
    )
    val downloadSourceSetting = remember {
        PojavSettingsBridge.misc.settings.filterIsInstance<PojavSetting.Choice>().firstOrNull { it.key == "downloadSource" }
    }
    val downloadSourceEntries = remember(downloadSourceSetting) {
        downloadSourceSetting?.entries(activity) ?: emptyList()
    }
    var currentDownloadSource by remember(revision, downloadSourceSetting) {
        mutableStateOf(downloadSourceSetting?.current() ?: "default")
    }

    if (showColorPicker) {
        HsvColorDialog(
            initial = Color(AerixPreferences.customColor),
            onDismiss = { showColorPicker = false },
            onApply = { color ->
                AerixPreferences.saveCustomColor(color.toArgb())
                AerixPreferences.saveColorTheme(AerixColorTheme.CUSTOM)
                showColorPicker = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // Discord Rich Presence: what your own profile shows while the launcher is open.
        item {
            DiscordPresenceSettingsCard()
        }

        // Group 1: Color theme, custom color, festival effects, full screen (Mirai LauncherSettingsScreen)
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiListSettingsCard(
                    position = CardPosition.Top,
                    items = themeEntries,
                    currentId = AerixPreferences.colorTheme.name,
                    defaultId = AerixColorTheme.AERIX.name,
                    title = stringResource(R.string.aerix_settings_color_theme),
                    summary = stringResource(R.string.aerix_settings_color_theme_sub),
                    getItemText = { it.second },
                    getItemId = { it.first },
                    onValueChange = { item ->
                        AerixPreferences.saveColorTheme(AerixColorTheme.valueOf(item.first))
                    }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_custom_color),
                    summary = stringResource(R.string.aerix_settings_custom_color_sub),
                    value = "#%06X".format(AerixPreferences.customColor and 0xFFFFFF),
                    onClick = { showColorPicker = true }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_festival),
                    summary = stringResource(R.string.aerix_settings_festival_sub),
                    checked = AerixPreferences.festivalEffects,
                    onCheckedChange = { AerixPreferences.saveFestivalEffects(it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.aerix_settings_fullscreen),
                    summary = stringResource(R.string.aerix_settings_fullscreen_sub),
                    checked = AerixPreferences.fullScreen,
                    onCheckedChange = { value ->
                        AerixPreferences.saveFullScreen(value)
                        val portrait = activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                        Tools.setInsetsMode(activity, value, portrait)
                    }
                )
            }
        }

        // Group 2: Background opacity & blur
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSliderSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.aerix_settings_bg_opacity),
                    summary = stringResource(R.string.aerix_settings_bg_opacity_sub),
                    value = AerixPreferences.backgroundOpacity,
                    range = 20..100,
                    step = 1,
                    suffix = " %",
                    onValueChange = { AerixPreferences.saveBackgroundOpacity(it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.aerix_settings_bg_blur),
                    summary = stringResource(R.string.aerix_settings_bg_blur_sub),
                    value = AerixPreferences.backgroundBlur,
                    range = 0..20,
                    step = 1,
                    suffix = " dp",
                    onValueChange = { AerixPreferences.saveBackgroundBlur(it) }
                )
            }
        }

        // Group 3: Animation speed, extent & swap transition type
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.aerix_settings_theme_motion),
                    summary = stringResource(R.string.aerix_settings_theme_motion_sub),
                    checked = AerixPreferences.themeMotion,
                    onCheckedChange = { AerixPreferences.saveThemeMotion(it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_animate_speed),
                    summary = stringResource(R.string.aerix_settings_animate_speed_sub),
                    value = AerixPreferences.animateSpeed,
                    range = 0..10,
                    step = 1,
                    suffix = "",
                    onValueChange = { AerixPreferences.saveAnimateSpeed(it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_animate_extent),
                    summary = stringResource(R.string.aerix_settings_animate_extent_sub),
                    value = AerixPreferences.animateExtent,
                    range = 0..10,
                    step = 1,
                    suffix = "",
                    onValueChange = { AerixPreferences.saveAnimateExtent(it) }
                )
                MiraiListSettingsCard(
                    position = CardPosition.Bottom,
                    items = transitionLabels,
                    currentId = AerixPreferences.swapAnimateType.name,
                    defaultId = AerixTransitionType.JELLY_BOUNCE.name,
                    title = stringResource(R.string.aerix_settings_transition),
                    summary = stringResource(R.string.aerix_settings_transition_sub),
                    getItemText = { it.second },
                    getItemId = { it.first },
                    onValueChange = { item ->
                        AerixPreferences.saveSwapAnimateType(AerixTransitionType.valueOf(item.first))
                    }
                )
            }
        }

        // Group 4: Mirror source, asset platform, log retention, CurseForge API key, log share, force English
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                if (downloadSourceSetting != null && downloadSourceEntries.isNotEmpty()) {
                    MiraiListSettingsCard(
                        position = CardPosition.Top,
                        items = downloadSourceEntries,
                        currentId = currentDownloadSource,
                        defaultId = downloadSourceEntries.first().first,
                        title = stringResource(downloadSourceSetting.titleRes),
                        summary = downloadSourceSetting.summaryRes?.let { stringResource(it) },
                        getItemText = { it.second },
                        getItemId = { it.first },
                        onValueChange = { item ->
                            currentDownloadSource = item.first
                            PojavSettingsBridge.writeString(activity, downloadSourceSetting.key, item.first)
                        }
                    )
                }
                MiraiListSettingsCard(
                    position = if (downloadSourceSetting != null && downloadSourceEntries.isNotEmpty()) CardPosition.Middle else CardPosition.Top,
                    items = platformLabels,
                    currentId = AerixPreferences.assetPlatformSource.name,
                    defaultId = AssetPlatform.MODRINTH.name,
                    title = stringResource(R.string.aerix_settings_asset_platform),
                    summary = stringResource(R.string.aerix_settings_asset_platform_sub),
                    getItemText = { it.second },
                    getItemId = { it.first },
                    onValueChange = { item ->
                        AerixPreferences.saveAssetPlatformSource(AssetPlatform.valueOf(item.first))
                    }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_log_retention),
                    summary = stringResource(R.string.aerix_settings_log_retention_sub),
                    value = AerixPreferences.logRetentionDays,
                    range = 1..14,
                    step = 1,
                    suffix = " d",
                    onValueChange = { AerixPreferences.saveLogRetentionDays(it) }
                )
                MiraiTextSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.aerix_settings_curseforge_key),
                    summary = stringResource(R.string.aerix_settings_curseforge_key_sub),
                    value = AerixPreferences.curseForgeApiKey,
                    onValueChange = { AerixPreferences.saveCurseForgeApiKey(it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_force_english_title),
                    summary = stringResource(R.string.preference_force_english_description),
                    checked = remember(revision) { LauncherPreferences.PREF_FORCE_ENGLISH },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "force_english", it) }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.main_share_logs),
                    summary = stringResource(R.string.aerix_logs_subtitle),
                    value = "Share",
                    onClick = { PojavNavigator.shareLogs(activity) }
                )
            }
        }

        // Group 5: the Aetherium mod. It is a recommendation, so it lives here with the rest of
        // the launcher settings instead of over the home screen.
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiActionSettingsCard(
                    position = CardPosition.Single,
                    title = stringResource(R.string.aerix_settings_aetherium),
                    summary = stringResource(R.string.aerix_settings_aetherium_sub),
                    value = stringResource(R.string.aerix_settings_aetherium_tag),
                    onClick = { AerixNav.navigate(LauncherSection.DISCOVER) }
                )
            }
        }
    }
}

// ============================================================================================
// 5. Controls tab (Mirai ControlSettingsScreen.kt: mouse, touch gestures, gyroscope)
// ============================================================================================

@Composable
private fun ControlsTabContent(activity: FragmentActivity) {
    val revision = PojavSettingsBridge.revision
    val gyroEnabled = remember(revision) { LauncherPreferences.PREF_ENABLE_GYRO }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // Mouse group
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.preference_mouse_start_title),
                    summary = stringResource(R.string.preference_mouse_start_description),
                    checked = remember(revision) { LauncherPreferences.PREF_VIRTUAL_MOUSE_START },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "mouse_start", it) }
                )
            }
        }

        // Mouse settings: the pointer's image, size and opacity, its speed, and the game's look
        // sensitivity kept as a separate control. The two speeds used to be one preference, which is
        // why turning aim up also slid the launcher cursor out from under the finger.
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                Text(
                    "Mouse settings",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                AerixMouseSettingsSection()
            }
        }

        // Gestures & on-screen buttons group
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.mcl_disable_gestures),
                    summary = stringResource(R.string.mcl_disable_gestures_subtitle),
                    checked = remember(revision) { LauncherPreferences.PREF_DISABLE_GESTURES },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "disableGestures", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_disable_swap_hand),
                    summary = stringResource(R.string.mcl_disable_swap_hand_subtitle),
                    checked = remember(revision) { LauncherPreferences.PREF_DISABLE_SWAP_HAND },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "disableDoubleTap", it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_setting_title_longpresstrigger),
                    summary = stringResource(R.string.mcl_setting_subtitle_longpresstrigger),
                    value = remember(revision) { LauncherPreferences.PREF_LONGPRESS_TRIGGER },
                    range = 100..1000,
                    step = 10,
                    suffix = " ms",
                    onValueChange = { PojavSettingsBridge.writeInt(activity, "timeLongPressTrigger", it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_setting_title_buttonscale),
                    summary = stringResource(R.string.mcl_setting_subtitle_buttonscale),
                    value = remember(revision) { LauncherPreferences.PREF_BUTTONSIZE.toInt() },
                    range = 50..250,
                    step = 5,
                    suffix = " %",
                    onValueChange = { PojavSettingsBridge.writeInt(activity, "buttonscale", it) }
                )
                MiraiSliderSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_button_transparency),
                    summary = stringResource(R.string.preference_button_transparency_summary),
                    value = remember(revision) { LauncherPreferences.PREF_BUTTON_TRANSPARENCY.toInt() },
                    range = 0..100,
                    step = 1,
                    suffix = " %",
                    onValueChange = { PojavSettingsBridge.writeInt(activity, "buttonTransparency", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.mcl_setting_title_buttonallcaps),
                    summary = stringResource(R.string.mcl_setting_subtitle_buttonallcaps),
                    checked = remember(revision) { LauncherPreferences.PREF_BUTTON_ALL_CAPS },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "buttonAllCaps", it) }
                )
                MiraiSwitchSettingsCard(
                    position = CardPosition.Bottom,
                    title = stringResource(R.string.preference_keyboard_autopan_title),
                    summary = stringResource(R.string.preference_keyboard_autopan_summary),
                    checked = remember(revision) { LauncherPreferences.PREF_KEYBOARD_AUTOPANNING },
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "keyboardAutoPanning", it) }
                )
            }
        }

        // Gyroscope group
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiSwitchSettingsCard(
                    position = if (gyroEnabled) CardPosition.Top else CardPosition.Single,
                    title = stringResource(R.string.preference_enable_gyro_title),
                    summary = stringResource(R.string.preference_enable_gyro_description),
                    checked = gyroEnabled,
                    onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "enableGyro", it) }
                )
                if (gyroEnabled) {
                    MiraiSliderSettingsCard(
                        position = CardPosition.Middle,
                        title = stringResource(R.string.preference_gyro_sensitivity_title),
                        summary = stringResource(R.string.preference_gyro_sensitivity_description),
                        value = remember(revision) { (LauncherPreferences.PREF_GYRO_SENSITIVITY * 100).roundToInt() },
                        range = 25..300,
                        step = 5,
                        suffix = " %",
                        onValueChange = { PojavSettingsBridge.writeInt(activity, "gyroSensitivity", it) }
                    )
                    MiraiSliderSettingsCard(
                        position = CardPosition.Middle,
                        title = stringResource(R.string.preference_gyro_sample_rate_title),
                        summary = stringResource(R.string.preference_gyro_sample_rate_description),
                        value = remember(revision) { LauncherPreferences.PREF_GYRO_SAMPLE_RATE },
                        range = 5..50,
                        step = 1,
                        suffix = " ms",
                        onValueChange = { PojavSettingsBridge.writeInt(activity, "gyroSampleRate", it) }
                    )
                    MiraiSwitchSettingsCard(
                        position = CardPosition.Middle,
                        title = stringResource(R.string.preference_gyro_smoothing_title),
                        summary = stringResource(R.string.preference_gyro_smoothing_description),
                        checked = remember(revision) { LauncherPreferences.PREF_GYRO_SMOOTHING },
                        onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "gyroSmoothing", it) }
                    )
                    MiraiSwitchSettingsCard(
                        position = CardPosition.Middle,
                        title = stringResource(R.string.preference_gyro_invert_x_axis),
                        summary = stringResource(R.string.preference_gyro_invert_x_axis_description),
                        checked = remember(revision) { LauncherPreferences.PREF_GYRO_INVERT_X },
                        onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "gyroInvertX", it) }
                    )
                    MiraiSwitchSettingsCard(
                        position = CardPosition.Bottom,
                        title = stringResource(R.string.preference_gyro_invert_y_axis),
                        summary = stringResource(R.string.preference_gyro_invert_y_axis_description),
                        checked = remember(revision) { LauncherPreferences.PREF_GYRO_INVERT_Y },
                        onCheckedChange = { PojavSettingsBridge.writeBoolean(activity, "gyroInvertY", it) }
                    )
                }
            }
        }
    }
}

// ============================================================================================
// 6. Control Manager tab (Mirai ControlManageScreen.kt over Tools.CTRLMAP_PATH)
// ============================================================================================

@Composable
private fun ControlManagerTabContent(activity: FragmentActivity) {
    val catalog = rememberControlLayoutCatalog(activity)
    val files = catalog.files
    val defaultPath = catalog.selectedPath

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val intent = Intent(activity, ImportControlActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.preference_edit_controls_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.preference_edit_controls_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = AerixSurface.textSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                AerixGhostButton(
                    text = "",
                    iconRes = R.drawable.aerix_ic_refresh,
                    onClick = { catalog.refresh() }
                )
                AerixGhostButton(
                    text = "Import",
                    iconRes = R.drawable.aerix_ic_add,
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
                AerixPrimaryButton(
                    text = stringResource(R.string.mcl_option_customcontrol),
                    iconRes = R.drawable.aerix_ic_edit_outlined,
                    onClick = { PojavNavigator.openCustomControls(activity) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            items(files.size, key = { files[it].absolutePath }) { index ->
                val file = files[index]
                val isDefault = file.absolutePath == defaultPath
                Surface(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = isDefault,
                        role = Role.RadioButton,
                        onClick = { catalog.select(file) }
                    ),
                    shape = RoundedCornerShape(AerixRadii.card),
                    color = AerixSurface.panel,
                    border = BorderStroke(
                        if (isDefault) 1.5.dp else AerixSpacing.hairline,
                        if (isDefault) AerixThemeManager.currentAccent() else AerixSurface.borderSoft
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.smPlus),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.aerix_ic_videogame_asset_outlined),
                            contentDescription = null,
                            tint = if (isDefault) AerixThemeManager.currentAccent() else AerixSurface.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.nameWithoutExtension,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = AerixSurface.textSecondary
                            )
                        }
                        AerixGhostButton(
                            text = if (isDefault) "Default" else "Set default",
                            iconRes = if (isDefault) R.drawable.aerix_ic_check else null,
                            onClick = {
                                catalog.select(file)
                            }
                        )
                        IconButton(onClick = {
                            if (catalog.select(file)) PojavNavigator.openCustomControls(activity)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.aerix_ic_edit_outlined),
                                contentDescription = "Edit",
                                tint = AerixSurface.textSecondary
                            )
                        }
                        if (file.name != "default.json") {
                            IconButton(onClick = {
                                catalog.delete(file)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.aerix_ic_delete_outlined),
                                    contentDescription = stringResource(R.string.global_remove),
                                    tint = AerixSurface.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// 7. Gamepad tab (Mirai GamepadSettingsScreen.kt)
// ============================================================================================

@Composable
private fun GamepadTabContent(activity: FragmentActivity) {
    val revision = PojavSettingsBridge.revision
    val deadzoneSetting = remember {
        PojavSettingsBridge.controls.settings.filterIsInstance<PojavSetting.Slider>().firstOrNull { it.key == "gamepad_deadzone_scale" }
    }
    val wipeSetting = remember {
        PojavSettingsBridge.controls.settings.filterIsInstance<PojavSetting.Action>().firstOrNull { it.key == "wipe_controller" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        item {
            SettingsCardColumn(modifier = Modifier.fillMaxWidth()) {
                MiraiActionSettingsCard(
                    position = CardPosition.Top,
                    title = stringResource(R.string.preference_remap_controller_title),
                    summary = stringResource(R.string.preference_remap_controller_description),
                    value = "Open",
                    onClick = { PojavNavigator.openGamepadMapper(activity) }
                )
                MiraiActionSettingsCard(
                    position = CardPosition.Middle,
                    title = stringResource(R.string.preference_wipe_controller_title),
                    summary = stringResource(R.string.preference_wipe_controller_description),
                    value = "Reset",
                    onClick = { wipeSetting?.run?.invoke(activity) }
                )
                if (deadzoneSetting != null) {
                    val min = deadzoneSetting.min(activity)
                    val max = maxOf(min, deadzoneSetting.max(activity))
                    MiraiSliderSettingsCard(
                        position = CardPosition.Bottom,
                        title = stringResource(deadzoneSetting.titleRes),
                        summary = deadzoneSetting.summaryRes?.let { stringResource(it) },
                        value = remember(revision) { deadzoneSetting.current().coerceIn(min, max) },
                        range = min..max,
                        step = deadzoneSetting.step,
                        suffix = deadzoneSetting.suffix,
                        onValueChange = { PojavSettingsBridge.writeInt(activity, deadzoneSetting.key, it) }
                    )
                }
            }
        }
    }
}

// ============================================================================================
// 8. About tab (Mirai AboutInfoScreen.kt — Screenshot 5)
// ============================================================================================

@Composable
private fun AboutInfoTabContent(activity: FragmentActivity) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    if (showUpdateDialog) LauncherUpdateDialog(activity) { showUpdateDialog = false }

    val accent = AerixThemeManager.currentAccent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        contentPadding = PaddingValues(bottom = AerixSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // Card 1: Aerix Launcher + entitybrian + Discord (Screenshot 5 top card)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AerixRadii.hero),
                color = AerixSurface.panel,
                border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AerixSpacing.mdPlus),
                    verticalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
                ) {
                    AboutRowCard(
                        iconRes = R.drawable.ic_aerix_mark,
                        iconTint = Color.Unspecified,
                        title = "Aerix Launcher",
                        subtitle = "Launcher Version: ${BuildConfig.VERSION_NAME}",
                        actions = {
                            AboutPillButton("Check for Updates", accent) {
                                showUpdateDialog = true
                            }
                            AboutPillButton("Project Link", accent) {
                                PojavNavigator.openUrl(activity, "https://github.com/entitybrian69-bit/Aerix-launcher")
                            }
                            AboutPillButton("Discord", accent) {
                                PojavNavigator.openDiscord(activity)
                            }
                        }
                    )

                    AboutRowCard(
                        iconRes = R.drawable.aerix_img_avatar_entitybrian,
                        iconTint = Color.Unspecified,
                        title = "entitybrian",
                        subtitle = "Owner and maintainer of Aerix Launcher.",
                        actions = {
                            AboutPillButton("GitHub profile", accent) {
                                PojavNavigator.openUrl(activity, "https://github.com/entitybrian69-bit")
                            }
                        }
                    )
                }
            }
        }

    }
}

@Composable
private fun AboutRowCard(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AerixRadii.card),
        color = AerixSurface.panelRaised.copy(alpha = 0.36f),
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.smPlus),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
            if (iconTint == Color.Unspecified) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(AerixRadii.controlSmall))
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AerixSurface.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                actions()
            }
        }
    }
}

@Composable
private fun AboutPillButton(text: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = AerixSurface.onAccent
        ),
        contentPadding = PaddingValues(horizontal = AerixSpacing.lg, vertical = AerixSpacing.smCompact)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ============================================================================================
// Mirai SettingsCard layouts (SwitchSettingsCard, SliderSettingsCard, ActionSettingsCard, TextSettingsCard)
// ============================================================================================

@Composable
private fun MiraiSwitchSettingsCard(
    position: CardPosition,
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = rememberSettingsCardShape(position)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, tint = AerixSurface.glassTint, strength = 0.9f, elevation = AerixSpacing.zero),
        shape = shape,
        color = AerixSurface.panel,
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft),
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = AerixSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AerixSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = AerixSurface.textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
            DefaultSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun MiraiSliderSettingsCard(
    position: CardPosition,
    title: String,
    summary: String?,
    value: Int,
    range: IntRange,
    step: Int,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    MiraiIntSliderSettingsCard(
        value = value.coerceIn(range),
        title = title,
        position = position,
        summary = summary,
        valueRange = range,
        suffix = suffix,
        fineTuningControl = true,
        fineTuningStep = step.coerceAtLeast(1),
        onValueFinished = { nextVal ->
            val snapped = if (step > 0) {
                (range.first + ((nextVal - range.first).toFloat() / step).roundToInt() * step).coerceIn(range.first, range.last)
            } else nextVal.coerceIn(range.first, range.last)
            onValueChange(snapped)
        }
    )
}

@Composable
private fun MiraiActionSettingsCard(
    position: CardPosition,
    title: String,
    summary: String?,
    value: String,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val shape = rememberSettingsCardShape(position)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, tint = AerixSurface.glassTint, strength = 0.9f, elevation = AerixSpacing.zero),
        shape = shape,
        color = AerixSurface.panel,
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = AerixSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = AerixSurface.textSecondary
                    )
                }
            }
            trailingIcon?.invoke()
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AerixThemeManager.currentAccent(),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiraiTextSettingsCard(
    position: CardPosition,
    title: String,
    summary: String?,
    value: String,
    onValueChange: (String) -> Unit
) {
    val shape = rememberSettingsCardShape(position)
    var text by remember(value) { mutableStateOf(value) }
    val accent = AerixThemeManager.currentAccent()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, tint = AerixSurface.glassTint, strength = 0.9f, elevation = AerixSpacing.zero),
        shape = shape,
        color = AerixSurface.panel,
        contentColor = AerixSurface.textPrimary,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = AerixSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = AerixSurface.textSecondary
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = AerixSurface.borderSoft,
                    focusedTextColor = AerixSurface.textPrimary,
                    unfocusedTextColor = AerixSurface.textPrimary,
                    cursorColor = accent
                ),
                trailingIcon = {
                    if (text != value) {
                        Text(
                            text = "SAVE",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onValueChange(text) }
                                .padding(AerixSpacing.sm)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    entries: List<Pair<String, String>>,
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(title, color = AerixSurface.textPrimary) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                entries.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(value) }
                            .padding(vertical = AerixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == current,
                            onClick = { onPick(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = AerixSurface.textSecondary)
                        )
                        Text(label, color = AerixSurface.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.aerix_settings_cancel), color = AerixSurface.textSecondary) }
        }
    )
}

/** Hue / saturation / brightness picker for the custom accent. */
@Composable
private fun HsvColorDialog(initial: Color, onDismiss: () -> Unit, onApply: (Color) -> Unit) {
    val hsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var brightness by remember { mutableFloatStateOf(hsv[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(stringResource(R.string.aerix_settings_custom_color), color = AerixSurface.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .border(1.dp, AerixSurface.borderSoft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("#%06X".format(color.toArgb() and 0xFFFFFF), color = if (brightness > 0.6f) Color.Black else Color.White, fontSize = 13.sp)
                }
                ColorSlider(stringResource(R.string.aerix_settings_hue), hue, 0f..360f) { hue = it }
                ColorSlider(stringResource(R.string.aerix_settings_saturation), saturation, 0f..1f) { saturation = it }
                ColorSlider(stringResource(R.string.aerix_settings_brightness), brightness, 0f..1f) { brightness = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(color) }) { Text(stringResource(R.string.aerix_settings_apply), color = color) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.aerix_settings_cancel), color = AerixSurface.textSecondary) }
        }
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = AerixSurface.textSecondary, fontSize = 11.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AerixSurface.textPrimary,
                activeTrackColor = AerixThemeManager.currentAccent(),
                inactiveTrackColor = AerixSurface.panelTrack
            )
        )
    }
}
