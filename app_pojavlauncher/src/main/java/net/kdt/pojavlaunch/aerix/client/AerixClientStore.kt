package net.kdt.pojavlaunch.aerix.client

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

enum class CrosshairShape(val label: String) {
    CLASSIC_PLUS("Classic +"),
    TACTICAL_GAP("Tactical Gap"),
    GAP_DOT("Gap + Dot"),
    PRECISION_DOT("Center Dot"),
    CIRCLE("Hollow Ring"),
    CIRCLE_DOT("Ring + Dot"),
    X_HIT("Diagonal X"),
    CHEVRON("Aim Chevron"),
    DIAMOND("Diamond"),
    TRI_STAR("Tri-Star"),
    CORNER_BOX("Corner Box"),
    SNIPER_T("Sniper T")
}

enum class KeystrokesStyle(val label: String) {
    CRIMSON_DARK("Crimson & Dark"),
    CPS_PILL_ONLY("CPS Only")
}

enum class TouchCurveMode(val label: String, val desc: String) {
    LINEAR_RAW("1:1 Raw Input", "Unmodified direct touch delta"),
    PVP_FLICK("PvP Flick Curve", "Steady micro-aim + fast 180° flicks"),
    SMOOTH_CINEMA("Smooth Gliding", "Damped camera motion")
}

enum class VisualFilterPreset(val label: String, val tintArgb: Long, val desc: String) {
    NONE("Off", 0x00000000L, "Unmodified display output"),
    CRISP_PVP("Crisp PvP", 0x14FFFFFFL, "Brightens dark shadows")
}

enum class SkyboxTheme(val label: String) {
    AURORA_CRYSTAL("Crimson Dusk"),
    SAKURA_SUNSET("Sakura Horizon"),
    STARRY_NEBULA("Starry Nebula"),
    DEEP_VOID("Esports Deep Void")
}

enum class HitSoundPreset(val label: String, val soundEvent: String, val pitch: Float) {
    CRYSTAL_BELL("Crimson Ding", "block.note_block.bell", 1.35f),
    VANILLA("Vanilla Default", "", 1.0f)
}

enum class AfkFarmAction(val label: String, val desc: String) {
    PASSIVE_HOLD("Passive Stand-Still", "Keeps session alive with OLED off"),
    AUTO_FISH_RCLICK("Hold Right-Click", "Continuous right-click hold"),
    XP_GRINDER_SWING("Sword Swing", "Timed left-click swing (1.25s)"),
    ANTI_IDLE_PULSE("Anti-Idle Pulse", "Gentle jump tap every 40s")
}

data class HudWidgetPos(
    val id: String,
    val label: String,
    val xNorm: Float,
    val yNorm: Float,
    val scale: Float = 1.0f
)

data class ClientWaypoint(
    val id: String,
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: String,
    val colorHex: Long = 0xFFDC1E3AL,
    val pinned: Boolean = false
)

object AerixClientStore {
    const val SUPPORTED_MC_VERSION = "1.16.5"
    const val CLIENT_INSTANCE_NAME = "Aerix Client 1.16.5"

    // Locked Crimson Red + Dark Gray Theme (immutable, cannot be changed)
    const val LOCKED_CRIMSON_HEX = 0xFFDC1E3AL
    const val LOCKED_CRIMSON_GLOW_HEX = 0xFFFF2E4CL
    const val LOCKED_DARK_BASE_HEX = 0xFF141519L
    const val LOCKED_DARK_PANEL_HEX = 0xFF1C1E24L
    const val LOCKED_DARK_TILE_HEX = 0xFF252830L

    // Fresh v2 preferences namespace so ALL HUD items default to OFF and zero-overlap positions apply immediately
    /** The cape the previous build equipped for everybody on first launch. */
    private const val LEGACY_DEFAULT_CAPE = "crimson_sovereign"

    private const val PREFS_FILE = "aerix_client_v2_prefs"

    private var prefs: SharedPreferences? = null

