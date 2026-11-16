package net.kdt.pojavlaunch.prefs;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;

import static net.kdt.pojavlaunch.Architecture.is32BitsDevice;

import android.app.Activity;
import android.content.*;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.utils.DisplayRefreshMode;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.utils.GpuUtils;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.io.IOException;

import net.kdt.pojavlaunch.R;

public class LauncherPreferences {
    public static final String PREF_KEY_CURRENT_INSTANCE = "currentInstance";
    public static final String PREF_KEY_SKIP_NOTIFICATION_CHECK = "skipNotificationPermissionCheck";

    public static SharedPreferences DEFAULT_PREF;
    public static String PREF_RENDERER = "opengles2";

		/**
	 * Edge-to-edge is the default, exactly like the launcher's own window. Leaving the cutout
	 * reserved produced a black strip next to the game (reported as "some space left at the left
	 * side"), because the game window is rotated to landscape and the cutout then sits on its left.
	 */
	public static boolean PREF_IGNORE_NOTCH = true;
	/** Zoom strength of the built-in client ZOOM button, as a percentage of the game FOV. */
	public static float PREF_ZOOM_FOV_SCALE = 0.45f;
	public static float PREF_BUTTONSIZE = 100f;
	public static float PREF_MOUSESCALE = 1f;
	public static int PREF_LONGPRESS_TRIGGER = 300;
	public static String PREF_DEFAULTCTRL_PATH = Tools.CTRLDEF_FILE;
	public static String PREF_CUSTOM_JAVA_ARGS;
    public static boolean PREF_FORCE_ENGLISH = false;
    public static final String PREF_VERSION_REPOS = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static boolean PREF_DISABLE_GESTURES = false;
    public static boolean PREF_DISABLE_SWAP_HAND = false;
    public static float PREF_MOUSESPEED = 1f;
    /**
     * Look/aim sensitivity while the game holds the pointer. Deliberately separate from
     * {@link #PREF_MOUSESPEED}, which scales the launcher's own cursor: sharing one value meant a
     * player tuning their aim in the in-game slider also moved the cursor on every other screen.
     */
    public static float PREF_GAME_MOUSE_SENSITIVITY = 1f;
    public static int PREF_RAM_ALLOCATION;
    public static String PREF_DEFAULT_RUNTIME;
	public static boolean PREF_SUSTAINED_PERFORMANCE = false;
	/**
	 * Ask the display for the fastest refresh mode it offers at the resolution the game is already
	 * running at. Off by default: it is a battery and thermal cost the player opts into, and on most
	 * devices the system policy is already the right one.
	 */
	public static boolean PREF_MAX_REFRESH_RATE = false;
    public static boolean PREF_VIRTUAL_MOUSE_START = false;
    public static boolean PREF_USE_ALTERNATE_SURFACE = true;
    public static boolean PREF_JAVA_SANDBOX = true;
    public static float PREF_SCALE_FACTOR = 1f;

    public static boolean PREF_ENABLE_GYRO = false;
    public static float PREF_GYRO_SENSITIVITY = 1f;
    public static int PREF_GYRO_SAMPLE_RATE = 16;
    public static boolean PREF_GYRO_SMOOTHING = true;
    public static boolean PREF_GYRO_INVERT_X = false;
    public static boolean PREF_GYRO_INVERT_Y = false;

    public static boolean PREF_FORCE_VSYNC = false;

    public static boolean PREF_USE_ANGLE = false;
    public static boolean PREF_USE_SYSTEM_ANGLE = false;

    public static boolean PREF_BUTTON_ALL_CAPS = true;
    public static boolean PREF_DUMP_SHADERS = false;
    public static float PREF_DEADZONE_SCALE = 1f;
    public static boolean PREF_BIG_CORE_AFFINITY = false;
    public static boolean PREF_ZINK_PREFER_SYSTEM_DRIVER = false;
    public static boolean PREF_VERIFY_MANIFEST = true;
    public static String PREF_DOWNLOAD_SOURCE = "default";
    public static boolean PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = false;
    public static boolean PREF_RAPID_START = true;
    public static boolean PREF_VERIFY_FILES = true;

    public static boolean PREF_FREEDRENO_SYSMEM = false;
    public static boolean PREF_KEYBOARD_AUTOPANNING = true;
    public static boolean PREF_MIGRATION_NOTICE = true;
    public static boolean PREF_ALSOFT_FORCE_OPENSL = false;
    public static boolean PREF_SHOW_MEMORY_WARNING_DIALOG = true;
    public static short PREF_BUTTON_TRANSPARENCY = 100;

