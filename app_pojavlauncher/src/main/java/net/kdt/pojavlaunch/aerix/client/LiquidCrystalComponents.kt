package net.kdt.pojavlaunch.aerix.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.components.bringFocusedItemIntoView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Locked Crimson Red & Dark Gray palette across the entire Aerix Client (cannot be changed).
 */
object LiquidCrystalPalette {
    val CrimsonPrimary = Color(0xFFDC1E3A)
    val CrimsonGlow = Color(0xFFFF2E4C)
    val CrimsonDeep = Color(0xFF8E1125)
    val CrimsonTintFill = Color(0xFF2B171D)

    val DarkOuter = Color(0xFF121317)
    val DarkPanel = Color(0xFF1B1D23)
    val DarkHeader = Color(0xFF15171C)
    val DarkTile = Color(0xFF242730)
    val DarkTileBorder = Color(0xFF343844)

    val TextPrimary = Color(0xFFF5F6F8)
    val TextSecondary = Color(0xFFB3B8C4)
    val TextMuted = Color(0xFF7D8392)

    val OwnerCrimson = Color(0xFFFF3855)
    val CoOwnerSky = Color(0xFF4AD4FF)
    val GoldStar = Color(0xFFFFD166)

    // Compatibility aliases so HUD/components share the exact locked Crimson + Dark Gray colors
    val IceWhite = TextPrimary
    val IceSub = TextSecondary
    val IceMuted = TextMuted
    val PrismCyan = CrimsonPrimary
    val PrismMint = CrimsonGlow
}

@Composable
fun LiquidCrystalCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    strong: Boolean = false,
    accentBorder: Color? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bg = if (strong) LiquidCrystalPalette.DarkHeader else LiquidCrystalPalette.DarkPanel
    val rim = accentBorder ?: if (strong) LiquidCrystalPalette.CrimsonPrimary else LiquidCrystalPalette.DarkTileBorder

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.2.dp, rim, shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidCrystalPill(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = LiquidCrystalPalette.CrimsonPrimary,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = if (selected) LiquidCrystalPalette.CrimsonTintFill else LiquidCrystalPalette.DarkTile
    val border = if (selected) accent else LiquidCrystalPalette.DarkTileBorder

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.bringFocusedItemIntoView().clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else LiquidCrystalPalette.TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.28f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun LiquidCrystalPrimaryButton(
    text: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    accent: Color = LiquidCrystalPalette.CrimsonPrimary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val fill = Brush.horizontalGradient(
        if (enabled) listOf(accent, LiquidCrystalPalette.CrimsonDeep)
        else listOf(LiquidCrystalPalette.DarkTile, LiquidCrystalPalette.DarkHeader)
    )
    val border = if (enabled) LiquidCrystalPalette.CrimsonGlow else LiquidCrystalPalette.DarkTileBorder

    Column(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .border(1.dp, border, shape)
            .bringFocusedItemIntoView()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else LiquidCrystalPalette.TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 11.sp
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * LIVE ANIMATED CAPE SWATCH (Used inside the 15-Cape Grid on the Right)
 * ═══════════════════════════════════════════════════════════════════════════ */
@Composable
fun AnimatedCapePreviewSwatch(
    spec: ClientCapeSpec,
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val topCol = Color(spec.topHex)
        val botCol = Color(spec.bottomHex)
        val embCol = Color(spec.emblemHex)
        val cols = 6
        val rows = 9
        val cellW = size.width / cols
        val cellH = size.height / rows

        for (r in 0 until rows) {
            val v = (r + 0.5f) / rows
            val baseRow = lerpColor(topCol, botCol, v)
            for (c in 0 until cols) {
                val u = (c + 0.5f) / cols
                val glow = AerixClientCosmetics.sampleAnimatedHighlight(spec.animStyle, u, v, phase)
                val finalCol = lerpColor(baseRow, embCol, glow.coerceIn(0f, 1f))
                drawRect(
                    color = finalCol,
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW + 0.8f, cellH + 0.8f)
                )
            }
        }
        // Crisp emblem border
        drawRoundRect(
            color = embCol.copy(alpha = 0.78f),
            size = size,
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 1.5f)
        )
    }
}

