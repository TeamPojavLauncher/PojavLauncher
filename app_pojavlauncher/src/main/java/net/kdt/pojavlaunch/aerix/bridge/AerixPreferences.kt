/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Preference adapter for the Aerix-only launcher settings that come from
 * mirai-launcher- MiraiLauncher/src/main/java/com/movtery/zalithlauncher/setting/AllSettings.kt
 * and ui/theme/MiraiThemeManager.kt.
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

package net.kdt.pojavlaunch.aerix.bridge

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.kdt.pojavlaunch.prefs.LauncherPreferences

/** Color theme types kept from Zalith/Aerix. Only AERIX and CUSTOM are reachable from the port. */
enum class AerixColorTheme { AERIX, CUSTOM }

/**
 * The two home screens the launcher can draw.
 *
 * [PREMIUM] is the original dashboard - the utility tiles under "YOUR SPACE", the active-world card,
 * tasks and recent worlds below it, and the docked 3D skin card with its own PLAY on wide screens,
 * with the large glass stage, wide padding and pill-shaped buttons that go with it. Players asked
 * for "the old UI" back, so that is what the pill is labelled, rather than a name to decode.
 * [CLEAN] is the newer one: the same controls and the same data, flat panels, tighter rows and the
 * rotatable character as the hero of the page, which reads better on a phone held sideways.
 *
 * Both are live - the player picks one at the top of the dashboard and the home page composes into
 * it, because restarting the launcher to try a skin is not a choice, it is a chore.
 */
enum class AerixLauncherUiStyle(val label: String) { PREMIUM("Old UI (dashboard)"), CLEAN("New UI (hero)") }

/** Page transition types, from mirai utils/animation/TransitionAnimationType.kt. */
enum class AerixTransitionType { CLOSE, JELLY_BOUNCE, BOUNCE, SLICE_IN }

/**
 * Stores the Aerix launcher appearance settings in their own SharedPreferences file so they never
 * collide with Pojav keys. Keys whose meaning overlaps a Pojav setting are written through to
 * [LauncherPreferences] so the backend stays the single source of truth for them.
 */
object AerixPreferences {
    private const val FILE = "aerix_ui_prefs"

    const val KEY_COLOR_THEME = "launcherColorTheme"
    const val KEY_CUSTOM_COLOR = "launcherCustomColor"
    const val KEY_BACKGROUND_OPACITY = "launcherBackgroundOpacity"
    const val KEY_UI_STYLE = "launcherUiStyle"
    const val KEY_FULL_SCREEN = "launcherFullScreen"
    const val KEY_ANIMATE_SPEED = "launcherAnimateSpeed"
    const val KEY_SELECTED_WALLPAPER = "selected_wallpaper_id"
    const val KEY_LAST_SECTION = "aerixLastSection"
    const val KEY_BACKGROUND_BLUR = "backgroundBlur"
    const val KEY_SWAP_ANIMATE_TYPE = "launcherSwapAnimateType"
    const val KEY_ANIMATE_EXTENT = "launcherAnimateExtent"
    const val KEY_FESTIVAL_EFFECTS = "launcherFestivalEffects"
    /** Accent pulses and glows. Read as Compose state, so the switch applies without a restart. */
    const val KEY_THEME_MOTION = "launcherThemeMotion"
    /** True once the first-run guide has been completed or skipped. */
    const val KEY_GUIDE_PLAYED = "launcherGuidePlayed"
    /** True once the first-run Notification ON/OFF popup window has been answered. */
    const val KEY_NOTIFICATION_PROMPT_SHOWN = "aerixFirstRunNotificationPromptShown"
    /** True when the launcher's wallpaper is drawn behind the controls while the game starts. */
    const val KEY_LOADING_WALLPAPER = "loadingScreenWallpaper"
    const val KEY_LOG_TEXT_SIZE = "logTextSize"
    const val KEY_LOG_BUFFER_FLUSH_INTERVAL = "logBufferFlushInterval"
    const val KEY_ASSET_PLATFORM = "assetPlatformSource"
    const val KEY_LOG_RETENTION_DAYS = "launcherLogRetentionDays"
    const val KEY_CURSEFORGE_API_KEY = "curseForgeApiKey"
    /** JSON array of favorite Discover projects (mirai download/assets/favorites). */
    const val KEY_FAVORITES = "aerixFavoriteProjects"
    /** JSON array of pinned instance names (mirai VersionsManageScreen "Pinned" tab). */
    const val KEY_PINNED_INSTANCES = "aerixPinnedInstances"
    /** Discord Rich Presence: show "Playing Aerix Launcher" on the player's own profile. */
    const val KEY_DISCORD_PRESENCE = "aerixDiscordPresence"
    /** The Discord application id the presence is published under (Developer Portal). */
    const val KEY_DISCORD_APP_ID = "aerixDiscordAppId"
    /** Switch in-game Rich Presence to Minecraft's verified catalog ID so clicking opens its Game Profile. */
    const val KEY_DISCORD_GAME_PROFILE_MODE = "aerixDiscordGameProfileMode"
    /** Attach modpack art, clickable info URLs, and action buttons to Discord Rich Presence cards. */
    const val KEY_DISCORD_CLICKABLE_LINKS = "aerixDiscordClickableLinks"

