/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Installs a Team Aerix modpack from a downloaded archive with one press.
 *
 * Two archive layouts are accepted:
 *   * a MojoLauncher instance export - a folder holding mojo_instance.json, mods/, config/ and
 *     icon.webp, which is how Team Aerix packs are published. It names its own game version and
 *     loader, so nothing has to be guessed.
 *   * a Modrinth (.mrpack) or CurseForge pack, which the launcher already installed before this.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.modpacks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceRefreshEvents
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * What an exported instance folder says about itself. The keys match the launcher's own instance
 * metadata, because the export is written by the same code.
 */
internal data class MojoInstanceManifest(
    val name: String,
    /** "fabric-loader-0.14.25-1.20.1", the same shape our own instances use. */
    val versionId: String,
    val iconUrl: String
) {
    val mcVersion: String
        get() = versionId.substringAfterLast('-', "")
    val loaderVersion: String
        get() = versionId.removePrefix("fabric-loader-").substringBefore('-')
}

object AerixModpackInstaller {

    /**
     * Where a downloaded pack waits until it has been installed and can be deleted.
     *
     * The cache name carries the archive name as well as the pack id: a pack with two builds would
     * otherwise download one version and install the other from the file the first one left behind.
     */
    private fun downloadTarget(context: android.content.Context, pack: AerixModpack, version: AerixModpackVersion): File {
        val packPart = pack.id.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val versionPart = version.file.substringAfterLast('/').replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(context.cacheDir, "aerix_modpack_${packPart}_$versionPart")
    }

    var isInstalling by mutableStateOf(false)
        private set
    var installingName by mutableStateOf<String?>(null)
        private set
    /** The build being installed, so a card can name the version in its progress line. */
    var installingVersion by mutableStateOf<AerixModpackVersion?>(null)
        private set
    var progressFraction by mutableFloatStateOf(0f)
        private set
    var statusHeadline by mutableStateOf("")
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    /** The instance a pack produced, so the tab can offer "Play" straight afterwards. */
    var installedInstance by mutableStateOf<Instance?>(null)
        private set

    fun clearError() { lastError = null }

    /**
     * Downloads the chosen build of [pack] and turns it into a playable instance.
     *
     * Everything after the download runs off the UI thread: it touches the network twice (Fabric
     * meta) and writes a whole game directory, which must never happen on the main thread.
     */
    suspend fun install(
        context: android.content.Context,
        pack: AerixModpack,
        version: AerixModpackVersion = pack.offeredVersions.first()
    ) {
        if (isInstalling) return
        isInstalling = true
        installingName = pack.name
        installingVersion = version
        lastError = null
        installedInstance = null
        progressFraction = 0f
        statusHeadline = "Starting…"
        try {
            val archive = download(context, pack, version)
            installArchive(context, pack, version, archive)
        } catch (throwable: Throwable) {
            lastError = throwable.message ?: "The modpack could not be installed."
        } finally {
            isInstalling = false
            installingName = null
            installingVersion = null
            statusHeadline = ""
        }
    }

    private suspend fun download(
        context: android.content.Context,
        pack: AerixModpack,
        version: AerixModpackVersion
    ): File =
        withContext(Dispatchers.IO) {
            val target = downloadTarget(context, pack, version)
            var lastFailure: String? = null
            // The pack is mirrored (release asset, then the repository copy), so a missing asset
            // falls through to the next source instead of failing the press outright.
            for (url in version.downloadUrls) {
                target.delete()
                statusHeadline = "Downloading ${pack.name}…"
                val attempt = runCatching {
                    DownloadUtils.downloadFileMonitored(url, target, null) { current, total ->
                        if (total > 0) {
                            progressFraction = (current.toFloat() / total.toFloat()).coerceIn(0f, 0.7f)
                            statusHeadline = "Downloading ${pack.name}… ${current * 100 / total}%"
                        }
                    }
                }
                if (attempt.isSuccess && target.isFile && target.length() > 0L) return@withContext target
                lastFailure = attempt.exceptionOrNull()?.message
            }
            target.delete()
            throw IOException(
                "The download failed" + (lastFailure?.let { " ($it)" } ?: "") +
                    ". Check your connection and retry."
            )
        }

    private suspend fun installArchive(
        context: android.content.Context,
        pack: AerixModpack,
        version: AerixModpackVersion,
        archive: File
    ) {
        val kind = withContext(Dispatchers.IO) { detectLayout(archive) }
        when (kind) {
            Layout.MOJO_INSTANCE -> installMojoInstance(context, pack, version, archive)
            Layout.MODRINTH, Layout.CURSEFORGE -> installModpackArchive(context, pack, archive)
            Layout.UNKNOWN -> {
                archive.delete()
                throw IOException(
                    "This file is not a modpack Aerix can read. It should be a Team Aerix pack " +
                        "(.zip exported from an instance) or a .mrpack."
                )
            }
        }
    }

