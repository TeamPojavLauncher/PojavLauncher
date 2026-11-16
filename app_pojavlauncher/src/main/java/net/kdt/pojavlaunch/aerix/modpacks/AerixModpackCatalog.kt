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
 * One pack as the catalogue describes it.
 *
 * [file] is resolved against [AerixModpackCatalog.baseUrl], so moving the whole catalogue to
 * another host is a one-line change here instead of an edit per pack.
 */
data class AerixModpack(
    val id: String,
    val name: String,
    val summary: String,
    val file: String,
    /** Filled in from the catalogue when it says so; otherwise read out of the archive itself. */
    val mcVersion: String = "",
    val loader: String = "",
    val iconUrl: String = "",
    val sizeBytes: Long = 0L
) {
    val downloadUrl: String
        get() = if (file.startsWith("http")) file else AerixModpackCatalog.baseUrl + file
}

object AerixModpackCatalog {

    /**
     * The release the catalogue lives in. A dedicated tag, not `releases/latest`: "latest" on this
     * repository is the launcher's own APK release, and the website's download buttons plus the
     * in-app updater both point at it. Publishing a modpack release as latest would break them.
     */
    private const val CATALOG_URL =
        "https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/modpacks/modpacks.json"

    /** Everything after the last slash of the catalogue URL: where relative pack files live. */
    val baseUrl: String = CATALOG_URL.substringBeforeLast('/') + "/"

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
        val request = Request.Builder()
            .url(CATALOG_URL)
            .header("User-Agent", USER_AGENT)
            .build()
        // GitHub answers a missing file with an HTML error page, so a 404 is called out instead of
        // surfacing as "unexpected character <" from the JSON parser.
        SocialRealtime.restClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    if (response.code == 404) "The modpack catalogue has not been published yet."
                    else "The modpack catalogue could not be read (HTTP ${response.code})."
                )
            }
            val body = response.body?.string().orEmpty()
            parse(body)
        }
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
            out += AerixModpack(
                id = item.optString("id").ifBlank { file },
                name = item.optString("name").ifBlank { file.substringAfterLast('/') },
                summary = item.optString("summary"),
                file = file,
                mcVersion = item.optString("mcVersion"),
                loader = item.optString("loader"),
                iconUrl = item.optString("icon"),
                sizeBytes = item.optLong("size", 0L)
            )
        }
        return out
    }
}