    /* ── Selected Client Version in Launcher Tab ──────────────────────────── */
    var selectedClientVersion by mutableStateOf("1.16.5")

    /* ── Installer & Resource Pack Flags (locked defaults) ────────────────── */
    var installFpsStack by mutableStateOf(true)
    var installShadersAddon by mutableStateOf(true)
    var installSurvivalPolishAddon by mutableStateOf(true)
    var installZoomAddon by mutableStateOf(true)
    var installClientResourcePack by mutableStateOf(true)

    var packVanillaAnimatedGear by mutableStateOf(true)
    var packAnimateAllItemSprites by mutableStateOf(false)
    var packEnhancedGlint by mutableStateOf(true)
    var packGlintColor by mutableStateOf("CRIMSON")
    var packLowFire by mutableStateOf(true)
    var packShortSwords by mutableStateOf(false)
    var packLoweredShield by mutableStateOf(true)
    var packCompactTotem by mutableStateOf(true)
    var packOutlinedOres by mutableStateOf(false)
    var packClearWater by mutableStateOf(true)
    var packNoPumpkinBlur by mutableStateOf(true)
    var packBorderlessGlass by mutableStateOf(false)
    var packRgbBowStages by mutableStateOf(false)
    var packMiningProgressBar by mutableStateOf(false)
    var packCustomMainMenu by mutableStateOf(true)
    var packCustomMojangSplash by mutableStateOf(true)
    var packSkyboxTheme by mutableStateOf(SkyboxTheme.DEEP_VOID)
    var packHitSound by mutableStateOf(HitSoundPreset.VANILLA)

    /* ── Capes State (3D Character + Waving Cape + 15 Animated Capes) ─────── */

    /**
     * Nothing equipped: the player wears no cape unless their own account has one.
     *
     * This is the default. An Aerix cape used to be equipped out of the box, which put a cape on
     * every player who had never picked one - and, because a client cape outranks the account's
     * own, it also replaced the cape someone had genuinely equipped.
     */
    const val CAPE_NONE = "none"

    /**
     * Marks that the new-builds-with-no-cape decision has been applied to this install.
     *
     * Changing a default is not enough on its own: the previous build *saved* its default cape to
     * the preferences file, so a player who never opened the capes page still has
     * `equippedCapeId = "crimson_sovereign"` stored. Without this migration the new default only
     * ever applies to a fresh install - which is exactly how the cape kept coming back.
     */
    private const val MIGRATION_CAPE_DEFAULT = "migratedCapeDefaults"

    var equippedCapeId by mutableStateOf(CAPE_NONE)
    var wavingCapeEnabled by mutableStateOf(true)
    var animatedCapeShimmer by mutableStateOf(true)

    /* ── In-Game Client Modules — ALL HUD ITEMS OFF BY DEFAULT ────────────── */
    var crosshairEnabled by mutableStateOf(false)
    var crosshairColor by mutableStateOf("CRIMSON")
    var cooldownDurationMs by mutableIntStateOf(625)
    var crosshairShape by mutableStateOf(CrosshairShape.TACTICAL_GAP)
    var crosshairSize by mutableFloatStateOf(9f)
    var crosshairGap by mutableFloatStateOf(3.5f)
    var crosshairThickness by mutableFloatStateOf(2f)
    var crosshairOutline by mutableFloatStateOf(1.4f)
    var crosshairOpacity by mutableFloatStateOf(0.95f)
    var crosshairAttackPulse by mutableStateOf(false)

    /**
     * The launcher's frame rate pill, switched from inside the game. An Aerix Client instance has no
     * other FPS readout, so without this the player cannot ask for one - or get rid of one.
     */
    var fpsOverlayEnabled by mutableStateOf(true)
    var perfPillEnabled by mutableStateOf(false)
    var fpsHistoryEnabled by mutableStateOf(false)
    var frameTimingEnabled by mutableStateOf(false)
    var adaptiveDistanceEnabled by mutableStateOf(false)
    var adaptiveFpsTarget by mutableIntStateOf(60)
    var adaptiveMinDistance by mutableIntStateOf(4)
    var cpsPillEnabled by mutableStateOf(false)
    var keystrokesEnabled by mutableStateOf(false)
    var keystrokesStyle by mutableStateOf(KeystrokesStyle.CRIMSON_DARK)
    var swordCooldownBarEnabled by mutableStateOf(false)
    var hitmarkerEnabled by mutableStateOf(false)
    var comboCounterEnabled by mutableStateOf(false)

