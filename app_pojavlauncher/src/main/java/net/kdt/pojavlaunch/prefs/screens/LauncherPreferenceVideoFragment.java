package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.kdt.pojavlaunch.R;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.mobileglues.MainActivity;
import net.kdt.pojavlaunch.game.renderer.RendererCache;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.game.renderer.extra.GLESProvider;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.game.renderer.MobileGluesWarning;
import net.kdt.pojavlaunch.utils.DisplayRefreshMode;
import net.kdt.pojavlaunch.utils.GpuUtils;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    private Boolean hasAngle = null;
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);

        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio",
                CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100);
        } else {
            resolutionSeekbar.setValue(resolution);
        }

        // Sustained performance is only available since Nougat
        SwitchPreferenceCompat sustainedPerfSwitch = requirePreference("sustainedPerformance",
                SwitchPreferenceCompat.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);

        // The preference fragment persists this key on its own; what is added here is the readback, so
        // the row says what THIS screen can do instead of restating the switch description.
        Preference maxRefreshSwitch = requirePreference(DisplayRefreshMode.PREF_KEY);
        float maxRefreshCeiling = DisplayRefreshMode.ceilingRefreshHz(requireContext());
        float maxRefreshActive = DisplayRefreshMode.activeRefreshHz(requireContext());
        if (maxRefreshCeiling <= 0f) {
            maxRefreshSwitch.setSummary(getString(R.string.aerix_max_refresh_desc));
        } else if (maxRefreshCeiling <= maxRefreshActive + 0.1f) {
            maxRefreshSwitch.setSummary(getString(R.string.aerix_max_refresh_unavailable,
                    Math.round(maxRefreshActive)));
        } else {
            maxRefreshSwitch.setSummary(getString(R.string.aerix_max_refresh_summary,
                    Math.round(maxRefreshCeiling), Math.round(maxRefreshActive)));
        }

        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);

        Preference driverPreference = requirePreference("zinkPreferSystemDriver");
        PackageManager packageManager = driverPreference.getContext().getPackageManager();
        boolean supportsTurnip = GpuUtils.checkVulkanSupport(packageManager) && GpuUtils.getGlInfo().isAdreno();
        driverPreference.setVisible(supportsTurnip);

        // Show ANGLE switch only if AnglePlugin is available
        if(hasAngle == null) {
            hasAngle = new GLESProvider.ExternalAngleProvider(requireContext()).supported();
        }
        SwitchPreferenceCompat angleSwitch = requirePreference("use_angle", SwitchPreferenceCompat.class);
        SwitchPreferenceCompat systemAngleSwitch = requirePreference("use_system_angle", SwitchPreferenceCompat.class);
        angleSwitch.setVisible(hasAngle);
        angleSwitch.setChecked(LauncherPreferences.PREF_USE_ANGLE);
        systemAngleSwitch.setVisible(Build.VERSION.SDK_INT >= 35);
        systemAngleSwitch.setChecked(LauncherPreferences.PREF_USE_SYSTEM_ANGLE);
        angleSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) systemAngleSwitch.setChecked(false);
            return true;
        });
        systemAngleSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) angleSwitch.setChecked(false);
            return true;
        });

        ListPreference rendererListPreference = requirePreference("renderer",
                ListPreference.class);
        RendererCache list = RendererCache.getCompatibleRenderers(getContext());
        rendererListPreference.setEntries(list.rendererDisplayNames);
        rendererListPreference.setEntryValues(list.rendererIds.toArray(new String[0]));
        // The default renderer depends on the device, so it cannot live in the XML as a
        // defaultValue. Show the one the launcher picked until the user changes it.
        rendererListPreference.setValue(LauncherPreferences.PREF_RENDERER);

        // The list writes the choice straight to the preference file, so the warning has to run
        // before that write and take the value back when it is answered with "no".
        rendererListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String chosen = String.valueOf(newValue);
            if (!MobileGluesWarning.applies(chosen)) return true;
            showMobileGluesWarning(chosen);
            return false;
        });

        requirePreference("renderer_settings", Preference.class).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), MainActivity.class));
            return true;
        });

        computeVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if(activity != null) {
            requirePreference("ignoreNotch").setVisible(LauncherPreferences.hasNotch(activity));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    /**
     * Says what MobileGlues is before the preference keeps it: not recommended, experimental, and
     * only for 1.17+, with the renderer this version would rather run on offered as the way out.
     */
    private void showMobileGluesWarning(String chosenTag) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;
        String versionId = null;
        try {
            net.kdt.pojavlaunch.instances.Instance instance =
                    net.kdt.pojavlaunch.instances.Instances.loadSelectedInstance();
            if (instance != null) versionId = instance.versionId;
        } catch (Throwable ignored) {
            // No instance to name: the warning still applies, it just cannot name its alternative.
        }
        String recommended = net.kdt.pojavlaunch.game.renderer.RendererDefaults
                .launchTagForVersion(null, versionId);
        new android.app.AlertDialog.Builder(activity)
                .setTitle(MobileGluesWarning.title())
                .setMessage(MobileGluesWarning.message())
                .setPositiveButton(MobileGluesWarning.confirmLabel(), (dialog, which) -> {
                    LauncherPreferences.DEFAULT_PREF.edit().putString("renderer", chosenTag).apply();
                    LauncherPreferences.loadPreferences(requireContext());
                    ListPreference preference = findPreference("renderer");
                    if (preference != null) preference.setValue(chosenTag);
                    computeVisibility();
                })
                .setNeutralButton(MobileGluesWarning.recommendedLabel(versionId), (dialog, which) -> {
                    LauncherPreferences.DEFAULT_PREF.edit().putString("renderer", recommended).apply();
                    LauncherPreferences.loadPreferences(requireContext());
                    ListPreference preference = findPreference("renderer");
                    if (preference != null) preference.setValue(recommended);
                    computeVisibility();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // Nothing was written, so the list must be put back on the value it still has.
                    ListPreference preference = findPreference("renderer");
                    if (preference != null) preference.setValue(LauncherPreferences.PREF_RENDERER);
                })
                .setCancelable(false)
                .show();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
       boolean isMobileGluesRenderer = Renderers.MOBILEGLUES_RENDERER.equals(LauncherPreferences.PREF_RENDERER);
       requirePreference("renderer_settings", Preference.class).setVisible(isMobileGluesRenderer);
    }
}
