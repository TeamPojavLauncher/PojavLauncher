package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.kdt.pojavlaunch.utils.GpuUtils;

import net.kdt.pojavlaunch.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
        SwitchPreferenceCompat pref = requirePreference("freedrenoSysmem", SwitchPreferenceCompat.class);
        boolean hasFreedreno = GpuUtils.getGlInfo().isAdreno();
        pref.setVisible(hasFreedreno);
    }
}