    /** Control themes: felt on touch down, and heard, unless the player turns either off. */
    public static boolean PREF_CONTROL_THEME_HAPTICS = true;
    /** Control themes: the system key click, played on touch down. */
    public static boolean PREF_CONTROL_THEME_SOUND = true;
    /** Control themes: the art keeps moving at rest. Off makes a themed control completely still. */
    public static boolean PREF_CONTROL_THEME_ANIMATED = true;
    public static boolean PREF_UBWC_WORKAROUND = false;
    /** Whether the frame rate counter is drawn over the game. */
    public static boolean PREF_SHOW_FPS = true;

    public static void loadPreferences(Context ctx) {
        //Required for CTRLDEF_FILE and MultiRT
        Tools.initStorageConstants(ctx);
        boolean isDevicePowerful = isDevicePowerful(ctx);

        net.kdt.pojavlaunch.aerix.bridge.RetiredRendererCleanup.clean(ctx);
        PREF_RENDERER = DEFAULT_PREF.getString("renderer", defaultRendererId(ctx));
        if (net.kdt.pojavlaunch.aerix.bridge.RetiredRendererCleanup.isRetiredTag(PREF_RENDERER)) {
            PREF_RENDERER = defaultRendererId(ctx);
            DEFAULT_PREF.edit().putString("renderer", PREF_RENDERER).apply();
        }
        PREF_BUTTONSIZE = DEFAULT_PREF.getInt("buttonscale", 100);
        PREF_MOUSESCALE = DEFAULT_PREF.getInt("mousescale", 100)/100f;
        PREF_MOUSESPEED = clampPercent(DEFAULT_PREF.getInt("mousespeed", 100)) / 100f;
        // One key used to serve both the game's aim and the launcher cursor. Carry the stored value
        // over on the first launch of the split build so aim sensitivity does not silently reset.
        PREF_GAME_MOUSE_SENSITIVITY = clampPercent(DEFAULT_PREF.getInt("gameMouseSensitivity",
                DEFAULT_PREF.getInt("mousespeed", 100))) / 100f;
        PREF_IGNORE_NOTCH = DEFAULT_PREF.getBoolean("ignoreNotch", true);
        PREF_ZOOM_FOV_SCALE = DEFAULT_PREF.getInt("zoomFovScale", 45) / 100f;
		PREF_LONGPRESS_TRIGGER = DEFAULT_PREF.getInt("timeLongPressTrigger", 300);
		PREF_DEFAULTCTRL_PATH = DEFAULT_PREF.getString("defaultCtrl", Tools.CTRLDEF_FILE);
        PREF_FORCE_ENGLISH = DEFAULT_PREF.getBoolean("force_english", false);
        PREF_DISABLE_GESTURES = DEFAULT_PREF.getBoolean("disableGestures",false);
        PREF_DISABLE_SWAP_HAND = DEFAULT_PREF.getBoolean("disableDoubleTap", false);
        PREF_RAM_ALLOCATION = DEFAULT_PREF.getInt("allocation", findBestRAMAllocation(ctx));
        PREF_CUSTOM_JAVA_ARGS = DEFAULT_PREF.getString("javaArgs", "");
        PREF_SUSTAINED_PERFORMANCE = DEFAULT_PREF.getBoolean("sustainedPerformance", isDevicePowerful);
        // A phone whose panel can do 90 or 120 Hz should not have to discover a hidden switch to
        // get the refresh rate it paid for: the request is on by default exactly when a faster mode
        // exists at the current resolution, and DisplayRefreshMode reports whether the device
        // honoured it, so a 60 Hz panel is unaffected rather than being promised something it lacks.
        PREF_MAX_REFRESH_RATE = DEFAULT_PREF.getBoolean(DisplayRefreshMode.PREF_KEY,
                maxRefreshDefault(ctx));
        PREF_VIRTUAL_MOUSE_START = DEFAULT_PREF.getBoolean("mouse_start", false);
        PREF_USE_ALTERNATE_SURFACE = DEFAULT_PREF.getBoolean("alternate_surface", isDevicePowerful);
        PREF_JAVA_SANDBOX = DEFAULT_PREF.getBoolean("java_sandbox", true);
        PREF_SCALE_FACTOR = DEFAULT_PREF.getInt("resolutionRatio", findBestResolution(ctx, isDevicePowerful))/100f;
        PREF_ENABLE_GYRO = DEFAULT_PREF.getBoolean("enableGyro", false);
        PREF_GYRO_SENSITIVITY = ((float)DEFAULT_PREF.getInt("gyroSensitivity", 100))/100f;
        PREF_GYRO_SAMPLE_RATE = DEFAULT_PREF.getInt("gyroSampleRate", 16);
        PREF_GYRO_SMOOTHING = DEFAULT_PREF.getBoolean("gyroSmoothing", true);
        PREF_GYRO_INVERT_X = DEFAULT_PREF.getBoolean("gyroInvertX", false);
        PREF_GYRO_INVERT_Y = DEFAULT_PREF.getBoolean("gyroInvertY", false);
        PREF_FORCE_VSYNC = DEFAULT_PREF.getBoolean("force_vsync", isDevicePowerful);
        PREF_USE_ANGLE = DEFAULT_PREF.getBoolean("use_angle", false);
        PREF_USE_SYSTEM_ANGLE = DEFAULT_PREF.getBoolean("use_system_angle", false);
        PREF_BUTTON_ALL_CAPS = DEFAULT_PREF.getBoolean("buttonAllCaps", true);
        PREF_DUMP_SHADERS = DEFAULT_PREF.getBoolean("dump_shaders", false);
        PREF_DEADZONE_SCALE = ((float) DEFAULT_PREF.getInt("gamepad_deadzone_scale", 100))/100f;
        PREF_BIG_CORE_AFFINITY = DEFAULT_PREF.getBoolean("bigCoreAffinity", false);
        PREF_ZINK_PREFER_SYSTEM_DRIVER = DEFAULT_PREF.getBoolean("zinkPreferSystemDriver", false);
        PREF_DOWNLOAD_SOURCE = DEFAULT_PREF.getString("downloadSource", "default");
        PREF_VERIFY_MANIFEST = DEFAULT_PREF.getBoolean("verifyManifest", true);
        PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = DEFAULT_PREF.getBoolean(PREF_KEY_SKIP_NOTIFICATION_CHECK, false);
        PREF_VERIFY_FILES = DEFAULT_PREF.getBoolean("checkGameFiles", true);
        PREF_RAPID_START = DEFAULT_PREF.getBoolean("fastStartupCheck", true);
        PREF_FREEDRENO_SYSMEM = DEFAULT_PREF.getBoolean("freedrenoSysmem", false);
        PREF_KEYBOARD_AUTOPANNING = DEFAULT_PREF.getBoolean("keyboardAutoPanning", true);
        PREF_MIGRATION_NOTICE = DEFAULT_PREF.getBoolean("migrationNotice", true);
        PREF_ALSOFT_FORCE_OPENSL = DEFAULT_PREF.getBoolean("alsoftForceOpenSL", false);
        PREF_SHOW_MEMORY_WARNING_DIALOG = DEFAULT_PREF.getBoolean("showMemoryWarning", true);
        PREF_BUTTON_TRANSPARENCY = (short) DEFAULT_PREF.getInt("buttonTransparency", 100);
        PREF_CONTROL_THEME_HAPTICS = DEFAULT_PREF.getBoolean("controlThemeHaptics", true);
        PREF_CONTROL_THEME_SOUND = DEFAULT_PREF.getBoolean("controlThemeSound", true);
        PREF_CONTROL_THEME_ANIMATED = DEFAULT_PREF.getBoolean("controlThemeAnimated", true);
        PREF_UBWC_WORKAROUND = DEFAULT_PREF.getBoolean("ubwcWorkaround", false);
        PREF_SHOW_FPS = DEFAULT_PREF.getBoolean("show_fps", true);

        String argLwjglLibname = "-Dorg.lwjgl.opengl.libname=";
        for (String arg : JREUtils.parseJavaArguments(PREF_CUSTOM_JAVA_ARGS)) {
            if (arg.startsWith(argLwjglLibname)) {
                // purge arg
                DEFAULT_PREF.edit().putString("javaArgs",
                    PREF_CUSTOM_JAVA_ARGS.replace(arg, "")).apply();
            }
        }
        if(DEFAULT_PREF.contains("defaultRuntime")) {
            PREF_DEFAULT_RUNTIME = DEFAULT_PREF.getString("defaultRuntime","");
        }else{
            if(MultiRTUtils.getRuntimes().isEmpty()) {
                PREF_DEFAULT_RUNTIME = "";
                return;
            }
            PREF_DEFAULT_RUNTIME = MultiRTUtils.getRuntimes().get(0).name;
            LauncherPreferences.DEFAULT_PREF.edit().putString("defaultRuntime",LauncherPreferences.PREF_DEFAULT_RUNTIME).apply();
        }
    }

