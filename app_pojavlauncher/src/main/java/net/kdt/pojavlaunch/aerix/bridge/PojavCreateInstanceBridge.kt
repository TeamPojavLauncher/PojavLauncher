/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Adapter behind the Aerix "Create New Instance" page. Every install path calls the same Pojav backend
 * code the Pojav installer fragments call (FabriclikeInstallFragment, ForgelikeInstallFragment,
 * OptiFineInstallFragment, BTAInstallFragment, ProfileTypeSelectFragment vanilla button):
 *   Fabric / Quilt / Legacy Fabric  FabriclikeUtils.install + Instances.createInstance
 *   Forge / NeoForge                ForgelikeUtils.createInstaller + Instances.createInstance + InstanceInstaller.start
 *   OptiFine                        OptiFineDownloadTask.prepareForInstall + OptiFineUtils.createInstaller
 *   BTA                             BTADownloadTask
 *   Vanilla                         Instances.createInstance with the picked version id
 * Replaces the install flow of mirai-launcher-
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/download/game/DownloadGameWithAddonScreen.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/game/download/game/GameInstaller.kt
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

package net.kdt.pojavlaunch.aerix.bridge

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.game.renderer.RendererDefaults
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.BTADownloadTask
import net.kdt.pojavlaunch.modloaders.BTAUtils
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask
import net.kdt.pojavlaunch.modloaders.OptiFineUtils
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import com.kdt.mcgui.ProgressLayout
import java.io.File
import java.io.IOException

/** Loader tiles of the Create New Instance page, in the order of the Aerix design. */
enum class AerixLoader(
    val displayName: String,
    @DrawableRes val iconRes: Int,
    @StringRes val taglineRes: Int,
    /** Whether the loader needs a Minecraft version from the list on the left. */
    val usesGameVersion: Boolean,
    /**
     * Whether the loader publishes builds to choose from. Vanilla has none - there is nothing to
     * pick between - so a vanilla instance is complete as soon as a Minecraft version is selected.
     */
    val hasBuilds: Boolean
) {
    VANILLA("Vanilla", R.drawable.aerix_loader_vanilla, R.string.aerix_loader_vanilla_sub, true, false),
    OPTIFINE("OptiFine", R.drawable.aerix_loader_optifine, R.string.aerix_loader_optifine_sub, true, true),
    FABRIC("Fabric", R.drawable.ic_fabric, R.string.aerix_loader_fabric_sub, true, true),
    FORGE("Forge", R.drawable.ic_forge, R.string.aerix_loader_forge_sub, true, true),
    NEOFORGE("NeoForge", R.drawable.ic_neoforge, R.string.aerix_loader_neoforge_sub, true, true),
    QUILT("Quilt", R.drawable.ic_quilt, R.string.aerix_loader_quilt_sub, true, true),
    LEGACY_FABRIC("Legacy Fabric", R.drawable.aerix_loader_legacy_fabric, R.string.aerix_loader_legacy_fabric_sub, true, true),
    BTA("BTA", R.drawable.aerix_loader_bta, R.string.aerix_loader_bta_sub, false, true)
}

/** Version groups of Pojav's VersionListAdapter (installed, release, snapshot, old_beta, old_alpha). */
enum class AerixVersionType { RELEASE, SNAPSHOT, OLD_BETA, OLD_ALPHA, INSTALLED }

class AerixGameVersion(val id: String, val type: AerixVersionType)

/** One selectable loader build. [source] is the untouched Pojav object (OptiFineVersion, BTAVersion, version string). */
class AerixLoaderVersion(val id: String, val label: String, val stable: Boolean, val source: Any?)

object PojavCreateInstanceBridge {
    /** Bumped when Pojav publishes the Mojang version manifest (ExtraConstants.RELEASE_TABLE). */
    var versionTableRevision by mutableIntStateOf(0)
        private set
    var installing by mutableStateOf(false)
        private set

