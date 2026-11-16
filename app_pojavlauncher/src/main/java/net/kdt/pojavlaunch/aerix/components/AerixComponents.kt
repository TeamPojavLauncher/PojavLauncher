/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Visual building blocks reproduced from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/components/AerixComponents.kt
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/home/MiraiHomeDashboard.kt
 *     (QuickActionTile, HeroMetaPill, HomeSectionLabel, QuietEmptyState)
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/layouts/(all .kt files)
 * Zalith Launcher 2 Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
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

package net.kdt.pojavlaunch.aerix.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.aerix.theme.AerixMetrics
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import kotlin.math.roundToInt

/** Translucent glass card used for every Aerix panel. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = AerixRadii.panel,
    tint: Color = AerixSurface.glassTint,
    strength: Float = 0.9f,
    color: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = AerixSpacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    // Resolved inside the composable body so the "Layout transparency" setting is read while
    // composing, not captured once as a default argument.
    val panelColor = if (color == Color.Unspecified) AerixSurface.panel else color
    val shape = RoundedCornerShape(radius)
    val base = modifier
        .liquidGlass(shape = shape, tint = tint, strength = strength)
        .background(panelColor, shape)
    val clickable = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base
    Column(
        modifier = clickable.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
        content = content
    )
}

/** Accent pill button: the Aerix "PLAY NOW" / primary action style. */
@Composable
fun AerixPrimaryButton(
    text: String,
    @DrawableRes iconRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = AerixSurface.accent
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .liquidGlass(shape = CircleShape, tint = accent, strength = 0.92f, elevation = AerixMetrics.glassSelectedElevation),
        shape = CircleShape,
        color = if (enabled) accent else accent.copy(alpha = 0.35f),
        contentColor = AerixSurface.onAccent,
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AerixSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm, Alignment.CenterHorizontally)
        ) {
            if (iconRes != null) {
                Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(text = text, fontSize = 11.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

/** Glass outline button used for secondary actions. */
@Composable
fun AerixGhostButton(
    text: String,
    @DrawableRes iconRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AerixSurface.textPrimary
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .liquidGlass(shape = CircleShape, strength = 0.6f, elevation = AerixMetrics.glassSubtleElevation),
        shape = CircleShape,
        color = AerixSurface.panelRaised.copy(alpha = 0.27f),
        contentColor = tint,
        border = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AerixSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs, Alignment.CenterHorizontally)
        ) {
            if (iconRes != null) {
                Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Small translucent pill used for metadata (version id, loader, counts). */
@Composable
fun MetaPill(text: String, @DrawableRes iconRes: Int? = null, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = AerixSurface.panelRaised.copy(alpha = 0.27f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AerixSpacing.smPlus, vertical = AerixSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
        ) {
            if (iconRes != null) Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(13.dp))
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Uppercase section label with optional trailing action, as used across the Aerix dashboard. */
@Composable
fun SectionLabel(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title.uppercase(),
            color = AerixSurface.textSecondary,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Text(
                text = action.uppercase(),
                color = AerixSurface.accent,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = AerixSpacing.sm, vertical = AerixSpacing.xs)
            )
        }
    }
}

/** Page header: title and subtitle with optional trailing content. */
@Composable
fun PageHeader(title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = AerixSurface.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            if (subtitle != null) {
                Text(text = subtitle, color = AerixSurface.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Dashboard tile with a tinted icon well, title and subtitle. */
@Composable
fun QuickActionTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.cardLarge)
    Row(
        modifier = modifier
            .liquidGlass(shape = shape, tint = accent, strength = 0.7f)
            .background(AerixSurface.panel, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.smPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(AerixRadii.control))
                .background(accent.copy(alpha = 0.18f))
                .border(BorderStroke(AerixSpacing.hairline, accent.copy(alpha = 0.45f)), RoundedCornerShape(AerixRadii.control)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = AerixSurface.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, color = AerixSurface.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Empty state shown when a list has nothing to display. */
@Composable
fun QuietEmptyState(@DrawableRes iconRes: Int, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AerixSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = AerixSurface.textMuted, modifier = Modifier.size(32.dp))
        Text(text = text, color = AerixSurface.textMuted, fontSize = 12.sp)
    }
}

/** Selectable pill tabs used by Discover and Settings. */
@Composable
fun PillTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .liquidGlass(shape = CircleShape, strength = 0.6f)
            .background(AerixSurface.panel, CircleShape)
            .padding(AerixSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .height(AerixMetrics.pillTabHeight - AerixSpacing.sm)
                    .clip(CircleShape)
                    .background(if (isSelected) AerixSurface.accent else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = AerixSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) AerixSurface.onAccent else AerixSurface.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/** Settings row container shared by switch / slider / text / action rows. */
@Composable
private fun SettingRowFrame(
    title: String,
    summary: String?,
    @DrawableRes iconRes: Int?,
    onClick: (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    below: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(AerixRadii.cardSmall)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(AerixSurface.panelRaised.copy(alpha = 0.21f), shape)
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.smPlus)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(painter = painterResource(iconRes), contentDescription = null, tint = AerixSurface.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(AerixSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = AerixSurface.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (!summary.isNullOrEmpty()) {
                    Text(text = summary, color = AerixSurface.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(AerixSpacing.sm))
                trailing()
            }
        }
        if (below != null) below()
    }
}

@Composable
fun SwitchSettingRow(title: String, summary: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, @DrawableRes iconRes: Int? = null) {
    SettingRowFrame(
        title = title,
        summary = summary,
        iconRes = iconRes,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AerixSurface.onAccent,
                    checkedTrackColor = AerixSurface.accent,
                    uncheckedThumbColor = AerixSurface.textSecondary,
                    uncheckedTrackColor = AerixSurface.panelTrack,
                    uncheckedBorderColor = AerixSurface.borderSoft
                )
            )
        }
    )
}

@Composable
fun SliderSettingRow(
    title: String,
    summary: String?,
    value: Int,
    range: IntRange,
    step: Int,
    suffix: String,
    onValueChange: (Int) -> Unit,
    @DrawableRes iconRes: Int? = null
) {
    var dragValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val steps = if (step > 0) ((range.last - range.first) / step - 1).coerceAtLeast(0) else 0
    SettingRowFrame(
        title = title,
        summary = summary,
        iconRes = iconRes,
        onClick = null,
        trailing = {
            Text(text = "${dragValue.roundToInt()}$suffix", color = AerixSurface.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        },
        below = {
            Slider(
                value = dragValue.coerceIn(range.first.toFloat(), range.last.toFloat()),
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    val snapped = if (step > 0) {
                        (range.first + ((dragValue - range.first) / step).roundToInt() * step).coerceIn(range.first, range.last)
                    } else dragValue.roundToInt()
                    dragValue = snapped.toFloat()
                    onValueChange(snapped)
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = if (steps in 1..200) steps else 0,
                colors = SliderDefaults.colors(
                    thumbColor = AerixSurface.accent,
                    activeTrackColor = AerixSurface.accent,
                    inactiveTrackColor = AerixSurface.panelTrack
                )
            )
        }
    )
}

@Composable
fun TextSettingRow(title: String, summary: String?, value: String, onValueChange: (String) -> Unit, @DrawableRes iconRes: Int? = null) {
    var text by remember(value) { mutableStateOf(value) }
    SettingRowFrame(
        title = title,
        summary = summary,
        iconRes = iconRes,
        onClick = null,
        trailing = null,
        below = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AerixSpacing.xs),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AerixSurface.accent,
                    unfocusedBorderColor = AerixSurface.borderSoft,
                    focusedTextColor = AerixSurface.textPrimary,
                    unfocusedTextColor = AerixSurface.textPrimary,
                    cursorColor = AerixSurface.accent
                ),
                trailingIcon = {
                    if (text != value) {
                        Text(
                            text = "SAVE",
                            color = AerixSurface.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onValueChange(text) }
                                .padding(AerixSpacing.sm)
                        )
                    }
                }
            )
        }
    )
}

