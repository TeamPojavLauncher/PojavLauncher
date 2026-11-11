package net.kdt.pojavlaunch.mobileglues.settings

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Access to the `MobileGlues` directory inside the launcher's private data folder.
 *
 * Every function may do IO, callers run them off the main thread.
 */
interface MgStorage {

    /** Absolute path of the directory, shown to the user. */
    val displayPath: String

    fun isAccessible(): Boolean

    fun configExists(): Boolean

    @Throws(IOException::class)
    fun readConfig(): String

    /** Writes the whole config through a temporary file and a rename, so a reader never sees half a file. */
    @Throws(IOException::class)
    fun writeConfig(text: String)

    /** Keeps the broken config next to the original. Returns the backup name, or null if the copy failed. */
    fun writeCorruptBackup(text: String): String?

    /** Size of the GLSL cache file in bytes, null if there is none. */
    fun glslCacheBytes(): Long?

    @Throws(IOException::class)
    fun deleteGlslCache()

    /** Deletes the files MobileGlues itself creates in the directory and leaves everything else alone. */
    @Throws(IOException::class)
    fun deleteAll()
}

class DirectMgStorage(private val root: File) : MgStorage {

    private val configFile = File(root, CONFIG_FILE_NAME)
    private val glslCacheFile = File(root, GLSL_CACHE_FILE_NAME)

    override val displayPath: String get() = root.absolutePath

    override fun isAccessible(): Boolean = root.isDirectory || root.exists().not()

    override fun configExists(): Boolean = configFile.isFile

    override fun readConfig(): String = configFile.readText()

    override fun writeConfig(text: String) = configFile.writeAtomically(text)

    override fun writeCorruptBackup(text: String): String? = runCatching {
        val backup = File(root, CONFIG_FILE_NAME + CORRUPT_BACKUP_SUFFIX)
        backup.writeText(text)
        backup.name
    }.getOrNull()

    override fun glslCacheBytes(): Long? = glslCacheFile.takeIf { it.isFile }?.length()

    override fun deleteGlslCache() {
        if (glslCacheFile.exists() && !glslCacheFile.delete()) {
            throw IOException("Could not delete ${glslCacheFile.path}")
        }
    }

    override fun deleteAll() {
        if (!root.isDirectory) return
        for (name in KNOWN_MG_FILE_NAMES) {
            val file = File(root, name)
            if (file.exists() && !file.delete()) {
                throw IOException("Could not delete ${file.path}")
            }
        }
        if (root.list()?.isEmpty() == true) {
            root.delete()
        }
    }
}

internal const val CONFIG_FILE_NAME = "config.json"
internal const val GLSL_CACHE_FILE_NAME = "glsl_cache.tmp"
internal const val STATS_FILE_NAME = "stats.json"
internal const val LOG_FILE_NAME = "latest.log"
internal const val GL_CALLS_FILE_NAME = "glcalls.txt"
internal const val CORRUPT_BACKUP_SUFFIX = ".corrupt"
private const val CONFIG_TEMP_FILE_NAME = CONFIG_FILE_NAME + ".tmp"

/** File names MobileGlues creates in its directory. Anything else in there belongs to the user. */
internal val KNOWN_MG_FILE_NAMES = listOf(
    CONFIG_FILE_NAME,
    CONFIG_TEMP_FILE_NAME,
    CONFIG_FILE_NAME + CORRUPT_BACKUP_SUFFIX,
    GLSL_CACHE_FILE_NAME,
    STATS_FILE_NAME,
    LOG_FILE_NAME,
    GL_CALLS_FILE_NAME,
)

private fun File.writeAtomically(text: String) {
    parentFile?.mkdirs()
    val temporary = File(parentFile, "$name.tmp")
    FileOutputStream(temporary).use { output ->
        output.write(text.toByteArray())
        output.flush()
        output.fd.sync()
    }
    if (!temporary.renameTo(this)) {
        temporary.delete()
        writeText(text)
    }
}