    /**
     * Renderer used until one is picked in the settings.
     *
     * This used to be MobileGlues on any device with OpenGL ES 3 - which meant the launcher itself
     * made the one choice it now warns players about, silently. The automatic pick is Emesis (the
     * renderer that ships every ABI and covers old and new versions), and the version rule in
     * {@link net.kdt.pojavlaunch.game.renderer.RendererDefaults} moves a 1.17+ instance onto LTW at
     * launch. MobileGlues stays available, by name, in every renderer list.
     *
     * @param ctx Context needed to read the graphics capabilities of the device.
     * @return The renderer id to use when nothing was picked yet.
     */
    public static String defaultRendererId(Context ctx){
        if (net.kdt.pojavlaunch.game.renderer.RendererDefaults.isBundled(
                Renderers.EMESIS_RENDERER)) {
            return Renderers.EMESIS_RENDERER;
        }
        if (net.kdt.pojavlaunch.game.renderer.RendererDefaults.isBundled(
                Renderers.LTW_RENDERER)) {
            return Renderers.LTW_RENDERER;
        }
        return Renderers.GL4ES_RENDERER;
    }

    /**
     * This functions aims at finding the best default RAM amount,
     * according to the RAM amount of the physical device.
     * Put not enough RAM ? Minecraft will lag and crash.
     * Put too much RAM ?
     * The GC will lag, android won't be able to breathe properly.
     * @param ctx Context needed to get the total memory of the device.
     * @return The best default value found.
     */
    /** Mouse sensitivity percentages stay in a range the pointer can actually be used at. */
    /**
     * Whether this screen has a faster mode at its own resolution, which is what decides the default for
     * the max-refresh-rate switch. The question is asked from Application.onCreate, against a Context
     * that has no display of its own, and DisplayRefreshMode answers it through Android APIs an OEM ROM
     * is free to refuse - so the answer is optional here. A launcher that cannot ask falls back to "off"
     * and keeps working; one that lets the query escape takes the process down on every start, which is
     * what 2.1.0 through 2.3.0 did to Android 11+ devices that hit that path.
     */
    private static boolean maxRefreshDefault(Context ctx) {
        try {
            return DisplayRefreshMode.isAvailable(ctx);
        } catch (Throwable t) {
            Log.w("LauncherPreferences", "max refresh rate default unavailable, leaving it off", t);
            return false;
        }
    }