    private var attached = false
    private var optiFineCache: OptiFineUtils.OptiFineVersions? = null
    private var btaCache: BTAUtils.BTAVersionList? = null

    // Held strongly: ExtraCore keeps only weak references.
    private val releaseTableListener = ExtraListener<Any> { _, _ ->
        Tools.runOnUiThread { versionTableRevision++ }
        false
    }

    fun attach() {
        if (attached) return
        attached = true
        ExtraCore.addExtraListener(ExtraConstants.RELEASE_TABLE, releaseTableListener)
    }

    /** Same sources as Pojav's VersionListAdapter: the manifest in ExtraConstants.RELEASE_TABLE and .minecraft/versions. */
    fun gameVersions(): List<AerixGameVersion> {
        val result = ArrayList<AerixGameVersion>()
        val installed = File(Tools.DIR_GAME_NEW, "versions").list()
        if (installed != null) {
            installed.sorted().forEach { result.add(AerixGameVersion(it, AerixVersionType.INSTALLED)) }
        }
        val table = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JVersionList
        table?.versions?.forEach { version ->
            val id = version.id ?: return@forEach
            val type = when (version.type) {
                "release" -> AerixVersionType.RELEASE
                "snapshot" -> AerixVersionType.SNAPSHOT
                "old_beta" -> AerixVersionType.OLD_BETA
                "old_alpha" -> AerixVersionType.OLD_ALPHA
                else -> return@forEach
            }
            result.add(AerixGameVersion(id, type))
        }
        return result
    }

    /** Builds available for [loader] on [gameVersion], newest first. Empty when the loader has none for that version. */
    @WorkerThread
    @Throws(IOException::class)
    fun loadLoaderVersions(loader: AerixLoader, gameVersion: String?): List<AerixLoaderVersion> {
        return when (loader) {
            AerixLoader.VANILLA -> emptyList()
            AerixLoader.FABRIC -> fabriclike(FabriclikeUtils.FABRIC_UTILS, gameVersion)
            AerixLoader.QUILT -> fabriclike(FabriclikeUtils.QUILT_UTILS, gameVersion)
            AerixLoader.LEGACY_FABRIC -> fabriclike(FabriclikeUtils.LEGACY_FABRIC_UTILS, gameVersion)
            AerixLoader.FORGE -> forgelike(ForgelikeUtils.FORGE_UTILS, gameVersion)
            AerixLoader.NEOFORGE -> forgelike(ForgelikeUtils.NEOFORGE_UTILS, gameVersion)
            AerixLoader.OPTIFINE -> optifine(gameVersion)
            AerixLoader.BTA -> bta()
        }
    }

    private fun fabriclike(utils: FabriclikeUtils, gameVersion: String?): List<AerixLoaderVersion> {
        if (gameVersion == null) return emptyList()
        // The loader endpoint answers for any id; only offer it when the game version is supported.
        val games = utils.downloadGameVersions() ?: return emptyList()
        if (games.none { it.version == gameVersion }) return emptyList()
        val loaders = utils.downloadLoaderVersions(gameVersion) ?: return emptyList()
        return loaders.map { AerixLoaderVersion(it.version, it.version, it.stable, it.version) }
    }

    private fun forgelike(utils: ForgelikeUtils, gameVersion: String?): List<AerixLoaderVersion> {
        if (gameVersion == null) return emptyList()
        val all = utils.downloadVersions() ?: return emptyList()
        // Grouping and ordering identical to ForgelikeVersionListAdapter.
        val matching = all.filter { !utils.shouldSkipVersion(it) && utils.processVersionString(it) == gameVersion }
        val ordered = if (utils.isVersionOrderInversed) matching.reversed() else matching
        return ordered.map { full -> AerixLoaderVersion(full, full, true, full) }
    }

