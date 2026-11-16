/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/theme/AerixSurface.kt
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences

object AerixSurface {
    val canvas = Color(0xFF07131D)

    /**
     * Scales the alpha of a surface colour by the "Layout transparency" setting.
     *
     * The setting is a percentage of the card colour's own alpha, not of full opacity: at 100 the
     * colours below are exactly as designed, and lowering it only ever makes a surface more
     * see-through, so a card can never end up more solid than the theme intends.
     *
     * Reading the preference here - rather than at each call site - is what makes the slider apply
     * to every card, pill and panel at once: they are all built from the colours below.
     */
    private fun glass(base: Color): Color {
        val percent = AerixPreferences.layoutTransparency
        if (percent >= 100) return base
        return base.copy(alpha = (base.alpha * percent / 100f).coerceIn(0f, 1f))
    }

    // Neutral smoky glass lets the chosen wallpaper show through, without a blue colour cast.
    // Plain getters, not @Composable ones, so they stay readable outside composition too. They still
    // pick up the slider live: [glass] reads a snapshot-backed preference, and any read of that
    // during composition subscribes the caller to its changes.
    val panel: Color get() = glass(Color(0x66343839))
    val panelRaised: Color get() = glass(Color(0x80515858))
    val panelTrack: Color get() = glass(Color(0x604A5050))
    val panelGlassTint: Color get() = glass(Color(0x3D596362))
    val border = Color(0x78F2F7F5)
    val borderSoft = Color(0x48F2F7F5)
    val borderHighlight = Color(0xDDF5FFFA)
    val glassTint = Color(0xFF7A8582)
    val glassBlue = Color(0xFF91E9FF)
    val glassViolet = Color(0xFFB7A9FF)
    val glassRose = Color(0xFFF3A9D9)
    val auroraCyan = Color(0xFF37DCEC)
    val auroraViolet = Color(0xFF7A71F5)
    val glassShadow = Color(0x66000610)

    /** Static fallback used to define the built-in Aerix palette and wallpaper presets. */
    val accentDefault = Color(0xFF8DEFE0)

    /** Current launcher accent; UI controls use this instead of a baked-in color. */
    val accent: Color
        @Composable get() = AerixThemeManager.currentAccent()
    val action: Color
        @Composable get() = accent
    val onAction: Color
        @Composable get() = onAccent
    val accentSecondary = Color(0xFFC1B5FF)
    val accentGlow = Color(0x668DEFE0)
    val accentContainerDefault = Color(0x5539C8C1)
    val accentContainer: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.33f)
    val onAccentDefault = Color(0xFF06201D)
    val onAccent: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
    val onAccentContainer = Color(0xFFBDFAF3)
    val accentLight = Color(0xFF006D73)
    val lightPanel = Color(0xFFFCFEFF)
    val lightTextPrimary = Color(0xFF14212B)
    val errorDarkContainer = Color(0xFF7A2635)
    val dangerContainer = Color(0xFF3B1A1E)
    val warningContainer = Color(0xFF3A2E16)
    val modrinthBrand = Color(0xFF1BD96A)
    val curseForgeBrand = Color(0xFFF16436)
    val shadowSoft = Color(0x3A000000)
    val scrimSoft = Color(0x33000000)
    val textPrimary = Color(0xFFF1F6FA)
    val textSecondary = Color(0xFFC8D1CF)
    val textMuted = Color(0xFF7D909D)
    val success = Color(0xFF73E5B0)
    val warning = Color(0xFFFFC777)
    val danger = Color(0xFFFF7189)
    val glassShine = Color(0x48FFFFFF)
    /** Log box surfaces and syntax tokens, values from mirai-launcher- ui/theme/AerixSurface.kt. */
    val logInfoSurface = Color(0xFF447152)
    val logDebugSurface = Color(0xFF43698D)
    val logWarningSurface = Color(0xFF656E76)
    val syntaxMuted = Color(0xFF6E7C83)
    val syntaxString = Color(0xFF6AAB73)
    val syntaxAccent = Color(0xFFC67CBA)
}

const val AERIX_COMPACT_HEIGHT_THRESHOLD_DP = 520

object AerixSpacing {
    val zero: Dp = 0.dp
    val hairline: Dp = 1.dp
    val xxs: Dp = 2.dp
    val tiny: Dp = 3.dp
    val xs: Dp = 4.dp
    val xsPlus: Dp = 5.dp
    val smCompact: Dp = 6.dp
    val smTight: Dp = 7.dp
    val sm: Dp = 8.dp
    val smPlus: Dp = 10.dp
    val smNarrow: Dp = 9.dp
    val mdTight: Dp = 11.dp
    val md: Dp = 12.dp
    val mdPlus: Dp = 14.dp
    val lg: Dp = 16.dp
    val lgPlus: Dp = 18.dp
    val xl: Dp = 20.dp
    val xlPlus: Dp = 22.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val section: Dp = 40.dp
}

object AerixLayoutDefaults {
    /** Page padding. Short screens (landscape phones) tighten automatically. */
    @Composable
    fun pagePadding(
        horizontal: Dp = AerixSpacing.lg,
        vertical: Dp = AerixSpacing.lg
    ): PaddingValues {
        val compact = LocalConfiguration.current.screenHeightDp < AERIX_COMPACT_HEIGHT_THRESHOLD_DP
        return PaddingValues(
            horizontal = if (compact) AerixSpacing.md else horizontal,
            vertical = if (compact) AerixSpacing.sm else vertical
        )
    }
}

object AerixRadii {
    val square: Dp = 0.dp
    val tiny: Dp = 4.dp
    val micro: Dp = 6.dp
    val compact: Dp = 8.dp
    val controlSmall: Dp = 10.dp
    val control: Dp = 12.dp
    val cardSmall: Dp = 16.dp
    val card: Dp = 18.dp
    val cardLarge: Dp = 20.dp
    val panelSmall: Dp = 22.dp
    val panel: Dp = 24.dp
    val dialog: Dp = 28.dp
    val hero: Dp = 32.dp
    val pill: Dp = 50.dp
}

object AerixMetrics {
    val pillTabHeight: Dp = 38.dp
    val pageHeaderHeight: Dp = 44.dp
    val navigationRailWidth: Dp = 62.dp
    val expandedNavigationRailWidth: Dp = 72.dp
    val glassSurfaceElevation: Dp = 1.dp
    val glassFloatingElevation: Dp = 3.dp
    val glassRailElevation: Dp = 2.dp
    val glassDialogElevation: Dp = 3.dp
    val glassSelectedElevation: Dp = 0.dp
    val glassSubtleElevation: Dp = 0.dp
}
