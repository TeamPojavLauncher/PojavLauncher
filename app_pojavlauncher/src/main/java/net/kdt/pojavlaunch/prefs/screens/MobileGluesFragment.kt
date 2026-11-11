package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.appcompat.app.AlertDialog
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.mobileglues.MgController
import net.kdt.pojavlaunch.mobileglues.MgDialogHost
import net.kdt.pojavlaunch.mobileglues.MgDialogs
import net.kdt.pojavlaunch.mobileglues.MgSummaries
import net.kdt.pojavlaunch.mobileglues.settings.AngleConfig
import net.kdt.pojavlaunch.mobileglues.settings.ConfigLoadResult
import net.kdt.pojavlaunch.mobileglues.settings.DepthClearFixMode
import net.kdt.pojavlaunch.mobileglues.settings.Fsr1Preset
import net.kdt.pojavlaunch.mobileglues.settings.GlVersion
import net.kdt.pojavlaunch.mobileglues.settings.GlslCacheScale
import net.kdt.pojavlaunch.mobileglues.settings.MGConfig
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawSettings
import net.kdt.pojavlaunch.mobileglues.settings.NoErrorConfig
import net.kdt.pojavlaunch.mobileglues.settings.SpinnerOption
import kotlinx.coroutines.launch

/**
 * MobileGlues renderer settings: rendering, shader cache, extensions and advanced options.
 * The values live in MobileGlues' own config.json, not in the launcher's shared preferences.
 */
class MobileGluesFragment : LauncherPreferenceFragment() {

