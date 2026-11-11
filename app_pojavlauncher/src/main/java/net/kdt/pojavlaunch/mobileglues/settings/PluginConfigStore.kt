package net.kdt.pojavlaunch.mobileglues.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Launcher-side preferences for the MobileGlues screens. Nothing here is written to MG/config.json.
 */
class PluginConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mutableAngleSource =
        MutableStateFlow(prefs.getString(KEY_ANGLE_SOURCE, null))

    /** Package name of the launcher the user last trusted to borrow ANGLE from. */
    val angleSourcePackage: StateFlow<String?> = mutableAngleSource.asStateFlow()

    fun setAngleSourcePackage(packageName: String?) {
        val editor = prefs.edit()
        if (packageName == null) editor.remove(KEY_ANGLE_SOURCE) else editor.putString(KEY_ANGLE_SOURCE, packageName)
        editor.apply()
        mutableAngleSource.value = packageName
    }

    private companion object {
        const val PREFS_NAME = "mobileglues_plugin_config"
        const val KEY_ANGLE_SOURCE = "angle_source_package"
    }
}
