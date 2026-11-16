package net.kdt.pojavlaunch.aerix.client

import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.components.bringFocusedItemIntoView
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.theme.AerixLauncherTheme
import net.kdt.pojavlaunch.game.NativeFrameCounter
import net.kdt.pojavlaunch.game.FrameTimingStats
import net.kdt.pojavlaunch.game.platform.Platform
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener
import net.kdt.pojavlaunch.instances.Instance
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class InGameClientModal {
    NONE,
    MODULES,
    HUD_EDITOR,
    OLED_AFK
}

private enum class ClientMenuSubPage {
    GRID,
    CAPES,
    CROSSHAIRS,
    PERFORMANCE,
    UTILITY,
    INFORMATION,
    WAYPOINTS,
    VISUALS,
    GAME_OPTIONS,
    CHAT_SHORTCUTS,
    AIM,
    PACKS,
    KEY_PROFILES,
    TRANSFER,
    GALLERY,
    WORLD_SHARING,
    SOCIALS
}

private enum class FeatureIconKind {
    CAPE,
    CROSSHAIR,
    FPS,
    CPS,
    KEYSTROKES,
    SPRINT,
    SNEAK,
    AUTOWALK,
    FULLBRIGHT,
    HITMARKER,
    COOLDOWN,
    COMPASS,
    CLOCK,
    SPEEDRUN,
    CLAW_PVP,
    HOTBAR,
    GYRO,
    AUTOGG,
    THERMAL,
    OLED_AFK
}

class AerixClientOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), SensorEventListener {

    private val composeView = ComposeView(context).apply {
        isFocusable = false
        isFocusableInTouchMode = false
    }

    private var contentInstalled = false
    private var activeClientInstance: Instance? = null

    private var bootOverlayVisible by mutableStateOf(true)
    private var activeModal by mutableStateOf(InGameClientModal.NONE)
    private var menuSubPage by mutableStateOf(ClientMenuSubPage.GRID)
    private var isGrabbingState by mutableStateOf(false)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var gyroListening = false

    private val grabListener = PlatformGrabListener { grabbing ->
        isGrabbingState = grabbing
    }

    init {
        visibility = GONE
        isClickable = false
        isFocusable = false
        setWillNotDraw(true)
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        AerixPreferences.init(context.applicationContext)
        AerixClientStore.init(context.applicationContext)
        ClientPerformanceSettings.init(context.applicationContext)
        PojavAccountBridge.attach(context.applicationContext)
    }

    fun attachForInstance(instance: Instance?) {
        if (instance == null || !instance.isAerixClient) {
            visibility = GONE
            AerixClientInputBridge.clientSessionActive = false
            return
        }
        activeClientInstance = instance
        ClientLiveOptionsBridge.activeGameDir = instance.gameDirectory
        AerixClientStore.reload(context.applicationContext)
        ClientPerformanceSettings.reload(context.applicationContext)
        AerixClientInputBridge.resetSessionState(instance.versionId)
        AerixClientInputBridge.clientSessionActive = true
        bootOverlayVisible = true
        activeModal = InGameClientModal.NONE
        menuSubPage = ClientMenuSubPage.GRID
        isGrabbingState = Platform.isGrabbing()
        Platform.addGrabListener(grabListener)
        installComposeContent()
        visibility = VISIBLE
        syncGyroRegistration()
    }

    fun isClientActive(): Boolean = visibility == VISIBLE

    fun openClientMenu() {
        if (visibility != VISIBLE) return
        bootOverlayVisible = false
        menuSubPage = ClientMenuSubPage.GRID
        AerixOverlayInput.noteOpened()
        activeModal = InGameClientModal.MODULES
    }

    fun onGlWindowReady() {
        if (visibility != VISIBLE) return
        postDelayed({
            bootOverlayVisible = false
        }, 700L)
    }

    fun onHostDestroy() {
        activeClientInstance?.gameDirectory?.let { runCatching { ClientTelemetryBridge.setEnabled(it, false) } }
        ClientLiveOptionsBridge.activeGameDir = null
        AerixClientInputBridge.clientSessionActive = false
        unregisterGyro()
    }

    fun handleBackPress(): Boolean {
        if (visibility != VISIBLE) return false
        if (activeModal == InGameClientModal.MODULES && menuSubPage != ClientMenuSubPage.GRID) {
            menuSubPage = ClientMenuSubPage.GRID
            return true
        }
        if (activeModal != InGameClientModal.NONE) {
            // A tap on the Client button can reach the game as an escape key as well as a touch, in
            // which case the panel that the tap opened is closed by the very same tap. Key and tap
            // share one grace window, so the menu only answers BACK/ESC once it has been on screen.
            if (!AerixOverlayInput.acceptKeyDismiss()) return true
            if (activeModal == InGameClientModal.OLED_AFK) {
                AerixClientInputBridge.setRightClickHold(false)
            }
            activeModal = InGameClientModal.NONE
            return true
        }
        return false
    }