    var toggleSprintEnabled by mutableStateOf(false)
    var toggleSneakEnabled by mutableStateOf(false)
    var autoWalkEnabled by mutableStateOf(false)

    var coordsHudEnabled by mutableStateOf(false)
    var armorHudEnabled by mutableStateOf(false)
    var effectsHudEnabled by mutableStateOf(false)
    var worldTimeHudEnabled by mutableStateOf(false)
    var serverHudEnabled by mutableStateOf(false)
    var pingHudEnabled by mutableStateOf(false)
    var lightHudEnabled by mutableStateOf(false)
    var biomeHudEnabled by mutableStateOf(false)
    var clockHudEnabled by mutableStateOf(false)
    var waypointHudEnabled by mutableStateOf(false)
    var deathWaypointEnabled by mutableStateOf(false)
    var healthHudEnabled by mutableStateOf(false)
    var selectedSlotHudEnabled by mutableStateOf(false)
    var compassEnabled by mutableStateOf(false)
    var matchStatsEnabled by mutableStateOf(false)
    var speedrunTimerEnabled by mutableStateOf(false)

    var touchAimEnabled by mutableStateOf(false)
    var touchAimMultiplier by mutableFloatStateOf(1.0f)
    var gyroAdsEnabled by mutableStateOf(false)
    var gyroSensitivity by mutableFloatStateOf(1.0f)
    var touchCurveMode by mutableStateOf(TouchCurveMode.LINEAR_RAW)
    var palmShieldEnabled by mutableStateOf(false)
    var hotbarSwipeEnabled by mutableStateOf(false)
    var quickPvpButtonsEnabled by mutableStateOf(false)
    var quickFlickEnabled by mutableStateOf(true)
    var quickZoomEnabled by mutableStateOf(true)
    var quickOffhandEnabled by mutableStateOf(true)
    var quickPerspectiveEnabled by mutableStateOf(false)
    var quickScreenshotEnabled by mutableStateOf(false)
    var quickHitboxesEnabled by mutableStateOf(false)

    var fullbrightEnabled by mutableStateOf(false)
    /** Multiplayer-only performance profile the bridge mod applies live, inside a server. */
    var serverBoostEnabled by mutableStateOf(false)
    var serverBoostPreset by mutableStateOf("MEDIUM")
    var visualTimeMode by mutableStateOf("OFF")
    var visualWeatherMode by mutableStateOf("OFF")
    var visualFilter by mutableStateOf(VisualFilterPreset.NONE)

    var autoGgEnabled by mutableStateOf(false)
    var smartChatFilterEnabled by mutableStateOf(false)
    var streamerPrivacyEnabled by mutableStateOf(false)
    var thermalGuardEnabled by mutableStateOf(false)
    var queueAlarmEnabled by mutableStateOf(false)
    var queueAlertThreshold by mutableIntStateOf(5)

    var afkAction by mutableStateOf(AfkFarmAction.XP_GRINDER_SWING)
    var afkSwingIntervalMs by mutableLongStateOf(1250L)

    /* ── HUD Positions (100% Zero-Overlap Safe Slots) & Telemetry ─────────── */
    var hudPositions by mutableStateOf(defaultHudPositions())
    var waypoints by mutableStateOf<List<ClientWaypoint>>(emptyList())
    var chatShortcuts by mutableStateOf(listOf("gg", "glhf", "/spawn", "/hub"))

