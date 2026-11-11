package net.kdt.pojavlaunch.mobileglues.utils

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import net.kdt.pojavlaunch.R

/** Colors of the launcher's dark theme, used by the MobileGlues dialogs built in code. */
object ThemeColors {
    @JvmStatic @ColorInt fun error(context: Context): Int = ContextCompat.getColor(context, R.color.mg_error)
    @JvmStatic @ColorInt fun primary(context: Context): Int = ContextCompat.getColor(context, R.color.minebutton_color)
    @JvmStatic @ColorInt fun onSurface(context: Context): Int = ContextCompat.getColor(context, R.color.primary_text)
    @JvmStatic @ColorInt fun onSurfaceVariant(context: Context): Int = ContextCompat.getColor(context, R.color.secondary_text)
}
