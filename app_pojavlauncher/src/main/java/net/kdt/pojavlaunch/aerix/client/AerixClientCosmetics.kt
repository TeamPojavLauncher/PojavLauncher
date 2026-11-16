package net.kdt.pojavlaunch.aerix.client

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import net.kdt.pojavlaunch.aerix.social.SocialBackend
import net.kdt.pojavlaunch.instances.Instances
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class CapeAnimStyle {
    EMBERS,
    LASER_SCAN,
    ECLIPSE,
    FLAMES,
    LIGHTNING,
    VORTEX,
    AURORA,
    MATRIX,
    STARFIELD,
    FROST,
    PETALS,
    GOLD_SHEEN,
    SONIC_RINGS,
    ROYAL_CREST,
    CELESTIAL_WINGS
}

data class ClientCapeSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val topHex: Long,
    val bottomHex: Long,
    val emblemHex: Long,
    val animStyle: CapeAnimStyle,
    val requiredEmail: String? = null,
    val badgeText: String? = null
) {
    fun isUnlockedFor(email: String?): Boolean {
        val req = requiredEmail ?: return true
        return email?.trim()?.equals(req, ignoreCase = true) == true
    }
}

object AerixClientCosmetics {
    const val OWNER_EMAIL = "entitybrian69@gmail.com"
    const val CO_OWNER_EMAIL = "kushaladhikari982648@gmail.com"