    var clientXp by mutableIntStateOf(0)
    var installsCompleted by mutableIntStateOf(0)
    var peakCpsRecord by mutableIntStateOf(0)
    var sessionWins by mutableIntStateOf(0)
    var sessionStreak by mutableIntStateOf(0)

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        prefs = p

        packVanillaAnimatedGear = p.getBoolean("packVanillaAnimatedGear", true)
        packAnimateAllItemSprites = p.getBoolean("packAnimateAllItemSprites", false)
        packEnhancedGlint = p.getBoolean("packEnhancedGlint", true)
        packGlintColor = p.getString("packGlintColor", "CRIMSON")?.takeIf { it in setOf("CRIMSON", "VIOLET", "BLUE", "GOLD", "GREEN", "WHITE") } ?: "CRIMSON"
        packLowFire = p.getBoolean("packLowFire", true)
        packLoweredShield = p.getBoolean("packLoweredShield", true)
        packCompactTotem = p.getBoolean("packCompactTotem", true)
        packClearWater = p.getBoolean("packClearWater", true)
        packNoPumpkinBlur = p.getBoolean("packNoPumpkinBlur", true)
        selectedClientVersion = p.getString("selectedClientVersion", "1.16.5") ?: "1.16.5"

        val storedCape = p.getString("equippedCapeId", CAPE_NONE) ?: CAPE_NONE
        equippedCapeId = if (p.getBoolean(MIGRATION_CAPE_DEFAULT, false)) {
            storedCape
        } else {
            // One-time: a cape this build chose for the player was never a choice, so it is
            // dropped. An imported cape ("custom") and a cape the player picked themselves are
            // indistinguishable from the old default in here, so they are kept - only the exact
            // value the old build wrote on first launch is treated as unchosen.
            val migrated = if (storedCape == LEGACY_DEFAULT_CAPE) CAPE_NONE else storedCape
            // Write the cleared value back as well as the flag, so the prefs file no longer holds a
            // cape the player never chose even if the flag is ever reset.
            p.edit()
                .putBoolean(MIGRATION_CAPE_DEFAULT, true)
                .putString("equippedCapeId", migrated)
                .apply()
            migrated
        }
        wavingCapeEnabled = p.getBoolean("wavingCapeEnabled", true)
        animatedCapeShimmer = true

        // Every HUD & gameplay overlay defaults strictly to FALSE unless explicitly enabled by user
        crosshairEnabled = p.getBoolean("crosshairEnabled", false)
        crosshairColor = p.getString("crosshairColor", "CRIMSON")?.takeIf { it in setOf("CRIMSON", "WHITE", "GREEN", "CYAN", "YELLOW") } ?: "CRIMSON"
        cooldownDurationMs = p.getInt("cooldownDurationMs", 625).coerceIn(250, 1500)
        crosshairShape = enumOrDefault(p.getString("crosshairShape", null), CrosshairShape.TACTICAL_GAP)
        crosshairSize = p.getFloat("crosshairSize", 9f)
        crosshairGap = p.getFloat("crosshairGap", 3.5f)
        crosshairThickness = p.getFloat("crosshairThickness", 2f)
        crosshairOutline = p.getFloat("crosshairOutline", 1.4f)
        crosshairOpacity = p.getFloat("crosshairOpacity", 0.95f)
        crosshairAttackPulse = p.getBoolean("crosshairAttackPulse", false)

        fpsOverlayEnabled = p.getBoolean("fpsOverlayEnabled", true)
        perfPillEnabled = p.getBoolean("perfPillEnabled", false)
        fpsHistoryEnabled = p.getBoolean("fpsHistoryEnabled", false)
        frameTimingEnabled = p.getBoolean("frameTimingEnabled", false)
        adaptiveDistanceEnabled = p.getBoolean("adaptiveDistanceEnabled", false)
        adaptiveFpsTarget = p.getInt("adaptiveFpsTarget", 60).coerceIn(20, 120)
        adaptiveMinDistance = p.getInt("adaptiveMinDistance", 4).coerceIn(2, 16)
        cpsPillEnabled = p.getBoolean("cpsPillEnabled", false)
        keystrokesEnabled = p.getBoolean("keystrokesEnabled", false)
        swordCooldownBarEnabled = p.getBoolean("swordCooldownBarEnabled", false)
        hitmarkerEnabled = p.getBoolean("hitmarkerEnabled", false)
        comboCounterEnabled = p.getBoolean("comboCounterEnabled", false)

