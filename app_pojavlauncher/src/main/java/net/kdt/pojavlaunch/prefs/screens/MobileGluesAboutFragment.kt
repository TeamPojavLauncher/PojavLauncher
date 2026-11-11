package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools

/** Who makes MobileGlues, where its source lives and a link to the credits page. */
class MobileGluesAboutFragment : LauncherPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_mobileglues_about)

        openOnClick("mg_repo_release", RELEASE_URL)
        openOnClick("mg_repo_renderer", RENDERER_URL)
        openOnClick("mg_credits", CREDITS_URL)
    }

    private fun openOnClick(key: String, url: String) {
        requirePreference(key).setOnPreferenceClickListener {
            activity?.let { Tools.openURL(it, url) }
            true
        }
    }

    private companion object {
        const val RELEASE_URL = "https://github.com/MobileGL-Dev/MobileGlues-release"
        const val RENDERER_URL = "https://github.com/MobileGL-Dev/MobileGlues"
        const val CREDITS_URL = "https://github.com/CopperLauncher/Copper-Info/blob/main/Mobile-Glues-Credits.md"
    }
}
