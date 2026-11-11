package net.kdt.pojavlaunch.mobileglues

import android.content.Context
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawBenchQuality
import net.kdt.pojavlaunch.mobileglues.utils.ThemeColors
import kotlinx.coroutines.launch
import java.util.Locale

object MgDialogs {

    /**
     * Asks for confirmation before a risky change is applied.
     *
     * With [countdownSeconds] above zero the confirm button stays disabled until the countdown ends
     * and the message is drawn as HTML, where `@colorError` is replaced by the theme error color.
     * [onResult] runs once: true for the confirm button, false for anything else.
     */
    @JvmStatic
    fun confirm(
        context: Context,
        @StringRes messageRes: Int,
        countdownSeconds: Int = 0,
        messageIsHtml: Boolean = false,
        onResult: (Boolean) -> Unit,
    ) {
        var resolved = false
        fun resolve(result: Boolean) {
            if (resolved) return
            resolved = true
            onResult(result)
        }

        val raw = context.getString(messageRes)
        val message: CharSequence = if (messageIsHtml) {
            val hex = String.format("#%06X", 0xFFFFFF and ThemeColors.error(context))
            HtmlCompat.fromHtml(raw.replace("@colorError", hex), HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            raw
        }

        var timer: CountDownTimer? = null
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.dialog_title_warning)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> resolve(true) }
            .setNegativeButton(R.string.dialog_negative) { _, _ -> resolve(false) }
            .setOnCancelListener { resolve(false) }
            .setOnDismissListener {
                timer?.cancel()
                resolve(false)
            }
            .create()
        dialog.show()

        if (countdownSeconds > 0) {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = false
            positive.setTextColor(ThemeColors.error(context))
            positive.text = context.getString(R.string.ok_with_countdown, countdownSeconds)
            timer = object : CountDownTimer(countdownSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val left = ((millisUntilFinished + 999L) / 1000L).toInt()
                    positive.text = context.getString(R.string.ok_with_countdown, left)
                }

                override fun onFinish() {
                    positive.text = context.getString(R.string.ok)
                    positive.isEnabled = true
                }
            }.start()
        }
    }

    /** Plain dialog with a title, a message and an OK button. */
    @JvmStatic
    fun message(context: Context, title: CharSequence, message: CharSequence, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> }
            .setOnDismissListener { onDismiss() }
            .show()
    }
}

/**
 * Shows the benchmark progress, the benchmark result and the ANGLE source picker for a fragment.
 *
 * The state lives in [MgController], so the dialogs are rebuilt from it whenever the fragment
 * becomes visible again and are removed when it stops.
 */
class MgDialogHost(private val fragment: Fragment, private val controller: MgController) {

    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var resultDialog: AlertDialog? = null
    private var resultState: MgController.BenchState? = null
    private var angleDialog: AlertDialog? = null
    private var anglePrompt: MgController.AngleSourcePrompt? = null

    private val context: Context get() = fragment.requireContext()

