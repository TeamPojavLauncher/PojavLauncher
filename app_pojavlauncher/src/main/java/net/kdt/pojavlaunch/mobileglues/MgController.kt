package net.kdt.pojavlaunch.mobileglues

import android.content.Context
import android.os.DeadObjectException
import androidx.annotation.StringRes
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.mobileglues.settings.AngleConfig
import net.kdt.pojavlaunch.mobileglues.settings.AngleProvider
import net.kdt.pojavlaunch.mobileglues.settings.AngleSource
import net.kdt.pojavlaunch.mobileglues.settings.ConfigLoadResult
import net.kdt.pojavlaunch.mobileglues.settings.ConfigStoreEvent
import net.kdt.pojavlaunch.mobileglues.settings.DirectMgStorage
import net.kdt.pojavlaunch.mobileglues.settings.GlslCacheScale
import net.kdt.pojavlaunch.mobileglues.settings.GlslCacheSize
import net.kdt.pojavlaunch.mobileglues.settings.MGCacheExporter
import net.kdt.pojavlaunch.mobileglues.settings.MGConfig
import net.kdt.pojavlaunch.mobileglues.settings.MGConfigStore
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawBackend
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawBenchAnalyzer
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawBenchQuality
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawBenchReport
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawEntry
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawOrderItem
import net.kdt.pojavlaunch.mobileglues.settings.PluginConfigStore
import net.kdt.pojavlaunch.mobileglues.settings.RankedItem
import net.kdt.pojavlaunch.mobileglues.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Process-wide holder of everything the MobileGlues fragments share: the config store,
 * the benchmark and GL info runners and the state they expose as flows.
 *
 * It lives outside the fragments so a running benchmark survives screen rotation and
 * navigation between the MobileGlues pages.
 */
