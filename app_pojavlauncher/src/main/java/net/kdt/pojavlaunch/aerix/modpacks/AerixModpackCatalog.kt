/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The catalogue of Team Aerix modpacks offered by the "Our modpacks" rail entry.
 *
 * The archives themselves stay on GitHub Releases so they never enter the APK: the catalogue is a
 * small JSON file attached to a dedicated `modpacks` release, and it is re-read on demand, so a new
 * pack ships by uploading a file and editing that JSON - never by releasing a new launcher.
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.social.SocialRealtime
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * One downloadable build of a pack.
 *
 * A pack page offers these as a choice, because the same pack is often built for more than one
 * Minecraft version and installing the wrong one is the whole failure mode worth designing out.
 *
 * [file] is resolved against [AerixModpackCatalog.PACK_BASE_URLS], so moving the whole catalogue
 * to another host is a one-line change here instead of an edit per pack.
 */
data class AerixModpackVersion(
    /** "1.20.1", shown next to the loader on the version picker. */
    val mcVersion: String,
    /** "fabric", shown beside the version. Defaults to the pack's loader when the entry omits it. */
    val loader: String = "",
    /** Archive name, relative to the catalogue. */
    val file: String,
    val sizeBytes: Long = 0L,
    /** Optional per-version note, e.g. "Recommended" or "Needs 6 GB of RAM". */
    val note: String = ""
) {
    val downloadUrls: List<String>
        get() = if (file.startsWith("http")) listOf(file)
        else AerixModpackCatalog.PACK_BASE_URLS.map { it + file }
}

/**
 * One pack as the catalogue describes it.
 *
 * A pack without an explicit [versions] array still works: it simply has the one build that its
 * top-level [file]/[mcVersion]/[size] describe, which keeps a one-build pack a one-line entry.
 */
data class AerixModpack(
    val id: String,
    val name: String,
    val summary: String,
    /** The default build, also the only one when [versions] is not given. */
    val file: String,
    /** Filled in from the catalogue when it says so; otherwise read out of the archive itself. */
    val mcVersion: String = "",
    val loader: String = "",
    val iconUrl: String = "",
    val sizeBytes: Long = 0L,
    /** Every build of this pack, in the order the picker should offer them. */
    val versions: List<AerixModpackVersion> = emptyList()
) {
    /** What the version picker lists; always at least one entry. */
    val offeredVersions: List<AerixModpackVersion>
        get() = versions.ifEmpty {
            listOf(
                AerixModpackVersion(
                    mcVersion = mcVersion,
                    loader = loader,
                    file = file,
                    sizeBytes = sizeBytes
                )
            )
        }

    /** Every place the default build can be fetched from, in the order they should be tried. */
    val downloadUrls: List<String>
        get() = offeredVersions.first().downloadUrls

    /**
     * Where this pack's artwork can be fetched from, tried in order; empty when it names none.
     *
     * Like [file], a relative name ("icons/x.png") resolves against the same two mirrors, so the
     * artwork can be committed next to the catalogue instead of needing a third-party host.
     */
    val iconUrls: List<String>
        get() = when {
            iconUrl.isBlank() -> emptyList()
            iconUrl.startsWith("http") -> listOf(iconUrl)
            else -> AerixModpackCatalog.PACK_BASE_URLS.map { it + iconUrl }
        }
}

object AerixModpackCatalog {

    /**
     * Where the catalogue is read from, tried in order until one answers.
     *
     * Both are mirrors of `modpacks/` in the repository, and both keep the archives out of the APK -
     * which is the point: a modpack is downloaded on demand, never built into the launcher.
     *
     * Releases come first because that is the cheap route (a CDN, no clone). The raw mirror exists
     * because release assets can only be uploaded by someone with the upload permission, whereas a
     * committed file needs nothing but a push; when no release asset is present the raw copy still
     * serves the pack.
     *
     * Neither is `releases/latest`: that slot belongs to the launcher's own APK release, and both
     * the website's download buttons and the in-app updater point at it.
     */
    private val CATALOG_URLS = listOf(
        "https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/modpacks/modpacks.json",
        "https://raw.githubusercontent.com/entitybrian69-bit/Aerix-launcher/v3_openjdk/modpacks/modpacks.json"
    )