        toggleSprintEnabled = p.getBoolean("toggleSprintEnabled", false)
        toggleSneakEnabled = p.getBoolean("toggleSneakEnabled", false)
        autoWalkEnabled = p.getBoolean("autoWalkEnabled", false)

        coordsHudEnabled = p.getBoolean("coordsHudEnabled", false)
        armorHudEnabled = p.getBoolean("armorHudEnabled", false)
        effectsHudEnabled = p.getBoolean("effectsHudEnabled", false)
        worldTimeHudEnabled = p.getBoolean("worldTimeHudEnabled", false)
        serverHudEnabled = p.getBoolean("serverHudEnabled", false)
        pingHudEnabled = p.getBoolean("pingHudEnabled", false)
        lightHudEnabled = p.getBoolean("lightHudEnabled", false)
        biomeHudEnabled = p.getBoolean("biomeHudEnabled", false)
        clockHudEnabled = p.getBoolean("clockHudEnabled", false)
        waypointHudEnabled = p.getBoolean("waypointHudEnabled", false)
        deathWaypointEnabled = p.getBoolean("deathWaypointEnabled", false)
        healthHudEnabled = p.getBoolean("healthHudEnabled", false)
        selectedSlotHudEnabled = p.getBoolean("selectedSlotHudEnabled", false)
        loadWaypoints(p.getString("clientWaypoints", null))
        chatShortcuts = runCatching {
            val arr = JSONArray(p.getString("clientChatShortcuts", "[\"gg\",\"glhf\",\"/spawn\",\"/hub\"]"))
            (0 until arr.length().coerceAtMost(12)).map { arr.getString(it) }
        }.getOrDefault(listOf("gg", "glhf", "/spawn", "/hub"))
        compassEnabled = p.getBoolean("compassEnabled", false)
        matchStatsEnabled = p.getBoolean("matchStatsEnabled", false)
        speedrunTimerEnabled = p.getBoolean("speedrunTimerEnabled", false)

        touchAimEnabled = p.getBoolean("touchAimEnabled", false)
        touchAimMultiplier = p.getFloat("touchAimMultiplier", 1.0f).coerceIn(0.25f, 2f)
        gyroAdsEnabled = p.getBoolean("gyroAdsEnabled", false)
        gyroSensitivity = p.getFloat("gyroSensitivity", 1.0f)
        palmShieldEnabled = p.getBoolean("palmShieldEnabled", false)
        hotbarSwipeEnabled = p.getBoolean("hotbarSwipeEnabled", false)
        quickPvpButtonsEnabled = p.getBoolean("quickPvpButtonsEnabled", false)
        quickFlickEnabled = p.getBoolean("quickFlickEnabled", true)
        quickZoomEnabled = p.getBoolean("quickZoomEnabled", true)
        quickOffhandEnabled = p.getBoolean("quickOffhandEnabled", true)
        quickPerspectiveEnabled = p.getBoolean("quickPerspectiveEnabled", false)
        quickScreenshotEnabled = p.getBoolean("quickScreenshotEnabled", false)
        quickHitboxesEnabled = p.getBoolean("quickHitboxesEnabled", false)