class MgController private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val query = MgQuery(appContext)

    val pluginConfig = PluginConfigStore(appContext)
    val configStore = MGConfigStore(DirectMgStorage(File(Constants.MG_DIRECTORY)), scope)
    private val cacheExporter = MGCacheExporter(appContext, configStore)

    val config: StateFlow<MGConfig?> get() = configStore.config
    val glslCacheBytes: StateFlow<Long?> get() = configStore.glslCacheBytes

    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Short one-off messages for the UI to toast, such as a failed save. */
    val messages: SharedFlow<String> = mutableMessages.asSharedFlow()

    enum class LoadState { NotLoaded, Loading, Ready }

    private val mutableLoadState = MutableStateFlow(LoadState.NotLoaded)
    val loadState: StateFlow<LoadState> = mutableLoadState.asStateFlow()

    private val mutableCorrupt = MutableStateFlow<ConfigLoadResult.Corrupt?>(null)

    /** Set while config.json is broken and the user has not decided what to do about it. */
    val corrupt: StateFlow<ConfigLoadResult.Corrupt?> = mutableCorrupt.asStateFlow()

    private var loadJob: Job? = null

    init {
        scope.launch {
            configStore.events.collect { event ->
                when (event) {
                    is ConfigStoreEvent.SaveFailed -> mutableMessages.tryEmit(
                        appContext.getString(
                            R.string.config_save_failed,
                            event.cause.message ?: event.cause.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    // ---- loading ----

    fun ensureLoaded() {
        if (mutableLoadState.value == LoadState.Ready) {
            configStore.refreshGlslCacheFile()
            return
        }
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            mutableLoadState.value = LoadState.Loading
            when (val result = configStore.load()) {
                ConfigLoadResult.Missing -> {
                    configStore.flush()
                    mutableLoadState.value = LoadState.Ready
                }
                is ConfigLoadResult.Loaded -> mutableLoadState.value = LoadState.Ready
                is ConfigLoadResult.Corrupt -> {
                    mutableLoadState.value = LoadState.NotLoaded
                    mutableCorrupt.value = result
                }
            }
        }
    }

    /** The user agreed to replace a damaged config.json with the defaults. */
    fun resetCorruptConfig() {
        mutableCorrupt.value = null
        resetToDefaults()
    }

    /** The user chose to leave a damaged config.json alone. */
    fun dismissCorruptConfig() {
        mutableCorrupt.value = null
    }

    fun resetToDefaults() {
        scope.launch {
            configStore.resetToDefaults()
            mutableLoadState.value = LoadState.Ready
        }
    }

    fun flushAsync() = configStore.flushAsync()

    // ---- device ----

    private val mutableDeviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = mutableDeviceInfo.asStateFlow()

    fun ensureDeviceInfo() {
        if (mutableDeviceInfo.value != null) return
        scope.launch { mutableDeviceInfo.value = DeviceInfoProvider.get(appContext) }
    }

    suspend fun isAdreno740(): Boolean = DeviceInfoProvider.isAdreno740(appContext)

    // ---- settings ----

    fun update(transform: (MGConfig) -> MGConfig) = configStore.update(transform)

    fun setGlslCacheSliderPosition(position: Int, ceiling: Int) {
        update { it.copy(glslCache = GlslCacheSize.ofMebibytes(GlslCacheScale.mebibytesAt(position, ceiling))) }
    }

    fun deleteGlslCache() {
        scope.launch {
            configStore.clearGlslCache().onFailure { cause ->
                mutableMessages.tryEmit(
                    appContext.getString(
                        R.string.option_glsl_cache_delete_failed,
                        cause.message ?: cause.javaClass.simpleName,
                    ),
                )
            }
        }
    }

    fun formatCacheSize(bytes: Long): String {
        val mebibytes = bytes / (1024.0 * 1024.0)
        return if (mebibytes >= 1.0) {
            appContext.getString(R.string.option_glsl_cache_size_mib, mebibytes)
        } else {
            appContext.getString(R.string.option_glsl_cache_size_kib, bytes / 1024.0)
        }
    }

    // ---- MultiDraw order ----

    fun moveGlobalItem(from: Int, to: Int) {
        update {
            val order = it.multidraw.globalOrder.toMutableList()
            if (!order.moveItem(from, to)) return@update it
            it.copy(multidraw = it.multidraw.withGlobalOrder(order))
        }
    }

    fun resetGlobalOrder() {
        update { it.copy(multidraw = it.multidraw.withGlobalOrder(MultidrawOrderItem.DefaultOrder)) }
    }

    fun setException(entry: MultidrawEntry, enabled: Boolean) {
        update { it.copy(multidraw = it.multidraw.withException(entry, enabled)) }
    }

    fun resetExceptionOrder(entry: MultidrawEntry) {
        update {
            it.copy(multidraw = it.multidraw.withExceptionOrder(entry, it.multidraw.globalOrderFor(entry)))
        }
    }

    fun moveExceptionItem(entry: MultidrawEntry, from: Int, to: Int) {
        update {
            val order = it.multidraw.effectiveOrderFor(entry).toMutableList()
            if (!order.moveItem(from, to)) return@update it
            it.copy(multidraw = it.multidraw.withExceptionOrder(entry, order))
        }
    }

    private fun <T> MutableList<T>.moveItem(from: Int, to: Int): Boolean {
        if (from == to || from !in indices || to !in indices) return false
        add(to, removeAt(from))
        return true
    }

    // ---- ANGLE source ----

    sealed interface AngleUse {
        data class Bench(val target: BenchTarget) : AngleUse
        data object GlInfo : AngleUse
    }

    data class AngleSourcePrompt(
        val use: AngleUse,
        val sources: List<AngleSource>,
        val lastChosen: String?,
    )

    private val mutableAngleSourcePrompt = MutableStateFlow<AngleSourcePrompt?>(null)
    val angleSourcePrompt: StateFlow<AngleSourcePrompt?> = mutableAngleSourcePrompt.asStateFlow()

    private fun promptForAngle(use: AngleUse) {
        // Copper ships its own ANGLE, so there is nothing to borrow from Zalith or FoldCraft
        // and nobody to ask: run with the bundled one right away
        AngleProvider.bundled(appContext)?.let { bundled ->
            resumeWithAngle(use, bundled.libraryDir)
            return
        }
        val last = pluginConfig.angleSourcePackage.value
        val found = AngleProvider.sources(appContext)
        mutableAngleSourcePrompt.value = AngleSourcePrompt(
            use = use,
            sources = found.sortedByDescending { it.packageName == last },
            lastChosen = last,
        )
    }

    /** Whether the saved config makes the game run on ANGLE, which a benchmark or GL query must borrow. */
    fun needsAngle(): Boolean = when (configStore.config.value?.angle) {
        AngleConfig.ForceEnable, AngleConfig.EnableIfPossible -> true
        else -> false
    }

    fun confirmAngleSource(source: AngleSource) {
        val pending = mutableAngleSourcePrompt.value ?: return
        pluginConfig.setAngleSourcePackage(source.packageName)
        mutableAngleSourcePrompt.value = null
        resumeWithAngle(pending.use, source.libraryDir)
    }

    fun continueWithoutAngle() {
        val pending = mutableAngleSourcePrompt.value ?: return
        mutableAngleSourcePrompt.value = null
        resumeWithAngle(pending.use, null)
    }

    fun dismissAngleSourcePrompt() {
        mutableAngleSourcePrompt.value = null
    }

    private fun resumeWithAngle(use: AngleUse, angleDirectory: String?) {
        when (use) {
            is AngleUse.Bench -> startBench(use.target, angleDirectory)
            is AngleUse.GlInfo -> startGlInfo(angleDirectory)
        }
    }

    // ---- MultiDraw benchmark ----

    sealed interface BenchTarget {
        data object AllEntries : BenchTarget
        data class Entry(val entry: MultidrawEntry) : BenchTarget
    }

    sealed interface BenchState {
        data class Running(
            val target: BenchTarget,
            /** 0f..1f, null while the renderer reports no progress. */
            val progress: Float? = null,
            val attempt: Int = 1,
            val retryingAtSections: Int? = null,
        ) : BenchState

        data class Done(
            val target: BenchTarget,
            val rankings: Map<MultidrawEntry, List<RankedItem<MultidrawBackend>>>,
            val quality: Map<MultidrawEntry, MultidrawBenchQuality> = emptyMap(),
            val angleNote: BenchAngleNote? = null,
        ) : BenchState {
            val anyNoisy: Boolean get() = quality.values.any { it.noisy }
            val driverMismatch: Boolean
                get() = angleNote == BenchAngleNote.SystemInsteadOfAngle ||
                    angleNote == BenchAngleNote.BorrowFailed
        }

        data class Failed(val message: String) : BenchState
    }

    enum class BenchAngleNote(@param:StringRes val messageRes: Int) {
        SystemInsteadOfAngle(R.string.md_bench_wrong_driver),
        BorrowFailed(R.string.md_bench_borrow_failed),
        BorrowUnsupported(R.string.md_bench_borrow_unsupported),
    }

    private val mutableBenchState = MutableStateFlow<BenchState?>(null)
    val benchState: StateFlow<BenchState?> = mutableBenchState.asStateFlow()

    fun runBench(target: BenchTarget) {
        if (mutableBenchState.value is BenchState.Running) return
        if (needsAngle()) {
            promptForAngle(AngleUse.Bench(target))
            return
        }
        startBench(target, null)
    }

    private fun startBench(target: BenchTarget, angleDirectory: String?) {
        if (mutableBenchState.value is BenchState.Running) return
        mutableBenchState.value = BenchState.Running(target)
        scope.launch {
            val directory = cacheExporter.export().getOrElse { cacheExporter.directory }
            val report = runBenchWithBackoff(target, directory.path, angleDirectory)
            val rankings = when (target) {
                is BenchTarget.AllEntries -> MultidrawEntry.entries
                    .associateWith { MultidrawBenchAnalyzer.rankEntry(report, it) }
                    .filterValues { ranking -> ranking.any { it.relativeCost != null } }
                is BenchTarget.Entry ->
                    mapOf(target.entry to MultidrawBenchAnalyzer.rankEntry(report, target.entry))
            }
            mutableBenchState.value = when {
                report.error != null -> BenchState.Failed(benchErrorMessage(report.error))
                rankings.isEmpty() -> BenchState.Failed(
                    appContext.getString(R.string.md_bench_failed, appContext.getString(R.string.md_bench_nothing)),
                )
                else -> BenchState.Done(
                    target = target,
                    rankings = rankings,
                    quality = report.quality.filterKeys { it in rankings },
                    angleNote = benchAngleNote(report, borrowed = angleDirectory != null),
                )
            }
        }
    }

    /**
     * Runs the benchmark, and when the GL context is lost starts again on a smaller scene.
     * Every retry binds a fresh query process, so a crashed driver cannot affect the next run.
     */
    private suspend fun runBenchWithBackoff(
        target: BenchTarget,
        mgDirectory: String,
        angleDirectory: String?,
    ): MultidrawBenchReport {
        var start = BENCH_START_SECTIONS
        var ceiling = 0
        var report = runBenchOnce(mgDirectory, angleDirectory, start, ceiling)
        var retries = 0
        while (report.error == "context-lost" && retries < BENCH_MAX_BACKOFFS) {
            val crashed = report.sections.takeIf { it > 0 } ?: start
            val next = crashed / 2
            if (next < BENCH_MIN_SECTIONS) break
            ++retries
            start = next
            ceiling = next
            mutableBenchState.value = BenchState.Running(target, retryingAtSections = next)
            report = runBenchOnce(mgDirectory, angleDirectory, start, ceiling)
        }
        return report
    }

    private suspend fun runBenchOnce(
        mgDirectory: String,
        angleDirectory: String?,
        startSections: Int,
        maxSections: Int,
    ): MultidrawBenchReport = try {
        query.use { remote ->
            coroutineScope {
                val measuring = async(Dispatchers.IO) {
                    runCatching {
                        MultidrawBenchReport.parse(
                            remote.runBench(mgDirectory, angleDirectory.orEmpty(), startSections, maxSections),
                        )
                    }.getOrElse { MultidrawBenchReport(emptyMap(), error = queryFailure(it)) }
                }
                val polling = launch {
                    while (isActive) {
                        delay(BENCH_PROGRESS_POLL_MS)
                        val raw = withContext(Dispatchers.IO) {
                            runCatching { remote.benchProgress() }.getOrDefault(-1)
                        }
                        val progress = MGBench.decodeProgress(raw) ?: continue
                        val running = mutableBenchState.value as? BenchState.Running ?: break
                        mutableBenchState.value = running.copy(
                            progress = progress.fraction,
                            attempt = progress.attempt.coerceIn(1, BENCH_MAX_ATTEMPTS),
                        )
                    }
                }
                try {
                    measuring.await()
                } finally {
                    polling.cancel()
                }
            }
        }
    } catch (e: Exception) {
        MultidrawBenchReport(timings = emptyMap(), error = queryFailure(e))
    }

    private fun benchErrorMessage(error: String): String = when (error) {
        "context-lost" -> appContext.getString(R.string.md_bench_context_lost)
        else -> appContext.getString(R.string.md_bench_failed, error)
    }

    private fun queryFailure(e: Throwable): String = when (e) {
        is DeadObjectException -> appContext.getString(R.string.md_bench_process_died)
        else -> e.message ?: e.javaClass.simpleName
    }

    private fun benchAngleNote(report: MultidrawBenchReport, borrowed: Boolean): BenchAngleNote? = when {
        borrowed && report.angleInUse -> null
        borrowed && report.angleConfigured == AngleConfig.EnableIfPossible.wire && !report.angleSupported ->
            BenchAngleNote.BorrowUnsupported
        borrowed -> BenchAngleNote.BorrowFailed
        report.wrongDriver -> BenchAngleNote.SystemInsteadOfAngle
        else -> null
    }

    /** Writes the measured order of every ranked function as that function's own order. */
    fun adoptBenchResult() {
        val done = mutableBenchState.value as? BenchState.Done ?: return
        update { config ->
            val multidraw = done.rankings.entries.fold(config.multidraw) { settings, (entry, ranking) ->
                settings.withExceptionOrder(entry, ranking.map { it.item })
            }
            config.copy(multidraw = multidraw)
        }
        mutableBenchState.value = null
    }

    fun dismissBench() {
        if (mutableBenchState.value is BenchState.Running) return
        mutableBenchState.value = null
    }

    // ---- GL info ----

    enum class GlInfoAngle { System, Borrowed, BorrowIneffective }

    private val mutableGlInfo = MutableStateFlow<String?>(null)
    val glInfo: StateFlow<String?> = mutableGlInfo.asStateFlow()

    private val mutableGlInfoLoading = MutableStateFlow(false)
    val glInfoLoading: StateFlow<Boolean> = mutableGlInfoLoading.asStateFlow()

    private val mutableGlInfoAngle = MutableStateFlow(GlInfoAngle.System)
    val glInfoAngle: StateFlow<GlInfoAngle> = mutableGlInfoAngle.asStateFlow()

    /** The info came from the system driver while the config makes the game use ANGLE. */
    val glInfoNeedsAngle: StateFlow<Boolean> =
        combine(glInfo, glInfoAngle, configStore.config) { info, angle, _ ->
            info != null && angle == GlInfoAngle.System && needsAngle()
        }.stateIn(scope, SharingStarted.Eagerly, false)

    fun loadGlInfo() {
        if (mutableGlInfo.value == null) startGlInfo(null)
    }

    fun reloadGlInfoWithAngle() {
        if (mutableGlInfoLoading.value) return
        promptForAngle(AngleUse.GlInfo)
    }

    private fun startGlInfo(angleDirectory: String?) {
        if (mutableGlInfoLoading.value) return
        mutableGlInfoLoading.value = true
        mutableGlInfo.value = null
        scope.launch {
            val directory = cacheExporter.export().getOrElse { cacheExporter.directory }
            val info = try {
                query.use { remote ->
                    withContext(Dispatchers.IO) { remote.glInfo(directory.path, angleDirectory.orEmpty()) }
                }
            } catch (e: Exception) {
                "Error: ${queryFailure(e)}"
            }
            mutableGlInfo.value = info
            mutableGlInfoAngle.value = when {
                angleDirectory == null -> GlInfoAngle.System
                info.contains("ANGLE in use: yes") -> GlInfoAngle.Borrowed
                info.contains("ANGLE in use:") -> GlInfoAngle.BorrowIneffective
                else -> GlInfoAngle.Borrowed
            }
            mutableGlInfoLoading.value = false
        }
    }

    companion object {
        const val CUSTOM_GL_VERSION_COOLDOWN_SECONDS = 41
        const val BENCH_MAX_ATTEMPTS = 4

        private const val BENCH_PROGRESS_POLL_MS = 100L
        private const val BENCH_START_SECTIONS = 256
        private const val BENCH_MIN_SECTIONS = 32
        private const val BENCH_MAX_BACKOFFS = 3

        @Volatile
        private var instance: MgController? = null

        @JvmStatic
        fun get(context: Context): MgController =
            instance ?: synchronized(this) {
                instance ?: MgController(context).also { instance = it }
            }
    }
}