    private fun optifine(gameVersion: String?): List<AerixLoaderVersion> {
        if (gameVersion == null) return emptyList()
        val versions = optiFineCache ?: OptiFineUtils.downloadOptiFineVersions()?.also { optiFineCache = it } ?: return emptyList()
        val index = versions.gameVersions.indexOfFirst { it.removePrefix("Minecraft").trim() == gameVersion }
        if (index < 0) return emptyList()
        return versions.optifineVersions[index].map { version ->
            val label = version.versionName.removePrefix("OptiFine").trim()
            AerixLoaderVersion(version.versionName, label, !version.versionName.contains("pre", ignoreCase = true), version)
        }
    }

    private fun bta(): List<AerixLoaderVersion> {
        val list = btaCache ?: BTAUtils.downloadVersionList()?.also { btaCache = it } ?: return emptyList()
        val result = ArrayList<AerixLoaderVersion>()
        list.testedVersions.forEach { result.add(AerixLoaderVersion(it.versionName, it.versionName, true, it)) }
        list.untestedVersions.forEach { result.add(AerixLoaderVersion(it.versionName, it.versionName, false, it)) }
        list.nightlyVersions.forEach { result.add(AerixLoaderVersion(it.versionName, it.versionName, false, it)) }
        return result
    }

    /** Name proposed in the Instance Info field, e.g. "Fabric 1.21.1". */
    fun suggestedName(loader: AerixLoader, gameVersion: String?, loaderVersion: AerixLoaderVersion?): String = when (loader) {
        AerixLoader.BTA -> "BTA " + (loaderVersion?.label ?: "")
        AerixLoader.VANILLA -> gameVersion ?: ""
        else -> loader.displayName + " " + (gameVersion ?: "")
    }.trim()

