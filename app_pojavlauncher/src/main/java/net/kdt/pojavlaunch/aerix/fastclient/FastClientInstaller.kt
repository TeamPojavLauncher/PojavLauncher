package net.kdt.pojavlaunch.aerix.fastclient

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.social.SocialRealtime
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.ComparableVersionString
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

/** FastClient jars were provided by its creator for distribution in Aerix. */
data class FastClientBuild(val minecraft: String, val minLoader: String, val java: Int, val sha256: String) {
    val asset get() = "components/fastclient/fastclient-hud-$minecraft.jar"
}

object FastClientInstaller {
    val builds = listOf(
        FastClientBuild("1.21.11", "0.18.4", 21, "4419bf1ca9dfdd8ed4f10c46a794bff3f72524b5d51a62a7223b12d03127f638"),
        FastClientBuild("26.1.2", "0.18.5", 25, "128502036f844b10d8d7023d07ac5aef06b5eae445cfb3782c7cc4995830d835"),
        FastClientBuild("26.2", "0.19.3", 25, "c8ab99cf7795c31f1bc010184783b73b3fac9defb1e95185108fe468ab8d647d")
    )
    private val installationLock = AtomicBoolean(false)
    var installingVersion by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var status by mutableStateOf("")
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    fun findInstalled(build: FastClientBuild, instances: List<Instance>): Instance? =
        instances.firstOrNull { it.fastClientVersion == build.minecraft }

    private fun loaderMeets(version: String, minimum: String): Boolean {
        val actual = ComparableVersionString.parse(version)
        val required = ComparableVersionString.parse(minimum)
        return actual.isValid && required.isValid && actual >= required
    }