    const val DEFAULT_CUSTOM_COLOR = 0xFF8DEFE0.toInt()
    const val DEFAULT_BACKGROUND_OPACITY = 60

    /**
     * How solid the cards, pills and panels on top of the wallpaper are, as a percentage.
     *
     * Separate from [backgroundOpacity], which washes even the wallpaper: this one leaves the
     * wallpaper alone and only changes the surfaces drawn over it, so a player can keep the art
     * bright and still have crisp cards - or see straight through them.
     */
    const val KEY_LAYOUT_TRANSPARENCY = "layoutTransparency"
    const val DEFAULT_LAYOUT_TRANSPARENCY = 100
    const val DEFAULT_ANIMATE_SPEED = 3
    const val DEFAULT_WALLPAPER = "wp_01_lush_caves"
    /** Mirai AllSettings.logBufferFlushInterval default. */
    const val DEFAULT_LOG_BUFFER_FLUSH_INTERVAL = 200

    private var prefs: SharedPreferences? = null

    var colorTheme by mutableStateOf(AerixColorTheme.AERIX)
        private set
    var customColor by mutableIntStateOf(DEFAULT_CUSTOM_COLOR)
        private set
    var backgroundOpacity by mutableIntStateOf(DEFAULT_BACKGROUND_OPACITY)
        private set
    /** 25 = barely there glass, 100 = the card colours exactly as designed. */
    var layoutTransparency by mutableIntStateOf(DEFAULT_LAYOUT_TRANSPARENCY)
        private set
    /** The dashboard skin, as state so switching it recomposes the shell instead of restarting it. */
    var uiStyle by mutableStateOf(AerixLauncherUiStyle.CLEAN)
        private set

    var fullScreen by mutableStateOf(true)
        private set
    var animateSpeed by mutableIntStateOf(DEFAULT_ANIMATE_SPEED)
        private set
    var selectedWallpaper by mutableStateOf(DEFAULT_WALLPAPER)
        private set
    var lastSection by mutableStateOf("HOME")
        private set

    /** Wallpaper blur radius in dp (Aerix "backgroundBlur"); rendered on Android 12+ only. */
    var backgroundBlur by mutableIntStateOf(0)
        private set

    /** Aerix "launcherSwapAnimateType": section transition style. */
    var swapAnimateType by mutableStateOf(AerixTransitionType.JELLY_BOUNCE)
        private set

    /** Aerix "launcherAnimateExtent" (0..10, 5 = base amplitude). */
    var animateExtent by mutableIntStateOf(3)
        private set

    /** Aerix "launcherFestivalEffects": persistent festival particle layer. */
    var festivalEffects by mutableStateOf(false)
        private set

    /**
     * Animated theme: the accent breathes and selected elements glow. Compose state, so flipping it
     * repaints whatever is on screen instead of asking the user to restart.
     */
    var themeMotion by mutableStateOf(true)
    /** Set once the first-run tour has been shown, so it never appears again on its own. */
    var guidePlayed by mutableStateOf(false)
        private set
    /** Set once the first-time app open Notification ON/OFF popup has been answered. */
    var notificationPromptShown by mutableStateOf(false)
        private set

