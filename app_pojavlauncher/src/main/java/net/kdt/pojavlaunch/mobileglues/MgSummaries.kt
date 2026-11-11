package net.kdt.pojavlaunch.mobileglues

import android.content.Context
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawSettings

object MgSummaries {

    /** One line describing the state of the MultiDraw order: default, custom, and how many functions differ. */
    @JvmStatic
    fun multidraw(context: Context, settings: MultidrawSettings): String {
        val exceptionCount = settings.exceptions.size
        if (!settings.globalCustomized && exceptionCount == 0) {
            return context.getString(R.string.md_summary_default)
        }
        val global = context.getString(
            if (settings.globalCustomized) R.string.md_summary_global_custom else R.string.md_summary_global_default,
        )
        return if (exceptionCount == 0) {
            global
        } else {
            "$global · ${context.getString(R.string.md_summary_exception_count, exceptionCount)}"
        }
    }
}