    /** Exact 15 Animated Capes displayed in the Aerix Client Capes menu. */
    val capes: List<ClientCapeSpec> = listOf(
        ClientCapeSpec(
            id = "crimson_sovereign",
            title = "Crimson Sovereign",
            subtitle = "Pulsing crimson emblem + rising embers",
            topHex = 0xFFDC1E3AL,
            bottomHex = 0xFF1A0E12L,
            emblemHex = 0xFFFF4D6AL,
            animStyle = CapeAnimStyle.EMBERS,
            badgeText = "DEFAULT"
        ),
        ClientCapeSpec(
            id = "phantom_obsidian",
            title = "Phantom Obsidian",
            subtitle = "Dark gray carbon weave + crimson scan",
            topHex = 0xFF282B34L,
            bottomHex = 0xFF111216L,
            emblemHex = 0xFFDC1E3AL,
            animStyle = CapeAnimStyle.LASER_SCAN
        ),
        ClientCapeSpec(
            id = "blood_moon",
            title = "Blood Moon",
            subtitle = "Crimson lunar eclipse + night mist",
            topHex = 0xFF5A0D1BL,
            bottomHex = 0xFF120B10L,
            emblemHex = 0xFFFF2E4CL,
            animStyle = CapeAnimStyle.ECLIPSE
        ),
        ClientCapeSpec(
            id = "inferno_dragon",
            title = "Inferno Blaze",
            subtitle = "Molten lava veins + rising fire",
            topHex = 0xFFB5192BL,
            bottomHex = 0xFF2B0B0EL,
            emblemHex = 0xFFFF9F1CL,
            animStyle = CapeAnimStyle.FLAMES
        ),
        ClientCapeSpec(
            id = "crimson_storm",
            title = "Crimson Storm",
            subtitle = "Forked red lightning on dark slate",
            topHex = 0xFF22252EL,
            bottomHex = 0xFF0E1015L,
            emblemHex = 0xFFFF3355L,
            animStyle = CapeAnimStyle.LIGHTNING
        ),
        ClientCapeSpec(
            id = "ender_vortex",
            title = "Ender Vortex",
            subtitle = "Swirling void portal + rift sparks",
            topHex = 0xFF3A1C6EL,
            bottomHex = 0xFF0E091AL,
            emblemHex = 0xFFC77DFFL,
            animStyle = CapeAnimStyle.VORTEX
        ),
        ClientCapeSpec(
            id = "aurora_drift",
            title = "Aurora Borealis",
            subtitle = "Flowing northern-lights ribbons",
            topHex = 0xFF154E56L,
            bottomHex = 0xFF121829L,
            emblemHex = 0xFF5CE1E6L,
            animStyle = CapeAnimStyle.AURORA
        ),
        ClientCapeSpec(
            id = "cyber_matrix",
            title = "Cyber Matrix",
            subtitle = "Digital rain over dark steel",
            topHex = 0xFF123524L,
            bottomHex = 0xFF0B1410L,
            emblemHex = 0xFF2CEAA3L,
            animStyle = CapeAnimStyle.MATRIX
        ),
        ClientCapeSpec(
            id = "cosmic_nebula",
            title = "Cosmic Nebula",
            subtitle = "Twinkling stars + shooting comet",
            topHex = 0xFF241B4BL,
            bottomHex = 0xFF0B0A16L,
            emblemHex = 0xFF9BF6FFL,
            animStyle = CapeAnimStyle.STARFIELD
        ),
        ClientCapeSpec(
            id = "glacier_shards",
            title = "Glacier Frost",
            subtitle = "Drifting ice shards + crystal pulse",
            topHex = 0xFF25688AL,
            bottomHex = 0xFF102332L,
            emblemHex = 0xFFD8F8FFL,
            animStyle = CapeAnimStyle.FROST
        ),
        ClientCapeSpec(
            id = "sakura_wind",
            title = "Sakura Tempest",
            subtitle = "Drifting blossom petals at dusk",
            topHex = 0xFF6B2446L,
            bottomHex = 0xFF1B121CL,
            emblemHex = 0xFFFFB3D9L,
            animStyle = CapeAnimStyle.PETALS
        ),
        ClientCapeSpec(
            id = "golden_monarch",
            title = "Golden Monarch",
            subtitle = "Sweeping royal gold sheen",
            topHex = 0xFF7D5310L,
            bottomHex = 0xFF1F160AL,
            emblemHex = 0xFFFFD166L,
            animStyle = CapeAnimStyle.GOLD_SHEEN
        ),
        ClientCapeSpec(
            id = "sculk_pulse",
            title = "Sculk Warden",
            subtitle = "Sonic soul rings in deep dark",
            topHex = 0xFF0B3942L,
            bottomHex = 0xFF071318L,
            emblemHex = 0xFF2EC4B6L,
            animStyle = CapeAnimStyle.SONIC_RINGS
        ),
        ClientCapeSpec(
            id = "owner_crimson",
            title = "Owner Sovereign",
            subtitle = "Exclusive Royal Crimson & Gold Crest",
            topHex = 0xFFDC1E3AL,
            bottomHex = 0xFF3B0611L,
            emblemHex = 0xFFFFD166L,
            animStyle = CapeAnimStyle.ROYAL_CREST,
            requiredEmail = OWNER_EMAIL,
            badgeText = "(OWNER)"
        ),
        ClientCapeSpec(
            id = "coowner_celestial",
            title = "Co-Owner Celestial",
            subtitle = "Exclusive Sky-Blue Starlight Mantle",
            topHex = 0xFF2EB8E6L,
            bottomHex = 0xFF0A283DL,
            emblemHex = 0xFFE6FCFFL,
            animStyle = CapeAnimStyle.CELESTIAL_WINGS,
            requiredEmail = CO_OWNER_EMAIL,
            badgeText = "(CO-OWNER)"
        )
    )

    fun currentEmail(): String? = SocialBackend.email

    fun canEquipCape(spec: ClientCapeSpec): Boolean = spec.isUnlockedFor(currentEmail())

    /**
     * The cape this player actually wears, or null when they have chosen none.
     *
     * It returns null rather than falling back to a cape: an unequipped cape that silently becomes
     * "Crimson Sovereign" is exactly the bug where a player saw a cape nobody had picked. A cape
     * the account cannot equip (a co-owner cape on someone else's account) is likewise treated as
     * unequipped instead of being swapped for a different one.
     */
    fun effectiveCapeSpec(): ClientCapeSpec? {
        if (AerixClientStore.equippedCapeId == AerixClientStore.CAPE_NONE) return null
        val wanted = capes.firstOrNull { it.id == AerixClientStore.equippedCapeId } ?: return null
        return wanted.takeIf { canEquipCape(it) }
    }

