package net.kdt.pojavlaunch.prefs.screens;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.FileUtils;

public class LauncherPreferenceMobileGluseSettingsFragment extends LauncherPreferenceFragment {

    private static final int GLSL_CACHE_MAX_MIB = 512;
    private static final int GLSL_CACHE_OFF = -1;

    private File configFile;
    private CustomSeekBarPreference mGlslCachePreference;


    @Override
    public void onCreatePreferences(Bundle b, String str) {

        configFile = new File(Tools.DIR_DATA + "/MobileGlues", "config.json");

        addPreferencesFromResource(R.xml.pref_mobilegluse);

        LauncherPreferences.loadPreferences(requireContext());

        setupGlslCachePreference();

        SharedPreferences preferences =
                getPreferenceManager().getSharedPreferences();

        preferences.registerOnSharedPreferenceChangeListener(this);

        writeMobileGluesConfig(preferences);
    }

    private void setupGlslCachePreference() {
        mGlslCachePreference = requirePreference("maxGlslCacheSize", CustomSeekBarPreference.class);
        mGlslCachePreference.setRange(GLSL_CACHE_OFF, GLSL_CACHE_MAX_MIB);
        mGlslCachePreference.setSuffix(" MB");
        mGlslCachePreference.setValue(LauncherPreferences.PREF_MG_GLSL_CACHE_MIB);
        updateGlslCacheSummary();
    }

    private void updateGlslCacheSummary() {
        int value = mGlslCachePreference.getValue();
        mGlslCachePreference.setSummary(value <= GLSL_CACHE_OFF ? "Off" : value + " MB");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences preferences,
            String key
    ) {
        if ("maxGlslCacheSize".equals(key)) {
            updateGlslCacheSummary();
        }
        writeMobileGluesConfig(preferences);
    }


    private void writeMobileGluesConfig(
            SharedPreferences preferences
    ) {

        try {

            LauncherPreferences.loadPreferences(requireContext());

            JsonObject config = new JsonObject();


            config.addProperty(
                    "enableANGLE",
                    LauncherPreferences.PREF_MG_ANGLE
            );

            config.addProperty(
                    "enableNoError",
                    LauncherPreferences.PREF_MG_NO_ERROR
            );

            config.addProperty(
                    "enableExtTimerQuery",
                    LauncherPreferences.PREF_MG_EXT_TIMER_QUERY
            );

            config.addProperty(
                    "enableExtComputeShader",
                    LauncherPreferences.PREF_MG_EXT_COMPUTE_SHADER
            );

            config.addProperty(
                    "enableExtDirectStateAccess",
                    LauncherPreferences.PREF_MG_EXT_DIRECT_STATE_ACCESS
            );

            config.addProperty(
                    "maxGlslCacheSize",
                    LauncherPreferences.PREF_MG_GLSL_CACHE_MIB
            );

            config.addProperty(
                    "angleDepthClearFixMode",
                    LauncherPreferences.PREF_MG_DEPTH_CLEAR_FIX
            );

            config.addProperty(
                    "customGLVersion",
                    LauncherPreferences.PREF_MG_GL_VERSION
            );

            config.addProperty(
                    "fsr1Setting",
                    LauncherPreferences.PREF_MG_FSR1
            );


            FileUtils.ensureParentDirectory(configFile);


            try (FileWriter writer = new FileWriter(configFile)) {

                writer.write(
                        Tools.GLOBAL_GSON.toJson(config)
                );
            }


            Logger.appendToLog(
                    "MobileGlues config generated: "
                            + Tools.GLOBAL_GSON.toJson(config)
            );


        } catch (IOException | RuntimeException e) {

            Logger.appendToLog(
                    "Failed writing MobileGlues config: " + e
            );
        }
    }
}
