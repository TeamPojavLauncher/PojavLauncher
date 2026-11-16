/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/theme/Theme.kt (aerixDark scheme)
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/theme/Type.kt
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

package net.kdt.pojavlaunch.aerix.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.kdt.pojavlaunch.aerix.bridge.AerixColorTheme
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences

private val defaults = Typography()

private fun TextStyle.aerixText(family: FontFamily, weight: FontWeight = fontWeight ?: FontWeight.Normal) = copy(
    fontFamily = family,
    fontWeight = weight
)

private fun aerixTypography(family: FontFamily) = Typography(
    displayLarge = defaults.displayLarge.aerixText(family, FontWeight.Bold),
    displayMedium = defaults.displayMedium.aerixText(family, FontWeight.Bold),
    displaySmall = defaults.displaySmall.aerixText(family, FontWeight.Bold),
    headlineLarge = defaults.headlineLarge.aerixText(family, FontWeight.Bold),
    headlineMedium = defaults.headlineMedium.aerixText(family, FontWeight.SemiBold),
    headlineSmall = defaults.headlineSmall.aerixText(family, FontWeight.SemiBold),
    titleLarge = defaults.titleLarge.aerixText(family, FontWeight.SemiBold),
    titleMedium = defaults.titleMedium.aerixText(family, FontWeight.Medium),
    titleSmall = defaults.titleSmall.aerixText(family, FontWeight.Medium),
    bodyLarge = defaults.bodyLarge.aerixText(family),
    bodyMedium = defaults.bodyMedium.aerixText(family),
    bodySmall = defaults.bodySmall.aerixText(family),
    labelLarge = defaults.labelLarge.aerixText(family, FontWeight.Medium),
    labelMedium = defaults.labelMedium.aerixText(family, FontWeight.Medium),
    labelSmall = defaults.labelSmall.aerixText(family, FontWeight.Medium),
)

/**
 * Built as a function, not a val: its surface roles come from `AerixSurface.panel*`, which follow
 * the "Layout transparency" setting, so the scheme has to be assembled on every pass rather than
 * baked once into a top-level property.
 */
private fun aerixDark(): ColorScheme = darkColorScheme(
    primary = AerixSurface.accentDefault,
    onPrimary = AerixSurface.onAccentDefault,
    primaryContainer = AerixSurface.accentContainerDefault,
    onPrimaryContainer = AerixSurface.onAccentContainer,
    secondary = AerixSurface.accentSecondary,
    onSecondary = AerixSurface.canvas,
    secondaryContainer = AerixSurface.panelRaised,
    onSecondaryContainer = AerixSurface.textPrimary,
    tertiary = AerixSurface.success,
    onTertiary = AerixSurface.canvas,
    tertiaryContainer = AerixSurface.panelRaised,
    onTertiaryContainer = AerixSurface.textPrimary,
    error = AerixSurface.danger,
    onError = AerixSurface.canvas,
    errorContainer = AerixSurface.errorDarkContainer,
    onErrorContainer = AerixSurface.textPrimary,
    background = AerixSurface.canvas,
    onBackground = AerixSurface.textPrimary,
    surface = AerixSurface.panel,
    onSurface = AerixSurface.textPrimary,
    surfaceVariant = AerixSurface.panelRaised,
    onSurfaceVariant = AerixSurface.textSecondary,
    outline = AerixSurface.textMuted,
    outlineVariant = AerixSurface.border,
    scrim = Color.Black,
    inverseSurface = AerixSurface.lightTextPrimary,
    inverseOnSurface = AerixSurface.lightPanel,
    inversePrimary = AerixSurface.accentLight,
    surfaceDim = AerixSurface.canvas,
    surfaceBright = AerixSurface.panelRaised,
    surfaceContainerLowest = AerixSurface.canvas,
    surfaceContainerLow = AerixSurface.panelTrack,
    surfaceContainer = AerixSurface.canvas,
    surfaceContainerHigh = AerixSurface.panel,
    surfaceContainerHighest = AerixSurface.panelRaised
)

/**
 * The CUSTOM theme in Zalith builds a full tonal palette through MaterialKolor. The port keeps the
 * Aerix glass surfaces and swaps only the accent roles, which is what the Aerix shell renders.
 */
private fun customAccentScheme(accent: Color): ColorScheme {
    val onAccent = if (accent.luminance() > 0.45f) AerixSurface.onAccentDefault else Color.White
    return aerixDark().copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.33f),
        onPrimaryContainer = AerixSurface.textPrimary,
        inversePrimary = accent
    )
}

@Composable
fun AerixLauncherTheme(content: @Composable () -> Unit) {
    val theme = AerixPreferences.colorTheme
    val custom = AerixPreferences.customColor
    // aerixDark() reads AerixPreferences.layoutTransparency, so Material's surface roles follow the
    // "Layout transparency" slider the same way the Aerix cards do.
    val scheme = when (theme) {
        AerixColorTheme.AERIX -> aerixDark()
        AerixColorTheme.CUSTOM -> customAccentScheme(Color(custom))
    }
    AerixFonts.initialize(LocalContext.current)
    val fontId = AerixFonts.selectedId
    val typography = remember(fontId) { aerixTypography(AerixFonts.selected().family()) }
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        content = content
    )
}