    private fun copyVerified(input: InputStream, file: File, algorithm: String, expected: String) {
        val digest = MessageDigest.getInstance(algorithm)
        input.use { source -> file.outputStream().use { out ->
            val buffer = ByteArray(8192)
            var count = 0L
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                count += read
                if (count > 40_000_000) throw IOException("Mod file is too large")
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
            }
        } }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        if (!actual.equals(expected, ignoreCase = true)) throw IOException("Mod checksum mismatch")
    }

    private fun modId(file: File): String = ZipFile(file).use { zip ->
        val entry = zip.getEntry("fabric.mod.json") ?: throw IOException("Downloaded file is not a Fabric mod")
        if (entry.size !in 1L..262144L) throw IOException("Invalid Fabric mod metadata")
        JSONObject(zip.getInputStream(entry).bufferedReader().use { it.readText() }).optString("id")
    }

    private fun fabricApi(version: String, output: File) {
        val loader = URLEncoder.encode("[\"fabric\"]", "UTF-8")
        val game = URLEncoder.encode("[\"$version\"]", "UTF-8")
        val metadataUrl = "https://api.modrinth.com/v2/project/fabric-api/version?loaders=$loader&game_versions=$game"
        val request = Request.Builder().url(metadataUrl).header("User-Agent", "AerixLauncher/FastClient").build()
        val data = SocialRealtime.restClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Fabric API lookup failed (HTTP ${response.code})")
            val body = response.body ?: throw IOException("Fabric API response was empty")
            if (body.contentLength() > 2_000_000) throw IOException("Fabric API response is too large")
            body.string().also { if (it.length > 2_000_000) throw IOException("Fabric API response is too large") }
        }
        val versions = JSONArray(data)
        if (versions.length() == 0) throw IOException("Fabric API is not available for Minecraft $version")
        val files = versions.getJSONObject(0).getJSONArray("files")
        val item = (0 until files.length()).map { files.getJSONObject(it) }.firstOrNull { it.optBoolean("primary") }
            ?: throw IOException("Fabric API download is missing")
        val url = item.optString("url")
        val hash = item.optJSONObject("hashes")?.optString("sha512").orEmpty()
        if (!url.startsWith("https://cdn.modrinth.com/") || !hash.matches(Regex("[a-fA-F0-9]{128}"))) {
            throw IOException("Fabric API download metadata is invalid")
        }
        SocialRealtime.restClient.newCall(Request.Builder().url(url).header("User-Agent", "AerixLauncher/FastClient").build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Fabric API download failed (HTTP ${response.code})")
            val body = response.body ?: throw IOException("Fabric API download was empty")
            if (body.contentLength() > 40_000_000) throw IOException("Fabric API is too large")
            copyVerified(body.byteStream(), output, "SHA-512", hash)
        }
        if (modId(output) != "fabric-api") throw IOException("Downloaded Fabric API has an unexpected mod ID")
    }

    /** Pinned, version-specific upstream releases. Verify both hash and Fabric mod identity. */
    private fun downloadPerformanceMods(context: Context, version: String, staging: File): List<File> {
        val manifest = context.assets.open("components/fastclient/performance-mods.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val entries = manifest.getJSONArray(version)
        if (entries.length() != 2) throw IOException("Performance mod list is incomplete")
        val result = mutableListOf<File>()
        val ids = mutableSetOf<String>()
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val id = entry.getString("id")
            val url = entry.getString("url")
            val hash = entry.getString("sha256")
            if (id !in setOf("lithium", "entityculling") || !ids.add(id) ||
                !url.startsWith("https://github.com/") || !hash.matches(Regex("[a-f0-9]{64}"))) {
                throw IOException("Invalid performance mod configuration")
            }
            status = "Downloading ${if (id == "lithium") "Lithium" else "Entity Culling"}…"
            val output = File(staging, "$id.jar")
            SocialRealtime.restClient.newCall(Request.Builder().url(url).header("User-Agent", "AerixLauncher/FastClient").build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("$id download failed (HTTP ${response.code})")
                val body = response.body ?: throw IOException("$id download was empty")
                if (body.contentLength() > 40_000_000) throw IOException("$id is too large")
                copyVerified(body.byteStream(), output, "SHA-256", hash)
            }
            if (modId(output) != id) throw IOException("Unexpected mod ID in $id download")
            result.add(output)
        }
        return result
    }

    /** Creates a separate private instance. Never changes an existing instance or its mods. */
    suspend fun install(context: Context, build: FastClientBuild): Result<Instance> = withContext(Dispatchers.IO) {
        if (!installationLock.compareAndSet(false, true)) return@withContext Result.failure(IOException("Another FastClient installation is in progress"))
        installingVersion = build.minecraft
        lastError = null
        progress = 0.05f
        var staging: File? = null
        var created: Instance? = null
        try {
            require(build in builds) { "Unknown FastClient version" }
            check(findInstalled(build, Instances.loadAllInstances()) == null) { "FastClient ${build.minecraft} is already installed" }
            status = "Checking Fabric Loader…"
            val versions = FabriclikeUtils.FABRIC_UTILS.downloadLoaderVersions(build.minecraft)
                ?: throw IOException("Could not contact Fabric Meta")
            val compatible = versions.filter { loaderMeets(it.version, build.minLoader) }
            val loader = compatible.firstOrNull { it.stable } ?: compatible.firstOrNull()
                ?: throw IOException("No compatible Fabric Loader found (requires ${build.minLoader} or newer)")
            val profile = FabriclikeUtils.FABRIC_UTILS.install(build.minecraft, loader.version)
                ?: throw IOException("Could not install the Fabric profile")
            progress = 0.3f
            staging = File(context.cacheDir, "fastclient-" + java.util.UUID.randomUUID()).apply {
                if (!mkdirs()) throw IOException("Could not create installation staging folder")
            }
            status = "Checking FastClient files…"
            val client = File(staging, "client.jar")
            copyVerified(context.assets.open(build.asset), client, "SHA-256", build.sha256)
            if (modId(client) != "fasteclient") throw IOException("FastClient mod metadata is invalid")
            progress = 0.48f
            status = "Downloading Fabric API…"
            val api = File(staging, "fabric-api.jar")
            fabricApi(build.minecraft, api)
            progress = 0.62f
            val performanceMods = downloadPerformanceMods(context, build.minecraft, staging)
            progress = 0.82f
            // Do not adopt an existing, similarly named profile: users may have their own data there.
            check(findInstalled(build, Instances.loadAllInstances()) == null) { "FastClient ${build.minecraft} is already installed" }
            created = Instances.createInstance({ instance ->
                instance.name = "FastClient ${build.minecraft} (incomplete)"
                instance.versionId = profile
                instance.icon = "fastclient"
                instance.sharedData = false
                instance.isAerixClient = false
            }, "fastclient_${build.minecraft.replace('.', '_')}")
            status = "Creating FastClient instance…"
            val mods = File(created.gameDirectory, "mods")
            if (!mods.mkdirs() && !mods.isDirectory) throw IOException("Could not create mods folder")
            client.copyTo(File(mods, "fastclient-hud-${build.minecraft}.jar"), overwrite = false)
            api.copyTo(File(mods, "fabric-api.jar"), overwrite = false)
            performanceMods.forEach { it.copyTo(File(mods, it.name), overwrite = false) }
            created.name = "FastClient ${build.minecraft}"
            created.fastClientVersion = build.minecraft
            // A FastClient build is 1.21-era, so it starts on the renderer that era wants. The
            // profile it writes is named after Fabric, not after the game, so the version name
            // alone would not say which renderer this needs.
            created.renderer = net.kdt.pojavlaunch.game.renderer.RendererDefaults
                .launchTagForVersion(null, build.minecraft)
            created.write()
            Instances.setSelectedInstance(created)
            progress = 1f
            status = "FastClient ${build.minecraft} is ready"
            Result.success(created)
        } catch (error: Exception) {
            // A newly created profile may be visible to other UI; never delete it or user data.
            val detail = error.message ?: "Installation failed"
            lastError = detail + if (created != null) ". An incomplete instance remains in Library; you can remove it." else ""
            status = "Installation failed"
            Result.failure(IOException(lastError, error))
        } finally {
            staging?.deleteRecursively()
            installingVersion = null
            installationLock.set(false)
        }
    }
}