    fun isBlockingModalOpen(): Boolean =
        visibility == VISIBLE && (bootOverlayVisible || activeModal != InGameClientModal.NONE)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (visibility != VISIBLE) return false
        if (isBlockingModalOpen()) {
            super.dispatchTouchEvent(ev)
            return true
        }
        if (AerixClientStore.palmShieldEnabled && isGrabbingState && width > 0 && height > 0) {
            val edgePx = 18f * resources.displayMetrics.density
            val inBottomCorner = ev.y > (height - edgePx * 2.2f) &&
                (ev.x < edgePx || ev.x > (width - edgePx))
            if (inBottomCorner) return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun syncGyroRegistration() {
        val want = visibility == VISIBLE && AerixClientStore.gyroAdsEnabled
        if (want && !gyroListening && gyroSensor != null) {
            gyroListening = true
            sensorManager?.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (!want && gyroListening) {
            unregisterGyro()
        }
    }

    private fun unregisterGyro() {
        if (gyroListening) {
            gyroListening = false
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !AerixClientStore.gyroAdsEnabled || !isGrabbingState) return
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val dyaw = -event.values[0]
            val dpitch = event.values[1]
            if (abs(dyaw) > 0.015f || abs(dpitch) > 0.015f) {
                AerixClientInputBridge.applyGyroDelta(dyaw, dpitch)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun installComposeContent() {
        if (contentInstalled) return
        contentInstalled = true
        val host = rootView
        findOwnerIn<LifecycleOwner>(context)?.let { owner ->
            if (host.findViewTreeLifecycleOwner() == null) host.setViewTreeLifecycleOwner(owner)
        }
        findOwnerIn<SavedStateRegistryOwner>(context)?.let { owner ->
            if (host.findViewTreeSavedStateRegistryOwner() == null) host.setViewTreeSavedStateRegistryOwner(owner)
        }
        findOwnerIn<ViewModelStoreOwner>(context)?.let { owner ->
            if (host.findViewTreeViewModelStoreOwner() == null) host.setViewTreeViewModelStoreOwner(owner)
        }
        findOwnerIn<OnBackPressedDispatcherOwner>(context)?.let { owner ->
            if (host.findViewTreeOnBackPressedDispatcherOwner() == null) {
                host.setViewTreeOnBackPressedDispatcherOwner(owner)
            }
        }
        composeView.setContent {
            AerixLauncherTheme {
                val worldSharingTarget = remember(activeClientInstance) {
                    net.kdt.pojavlaunch.aerix.bridge.GameDownloadTarget.of(activeClientInstance)
                }
                AerixClientRootOverlay(
                    worldSharingTarget = worldSharingTarget,
                    mcVersion = AerixClientPackGenerator.resolveMcVersionFromInstance(activeClientInstance?.versionId, AerixClientStore.selectedClientVersion),
                    gameDir = activeClientInstance?.gameDirectory,
                    bootVisible = bootOverlayVisible,
                    isGrabbing = isGrabbingState,
                    modal = activeModal,
                    subPage = menuSubPage,
                    onChangeSubPage = { menuSubPage = it },
                    onChangeModal = { next ->
                        if (activeModal == InGameClientModal.OLED_AFK && next != InGameClientModal.OLED_AFK) {
                            AerixClientInputBridge.setRightClickHold(false)
                        }
                        if (next == InGameClientModal.MODULES) {
                            menuSubPage = ClientMenuSubPage.GRID
                        }
                        if (next != InGameClientModal.NONE) {
                            AerixOverlayInput.noteOpened()
                        }
                        activeModal = next
                        syncGyroRegistration()
                    },
                    onSyncOptionsTxt = {
                        activeClientInstance?.let { inst ->
                            val version = AerixClientPackGenerator.resolveMcVersionFromInstance(inst.versionId, AerixClientStore.selectedClientVersion)
                            AerixClientPackGenerator.applyClientOptionsTxt(inst.gameDirectory, version)
                        }
                        syncGyroRegistration()
                    },
                    onSyncCape = {
                        activeClientInstance?.gameDirectory?.let { dir ->
                            AerixClientCosmetics.syncCapeAssetsToInstance(context, dir)
                        }
                    }
                )
            }
        }
    }

    private inline fun <reified T> findOwnerIn(context: Context?): T? {
        var candidate = context
        while (candidate != null) {
            if (candidate is T) return candidate
            candidate = (candidate as? ContextWrapper)?.baseContext
        }
        return null
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * ROOT IN-GAME CLIENT OVERLAY (LOCKED CRIMSON RED + DARK GRAY THEME)
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun AerixClientRootOverlay(
    worldSharingTarget: net.kdt.pojavlaunch.aerix.bridge.GameDownloadTarget,
    gameDir: File?,
    bootVisible: Boolean,
    isGrabbing: Boolean,
    mcVersion: String,
    modal: InGameClientModal,
    subPage: ClientMenuSubPage,
    onChangeSubPage: (ClientMenuSubPage) -> Unit,
    onChangeModal: (InGameClientModal) -> Unit,
    onSyncOptionsTxt: () -> Unit,
    onSyncCape: () -> Unit
) {
    val context = LocalContext.current

    var currentFps by remember { mutableIntStateOf(0) }
    var fpsHistory by remember { mutableStateOf<List<Int>>(emptyList()) }
    var frameTiming by remember { mutableStateOf<FrameTimingStats?>(null) }
    var previousTiming by remember { mutableStateOf<LongArray?>(null) }
    DisposableEffect(AerixClientStore.frameTimingEnabled) {
        previousTiming = null
        frameTiming = null
        NativeFrameCounter.setTimingEnabled(AerixClientStore.frameTimingEnabled)
        onDispose { NativeFrameCounter.setTimingEnabled(false) }
    }
    var onePercentLowFps by remember { mutableIntStateOf(0) }
    var usedRamMb by remember { mutableIntStateOf(0) }
    var batteryPct by remember { mutableIntStateOf(100) }
    var batteryTempC by remember { mutableFloatStateOf(32f) }
    var sessionElapsedSec by remember { mutableLongStateOf(0L) }
    var frameTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var telemetry by remember { mutableStateOf<ClientTelemetrySnapshot?>(null) }
    var wallClock by remember { mutableStateOf("") }
    val needsTelemetry = (mcVersion == "26.3" && (AerixClientStore.perfPillEnabled || net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SHOW_FPS || AerixClientStore.fpsHistoryEnabled || AerixClientStore.adaptiveDistanceEnabled || modal == InGameClientModal.OLED_AFK)) || AerixClientStore.coordsHudEnabled || AerixClientStore.armorHudEnabled ||
        AerixClientStore.effectsHudEnabled || AerixClientStore.worldTimeHudEnabled ||
        AerixClientStore.serverHudEnabled || AerixClientStore.pingHudEnabled || AerixClientStore.lightHudEnabled || AerixClientStore.biomeHudEnabled ||
        AerixClientStore.compassEnabled || AerixClientStore.waypointHudEnabled || AerixClientStore.deathWaypointEnabled || AerixClientStore.healthHudEnabled || AerixClientStore.selectedSlotHudEnabled || AerixClientStore.hotbarSwipeEnabled || AerixClientStore.adaptiveDistanceEnabled || ClientAdaptiveBridge.recovering ||
        (modal == InGameClientModal.MODULES && subPage == ClientMenuSubPage.WAYPOINTS)
    val telemetryRequested by rememberUpdatedState(needsTelemetry)
    LaunchedEffect(gameDir) {
        if (gameDir == null) return@LaunchedEffect
        var enabled = false
        try {
            while (true) {
                if (enabled != telemetryRequested) {
                    val target = telemetryRequested
                    enabled = withContext(Dispatchers.IO) {
                        runCatching { ClientTelemetryBridge.setEnabled(gameDir, target); target }.getOrDefault(false)
                    }
                }
                telemetry = if (enabled) withContext(Dispatchers.IO) { ClientTelemetryBridge.read(gameDir) } else null
                telemetry?.yaw?.let { yaw -> AerixClientInputBridge.headingYawDeg = ((yaw % 360f) + 360f) % 360f }
                telemetry?.selectedSlot?.let { slot -> AerixClientInputBridge.syncHotbarSlot(slot) }
                delay(1000L)
            }
        } finally {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { runCatching { ClientTelemetryBridge.setEnabled(gameDir, false) } }
        }
    }
    val adaptiveEnabled by rememberUpdatedState(AerixClientStore.adaptiveDistanceEnabled)
    val adaptiveMinimum by rememberUpdatedState(AerixClientStore.adaptiveMinDistance)
    val adaptiveTarget by rememberUpdatedState(AerixClientStore.adaptiveFpsTarget)
    val latestFps by rememberUpdatedState(currentFps)
    val latestGameData by rememberUpdatedState(telemetry)
    val gameIsGrabbing by rememberUpdatedState(isGrabbing)
    LaunchedEffect(gameDir) {
        if (gameDir == null) return@LaunchedEffect
        var controller: AdaptiveDistanceController? = null
        var original: Int? = null
        var lastSample = 0L
        var active = false
        var restoring = false
        var restoreStarted = 0L
        try {
            while (true) {
                if (!adaptiveEnabled && active) {
                    withContext(Dispatchers.IO) { ClientAdaptiveBridge.request(gameDir, -1) }
                    active = false
                    restoring = true
                    restoreStarted = android.os.SystemClock.elapsedRealtime()
                    ClientAdaptiveBridge.recovering = true
                    ClientAdaptiveBridge.status = "Restoring original distance"
                }
                if (restoring) {
                    if (android.os.SystemClock.elapsedRealtime() - restoreStarted >= 4_000L && latestGameData?.renderDistance == original) {
                        withContext(Dispatchers.IO) { ClientAdaptiveBridge.clearRecovery(gameDir) }
                        restoring = false
                        ClientAdaptiveBridge.recovering = false
                        controller = null
                        original = null
                        ClientAdaptiveBridge.status = "Original distance restored"
                    }
                } else if (!adaptiveEnabled) {
                    controller = null
                    ClientAdaptiveBridge.status = "Off"
                } else {
                    val data = latestGameData
                    if (controller == null) {
                        val initial = data?.renderDistance
                        if (initial != null && initial in 2..64 && latestFps > 0) {
                            controller = AdaptiveDistanceController(initial, adaptiveMinimum, adaptiveTarget)
                            original = initial
                            withContext(Dispatchers.IO) {
                                ClientAdaptiveBridge.saveRecovery(gameDir, initial)
                                ClientAdaptiveBridge.request(gameDir, initial)
                            }
                            active = true
                            lastSample = android.os.SystemClock.elapsedRealtime()
                            ClientAdaptiveBridge.status = "Requested $initial chunks · waiting for game confirmation"
                        } else ClientAdaptiveBridge.status = "Waiting for FPS and game options"
                    } else if (gameIsGrabbing && android.os.SystemClock.elapsedRealtime() - lastSample >= 4_000L) {
                        val control = controller ?: continue
                        val before = control.current()
                        control.configure(adaptiveMinimum, adaptiveTarget)
                        val requested = control.sample(latestFps)
                        if (requested != before) withContext(Dispatchers.IO) { ClientAdaptiveBridge.request(gameDir, requested) }
                        ClientAdaptiveBridge.status = if (data?.renderDistance == requested) "Game reports $requested chunks" else "Requested $requested chunks · awaiting game"
                        lastSample = android.os.SystemClock.elapsedRealtime()
                    }
                }
                delay(1000L)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ClientAdaptiveBridge.status = "Unavailable: ${error.message ?: "file error"}"
        } finally {
            ClientAdaptiveBridge.recovering = false
            if (active) withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { runCatching { ClientAdaptiveBridge.request(gameDir, -1) } }
        }
    }
    var wasAlive by remember { mutableStateOf(false) }
    LaunchedEffect(telemetry, AerixClientStore.deathWaypointEnabled) {
        val data = telemetry
        if (!AerixClientStore.deathWaypointEnabled || data == null || data.health == null) {
            wasAlive = false
        } else if (data.health > 0f) {
            wasAlive = true
        } else if (wasAlive) {
            wasAlive = false
            val current = AerixClientStore.waypoints.filterNot { it.id == "last_death" }
            if (current.size < 30) {
                AerixClientStore.waypoints = current + ClientWaypoint("last_death", "Last death", data.blockX, data.blockY, data.blockZ, data.dimension)
                AerixClientStore.persist()
            }
        }
    }
    LaunchedEffect(AerixClientStore.clockHudEnabled) {
        while (AerixClientStore.clockHudEnabled) {
            wallClock = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            delay(1000L)
        }
    }

    var speedrunRunning by remember { mutableStateOf(false) }
    var speedrunAccumMs by remember { mutableLongStateOf(0L) }
    var speedrunStartedAtMs by remember { mutableLongStateOf(0L) }
    var speedrunLastSplitText by remember { mutableStateOf("Ready") }

    LaunchedEffect(Unit) {
        val startWall = System.currentTimeMillis()
        var lastNativeFrames = -1L
        var lastSampleNanos = 0L
        val recentFpsWindow = ArrayDeque<Int>(20)
        var slowTick = 0

        while (true) {
            val nowMs = System.currentTimeMillis()
            val combatTimerActive = AerixClientStore.hitmarkerEnabled || AerixClientStore.swordCooldownBarEnabled || AerixClientStore.speedrunTimerEnabled
            if (combatTimerActive) frameTickMs = nowMs
            if (AerixClientStore.cpsPillEnabled || AerixClientStore.keystrokesEnabled) AerixClientInputBridge.refreshCps(nowMs)
            if (AerixClientStore.matchStatsEnabled) sessionElapsedSec = (nowMs - startWall) / 1000L

            slowTick++
            if (slowTick % 10 == 0) {
                val totalFrames = if (mcVersion != "26.3" && (AerixClientStore.perfPillEnabled || AerixClientStore.fpsHistoryEnabled || AerixClientStore.frameTimingEnabled || AerixClientStore.adaptiveDistanceEnabled || modal == InGameClientModal.OLED_AFK)) NativeFrameCounter.read() else -1L
                val nowNanos = System.nanoTime()
                if (totalFrames >= 0) {
                    if (lastNativeFrames >= 0 && nowNanos > lastSampleNanos) {
                        val deltaFrames = (totalFrames - lastNativeFrames) and 0xffffffffL
                        val elapsedNs = nowNanos - lastSampleNanos
                        val instantFps = ((deltaFrames * 1_000_000_000.0) / elapsedNs).roundToInt().coerceAtLeast(0)
                        currentFps = instantFps
                        if (AerixClientStore.fpsHistoryEnabled) fpsHistory = (fpsHistory + instantFps).takeLast(60) else fpsHistory = emptyList()
                        if (recentFpsWindow.size >= 20) recentFpsWindow.removeFirst()
                        recentFpsWindow.addLast(instantFps)
                        onePercentLowFps = recentFpsWindow.minOrNull() ?: instantFps
                    }
                    lastNativeFrames = totalFrames
                    lastSampleNanos = nowNanos
                } else {
                    lastNativeFrames = -1L
                    lastSampleNanos = 0L
                    val gameFps = if (mcVersion == "26.3") telemetry?.fps else null
                    if (gameFps != null) {
                        currentFps = gameFps
                        if (AerixClientStore.fpsHistoryEnabled) fpsHistory = (fpsHistory + gameFps).takeLast(60) else fpsHistory = emptyList()
                        if (recentFpsWindow.size >= 20) recentFpsWindow.removeFirst()
                        recentFpsWindow.addLast(gameFps)
                        onePercentLowFps = recentFpsWindow.minOrNull() ?: gameFps
                    } else if (mcVersion == "26.3") currentFps = 0
                }
                if (AerixClientStore.frameTimingEnabled && mcVersion != "26.3") {
                    val current = NativeFrameCounter.timingHistogram()
                    frameTiming = FrameTimingStats.calculate(previousTiming, current)
                    previousTiming = current
                }
                AerixClientInputBridge.pollLatestLog(context)
            }

            if (slowTick % 50 == 1 && (AerixClientStore.perfPillEnabled || AerixClientStore.fpsHistoryEnabled || AerixClientStore.frameTimingEnabled || AerixClientStore.thermalGuardEnabled || modal == InGameClientModal.OLED_AFK)) {
                runCatching {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    if (am != null) {
                        val mi = ActivityManager.MemoryInfo()
                        am.getMemoryInfo(mi)
                        usedRamMb = (android.os.Debug.getPss() / 1024L).toInt()
                    }
                    val bmIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    if (bmIntent != null) {
                        val lvl = bmIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                        val scl = bmIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                        batteryPct = ((lvl * 100f) / scl).roundToInt()
                        val tenthsC = bmIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320)
                        batteryTempC = tenthsC / 10f
                    }
                }
            }
            val needsFastUpdates = AerixClientStore.perfPillEnabled || AerixClientStore.fpsHistoryEnabled || AerixClientStore.frameTimingEnabled || AerixClientStore.adaptiveDistanceEnabled || AerixClientStore.cpsPillEnabled ||
                AerixClientStore.keystrokesEnabled || AerixClientStore.swordCooldownBarEnabled ||
                AerixClientStore.hitmarkerEnabled || AerixClientStore.speedrunTimerEnabled
            delay(if (needsFastUpdates) 100L else 500L)
        }
    }

    LaunchedEffect(modal, AerixClientStore.afkAction, AerixClientStore.afkSwingIntervalMs) {
        if (modal != InGameClientModal.OLED_AFK) return@LaunchedEffect
        when (AerixClientStore.afkAction) {
            AfkFarmAction.AUTO_FISH_RCLICK -> {
                AerixClientInputBridge.setRightClickHold(true)
                while (true) delay(1000L)
            }
            AfkFarmAction.XP_GRINDER_SWING -> {
                while (true) {
                    AerixClientInputBridge.triggerLeftSwingClick()
                    delay(AerixClientStore.afkSwingIntervalMs.coerceAtLeast(650L))
                }
            }
            AfkFarmAction.ANTI_IDLE_PULSE -> {
                while (true) {
                    delay(38_000L)
                    AerixClientInputBridge.tapKey(android.view.KeyEvent.KEYCODE_SPACE)
                }
            }
            AfkFarmAction.PASSIVE_HOLD -> {
                while (true) delay(2000L)
            }
        }
    }

    // A modal may only be dismissed by an outside tap or by back/escape once it has actually been
    // drawn, and then only for a gesture that started after that plus the grace window - see
    // AerixOverlayInput. This is armed here rather than inside the client menu because every modal
    // answers the same two dismissal paths: a subpage that could never be left would be a far worse
    // bug than the flicker this guard is for.
    LaunchedEffect(modal, bootVisible) {
        if (bootVisible || modal == InGameClientModal.NONE) return@LaunchedEffect
        withFrameNanos { }
        AerixOverlayInput.arm()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Compact non-overlapping `[✦ Client]` button in the safe gap (x=0.57, clear of TAB at 0.46
        // & A at 0.77). Its height comes from the player's own saved position, floored below the
        // system's top-edge gesture strip so the tap that opens the menu stays a tap.
        if (!bootVisible && modal == InGameClientModal.NONE) {
            val btnPos = AerixClientStore.hudPositions["client_btn"]
            val bx = ((btnPos?.xNorm ?: 0.57f) * wPx).roundToInt()
            // 1.8% of the screen height is about nineteen pixels, which is inside the strip Android
            // hands to the system as a top-edge gesture on many phones: the tap is eaten or repeated,
            // and the panel flashes open and shut. Anything higher than this stays a launcher touch.
            val pillDensity = LocalDensity.current
            val topSafePx = with(pillDensity) { 26.dp.roundToPx() }
            val by = maxOf(((btnPos?.yNorm ?: 0.035f) * hPx).roundToInt(), topSafePx)
            Row(
                modifier = Modifier
                    .offset { IntOffset(bx, by) }
                    .scale(btnPos?.scale ?: 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE016181F))
                    .border(1.1.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(8.dp))
                    .clickable { onChangeModal(InGameClientModal.MODULES) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LiquidCrystalPalette.CrimsonGlow)
                )
                Text(
                    text = "Client",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Optional In-Game HUD (ALL items OFF by default; rendered only when explicitly toggled ON)
        if (!bootVisible && isGrabbing && modal == InGameClientModal.NONE) {
            InGameCrimsonHud(
                telemetry = telemetry,
                wallClock = wallClock,
                nowMs = frameTickMs,
                wPx = wPx,
                hPx = hPx,
                fps = currentFps,
                fpsHistory = fpsHistory,
                frameTiming = frameTiming,
                frameTimingSupported = mcVersion != "26.3",
                low1Pct = onePercentLowFps,
                ramMb = usedRamMb,
                batteryPct = batteryPct,
                tempC = batteryTempC,
                sessionSec = sessionElapsedSec,
                speedrunDisplayMs = if (speedrunRunning) speedrunAccumMs + (frameTickMs - speedrunStartedAtMs) else speedrunAccumMs,
                speedrunRunning = speedrunRunning,
                speedrunLastSplit = speedrunLastSplitText,
                onToggleSpeedrun = {
                    if (speedrunRunning) {
                        val elapsed = speedrunAccumMs + (System.currentTimeMillis() - speedrunStartedAtMs)
                        speedrunAccumMs = elapsed
                        speedrunLastSplitText = "Split ${formatStopwatch(elapsed)}"
                        speedrunRunning = false
                    } else {
                        speedrunStartedAtMs = System.currentTimeMillis()
                        speedrunRunning = true
                    }
                },
                onResetSpeedrun = {
                    speedrunRunning = false
                    speedrunAccumMs = 0L
                    speedrunLastSplitText = "Reset"
                }
            )
        }

        // Famous-Client Style Big Dark Gray & Crimson Red Rectangle Menu
        if (!bootVisible && modal == InGameClientModal.MODULES) {
            FamousClientRectangleMenu(
                subPage = subPage,
                onChangeSubPage = onChangeSubPage,
                onClose = { onChangeModal(InGameClientModal.NONE) },
                onOpenHudEditor = { onChangeModal(InGameClientModal.HUD_EDITOR) },
                onOpenOledAfk = { onChangeModal(InGameClientModal.OLED_AFK) },
                onSyncOptionsTxt = onSyncOptionsTxt,
                onSyncCape = onSyncCape,
                telemetry = telemetry,
                gameDir = gameDir,
                worldSharingTarget = worldSharingTarget
            )
        }

        // Drag-and-Place HUD Layout Editor
        if (!bootVisible && modal == InGameClientModal.HUD_EDITOR) {
            HudPositionEditorModal(
                onDone = { onChangeModal(InGameClientModal.MODULES) }
            )
        }

        // True #000000 OLED AFK Blackout Screen
        if (!bootVisible && modal == InGameClientModal.OLED_AFK) {
            OledAfkScreen(
                fps = currentFps,
                batteryPct = batteryPct,
                tempC = batteryTempC,
                onWake = { onChangeModal(InGameClientModal.NONE) }
            )
        }

        // Pre-GL Crimson & Dark Gray Boot Splash
        AnimatedVisibility(
            visible = bootVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CrimsonBootSplash()
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * FAMOUS-CLIENT BIG RECTANGLE MENU (SCROLLABLE SMALL SQUARES + CAPES + CROSSHAIRS)
 * ═══════════════════════════════════════════════════════════════════════════ */
private class ClientSquareModule(
    val id: String,
    val title: String,
    val icon: FeatureIconKind,
    val isActive: () -> Boolean,
    val statusText: () -> String,
    val onClick: () -> Unit
)

@Composable
private fun FamousClientRectangleMenu(
    subPage: ClientMenuSubPage,
    onChangeSubPage: (ClientMenuSubPage) -> Unit,
    onClose: () -> Unit,
    onOpenHudEditor: () -> Unit,
    onOpenOledAfk: () -> Unit,
    onSyncOptionsTxt: () -> Unit,
    onSyncCape: () -> Unit,
    telemetry: ClientTelemetrySnapshot?,
    gameDir: File?,
    worldSharingTarget: net.kdt.pojavlaunch.aerix.bridge.GameDownloadTarget
) {
    val context = LocalContext.current
    var choosingInviteRecipient by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x990A0B0E))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { if (AerixOverlayInput.acceptTapDismiss()) onClose() })
            },
        contentAlignment = Alignment.Center
    ) {
        val rectShape = RoundedCornerShape(14.dp)

        // Big Dark Gray & Crimson Red Client Rectangle
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.86f)
                .drawBehind {
                    // Subtle outer Crimson ambient glow around the big rectangle
                    drawRoundRect(
                        color = LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.16f),
                        topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                        size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }
                .clip(rectShape)
                .background(LiquidCrystalPalette.DarkPanel, rectShape)
                .border(1.6.dp, LiquidCrystalPalette.CrimsonPrimary, rectShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* consume inside rectangle */ })
                }
        ) {
            // Top Bar of the Big Rectangle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LiquidCrystalPalette.DarkHeader)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (subPage != ClientMenuSubPage.GRID) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(LiquidCrystalPalette.DarkTile)
                                .border(1.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(7.dp))
                                .clickable { onChangeSubPage(ClientMenuSubPage.GRID) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "← Back",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_aerix_mark),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }

                    Text(
                        text = when (subPage) {
                            ClientMenuSubPage.GRID -> "AERIX CLIENT"
                            ClientMenuSubPage.CAPES -> "AERIX CLIENT  ·  CAPES"
                            ClientMenuSubPage.CROSSHAIRS -> "AERIX CLIENT  ·  CROSSHAIR"
                            ClientMenuSubPage.PERFORMANCE -> "AERIX CLIENT  ·  PERFORMANCE"
                            ClientMenuSubPage.UTILITY -> "AERIX CLIENT  ·  UTILITIES"
                            ClientMenuSubPage.INFORMATION -> "AERIX CLIENT  ·  HUD INFORMATION"
                            ClientMenuSubPage.WAYPOINTS -> "AERIX CLIENT  ·  WAYPOINTS"
                            ClientMenuSubPage.VISUALS -> "AERIX CLIENT  ·  VISUALS"
                            ClientMenuSubPage.GAME_OPTIONS -> "AERIX CLIENT  ·  GAME OPTIONS"
                            ClientMenuSubPage.CHAT_SHORTCUTS -> "AERIX CLIENT  ·  CHAT SHORTCUTS"
                            ClientMenuSubPage.AIM -> "AERIX CLIENT  ·  AIM"
                            ClientMenuSubPage.PACKS -> "AERIX CLIENT  ·  RESOURCE PACKS"
                            ClientMenuSubPage.KEY_PROFILES -> "AERIX CLIENT  ·  KEYBIND PROFILES"
                            ClientMenuSubPage.TRANSFER -> "AERIX CLIENT  ·  IMPORT / EXPORT"
                            ClientMenuSubPage.GALLERY -> "AERIX CLIENT  ·  SCREENSHOTS"
                            ClientMenuSubPage.WORLD_SHARING -> "AERIX CLIENT  ·  WORLD SHARING"
                            ClientMenuSubPage.SOCIALS -> "AERIX CLIENT  ·  CHATS"
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subPage == ClientMenuSubPage.GRID) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(LiquidCrystalPalette.DarkTile)
                                .border(1.dp, LiquidCrystalPalette.DarkTileBorder, RoundedCornerShape(7.dp))
                                .clickable { onOpenHudEditor() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Edit HUD",
                                color = LiquidCrystalPalette.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(LiquidCrystalPalette.CrimsonTintFill)
                            .border(1.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(7.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Crimson divider line under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                LiquidCrystalPalette.CrimsonPrimary,
                                LiquidCrystalPalette.CrimsonGlow,
                                LiquidCrystalPalette.CrimsonDeep
                            )
                        )
                    )
            )

            // Body Content of the Big Rectangle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                when (subPage) {
                    ClientMenuSubPage.GRID -> ClientSquaresGridPage(
                        context = context,
                        onOpenCapes = { onChangeSubPage(ClientMenuSubPage.CAPES) },
                        onOpenCrosshairs = { onChangeSubPage(ClientMenuSubPage.CROSSHAIRS) },
                        onOpenPerformance = { onChangeSubPage(ClientMenuSubPage.PERFORMANCE) },
                        onOpenUtility = { onChangeSubPage(ClientMenuSubPage.UTILITY) },
                        onOpenInformation = { onChangeSubPage(ClientMenuSubPage.INFORMATION) },
                        onOpenWaypoints = { onChangeSubPage(ClientMenuSubPage.WAYPOINTS) },
                        onOpenVisuals = { onChangeSubPage(ClientMenuSubPage.VISUALS) },
                        onOpenGameOptions = { onChangeSubPage(ClientMenuSubPage.GAME_OPTIONS) },
                        onOpenChatShortcuts = { onChangeSubPage(ClientMenuSubPage.CHAT_SHORTCUTS) },
                        onOpenAim = { onChangeSubPage(ClientMenuSubPage.AIM) },
                        onOpenPacks = { onChangeSubPage(ClientMenuSubPage.PACKS) },
                        onOpenKeyProfiles = { onChangeSubPage(ClientMenuSubPage.KEY_PROFILES) },
                        onOpenTransfer = { onChangeSubPage(ClientMenuSubPage.TRANSFER) },
                        onOpenGallery = { onChangeSubPage(ClientMenuSubPage.GALLERY) },
                        onOpenWorldSharing = { onChangeSubPage(ClientMenuSubPage.WORLD_SHARING) },
                        onOpenSocials = { choosingInviteRecipient = false; onChangeSubPage(ClientMenuSubPage.SOCIALS) },
                        onOpenRecorder = {
                            var candidate: Context? = context
                            while (candidate is ContextWrapper && candidate !is net.kdt.pojavlaunch.game.GameActivity) candidate = candidate.baseContext
                            val game = candidate as? net.kdt.pojavlaunch.game.GameActivity
                            if (game == null) Toast.makeText(context, "Screen recorder unavailable", Toast.LENGTH_SHORT).show()
                            else { onClose(); game.openAerixScreenRecorder() }
                        },
                        onOpenOledAfk = onOpenOledAfk,
                        onSyncOptionsTxt = onSyncOptionsTxt
                    )
                    ClientMenuSubPage.CAPES -> ClientCapesSubPage(onSyncCape = onSyncCape)
                    ClientMenuSubPage.CROSSHAIRS -> ClientCrosshairsSubPage()
                    ClientMenuSubPage.PERFORMANCE -> ClientPerformancePage(gameDir, onClose)
                    ClientMenuSubPage.UTILITY -> ClientUtilityPage(onClose)
                    ClientMenuSubPage.INFORMATION -> ClientInformationPage()
                    ClientMenuSubPage.WAYPOINTS -> ClientWaypointsPage(telemetry)
                    ClientMenuSubPage.VISUALS -> ClientVisualPage(gameDir)
                    ClientMenuSubPage.GAME_OPTIONS -> ClientGameOptionsPage()
                    ClientMenuSubPage.CHAT_SHORTCUTS -> ClientChatShortcutsPage(onClose)
                    ClientMenuSubPage.AIM -> ClientAimPage()
                    ClientMenuSubPage.PACKS -> ClientResourcePacksPage(gameDir)
                    ClientMenuSubPage.KEY_PROFILES -> ClientKeyProfilesPage(gameDir)
                    ClientMenuSubPage.TRANSFER -> ClientTransferPage()
                    ClientMenuSubPage.GALLERY -> ClientGalleryPage(gameDir)
                    ClientMenuSubPage.WORLD_SHARING -> net.kdt.pojavlaunch.aerix.ui.game.GameWorldSharingScreen(
                        target = worldSharingTarget,
                        accent = LiquidCrystalPalette.CrimsonPrimary,
                        onBack = { onChangeSubPage(ClientMenuSubPage.GRID) },
                        onResumeGame = onClose,
                        onOpenChats = { choosingInviteRecipient = true; onChangeSubPage(ClientMenuSubPage.SOCIALS) }
                    )
                    ClientMenuSubPage.SOCIALS -> net.kdt.pojavlaunch.aerix.ui.game.GameSocialsScreen(
                        accent = LiquidCrystalPalette.CrimsonPrimary,
                        onBack = { onChangeSubPage(ClientMenuSubPage.GRID) },
                        chooseInviteRecipient = choosingInviteRecipient
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientSquaresGridPage(
    context: Context,
    onOpenCapes: () -> Unit,
    onOpenCrosshairs: () -> Unit,
    onOpenPerformance: () -> Unit,
    onOpenUtility: () -> Unit,
    onOpenInformation: () -> Unit,
    onOpenWaypoints: () -> Unit,
    onOpenVisuals: () -> Unit,
    onOpenGameOptions: () -> Unit,
    onOpenChatShortcuts: () -> Unit,
    onOpenAim: () -> Unit,
    onOpenPacks: () -> Unit,
    onOpenKeyProfiles: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenWorldSharing: () -> Unit,
    onOpenSocials: () -> Unit,
    onOpenRecorder: () -> Unit,
    onOpenOledAfk: () -> Unit,
    onSyncOptionsTxt: () -> Unit
) {
    val modules = listOf(
        ClientSquareModule("screen_recorder", "Screen Recorder", FeatureIconKind.HOTBAR,
            { net.kdt.pojavlaunch.recording.RecordingState.isActive() },
            { if (net.kdt.pojavlaunch.recording.RecordingState.isActive()) "RECORDING" else "OPEN ›" }, onOpenRecorder),
        ClientSquareModule("world_sharing", "World Sharing", FeatureIconKind.COMPASS,
            { false }, { "OPEN ›" }, onOpenWorldSharing),
        ClientSquareModule("socials", "Friend Chats", FeatureIconKind.AUTOGG,
            { false }, { "OPEN ›" }, onOpenSocials),
        ClientSquareModule("gallery", "Screenshots", FeatureIconKind.HOTBAR,
            { false }, { "GALLERY ›" }, onOpenGallery),
        ClientSquareModule("transfer", "Import / Export", FeatureIconKind.HOTBAR,
            { false }, { "OPEN ›" }, onOpenTransfer),
        ClientSquareModule("packs", "Resource Packs", FeatureIconKind.FULLBRIGHT,
            { false }, { "MANAGE ›" }, onOpenPacks),
        ClientSquareModule("key_profiles", "Key Profiles", FeatureIconKind.KEYSTROKES,
            { false }, { "MANAGE ›" }, onOpenKeyProfiles),
        ClientSquareModule("aim_settings", "Aim Profiles", FeatureIconKind.GYRO,
            { AerixClientStore.touchAimEnabled || AerixClientStore.gyroAdsEnabled }, { "SETTINGS ›" }, onOpenAim),
        ClientSquareModule("chat_shortcuts", "Chat Shortcuts", FeatureIconKind.AUTOGG,
            { false }, { "OPEN ›" }, onOpenChatShortcuts),
        ClientSquareModule("game_options", "Game Options", FeatureIconKind.GYRO,
            { false }, { "SETTINGS ›" }, onOpenGameOptions),
        ClientSquareModule("visuals", "Visuals", FeatureIconKind.FULLBRIGHT,
            { false }, { "SETTINGS ›" }, onOpenVisuals),
        ClientSquareModule("info", "Information HUD", FeatureIconKind.COMPASS,
            { AerixClientStore.coordsHudEnabled || AerixClientStore.armorHudEnabled || AerixClientStore.effectsHudEnabled || AerixClientStore.clockHudEnabled || AerixClientStore.worldTimeHudEnabled || AerixClientStore.serverHudEnabled || AerixClientStore.pingHudEnabled || AerixClientStore.lightHudEnabled || AerixClientStore.biomeHudEnabled },
            { "SETTINGS ›" }, onOpenInformation),
        ClientSquareModule("waypoints", "Waypoints", FeatureIconKind.COMPASS,
            { AerixClientStore.waypointHudEnabled }, { "MANAGE ›" }, onOpenWaypoints),
        ClientSquareModule("performance", "Performance", FeatureIconKind.FPS,
            { false }, { "SETTINGS ›" }, onOpenPerformance),
        ClientSquareModule("utility", "Utilities", FeatureIconKind.HOTBAR,
            { false }, { "ACTIONS ›" }, onOpenUtility),
        ClientSquareModule(
            id = "capes",
            title = "Capes",
            icon = FeatureIconKind.CAPE,
            isActive = { true },
            statusText = { "15 CAPES ›" },
            onClick = onOpenCapes
        ),
        ClientSquareModule(
            id = "crosshair",
            title = "Crosshair",
            icon = FeatureIconKind.CROSSHAIR,
            isActive = { AerixClientStore.crosshairEnabled },
            statusText = { if (AerixClientStore.crosshairEnabled) "ON · EDIT" else "SELECT ›" },
            onClick = onOpenCrosshairs
        ),
        ClientSquareModule(
            id = "fps",
            title = "FPS Display",
            icon = FeatureIconKind.FPS,
            isActive = { AerixClientStore.perfPillEnabled },
            statusText = { if (AerixClientStore.perfPillEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.perfPillEnabled = !AerixClientStore.perfPillEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "cps",
            title = "CPS Counter",
            icon = FeatureIconKind.CPS,
            isActive = { AerixClientStore.cpsPillEnabled },
            statusText = { if (AerixClientStore.cpsPillEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.cpsPillEnabled = !AerixClientStore.cpsPillEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "keystrokes",
            title = "Keystrokes",
            icon = FeatureIconKind.KEYSTROKES,
            isActive = { AerixClientStore.keystrokesEnabled },
            statusText = { if (AerixClientStore.keystrokesEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.keystrokesEnabled = !AerixClientStore.keystrokesEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "toggle_sprint",
            title = "Toggle Sprint",
            icon = FeatureIconKind.SPRINT,
            isActive = { AerixClientStore.toggleSprintEnabled },
            statusText = { if (AerixClientStore.toggleSprintEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                val next = !AerixClientStore.toggleSprintEnabled
                AerixClientStore.toggleSprintEnabled = next
                AerixClientInputBridge.setSprintLatch(next)
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "toggle_sneak",
            title = "Toggle Sneak",
            icon = FeatureIconKind.SNEAK,
            isActive = { AerixClientStore.toggleSneakEnabled },
            statusText = { if (AerixClientStore.toggleSneakEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                val next = !AerixClientStore.toggleSneakEnabled
                AerixClientStore.toggleSneakEnabled = next
                AerixClientInputBridge.setSneakLatch(next)
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "fullbright",
            title = "Fullbright",
            icon = FeatureIconKind.FULLBRIGHT,
            isActive = { AerixClientStore.fullbrightEnabled },
            statusText = { if (AerixClientStore.fullbrightEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.fullbrightEnabled = !AerixClientStore.fullbrightEnabled
                ClientLiveOptionsBridge.syncAll()
                AerixClientStore.persist()
                onSyncOptionsTxt()
                Toast.makeText(
                    context,
                    if (AerixClientStore.fullbrightEnabled) "Fullbright ON" else "Fullbright OFF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ),
        ClientSquareModule(
            id = "hitmarker",
            title = "Click Marker",
            icon = FeatureIconKind.HITMARKER,
            isActive = { AerixClientStore.hitmarkerEnabled },
            statusText = { if (AerixClientStore.hitmarkerEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.hitmarkerEnabled = !AerixClientStore.hitmarkerEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "cooldown",
            title = "Hit Cooldown",
            icon = FeatureIconKind.COOLDOWN,
            isActive = { AerixClientStore.swordCooldownBarEnabled },
            statusText = { if (AerixClientStore.swordCooldownBarEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.swordCooldownBarEnabled = !AerixClientStore.swordCooldownBarEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "compass",
            title = "Direction HUD",
            icon = FeatureIconKind.COMPASS,
            isActive = { AerixClientStore.compassEnabled },
            statusText = { if (AerixClientStore.compassEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.compassEnabled = !AerixClientStore.compassEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "session_clock",
            title = "Session Timer",
            icon = FeatureIconKind.CLOCK,
            isActive = { AerixClientStore.matchStatsEnabled },
            statusText = { if (AerixClientStore.matchStatsEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.matchStatsEnabled = !AerixClientStore.matchStatsEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "speedrun",
            title = "Speedrun RTA",
            icon = FeatureIconKind.SPEEDRUN,
            isActive = { AerixClientStore.speedrunTimerEnabled },
            statusText = { if (AerixClientStore.speedrunTimerEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.speedrunTimerEnabled = !AerixClientStore.speedrunTimerEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "claw_pvp",
            title = "Zoom & 180°",
            icon = FeatureIconKind.CLAW_PVP,
            isActive = { AerixClientStore.quickPvpButtonsEnabled },
            statusText = { if (AerixClientStore.quickPvpButtonsEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.quickPvpButtonsEnabled = !AerixClientStore.quickPvpButtonsEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "hotbar_strip",
            title = "Touch Hotbar",
            icon = FeatureIconKind.HOTBAR,
            isActive = { AerixClientStore.hotbarSwipeEnabled },
            statusText = { if (AerixClientStore.hotbarSwipeEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.hotbarSwipeEnabled = !AerixClientStore.hotbarSwipeEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "autowalk",
            title = "Auto Walk",
            icon = FeatureIconKind.AUTOWALK,
            isActive = { AerixClientStore.autoWalkEnabled },
            statusText = { if (AerixClientStore.autoWalkEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                val next = !AerixClientStore.autoWalkEnabled
                AerixClientStore.autoWalkEnabled = next
                AerixClientInputBridge.setAutoWalkLatch(next)
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "gyro",
            title = "Gyro Aim",
            icon = FeatureIconKind.GYRO,
            isActive = { AerixClientStore.gyroAdsEnabled },
            statusText = { if (AerixClientStore.gyroAdsEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.gyroAdsEnabled = !AerixClientStore.gyroAdsEnabled
                AerixClientStore.persist()
                onSyncOptionsTxt()
            }
        ),
        ClientSquareModule(
            id = "autogg",
            title = "Auto GG",
            icon = FeatureIconKind.AUTOGG,
            isActive = { AerixClientStore.autoGgEnabled },
            statusText = { if (AerixClientStore.autoGgEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.autoGgEnabled = !AerixClientStore.autoGgEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "thermal",
            title = "Thermal Guard",
            icon = FeatureIconKind.THERMAL,
            isActive = { AerixClientStore.thermalGuardEnabled },
            statusText = { if (AerixClientStore.thermalGuardEnabled) "ENABLED" else "DISABLED" },
            onClick = {
                AerixClientStore.thermalGuardEnabled = !AerixClientStore.thermalGuardEnabled
                AerixClientStore.persist()
            }
        ),
        ClientSquareModule(
            id = "oled_afk",
            title = "OLED AFK",
            icon = FeatureIconKind.OLED_AFK,
            isActive = { false },
            statusText = { "ENTER ›" },
            onClick = onOpenOledAfk
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cols = when {
            maxWidth >= 560.dp -> 5
            maxWidth >= 430.dp -> 4
            else -> 3
        }
        val rows = modules.chunked(cols)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { mod ->
                        GlowingFeatureSquareTile(
                            module = mod,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(cols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowingFeatureSquareTile(
    module: ClientSquareModule,
    modifier: Modifier = Modifier
) {
    val active = module.isActive()
    val tileShape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .height(92.dp)
            .drawBehind {
                // Soft slight Crimson glow when active
                if (active) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                LiquidCrystalPalette.CrimsonGlow.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.maxDimension * 0.72f
                        ),
                        topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
            .clip(tileShape)
            .background(
                brush = if (active) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF331820),
                            Color(0xFF21161B)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            LiquidCrystalPalette.DarkTile,
                            Color(0xFF1F222A)
                        )
                    )
                },
                shape = tileShape
            )
            .border(
                width = if (active) 1.4.dp else 1.dp,
                color = if (active) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder,
                shape = tileShape
            )
            .clickable { module.onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Custom Vector Feature Symbol
        Canvas(modifier = Modifier.size(28.dp)) {
            drawFeatureSymbol(
                kind = module.icon,
                color = if (active) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.TextSecondary
            )
        }

        Text(
            text = module.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (active) LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.30f)
                    else Color(0xFF181A20)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = module.statusText(),
                color = if (active) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun DrawScope.drawFeatureSymbol(kind: FeatureIconKind, color: Color) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val sw = 2.1.dp.toPx()

    when (kind) {
        FeatureIconKind.CAPE -> {
            val p = Path().apply {
                moveTo(cx - 7.dp.toPx(), cy - 10.dp.toPx())
                lineTo(cx + 7.dp.toPx(), cy - 10.dp.toPx())
                lineTo(cx + 9.dp.toPx(), cy + 10.dp.toPx())
                lineTo(cx - 5.dp.toPx(), cy + 8.dp.toPx())
                close()
            }
            drawPath(p, color, style = Stroke(width = sw))
            drawCircle(color, radius = 2.2.dp.toPx(), center = Offset(cx + 1.dp.toPx(), cy))
        }
        FeatureIconKind.CROSSHAIR -> {
            val r = 8.dp.toPx()
            val g = 3.dp.toPx()
            drawCircle(color, radius = 6.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = sw * 0.85f))
            drawLine(color, Offset(cx - r - 2.dp.toPx(), cy), Offset(cx - g, cy), sw, StrokeCap.Round)
            drawLine(color, Offset(cx + g, cy), Offset(cx + r + 2.dp.toPx(), cy), sw, StrokeCap.Round)
            drawLine(color, Offset(cx, cy - r - 2.dp.toPx()), Offset(cx, cy - g), sw, StrokeCap.Round)
            drawLine(color, Offset(cx, cy + g), Offset(cx, cy + r + 2.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.FPS -> {
            drawArc(
                color = color,
                startAngle = 160f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(cx - 9.dp.toPx(), cy - 7.dp.toPx()),
                size = Size(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawLine(color, Offset(cx, cy + 2.dp.toPx()), Offset(cx + 5.dp.toPx(), cy - 4.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.CPS -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - 6.dp.toPx(), cy - 9.dp.toPx()),
                size = Size(12.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = sw)
            )
            drawLine(color, Offset(cx, cy - 8.dp.toPx()), Offset(cx, cy - 2.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.KEYSTROKES -> {
            val s = 6.dp.toPx()
            drawRoundRect(color, Offset(cx - s * 0.5f, cy - s - 1.dp.toPx()), Size(s, s), CornerRadius(2f, 2f), Stroke(sw * 0.85f))
            drawRoundRect(color, Offset(cx - s * 1.65f, cy + 1.dp.toPx()), Size(s, s), CornerRadius(2f, 2f), Stroke(sw * 0.85f))
            drawRoundRect(color, Offset(cx - s * 0.5f, cy + 1.dp.toPx()), Size(s, s), CornerRadius(2f, 2f), Stroke(sw * 0.85f))
            drawRoundRect(color, Offset(cx + s * 0.65f, cy + 1.dp.toPx()), Size(s, s), CornerRadius(2f, 2f), Stroke(sw * 0.85f))
        }
        FeatureIconKind.SPRINT -> {
            val bolt = Path().apply {
                moveTo(cx + 2.dp.toPx(), cy - 10.dp.toPx())
                lineTo(cx - 6.dp.toPx(), cy + 1.dp.toPx())
                lineTo(cx, cy + 1.dp.toPx())
                lineTo(cx - 2.dp.toPx(), cy + 10.dp.toPx())
                lineTo(cx + 6.dp.toPx(), cy - 1.dp.toPx())
                lineTo(cx, cy - 1.dp.toPx())
                close()
            }
            drawPath(bolt, color, style = Stroke(width = sw * 0.9f))
        }
        FeatureIconKind.SNEAK -> {
            drawLine(color, Offset(cx - 7.dp.toPx(), cy - 3.dp.toPx()), Offset(cx, cy + 4.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx, cy + 4.dp.toPx()), Offset(cx + 7.dp.toPx(), cy - 3.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx - 7.dp.toPx(), cy + 3.dp.toPx()), Offset(cx, cy + 9.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx, cy + 9.dp.toPx()), Offset(cx + 7.dp.toPx(), cy + 3.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.AUTOWALK -> {
            drawLine(color, Offset(cx, cy + 8.dp.toPx()), Offset(cx, cy - 8.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx - 5.dp.toPx(), cy - 3.dp.toPx()), Offset(cx, cy - 8.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx + 5.dp.toPx(), cy - 3.dp.toPx()), Offset(cx, cy - 8.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.FULLBRIGHT -> {
            drawCircle(color, radius = 4.2.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = sw))
            for (i in 0 until 8) {
                val a = i * (PI / 4.0)
                val x0 = cx + cos(a).toFloat() * 6.8.dp.toPx()
                val y0 = cy + sin(a).toFloat() * 6.8.dp.toPx()
                val x1 = cx + cos(a).toFloat() * 9.8.dp.toPx()
                val y1 = cy + sin(a).toFloat() * 9.8.dp.toPx()
                drawLine(color, Offset(x0, y0), Offset(x1, y1), sw * 0.85f, StrokeCap.Round)
            }
        }
        FeatureIconKind.HITMARKER -> {
            val d0 = 3.dp.toPx()
            val d1 = 8.5.dp.toPx()
            drawLine(color, Offset(cx - d1, cy - d1), Offset(cx - d0, cy - d0), sw, StrokeCap.Round)
            drawLine(color, Offset(cx + d0, cy - d0), Offset(cx + d1, cy - d1), sw, StrokeCap.Round)
            drawLine(color, Offset(cx - d1, cy + d1), Offset(cx - d0, cy + d0), sw, StrokeCap.Round)
            drawLine(color, Offset(cx + d0, cy + d0), Offset(cx + d1, cy + d1), sw, StrokeCap.Round)
        }
        FeatureIconKind.COOLDOWN -> {
            drawLine(color, Offset(cx - 7.dp.toPx(), cy + 7.dp.toPx()), Offset(cx + 7.dp.toPx(), cy - 7.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx - 6.dp.toPx(), cy + 2.dp.toPx()), Offset(cx - 2.dp.toPx(), cy + 6.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx - 8.dp.toPx(), cy + 10.dp.toPx()), Offset(cx + 8.dp.toPx(), cy + 10.dp.toPx()), sw * 0.85f, StrokeCap.Round)
        }
        FeatureIconKind.COMPASS -> {
            drawCircle(color, radius = 9.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = sw * 0.9f))
            val needle = Path().apply {
                moveTo(cx, cy - 6.dp.toPx())
                lineTo(cx + 3.dp.toPx(), cy)
                lineTo(cx, cy + 6.dp.toPx())
                lineTo(cx - 3.dp.toPx(), cy)
                close()
            }
            drawPath(needle, color)
        }
        FeatureIconKind.CLOCK, FeatureIconKind.SPEEDRUN -> {
            drawCircle(color, radius = 8.5.dp.toPx(), center = Offset(cx, cy + 1.dp.toPx()), style = Stroke(width = sw * 0.9f))
            drawLine(color, Offset(cx, cy + 1.dp.toPx()), Offset(cx, cy - 4.dp.toPx()), sw, StrokeCap.Round)
            drawLine(color, Offset(cx, cy + 1.dp.toPx()), Offset(cx + 4.dp.toPx(), cy + 2.dp.toPx()), sw, StrokeCap.Round)
            if (kind == FeatureIconKind.SPEEDRUN) {
                drawLine(color, Offset(cx - 3.dp.toPx(), cy - 9.5.dp.toPx()), Offset(cx + 3.dp.toPx(), cy - 9.5.dp.toPx()), sw, StrokeCap.Round)
            }
        }
        FeatureIconKind.CLAW_PVP -> {
            drawCircle(color, radius = 7.dp.toPx(), center = Offset(cx - 1.dp.toPx(), cy - 1.dp.toPx()), style = Stroke(width = sw))
            drawLine(color, Offset(cx + 4.dp.toPx(), cy + 4.dp.toPx()), Offset(cx + 9.dp.toPx(), cy + 9.dp.toPx()), sw, StrokeCap.Round)
        }
        FeatureIconKind.HOTBAR -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - 10.dp.toPx(), cy - 4.dp.toPx()),
                size = Size(20.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                style = Stroke(width = sw * 0.9f)
            )
            drawLine(color, Offset(cx - 3.3.dp.toPx(), cy - 4.dp.toPx()), Offset(cx - 3.3.dp.toPx(), cy + 4.dp.toPx()), sw * 0.8f)
            drawLine(color, Offset(cx + 3.3.dp.toPx(), cy - 4.dp.toPx()), Offset(cx + 3.3.dp.toPx(), cy + 4.dp.toPx()), sw * 0.8f)
        }
        FeatureIconKind.GYRO -> {
            drawOval(color, Offset(cx - 9.dp.toPx(), cy - 4.dp.toPx()), Size(18.dp.toPx(), 8.dp.toPx()), style = Stroke(sw * 0.85f))
            drawOval(color, Offset(cx - 4.dp.toPx(), cy - 9.dp.toPx()), Size(8.dp.toPx(), 18.dp.toPx()), style = Stroke(sw * 0.85f))
        }
        FeatureIconKind.AUTOGG -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - 9.dp.toPx(), cy - 7.dp.toPx()),
                size = Size(18.dp.toPx(), 13.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = sw * 0.9f)
            )
            drawCircle(color, 1.3.dp.toPx(), Offset(cx - 3.5.dp.toPx(), cy - 0.5.dp.toPx()))
            drawCircle(color, 1.3.dp.toPx(), Offset(cx + 3.5.dp.toPx(), cy - 0.5.dp.toPx()))
        }
        FeatureIconKind.THERMAL -> {
            val sh = Path().apply {
                moveTo(cx, cy - 9.dp.toPx())
                lineTo(cx + 7.5.dp.toPx(), cy - 5.5.dp.toPx())
                lineTo(cx + 6.dp.toPx(), cy + 3.dp.toPx())
                lineTo(cx, cy + 9.5.dp.toPx())
                lineTo(cx - 6.dp.toPx(), cy + 3.dp.toPx())
                lineTo(cx - 7.5.dp.toPx(), cy - 5.5.dp.toPx())
                close()
            }
            drawPath(sh, color, style = Stroke(width = sw * 0.9f))
        }
        FeatureIconKind.OLED_AFK -> {
            drawCircle(color, radius = 7.5.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = sw * 0.9f))
            drawCircle(color, radius = 3.dp.toPx(), center = Offset(cx + 2.dp.toPx(), cy - 2.dp.toPx()))
        }
    }
}

@Composable
private fun ClientGalleryPage(gameDir: File?) {
    var files by remember(gameDir) { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember(gameDir) { mutableStateOf<File?>(null) }
    var preview by remember(gameDir) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(gameDir) {
        files = withContext(Dispatchers.IO) {
            gameDir?.let { File(it, "screenshots").listFiles().orEmpty().filter { f ->
                f.isFile && f.extension.lowercase() in setOf("png", "jpg", "jpeg")
            }.sortedByDescending { it.lastModified() }.take(100) } ?: emptyList()
        }
    }
    LaunchedEffect(selected) {
        preview = withContext(Dispatchers.IO) {
            selected?.let { file -> runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1280 || bounds.outHeight / sample > 1280) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull() }
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Minecraft screenshots in this instance · newest first", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        if (selected != null) {
            Text(selected?.name.orEmpty(), color = Color.White, fontSize = 11.sp)
            preview?.let { Image(bitmap = it, contentDescription = selected?.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(180.dp)) }
            if (preview == null) Text("Loading preview…", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
            LiquidCrystalPill("Back to screenshots", onClick = { selected = null })
        } else {
            if (files.isEmpty()) Text("No screenshots found. Use the screenshot action or Minecraft's screenshot key.", color = LiquidCrystalPalette.TextMuted, fontSize = 11.sp)
            files.forEach { file ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(file.name, modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LiquidCrystalPill("View", onClick = { selected = file })
                }
            }
        }
    }
}

@Composable
private fun ClientTransferPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val result = runCatching { ClientSettingsTransfer.export(context) }
            val text = result.getOrNull()
            if (text == null) Toast.makeText(context, result.exceptionOrNull()?.message ?: "Export failed", Toast.LENGTH_LONG).show()
            else scope.launch {
                val written = withContext(Dispatchers.IO) { runCatching {
                    val output = context.contentResolver.openOutputStream(uri) ?: error("Could not open destination")
                    output.bufferedWriter().use { it.write(text) }
                } }
                Toast.makeText(context, if (written.isSuccess) "Client settings exported" else written.exceptionOrNull()?.message ?: "Export failed", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val loaded = withContext(Dispatchers.IO) { runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: error("Could not open file")
                input.bufferedReader().use { reader ->
                    val out = StringBuilder()
                    val buffer = CharArray(4096)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        require(out.length + count <= 250_000) { "Settings file is too large" }
                        out.append(buffer, 0, count)
                    }
                    out.toString()
                }
            } }
            val imported = loaded.mapCatching { ClientSettingsTransfer.importSettings(context, it) }
            Toast.makeText(context, if (imported.isSuccess) "Client settings imported; restart Minecraft to apply pack and game options" else imported.exceptionOrNull()?.message ?: "Import failed", Toast.LENGTH_LONG).show()
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Transfer Client settings as a JSON file. It includes waypoints and saved chat shortcuts, but no account credentials. Keep the file private.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        Text("Import replaces included settings and performance overrides. Movement latches are turned off for safety; pack and game-option changes apply next launch.",
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiquidCrystalPill("Export settings", onClick = { exportLauncher.launch("aerix-client-settings.json") })
            LiquidCrystalPill("Import settings", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) })
        }
    }
}

@Composable
private fun ClientResourcePacksPage(gameDir: File?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var packs by remember { mutableStateOf<List<ClientInstanceOptions.Pack>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(gameDir, revision) {
        val result = withContext(Dispatchers.IO) { runCatching { gameDir?.let { ClientInstanceOptions.listPacks(it) } ?: emptyList() } }
        packs = result.getOrDefault(emptyList())
        error = result.exceptionOrNull()?.message
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Changes are queued for the next launch. Existing packs and their order are preserved; incompatible packs may require Minecraft confirmation.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        if (error != null) Text("Could not read packs: $error", color = LiquidCrystalPalette.CrimsonGlow, fontSize = 11.sp)
        if (packs.isEmpty() && error == null) Text("No other packs found in this instance's resourcepacks folder.", color = LiquidCrystalPalette.TextMuted, fontSize = 11.sp)
        packs.forEach { pack ->
            val selected = pack.queued ?: pack.active
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(pack.name, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (pack.queued != null) "Queued: ${if (selected) "on" else "off"}" else if (pack.active) "Selected" else "Not selected", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
                }
                Switch(modifier = Modifier.bringFocusedItemIntoView(), checked = selected, onCheckedChange = { value ->
                    if (gameDir != null) scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { ClientInstanceOptions.queuePack(gameDir, pack.name, value) } }
                        result.exceptionOrNull()?.let { Toast.makeText(context, it.message ?: "Could not queue pack", Toast.LENGTH_LONG).show() }
                        revision++
                    }
                })
            }
        }
    }
}

@Composable
private fun ClientKeyProfilesPage(gameDir: File?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var view by remember { mutableStateOf(ClientInstanceOptions.KeyProfileView(emptyList(), null)) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(gameDir, revision) {
        val result = withContext(Dispatchers.IO) { runCatching { gameDir?.let { ClientInstanceOptions.listKeyProfiles(it) } ?: ClientInstanceOptions.KeyProfileView(emptyList(), null) } }
        view = result.getOrDefault(ClientInstanceOptions.KeyProfileView(emptyList(), null))
        error = result.exceptionOrNull()?.message
    }
    fun action(block: () -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(block) }
            result.exceptionOrNull()?.let { Toast.makeText(context, it.message ?: "Could not save profile", Toast.LENGTH_LONG).show() }
            revision++
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Save keybinds already written to Minecraft's options file. Applying a profile happens next launch and leaves other options untouched.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        if (error != null) Text("Could not read profiles: $error", color = LiquidCrystalPalette.CrimsonGlow, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, singleLine = true, label = { Text("Profile name") }, modifier = Modifier.weight(1f))
            LiquidCrystalPill("Save current", onClick = {
                val profileName = name.trim()
                if (gameDir != null) action { ClientInstanceOptions.saveKeyProfile(gameDir, profileName) }
            })
        }
        view.profiles.forEach { profile ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${profile.name} · ${profile.bindings.size} bindings${if (view.queued == profile.name) " · queued" else ""}", modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp)
                LiquidCrystalPill("Apply next launch", onClick = { if (gameDir != null) action { ClientInstanceOptions.queueKeyProfile(gameDir, profile.name) } })
                LiquidCrystalPill("Delete", onClick = { if (gameDir != null) action { ClientInstanceOptions.deleteKeyProfile(gameDir, profile.name) } })
            }
        }
    }
}

@Composable
private fun ClientAimPage() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Touch multiplier applies only while looking in Aerix Client; your launcher setting is unchanged.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        ClientInformationToggle("Enable touch aim multiplier", AerixClientStore.touchAimEnabled) { AerixClientStore.touchAimEnabled = it }
        Text("Touch aim  ${"%.2f".format(AerixClientStore.touchAimMultiplier)}×", color = Color.White, fontSize = 11.sp)
        Slider(value = AerixClientStore.touchAimMultiplier, onValueChange = {
            AerixClientStore.touchAimMultiplier = it; AerixClientStore.persist()
        }, valueRange = 0.25f..2f)
        ClientInformationToggle("Enable Aerix gyroscope", AerixClientStore.gyroAdsEnabled) { AerixClientStore.gyroAdsEnabled = it }
        Text("Gyro aim  ${"%.2f".format(AerixClientStore.gyroSensitivity)}×", color = Color.White, fontSize = 11.sp)
        Slider(value = AerixClientStore.gyroSensitivity.coerceIn(0.25f, 2f), onValueChange = {
            AerixClientStore.gyroSensitivity = it; AerixClientStore.persist()
        }, valueRange = 0.25f..2f)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Precise" to 0.6f, "Balanced" to 1f, "Fast" to 1.5f).forEach { (label, multiplier) ->
                LiquidCrystalPill(label, onClick = {
                    AerixClientStore.touchAimMultiplier = multiplier
                    AerixClientStore.gyroSensitivity = multiplier
                    AerixClientStore.touchAimEnabled = true
                    AerixClientStore.persist()
                })
            }
        }
        Text("Quick action buttons · default Minecraft keybinds", color = Color.White, fontSize = 11.sp)
        ClientInformationToggle("Show quick-action HUD", AerixClientStore.quickPvpButtonsEnabled) { AerixClientStore.quickPvpButtonsEnabled = it }
        ClientInformationToggle("180° flick", AerixClientStore.quickFlickEnabled) { AerixClientStore.quickFlickEnabled = it }
        ClientInformationToggle("Hold zoom · requires compatible zoom mod", AerixClientStore.quickZoomEnabled) { AerixClientStore.quickZoomEnabled = it }
        ClientInformationToggle("Swap offhand · F", AerixClientStore.quickOffhandEnabled) { AerixClientStore.quickOffhandEnabled = it }
        ClientInformationToggle("Perspective · F5", AerixClientStore.quickPerspectiveEnabled) { AerixClientStore.quickPerspectiveEnabled = it }
        ClientInformationToggle("Screenshot · F2", AerixClientStore.quickScreenshotEnabled) { AerixClientStore.quickScreenshotEnabled = it }
        ClientInformationToggle("Hitboxes · F3+B", AerixClientStore.quickHitboxesEnabled) { AerixClientStore.quickHitboxesEnabled = it }
        Text("Gyro requires a supported sensor. Avoid enabling both Aerix and launcher gyro at once.",
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ClientChatShortcutsPage(onClose: () -> Unit) {
    var newText by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tap a shortcut to send it using Minecraft chat. Follow each server’s rules; keybinds must be default.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newText, onValueChange = { newText = it.take(100) }, singleLine = true,
                label = { Text("Message or command") }, modifier = Modifier.weight(1f))
            LiquidCrystalPill("Add", onClick = {
                val clean = newText.trim().replace('\n', ' ').replace('\r', ' ')
                if (clean.isNotEmpty() && AerixClientStore.chatShortcuts.size < 12) {
                    AerixClientStore.chatShortcuts = AerixClientStore.chatShortcuts + clean
                    AerixClientStore.persist()
                    newText = ""
                }
            })
        }
        AerixClientStore.chatShortcuts.forEachIndexed { index, shortcut ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shortcut, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, fontSize = 11.sp)
                LiquidCrystalPill("Send", onClick = {
                    if (!Platform.isGrabbing()) Toast.makeText(context, "Enter a world before sending chat", Toast.LENGTH_SHORT).show()
                    else { onClose(); AerixClientInputBridge.sendInGameChatMessage(shortcut) }
                })
                LiquidCrystalPill("Remove", onClick = {
                    AerixClientStore.chatShortcuts = AerixClientStore.chatShortcuts.filterIndexed { i, _ -> i != index }
                    AerixClientStore.persist()
                })
            }
        }
    }
}

@Composable
private fun ClientGameOptionsPage() {
    val options = ClientPerformanceSettings.overrides
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("These Minecraft options apply next launch. Availability may differ between versions.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        PerformanceChoice("View bobbing", "viewBobbing", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("Auto jump", "autoJump", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("Raw mouse input", "rawMouseInput", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("Minecraft sensitivity", "mouseSensitivity", options, listOf("0.0" to "Low", "0.5" to "Normal", "1.0" to "High"))
        PerformanceChoice("Hurt tilt · 1.19.4+", "damageTiltStrength", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Glint intensity · 1.19.4+", "glintStrength", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Glint speed · 1.19.4+", "glintSpeed", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Darkness effect · 1.19+", "darknessEffectScale", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Subtitles", "showSubtitles", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("FOV speed effects", "fovEffectScale", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Screen effects", "screenEffectScale", options, listOf("0.0" to "Off", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("GUI scale", "guiScale", options, listOf("0" to "Auto", "1" to "1", "2" to "2", "3" to "3"))
        PerformanceChoice("FOV", "fov", options, listOf("0.0" to "Normal", "0.5" to "Wide", "1.0" to "Quake Pro"))
        PerformanceChoice("Music volume", "soundCategory_music", options, listOf("0.0" to "Mute", "0.5" to "50%", "1.0" to "100%"))
        PerformanceChoice("Master volume", "soundCategory_master", options, listOf("0.0" to "Mute", "0.5" to "50%", "1.0" to "100%"))
    }
}

@Composable
private fun ClientVisualPage(gameDir: File?) {
    val context = LocalContext.current
    fun syncVisual() {
        AerixClientStore.persist()
        if (gameDir != null) runCatching { ClientVisualBridge.sync(gameDir) }.onFailure {
            Toast.makeText(context, it.message ?: "Could not save visual settings", Toast.LENGTH_LONG).show()
        }
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Visual time and weather affect only this client, not the server. Turning them off restores the captured state until the server updates it. Not yet available on 26.x.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Time", color = Color.White, fontSize = 11.sp)
            listOf("OFF" to "Server", "DAY" to "Day", "SUNSET" to "Sunset", "NIGHT" to "Night").forEach { (mode, label) ->
                LiquidCrystalPill(label, selected = AerixClientStore.visualTimeMode == mode, onClick = { AerixClientStore.visualTimeMode = mode; syncVisual() })
            }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Weather", color = Color.White, fontSize = 11.sp)
            listOf("OFF" to "Server", "CLEAR" to "Clear", "RAIN" to "Rain").forEach { (mode, label) ->
                LiquidCrystalPill(label, selected = AerixClientStore.visualWeatherMode == mode, onClick = { AerixClientStore.visualWeatherMode = mode; syncVisual() })
            }
        }
        Text("Resource-pack changes apply when Minecraft next starts.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        ClientInformationToggle("Animated vanilla item textures", AerixClientStore.packVanillaAnimatedGear) { AerixClientStore.packVanillaAnimatedGear = it }
        ClientInformationToggle("Animate all eligible item sprites", AerixClientStore.packAnimateAllItemSprites) { AerixClientStore.packAnimateAllItemSprites = it }
        Text("All eligible square vanilla item sprites; existing vanilla animations are preserved. Requires animated item textures and may substantially reduce performance.", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        ClientInformationToggle("Sparkle enchantment glint", AerixClientStore.packEnhancedGlint) { AerixClientStore.packEnhancedGlint = it }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Glint color", color = Color.White, fontSize = 11.sp)
            listOf("CRIMSON", "VIOLET", "BLUE", "GOLD", "GREEN", "WHITE").forEach { color ->
                LiquidCrystalPill(color.lowercase().replaceFirstChar { it.uppercase() }, selected = AerixClientStore.packGlintColor == color, onClick = { AerixClientStore.packGlintColor = color; AerixClientStore.persist() })
            }
        }
        ClientInformationToggle("Low fire", AerixClientStore.packLowFire) { AerixClientStore.packLowFire = it }
        ClientInformationToggle("Lowered shield", AerixClientStore.packLoweredShield) { AerixClientStore.packLoweredShield = it }
        ClientInformationToggle("Compact first-person totem", AerixClientStore.packCompactTotem) { AerixClientStore.packCompactTotem = it }
        ClientInformationToggle("Clear underwater overlay", AerixClientStore.packClearWater) { AerixClientStore.packClearWater = it }
        ClientInformationToggle("Remove pumpkin blur", AerixClientStore.packNoPumpkinBlur) { AerixClientStore.packNoPumpkinBlur = it }
    }
}

@Composable
private fun ClientInformationPage() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("HUD items are off by default. Information is read locally from the running game.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        ClientInformationToggle("Coordinates and dimension", AerixClientStore.coordsHudEnabled) { AerixClientStore.coordsHudEnabled = it }
        ClientInformationToggle("Armor and held-item durability", AerixClientStore.armorHudEnabled) { AerixClientStore.armorHudEnabled = it }
        ClientInformationToggle("Potion effects and timers", AerixClientStore.effectsHudEnabled) { AerixClientStore.effectsHudEnabled = it }
        ClientInformationToggle("Minecraft day and time", AerixClientStore.worldTimeHudEnabled) { AerixClientStore.worldTimeHudEnabled = it }
        ClientInformationToggle("Server address", AerixClientStore.serverHudEnabled) { AerixClientStore.serverHudEnabled = it }
        ClientInformationToggle("Active hotbar slot", AerixClientStore.selectedSlotHudEnabled) { AerixClientStore.selectedSlotHudEnabled = it }
        ClientInformationToggle("Health", AerixClientStore.healthHudEnabled) { AerixClientStore.healthHudEnabled = it }
        ClientInformationToggle("Server ping", AerixClientStore.pingHudEnabled) { AerixClientStore.pingHudEnabled = it }
        ClientInformationToggle("Biome", AerixClientStore.biomeHudEnabled) { AerixClientStore.biomeHudEnabled = it }
        ClientInformationToggle("Light level", AerixClientStore.lightHudEnabled) { AerixClientStore.lightHudEnabled = it }
        ClientInformationToggle("Phone clock", AerixClientStore.clockHudEnabled) { AerixClientStore.clockHudEnabled = it }
        Text("Use Edit HUD to move the Information HUD. Server addresses are visible on screen when enabled.",
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ClientInformationToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Switch(modifier = Modifier.bringFocusedItemIntoView(), checked = checked, onCheckedChange = { onChange(it); AerixClientStore.persist() })
    }
}

@Composable
private fun ClientWaypointsPage(telemetry: ClientTelemetrySnapshot?) {
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ClientInformationToggle("Save last observed death position", AerixClientStore.deathWaypointEnabled) { AerixClientStore.deathWaypointEnabled = it }
        ClientInformationToggle("Show saved waypoints in HUD", AerixClientStore.waypointHudEnabled) { AerixClientStore.waypointHudEnabled = it }
        Text("Saved locally; distance is shown only in the same dimension. Open a world to mark a position.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(24) },
                label = { Text("Waypoint name") }, singleLine = true, modifier = Modifier.weight(1f))
            LiquidCrystalPill("Mark here", onClick = {
                if (telemetry == null) {
                    Toast.makeText(context, "Position unavailable. Enter a world and wait a moment.", Toast.LENGTH_SHORT).show()
                } else if (AerixClientStore.waypoints.size >= 30) {
                    Toast.makeText(context, "Maximum 30 waypoints", Toast.LENGTH_SHORT).show()
                } else {
                    val wp = ClientWaypoint(java.util.UUID.randomUUID().toString(), name.trim().ifEmpty { "Waypoint" },
                        telemetry.blockX, telemetry.blockY, telemetry.blockZ, telemetry.dimension)
                    AerixClientStore.waypoints = AerixClientStore.waypoints + wp
                    AerixClientStore.persist()
                    name = ""
                }
            })
        }
        if (telemetry == null) Text("Waiting for in-game position…", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        AerixClientStore.waypoints.forEach { waypoint ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${waypoint.name} · ${waypoint.x}, ${waypoint.y}, ${waypoint.z} · ${waypoint.dimension}",
                    modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, maxLines = 2)
                LiquidCrystalPill("Delete", onClick = {
                    AerixClientStore.waypoints = AerixClientStore.waypoints.filterNot { it.id == waypoint.id }
                    AerixClientStore.persist()
                })
            }
        }
    }
}

/** Settings are queued and applied before the next Minecraft launch. */
/** Read the local Fabric metadata; never claim a jar is active merely because it exists. */
private fun findInstalledOptimizationMods(gameDir: File?): List<String> {
    if (gameDir == null) return emptyList()
    val ids = setOf("sodium", "lithium", "ferritecore", "entityculling", "immediatelyfast", "modernfix",
        "krypton", "starlight", "lazydfu", "c2me", "moreculling", "enhancedblockentities", "dynamic_fps",
        "exordium", "noisium", "vmp", "memoryleakfix")
    return File(gameDir, "mods").listFiles().orEmpty().asSequence()
        .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
        .mapNotNull { file ->
            runCatching {
                java.util.zip.ZipFile(file).use { zip ->
                    val entry = zip.getEntry("fabric.mod.json") ?: return@use null
                    if (entry.size > 65536) return@use null
                    val json = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    org.json.JSONObject(json).optString("id").takeIf { it.lowercase() in ids }
                }
            }.getOrNull()
        }.distinct().sorted().take(30).toList()
}

@Composable
private fun ClientPerformancePage(gameDir: File?, onClose: () -> Unit) {
    // Read from the composition, once. LocalContext.current is itself a composable getter, so the
    // click and slider callbacks below cannot ask for it themselves.
    val pageContext = LocalContext.current
    val pageActivity = pageContext as? net.kdt.pojavlaunch.game.GameActivity
    val context = LocalContext.current
    val options = ClientPerformanceSettings.overrides
    var installedMods by remember(gameDir) { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(gameDir) {
        installedMods = withContext(Dispatchers.IO) { findInstalledOptimizationMods(gameDir) }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The pill that draws frames per second belongs to the launcher's window, and until now an
        // Aerix Client instance could neither show it nor hide it: the setting existed only on the
        // classic video page. It lives here, next to the counters it belongs to.
        ClientInformationToggle("FPS pill on screen", AerixClientStore.fpsOverlayEnabled) {
            AerixClientStore.fpsOverlayEnabled = it
            AerixClientStore.persist()
            pageActivity?.refreshFpsViewVisibility()
        }
        Text("Cursor size  ${net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.scalePercent}%",
            color = Color.White, fontSize = 11.sp)
        Slider(value = (net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.scalePercent / 100f).coerceIn(0.25f, 3f),
            onValueChange = {
                val percent = (it * 100).roundToInt()
                net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.setScale(pageContext, percent)
                pageActivity?.applyAerixPointerPreferences()
            }, valueRange = 0.25f..3f)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Tiny" to 45, "Normal" to 100, "Large" to 165, "Huge" to 240).forEach { (label, percent) ->
                LiquidCrystalPill(label, onClick = {
                    net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.setScale(pageContext, percent)
                    pageActivity?.applyAerixPointerPreferences()
                })
            }
        }
        Text(net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.report(pageContext),
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        Text(AerixOverlayInput.report(), color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        ClientInformationToggle("Show FPS history HUD", AerixClientStore.fpsHistoryEnabled) { AerixClientStore.fpsHistoryEnabled = it }
        ClientInformationToggle("Frame time and stutters HUD", AerixClientStore.frameTimingEnabled) { AerixClientStore.frameTimingEnabled = it }
        ClientInformationToggle("Adaptive render distance", AerixClientStore.adaptiveDistanceEnabled) { AerixClientStore.adaptiveDistanceEnabled = it }
        Text("${ClientAdaptiveBridge.status}. Changes only client render distance; it returns to the original when disabled. Compatibility varies by version/mod.", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        // A server costs far more than a world of your own: chunks arrive from the network and every
        // player on screen brings entities. This profile is therefore applied only inside a server and
        // given straight back when you leave, so your own settings stay yours.
        var gameReport by remember(gameDir) { mutableStateOf(ClientLiveOptionsBridge.describeGameStatus()) }
        LaunchedEffect(gameDir) {
            while (true) {
                ClientLiveOptionsBridge.refreshGameStatus(gameDir)
                gameReport = ClientLiveOptionsBridge.describeGameStatus()
                delay(2_000L)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Server Boost", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Lowers chunk, entity and particle load while you are on a server only. Applied live - no restart, and your own options come back when you leave.",
                    color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            }
            Switch(
                checked = AerixClientStore.serverBoostEnabled,
                onCheckedChange = {
                    AerixClientStore.serverBoostEnabled = it
                    AerixClientStore.persist()
                    ClientLiveOptionsBridge.syncAll(gameDir)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LiquidCrystalPalette.CrimsonPrimary,
                    uncheckedThumbColor = LiquidCrystalPalette.TextSecondary,
                    uncheckedTrackColor = LiquidCrystalPalette.DarkOuter
                )
            )
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Strength", color = Color.White, fontSize = 11.sp)
            ClientLiveOptionsBridge.BoostPreset.values().forEach { preset ->
                LiquidCrystalPill(preset.label, selected = AerixClientStore.serverBoostPreset == preset.name, onClick = {
                    AerixClientStore.serverBoostPreset = preset.name
                    AerixClientStore.persist()
                    ClientLiveOptionsBridge.syncAll(gameDir)
                })
            }
        }
        Text("Zoom strength and cursor size are in Quick Settings (the menu button in game); the ZOOM pill works without any zoom mod installed.",
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        Text(gameReport, color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Target FPS", color = Color.White, fontSize = 11.sp)
            listOf(30, 45, 60, 90, 120).forEach { value -> LiquidCrystalPill("$value", selected = AerixClientStore.adaptiveFpsTarget == value, onClick = { AerixClientStore.adaptiveFpsTarget = value; AerixClientStore.persist() }) }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Minimum chunks", color = Color.White, fontSize = 11.sp)
            listOf(2, 4, 6, 8).forEach { value -> LiquidCrystalPill("$value", selected = AerixClientStore.adaptiveMinDistance == value, onClick = { AerixClientStore.adaptiveMinDistance = value; AerixClientStore.persist() }) }
        }
        Text("Frame timing measures GLFW swap intervals (100 ms and longer grouped). On 26.3, FPS comes from the game; frame timing is unavailable.", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        Text("Optimization mods installed (loading and compatibility are not guaranteed):",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
        Text(if (installedMods.isEmpty()) "None detected" else installedMods.joinToString(" · "),
            color = Color.White, fontSize = 10.sp)
        LiquidCrystalPill("Resolution / launcher input settings", onClick = {
            var candidate: Context? = context
            while (candidate is ContextWrapper && candidate !is net.kdt.pojavlaunch.game.GameActivity) candidate = candidate.baseContext
            val game = candidate as? net.kdt.pojavlaunch.game.GameActivity
            if (game == null) Toast.makeText(context, "Quick settings unavailable", Toast.LENGTH_SHORT).show()
            else { onClose(); game.openAerixQuickSettings() }
        })
        Text("Resolution and launcher input settings affect the launcher globally. Other options below apply next Minecraft launch.", color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        Text("Settings apply next time Minecraft starts. Lower settings can improve FPS but reduce visual quality.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Maximum FPS", "Balanced", "Battery Saver", "Charging", "Cool Down", "PvP").forEach { profile ->
                LiquidCrystalPill(profile, onClick = { ClientPerformanceSettings.profile(profile) })
            }
            LiquidCrystalPill("Stop FPS overrides", onClick = { ClientPerformanceSettings.profile("Custom") })
        }
        Text("Stopping FPS overrides keeps Minecraft’s current settings; it does not restore earlier values.",
            color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
        PerformanceChoice("Render distance", "renderDistance", options, listOf("4" to "4 chunks", "6" to "6 chunks", "8" to "8 chunks", "12" to "12 chunks", "16" to "16 chunks"))
        PerformanceChoice("Simulation distance · 1.18+", "simulationDistance", options, listOf("5" to "5 chunks", "6" to "6 chunks", "8" to "8 chunks", "12" to "12 chunks"))
        PerformanceChoice("Graphics / leaves", "graphicsMode", options, listOf("0" to "Fast", "1" to "Fancy"))
        PerformanceChoice("Clouds", "renderClouds", options, listOf("\"false\"" to "Off", "\"fast\"" to "Fast", "\"true\"" to "Fancy"))
        PerformanceChoice("Entity render distance", "entityDistanceScaling", options, listOf("0.5" to "50%", "0.75" to "75%", "1.0" to "100%", "1.5" to "150%"))
        PerformanceChoice("Entity shadows", "entityShadows", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("Particles", "particles", options, listOf("2" to "Minimal", "1" to "Decreased", "0" to "All"))
        PerformanceChoice("Biome blend", "biomeBlendRadius", options, listOf("0" to "Off", "2" to "2 blocks", "5" to "5 blocks"))
        PerformanceChoice("Mipmap levels", "mipmapLevels", options, listOf("0" to "Off", "2" to "2", "4" to "4"))
        PerformanceChoice("VSync", "enableVsync", options, listOf("false" to "Off", "true" to "On"))
        PerformanceChoice("FPS limit", "maxFps", options, listOf("30" to "30", "60" to "60", "120" to "120", "260" to "Unlimited"))
        // The in-game FPS limit is not what holds a phone at 60. Two launcher-side decisions are:
        // the panel being left in its default mode, and "force vsync", which is switched on for
        // devices this launcher considers powerful - a Dimensity 6000-series phone therefore paints
        // at the refresh rate the display was never asked to raise, whatever the sliders above say.
        // So the ceiling is measured here, stated here, and lifted here.
        val displayContext = pageContext
        val liveHz = net.kdt.pojavlaunch.utils.DisplayRefreshMode.activeRefreshHz(displayContext)
        val ceilingHz = net.kdt.pojavlaunch.utils.DisplayRefreshMode.ceilingRefreshHz(displayContext)
        val vsyncForced = net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_FORCE_VSYNC
        val refreshRequested = net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_MAX_REFRESH_RATE
        Text(
            "Panel serving ${Math.round(liveHz)} Hz · fastest at this resolution ${Math.round(ceilingHz)} Hz · " +
                "launcher vsync ${if (vsyncForced) "forced on" else "off"} · " +
                "max refresh request ${if (refreshRequested) "on" else "off"}",
            color = LiquidCrystalPalette.TextMuted,
            fontSize = 10.sp
        )
        if (ceilingHz > liveHz + 1f || vsyncForced || !refreshRequested) {
            val label = when {
                ceilingHz > liveHz + 1f -> "Unlock ${Math.round(ceilingHz)} Hz and drop vsync"
                vsyncForced -> "Release the vsync cap"
                else -> "Ask the display for its fastest mode"
            }
            LiquidCrystalPill(label, onClick = {
                val prefs = net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF
                prefs?.edit()
                    ?.putBoolean(net.kdt.pojavlaunch.utils.DisplayRefreshMode.PREF_KEY, true)
                    ?.putBoolean("force_vsync", false)
                    ?.apply()
                net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_MAX_REFRESH_RATE = true
                net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_FORCE_VSYNC = false
                // Apply to this window at once; the next launch reads the same preferences.
                net.kdt.pojavlaunch.utils.DisplayRefreshMode.applyTo(
                    displayContext as? android.app.Activity
                )
                ClientPerformanceSettings.set("enableVsync", "false")
                ClientPerformanceSettings.set("maxFps", "260")
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Animated item textures · next launch", color = Color.White, fontSize = 11.sp)
            Switch(modifier = Modifier.bringFocusedItemIntoView(), checked = AerixClientStore.packVanillaAnimatedGear, onCheckedChange = {
                AerixClientStore.packVanillaAnimatedGear = it
                AerixClientStore.persist()
            })
        }
        Text("Disabling animated item textures can reduce texture updates. Capes are controlled separately.",
            color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun PerformanceChoice(title: String, key: String, options: Map<String, String>, choices: List<Pair<String, String>>) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.width(150.dp), color = Color.White, fontSize = 11.sp)
        choices.forEach { (value, label) ->
            LiquidCrystalPill(label, selected = options[key] == value, onClick = { ClientPerformanceSettings.set(key, value) })
        }
    }
}

/** Uses Minecraft's own default keys; custom key bindings may require the player to use those instead. */
@Composable
private fun ClientUtilityPage(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Actions use Minecraft’s default keyboard shortcuts.", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
        val actions = listOf(
            "Screenshot · F2" to { AerixClientInputBridge.tapKey(android.view.KeyEvent.KEYCODE_F2) },
            "Perspective · F5" to { AerixClientInputBridge.triggerPerspectiveToggle() },
            "Swap offhand · F" to { AerixClientInputBridge.triggerSwapOffhand() },
            "Reload chunks · F3+A" to { AerixClientInputBridge.triggerF3Chord(android.view.KeyEvent.KEYCODE_A) },
            "Hitboxes · F3+B" to { AerixClientInputBridge.triggerF3Chord(android.view.KeyEvent.KEYCODE_B) },
            "Chunk borders · F3+G" to { AerixClientInputBridge.triggerF3Chord(android.view.KeyEvent.KEYCODE_G) },
            "Clear chat · F3+D" to { AerixClientInputBridge.triggerF3Chord(android.view.KeyEvent.KEYCODE_D) }
        )
        actions.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, action) ->
                    LiquidCrystalPill(label, onClick = { onClose(); action() })
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * CAPES SUB-PAGE: LEFT = 3D ROTATABLE CHARACTER + WAVING TOGGLE | RIGHT = 15 ANIMATED CAPES
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun ClientCapesSubPage(onSyncCape: () -> Unit) {
    val context = LocalContext.current
    val activeCape = AerixClientCosmetics.effectiveCapeSpec()
    val playerName = PojavAccountBridge.current?.username?.takeIf { it.isNotBlank() } ?: "Player"

    val inf = rememberInfiniteTransition(label = "capeGridPhase")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "capeSwatchPhase"
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LEFT: 3D Rotatable Character + Waving Cape ON/OFF Toggle
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(LiquidCrystalPalette.DarkHeader)
                .border(1.dp, LiquidCrystalPalette.DarkTileBorder, RoundedCornerShape(11.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Interactive3DCosmeticStage(
                capeSpec = activeCape,
                wavingCape = AerixClientStore.wavingCapeEnabled,
                playerName = playerName,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            // Waving Cape ON/OFF Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (AerixClientStore.wavingCapeEnabled) LiquidCrystalPalette.CrimsonTintFill
                        else LiquidCrystalPalette.DarkTile
                    )
                    .border(
                        1.dp,
                        if (AerixClientStore.wavingCapeEnabled) LiquidCrystalPalette.CrimsonPrimary
                        else LiquidCrystalPalette.DarkTileBorder,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable {
                        AerixClientStore.wavingCapeEnabled = !AerixClientStore.wavingCapeEnabled
                        AerixClientStore.persist()
                        onSyncCape()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Waving Cape",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (AerixClientStore.wavingCapeEnabled) "ON · Flowing physics" else "OFF · Classic straight",
                        color = LiquidCrystalPalette.TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = AerixClientStore.wavingCapeEnabled,
                    onCheckedChange = {
                        AerixClientStore.wavingCapeEnabled = it
                        AerixClientStore.persist()
                        onSyncCape()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LiquidCrystalPalette.CrimsonPrimary,
                        uncheckedThumbColor = LiquidCrystalPalette.TextSecondary,
                        uncheckedTrackColor = LiquidCrystalPalette.DarkOuter
                    )
                )
            }
        }

        // RIGHT: 15 Animated Capes Grid
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "15 Animated Capes",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Equipped: ${activeCape?.title ?: "none"}",
                        color = LiquidCrystalPalette.CrimsonGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Game side: ${ClientLiveOptionsBridge.readGameStatus()["cape"] ?: "not reporting yet"} · mod: ${AerixClientModBridge.lastStatus}",
                        color = LiquidCrystalPalette.TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            LiquidCrystalPill("Re-send cape frames to the game", onClick = {
                onSyncCape()
                Toast.makeText(context, AerixClientCosmetics.lastCapeSyncStatus, Toast.LENGTH_LONG).show()
            })
            Spacer(Modifier.height(6.dp))
            // Taking a cape off has to be possible. Before this, the only way to stop wearing an
            // Aerix cape was to import a custom one, and the default was an equipped cape nobody
            // had chosen.
            val noCapeSelected = activeCape == null &&
                AerixClientStore.equippedCapeId != "custom"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (noCapeSelected) LiquidCrystalPalette.CrimsonTintFill
                        else LiquidCrystalPalette.DarkTile,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = if (noCapeSelected) 1.4.dp else 1.dp,
                        color = if (noCapeSelected) LiquidCrystalPalette.CrimsonGlow
                        else LiquidCrystalPalette.DarkTileBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        AerixClientStore.equippedCapeId = AerixClientStore.CAPE_NONE
                        AerixClientStore.persist()
                        onSyncCape()
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "No cape",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Wear nothing, or the cape your own account has",
                        color = Color(0xFFB8BCCA).copy(alpha = 0.75f),
                        fontSize = 10.sp
                    )
                }
                if (noCapeSelected) {
                    Text("✓", color = LiquidCrystalPalette.CrimsonGlow, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            val capeRows = AerixClientCosmetics.capes.chunked(3)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capeRows.forEach { rowCapes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCapes.forEach { spec ->
                            val selected = activeCape?.id == spec.id
                            val unlocked = AerixClientCosmetics.canEquipCape(spec)
                            val cardShape = RoundedCornerShape(10.dp)

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .drawBehind {
                                        if (selected) {
                                            drawRoundRect(
                                                brush = Brush.radialGradient(
                                                    listOf(
                                                        LiquidCrystalPalette.CrimsonGlow.copy(alpha = 0.28f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                                                size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                            )
                                        }
                                    }
                                    .clip(cardShape)
                                    .background(
                                        if (selected) LiquidCrystalPalette.CrimsonTintFill
                                        else LiquidCrystalPalette.DarkTile,
                                        cardShape
                                    )
                                    .border(
                                        width = if (selected) 1.4.dp else 1.dp,
                                        color = if (selected) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder,
                                        shape = cardShape
                                    )
                                    .clickable {
                                        if (unlocked) {
                                            AerixClientStore.equippedCapeId = spec.id
                                            AerixClientStore.persist()
                                            onSyncCape()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Exclusive ${spec.badgeText ?: "Staff"} cape",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedCapePreviewSwatch(
                                    spec = spec,
                                    phase = phase,
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spec.title,
                                        color = if (unlocked) Color.White else LiquidCrystalPalette.TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = when {
                                            !unlocked -> "Locked ${spec.badgeText.orEmpty()}"
                                            selected -> "EQUIPPED"
                                            else -> "Animated"
                                        },
                                        color = when {
                                            selected -> LiquidCrystalPalette.CrimsonGlow
                                            !unlocked -> LiquidCrystalPalette.TextMuted
                                            else -> LiquidCrystalPalette.TextSecondary
                                        },
                                        fontSize = 9.5.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        repeat(3 - rowCapes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * CROSSHAIR SUB-PAGE: GRID OF 12 SELECTABLE CROSSHAIR TYPES + SIZE/GAP CONTROLS
 * ═══════════════════════════════════════════════════════════════════════════ */
private fun selectedCrosshairColor(): Color = when (AerixClientStore.crosshairColor) {
    "WHITE" -> Color.White
    "GREEN" -> Color(0xFF55FF88)
    "CYAN" -> Color(0xFF66FFFF)
    "YELLOW" -> Color(0xFFFFE65C)
    else -> LiquidCrystalPalette.CrimsonPrimary
}

@Composable
private fun ClientCrosshairsSubPage() {
    val shapes = CrosshairShape.values().toList()
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toggle + Quick Sliders Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LiquidCrystalPalette.DarkHeader)
                .border(1.dp, LiquidCrystalPalette.DarkTileBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Custom Crosshair",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = AerixClientStore.crosshairEnabled,
                    onCheckedChange = {
                        AerixClientStore.crosshairEnabled = it
                        AerixClientStore.persist()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = LiquidCrystalPalette.CrimsonPrimary,
                        uncheckedThumbColor = LiquidCrystalPalette.TextSecondary,
                        uncheckedTrackColor = LiquidCrystalPalette.DarkOuter
                    )
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Size", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
                Slider(
                    value = AerixClientStore.crosshairSize,
                    onValueChange = {
                        AerixClientStore.crosshairSize = it
                        AerixClientStore.persist()
                    },
                    valueRange = 4f..18f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = LiquidCrystalPalette.CrimsonPrimary,
                        inactiveTrackColor = LiquidCrystalPalette.DarkTile
                    )
                )
                Text("Gap", color = LiquidCrystalPalette.TextSecondary, fontSize = 11.sp)
                Slider(
                    value = AerixClientStore.crosshairGap,
                    onValueChange = {
                        AerixClientStore.crosshairGap = it
                        AerixClientStore.persist()
                    },
                    valueRange = 0f..10f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = LiquidCrystalPalette.CrimsonPrimary,
                        inactiveTrackColor = LiquidCrystalPalette.DarkTile
                    )
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Opacity", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            Slider(value = AerixClientStore.crosshairOpacity, onValueChange = { AerixClientStore.crosshairOpacity = it; AerixClientStore.persist() },
                valueRange = 0.1f..1f, modifier = Modifier.width(90.dp))
            Text("Thickness", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            Slider(value = AerixClientStore.crosshairThickness, onValueChange = { AerixClientStore.crosshairThickness = it; AerixClientStore.persist() },
                valueRange = 0.5f..5f, modifier = Modifier.width(90.dp))
            Text("Outline", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            Slider(value = AerixClientStore.crosshairOutline, onValueChange = { AerixClientStore.crosshairOutline = it; AerixClientStore.persist() },
                valueRange = 0f..4f, modifier = Modifier.width(90.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Color", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            listOf("CRIMSON", "WHITE", "GREEN", "CYAN", "YELLOW").forEach { color ->
                LiquidCrystalPill(color.lowercase().replaceFirstChar { it.uppercase() }, selected = AerixClientStore.crosshairColor == color, onClick = { AerixClientStore.crosshairColor = color; AerixClientStore.persist() })
            }
            Text("Cooldown estimate ${AerixClientStore.cooldownDurationMs}ms", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
            Slider(value = AerixClientStore.cooldownDurationMs.toFloat(), onValueChange = { AerixClientStore.cooldownDurationMs = it.roundToInt(); AerixClientStore.persist() }, valueRange = 250f..1500f, modifier = Modifier.width(100.dp))
        }

        // 4x3 Grid of Crosshair Types
        val rows = shapes.chunked(4)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowShapes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowShapes.forEach { shape ->
                        val selected = AerixClientStore.crosshairEnabled && AerixClientStore.crosshairShape == shape
                        val tileShape = RoundedCornerShape(10.dp)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .drawBehind {
                                    if (selected) {
                                        drawRoundRect(
                                            brush = Brush.radialGradient(
                                                listOf(
                                                    LiquidCrystalPalette.CrimsonGlow.copy(alpha = 0.28f),
                                                    Color.Transparent
                                                )
                                            ),
                                            topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                                            size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                        )
                                    }
                                }
                                .clip(tileShape)
                                .background(
                                    if (selected) LiquidCrystalPalette.CrimsonTintFill
                                    else LiquidCrystalPalette.DarkTile,
                                    tileShape
                                )
                                .border(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder,
                                    shape = tileShape
                                )
                                .clickable {
                                    AerixClientStore.crosshairShape = shape
                                    AerixClientStore.crosshairEnabled = true
                                    AerixClientStore.persist()
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val d = density.density
                                drawAerixCrosshair(
                                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                                    shape = shape,
                                    armSizePx = AerixClientStore.crosshairSize * d * 0.9f,
                                    gapPx = AerixClientStore.crosshairGap * d * 0.9f,
                                    thickPx = AerixClientStore.crosshairThickness * d,
                                    outlinePx = 1.2f * d,
                                    mainColor = if (selected) selectedCrosshairColor() else Color.White
                                )
                            }

                            Text(
                                text = shape.label,
                                color = if (selected) LiquidCrystalPalette.CrimsonGlow else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(4 - rowShapes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * OPTIONAL IN-GAME HUD (ZERO OVERLAPS, ALL OFF BY DEFAULT)
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun InGameCrimsonHud(
    telemetry: ClientTelemetrySnapshot?,
    wallClock: String,
    nowMs: Long,
    wPx: Float,
    hPx: Float,
    fps: Int,
    fpsHistory: List<Int>,
    frameTiming: FrameTimingStats?,
    frameTimingSupported: Boolean,
    low1Pct: Int,
    ramMb: Int,
    batteryPct: Int,
    tempC: Float,
    sessionSec: Long,
    speedrunDisplayMs: Long,
    speedrunRunning: Boolean,
    speedrunLastSplit: String,
    onToggleSpeedrun: () -> Unit,
    onResetSpeedrun: () -> Unit
) {
    val positions = AerixClientStore.hudPositions
    fun Modifier.atHudSlot(id: String): Modifier {
        val slot = positions[id] ?: return this
        return this
            .offset {
                IntOffset(
                    (slot.xNorm * wPx).roundToInt(),
                    (slot.yNorm * hPx).roundToInt()
                )
            }
            .scale(slot.scale)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Center Custom Crosshair / Hitmarker / Sword Cooldown
        if (AerixClientStore.crosshairEnabled || AerixClientStore.hitmarkerEnabled || AerixClientStore.swordCooldownBarEnabled) {
            val density = LocalDensity.current
            val sinceHitMs = (nowMs - AerixClientInputBridge.lastAttackWallMs).coerceAtLeast(0L)
            val hitAlpha = if (AerixClientStore.hitmarkerEnabled && sinceHitMs < 180L) {
                1f - (sinceHitMs / 180f)
            } else 0f
            val cd = if (AerixClientStore.swordCooldownBarEnabled) {
                AerixClientInputBridge.swordCooldownProgress(nowMs)
            } else null

            Canvas(modifier = Modifier.fillMaxSize()) {
                val d = density.density
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                if (AerixClientStore.crosshairEnabled) {
                    drawAerixCrosshair(
                        center = center,
                        shape = AerixClientStore.crosshairShape,
                        armSizePx = AerixClientStore.crosshairSize * d,
                        gapPx = AerixClientStore.crosshairGap * d,
                        thickPx = AerixClientStore.crosshairThickness * d,
                        outlinePx = AerixClientStore.crosshairOutline * d,
                        mainColor = selectedCrosshairColor().copy(alpha = AerixClientStore.crosshairOpacity),
                        cooldownProgress = cd,
                        hitmarkerAlpha = hitAlpha
                    )
                } else {
                    drawAerixCrosshair(
                        center = center,
                        shape = CrosshairShape.PRECISION_DOT,
                        armSizePx = 0f,
                        gapPx = 4f * d,
                        thickPx = 0.01f,
                        outlinePx = 0f,
                        mainColor = Color.Transparent,
                        cooldownProgress = cd,
                        hitmarkerAlpha = hitAlpha
                    )
                }
            }
        }

        // One movable panel keeps enabled information together instead of overlapping separate HUDs.
        val infoEnabled = AerixClientStore.coordsHudEnabled || AerixClientStore.armorHudEnabled ||
            AerixClientStore.effectsHudEnabled || AerixClientStore.worldTimeHudEnabled ||
            AerixClientStore.serverHudEnabled || AerixClientStore.pingHudEnabled || AerixClientStore.lightHudEnabled || AerixClientStore.biomeHudEnabled || AerixClientStore.healthHudEnabled || AerixClientStore.selectedSlotHudEnabled || AerixClientStore.clockHudEnabled || AerixClientStore.waypointHudEnabled
        if (infoEnabled) {
            CrimsonHudBox(modifier = Modifier.atHudSlot("info")) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (AerixClientStore.clockHudEnabled) Text("Clock $wallClock", color = Color.White, fontSize = 10.sp)
                    if (telemetry == null && (AerixClientStore.coordsHudEnabled || AerixClientStore.armorHudEnabled ||
                            AerixClientStore.effectsHudEnabled || AerixClientStore.worldTimeHudEnabled ||
                            AerixClientStore.serverHudEnabled || AerixClientStore.pingHudEnabled || AerixClientStore.lightHudEnabled || AerixClientStore.biomeHudEnabled || AerixClientStore.healthHudEnabled || AerixClientStore.selectedSlotHudEnabled || AerixClientStore.waypointHudEnabled)) {
                        Text("Waiting for game data…", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
                    }
                    if (telemetry != null) {
                        if (AerixClientStore.coordsHudEnabled) {
                            val direction = telemetry.yaw?.let { " · ${cardinalLabel(((it % 360f) + 360f) % 360f)} ${it.roundToInt()}°" } ?: ""
                            Text("XYZ ${telemetry.blockX} / ${telemetry.blockY} / ${telemetry.blockZ} · ${telemetry.dimension}$direction", color = Color.White, fontSize = 10.sp)
                        }
                        if (AerixClientStore.worldTimeHudEnabled && telemetry.dayTime != null) {
                            val ticks = ((telemetry.dayTime % 24000) + 24000) % 24000
                            val minute = ((ticks * 60 / 1000) + 360) % 1440
                            Text("Day ${telemetry.dayTime / 24000 + 1} · %02d:%02d".format(minute / 60, minute % 60), color = Color.White, fontSize = 10.sp)
                        }
                        if (AerixClientStore.serverHudEnabled) Text("Server ${telemetry.server ?: "Unavailable"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.selectedSlotHudEnabled) Text("Slot ${telemetry.selectedSlot ?: "Unavailable"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.healthHudEnabled) Text("Health ${telemetry.health?.toString() ?: "?"} / ${telemetry.maxHealth?.toString() ?: "?"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.pingHudEnabled) Text("Ping ${telemetry.ping?.let { "$it ms" } ?: "Unavailable"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.biomeHudEnabled) Text("Biome ${telemetry.biome ?: "Unavailable"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.lightHudEnabled) Text("Light ${telemetry.light?.toString() ?: "Unavailable"}", color = Color.White, fontSize = 10.sp)
                        if (AerixClientStore.armorHudEnabled) {
                            (telemetry.armor + listOfNotNull(telemetry.held, telemetry.offhand)).forEach {
                                Text(it, color = Color.White, fontSize = 10.sp)
                            }
                        }
                        if (AerixClientStore.effectsHudEnabled) telemetry.effects.forEach {
                            Text(it, color = Color.White, fontSize = 10.sp)
                        }
                        if (AerixClientStore.waypointHudEnabled) AerixClientStore.waypoints.filter { it.dimension == telemetry.dimension }.take(3).forEach { wp ->
                            val dx = wp.x - telemetry.x; val dy = wp.y - telemetry.y; val dz = wp.z - telemetry.z
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).toInt()
                            Text("${wp.name} · ${distance}m", color = LiquidCrystalPalette.CrimsonGlow, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 2. FPS & Telemetry Pill (Safe slot x=0.02, y=0.21 — below 3RD, above D-pad)
        if (AerixClientStore.perfPillEnabled || AerixClientStore.fpsHistoryEnabled || AerixClientStore.frameTimingEnabled) {
            val hotWarning = AerixClientStore.thermalGuardEnabled && tempC >= 42.0f
            CrimsonHudBox(
                modifier = Modifier.atHudSlot("perf"),
                highlight = hotWarning
            ) {
                Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("$fps FPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text("MIN $low1Pct", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
                    Text("${ramMb}MB app", color = LiquidCrystalPalette.TextSecondary, fontSize = 10.sp)
                    Text(
                        "$batteryPct% · ${"%.0f".format(tempC)}°C",
                        color = if (hotWarning) LiquidCrystalPalette.GoldStar else LiquidCrystalPalette.CrimsonGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (AerixClientStore.frameTimingEnabled) {
                    val stat = frameTiming
                    val details = if (!frameTimingSupported) "Frame timing unavailable on Minecraft 26.3" else if (stat == null) "Frame timing: waiting for frames…" else {
                        val p95 = if (stat.p95Ms >= 100) "100+" else stat.p95Ms.toString()
                        "Frame ~${"%.1f".format(stat.meanMsCapped)} ms · P95 ${p95}ms · ≥50ms ${stat.stutters50ms}"
                    }
                    Text(details, color = LiquidCrystalPalette.TextSecondary, fontSize = 9.sp)
                }
                if (AerixClientStore.fpsHistoryEnabled && fpsHistory.size > 1) {
                    val peak = (fpsHistory.maxOrNull() ?: 1).coerceAtLeast(1)
                    Canvas(Modifier.width(160.dp).height(24.dp)) {
                        val step = size.width / 59f
                        fpsHistory.zipWithNext().forEachIndexed { i, (a, b) ->
                            drawLine(LiquidCrystalPalette.CrimsonGlow,
                                Offset(i * step, size.height * (1f - a.toFloat() / peak)),
                                Offset((i + 1) * step, size.height * (1f - b.toFloat() / peak)), strokeWidth = 2f)
                        }
                    }
                    Text("FPS history · 1 sample/s", color = LiquidCrystalPalette.TextMuted, fontSize = 9.sp)
                }
                }
            }
        }

        // 3. Dedicated CPS Pill (Safe slot x=0.22, y=0.21)
        if (AerixClientStore.cpsPillEnabled) {
            CrimsonHudBox(modifier = Modifier.atHudSlot("cps")) {
                Text(
                    text = "${AerixClientInputBridge.lmbCps} | ${AerixClientInputBridge.rmbCps} CPS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 4. Direction Compass (Safe slot x=0.39, y=0.11 — cleanly below TAB row)
        if (AerixClientStore.compassEnabled) {
            val yaw = AerixClientInputBridge.headingYawDeg
            val dirText = cardinalLabel(yaw)
            CrimsonHudBox(modifier = Modifier.atHudSlot("compass")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(cardinalLabel((yaw - 45f + 360f) % 360f), color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
                    Text("‹ $dirText ${yaw.roundToInt()}° ›", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(cardinalLabel((yaw + 45f) % 360f), color = LiquidCrystalPalette.TextMuted, fontSize = 10.sp)
                }
            }
        }

        // 5. Session Timer Pill (Safe slot x=0.83, y=0.13 — cleanly below A & MOUSE)
        if (AerixClientStore.matchStatsEnabled) {
            CrimsonHudBox(modifier = Modifier.atHudSlot("match")) {
                val mm = sessionSec / 60L
                val ss = sessionSec % 60L
                Text(
                    text = "%02d:%02d".format(mm, ss),
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Keystrokes + LMB/RMB (Safe slot x=0.02, y=0.29 — left middle lane)
        if (AerixClientStore.keystrokesEnabled) {
            Column(
                modifier = Modifier.atHudSlot("keystrokes"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                KeycapCell("W", AerixClientInputBridge.keyW || AerixClientInputBridge.autoWalkLatched)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    KeycapCell("A", AerixClientInputBridge.keyA)
                    KeycapCell("S", AerixClientInputBridge.keyS)
                    KeycapCell("D", AerixClientInputBridge.keyD)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    KeycapCell("LMB ${AerixClientInputBridge.lmbCps}", AerixClientInputBridge.mouseLmbDown, wide = true)
                    KeycapCell("RMB ${AerixClientInputBridge.rmbCps}", AerixClientInputBridge.mouseRmbDown, wide = true)
                }
            }
        }

        // 7. Speedrun Stopwatch (Safe slot x=0.79, y=0.21)
        if (AerixClientStore.speedrunTimerEnabled) {
            CrimsonHudBox(modifier = Modifier.atHudSlot("speedrun")) {
                Column {
                    Text(
                        text = formatStopwatch(speedrunDisplayMs),
                        color = if (speedrunRunning) LiquidCrystalPalette.CrimsonGlow else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(speedrunLastSplit, color = LiquidCrystalPalette.TextSecondary, fontSize = 9.sp)
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LiquidCrystalPill(
                            label = if (speedrunRunning) "Split" else "Start",
                            selected = speedrunRunning,
                            onClick = onToggleSpeedrun
                        )
                        LiquidCrystalPill("Reset", onClick = onResetSpeedrun)
                    }
                }
            }
        }

        // 8. PvP Quick Action Pills (Safe slot x=0.88, y=0.33)
        if (AerixClientStore.quickPvpButtonsEnabled) {
            Column(
                modifier = Modifier.atHudSlot("quick_pvp"),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (AerixClientStore.quickFlickEnabled) LiquidCrystalPill("180°") { AerixClientInputBridge.trigger180Flick() }
                if (AerixClientStore.quickZoomEnabled) Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (AerixClientInputBridge.zoomHolding) LiquidCrystalPalette.CrimsonTintFill
                            else LiquidCrystalPalette.DarkTile
                        )
                        .border(1.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    AerixClientInputBridge.setZoomActive(true)
                                    try { tryAwaitRelease() } finally { AerixClientInputBridge.setZoomActive(false) }
                                }
                            )
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text("ZOOM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (AerixClientStore.quickOffhandEnabled) LiquidCrystalPill("F · Offhand") { AerixClientInputBridge.triggerSwapOffhand() }
                if (AerixClientStore.quickPerspectiveEnabled) LiquidCrystalPill("F5 · View") { AerixClientInputBridge.triggerPerspectiveToggle() }
                if (AerixClientStore.quickScreenshotEnabled) LiquidCrystalPill("F2 · Shot") { AerixClientInputBridge.tapKey(android.view.KeyEvent.KEYCODE_F2) }
                if (AerixClientStore.quickHitboxesEnabled) LiquidCrystalPill("Hitboxes") { AerixClientInputBridge.triggerF3Chord(android.view.KeyEvent.KEYCODE_B) }
            }
        }

        // 9. Touch Hotbar Strip (Safe slot x=0.35, y=0.70 — cleanly ABOVE vanilla hotbar & hearts!)
        if (AerixClientStore.hotbarSwipeEnabled) {
            var swipeAccum by remember { mutableFloatStateOf(0f) }
            Row(
                modifier = Modifier
                    .atHudSlot("hotbar_swipe")
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD816181F))
                    .border(1.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            swipeAccum += dragAmount
                            if (abs(swipeAccum) >= 28f) {
                                val step = if (swipeAccum > 0f) 1 else -1
                                swipeAccum = 0f
                                AerixClientInputBridge.cycleHotbarBy(step)
                            }
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (slot in 1..9) {
                    val active = AerixClientInputBridge.activeHotbarSlot == slot
                    Box(
                        modifier = Modifier
                            .size(21.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (active) LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.55f)
                                else LiquidCrystalPalette.DarkTile
                            )
                            .border(
                                width = if (active) 1.1.dp else 0.6.dp,
                                color = if (active) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable { AerixClientInputBridge.selectHotbarSlot(slot) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$slot", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CrimsonHudBox(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xDD171920), shape)
            .border(
                1.dp,
                if (highlight) LiquidCrystalPalette.GoldStar else LiquidCrystalPalette.CrimsonPrimary,
                shape
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun KeycapCell(label: String, pressed: Boolean, wide: Boolean = false) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(width = if (wide) 45.dp else 29.dp, height = 24.dp)
            .clip(shape)
            .background(
                if (pressed) LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.65f)
                else Color(0xDD171920)
            )
            .border(
                width = if (pressed) 1.2.dp else 0.9.dp,
                color = if (pressed) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * DRAG-AND-PLACE HUD LAYOUT EDITOR (ZERO-OVERLAP SAFE RESET INCLUDED)
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun HudPositionEditorModal(onDone: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA0C0D11))
    ) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        var selectedWidget by remember { mutableStateOf<String?>(null) }
        Canvas(Modifier.fillMaxSize()) {
            listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                val shade = if (fraction == 0.5f) LiquidCrystalPalette.CrimsonGlow.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.13f)
                drawLine(shade, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), strokeWidth = 1.dp.toPx())
                drawLine(shade, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), strokeWidth = 1.dp.toPx())
            }
        }

        AerixClientStore.hudPositions.values.forEach { widget ->
            var dragX by remember(widget.id, widget.xNorm) { mutableFloatStateOf(widget.xNorm) }
            var dragY by remember(widget.id, widget.yNorm) { mutableFloatStateOf(widget.yNorm) }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((dragX * wPx).roundToInt(), (dragY * hPx).roundToInt())
                    }
                    .scale(widget.scale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LiquidCrystalPalette.CrimsonTintFill)
                    .border(if (selectedWidget == widget.id) 2.dp else 1.2.dp, if (selectedWidget == widget.id) Color.White else LiquidCrystalPalette.CrimsonGlow, RoundedCornerShape(8.dp))
                    .clickable { selectedWidget = widget.id }
                    .pointerInput(widget.id) {
                        detectDragGestures(
                            onDragStart = { selectedWidget = widget.id },
                            onDragEnd = {
                                AerixClientStore.updateWidgetPos(widget.id, dragX, dragY)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragX = (dragX + dragAmount.x / wPx).coerceIn(0.01f, 0.90f)
                            dragY = (dragY + dragAmount.y / hPx).coerceIn(0.015f, 0.88f)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✥ ${widget.label}",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LiquidCrystalPalette.DarkHeader)
                .border(1.2.dp, LiquidCrystalPalette.CrimsonPrimary, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = selectedWidget?.let { "Selected: ${AerixClientStore.hudPositions[it]?.label}" } ?: "Drag or tap a widget",
                color = LiquidCrystalPalette.TextSecondary,
                fontSize = 11.sp
            )
            LiquidCrystalPill("Size −", onClick = { selectedWidget?.let { AerixClientStore.scaleWidget(it, -0.1f) } ?: AerixClientStore.scaleHud(-0.1f) })
            LiquidCrystalPill("Size +", onClick = { selectedWidget?.let { AerixClientStore.scaleWidget(it, 0.1f) } ?: AerixClientStore.scaleHud(0.1f) })
            LiquidCrystalPill(
                label = "Reset Safe Positions",
                onClick = { AerixClientStore.resetHudPositions() }
            )
            LiquidCrystalPill(
                label = "Done",
                selected = true,
                onClick = onDone
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * OLED AFK SCREEN & PRE-GL CRIMSON BOOT SPLASH
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
private fun OledAfkScreen(
    fps: Int,
    batteryPct: Int,
    tempC: Float,
    onWake: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onWake() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AERIX CLIENT · OLED AFK",
                color = LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$fps FPS · $batteryPct% · ${"%.0f".format(tempC)}°C · Double-tap to wake",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CrimsonBootSplash() {
    val context = LocalContext.current
    var background by remember(context) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(context) {
        background = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(AerixClientPackGenerator.CLIENT_BACKGROUND_ASSET).use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LiquidCrystalPalette.DarkOuter),
        contentAlignment = Alignment.Center
    ) {
        background?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
        }
        LiquidCrystalCard(
            modifier = Modifier.width(420.dp),
            cornerRadius = 16.dp,
            strong = true,
            contentPadding = PaddingValues(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_aerix_mark),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = "AERIX CLIENT",
                            color = LiquidCrystalPalette.CrimsonGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = ClientBootLabels.header(AerixClientInputBridge.sessionMinecraftVersion),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { AerixClientInputBridge.bootProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = LiquidCrystalPalette.CrimsonPrimary,
                    trackColor = LiquidCrystalPalette.DarkTile
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = AerixClientInputBridge.bootStageTitle,
                    color = LiquidCrystalPalette.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun cardinalLabel(yawDeg: Float): String {
    val norm = ((yawDeg % 360f) + 360f) % 360f
    return when {
        norm >= 337.5f || norm < 22.5f -> "S"
        norm < 67.5f -> "SW"
        norm < 112.5f -> "W"
        norm < 157.5f -> "NW"
        norm < 202.5f -> "N"
        norm < 247.5f -> "NE"
        norm < 292.5f -> "E"
        else -> "SE"
    }
}

private fun formatStopwatch(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = ((ms % 1000L) / 100L).coerceIn(0L, 9L)
    return "%02d:%02d.%d".format(mins, secs, tenths)
}