        fullbrightEnabled = p.getBoolean("fullbrightEnabled", false)
        serverBoostEnabled = p.getBoolean("serverBoostEnabled", false)
        serverBoostPreset = p.getString("serverBoostPreset", "MEDIUM")
            ?.takeIf { it in setOf("LIGHT", "MEDIUM", "EXTREME") } ?: "MEDIUM"
        visualTimeMode = p.getString("visualTimeMode", "OFF")?.takeIf { it in setOf("OFF", "DAY", "SUNSET", "NIGHT") } ?: "OFF"
        visualWeatherMode = p.getString("visualWeatherMode", "OFF")?.takeIf { it in setOf("OFF", "CLEAR", "RAIN") } ?: "OFF"
        autoGgEnabled = p.getBoolean("autoGgEnabled", false)
        streamerPrivacyEnabled = p.getBoolean("streamerPrivacyEnabled", false)
        thermalGuardEnabled = p.getBoolean("thermalGuardEnabled", false)
        queueAlarmEnabled = p.getBoolean("queueAlarmEnabled", false)

        peakCpsRecord = p.getInt("peakCpsRecord", 0)
        installsCompleted = p.getInt("installsCompleted", 0)

        loadHudPositions(p.getString("hudPositionsJson", null))
    }

    /** Reload in the game process after a validated settings import. */
    fun reload(context: Context) {
        prefs = null
        init(context)
    }

    fun persist() {
        val p = prefs ?: return
        p.edit().apply {
            putBoolean("packVanillaAnimatedGear", packVanillaAnimatedGear)
            putBoolean("packAnimateAllItemSprites", packAnimateAllItemSprites)
            putBoolean("packEnhancedGlint", packEnhancedGlint)
            putString("packGlintColor", packGlintColor)
            putBoolean("packLowFire", packLowFire)
            putBoolean("packLoweredShield", packLoweredShield)
            putBoolean("packCompactTotem", packCompactTotem)
            putBoolean("packClearWater", packClearWater)
            putBoolean("packNoPumpkinBlur", packNoPumpkinBlur)
            putString("selectedClientVersion", selectedClientVersion)

            putString("equippedCapeId", equippedCapeId)
            putBoolean("wavingCapeEnabled", wavingCapeEnabled)

            putBoolean("crosshairEnabled", crosshairEnabled)
            putString("crosshairColor", crosshairColor)
            putInt("cooldownDurationMs", cooldownDurationMs)
            putString("crosshairShape", crosshairShape.name)
            putFloat("crosshairSize", crosshairSize)
            putFloat("crosshairGap", crosshairGap)
            putFloat("crosshairThickness", crosshairThickness)
            putFloat("crosshairOutline", crosshairOutline)
            putFloat("crosshairOpacity", crosshairOpacity)
            putBoolean("crosshairAttackPulse", crosshairAttackPulse)

            putBoolean("fpsOverlayEnabled", fpsOverlayEnabled)
            putBoolean("perfPillEnabled", perfPillEnabled)
            putBoolean("fpsHistoryEnabled", fpsHistoryEnabled)
            putBoolean("frameTimingEnabled", frameTimingEnabled)
            putBoolean("adaptiveDistanceEnabled", adaptiveDistanceEnabled)
            putInt("adaptiveFpsTarget", adaptiveFpsTarget)
            putInt("adaptiveMinDistance", adaptiveMinDistance)
            putBoolean("cpsPillEnabled", cpsPillEnabled)
            putBoolean("keystrokesEnabled", keystrokesEnabled)
            putBoolean("swordCooldownBarEnabled", swordCooldownBarEnabled)
            putBoolean("hitmarkerEnabled", hitmarkerEnabled)
            putBoolean("comboCounterEnabled", comboCounterEnabled)

            putBoolean("toggleSprintEnabled", toggleSprintEnabled)
            putBoolean("toggleSneakEnabled", toggleSneakEnabled)
            putBoolean("autoWalkEnabled", autoWalkEnabled)

            putBoolean("coordsHudEnabled", coordsHudEnabled)
            putBoolean("armorHudEnabled", armorHudEnabled)
            putBoolean("effectsHudEnabled", effectsHudEnabled)
            putBoolean("worldTimeHudEnabled", worldTimeHudEnabled)
            putBoolean("serverHudEnabled", serverHudEnabled)
            putBoolean("pingHudEnabled", pingHudEnabled)
            putBoolean("lightHudEnabled", lightHudEnabled)
            putBoolean("biomeHudEnabled", biomeHudEnabled)
            putBoolean("clockHudEnabled", clockHudEnabled)
            putBoolean("waypointHudEnabled", waypointHudEnabled)
            putBoolean("deathWaypointEnabled", deathWaypointEnabled)
            putBoolean("healthHudEnabled", healthHudEnabled)
            putBoolean("selectedSlotHudEnabled", selectedSlotHudEnabled)
            putString("clientWaypoints", encodeWaypoints())
            putString("clientChatShortcuts", JSONArray(chatShortcuts).toString())
            putBoolean("compassEnabled", compassEnabled)
            putBoolean("matchStatsEnabled", matchStatsEnabled)
            putBoolean("speedrunTimerEnabled", speedrunTimerEnabled)

            putBoolean("touchAimEnabled", touchAimEnabled)
            putFloat("touchAimMultiplier", touchAimMultiplier)
            putBoolean("gyroAdsEnabled", gyroAdsEnabled)
            putFloat("gyroSensitivity", gyroSensitivity)
            putBoolean("palmShieldEnabled", palmShieldEnabled)
            putBoolean("hotbarSwipeEnabled", hotbarSwipeEnabled)
            putBoolean("quickPvpButtonsEnabled", quickPvpButtonsEnabled)
            putBoolean("quickFlickEnabled", quickFlickEnabled)
            putBoolean("quickZoomEnabled", quickZoomEnabled)
            putBoolean("quickOffhandEnabled", quickOffhandEnabled)
            putBoolean("quickPerspectiveEnabled", quickPerspectiveEnabled)
            putBoolean("quickScreenshotEnabled", quickScreenshotEnabled)
            putBoolean("quickHitboxesEnabled", quickHitboxesEnabled)

            putBoolean("fullbrightEnabled", fullbrightEnabled)
            putBoolean("serverBoostEnabled", serverBoostEnabled)
            putString("serverBoostPreset", serverBoostPreset)
            putString("visualTimeMode", visualTimeMode)
            putString("visualWeatherMode", visualWeatherMode)
            putBoolean("autoGgEnabled", autoGgEnabled)
            putBoolean("streamerPrivacyEnabled", streamerPrivacyEnabled)
            putBoolean("thermalGuardEnabled", thermalGuardEnabled)
            putBoolean("queueAlarmEnabled", queueAlarmEnabled)

            putInt("peakCpsRecord", peakCpsRecord)
            putInt("installsCompleted", installsCompleted)

            putString("hudPositionsJson", encodeHudPositions())
            apply()
        }
    }

    fun awardXp(points: Int) {
        clientXp = (clientXp + points).coerceAtLeast(0)
    }

    /** Locked Crimson Red theme color — immutable across Aerix Client. */
    fun activeThemeColor(@Suppress("UNUSED_PARAMETER") nowMs: Long = 0L): Color =
        Color(LOCKED_CRIMSON_HEX)

    fun updateWidgetPos(id: String, xNorm: Float, yNorm: Float, scale: Float? = null) {
        hudPositions = hudPositions.toMutableMap().apply {
            val cur = this[id] ?: return@apply
            this[id] = cur.copy(
                xNorm = xNorm.coerceIn(0.01f, 0.92f),
                yNorm = yNorm.coerceIn(0.015f, 0.90f),
                scale = (scale ?: cur.scale).coerceIn(0.65f, 1.45f)
            )
        }
        persist()
    }

    fun scaleWidget(id: String, delta: Float) {
        val pos = hudPositions[id] ?: return
        updateWidgetPos(id, pos.xNorm, pos.yNorm, pos.scale + delta)
    }

    fun scaleHud(delta: Float) {
        hudPositions = hudPositions.mapValues { (id, pos) ->
            if (id == "client_btn") pos else pos.copy(scale = (pos.scale + delta).coerceIn(0.65f, 1.45f))
        }
        persist()
    }

    fun resetHudPositions() {
        hudPositions = defaultHudPositions()
        persist()
    }

    /**
     * Suggested default HUD slots for the default control layout; custom layouts may differ:
     * - Top-left DEBUG|CHAT|KEYBOARD|TAB (`0.04..0.46, 0.00..0.09`) & 3RD (`0.04..0.14, 0.09..0.18`) untouched.
     * - Top-right `A` button (`~0.77, 0.03`) & MOUSE (`0.89..1.00, 0.00..0.09`) untouched.
     * - Left D-pad / PRI / SEC / GUI / INV (`0.04..0.25, 0.54..0.98`) untouched.
     * - Bottom-center Vanilla 9-slot Hotbar & Hearts (`0.30..0.70, 0.78..1.00`) untouched.
     */
    private fun defaultHudPositions(): Map<String, HudWidgetPos> = mapOf(
        "client_btn" to HudWidgetPos("client_btn", "Client Menu Button", 0.57f, 0.018f, 1.0f),
        "perf" to HudWidgetPos("perf", "FPS & Telemetry", 0.02f, 0.21f, 1.0f),
        "cps" to HudWidgetPos("cps", "CPS Counter", 0.22f, 0.21f, 1.0f),
        "keystrokes" to HudWidgetPos("keystrokes", "Keystrokes", 0.02f, 0.29f, 1.0f),
        "info" to HudWidgetPos("info", "Information HUD", 0.30f, 0.25f, 1.0f),
        "compass" to HudWidgetPos("compass", "Direction Compass", 0.39f, 0.11f, 1.0f),
        "match" to HudWidgetPos("match", "Session Timer", 0.83f, 0.13f, 1.0f),
        "speedrun" to HudWidgetPos("speedrun", "Speedrun Stopwatch", 0.79f, 0.21f, 1.0f),
        "quick_pvp" to HudWidgetPos("quick_pvp", "PvP Action Pills", 0.88f, 0.33f, 1.0f),
        "hotbar_swipe" to HudWidgetPos("hotbar_swipe", "Touch Hotbar Strip", 0.35f, 0.70f, 1.0f)
    )

    private inline fun <reified E : Enum<E>> enumOrDefault(raw: String?, fallback: E): E {
        if (raw.isNullOrBlank()) return fallback
        return enumValues<E>().firstOrNull { it.name == raw } ?: fallback
    }

    private fun encodeWaypoints(): String {
        val array = JSONArray()
        waypoints.forEach { waypoint ->
            array.put(JSONObject().put("id", waypoint.id).put("name", waypoint.name)
                .put("x", waypoint.x).put("y", waypoint.y).put("z", waypoint.z)
                .put("dimension", waypoint.dimension))
        }
        return array.toString()
    }

    private fun loadWaypoints(raw: String?) {
        waypoints = runCatching {
            val array = JSONArray(raw ?: "[]")
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                ClientWaypoint(obj.optString("id"), obj.optString("name", "Waypoint"),
                    obj.optInt("x"), obj.optInt("y"), obj.optInt("z"), obj.optString("dimension"))
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeHudPositions(): String {
        val arr = JSONArray()
        hudPositions.values.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("label", p.label)
                    .put("x", p.xNorm.toDouble())
                    .put("y", p.yNorm.toDouble())
                    .put("scale", p.scale.toDouble())
            )
        }
        return arr.toString()
    }

    private fun loadHudPositions(raw: String?) {
        val defaults = defaultHudPositions().toMutableMap()
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    val cur = defaults[id] ?: continue
                    defaults[id] = cur.copy(
                        xNorm = o.optDouble("x", cur.xNorm.toDouble()).toFloat().coerceIn(0.01f, 0.92f),
                        yNorm = o.optDouble("y", cur.yNorm.toDouble()).toFloat().coerceIn(0.015f, 0.90f),
                        scale = o.optDouble("scale", cur.scale.toDouble()).toFloat().coerceIn(0.65f, 1.45f)
                    )
                }
            }
        }
        hudPositions = defaults
    }
}