    /**
     * Where a pack file is fetched from: the release that holds the catalogue, then the raw mirror.
     * Relative names in the catalogue ("packs/x.zip") resolve against both.
     */
    val PACK_BASE_URLS: List<String> = listOf(
        "https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/modpacks/",
        "https://raw.githubusercontent.com/entitybrian69-bit/Aerix-launcher/v3_openjdk/modpacks/"
    )

    private const val USER_AGENT = "entitybrian69-bit/Aerix-launcher/modpacks"

    /**
     * What the tab shows while the catalogue is being read, and after a failed read.
     *
     * The packs used to be shipped in the APK, which kept the list available offline but meant every
     * new pack needed a release. The trade is deliberate: the list now needs one network read, and
     * when that read fails the tab says so and offers to retry rather than showing an empty page.
     */
    var packs by mutableStateOf<List<AerixModpack>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    /** Last read failure, or null when the last read succeeded. */
    var error by mutableStateOf<String?>(null)
        private set
    private var loadedOnce = false

    /** Reads the catalogue unless it is already in hand; [force] re-reads after an error. */
    suspend fun load(force: Boolean = false) {
        if (isLoading) return
        if (loadedOnce && !force && error == null) return
        isLoading = true
        error = null
        val result = runCatching { fetch() }
        isLoading = false
        result.onSuccess { list ->
            packs = list
            loadedOnce = true
            error = if (list.isEmpty()) "The modpack catalogue is empty." else null
        }.onFailure { throwable ->
            error = throwable.message ?: "Could not reach the modpack catalogue."
        }
    }

    private suspend fun fetch(): List<AerixModpack> = withContext(Dispatchers.IO) {
        var lastFailure: String? = null
        for (url in CATALOG_URLS) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            // GitHub answers a missing file with an HTML error page, so a status is checked before
            // parsing instead of surfacing as "unexpected character <" from the JSON parser.
            val attempt = runCatching {
                SocialRealtime.restClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val packs = parse(body)
                    if (packs.isEmpty()) throw IllegalStateException("empty catalogue")
                    packs
                }
            }
            val packs = attempt.getOrNull()
            if (packs != null) return@withContext packs
            lastFailure = attempt.exceptionOrNull()?.message
        }
        throw IllegalStateException(
            "Could not read the modpack catalogue" +
                (lastFailure?.let { " ($it)" } ?: "") + ". Check your connection and retry."
        )
    }

    /**
     * Reads either a bare array or an object with a "packs" array, so the file can grow a version
     * field later without breaking launchers that already shipped.
     */
    internal fun parse(body: String): List<AerixModpack> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            JSONObject(trimmed).optJSONArray("packs") ?: JSONArray()
        }
        val out = ArrayList<AerixModpack>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val file = item.optString("file").trim()
            if (file.isEmpty()) continue
            val loader = item.optString("loader")
            out += AerixModpack(
                id = item.optString("id").ifBlank { file },
                name = item.optString("name").ifBlank { file.substringAfterLast('/') },
                summary = item.optString("summary"),
                file = file,
                mcVersion = item.optString("mcVersion"),
                loader = loader,
                iconUrl = item.optString("icon"),
                sizeBytes = item.optLong("size", 0L),
                versions = parseVersions(item.optJSONArray("versions"), loader)
            )
        }
        return out
    }

    /**
     * Reads a pack's `versions` array. Entries without a `file` are skipped rather than guessed at,
     * and an entry that omits `loader` inherits the pack's, since a pack is normally one loader
     * across all its builds.
     */
    private fun parseVersions(array: JSONArray?, packLoader: String): List<AerixModpackVersion> {
        if (array == null || array.length() == 0) return emptyList()
        val out = ArrayList<AerixModpackVersion>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val file = item.optString("file").trim()
            if (file.isEmpty()) continue
            out += AerixModpackVersion(
                mcVersion = item.optString("mcVersion").trim(),
                loader = item.optString("loader").ifBlank { packLoader },
                file = file,
                sizeBytes = item.optLong("size", 0L),
                note = item.optString("note").trim()
            )
        }
        return out
    }
}