    /** Whether the player has picked an Aerix cape; a cape the account cannot equip does not count. */
    fun hasEquippedCape(): Boolean = effectiveCapeSpec() != null

    private fun resolveImportedCapeBytes(): ByteArray? = runCatching {
        val account = net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge.current ?: return@runCatching null
        val file = net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge.customCapeFile(account.source)
        if (!file.isFile || file.length() <= 0L) return@runCatching null
        val bytes = file.readBytes()
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
        val valid = decoded.width in 22..1024 && decoded.height in 17..1024
        decoded.recycle()
        bytes.takeIf { valid }
    }.getOrNull()

    /**
     * Generates a genuine `64×32` Minecraft Cape + Elytra PNG byte array served by `AerixSkinServer`:
     * - Front/Back/Sides Cape UV: `(0..21, 0..16)` (Front visible face = `1..10, 1..16`)
     * - Elytra Wing UV: `(22..45, 0..21)` so wearing an Elytra renders matching custom wings too!
     */
    fun resolveInGameCapeBytes(context: Context): ByteArray? = runCatching {
        AerixClientStore.init(context)
        if (AerixClientStore.equippedCapeId == "custom") {
            resolveImportedCapeBytes()?.let { return@runCatching it }
        }
        // No Aerix cape equipped means no cape at all - not a generated one. The game falls back to
        // whatever the account itself owns, which is the whole point.
        val spec = effectiveCapeSpec() ?: return@runCatching null

        val bmp = buildAnimatedCapeFrameBitmap(spec, frameIndex = 0, totalFrames = 12)
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }.getOrNull()

    /**
     * Writes 12 live-animated `64×32` PNG frames (`frame_00.png`..`frame_11.png`) + `meta.properties`
     * into `<instanceGameDir>/config/aerix_cape_frames/` and syncs `<instanceGameDir>/config/waveycapes.json`
     * so `AerixClientMod` + `WaveyCapes` animate AND wave the cape live in-game!
     */
    /** Last outcome of [syncCapeAssetsToInstance], shown in the Client menu so a silent failure
     *  cannot hide behind a switch that looks enabled. */
    @Volatile
    @JvmStatic
    var lastCapeSyncStatus: String = "Not synced in this session"
        private set

    fun syncCapeAssetsToInstance(context: Context, instanceGameDir: File) {
        runCatching {
            AerixClientStore.init(context)
            val spec = effectiveCapeSpec()
            val importedCape = if (AerixClientStore.equippedCapeId == "custom") resolveImportedCapeBytes() else null
            val configDir = File(instanceGameDir, "config").apply { mkdirs() }
            val framesDir = File(configDir, "aerix_cape_frames").apply { mkdirs() }

            // Nothing equipped: clear the frames an earlier choice left behind. Leaving them would
            // keep drawing a cape the player has since taken off, because the bridge mod reads the
            // folder, not the launcher's preferences.
            if (spec == null && importedCape == null) {
                framesDir.listFiles()?.forEach { it.delete() }
                File(configDir, "waveycapes.json").delete()
                lastCapeSyncStatus = "No cape equipped; removed the frames of the previous one"
                return@runCatching
            }

            val total = 12
            for (i in 0 until total) {
                val outFile = File(framesDir, String.format(java.util.Locale.ROOT, "frame_%02d.png", i))
                if (importedCape != null) {
                    FileOutputStream(outFile).use { it.write(importedCape) }
                } else {
                    val bmp = buildAnimatedCapeFrameBitmap(spec!!, frameIndex = i, totalFrames = total)
                    FileOutputStream(outFile).use { fos -> bmp.compress(Bitmap.CompressFormat.PNG, 100, fos) }
                }
            }
            val now = System.currentTimeMillis()
            val metaFile = File(framesDir, "meta.properties")
            metaFile.writeText(
                buildString {
                    appendLine("capeId=${if (importedCape != null) "custom" else spec?.id ?: AerixClientStore.CAPE_NONE}")
                    appendLine("frames=$total")
                    appendLine("waving=${AerixClientStore.wavingCapeEnabled}")
                    appendLine("updatedAtMs=$now")
                }
            )
            metaFile.setLastModified(now)

            // Exact WaveyCapes v2 config schema (`dev.tr7zw.waveycapes.versionless.Config`)
            val waveyOn = AerixClientStore.wavingCapeEnabled
            val waveyJson = """
                {
                  "configVersion": 2,
                  "windMode": "${if (waveyOn) "WAVES" else "NONE"}",
                  "capeStyle": "${if (waveyOn) "SMOOTH" else "BLOCKY"}",
                  "capeMovement": "${if (waveyOn) "BASIC_SIMULATION_3D" else "VANILLA"}",
                  "gravity": 24,
                  "heightMultiplier": 7,
                  "straveMultiplier": 3
                }
            """.trimIndent()
            val waveyFile = File(configDir, "waveycapes.json")
            waveyFile.writeText(waveyJson)
            waveyFile.setLastModified(now)
            val bytes = framesDir.listFiles()?.sumOf { it.length() } ?: 0L
            lastCapeSyncStatus = "Wrote $total frames (${bytes} B) for " +
                "${if (importedCape != null) "your imported cape" else spec?.id} into ${framesDir.name}; " +
                "bridge mod: ${AerixClientModBridge.lastStatus}"
        }.onFailure { error ->
            lastCapeSyncStatus = "Could not write cape frames: ${error.message ?: error.javaClass.simpleName}"
            android.util.Log.w("AerixCapes", "Cape frame sync failed", error)
        }
    }