@Composable
fun ActionSettingRow(title: String, summary: String?, onClick: () -> Unit, @DrawableRes iconRes: Int? = null, value: String? = null) {
    SettingRowFrame(
        title = title,
        summary = summary,
        iconRes = iconRes,
        onClick = onClick,
        trailing = {
            Text(text = value ?: "›", color = AerixSurface.accent, fontSize = if (value == null) 20.sp else 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    )
}

/** Converts any Android drawable (instance icons, skin faces) into an ImageBitmap for Compose. */
fun Drawable.toAerixBitmap(sizePx: Int = 128): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap.asImageBitmap()
    // Loader marks are vector drawables with a 24dp intrinsic size; rendered at that they turn to
    // mush on a card, so they are drawn at the requested size instead.
    val w = if (intrinsicWidth > sizePx) intrinsicWidth else sizePx
    val h = if (intrinsicHeight > sizePx) intrinsicHeight else sizePx
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bitmap.asImageBitmap()
}

/** Rounded image well used for instance icons and player faces. */
@Composable
fun AerixImageWell(image: ImageBitmap?, @DrawableRes fallback: Int, size: Dp, modifier: Modifier = Modifier, radius: Dp = AerixRadii.control) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(AerixSurface.panelRaised)
            .border(BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft), shape),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(painter = painterResource(fallback), contentDescription = null, tint = AerixSurface.textSecondary, modifier = Modifier.size(size * 0.55f))
        }
    }
}
