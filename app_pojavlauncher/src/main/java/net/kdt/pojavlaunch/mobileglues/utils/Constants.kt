package net.kdt.pojavlaunch.mobileglues.utils
import net.kdt.pojavlaunch.R

import net.kdt.pojavlaunch.Tools

object Constants {
    const val CONFIG_FILE_NAME: String = "config.json"

    val MG_DIRECTORY: String = "${Tools.DIR_DATA}/MobileGlues"

    val CONFIG_FILE_PATH: String = "$MG_DIRECTORY/$CONFIG_FILE_NAME"

    val GLSL_CACHE_FILE_PATH: String = "$MG_DIRECTORY/glsl_cache.tmp"
}