    fun syncCapeAssetsToAllClientInstances(context: Context) {
        runCatching {
            val all = Instances.loadAllInstances()
            for (inst in all) {
                if (AerixClientInstaller.isManagedInstance(inst)) {
                    inst.gameDirectory?.let { syncCapeAssetsToInstance(context, it) }
                }
            }
        }
    }

    /**
     * Generates a single animated frame (`0 .. totalFrames-1`) of the `64×32` Cape + Elytra sheet
     * with unmistakable pixel-level motion across both the visible back face (`x=1..10, y=1..16`),
     * inner face (`x=12..21, y=1..16`), and Elytra wing (`x=22..45, y=0..21`).
     */
    fun buildAnimatedCapeFrameBitmap(
        spec: ClientCapeSpec,
        frameIndex: Int,
        totalFrames: Int = 12
    ): Bitmap {
        val bmp = buildCapeAndElytraBitmap(spec)
        val phase = ((frameIndex % totalFrames.coerceAtLeast(1)).toFloat() / totalFrames.coerceAtLeast(1)) *
            (2f * Math.PI.toFloat())
        val emb = spec.emblemHex.toInt()

        fun glowBlend(base: Int, target: Int, amount: Float): Int {
            val a = amount.coerceIn(0f, 1f)
            val r = (AndroidColor.red(base) + (AndroidColor.red(target) - AndroidColor.red(base)) * a).toInt().coerceIn(0, 255)
            val g = (AndroidColor.green(base) + (AndroidColor.green(target) - AndroidColor.green(base)) * a).toInt().coerceIn(0, 255)
            val b = (AndroidColor.blue(base) + (AndroidColor.blue(target) - AndroidColor.blue(base)) * a).toInt().coerceIn(0, 255)
            return AndroidColor.argb(255, r, g, b)
        }

        // Apply live shader highlight across front/back cape UV (x: 1..10 and 12..21, y: 1..16)
        for (faceOffset in intArrayOf(1, 12)) {
            for (ly in 0..15) {
                val v = ly / 15f
                for (lx in 0..9) {
                    val u = lx / 9f
                    val px = faceOffset + lx
                    val py = 1 + ly
                    val h = sampleAnimatedHighlight(spec.animStyle, u, v, phase)
                    if (h > 0.04f) {
                        val base = bmp.getPixel(px, py)
                        val hiTarget = if (h > 0.68f) AndroidColor.WHITE else emb
                        bmp.setPixel(px, py, glowBlend(base, hiTarget, (h * 0.92f).coerceAtMost(0.95f)))
                    }
                }
            }
        }

        // Distinct crisp moving pixel particles per frame on visible outer cape face (x=2..9, y=2..15)
        val f = frameIndex % totalFrames.coerceAtLeast(1)
        when (spec.animStyle) {
            CapeAnimStyle.EMBERS, CapeAnimStyle.FLAMES -> {
                for (k in 0..4) {
                    val ex = 2 + ((k * 3 + f) % 8)
                    val ey = 15 - ((f * 2 + k * 3) % 14)
                    bmp.setPixel(ex, ey, if (k % 2 == 0) AndroidColor.WHITE else emb)
                }
            }
            CapeAnimStyle.LASER_SCAN -> {
                val scanY = 2 + ((f * 14) / totalFrames.coerceAtLeast(1)).coerceIn(0, 13)
                for (x in 2..9) {
                    bmp.setPixel(x, scanY, AndroidColor.WHITE)
                    if (scanY > 2) bmp.setPixel(x, scanY - 1, emb)
                }
            }
            CapeAnimStyle.LIGHTNING -> {
                val shift = (f % 3) - 1
                for (ly in 2..15) {
                    val lx = (5 + shift + ((ly + f) % 2)).coerceIn(2, 9)
                    if ((ly + f) % 3 != 0) bmp.setPixel(lx, ly, AndroidColor.WHITE)
                }
            }
            CapeAnimStyle.MATRIX -> {
                for (col in intArrayOf(2, 4, 7, 9)) {
                    val headY = 2 + ((f * 2 + col * 3) % 14)
                    bmp.setPixel(col, headY, AndroidColor.WHITE)
                    if (headY > 2) bmp.setPixel(col, headY - 1, emb)
                }
            }
            CapeAnimStyle.STARFIELD, CapeAnimStyle.FROST, CapeAnimStyle.PETALS -> {
                for (k in 0..4) {
                    val sx = 2 + ((k * 5 + f) % 8)
                    val sy = 2 + ((f + k * 4) % 14)
                    bmp.setPixel(sx, sy, if ((f + k) % 2 == 0) AndroidColor.WHITE else emb)
                }
            }
            else -> {
                // Travelling border sparkle around the cape frame + pulsing emblem core
                val perim = (f * 3) % 28
                val (bx, by) = when {
                    perim < 8 -> (2 + perim) to 2
                    perim < 20 -> 9 to (2 + (perim - 8))
                    perim < 28 -> (9 - (perim - 20)).coerceAtLeast(2) to 15
                    else -> 2 to 8
                }
                bmp.setPixel(bx.coerceIn(1, 10), by.coerceIn(1, 16), AndroidColor.WHITE)
                if (f % 2 == 0) {
                    bmp.setPixel(5, 8, AndroidColor.WHITE)
                    bmp.setPixel(6, 8, AndroidColor.WHITE)
                }
            }
        }

        // Also animate Elytra wing surface (x: 23..44, y: 1..20)
        for (y in 1..20) {
            val v = y / 20f
            for (x in 23..44) {
                val u = (x - 23) / 21f
                val h = sampleAnimatedHighlight(spec.animStyle, u, v, phase)
                if (h > 0.08f) {
                    bmp.setPixel(x, y, glowBlend(bmp.getPixel(x, y), emb, h * 0.8f))
                }
            }
        }
        return bmp
    }

