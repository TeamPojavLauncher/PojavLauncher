/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The marks the launcher bundles for Team Aerix modpacks.
 *
 * A pack installed from "Our modpacks" must never appear in the library wearing the Fabric logo or
 * the launcher's own mark: the whole point of the tab is that these are curated packs with an
 * identity. Downloading the artwork is not enough on its own - the first draw of the instance list
 * happens before any download can finish - so each pack's mark also ships as vector art in res/,
 * keyed by the pack's catalogue id.
 *
 * Adding a pack therefore means adding two things with the same id: its entry in
 * modpacks/modpacks.json, and a drawable named `aerix_modpack_<id with dashes as underscores>`.
 * A pack without one still installs, and still falls back to the artwork the catalogue publishes.
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import java.io.ByteArrayOutputStream

object AerixModpackIcons {

    /**
     * The bundled mark for a catalogue pack id, or 0 when this build ships none.
     *
     * The lookup is a plain `when` rather than a reflect-into-R: resource ids are not stable enough
     * to build by name, and an explicit list keeps it obvious which packs have artwork in the app.
     * The match is on the id with everything but letters and digits removed, so
     * "aetherium-optimized", "aetherium_optimized" and "AetheriumOptimized" all resolve and a pack
     * keeps its mark if the catalogue entry is ever punctuated differently. The drawable is named
     * after the catalogue id (`aerix_modpack_` + id, dashes as underscores) - tests/modpacks_smoke.py
     * asserts that pairing, so a new pack cannot ship without its mark.
     */
    /** Icon files are drawn small; 512px covers any tile the launcher uses, at any density. */
    private const val ICON_EDGE_PX = 512

    /**
     * Draws the pack's bundled mark onto [instance] as its icon.
     *
     * Rendering the vector here rather than shipping a bitmap keeps one copy of the artwork in the
     * APK and lets it come out sharp at the size the icon file is asked for.
     *
     * @return whether the instance now carries the bundled mark
     */
    fun writeOnto(context: Context, instance: Instance, packId: String?): Boolean = runCatching {
        val iconRes = drawableFor(packId)
        if (iconRes == 0) return false
        val drawable = ResourcesCompat.getDrawable(context.resources, iconRes, null) ?: return false
        val bitmap = Bitmap.createBitmap(ICON_EDGE_PX, ICON_EDGE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, ICON_EDGE_PX, ICON_EDGE_PX)
        drawable.draw(canvas)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        val written = ModIconCache.writeInstanceIcon(instance, out.toByteArray())
        // The shown icon is cached per instance directory; it has to go, or the old one stays.
        if (written) InstanceIconProvider.dropIcon(instance)
        written
    }.getOrDefault(false)

    @DrawableRes
    fun drawableFor(packId: String?): Int {
        if (packId.isNullOrBlank()) return 0
        val key = packId.lowercase().filter { it.isLetterOrDigit() }
        return when {
            key.startsWith("aetherium") -> R.drawable.aerix_modpack_aetherium_optimized
            else -> 0
        }
    }
}