    private static int clampPercent(int percent) {
        if (percent <= 0) return 100;
        return Math.max(10, Math.min(400, percent));
    }

    private static int findBestRAMAllocation(Context ctx){
        int deviceRam = Tools.getTotalDeviceMemory(ctx);
        if (deviceRam < 1024) return 296;
        if (deviceRam < 1536) return 448;
        if (deviceRam < 2048) return 656;
        // Limit the max for 32 bits devices more harshly
        if (is32BitsDevice()) return 696;

        if (deviceRam < 3064) return 936;
        if (deviceRam < 4096) return 1144;
        if (deviceRam < 6144) return 1536;
        return 2048; //Default RAM allocation for 64 bits
    }

    /// Find a correct resolution for the device
    ///
    /// Some devices are shipped with a ridiculously high resolution, which can cause performance issues
    /// This function will try to find a resolution that is good enough for the device
    private static int findBestResolution(Context context, boolean isDevicePowerful) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int minSide = Math.min(metrics.widthPixels, metrics.heightPixels);
        int targetSide = isDevicePowerful ? 1080 : 720;
        if (minSide <= targetSide) return 100; // No need to scale down

        float ratio = (100f * targetSide / minSide);
        // The value must match the seekbar values
        int increment = context.getResources().getInteger(R.integer.resolution_seekbar_increment);
        return (int) (Math.ceil(ratio / increment) * increment);
    }

    /// Check if the device is considered powerful.
    /// Powerful devices will have some energy saving tweaks enabled by default
    private static boolean isDevicePowerful(Context context) {
        if (SDK_INT < Build.VERSION_CODES.Q) return false;
        if (Tools.getTotalDeviceMemory(context) <= 4096) return false;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (Math.min(metrics.widthPixels, metrics.heightPixels) < 1080) return false;
        if (Runtime.getRuntime().availableProcessors() <= 4) return false;
        if (hasAllCoreSameFreq()) return false;
        return true;
    }

    private static boolean hasAllCoreSameFreq() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        try {
            String freq0 = Tools.read("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            String freqX = Tools.read("/sys/devices/system/cpu/cpu" + (coreCount - 1) + "/cpufreq/cpuinfo_max_freq");
            if(freq0.equals(freqX)) return true;
        } catch (IOException e) {
            Log.e("LauncherPreferences", "Failed to read CPU frequencies", e);
        }
        return false;
    }

    /** Check if the device has a display cutout */
    public static boolean hasNotch(Activity activity) {
        if (Build.VERSION.SDK_INT < P) return false;
        try {
            final Rect cutout;
            if(SDK_INT >= Build.VERSION_CODES.S){
                cutout = activity.getWindowManager().getCurrentWindowMetrics().getWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            } else {
                cutout = activity.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout().getBoundingRects().get(0);
            }
            return cutout.width() != 0 || cutout.height() != 0;
        }catch (Exception e){
            Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode");
            return false;
        }
    }
}