    fun buildCapeAndElytraBitmap(spec: ClientCapeSpec): Bitmap {
        val bmp = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val top = spec.topHex.toInt()
        val bot = spec.bottomHex.toInt()
        val emb = spec.emblemHex.toInt()

        fun blend(c1: Int, c2: Int, t: Float): Int {
            val f = t.coerceIn(0f, 1f)
            val r = (AndroidColor.red(c1) + (AndroidColor.red(c2) - AndroidColor.red(c1)) * f).toInt()
            val g = (AndroidColor.green(c1) + (AndroidColor.green(c2) - AndroidColor.green(c1)) * f).toInt()
            val b = (AndroidColor.blue(c1) + (AndroidColor.blue(c2) - AndroidColor.blue(c1)) * f).toInt()
            return AndroidColor.argb(255, r, g, b)
        }

        // Fill Cape UV box (0..21, 0..16)
        for (y in 0..16) {
            val t = y / 16f
            val rowCol = blend(top, bot, t)
            for (x in 0..21) {
                bmp.setPixel(x, y, rowCol)
            }
        }

        // Border & animated-style motif on primary visible cape face (x: 1..10, y: 1..16)
        for (x in 1..10) {
            bmp.setPixel(x, 1, emb)
            bmp.setPixel(x, 16, emb)
        }
        for (y in 1..16) {
            bmp.setPixel(1, y, emb)
            bmp.setPixel(10, y, emb)
        }

        when (spec.animStyle) {
            CapeAnimStyle.LIGHTNING -> {
                val bolt = listOf(6 to 2, 5 to 4, 4 to 6, 6 to 7, 5 to 9, 4 to 11, 6 to 13, 5 to 15)
                bolt.forEach { (bx, by) -> bmp.setPixel(bx, by, emb) }
            }
            CapeAnimStyle.ECLIPSE, CapeAnimStyle.SONIC_RINGS -> {
                for (y in 5..11) for (x in 3..8) {
                    val d = sqrt(((x - 5.5f) * (x - 5.5f) + (y - 8f) * (y - 8f)).toDouble())
                    if (d in 1.8..3.1) bmp.setPixel(x, y, emb)
                }
            }
            CapeAnimStyle.MATRIX -> {
                for (y in 2..15 step 2) {
                    bmp.setPixel(3, y, emb)
                    bmp.setPixel(6, (y + 1).coerceAtMost(15), emb)
                    bmp.setPixel(8, y, emb)
                }
            }
            else -> {
                // Iconic Aerix diamond-star emblem in center (x: 4..7, y: 6..11)
                bmp.setPixel(5, 6, emb); bmp.setPixel(6, 6, emb)
                bmp.setPixel(4, 7, emb); bmp.setPixel(5, 7, AndroidColor.WHITE); bmp.setPixel(6, 7, AndroidColor.WHITE); bmp.setPixel(7, 7, emb)
                bmp.setPixel(3, 8, emb); bmp.setPixel(4, 8, AndroidColor.WHITE); bmp.setPixel(5, 8, emb); bmp.setPixel(6, 8, emb); bmp.setPixel(7, 8, AndroidColor.WHITE); bmp.setPixel(8, 8, emb)
                bmp.setPixel(4, 9, emb); bmp.setPixel(5, 9, AndroidColor.WHITE); bmp.setPixel(6, 9, AndroidColor.WHITE); bmp.setPixel(7, 9, emb)
                bmp.setPixel(5, 10, emb); bmp.setPixel(6, 10, emb)
                if (spec.animStyle == CapeAnimStyle.FLAMES || spec.animStyle == CapeAnimStyle.EMBERS) {
                    bmp.setPixel(3, 14, emb); bmp.setPixel(5, 13, emb); bmp.setPixel(8, 14, emb); bmp.setPixel(6, 15, emb)
                }
            }
        }

        // Fill Elytra Wing UV (x: 22..45, y: 0..21) so Elytras match the equipped cape
        for (y in 0..21) {
            val t = y / 21f
            val baseCol = blend(top, bot, t)
            for (x in 22..45) {
                val rim = (x == 22 || x == 45 || y == 0 || y == 21 || abs((x - 22) - y) <= 1)
                bmp.setPixel(x, y, if (rim) emb else baseCol)
            }
        }
        return bmp
    }