    /**
     * The classic path: hand the archive to the existing modpack installer, which understands
     * Modrinth and CurseForge layouts.
     */
    private suspend fun installModpackArchive(context: android.content.Context, pack: AerixModpack, archive: File) =
        withContext(Dispatchers.IO) {
            statusHeadline = "Installing ${pack.name}…"
            progressFraction = 0.75f
            // The key is a generated string resource, the same one the search screen hands over.
            val api = CommonApi(context.getString(net.kdt.pojavlaunch.R.string.curseforge_api_key))
            val instance = api.installLocalModpack(pack.id, archive, pack.iconUrl)
            if (instance == null) {
                archive.delete()
                throw IOException("The pack could not be installed on this device.")
            }
            progressFraction = 1f
            installedInstance = Instances.loadSelectedInstance()
            refresh()
        }

    /**
     * A MojoLauncher instance export.
     *
     * The whole folder is copied to the instance root, minus the metadata file: the export keeps
     * [mojo_instance.json] at the top level, and leaving it there would make the copy look like a
     * nested export if the folder is ever exported again.
     */
    private suspend fun installMojoInstance(
        context: android.content.Context,
        pack: AerixModpack,
        version: AerixModpackVersion,
        archive: File
    ) =
        withContext(Dispatchers.IO) {
            val manifest = readManifest(archive)
                ?: throw IOException("This pack is missing its instance metadata.")
            if (manifest.mcVersion.isBlank()) {
                throw IOException("This pack does not name its Minecraft version.")
            }
            // The export is authoritative, but a pack page offering 1.20.1 while its archive
            // says something else is exactly the mistake worth reporting rather than installing.
            if (version.mcVersion.isNotBlank() && version.mcVersion != manifest.mcVersion) {
                throw IOException(
                    "This archive is for Minecraft ${manifest.mcVersion}, but the version you " +
                        "picked is ${version.mcVersion}. Try the other version."
                )
            }

            statusHeadline = "Preparing Fabric ${manifest.loaderVersion}…"
            progressFraction = 0.72f
            val loaders = FabriclikeUtils.FABRIC_UTILS.downloadLoaderVersions(manifest.mcVersion)
                ?: throw IOException("Could not reach the Fabric server. Check your connection.")
            if (loaders.isEmpty()) throw IOException("Fabric has no loader for ${manifest.mcVersion}.")
            // The version the pack was built against is what its mods expect; only fall back to a
            // stable build when the export names one Fabric no longer offers.
            val loader = loaders.firstOrNull { it.version == manifest.loaderVersion }
                ?: loaders.firstOrNull { it.stable }
                ?: loaders.first()
            val fabricVersionId = FabriclikeUtils.FABRIC_UTILS.install(manifest.mcVersion, loader.version)
                ?: throw IOException("Could not download the Fabric profile for ${manifest.mcVersion}.")

            statusHeadline = "Creating ${pack.name}…"
            progressFraction = 0.84f
            // The catalogue's title wins over the name inside the export: the card the user pressed
            // said "Aetherium Optimized", so the instance that appears in the library must not turn
            // up under the pack's internal project name instead.
            val instanceName = pack.name.ifBlank { manifest.name }
            val prefix = "aerix_modpack_" + instanceName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_").trim('_').take(24)
            val instance = Instances.createInstance(
                { inst ->
                    inst.name = instanceName
                    inst.versionId = fabricVersionId
                    inst.icon = "fabric"
                    // A modpack is its own game directory. Sharing the default folder is what makes
                    // one pack's mods load into another instance, so this stays false.
                    inst.sharedData = false
                    inst.modpackIconUrl = manifest.iconUrl.ifBlank { pack.iconUrl }
                    inst.modpackProjectId = null
                    inst.modpackSource = "aerix"
                    // Which catalogue pack this is, so the bundled mark can be restored if the
                    // instance's icon file is ever lost.
                    inst.aerixPackId = pack.id
                },
                prefix.ifBlank { "aerix_modpack" }
            )

            statusHeadline = "Adding mods and configs…"
            progressFraction = 0.9f
            extractInstance(archive, instance.gameDirectory)

            // The instance wears the pack's mark from the moment it exists, not after a download:
            // the bundled copy cannot fail, so a fresh install never shows the Fabric logo even for
            // the second it takes to resolve anything else. A pack this build ships no mark for
            // falls back to the artwork the catalogue publishes.
            if (!AerixModpackIcons.writeOnto(context, instance, pack.id)) {
                writePackIcon(instance, pack.iconUrls)
            }

            // The export's own artwork, when this build ships no bundled mark for the pack. It is
            // re-encoded onto the icon file the instance expects, so the pack shows its own logo
            // rather than a stock loader mark. The name says WebP but exporters have written PNG
            // bytes under it for years, so it is decoded by content, not by extension.
            //
            // It runs only in that fallback case: this re-encode used to run unconditionally, after
            // the bundled mark had been written, which replaced the right logo with whatever the
            // archive happened to contain.
            val exportedIcon = File(instance.gameDirectory, "icon.webp")
            if (exportedIcon.isFile && AerixModpackIcons.drawableFor(pack.id) == 0) {
                writePackIcon(instance, exportedIcon)
            }

            progressFraction = 1f
            Instances.setSelectedInstance(instance)
            installedInstance = instance
            archive.delete()
            refresh()
        }