    fun attach() {
        val owner = fragment.viewLifecycleOwner
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = dismissAll()
        })
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { controller.benchState.collect { renderBench(it) } }
                launch { controller.angleSourcePrompt.collect { renderAngle(it) } }
            }
        }
    }

    private fun dismissAll() {
        progressDialog?.dismiss()
        progressDialog = null
        progressBar = null
        progressText = null
        resultDialog?.dismiss()
        resultDialog = null
        resultState = null
        angleDialog?.dismiss()
        angleDialog = null
        anglePrompt = null
    }

    // ---- benchmark ----

    private fun renderBench(state: MgController.BenchState?) {
        when (state) {
            is MgController.BenchState.Running -> {
                dismissResult()
                showProgress(state)
            }
            is MgController.BenchState.Done -> {
                dismissProgress()
                if (resultState !== state) showDone(state)
            }
            is MgController.BenchState.Failed -> {
                dismissProgress()
                if (resultState !== state) showFailed(state)
            }
            null -> {
                dismissProgress()
                dismissResult()
            }
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
        progressBar = null
        progressText = null
    }

    private fun dismissResult() {
        resultDialog?.dismiss()
        resultDialog = null
        resultState = null
    }

    private fun showProgress(state: MgController.BenchState.Running) {
        if (progressDialog == null) {
            val content = LayoutInflater.from(context).inflate(R.layout.dialog_mg_bench_progress, null)
            progressBar = content.findViewById(R.id.mg_bench_progress_bar)
            progressText = content.findViewById(R.id.mg_bench_progress_text)
            progressDialog = AlertDialog.Builder(context)
                .setTitle(R.string.md_bench_running_title)
                .setView(content)
                .setCancelable(false)
                .create()
                .also { it.show() }
        }
        val bar = progressBar ?: return
        val progress = state.progress
        if (progress == null) {
            bar.isIndeterminate = true
        } else {
            bar.isIndeterminate = false
            bar.max = PROGRESS_STEPS
            bar.progress = (progress * PROGRESS_STEPS).toInt()
        }
        progressText?.text = when {
            state.retryingAtSections != null ->
                context.getString(R.string.md_bench_running_smaller, state.retryingAtSections)
            state.attempt > 1 ->
                context.getString(R.string.md_bench_running_retry, state.attempt, MgController.BENCH_MAX_ATTEMPTS)
            else -> context.getString(R.string.md_bench_running_msg)
        }
    }

    private fun showDone(state: MgController.BenchState.Done) {
        dismissResult()
        val column = column()
        val intro = when (val target = state.target) {
            is MgController.BenchTarget.AllEntries -> context.getString(R.string.md_bench_result_all_intro)
            is MgController.BenchTarget.Entry ->
                context.getString(R.string.md_bench_result_entry_intro, target.entry.glFunction)
        }
        column.addView(text(intro, TextView.BufferType.NORMAL))
        state.angleNote?.let { note ->
            column.addView(text(context.getString(note.messageRes), color = ThemeColors.error(context), topMargin = 12))
        }
        val showHeadings = state.target is MgController.BenchTarget.AllEntries
        state.rankings.forEach { (entry, ranking) ->
            if (showHeadings) {
                column.addView(
                    text(entry.glFunction, color = ThemeColors.primary(context), topMargin = 16, bold = true),
                )
            } else {
                column.addView(space(12))
            }
            ranking.forEachIndexed { index, ranked ->
                column.addView(rankedRow(index + 1, ranked.item.label(context).toString(), ranked.relativeCost))
            }
            if (ranking.count { it.relativeCost != null } == 1) {
                column.addView(text(context.getString(R.string.md_bench_single_candidate), size = SMALL_TEXT, topMargin = 4))
            }
            qualityNotes(state.quality[entry]).forEach { column.addView(it) }
        }
        val adoptLabel = if (state.anyNoisy || state.driverMismatch) {
            R.string.md_bench_adopt_anyway
        } else {
            R.string.md_bench_adopt
        }
        resultState = state
        resultDialog = AlertDialog.Builder(context)
            .setTitle(R.string.md_bench_result_title)
            .setView(scroll(column))
            .setPositiveButton(adoptLabel) { _, _ -> controller.adoptBenchResult() }
            .setNegativeButton(R.string.md_bench_discard) { _, _ -> controller.dismissBench() }
            .setOnCancelListener { controller.dismissBench() }
            .create()
            .also { it.show() }
    }

    private fun showFailed(state: MgController.BenchState.Failed) {
        dismissResult()
        resultState = state
        resultDialog = AlertDialog.Builder(context)
            .setTitle(R.string.md_bench_result_title)
            .setMessage(state.message)
            .setPositiveButton(R.string.ok) { _, _ -> controller.dismissBench() }
            .setOnCancelListener { controller.dismissBench() }
            .create()
            .also { it.show() }
    }

    private fun rankedRow(position: Int, label: String, relativeCost: Double?): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(4) }
        }
        row.addView(
            text("$position.", color = onSurfaceVariant()).apply {
                setPadding(0, 0, dp(8), 0)
            },
        )
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        body.addView(text(label))
        if (relativeCost == null) {
            body.addView(text(context.getString(R.string.md_bench_unmeasured), size = SMALL_TEXT, color = onSurfaceVariant()))
        }
        row.addView(body)
        if (relativeCost != null) {
            row.addView(
                text(
                    context.getString(R.string.md_bench_cost, String.format(Locale.US, "%.2f", relativeCost)),
                    color = onSurfaceVariant(),
                ),
            )
        }
        return row
    }

    private fun qualityNotes(quality: MultidrawBenchQuality?): List<View> {
        if (quality == null || quality.rounds <= 0) return emptyList()
        val noise = String.format(Locale.US, "%.1f", quality.noise * 100)
        val notes = mutableListOf<View>(
            text(
                context.getString(R.string.md_bench_quality, quality.rounds, noise),
                size = SMALL_TEXT,
                color = onSurfaceVariant(),
                topMargin = 4,
            ),
        )
        if (quality.noisy) {
            notes += text(
                context.getString(R.string.md_bench_noisy, quality.attempts, noise),
                size = SMALL_TEXT,
                color = ThemeColors.error(context),
                topMargin = 4,
            )
        }
        return notes
    }

    // ---- ANGLE source picker ----

    private fun renderAngle(prompt: MgController.AngleSourcePrompt?) {
        if (prompt == null) {
            angleDialog?.dismiss()
            angleDialog = null
            anglePrompt = null
            return
        }
        if (anglePrompt === prompt) return
        angleDialog?.dismiss()
        anglePrompt = prompt

        val column = column()
        column.addView(text(context.getString(R.string.md_angle_intro)))
        if (prompt.sources.isEmpty()) {
            column.addView(text(context.getString(R.string.md_angle_none), topMargin = 12))
        } else {
            column.addView(
                text(
                    HtmlCompat.fromHtml(context.getString(R.string.md_angle_trust), HtmlCompat.FROM_HTML_MODE_LEGACY),
                    color = ThemeColors.error(context),
                    topMargin = 12,
                ),
            )
            prompt.sources.forEach { source ->
                column.addView(angleSourceRow(source, prompt.lastChosen))
            }
        }
        angleDialog = AlertDialog.Builder(context)
            .setTitle(R.string.md_angle_title)
            .setView(scroll(column))
            .setPositiveButton(R.string.md_angle_skip) { _, _ -> controller.continueWithoutAngle() }
            .setNegativeButton(R.string.dialog_negative) { _, _ -> controller.dismissAngleSourcePrompt() }
            .setOnCancelListener { controller.dismissAngleSourcePrompt() }
            .create()
            .also { it.show() }
    }

    private fun angleSourceRow(source: net.kdt.pojavlaunch.mobileglues.settings.AngleSource, lastChosen: String?): View {
        val title = if (source.packageName == lastChosen) {
            "${source.label} · ${context.getString(R.string.md_angle_last_used)}"
        } else {
            source.label
        }
        val subtitle = source.packageName + (source.versionName?.let {
            " · " + context.getString(R.string.md_angle_source_version, it)
        } ?: "")
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val outValue = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
                setBackgroundResource(outValue.resourceId)
            }
            isClickable = true
            isFocusable = true
            addView(text(title))
            addView(text(subtitle, size = SMALL_TEXT, color = onSurfaceVariant()))
            setOnClickListener { controller.confirmAngleSource(source) }
        }
    }

    // ---- view helpers ----

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun onSurfaceVariant(): Int =
        ThemeColors.onSurfaceVariant(context)

    private fun column(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(8))
    }

    private fun scroll(content: View): ScrollView = ScrollView(context).apply { addView(content) }

    private fun space(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp))
    }

    private fun text(
        value: CharSequence,
        type: TextView.BufferType = TextView.BufferType.SPANNABLE,
        size: Float = BODY_TEXT,
        color: Int = ThemeColors.onSurface(context),
        topMargin: Int = 0,
        bold: Boolean = false,
    ): TextView = TextView(context).apply {
        setText(value, type)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { this.topMargin = dp(topMargin) }
    }

    private companion object {
        const val PROGRESS_STEPS = 1000
        const val BODY_TEXT = 15f
        const val SMALL_TEXT = 12f
    }
}
