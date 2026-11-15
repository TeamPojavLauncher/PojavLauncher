package net.kdt.pojavlaunch.game;


import static net.kdt.pojavlaunch.Tools.dialogForceClose;
import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.content.SharedPreferences;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.kdt.LoggerView;

import net.kdt.pojavlaunch.BaseActivity;
import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.game.renderer.RenderSpec;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.utils.GpuUtils;
import net.kdt.pojavlaunch.utils.KeycodeUtils;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences;
import net.kdt.pojavlaunch.aerix.bridge.AerixAccount;
import net.kdt.pojavlaunch.aerix.vulkan.VulkanDriverManager;
import net.kdt.pojavlaunch.aerix.ui.game.GameMenuCallbacks;
import net.kdt.pojavlaunch.aerix.ui.game.GameMenuOverlayView;
import net.kdt.pojavlaunch.aerix.log.PreviousLaunchLog;
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl;
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.lifecycle.LaunchDiagnostics;
import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.game.platform.backend.DummyBackend;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.QuickSettingSideDialog;
import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.authenticator.accounts.Account;
import net.kdt.pojavlaunch.utils.jre.GameRunner;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

import net.kdt.pojavlaunch.R;

public class GameActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection {
    private net.kdt.pojavlaunch.recording.RecorderController mRecorder;
    /** Request code of the legacy storage permission asked for by the in game downloader. */
    private static final int REQUEST_STORAGE_PERMISSION = 0x41E1;
    public static final String INTENT_LAUNCH_VERSION = "intent_version";
    public static final String INTENT_LAUNCH_CLASSPATH = "intent_classpath";
    public static final String INTENT_LAUNCH_SERVER = "intent_launch_server";
    public static final String INTENT_INSTANCE_ID = "intent_instance_id";
    public static final String INTENT_GLOBAL_RENDERER = "intent_global_renderer";
    public static final String INTENT_START_ACK = "intent_start_ack";
    public static final int START_ACK_READY = 1;

    public static TouchCharInput touchCharInput;
    private GameView launcherGLView;
    private static WeakReference<GameCursorView> weakCursor;
    private LoggerView loggerView;
    private DrawerLayout drawerLayout;
    private ListView navDrawer;
    private View mDrawerPullButton;
    private FpsView mFpsView;
    private GameMenuOverlayView mGameMenuOverlay;
    private net.kdt.pojavlaunch.aerix.client.AerixClientOverlayView mAerixClientOverlay;
    private GyroControl mGyroControl = null;
    /**
     * The refresh rate the window last asked the display for, or 0 for "nothing outstanding". Only
     * used to decide whether a layout pass silently dropped the request, so the answer can be said
     * out loud instead of the setting quietly doing nothing.
     */
    private float mRequestedRefreshHz = 0f;
    private ControlLayout mControlLayout;
    private HotbarView mHotbarView;
    private GameRenderer mGameRenderer;

    Instance instance;
    Account account;

    private ArrayAdapter<String> gameActionArrayAdapter;
    private AdapterView.OnItemClickListener gameActionClickListener;
    public ArrayAdapter<String> ingameControlsEditorArrayAdapter;
    public AdapterView.OnItemClickListener ingameControlsEditorListener;
    private GameService.LocalBinder mServiceBinder;
    private ResultReceiver mStartAck;
    private boolean mStartupPrepared;
    private boolean mResumedForLaunch;

    private QuickSettingSideDialog mQuickSettingSideDialog;