    /** Helper computing a live animated color shift / glow intensity at normalized UV `(u, v)` and `phase`. */
    fun sampleAnimatedHighlight(style: CapeAnimStyle, u: Float, v: Float, phase: Float): Float {
        return when (style) {
            CapeAnimStyle.EMBERS -> {
                val spark = sin(u * 9f + phase * 2.2f) * cos((v + phase * 0.45f) * 10f)
                if (spark > 0.62f) (spark - 0.62f) * 2.2f else 0f
            }
            CapeAnimStyle.LASER_SCAN -> {
                val scanY = ((phase / (2f * Math.PI.toFloat())) * 1.4f) % 1.0f
                (1f - (abs(v - scanY) * 7f)).coerceIn(0f, 1f) * 0.85f
            }
            CapeAnimStyle.ECLIPSE -> {
                val dx = u - 0.5f
                val dy = v - 0.45f
                val dist = sqrt(dx * dx + dy * dy)
                val pulse = 0.22f + 0.04f * sin(phase * 1.5f)
                (1f - abs(dist - pulse) * 14f).coerceIn(0f, 1f)
            }
            CapeAnimStyle.FLAMES -> {
                val flameH = 0.55f + 0.22f * sin(u * 8f + phase * 3f)
                if (v > flameH) ((v - flameH) / (1f - flameH)).coerceIn(0f, 1f) * 0.9f else 0f
            }
            CapeAnimStyle.LIGHTNING -> {
                val boltX = 0.5f + 0.16f * sin(v * 12f + phase * 4f)
                val flash = (sin(phase * 3f) + 0.4f).coerceIn(0f, 1f)
                (1f - abs(u - boltX) * 13f).coerceIn(0f, 1f) * flash
            }
            CapeAnimStyle.VORTEX -> {
                val dx = u - 0.5f
                val dy = v - 0.5f
                val r = sqrt(dx * dx + dy * dy)
                val ang = kotlin.math.atan2(dy, dx)
                ((sin(ang * 3f + r * 16f - phase * 2.5f) - 0.45f) * 1.6f).coerceIn(0f, 1f)
            }
            CapeAnimStyle.AURORA -> {
                val band = sin(u * 5f + v * 4f - phase * 1.8f)
                ((band - 0.25f) * 1.1f).coerceIn(0f, 0.85f)
            }
            CapeAnimStyle.MATRIX -> {
                val colDrop = ((v * 3f - phase * 0.8f + (u * 7f).toInt() * 0.37f) % 1f + 1f) % 1f
                if (colDrop < 0.28f) (1f - colDrop / 0.28f) * 0.85f else 0f
            }
            CapeAnimStyle.STARFIELD -> {
                val twinkle = sin(u * 17f + phase * 2f) * cos(v * 19f - phase * 1.5f)
                if (twinkle > 0.68f) (twinkle - 0.68f) * 2.6f else 0f
            }
            CapeAnimStyle.FROST -> {
                val shard = cos((u + v) * 9f - phase * 1.7f)
                ((shard - 0.45f) * 1.5f).coerceIn(0f, 0.9f)
            }
            CapeAnimStyle.PETALS -> {
                val p = sin((u * 8f + phase * 1.3f)) * cos((v * 8f - phase * 1.6f))
                if (p > 0.58f) (p - 0.58f) * 2.0f else 0f
            }
            CapeAnimStyle.GOLD_SHEEN -> {
                val diag = ((u + v) * 0.5f - (phase / (2f * Math.PI.toFloat()))) % 1f
                val norm = (diag + 1f) % 1f
                (1f - abs(norm - 0.5f) * 8f).coerceIn(0f, 0.95f)
            }
            CapeAnimStyle.SONIC_RINGS -> {
                val dx = u - 0.5f
                val dy = v - 0.5f
                val dist = sqrt(dx * dx + dy * dy)
                val ring = ((dist * 3.5f - phase * 0.6f) % 1f + 1f) % 1f
                (1f - abs(ring - 0.5f) * 7f).coerceIn(0f, 0.95f)
            }
            CapeAnimStyle.ROYAL_CREST, CapeAnimStyle.CELESTIAL_WINGS -> {
                val crest = 1f - (abs(u - 0.5f) * 5f + abs(v - 0.45f) * 4f)
                val shimmer = 0.5f + 0.5f * sin(phase * 2.2f - v * 5f)
                (crest.coerceIn(0f, 1f) * 0.75f + shimmer * 0.25f).coerceIn(0f, 1f)
            }
        }
    }
}