    private val controller: MgController by lazy { MgController.get(requireContext()) }
    private var corruptDialog: androidx.appcompat.app.AlertDialog? = null
    private var corruptShown: ConfigLoadResult.Corrupt? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_mobileglues)

        bindEnumList("mg_angle", AngleConfig.entries, ::selectAngle)
        bindEnumList("mg_no_error", NoErrorConfig.entries) { target ->
            controller.update { it.copy(noError = target) }
        }
        bindEnumList("mg_depth_clear", DepthClearFixMode.entries, ::selectDepthClear)
        bindEnumList("mg_gl_version", GlVersion.entries, ::selectGlVersion)

        requirePreference("mg_fsr1", SwitchPreferenceCompat::class.java).setOnPreferenceChangeListener { _, value ->
            setFsr1(value as Boolean)
            false
        }
        requirePreference("mg_ext_cs", SwitchPreferenceCompat::class.java).setOnPreferenceChangeListener { _, value ->
            setComputeShader(value as Boolean)
            false
        }
        // The switch reads "disable timer_query", while the config stores whether it is enabled
        requirePreference("mg_ext_timer_query", SwitchPreferenceCompat::class.java)
            .setOnPreferenceChangeListener { _, value ->
                controller.update { it.copy(extTimerQuery = !(value as Boolean)) }
                false
            }
        requirePreference("mg_ext_dsa", SwitchPreferenceCompat::class.java).setOnPreferenceChangeListener { _, value ->
            controller.update { it.copy(extDirectStateAccess = value as Boolean) }
            false
        }

        requirePreference("mg_glsl_cache").setOnPreferenceClickListener {
            showCacheDialog()
            true
        }
        requirePreference("mg_glsl_cache_delete").setOnPreferenceClickListener {
            controller.deleteGlslCache()
            true
        }
        requirePreference("mg_reset").setOnPreferenceClickListener {
            MgDialogs.confirm(requireContext(), R.string.mg_reset_message) { confirmed ->
                if (confirmed) controller.resetToDefaults()
            }
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.ensureLoaded()
        controller.ensureDeviceInfo()
        MgDialogHost(this, controller).attach()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { controller.config.collect { bindConfig(it) } }
                launch { controller.glslCacheBytes.collect { bindCacheDelete(it) } }
                launch { controller.corrupt.collect { showCorrupt(it) } }
                launch {
                    controller.messages.collect { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        corruptDialog?.dismiss()
        corruptDialog = null
        corruptShown = null
        controller.flushAsync()
    }

    // ---- list setup ----

    private fun <T : SpinnerOption> bindEnumList(
        key: String,
        options: List<T>,
        onSelect: (T) -> Unit,
    ) {
        val list = requirePreference(key, ListPreference::class.java)
        list.entries = options.map { it.label(requireContext()) }.toTypedArray()
        list.entryValues = options.map { it.wire.toString() }.toTypedArray()
        list.setOnPreferenceChangeListener { _, value ->
            options.firstOrNull { it.wire.toString() == value }?.let(onSelect)
            // The visible value follows the config flow, not the tap
            false
        }
    }

    // ---- state to UI ----

    private fun bindConfig(config: MGConfig?) {
        val ready = config != null
        for (key in CONFIG_KEYS) findPreference<Preference>(key)?.isEnabled = ready
        if (config == null) return

        requirePreference("mg_angle", ListPreference::class.java).value = config.angle.wire.toString()
        requirePreference("mg_no_error", ListPreference::class.java).value = config.noError.wire.toString()
        requirePreference("mg_depth_clear", ListPreference::class.java).value = config.depthClearFix.wire.toString()
        requirePreference("mg_gl_version", ListPreference::class.java).value = config.glVersion.wire.toString()

        requirePreference("mg_fsr1", SwitchPreferenceCompat::class.java).isChecked = config.fsr1Enabled
        requirePreference("mg_ext_cs", SwitchPreferenceCompat::class.java).isChecked = config.extComputeShader
        requirePreference("mg_ext_timer_query", SwitchPreferenceCompat::class.java).isChecked = !config.extTimerQuery
        requirePreference("mg_ext_dsa", SwitchPreferenceCompat::class.java).isChecked = config.extDirectStateAccess

        val mebibytes = config.glslCache.mebibytesOrZero
        requirePreference("mg_glsl_cache").summary = cacheLabel(mebibytes)
        requirePreference("mg_multidraw").summary = MgSummaries.multidraw(requireContext(), config.multidraw)
    }

    private fun bindCacheDelete(bytes: Long?) {
        val row = requirePreference("mg_glsl_cache_delete")
        row.isVisible = bytes != null
        if (bytes != null) {
            row.title = getString(R.string.option_glsl_cache_delete, controller.formatCacheSize(bytes))
        }
    }

    private fun cacheLabel(mebibytes: Int): String =
        if (mebibytes > 0) {
            getString(R.string.option_glsl_cache_value, mebibytes)
        } else {
            getString(R.string.option_glsl_cache_off)
        }

    private fun showCorrupt(result: ConfigLoadResult.Corrupt?) {
        if (result == null) {
            corruptDialog?.dismiss()
            corruptDialog = null
            corruptShown = null
            return
        }
        if (corruptShown === result) return
        corruptShown = result
        corruptDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_config_corrupt_title)
            .setMessage(
                getString(
                    R.string.dialog_config_corrupt_message,
                    result.backupName ?: "-",
                    result.cause.message ?: result.cause.javaClass.simpleName,
                ),
            )
            .setPositiveButton(R.string.dialog_config_corrupt_reset) { _, _ -> controller.resetCorruptConfig() }
            .setNegativeButton(R.string.dialog_negative) { _, _ -> controller.dismissCorruptConfig() }
            .setOnCancelListener { controller.dismissCorruptConfig() }
            .show()
    }

    // ---- changes that need a warning first ----

    private fun selectAngle(target: AngleConfig) {
        val current = controller.config.value ?: return
        if (target == current.angle) return
        viewLifecycleOwner.lifecycleScope.launch {
            val risky = target == AngleConfig.ForceEnable && controller.isAdreno740()
            val context = context ?: return@launch
            if (risky) {
                MgDialogs.confirm(context, R.string.warning_adreno_740_angle) { confirmed ->
                    if (confirmed) applyAngle(target)
                }
            } else {
                applyAngle(target)
            }
        }
    }

    private fun applyAngle(target: AngleConfig) {
        val before = controller.config.value ?: return
        controller.update { it.copy(angle = target) }
        // The saved MultiDraw order was measured on the previous driver
        if (before.multidraw != MultidrawSettings.Default) {
            context?.let { ctx ->
                AlertDialog.Builder(ctx)
                    .setMessage(R.string.md_bench_outdated)
                    .setPositiveButton(R.string.md_bench_outdated_action) { _, _ ->
                        controller.runBench(MgController.BenchTarget.AllEntries)
                    }
                    .setNegativeButton(R.string.dialog_negative, null)
                    .show()
            }
        }
    }

    private fun selectDepthClear(target: DepthClearFixMode) {
        val current = controller.config.value ?: return
        if (target == current.depthClearFix) return
        if (target == DepthClearFixMode.Disabled) {
            controller.update { it.copy(depthClearFix = target) }
        } else {
            MgDialogs.confirm(requireContext(), R.string.warning_enabling_angle_clear_workaround) { confirmed ->
                if (confirmed) controller.update { it.copy(depthClearFix = target) }
            }
        }
    }

    private fun selectGlVersion(target: GlVersion) {
        val current = controller.config.value ?: return
        if (target == current.glVersion) return
        if (current.glVersion == GlVersion.Default) {
            MgDialogs.confirm(
                requireContext(),
                R.string.warning_enabling_custom_gl_version,
                countdownSeconds = MgController.CUSTOM_GL_VERSION_COOLDOWN_SECONDS,
                messageIsHtml = true,
            ) { confirmed ->
                if (confirmed) controller.update { it.copy(glVersion = target) }
            }
        } else {
            controller.update { it.copy(glVersion = target) }
        }
    }

    private fun setFsr1(enabled: Boolean) {
        val current = controller.config.value ?: return
        if (enabled == current.fsr1Enabled) return
        if (enabled) {
            MgDialogs.confirm(requireContext(), R.string.warning_fsr1_enable) { confirmed ->
                if (confirmed) controller.update { it.copy(fsr1 = Fsr1Preset.UltraQuality) }
            }
        } else {
            controller.update { it.copy(fsr1 = Fsr1Preset.Disabled) }
        }
    }

    private fun setComputeShader(enabled: Boolean) {
        val current = controller.config.value ?: return
        if (enabled == current.extComputeShader) return
        if (enabled) {
            MgDialogs.confirm(requireContext(), R.string.warning_ext_cs_enable) { confirmed ->
                if (confirmed) controller.update { it.copy(extComputeShader = true) }
            }
        } else {
            controller.update { it.copy(extComputeShader = false) }
        }
    }

    // ---- shader cache size ----

    private fun showCacheDialog() {
        val current = controller.config.value?.glslCache?.mebibytesOrZero ?: return
        val content = layoutInflater.inflate(R.layout.dialog_mg_glsl_cache, null)
        val valueView = content.findViewById<TextView>(R.id.mg_glsl_value)
        val seek = content.findViewById<SeekBar>(R.id.mg_glsl_seek)

        val ramBytes = controller.deviceInfo.value?.totalRamBytes
        val base = ramBytes?.let { GlslCacheScale.baseCeiling(it) } ?: GlslCacheScale.MIN_UPPER_BOUND_MIB.toInt()
        val ceiling = maxOf(base, current)

        seek.max = GlslCacheScale.STEPS
        seek.progress = GlslCacheScale.positionFor(current, ceiling)
        valueView.text = cacheLabel(current)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                valueView.text = cacheLabel(GlslCacheScale.mebibytesAt(progress, ceiling))
                if (fromUser) controller.setGlslCacheSliderPosition(progress, ceiling)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.option_glsl_cache)
            .setView(content)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private companion object {
        val CONFIG_KEYS = listOf(
            "mg_angle", "mg_no_error", "mg_depth_clear", "mg_fsr1",
            "mg_glsl_cache", "mg_glsl_cache_delete",
            "mg_ext_cs", "mg_ext_timer_query", "mg_ext_dsa",
            "mg_gl_version", "mg_multidraw", "mg_reset",
        )
    }
}