    public static int mForcedPanningHeight = 0;
    public static int mImeHeight = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LaunchDiagnostics.mark(this, "Game activity created");
        mStartAck = getIntent().getParcelableExtra(INTENT_START_ACK);
        getIntent().removeExtra(INTENT_START_ACK);
        // Open the game log before instance, renderer, service and UI setup so early failures
        // do not leave an old launch log behind. Native process death may still prevent a report.
        try {
            File latestLogFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            // Logger.begin truncates this file, so the launch about to start would erase whatever the
            // last one left. A game that dies on startup is retried at once, and that retry is what
            // destroyed the trace - archive the old log first, then let the new one have the file.
            File archived = PreviousLaunchLog.archive(latestLogFile.getParentFile());
            if (archived != null) {
                Log.i("GameActivity", "Previous launch log kept at " + archived.getAbsolutePath());
            }
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());
            Logger.appendToLog("Aerix: game activity created; starting launch setup");
            if (archived != null) {
                Logger.appendToLog("Aerix: kept the log of the previous launch at " + archived.getName()
                        + " (Settings -> Log, or " + PreviousLaunchLog.ARCHIVE_DIR_NAME + "/)");
            }
        } catch (Throwable error) {
            LaunchDiagnostics.mark(this, "Could not open game log");
            Tools.showError(this, error, true);
            return;
        }
        try {
            net.kdt.pojavlaunch.aerix.social.SocialBackend.attach(getApplicationContext());
        } catch (Throwable ignored) {}
        // Explicit process policy before Minecraft creates its native audio tracks.
        // Per-track/backend restrictions and OEM privacy controls can still disallow capture.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            try {
                android.media.AudioManager audioManager = getSystemService(android.media.AudioManager.class);
                if (audioManager != null) audioManager.setAllowedCapturePolicy(android.media.AudioAttributes.ALLOW_CAPTURE_BY_ALL);
            } catch (RuntimeException error) {
                // Audio capture is optional; an OEM policy failure must not abort game startup.
                Log.w("GameActivity", "Audio capture policy unavailable", error);
                Logger.appendToLog("Aerix: audio capture policy unavailable; continuing game launch");
            }
        }
        String instanceId = getIntent().getStringExtra(INTENT_INSTANCE_ID);
        try {
            instance = instanceId != null ? Instances.loadInstance(instanceId) : Instances.loadSelectedInstance();
        } catch (java.io.IOException error) {
            android.util.Log.e("GameActivity", "Could not load the instance requested for this launch", error);
            instance = null;
        }
        account = Accounts.getCurrent();
        if(instance == null) {
            LaunchDiagnostics.mark(this, "Instance unavailable");
            Logger.appendToLog("Aerix: selected instance could not be loaded");
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // The instance choice is authoritative. An empty choice intentionally follows the global
        // selection captured by the launcher, rather than a stale :game-process preference cache.
        boolean instanceRenderer = Tools.isValidString(instance.renderer);
        String globalRenderer = getIntent().getStringExtra(INTENT_GLOBAL_RENDERER);
        String launchRenderer = instanceRenderer ? instance.renderer :
                (Tools.isValidString(globalRenderer) ? globalRenderer : instance.getLaunchRenderer());
        String resolvedRenderer = instanceRenderer ? launchRenderer :
                net.kdt.pojavlaunch.aerix.bridge.PojavPluginBridge.resolveLaunchTag(this, launchRenderer);
        if (resolvedRenderer == null) resolvedRenderer = launchRenderer;
        boolean plugin = resolvedRenderer.startsWith("plugin:");
        if (instanceRenderer && !plugin && GameRenderer.getKnownRenderer(resolvedRenderer) == null) {
            Toast.makeText(this, R.string.aerix_instance_renderer_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        LaunchDiagnostics.mark(this, "Preparing renderer and game window");
        mGameRenderer = new GameRenderer(resolvedRenderer, instanceRenderer);
        net.kdt.pojavlaunch.aerix.bridge.PojavPluginBridge.applyLaunchOverride(this, mGameRenderer, resolvedRenderer);
        if (instanceRenderer && plugin && !resolvedRenderer.equals(mGameRenderer.getCurrentRenderer().tag())) {
            Toast.makeText(this, R.string.aerix_instance_renderer_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String instanceDriverKey = instance.name != null ? instance.name : instance.versionId;
        String vulkanDriverId = VulkanDriverManager.selectedDriverId(this, instanceDriverKey);
        boolean useTurnip = !VulkanDriverManager.isSystemDriver(vulkanDriverId);
        boolean adrenoGpu = GpuUtils.getGlInfo().isAdreno();
        String customVulkanDriverPath = VulkanDriverManager.libraryDirectory(this, vulkanDriverId);
        if (useTurnip && !adrenoGpu) {
            Logger.appendToLog("Vulkan driver: Turnip is Adreno-only; falling back to the system Vulkan driver");
            useTurnip = false;
            customVulkanDriverPath = null;
        }
        GameRenderer.configureVulkanDriver(customVulkanDriverPath, useTurnip);
        if (useTurnip) mGameRenderer.overrideVulkanDriver();
        Logger.appendToLog("Vulkan driver selection: " + (useTurnip ? vulkanDriverId : VulkanDriverManager.SYSTEM_DRIVER));

        AsyncAssetManager.extractDefaultSettings(this, instance.getGameDirectory());
        MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());

        Intent gameServiceIntent = new Intent(this, GameService.class);
        // Start the service a bit early
        ContextCompat.startForegroundService(this, gameServiceIntent);
        initLayout(R.layout.activity_basemain);

        Platform.initialize(this, launcherGLView);

        mGyroControl = new GyroControl(this);

        // Enabling this on TextureView results in a broken white result
        if(PREF_USE_ALTERNATE_SURFACE) getWindow().setBackgroundDrawable(null);
        else getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        // This is required on Android 10 for the insets listener
        // https://issuetracker.google.com/issues/266331465
        boolean androidCompat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q;
        if(androidCompat)
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // Make keyboard pan the activity so the user sees what they're typing
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (view, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeBottom = imeVisible ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
            if (mGameMenuOverlay != null) {
                mGameMenuOverlay.onImeInsetsChanged(imeBottom);
            }
            if(launcherGLView.mSurface == null)
                return insets;
            if (mGameMenuOverlay != null && mGameMenuOverlay.isMenuShown()) {
                launcherGLView.mSurface.setTranslationY(0);
                launcherGLView.mCursorView.setTranslationY(0);
                mImeHeight = 0;
                if (!imeVisible && androidCompat) {
                    view.postDelayed(() -> {
                        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }, 150);
                }
                return insets;
            }
            ViewPropertyAnimator animSurface = launcherGLView.mSurface.animate()
                    .setDuration(100);
            ViewPropertyAnimator animCursor = launcherGLView.mCursorView.animate()
                    .setDuration(100);
            if(!imeVisible){
                animSurface.translationY(0).start();
                animCursor.translationY(0).start();
                mImeHeight = 0;
                if(androidCompat) {
                    // AndroidX keeps SystemUI visible for some reason after IME session
                    view.postDelayed(() -> {
                        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }, 150);
                }
                return insets;
            }
            if(mForcedPanningHeight == 0 && !LauncherPreferences.PREF_KEYBOARD_AUTOPANNING)
                return insets;
            mImeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int translationY;
            // Autopanning (if keyboardPan wasn't clicked)
            if(mForcedPanningHeight == 0) {
                translationY = Tools.getTranslationFromCursorY(
                        (int)(Platform.cursorY * launcherGLView.getCursorRatioY() + 100),
                        launcherGLView.getHeight(),
                        mImeHeight,
                        0
                );
            } else
                translationY = mForcedPanningHeight == -1 ? mImeHeight : Math.clamp(mImeHeight - mForcedPanningHeight, 0, mImeHeight);
            animSurface.translationY(-translationY).start();
            animCursor.translationY(-translationY).start();
            return insets;
        });

        ingameControlsEditorArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_customcontrol));
        ingameControlsEditorListener = (parent, view, position, id) -> {
            switch(position) {
                case 0: mControlLayout.addControlButton(new ControlData("New")); break;
                case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
                case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
                case 3: mControlLayout.openLoadDialog(); break;
                case 4: mControlLayout.openSaveDialog(this); break;
                case 5: mControlLayout.openSetDefaultDialog(); break;
                case 6: mControlLayout.openExitDialog(this);
            }
        };

        // Recompute the gui scale when options are changed
        MCOptionUtils.MCOptionListener optionListener = MCOptionUtils::getMcScale;
        MCOptionUtils.addMCOptionListener(optionListener);
        mControlLayout.setModifiable(false);

        // Set the activity for the executor. Must do this here, or else Tools.showErrorRemote() may not
        // execute the correct method
        ContextExecutor.setActivity(this);
        //Now, attach to the service. The game will only start when this happens, to make sure that we know the right state.
        bindService(gameServiceIntent, this, 0);
    }

    protected void initLayout(int resId) {
        setContentView(resId);
        bindValues();
        mControlLayout.setMenuListener(this);
        // A recording that ghosts the controls has to give them back on the player's own request.
        mControlLayout.setOnPlayerControlsToggle(() -> {
            if (mRecorder != null) mRecorder.onControlsRequestedByPlayer();
        });

        mDrawerPullButton.setOnClickListener(v -> onClickedMenu());
        mRecorder = new net.kdt.pojavlaunch.recording.RecorderController(this, instance.getInstanceDirectory());
        mGameMenuOverlay.setRunningInstance(instance);
        mGameMenuOverlay.setCallbacks(new GameMenuCallbacks() {
            @Override public void onScreenRecorder() {
                hideGameMenu();
                mRecorder.show();
            }

            @Override
            public void onResumeGame() {
                hideGameMenu();
            }

            @Override
            public void onForceCloseGame() {
                dialogForceClose(GameActivity.this);
            }

            @Override
            public void onLogOutput() {
                openLogOutput();
            }

            @Override
            public void onSendCustomKey() {
                dialogSendCustomKey();
            }

            @Override
            public void onQuickSettings() {
                openQuickSettings();
            }

            @Override
            public void onCustomControls() {
                openCustomControls();
            }

            @Override
            public void onRequestStoragePermission() {
                requestStorageAccess();
            }

            @Override
            public void onSwitchAccount(AerixAccount account) {
                switchAccount(account);
            }
        });
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        applyAerixPointerPreferences();
        weakCursor = new WeakReference<>(launcherGLView.mCursorView);

        try {
            Intent activityIntent = getIntent();
            Bundle extras = Objects.requireNonNull(activityIntent.getExtras());
            String version = extras.getString(INTENT_LAUNCH_VERSION);
            File[] classpath = (File[]) extras.getSerializable(INTENT_LAUNCH_CLASSPATH);
            String launchServer = extras.getString(INTENT_LAUNCH_SERVER);

            activityIntent.removeExtra(INTENT_LAUNCH_VERSION);
            activityIntent.removeExtra(INTENT_LAUNCH_CLASSPATH);
            activityIntent.removeExtra(INTENT_LAUNCH_SERVER);

            setIntent(activityIntent);

            GameRunner.setPendingServerAddress(launchServer);
            net.kdt.pojavlaunch.aerix.social.SocialBackend.onGameStarted(this, instance, version);
            net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.onGameStarted(
                    this,
                    instance != null ? instance.name : null,
                    version,
                    instance != null ? instance.modpackIconUrl : null
            );

            setTitle("MojoLauncher (" + version + ")");

            // Menu
            gameActionArrayAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_ingame));
            gameActionClickListener = (parent, view, position, id) -> {
                switch(position) {
                     case 0: dialogForceClose(GameActivity.this); break;
                     case 1: openLogOutput(); break;
                     case 2: dialogSendCustomKey(); break;
                     case 3: openQuickSettings(); break;
                     case 4: openCustomControls(); break;
                }
                drawerLayout.closeDrawers();
            };
            navDrawer.setAdapter(gameActionArrayAdapter);
            navDrawer.setOnItemClickListener(gameActionClickListener);
            drawerLayout.closeDrawers();

            launcherGLView.setSurfaceReadyListener(() -> {
                try {
                    Tools.runOnUiThread(() -> { if(PREF_VIRTUAL_MOUSE_START) launcherGLView.mCursorView.setVisibility(View.VISIBLE); });
                    if(version == null || classpath == null) {
                        Tools.runOnUiThread(()->{
                            Toast.makeText(this, R.string.main_please_restart, Toast.LENGTH_LONG).show();
                            finish();
                        });
                        return;
                    }
                    LaunchDiagnostics.mark(this, "Game surface ready; starting Minecraft");
                    runCraft(version, classpath);
                }catch (Throwable e){
                    Tools.showErrorRemote(e);
                }
            });
            mStartupPrepared = true;
        } catch (Throwable e) {
            LaunchDiagnostics.mark(this, "Game setup failed");
            Logger.appendToLog("Aerix: game setup failed: " + e.getClass().getName());
            Tools.showError(this, e, true);
        }
    }

    private void loadControls() {
        try {
            // Load keys
            mControlLayout.loadLayout(instance.getLaunchControls());
        } catch(IOException e) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e);
                mControlLayout.loadLayout(Tools.CTRLDEF_FILE);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        mControlLayout.toggleControlVisible();
    }

    /**
     * The view the game renders into, for the recorder's game-surface capture. Null until the game has a
     * window, which is the case the recorder treats as "this session cannot be clean" and mirrors the
     * display instead.
     */
    public View getRecordingSurfaceView() {
        return launcherGLView != null ? launcherGLView.getRecordingSurfaceView() : null;
    }

    /**
     * Puts the on-screen controls in or out of what a screen recording captures.
     *
     * A recording built from the game's own frames never contains them, which is the preferred path. This
     * is the fallback for when a session captures the display instead: the only way to leave the buttons
     * out of a mirrored display is to stop drawing them, and the only safe way to do that mid-game is
     * alpha, since a hidden control stops receiving touches and the player would be unable to play until
     * the recording ended. The recorder calls this when a session that opted out of controls starts, and
     * when it finishes.
     */
    public void setControlsCapturedInRecording(boolean captured) {
        if (mControlLayout == null) return;
        mControlLayout.setRecordingOpacity(captured ? 1f : 0f);
    }

    @Override
    public void onAttachedToWindow() {
        // A missing instance or unavailable renderer can finish before views are bound.
        if (mControlLayout == null) return;
        // Post to get the correct display dimensions after layout.
        mControlLayout.post(()->{
            Tools.getDisplayMetrics(this);
            loadControls();
            updateDisplayRefreshMode();
        });
    }

    /** Boilerplate binding */
    private void bindValues(){
        mControlLayout = findViewById(R.id.main_control_layout);
        launcherGLView = findViewById(R.id.main_game_render_view);
        drawerLayout = findViewById(R.id.main_drawer_options);
        navDrawer = findViewById(R.id.main_navigation_view);
        loggerView = findViewById(R.id.mainLoggerView);
        touchCharInput = findViewById(R.id.mainTouchCharInput);
        mDrawerPullButton = findViewById(R.id.drawer_button);
        mFpsView = findViewById(R.id.main_fps_view);
        mGameMenuOverlay = findViewById(R.id.game_menu_overlay);
        mAerixClientOverlay = findViewById(R.id.aerix_client_overlay);
        mHotbarView = findViewById(R.id.hotbar_view);
        if (instance != null && instance.isAerixClient && mAerixClientOverlay != null) {
            mAerixClientOverlay.attachForInstance(instance);
        } else if (mControlLayout != null) {
            // The wallpaper that can stand in for the black screen while the game starts. It is added
            // inside the control layout, above the game surface and under the buttons, so the controls
            // and the Aerix menu button stay usable and visible the whole time.
            net.kdt.pojavlaunch.aerix.loading.AerixLoadingWallpaper.attach(mControlLayout);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if(PREF_ENABLE_GYRO && mGyroControl != null) mGyroControl.enable();
        PLATFORM.setHovered(true);
        updateFpsViewVisibility();
    }

    /** Show the frame rate readout only when the player asked for it and the Aerix menu is hidden. */
    private void updateFpsViewVisibility() {
        if (mFpsView == null) return;
        boolean isClient = instance != null && instance.isAerixClient;
        // An Aerix Client instance used to have no readout at all, so "turn the FPS overlay off" was
        // a setting nobody could reach there and "turn it on" nobody could obey: the A overlay now
        // owns that switch, and the launcher's preference still rules everywhere else.
        boolean wanted = isClient
                ? net.kdt.pojavlaunch.aerix.client.AerixClientStore.INSTANCE.getFpsOverlayEnabled()
                : LauncherPreferences.PREF_SHOW_FPS;
        boolean show = wanted
                && (mGameMenuOverlay == null || !mGameMenuOverlay.isMenuShown());
        mFpsView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onPause() {
        mResumedForLaunch = false;
        if (touchCharInput != null) touchCharInput.setKeyboardState(false);
        ContextExecutor.clearActivity();
        hideGameMenu();
        if (mGyroControl != null) mGyroControl.disable();
        // Avoid going through the JNI each time.
        if (Platform.isGrabbing()){
            CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE);
        }
        if(mQuickSettingSideDialog != null) {
            mQuickSettingSideDialog.cancel();
        }
        PLATFORM.setHovered(false);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PLATFORM.setVisible(true);
        net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.onGameResumed(this);
    }

    @Override
    protected void onStop() {
        if (mRecorder != null) mRecorder.onGameStopped();
        PLATFORM.setVisible(false);
        net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.onGameStopped(this);
        super.onStop();
    }

    /**
     * Ask the display for the fastest refresh mode it offers at the game's resolution, if the player
     * switched that on. This is a window attribute, not a game setting: the renderer keeps swapping at
     * its own pace and VSync decides when a frame is presented, so all this can do is stop the panel
     * from being parked at 60 Hz while it is capable of more.
     *
     * Re-applied after configuration changes, because rotation, a fold, or the system dropping the
     * window preference on focus loss all leave the activity asking for nothing.
     */
    private void updateDisplayRefreshMode() {
        final net.kdt.pojavlaunch.utils.RefreshModeChoice.Plan plan =
                LauncherPreferences.PREF_MAX_REFRESH_RATE
                        ? net.kdt.pojavlaunch.utils.DisplayRefreshMode.planFor(this) : null;
        if (plan == null) {
            mRequestedRefreshHz = 0f;
            net.kdt.pojavlaunch.utils.DisplayRefreshMode.clear(this);
            return;
        }
        mRequestedRefreshHz = plan.getRequestedHz();
        if (!net.kdt.pojavlaunch.utils.DisplayRefreshMode.applyTo(this)) return;
        if (mControlLayout == null) return;
        // Deferred because the display policy may not have honoured the request yet, and a layout pass
        // in the meantime would have dropped it. See reportDisplayRefreshOutcome for the read-back.
        mControlLayout.postDelayed(this::reportDisplayRefreshOutcome, 900L);
    }

    /**
     * Read the panel back after a refresh-rate request and say what it ended up at. A max-refresh
     * setting that reports nothing is indistinguishable from one that is ignored, and that difference
     * decides whether the player leaves it on. Within a rounding step of the ask counts as honoured;
     * anything else gets one retry first, because a request that landed on the old mode is usually the
     * window attributes being re-evaluated between the write and the layout pass rather than a refusal.
     */
    private void reportDisplayRefreshOutcome() {
        if (!LauncherPreferences.PREF_MAX_REFRESH_RATE || mRequestedRefreshHz <= 0f) return;
        float active = net.kdt.pojavlaunch.utils.DisplayRefreshMode.activeRefreshHz(this);
        if (active + 0.5f >= mRequestedRefreshHz || active <= 0f) {
            reportRefreshHz(active);
            return;
        }
        if (!net.kdt.pojavlaunch.utils.DisplayRefreshMode.applyTo(this)) return;
        reportRefreshHz(net.kdt.pojavlaunch.utils.DisplayRefreshMode.activeRefreshHz(this));
    }

    /** The one line the player sees, phrased from what the display is doing rather than from the setting. */
    private void reportRefreshHz(float activeHz) {
        if (activeHz <= 0f) return;
        final int reachedHz = (int) Math.round(activeHz);
        final boolean honoured = reachedHz + 1 >= Math.round(mRequestedRefreshHz);
        Logger.appendToLog("Display refresh: " + (honoured ? "confirmed at " : "device kept the panel at ")
                + reachedHz + "Hz (asked for " + (int) Math.round(mRequestedRefreshHz) + "Hz)");
        runOnUiThread(() -> Toast.makeText(GameActivity.this,
                getString(honoured ? R.string.aerix_max_refresh_done : R.string.aerix_max_refresh_refused,
                        reachedHz),
                honoured ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        net.kdt.pojavlaunch.aerix.social.SocialBackend.onGameStopped();
        net.kdt.pojavlaunch.aerix.social.SocialBackend.shutdownBackgroundActivity(getApplicationContext());
        net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.onGameStopped(this);
        // Nothing of ours outlives the activity: if the player backs out during the boot, the
        // wallpaper goes with the screen.
        net.kdt.pojavlaunch.aerix.loading.AerixLoadingWallpaper.detach();
        if (mAerixClientOverlay != null) mAerixClientOverlay.onHostDestroy();
        // The loopback endpoint that gave the game its profile and skin is done with the game.
        net.kdt.pojavlaunch.aerix.bridge.AerixSkinServer.stop();
        if (mRecorder != null) mRecorder.destroy();
        super.onDestroy();
        ContextExecutor.clearActivity();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(mGyroControl != null) mGyroControl.updateOrientation();
        // Layout resize is practically guaranteed on a configuration change, and `onConfigurationChanged`
        // does not implicitly start a layout. So, request a layout and expect the screen dimensions to be valid after the]
        // post.
        if(mControlLayout == null) return;
        mControlLayout.requestLayout();
        mControlLayout.post(()->{
            // Child of mControlLayout, so refreshing size here is correct
            launcherGLView.refreshSize();
            mControlLayout.refreshControlButtonPositions();
            updateDisplayRefreshMode();
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(!(PLATFORM instanceof DummyBackend)) hideLoadingScreen();
        if(launcherGLView != null)  // Useful when backing out of the app
            Tools.MAIN_HANDLER.postDelayed(() -> launcherGLView.refreshSize(), 500);
        mResumedForLaunch = true;
        acknowledgeLaunchWhenReady();
    }

    private void acknowledgeLaunchWhenReady() {
        if (!mResumedForLaunch || !mStartupPrepared || mServiceBinder == null || isFinishing()) return;
        LaunchDiagnostics.mark(this, "Game window resumed and service connected");
        ResultReceiver ack = mStartAck;
        mStartAck = null;
        if (ack != null) {
            try { ack.send(START_ACK_READY, null); }
            catch (Throwable error) { Log.w("GameActivity", "Could not acknowledge game launch", error); }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mRecorder != null) mRecorder.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mRecorder != null && mRecorder.onActivityResult(requestCode, resultCode, data)) return;

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // Reload PREF_DEFAULTCTRL_PATH
            // If the storage root got unmounted/unreadable we won't be able to load the file anyway,
            // and MissingStorageActivity will be started.
            if(!Tools.checkStorageRoot(this)) return;
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runCraft(String versionId, File[] classpath) throws Throwable {
        Logger.appendToLog("--------- Starting game with Launcher Debug!");
        Tools.printLauncherInfo(versionId, instance.getLaunchArgs(), mGameRenderer.getCurrentRenderer(), this);
        JREUtils.redirectAndPrintJRELog();

        // Show Angelica + SFPEW notice
        RenderSpec renderer = mGameRenderer.getCurrentRenderer();
        File gamedir = instance.getGameDirectory();
        if (GameRunner.hasAngelica(gamedir) && renderer instanceof GLESRenderSpec.SFPEWRenderSpec) {
            Toast.makeText(this, R.string.angelica_found_sfpew_notice, Toast.LENGTH_LONG).show();
        }

        if (instance.isAerixClient) {
            net.kdt.pojavlaunch.aerix.client.AerixClientInstaller.syncLocalAssetsAtGameStart(this, instance);
        }
        GameRunner.launchGame(this, account, instance, versionId, classpath, mGameRenderer);
        //Note that we actually stall in the above function, even if the game crashes. But let's be safe.
        Tools.runOnUiThread(()-> mServiceBinder.isActive = false);
    }

    /**
     * Signs the game in as [account] by starting it again.
     *
     * The running game holds the session it was launched with; nothing inside it can be told to
     * become somebody else. So the launcher is brought back up with the chosen account and asked to
     * start the game again - what a player would otherwise have to do by hand, from the dashboard,
     * which is exactly the trip this panel used to send them on instead of switching.
     */
    private void switchAccount(AerixAccount account) {
        if (account == null) return;
        Logger.appendToLog("Switching account to " + account.getUsername()
                + "; restarting the game");
        try {
            // The account is selected by the launcher process, not here: the saved choice is a
            // preference, and this process is about to be killed - a write from here would race
            // with its own death, and the launcher's copy of the preference is the one that counts.
            Tools.restartLauncherActivityAndLaunch(this, account.getKey());
        } catch (Throwable error) {
            Log.e("GameActivity", "Could not switch account", error);
            Tools.showErrorRemote(error);
            return;
        }
        // Let the launch request reach the system before this process dies, so the switch cannot
        // be swallowed on the way out.
        Tools.MAIN_HANDLER.postDelayed(Tools::fullyExit, 700L);
    }

    private void dialogSendCustomKey() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.control_customkey);
        dialog.setItems(KeycodeUtils.generateKeyName(), (dInterface, position) -> KeycodeUtils.execKeyIndex(position));
        dialog.show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        if(ingameControlsEditorListener == null || ingameControlsEditorArrayAdapter == null) return;
        hideGameMenu();

        mControlLayout.setModifiable(true);
        navDrawer.setAdapter(ingameControlsEditorArrayAdapter);
        navDrawer.setOnItemClickListener(ingameControlsEditorListener);
        mDrawerPullButton.setVisibility(View.VISIBLE);
        isInEditor = true;
    }

    private void openLogOutput() {
        hideGameMenu();
        loggerView.setVisibility(View.VISIBLE);
    }

    /** Opens the existing in-game launcher settings (including resolution) from Aerix Client. */
    /** The on-screen cursor is drawn by the launcher, so its scale can follow the slider at once. */
    public void applyAerixPointerPreferences() {
        if (launcherGLView == null || launcherGLView.mCursorView == null) return;
        launcherGLView.mCursorView.setCursorScale(LauncherPreferences.PREF_MOUSESCALE);
        // The chosen cursor image, its opacity and its anchor, straight from the settings page. The
        // skin object caches the decode, so this is cheap to call on every cursor-related change.
        launcherGLView.mCursorView.setCursorSkin(
                net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.INSTANCE.currentBitmap(this),
                net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.INSTANCE.getOpacityPercent(),
                net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.INSTANCE.getAnchoredCenter());
        Logger.appendToLog("Aerix cursor: " + net.kdt.pojavlaunch.aerix.client.AerixCursorSkin.INSTANCE.report(this) + "\n");
    }

    /**
     * The game window is always edge-to-edge. Reserving the cutout produced a black strip next to the
     * rotated game - the launcher's own window already ignores it for exactly this reason - so the
     * player no longer has to find the setting to make it go away.
     */
    @Override
    protected boolean shouldIgnoreNotch() {
        return true;
    }

    public void openAerixQuickSettings() { openQuickSettings(); }

    /** Re-evaluate the frame rate readout, for when the A overlay flips its switch mid-game. */
    public void refreshFpsViewVisibility() {
        updateFpsViewVisibility();
    }

    public void openAerixScreenRecorder() {
        if (mRecorder != null) mRecorder.show();
    }

    private void openQuickSettings() {
        hideGameMenu();
        if(mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = new QuickSettingSideDialog(this, mControlLayout) {
                @Override
                public void onResolutionChanged() {
                    launcherGLView.refreshSize();
                    mHotbarView.onResolutionChanged();
                }

                @Override
                public void onGyroStateChanged() {
                    mGyroControl.updateOrientation();
                    if (PREF_ENABLE_GYRO) {
                        mGyroControl.enable();
                    } else {
                        mGyroControl.disable();
                    }
                }

                @Override
                public void onButtonTransparencyChanged() {
                    mControlLayout.updateButtonOpacity();
                }

                @Override
                public void onCursorScaleChanged() {
                    applyAerixPointerPreferences();
                }

                @Override
                public void onZoomStrengthChanged() {
                    // The bridge reads the strength from the request file, so push it again.
                    net.kdt.pojavlaunch.aerix.client.ClientLiveOptionsBridge.syncAll();
                }
            };
        }
        mQuickSettingSideDialog.appear(true);
    }

    public static void toggleMouse(Context ctx) {
        // Avoid going through the JNI each time.
        if (Platform.isGrabbing()) return;
        GameCursorView cursorView = Tools.getWeakReference(weakCursor);
        if(cursorView == null) return;
        int toastString = 0;
        switch (cursorView.getVisibility()) {
            case View.GONE:
            case View.INVISIBLE:
                toastString = R.string.control_mouseon;
                cursorView.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                toastString = R.string.control_mouseoff;
                cursorView.setVisibility(View.GONE);
                break;
        }

        if(toastString != 0) Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // The menu is on top of the game and takes the back button: if the soft keyboard is up
        // inside In-Game Socials / Downloads it dismisses the keyboard first, then navigates back.
        if (mGameMenuOverlay != null && mGameMenuOverlay.isMenuShown()) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                    || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                return super.dispatchKeyEvent(event);
            }
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (!mGameMenuOverlay.dismissKeyboardIfShown()) {
                        mGameMenuOverlay.handleBackPress();
                    }
                }
                return true;
            }
            // Deliver all other key events (Backspace, Enter, soft keyboard keys, physical keyboard)
            // to the focused editor inside GameMenuOverlayView, and never pass through to GameView.
            super.dispatchKeyEvent(event);
            return true;
        }
        if (mAerixClientOverlay != null && mAerixClientOverlay.isBlockingModalOpen()) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                    || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                return super.dispatchKeyEvent(event);
            }
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mAerixClientOverlay.handleBackPress();
                }
                return true;
            }
            super.dispatchKeyEvent(event);
            return true;
        }
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) mControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = launcherGLView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !touchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true; // We eat it anyway
                CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState(boolean panning) {
        if(touchCharInput != null) {
            touchCharInput.switchKeyboardState();
            GameActivity.mForcedPanningHeight = panning ? -1 : 0;
        }
    }
    public static void toggleKeyboardState(boolean state, int panningHeight) {
        if(touchCharInput != null) {
            touchCharInput.setKeyboardState(state);
            GameActivity.mForcedPanningHeight = panningHeight;
        }
    }

    /**
     * Called once the game's own window is up.
     *
     * There is no loading screen to take down any more: the game activity shows its controls and
     * its overlay from the first frame, so there is nothing covering them to remove. What is left
     * to do here is let the frame rate readout appear now that the game, not the boot, is what is
     * on screen.
     */
    public void hideLoadingScreen(){
        updateFpsViewVisibility();
        // The game window is here: the wallpaper, and anything decoding it, goes now.
        net.kdt.pojavlaunch.aerix.loading.AerixLoadingWallpaper.detach();
        if (mAerixClientOverlay != null) mAerixClientOverlay.onGlWindowReady();
    }

    @Override
    public void onClickedMenu() {
        if (isInEditor) {
            // While the controls are being edited the drawer holds the properties of the button
            // that was picked, which is what the menu button opens there.
            drawerLayout.openDrawer(navDrawer);
            navDrawer.requestLayout();
            return;
        }
        if (mGameMenuOverlay != null && mGameMenuOverlay.isMenuShown()) {
            hideGameMenu();
            return;
        }
        showGameMenu();
    }

    /** Open the full screen in game menu. */
    private void showGameMenu() {
        if (mGameMenuOverlay == null) return;
        drawerLayout.closeDrawers();
        if (touchCharInput != null) touchCharInput.disable();
        if (launcherGLView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launcherGLView.releasePointerCapture();
            }
            launcherGLView.clearFocus();
        }
        // The readout belongs to the game, not to the menu: it would sit on top of the buttons.
        if (mFpsView != null) mFpsView.setVisibility(View.GONE);
        mGameMenuOverlay.showMenu();
    }

    private void hideGameMenu() {
        if (mGameMenuOverlay == null) return;
        mGameMenuOverlay.hideMenu();
        if (launcherGLView != null) {
            launcherGLView.requestFocus();
        }
        updateFpsViewVisibility();
    }

    /**
     * The instance folders belong to the app, so the downloader writes without asking for
     * anything. On the older versions a custom folder on the shared storage still needs the
     * legacy permission, which is what this asks for; on the newer ones the launcher can only
     * report that the folder is not writable.
     */
    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            Toast.makeText(this, R.string.aerix_ingame_download_storage_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void exitEditor() {
        try {
            mControlLayout.loadLayout((CustomControls)null);
            mControlLayout.setModifiable(false);
            System.gc();
            mControlLayout.loadLayout(instance.getLaunchControls());
            mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        } catch (Exception e) {
            Tools.showError(this,e);
        }

        navDrawer.setAdapter(gameActionArrayAdapter);
        navDrawer.setOnItemClickListener(gameActionClickListener);
        isInEditor = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        GameService.LocalBinder localBinder = (GameService.LocalBinder) service;
        mServiceBinder = localBinder;
        launcherGLView.start(localBinder.isActive);
        localBinder.isActive = true;
        acknowledgeLaunchWhenReady();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /*
     * Android 14 (or some devices, at least) seems to dispatch the the captured mouse events as trackball events
     * due to a bug(?) somewhere(????)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkCaptureDispatchConditions(MotionEvent event) {
        int eventSource = event.getSource();
        // On my device, the mouse sends events as a relative mouse device.
        // Not comparing with == here because apparently `eventSource` is a mask that can
        // sometimes indicate multiple sources, like in the case of InputDevice.SOURCE_TOUCHPAD
        // (which is *also* an InputDevice.SOURCE_MOUSE when controlling a cursor)
        return (eventSource & InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource & InputDevice.SOURCE_MOUSE) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(Tools.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev))
            return launcherGLView.dispatchCapturedPointerEvent(ev);
        else return super.dispatchTrackballEvent(ev);
    }
}