private data class SkinPaletteColors(
    val skinFace: Color = Color(0xFFD6A382),
    val hairBack: Color = Color(0xFF3A2618),
    val eyeColor: Color = Color(0xFF3B4F9E),
    val torsoFront: Color = Color(0xFF0099A8),
    val torsoBack: Color = Color(0xFF007D8A),
    val armColor: Color = Color(0xFF008C99),
    val legColor: Color = Color(0xFF3A367A)
)

private fun extractSkinPalette(bmp: Bitmap?): SkinPaletteColors {
    if (bmp == null || bmp.width < 64 || bmp.height < 32) return SkinPaletteColors()
    fun sampleAvg(x0: Int, y0: Int, w: Int, h: Int, fallback: Color): Color {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0
        val maxX = (x0 + w).coerceAtMost(bmp.width)
        val maxY = (y0 + h).coerceAtMost(bmp.height)
        for (y in y0.coerceAtLeast(0) until maxY) {
            for (x in x0.coerceAtLeast(0) until maxX) {
                val px = bmp.getPixel(x, y)
                val a = (px ushr 24) and 0xFF
                if (a > 40) {
                    rSum += (px ushr 16) and 0xFF
                    gSum += (px ushr 8) and 0xFF
                    bSum += px and 0xFF
                    count++
                }
            }
        }
        if (count == 0) return fallback
        return Color(
            red = (rSum / count) / 255f,
            green = (gSum / count) / 255f,
            blue = (bSum / count) / 255f,
            alpha = 1f
        )
    }
    val def = SkinPaletteColors()
    return SkinPaletteColors(
        skinFace = sampleAvg(9, 11, 6, 4, def.skinFace),
        hairBack = sampleAvg(25, 9, 6, 6, def.hairBack),
        eyeColor = sampleAvg(10, 12, 2, 1, def.eyeColor),
        torsoFront = sampleAvg(20, 20, 8, 12, def.torsoFront),
        torsoBack = sampleAvg(32, 20, 8, 12, def.torsoBack),
        armColor = sampleAvg(44, 20, 4, 10, def.armColor),
        legColor = sampleAvg(4, 20, 4, 12, def.legColor)
    )
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}

private fun shadeColor(c: Color, factor: Float): Color = Color(
    red = (c.red * factor).coerceIn(0f, 1f),
    green = (c.green * factor).coerceIn(0f, 1f),
    blue = (c.blue * factor).coerceIn(0f, 1f),
    alpha = c.alpha
)

/**
 * Real 3D Perspective Cuboid Minecraft Character + Live Animated Cape Stage!
 * - Drag horizontally/vertically to rotate 360° around the player's actual skin & back cape.
 * - Supports Waving Cape ON (multi-segment 3D cloth wave) and OFF (straight classic rigid cape).
 */
