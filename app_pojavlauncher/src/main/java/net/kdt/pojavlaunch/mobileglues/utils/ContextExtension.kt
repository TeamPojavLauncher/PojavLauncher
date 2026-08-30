package net.kdt.pojavlaunch.mobileglues.utils
import net.kdt.pojavlaunch.R

import android.content.Context
import android.widget.Toast

fun Context.toast(text: Any, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text.toString(), duration).show()