    /**
     * Puts the pack's downloaded artwork where the launcher looks for an instance icon.
     *
     * Best effort, and only ever an improvement: it overwrites the bundled mark, so a catalogue
     * that publishes real artwork still wins once that artwork is in hand.
     */
    private fun writePackIcon(instance: Instance, source: File) {
        if (!source.isFile) return
        runCatching {
            val bitmap = android.graphics.BitmapFactory.decodeFile(source.absolutePath) ?: return
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
                .writeInstanceIcon(instance, out.toByteArray())
            net.kdt.pojavlaunch.instances.InstanceIconProvider.dropIcon(instance)
        }
        // The WebP copy has done its job either way; the instance keeps only what it can read.
        source.delete()
    }

    /**
     * Downloads the first of the pack's artwork mirrors that answers and writes it as the icon.
     *
     * The whole point of running this after the bundled mark is that the artwork can be updated
     * without shipping a launcher: the catalogue's file is authoritative, the app's copy is the
     * guarantee that something decent is there first.
     */
    private fun writePackIcon(instance: Instance, urls: List<String>) {
        if (urls.isEmpty()) return
        runCatching {
            val bytes = java.io.ByteArrayOutputStream()
                .also { out -> DownloadUtils.download(urls.first(), out) }
                .toByteArray()
            if (bytes.isEmpty()) return
            net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
                .writeInstanceIcon(instance, bytes)
            net.kdt.pojavlaunch.instances.InstanceIconProvider.dropIcon(instance)
        }
    }

    /** Copies every file in the archive onto [destination], skipping the export's metadata file. */
    private fun extractInstance(archive: File, destination: File) {
        val root = destination.apply { mkdirs() }
        ZipFile(archive).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.substringAfter('/', "")
                if (name.isEmpty()) continue
                if (name.equals("mojo_instance.json", ignoreCase = true)) continue
                val target = File(root, name)
                // Zip-slip guard: an entry may not write outside the instance folder.
                if (!target.canonicalPath.startsWith(root.canonicalPath + File.separator)) continue
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun readManifest(archive: File): MojoInstanceManifest? = runCatching {
        ZipFile(archive).use { zip ->
            val entry = zip.entries().asSequence()
                .firstOrNull { it.name.endsWith("mojo_instance.json", ignoreCase = true) }
                ?: return null
            val json = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
            val objectJson = JSONObject(json)
            val versionId = objectJson.optString("versionId")
            MojoInstanceManifest(
                name = objectJson.optString("name").ifBlank { "Team Aerix pack" },
                versionId = versionId,
                iconUrl = objectJson.optString("modpackIconUrl")
            )
        }
    }.getOrNull()

    private enum class Layout { MOJO_INSTANCE, MODRINTH, CURSEFORGE, UNKNOWN }

    /**
     * Which installer this archive belongs to. Mirrors CommonApi.checkModpack, which is what the
     * existing modpack path uses, and adds the instance-export layout on top.
     */
    private fun detectLayout(archive: File): Layout = runCatching {
        ZipFile(archive).use { zip ->
            var hasManifest = false
            var hasMojo = false
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (name == "modrinth.index.json" || name.endsWith("/modrinth.index.json")) {
                    return@use Layout.MODRINTH
                }
                if (name == "manifest.json") hasManifest = true
                if (name.endsWith("mojo_instance.json")) hasMojo = true
            }
            when {
                hasManifest -> Layout.CURSEFORGE
                hasMojo -> Layout.MOJO_INSTANCE
                else -> Layout.UNKNOWN
            }
        }
    }.getOrDefault(Layout.UNKNOWN)

    private fun refresh() {
        runCatching { Tools.runOnUiThread { InstanceRefreshEvents.notifyChanged() } }
    }
}