    /**
     * Creates the instance. Runs on Pojav's executor; [onFinished] is posted to the UI thread with true on success.
     * The created instance becomes the selected one and receives [icon] when the user picked one.
     */
    fun install(
        context: Context,
        loader: AerixLoader,
        gameVersion: String?,
        loaderVersion: AerixLoaderVersion?,
        name: String,
        icon: Bitmap?,
        onFinished: (Boolean) -> Unit
    ) {
        if (installing) return
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }
        val appContext = context.applicationContext
        installing = true
        PojavApplication.sExecutorService.execute {
            val result = runCatching { performInstall(appContext, loader, gameVersion, loaderVersion, name.trim(), icon) }
            Tools.runOnUiThread {
                installing = false
                result.onSuccess { created ->
                    if (created) {
                        Toast.makeText(appContext, appContext.getString(R.string.aerix_create_done, name.trim()), Toast.LENGTH_SHORT).show()
                    }
                    PojavInstanceBridge.reload()
                    onFinished(created)
                }.onFailure {
                    Tools.showErrorRemote(it)
                    onFinished(false)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun performInstall(
        context: Context,
        loader: AerixLoader,
        gameVersion: String?,
        loaderVersion: AerixLoaderVersion?,
        name: String,
        icon: Bitmap?
    ): Boolean {
        val prefix = directoryPrefix(name)
        val instance: Instance? = when (loader) {
            AerixLoader.VANILLA -> {
                val version = gameVersion ?: return false
                Instances.createInstance({ i ->
                    i.name = name.ifEmpty { null }
                    i.versionId = version
                    i.icon = "vanilla"
                    i.sharedData = true
                }, prefix)
            }
            AerixLoader.FABRIC, AerixLoader.QUILT, AerixLoader.LEGACY_FABRIC -> {
                val utils = when (loader) {
                    AerixLoader.QUILT -> FabriclikeUtils.QUILT_UTILS
                    AerixLoader.LEGACY_FABRIC -> FabriclikeUtils.LEGACY_FABRIC_UTILS
                    else -> FabriclikeUtils.FABRIC_UTILS
                }
                val version = gameVersion ?: return false
                val build = loaderVersion?.id ?: return false
                val versionId = utils.install(version, build)
                    ?: throw IOException(context.getString(R.string.fabric_dl_cant_read_meta, utils.name))
                Instances.createInstance({ i ->
                    i.name = name.ifEmpty { utils.name }
                    i.icon = if (loader == AerixLoader.LEGACY_FABRIC) "legacy_fabric" else utils.iconName
                    i.versionId = versionId
                }, prefix.ifEmpty { versionId })
            }
            AerixLoader.FORGE, AerixLoader.NEOFORGE -> {
                val utils = if (loader == AerixLoader.FORGE) ForgelikeUtils.FORGE_UTILS else ForgelikeUtils.NEOFORGE_UTILS
                val full = loaderVersion?.id ?: return false
                ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
                try {
                    val installer = utils.createInstaller(full)
                    val created = Instances.createInstance({ i ->
                        i.name = name.ifEmpty { utils.name }
                        i.icon = utils.iconName
                        i.installer = installer
                    }, prefix.ifEmpty { full })
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                    installer.start()
                    created
                } finally {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                }
            }
            AerixLoader.OPTIFINE -> {
                val version = loaderVersion?.source as? OptiFineUtils.OptiFineVersion ?: return false
                ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
                try {
                    OptiFineDownloadTask(version).prepareForInstall()
                    val installer = OptiFineUtils.createInstaller(version)
                    val created = Instances.createInstance({ i ->
                        i.name = name.ifEmpty { "OptiFine" }
                        // Without this the instance fell back to the stock icon and showed the
                        // launcher's logo instead of OptiFine's.
                        i.icon = "optifine"
                        i.installer = installer
                        i.sharedData = true
                    }, prefix.ifEmpty { "OptiFine" })
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                    installer.start()
                    created
                } finally {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                }
            }
            AerixLoader.BTA -> {
                val version = loaderVersion?.source as? BTAUtils.BTAVersion ?: return false
                runBta(version)
                // BTADownloadTask names the instance itself; apply the name typed on the page afterwards.
                val versionId = "bta-" + version.versionName
                val created = Instances.loadAllInstances().lastOrNull { it.versionId == versionId }
                if (created != null && name.isNotEmpty() && name != created.name) {
                    created.name = name
                    created.write()
                }
                created
            }
        }
        if (instance != null) {
            if (icon != null) {
                instance.encodeNewIcon(icon)
                InstanceIconProvider.dropIcon(instance)
            }
            // A brand new instance starts on the renderer its version calls for: LTW at 1.17 and
            // above, Emesis below it. Written onto the instance itself rather than left to the
            // global choice, so the instance keeps its own answer when the global renderer is
            // changed later, and the editor shows which one it is on.
            applyVersionRenderer(instance)
            Instances.setSelectedInstance(instance)
        }
        return instance != null
    }

    /**
     * Pin the renderer the instance's version should start on, when the instance does not already
     * carry an explicit choice.
     *
     * Forge and OptiFine install in the background and name the version they produce only when the
     * installer finishes; in that case there is no version id yet, and the launch path applies the
     * same rule from the version it ends up launching.
     */
    private fun applyVersionRenderer(instance: Instance) {
        if (!instance.renderer.isNullOrEmpty()) return
        val versionId = instance.versionId
        if (versionId.isNullOrEmpty() || versionId == Instance.VERSION_LATEST_RELEASE ||
            versionId == Instance.VERSION_LATEST_SNAPSHOT
        ) return
        val renderer = runCatching { RendererDefaults.launchTagForVersion(null, versionId) }.getOrNull()
        if (renderer.isNullOrEmpty()) return
        instance.renderer = renderer
        runCatching { instance.write() }
    }

    private fun runBta(version: BTAUtils.BTAVersion) {
        var failure: Exception? = null
        val listener = object : ModloaderDownloadListener {
            override fun onDownloadFinished(downloadedFile: File?) {}
            override fun onDataNotAvailable() {
                failure = IOException("BTA metadata is not available")
            }
            override fun onDownloadError(e: Exception) {
                failure = e
            }
        }
        BTADownloadTask(listener, version).run()
        failure?.let { throw it }
    }

    /** Directory prefix derived from the typed name; Instances appends a UUID so only safe characters are kept. */
    private fun directoryPrefix(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(48)
}