    /** Draw the launcher's wallpaper behind the controls while the game starts. */
    var loadingWallpaper by mutableStateOf(false)
        private set

    /** Aerix "logTextSize" (5..20 sp) used by the log viewer. */
    var logTextSize by mutableIntStateOf(15)

    /** Aerix "logBufferFlushInterval" (100..1000 ms) used by the in-game log box. */
    var logBufferFlushInterval by mutableIntStateOf(DEFAULT_LOG_BUFFER_FLUSH_INTERVAL)
        private set

    /** Aerix "launcherLogRetentionDays" (1..14): crash reports older than this are removed at startup. */
    var logRetentionDays by mutableIntStateOf(7)
        private set

    /** Aerix "assetPlatformSource": platform preselected in the download screens. */
    var assetPlatformSource by mutableStateOf(AssetPlatform.MODRINTH)
        private set

    /** Aerix "curseForgeApiKey": user key that overrides the key baked into the build. */
    var curseForgeApiKey by mutableStateOf("")
        private set

    /** Favorite projects as a JSON array of {platform,id,title,description,author,iconUrl,downloads,type}. */
    var favoritesJson by mutableStateOf("[]")
        private set

    /** Pinned instance names as a JSON array of strings. */
    var pinnedJson by mutableStateOf("[]")
        private set

    /** Aerix "aerixDiscordPresence": publish what the launcher is doing to the Discord app. */
    var discordPresenceEnabled by mutableStateOf(true)
        private set

    /** Aerix "aerixDiscordAppId": the Discord application the presence is shown under. */
    var discordAppId by mutableStateOf("")
        private set

    /** True to switch in-game Rich Presence to Minecraft's verified Discord Game Profile ID. */
    var discordGameProfileMode by mutableStateOf(true)
        private set