@Composable
fun Interactive3DCosmeticStage(
    /** The cape to draw on the model, or null when the player is wearing none. */
    capeSpec: ClientCapeSpec?,
    wavingCape: Boolean,
    playerName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val account = PojavAccountBridge.current
    val wardrobeRev = PojavAccountBridge.wardrobeRevision

    var skinPalette by remember { mutableStateOf(SkinPaletteColors()) }
    var skinBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(account?.key, account?.username, wardrobeRev) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val rawAcc = net.kdt.pojavlaunch.authenticator.accounts.Accounts.getCurrent() ?: return@runCatching null
                val f = PojavAccountBridge.customSkinFile(rawAcc)
                if (f.isFile && f.length() > 0) BitmapFactory.decodeFile(f.absolutePath) else null
            }.getOrNull()
        }
        skinBitmap = loaded
        skinPalette = extractSkinPalette(loaded)
    }

    var yawDeg by remember { mutableFloatStateOf(155f) } // Angled back view showing 3D character + cape!
    var pitchDeg by remember { mutableFloatStateOf(10f) }

    val inf = rememberInfiniteTransition(label = "cape3DWave")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    yawDeg = (yawDeg + dragAmount.x * 0.75f) % 360f
                    pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(-22f, 28f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.53f
            val unit = (size.height / 38f).coerceIn(3.2f, 8.5f)

            // Floor Crimson-Dark Pedestal Shadow & Ring
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy + 12.6f * unit),
                    radius = 10f * unit
                ),
                topLeft = Offset(cx - 9f * unit, cy + 10.6f * unit),
                size = Size(18f * unit, 4f * unit)
            )
            drawOval(
                color = LiquidCrystalPalette.CrimsonPrimary.copy(alpha = 0.55f),
                topLeft = Offset(cx - 7f * unit, cy + 11.1f * unit),
                size = Size(14f * unit, 3f * unit),
                style = Stroke(width = 1.4f)
            )

            val yawRad = yawDeg * (PI / 180.0)
            val pitchRad = pitchDeg * (PI / 180.0)
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()
            val cosP = cos(pitchRad).toFloat()
            val sinP = sin(pitchRad).toFloat()

            class Vec3(val x: Float, val y: Float, val z: Float)
            class Quad3D(val pts: Array<Offset>, val depth: Float, val color: Color, val stroke: Color? = null)

            fun project(x: Float, y: Float, z: Float): Vec3 {
                // Rotate around Y (yaw), then around X (pitch)
                val rx = x * cosY + z * sinY
                val rz0 = -x * sinY + z * cosY
                val ry = y * cosP - rz0 * sinP
                val rz = y * sinP + rz0 * cosP
                val persp = 46f / (46f - rz * 0.35f).coerceAtLeast(18f)
                return Vec3(
                    x = cx + rx * unit * persp,
                    y = cy + ry * unit * persp,
                    z = rz
                )
            }

            val quads = ArrayList<Quad3D>(96)

            fun addQuad(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, color: Color, stroke: Color? = null) {
                // Backface cull Optional? No, keeping all 6 cuboid faces depth-sorted guarantees watertight solids!
                val avgZ = (p0.z + p1.z + p2.z + p3.z) * 0.25f
                quads += Quad3D(
                    pts = arrayOf(Offset(p0.x, p0.y), Offset(p1.x, p1.y), Offset(p2.x, p2.y), Offset(p3.x, p3.y)),
                    depth = avgZ,
                    color = color,
                    stroke = stroke
                )
            }

            fun addCuboid(
                cx0: Float, cy0: Float, cz0: Float,
                hx: Float, hy: Float, hz: Float,
                frontCol: Color,
                backCol: Color,
                sideCol: Color,
                topCol: Color,
                botCol: Color
            ) {
                // In model coords: +Y is down, -Y is up; +Z is front (face), -Z is back (cape side)
                val v000 = project(cx0 - hx, cy0 - hy, cz0 - hz)
                val v100 = project(cx0 + hx, cy0 - hy, cz0 - hz)
                val v110 = project(cx0 + hx, cy0 + hy, cz0 - hz)
                val v010 = project(cx0 - hx, cy0 + hy, cz0 - hz)

                val v001 = project(cx0 - hx, cy0 - hy, cz0 + hz)
                val v101 = project(cx0 + hx, cy0 - hy, cz0 + hz)
                val v111 = project(cx0 + hx, cy0 + hy, cz0 + hz)
                val v011 = project(cx0 - hx, cy0 + hy, cz0 + hz)

                val outline = Color.Black.copy(alpha = 0.22f)
                // Front (+Z)
                addQuad(v001, v101, v111, v011, frontCol, outline)
                // Back (-Z)
                addQuad(v100, v000, v010, v110, backCol, outline)
                // Left (-X)
                addQuad(v000, v001, v011, v010, shadeColor(sideCol, 0.84f), outline)
                // Right (+X)
                addQuad(v101, v100, v110, v111, shadeColor(sideCol, 0.90f), outline)
                // Top (-Y)
                addQuad(v000, v100, v101, v001, shadeColor(topCol, 1.06f), outline)
                // Bottom (+Y)
                addQuad(v011, v111, v110, v010, shadeColor(botCol, 0.70f), outline)
            }

            val idleBreath = sin(phase) * 0.18f

            // 1. Left & Right Legs (4x12x4 Minecraft pixels -> scaled by 0.5 = 2x6x2 units)
            addCuboid(
                cx0 = -1.05f, cy0 = 9.0f, cz0 = 0f,
                hx = 1.0f, hy = 3.0f, hz = 1.0f,
                frontCol = skinPalette.legColor,
                backCol = shadeColor(skinPalette.legColor, 0.88f),
                sideCol = shadeColor(skinPalette.legColor, 0.82f),
                topCol = skinPalette.legColor,
                botCol = shadeColor(skinPalette.legColor, 0.65f)
            )
            addCuboid(
                cx0 = 1.05f, cy0 = 9.0f, cz0 = 0f,
                hx = 1.0f, hy = 3.0f, hz = 1.0f,
                frontCol = skinPalette.legColor,
                backCol = shadeColor(skinPalette.legColor, 0.88f),
                sideCol = shadeColor(skinPalette.legColor, 0.82f),
                topCol = skinPalette.legColor,
                botCol = shadeColor(skinPalette.legColor, 0.65f)
            )

            // 2. Torso (8x12x4 -> 4x6x2 units)
            addCuboid(
                cx0 = 0f, cy0 = 3.0f, cz0 = 0f,
                hx = 2.0f, hy = 3.0f, hz = 1.0f,
                frontCol = skinPalette.torsoFront,
                backCol = skinPalette.torsoBack,
                sideCol = shadeColor(skinPalette.torsoFront, 0.85f),
                topCol = skinPalette.torsoFront,
                botCol = shadeColor(skinPalette.torsoFront, 0.7f)
            )

            // 3. Left & Right Arms
            addCuboid(
                cx0 = -3.05f, cy0 = 3.0f + idleBreath, cz0 = 0f,
                hx = 1.0f, hy = 3.0f, hz = 1.0f,
                frontCol = skinPalette.armColor,
                backCol = shadeColor(skinPalette.armColor, 0.9f),
                sideCol = shadeColor(skinPalette.armColor, 0.84f),
                topCol = skinPalette.armColor,
                botCol = skinPalette.skinFace
            )
            addCuboid(
                cx0 = 3.05f, cy0 = 3.0f - idleBreath, cz0 = 0f,
                hx = 1.0f, hy = 3.0f, hz = 1.0f,
                frontCol = skinPalette.armColor,
                backCol = shadeColor(skinPalette.armColor, 0.9f),
                sideCol = shadeColor(skinPalette.armColor, 0.84f),
                topCol = skinPalette.armColor,
                botCol = skinPalette.skinFace
            )

            // 4. Head (8x8x8 -> 4x4x4 units)
            addCuboid(
                cx0 = 0f, cy0 = -2.1f, cz0 = 0f,
                hx = 2.0f, hy = 2.0f, hz = 2.0f,
                frontCol = skinPalette.skinFace,
                backCol = skinPalette.hairBack,
                sideCol = lerpColor(skinPalette.skinFace, skinPalette.hairBack, 0.45f),
                topCol = skinPalette.hairBack,
                botCol = shadeColor(skinPalette.skinFace, 0.8f)
            )
            // Eyes on Front Face (+Z = +2.03f)
            val eyeY0 = -2.1f
            val eyeY1 = -1.55f
            val el0 = project(-1.35f, eyeY0, 2.04f)
            val el1 = project(-0.45f, eyeY0, 2.04f)
            val el2 = project(-0.45f, eyeY1, 2.04f)
            val el3 = project(-1.35f, eyeY1, 2.04f)
            addQuad(el0, el1, el2, el3, skinPalette.eyeColor)

            val er0 = project(0.45f, eyeY0, 2.04f)
            val er1 = project(1.35f, eyeY0, 2.04f)
            val er2 = project(1.35f, eyeY1, 2.04f)
            val er3 = project(0.45f, eyeY1, 2.04f)
            addQuad(er0, er1, er2, er3, skinPalette.eyeColor)

            // Hair crown band on front forehead
            val hf0 = project(-2.02f, -4.12f, 2.04f)
            val hf1 = project(2.02f, -4.12f, 2.04f)
            val hf2 = project(2.02f, -2.95f, 2.04f)
            val hf3 = project(-2.02f, -2.95f, 2.04f)
            addQuad(hf0, hf1, hf2, hf3, skinPalette.hairBack)

            // 5. 3D Animated Back Cape (10x16 Minecraft pixels -> width 5.0, height 8.2 units)
            // No cape equipped means nothing is drawn here - the model shows a bare back rather
            // than somebody else's cape.
            capeSpec?.let { capeSpec ->
            val capeRows = 10
            val capeCols = 5
            val capeTopY = -0.05f
            val capeHeight = 8.4f
            val capeHalfW = 2.45f
            val topCol = Color(capeSpec.topHex)
            val botCol = Color(capeSpec.bottomHex)
            val embCol = Color(capeSpec.emblemHex)

            fun capeZAt(t: Float): Float {
                // Base drape behind torso (-1.15 to -2.25); if wavingCape == true, add smooth 3D sine wave!
                val baseDrape = -1.18f - t * 1.15f
                return if (wavingCape) {
                    baseDrape - sin(phase - t * 3.1f) * (0.25f + t * 1.15f)
                } else {
                    baseDrape
                }
            }

            fun capeSwayX(t: Float): Float {
                return if (wavingCape) sin(phase * 0.5f - t * 2.0f) * (t * 0.32f) else 0f
            }

            for (r in 0 until capeRows) {
                val t0 = r / capeRows.toFloat()
                val t1 = (r + 1) / capeRows.toFloat()
                val y0 = capeTopY + capeHeight * t0
                val y1 = capeTopY + capeHeight * t1
                val z0 = capeZAt(t0)
                val z1 = capeZAt(t1)
                val sx0 = capeSwayX(t0)
                val sx1 = capeSwayX(t1)
                val vMid = (t0 + t1) * 0.5f
                val baseRow = lerpColor(topCol, botCol, vMid)

                for (c in 0 until capeCols) {
                    val u0 = c / capeCols.toFloat()
                    val u1 = (c + 1) / capeCols.toFloat()
                    val uMid = (u0 + u1) * 0.5f
                    val x0Top = -capeHalfW + (capeHalfW * 2f) * u0 + sx0
                    val x1Top = -capeHalfW + (capeHalfW * 2f) * u1 + sx0
                    val x0Bot = -capeHalfW + (capeHalfW * 2f) * u0 + sx1
                    val x1Bot = -capeHalfW + (capeHalfW * 2f) * u1 + sx1

                    val glow = AerixClientCosmetics.sampleAnimatedHighlight(capeSpec.animStyle, uMid, vMid, phase)
                    val isRim = (r == 0 || r == capeRows - 1 || c == 0 || c == capeCols - 1)
                    val cellColor = if (isRim) {
                        lerpColor(baseRow, embCol, (0.55f + glow * 0.45f).coerceIn(0f, 1f))
                    } else {
                        lerpColor(baseRow, embCol, glow.coerceIn(0f, 1f))
                    }

                    val p0 = project(x0Top, y0, z0)
                    val p1 = project(x1Top, y0, z0)
                    val p2 = project(x1Bot, y1, z1)
                    val p3 = project(x0Bot, y1, z1)
                    addQuad(p0, p1, p2, p3, cellColor)
                }
            }
            }

            // Painter's Algorithm: sort quads back-to-front by projected Z depth
            quads.sortBy { it.depth }
            val reusablePath = Path()
            for (q in quads) {
                reusablePath.reset()
                reusablePath.moveTo(q.pts[0].x, q.pts[0].y)
                reusablePath.lineTo(q.pts[1].x, q.pts[1].y)
                reusablePath.lineTo(q.pts[2].x, q.pts[2].y)
                reusablePath.lineTo(q.pts[3].x, q.pts[3].y)
                reusablePath.close()
                drawPath(reusablePath, q.color)
                if (q.stroke != null) {
                    drawPath(reusablePath, q.stroke, style = Stroke(width = 0.8f))
                }
            }
        }

        // Player Nametag Pill at Top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC121317))
                .border(0.8.dp, LiquidCrystalPalette.DarkTileBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(playerName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            capeSpec?.badgeText?.let { b ->
                val bCol = when (b) {
                    "(OWNER)" -> LiquidCrystalPalette.OwnerCrimson
                    "(CO-OWNER)" -> LiquidCrystalPalette.CoOwnerSky
                    else -> LiquidCrystalPalette.CrimsonGlow
                }
                Text(b, color = bCol, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Text(
            text = "Drag to rotate 3D preview",
            color = LiquidCrystalPalette.TextMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 12-TYPE PROCEDURAL CROSSHAIR RENDERER
 * ═══════════════════════════════════════════════════════════════════════════ */
fun DrawScope.drawAerixCrosshair(
    center: Offset,
    shape: CrosshairShape,
    armSizePx: Float,
    gapPx: Float,
    thickPx: Float,
    outlinePx: Float,
    mainColor: Color,
    cooldownProgress: Float? = null,
    hitmarkerAlpha: Float = 0f
) {
    val outlineCol = Color.Black.copy(alpha = (mainColor.alpha * 0.82f).coerceIn(0f, 1f))

    fun strokeLine(a: Offset, b: Offset) {
        if (outlinePx > 0.2f) {
            drawLine(outlineCol, a, b, strokeWidth = thickPx + outlinePx * 2f, cap = StrokeCap.Round)
        }
        drawLine(mainColor, a, b, strokeWidth = thickPx, cap = StrokeCap.Round)
    }

    fun dot(pt: Offset, radius: Float = thickPx * 1.25f) {
        if (outlinePx > 0.2f) drawCircle(outlineCol, radius + outlinePx, pt)
        drawCircle(mainColor, radius, pt)
    }

    when (shape) {
        CrosshairShape.CLASSIC_PLUS -> {
            strokeLine(Offset(center.x - armSizePx, center.y), Offset(center.x + armSizePx, center.y))
            strokeLine(Offset(center.x, center.y - armSizePx), Offset(center.x, center.y + armSizePx))
        }
        CrosshairShape.TACTICAL_GAP -> {
            strokeLine(Offset(center.x - gapPx - armSizePx, center.y), Offset(center.x - gapPx, center.y))
            strokeLine(Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + armSizePx, center.y))
            strokeLine(Offset(center.x, center.y - gapPx - armSizePx), Offset(center.x, center.y - gapPx))
            strokeLine(Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + armSizePx))
        }
        CrosshairShape.GAP_DOT -> {
            strokeLine(Offset(center.x - gapPx - armSizePx, center.y), Offset(center.x - gapPx, center.y))
            strokeLine(Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + armSizePx, center.y))
            strokeLine(Offset(center.x, center.y - gapPx - armSizePx), Offset(center.x, center.y - gapPx))
            strokeLine(Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + armSizePx))
            dot(center, thickPx * 1.05f)
        }
        CrosshairShape.PRECISION_DOT -> {
            dot(center, thickPx * 1.55f)
        }
        CrosshairShape.CIRCLE -> {
            val r = (armSizePx + gapPx * 0.7f).coerceAtLeast(4.5f)
            if (outlinePx > 0.2f) drawCircle(outlineCol, r, center, style = Stroke(thickPx + outlinePx * 2f))
            drawCircle(mainColor, r, center, style = Stroke(thickPx))
        }
        CrosshairShape.CIRCLE_DOT -> {
            val r = (armSizePx + gapPx * 0.75f).coerceAtLeast(5.5f)
            if (outlinePx > 0.2f) drawCircle(outlineCol, r, center, style = Stroke(thickPx + outlinePx * 2f))
            drawCircle(mainColor, r, center, style = Stroke(thickPx))
            dot(center, thickPx * 1.05f)
        }
        CrosshairShape.X_HIT -> {
            val d0 = gapPx * 0.65f
            val d1 = (gapPx + armSizePx) * 0.78f
            strokeLine(Offset(center.x - d1, center.y - d1), Offset(center.x - d0, center.y - d0))
            strokeLine(Offset(center.x + d0, center.y - d0), Offset(center.x + d1, center.y - d1))
            strokeLine(Offset(center.x - d1, center.y + d1), Offset(center.x - d0, center.y + d0))
            strokeLine(Offset(center.x + d0, center.y + d0), Offset(center.x + d1, center.y + d1))
        }
        CrosshairShape.CHEVRON -> {
            strokeLine(Offset(center.x - armSizePx, center.y + armSizePx * 0.72f), center)
            strokeLine(center, Offset(center.x + armSizePx, center.y + armSizePx * 0.72f))
        }
        CrosshairShape.DIAMOND -> {
            val r = (armSizePx + gapPx * 0.45f).coerceAtLeast(5f)
            val p = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            if (outlinePx > 0.2f) drawPath(p, outlineCol, style = Stroke(thickPx + outlinePx * 2f))
            drawPath(p, mainColor, style = Stroke(thickPx))
        }
        CrosshairShape.TRI_STAR -> {
            val r0 = gapPx * 0.75f
            val r1 = gapPx + armSizePx
            for (angleDeg in listOf(-90.0, 30.0, 150.0)) {
                val rad = angleDeg * PI / 180.0
                val c = cos(rad).toFloat()
                val s = sin(rad).toFloat()
                strokeLine(
                    Offset(center.x + c * r0, center.y + s * r0),
                    Offset(center.x + c * r1, center.y + s * r1)
                )
            }
        }
        CrosshairShape.CORNER_BOX -> {
            val b = (gapPx + armSizePx * 0.75f).coerceAtLeast(5f)
            val leg = (armSizePx * 0.55f).coerceAtLeast(3f)
            // Top-left
            strokeLine(Offset(center.x - b, center.y - b), Offset(center.x - b + leg, center.y - b))
            strokeLine(Offset(center.x - b, center.y - b), Offset(center.x - b, center.y - b + leg))
            // Top-right
            strokeLine(Offset(center.x + b - leg, center.y - b), Offset(center.x + b, center.y - b))
            strokeLine(Offset(center.x + b, center.y - b), Offset(center.x + b, center.y - b + leg))
            // Bottom-left
            strokeLine(Offset(center.x - b, center.y + b - leg), Offset(center.x - b, center.y + b))
            strokeLine(Offset(center.x - b, center.y + b), Offset(center.x - b + leg, center.y + b))
            // Bottom-right
            strokeLine(Offset(center.x + b - leg, center.y + b), Offset(center.x + b, center.y + b))
            strokeLine(Offset(center.x + b, center.y + b - leg), Offset(center.x + b, center.y + b))
        }
        CrosshairShape.SNIPER_T -> {
            strokeLine(Offset(center.x - gapPx - armSizePx, center.y), Offset(center.x - gapPx, center.y))
            strokeLine(Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + armSizePx, center.y))
            strokeLine(Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + armSizePx * 1.15f))
            dot(center, thickPx * 0.9f)
        }
    }

    if (hitmarkerAlpha > 0.02f) {
        val hmCol = LiquidCrystalPalette.CrimsonGlow.copy(alpha = hitmarkerAlpha.coerceIn(0f, 1f))
        val r0 = gapPx + armSizePx * 0.55f
        val r1 = r0 + 7f
        drawLine(hmCol, Offset(center.x - r1, center.y - r1), Offset(center.x - r0, center.y - r0), 2f)
        drawLine(hmCol, Offset(center.x + r0, center.y - r0), Offset(center.x + r1, center.y - r1), 2f)
        drawLine(hmCol, Offset(center.x - r1, center.y + r1), Offset(center.x - r0, center.y + r0), 2f)
        drawLine(hmCol, Offset(center.x + r0, center.y + r0), Offset(center.x + r1, center.y + r1), 2f)
    }

    if (cooldownProgress != null && cooldownProgress < 0.995f) {
        val barW = 34f
        val barH = 3.5f
        val topY = center.y + gapPx + armSizePx + 10f
        drawRoundRect(
            color = Color(0xAA121317),
            topLeft = Offset(center.x - barW * 0.5f, topY),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = LiquidCrystalPalette.CrimsonPrimary,
            topLeft = Offset(center.x - barW * 0.5f, topY),
            size = Size(barW * cooldownProgress.coerceIn(0f, 1f), barH),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}
