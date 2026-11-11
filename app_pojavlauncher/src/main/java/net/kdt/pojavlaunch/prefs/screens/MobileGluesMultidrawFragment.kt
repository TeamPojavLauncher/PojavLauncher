package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.mobileglues.MgController
import net.kdt.pojavlaunch.mobileglues.MgDialogHost
import net.kdt.pojavlaunch.mobileglues.settings.MGConfig
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawEntry
import kotlinx.coroutines.launch

/** Entry page of the MultiDraw settings: the global order, the benchmark and one row per function. */
class MobileGluesMultidrawFragment : LauncherPreferenceFragment() {

    private val controller: MgController by lazy { MgController.get(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_mobileglues_multidraw)

        requirePreference("mg_md_bench_all").setOnPreferenceClickListener {
            controller.runBench(MgController.BenchTarget.AllEntries)
            true
        }

        val category = requirePreference("mg_md_entries", PreferenceCategory::class.java)
        for (entry in MultidrawEntry.entries) {
            val row = Preference(preferenceManager.context).apply {
                key = ENTRY_KEY_PREFIX + entry.name
                title = entry.glFunction
                isPersistent = false
                fragment = MobileGluesMultidrawOrderFragment::class.java.name
                extras.putString(MobileGluesMultidrawOrderFragment.ARG_ENTRY, entry.name)
            }
            category.addPreference(row)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.ensureLoaded()
        MgDialogHost(this, controller).attach()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                controller.config.collect { bind(it) }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        controller.flushAsync()
    }

    private fun bind(config: MGConfig?) {
        val ready = config != null
        requirePreference("mg_md_global").isEnabled = ready
        requirePreference("mg_md_bench_all").isEnabled = ready
        for (entry in MultidrawEntry.entries) {
            findPreference<Preference>(ENTRY_KEY_PREFIX + entry.name)?.isEnabled = ready
        }
        if (config == null) return

        val settings = config.multidraw
        requirePreference("mg_md_global").summary = getString(
            if (settings.globalCustomized) R.string.md_summary_global_custom else R.string.md_summary_global_default,
        )
        for (entry in MultidrawEntry.entries) {
            findPreference<Preference>(ENTRY_KEY_PREFIX + entry.name)?.summary = getString(
                if (settings.hasException(entry)) R.string.md_exception_on else R.string.md_exception_off,
            )
        }
    }

    private companion object {
        const val ENTRY_KEY_PREFIX = "mg_md_entry_"
    }
}