    /** True to attach modpack art, clickable info URLs, and action buttons to Rich Presence cards. */
    var discordClickableLinks by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        colorTheme = runCatching {
            AerixColorTheme.valueOf(p.getString(KEY_COLOR_THEME, AerixColorTheme.AERIX.name)!!)
        }.getOrDefault(AerixColorTheme.AERIX)
        customColor = p.getInt(KEY_CUSTOM_COLOR, DEFAULT_CUSTOM_COLOR)
        backgroundOpacity = p.getInt(KEY_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY).coerceIn(20, 100)
        layoutTransparency = p.getInt(KEY_LAYOUT_TRANSPARENCY, DEFAULT_LAYOUT_TRANSPARENCY).coerceIn(25, 100)
        fullScreen = p.getBoolean(KEY_FULL_SCREEN, true)
        uiStyle = runCatching {
            AerixLauncherUiStyle.valueOf(p.getString(KEY_UI_STYLE, AerixLauncherUiStyle.CLEAN.name)!!)
        }.getOrDefault(AerixLauncherUiStyle.CLEAN)
        animateSpeed = p.getInt(KEY_ANIMATE_SPEED, DEFAULT_ANIMATE_SPEED).coerceIn(0, 10)
        selectedWallpaper = p.getString(KEY_SELECTED_WALLPAPER, DEFAULT_WALLPAPER) ?: DEFAULT_WALLPAPER
        lastSection = p.getString(KEY_LAST_SECTION, "HOME") ?: "HOME"
        backgroundBlur = p.getInt(KEY_BACKGROUND_BLUR, 0).coerceIn(0, 20)
        swapAnimateType = runCatching {
            AerixTransitionType.valueOf(p.getString(KEY_SWAP_ANIMATE_TYPE, AerixTransitionType.JELLY_BOUNCE.name)!!)
        }.getOrDefault(AerixTransitionType.JELLY_BOUNCE)
        animateExtent = p.getInt(KEY_ANIMATE_EXTENT, 3).coerceIn(0, 10)
        festivalEffects = p.getBoolean(KEY_FESTIVAL_EFFECTS, false)
        themeMotion = p.getBoolean(KEY_THEME_MOTION, true)
        guidePlayed = p.getBoolean(KEY_GUIDE_PLAYED, false)
        notificationPromptShown = p.getBoolean(KEY_NOTIFICATION_PROMPT_SHOWN, false)
        loadingWallpaper = p.getBoolean(KEY_LOADING_WALLPAPER, false)
        logTextSize = p.getInt(KEY_LOG_TEXT_SIZE, 15).coerceIn(5, 20)
        logBufferFlushInterval = p.getInt(KEY_LOG_BUFFER_FLUSH_INTERVAL, DEFAULT_LOG_BUFFER_FLUSH_INTERVAL).coerceIn(100, 1000)
        assetPlatformSource = runCatching {
            AssetPlatform.valueOf(p.getString(KEY_ASSET_PLATFORM, AssetPlatform.MODRINTH.name)!!)
        }.getOrDefault(AssetPlatform.MODRINTH)
        curseForgeApiKey = p.getString(KEY_CURSEFORGE_API_KEY, "") ?: ""
        logRetentionDays = p.getInt(KEY_LOG_RETENTION_DAYS, 7).coerceIn(1, 14)
        favoritesJson = p.getString(KEY_FAVORITES, "[]") ?: "[]"
        pinnedJson = p.getString(KEY_PINNED_INSTANCES, "[]") ?: "[]"
        discordPresenceEnabled = p.getBoolean(KEY_DISCORD_PRESENCE, true)
        discordAppId = p.getString(KEY_DISCORD_APP_ID, "") ?: ""
        discordGameProfileMode = p.getBoolean(KEY_DISCORD_GAME_PROFILE_MODE, true)
        discordClickableLinks = p.getBoolean(KEY_DISCORD_CLICKABLE_LINKS, true)
    }

    fun saveFavoritesJson(value: String) {
        favoritesJson = value
        edit { putString(KEY_FAVORITES, value) }
    }

    fun saveDiscordPresenceEnabled(value: Boolean) {
        discordPresenceEnabled = value
        edit { putBoolean(KEY_DISCORD_PRESENCE, value) }
    }

    fun saveDiscordAppId(value: String) {
        discordAppId = value.trim().filter { it.isDigit() }
        edit { putString(KEY_DISCORD_APP_ID, discordAppId) }
    }

    fun saveDiscordGameProfileMode(value: Boolean) {
        discordGameProfileMode = value
        edit { putBoolean(KEY_DISCORD_GAME_PROFILE_MODE, value) }
    }

    fun saveDiscordClickableLinks(value: Boolean) {
        discordClickableLinks = value
        edit { putBoolean(KEY_DISCORD_CLICKABLE_LINKS, value) }
    }

    /**
     * Choose the dashboard skin. Persisted and applied by recomposition on the next frame - the
     * player is not asked to restart the launcher to see it, and the shell animates into place.
     */
    fun saveUiStyle(value: AerixLauncherUiStyle) {
        if (uiStyle == value) return
        uiStyle = value
        edit { putString(KEY_UI_STYLE, value.name) }
    }

    /** True while the flat, tight layout is on; call sites read it as Compose state. */
    val uiIsClean: Boolean get() = uiStyle == AerixLauncherUiStyle.CLEAN

    fun savePinnedJson(value: String) {
        pinnedJson = value
        edit { putString(KEY_PINNED_INSTANCES, value) }
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs?.edit()?.apply { block(); apply() }
    }

    fun saveAssetPlatformSource(value: AssetPlatform) {
        assetPlatformSource = value
        edit { putString(KEY_ASSET_PLATFORM, value.name) }
    }

    fun saveCurseForgeApiKey(value: String) {
        curseForgeApiKey = value.trim()
        edit { putString(KEY_CURSEFORGE_API_KEY, curseForgeApiKey) }
    }

    fun saveColorTheme(value: AerixColorTheme) {
        colorTheme = value
        edit { putString(KEY_COLOR_THEME, value.name) }
    }

    fun saveCustomColor(argb: Int) {
        customColor = argb
        edit { putInt(KEY_CUSTOM_COLOR, argb) }
    }

    fun saveBackgroundOpacity(value: Int) {
        backgroundOpacity = value.coerceIn(20, 100)
        edit { putInt(KEY_BACKGROUND_OPACITY, backgroundOpacity) }
    }

    fun saveLayoutTransparency(value: Int) {
        layoutTransparency = value.coerceIn(25, 100)
        edit { putInt(KEY_LAYOUT_TRANSPARENCY, layoutTransparency) }
    }

    /**
     * Aerix "launcherFullScreen" maps onto the launcher host window. The Pojav "ignoreNotch"
     * setting stays the owner of the cutout behaviour, so this only toggles the system bars.
     */
    fun saveFullScreen(value: Boolean) {
        fullScreen = value
        edit { putBoolean(KEY_FULL_SCREEN, value) }
    }

    fun saveAnimateSpeed(value: Int) {
        animateSpeed = value.coerceIn(0, 10)
        edit { putInt(KEY_ANIMATE_SPEED, animateSpeed) }
    }

    fun saveSelectedWallpaper(id: String) {
        selectedWallpaper = id
        edit { putString(KEY_SELECTED_WALLPAPER, id) }
    }

    fun saveLastSection(name: String) {
        lastSection = name
        edit { putString(KEY_LAST_SECTION, name) }
    }

    fun saveBackgroundBlur(value: Int) {
        backgroundBlur = value.coerceIn(0, 20)
        edit { putInt(KEY_BACKGROUND_BLUR, backgroundBlur) }
    }

    fun saveSwapAnimateType(value: AerixTransitionType) {
        swapAnimateType = value
        edit { putString(KEY_SWAP_ANIMATE_TYPE, value.name) }
    }

    fun saveAnimateExtent(value: Int) {
        animateExtent = value.coerceIn(0, 10)
        edit { putInt(KEY_ANIMATE_EXTENT, animateExtent) }
    }

    fun saveThemeMotion(value: Boolean) {
        themeMotion = value
        edit { putBoolean(KEY_THEME_MOTION, value) }
    }

    fun saveGuidePlayed(value: Boolean) {
        guidePlayed = value
        edit { putBoolean(KEY_GUIDE_PLAYED, value) }
    }

    fun saveNotificationPromptShown(value: Boolean) {
        notificationPromptShown = value
        edit { putBoolean(KEY_NOTIFICATION_PROMPT_SHOWN, value) }
    }

    fun saveLoadingWallpaper(value: Boolean) {
        loadingWallpaper = value
        edit { putBoolean(KEY_LOADING_WALLPAPER, value) }
    }

    fun saveFestivalEffects(value: Boolean) {
        festivalEffects = value
        edit { putBoolean(KEY_FESTIVAL_EFFECTS, value) }
    }

    fun saveLogRetentionDays(value: Int) {
        logRetentionDays = value.coerceIn(1, 14)
        edit { putInt(KEY_LOG_RETENTION_DAYS, logRetentionDays) }
    }

    fun saveLogTextSize(value: Int) {
        logTextSize = value.coerceIn(5, 20)
        edit { putInt(KEY_LOG_TEXT_SIZE, logTextSize) }
    }

    /** Aerix "logBufferFlushInterval": how often buffered log lines reach the in-game log box. */
    fun saveLogBufferFlushInterval(value: Int) {
        logBufferFlushInterval = value.coerceIn(100, 1000)
        edit { putInt(KEY_LOG_BUFFER_FLUSH_INTERVAL, logBufferFlushInterval) }
    }

    /**
     * Amplitude multiplier from mirai AnimationUtils.getTargetValueByAmplitude:
     * extent 0 -> 0.5x, 5 -> 1.0x, 10 -> 1.5x.
     */
    fun extentScale(): Float = 0.5f + animateExtent.coerceIn(0, 10) / 10f

    /** Animation length used by section transitions, derived from the Aerix speed slider (0..10). */
    fun transitionMillis(): Int = if (animateSpeed == 0) 0 else 120 + (10 - animateSpeed) * 40

    /** Pojav notch handling, exposed here so the Aerix launcher settings page can show it. */
    fun ignoreNotch(): Boolean = LauncherPreferences.PREF_IGNORE_NOTCH
}
