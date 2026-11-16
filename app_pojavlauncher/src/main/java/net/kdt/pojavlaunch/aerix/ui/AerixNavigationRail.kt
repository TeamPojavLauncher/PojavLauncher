/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Ported from mirai-launcher- (Zalith Launcher 2 fork):
 *   MiraiLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/main/MiraiNavigationRail.kt
 *     (LauncherSection, MiraiNavigationRail, RailIconItem, AccountAvatarRailButton)
 * Account state comes from PojavAccountBridge instead of AccountsManager.currentAccountFlow.
 * Zalith Launcher 2 Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 * Copyright (C) 2026 Aerix Launcher contributors.
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

package net.kdt.pojavlaunch.aerix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixAccount
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.components.bringFocusedItemIntoView
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.theme.AERIX_COMPACT_HEIGHT_THRESHOLD_DP
import net.kdt.pojavlaunch.aerix.theme.AerixMetrics
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.guide.AerixGuideState
import net.kdt.pojavlaunch.aerix.guide.guideAnchor
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.ThemeMotion
import net.kdt.pojavlaunch.aerix.theme.accentGlow
import net.kdt.pojavlaunch.aerix.theme.accentRing
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

/** Primary destinations kept visible while the Aerix launcher is open. */
enum class LauncherSection {
    HOME,
    /** Dedicated Aerix Client (Minecraft 1.16.5 Testing Edition · Liquid Crystal Clear). */
    AERIX_CLIENT,
    LIBRARY,
    SERVERS,
    /** Friends, messages and parties - the social launcher, opened from Home and the rail. */
    SOCIALS,
    DISCOVER,
    OTHER_RELEASES,
    DOWNLOADS,
    CONTENT,
    FILES,
    LOGS,
    SETTINGS,
    /** The team page, between Settings and the account entry. */
    CONTRIBUTION,
    /** Team Aerix modpacks, one press to install. Replaces the old Wallpapers rail entry. */
    MODPACKS,
    ACCOUNTS,
    ABOUT,
    /** Pages opened on top of a section (Mirai "Create New Instance", instance settings, account sign-in). */
    CREATE_INSTANCE,
    INSTANCE_EDITOR,
    /** Home utility destinations; Back returns to the dashboard. */
    QUICK_TOOLS,
    INSTANCE_SELECTOR,
    ADD_ACCOUNT,
    OFFLINE_ACCOUNT,
    AERIX_ACCOUNT,
    /** One team member's page, opened from Contribution. */
    CONTRIBUTION_DETAIL
}

@Composable
fun AerixNavigationRail(
    selectedSection: LauncherSection,
    onNavigate: (LauncherSection) -> Unit,
    onCreateInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = PojavAccountBridge.current
    val scrollState = rememberScrollState()
    val activeAccent = AerixThemeManager.currentAccent()
    val configuration = LocalConfiguration.current
    // Short screens (landscape phones): tighten the rail so every entry stays visible.
    val compactRail = configuration.screenHeightDp < AERIX_COMPACT_HEIGHT_THRESHOLD_DP
    val expandedRail = configuration.screenWidthDp >= 900 && !compactRail

    Row(modifier = modifier.fillMaxHeight()) {
        Surface(
            modifier = Modifier
                .width(if (expandedRail) AerixMetrics.expandedNavigationRailWidth else AerixMetrics.navigationRailWidth)
                .fillMaxHeight()
                // The pill sits against the left edge - only the end side keeps a gap, so the
                // pages own the rest of the width.
                .padding(start = 2.dp, end = AerixSpacing.xs, top = AerixSpacing.smPlus, bottom = AerixSpacing.smPlus)
                .liquidGlass(shape = CircleShape, tint = AerixSurface.glassTint, strength = 0.94f, elevation = AerixMetrics.glassRailElevation),
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(vertical = if (compactRail) AerixSpacing.xs else AerixSpacing.smPlus),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compactRail) AerixSpacing.xs else AerixSpacing.sm)
            ) {
                // Aerix monogram and wordmark establish a persistent brand anchor.
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AerixRadii.panelSmall))
                        .bringFocusedItemIntoView()
                        .clickable(role = Role.Button) { onNavigate(LauncherSection.HOME) }
                        .semantics { contentDescription = "Aerix Home" }
                        .padding(vertical = AerixSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AerixSpacing.xxs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compactRail) 32.dp else 38.dp)
                            .clip(RoundedCornerShape(AerixRadii.control))
                            .background(activeAccent.copy(alpha = 0.12f))
                            .liquidGlass(
                                shape = RoundedCornerShape(AerixRadii.control),
                                tint = activeAccent,
                                strength = 0.82f,
                                elevation = AerixMetrics.glassSubtleElevation
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_aerix_mark),
                            contentDescription = "Aerix",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(if (compactRail) 20.dp else 24.dp)
                        )
                    }
                    if (expandedRail) {
                        Text(text = "AERIX", fontSize = 8.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.ExtraBold, color = AerixSurface.textSecondary)
                    }
                }

                Spacer(Modifier.height(if (compactRail) AerixSpacing.zero else AerixSpacing.xs))

                RailIconItem(R.drawable.aerix_ic_home_filled, stringResource(R.string.aerix_nav_home),
                    selectedSection == LauncherSection.HOME, activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.HOME) }
                RailIconItem(R.drawable.aerix_ic_client_crystal, "Aerix Client",
                    selectedSection == LauncherSection.AERIX_CLIENT, activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.AERIX_CLIENT) }
                RailIconItem(R.drawable.aerix_ic_add, stringResource(R.string.create_instance),
                    selectedSection == LauncherSection.CREATE_INSTANCE, activeAccent, expandedRail, compactRail,
                    onClick = onCreateInstance, guideKey = AerixGuideState.CREATE)
                RailIconItem(R.drawable.aerix_ic_dashboard_filled, stringResource(R.string.aerix_nav_library),
                    librarySelected(selectedSection), activeAccent, expandedRail, compactRail,
                    onClick = { onNavigate(LauncherSection.LIBRARY) }, guideKey = AerixGuideState.LIBRARY)
                RailIconItem(R.drawable.aerix_ic_sports_esports_outlined, "Servers",
                    selectedSection == LauncherSection.SERVERS, activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.SERVERS) }
                // Socials is not a rail entry: it opens from the Home dashboard and brings its own
                // sidebar, so it is not listed next to the launcher's own sections.
                RailIconItem(R.drawable.aerix_ic_public, stringResource(R.string.aerix_nav_discover),
                    discoverSelected(selectedSection), activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.DISCOVER) }
                RailIconItem(R.drawable.aerix_ic_rocket_launch_filled, "Our other new releases",
                    selectedSection == LauncherSection.OTHER_RELEASES, activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.OTHER_RELEASES) }
                RailIconItem(R.drawable.aerix_ic_archive_filled, "Our modpacks",
                    selectedSection == LauncherSection.MODPACKS, activeAccent, expandedRail, compactRail,
                    guideKey = AerixGuideState.MODPACKS) { onNavigate(LauncherSection.MODPACKS) }

                HorizontalDivider(modifier = Modifier.width(28.dp).padding(vertical = AerixSpacing.xxs), color = AerixSurface.borderSoft)

                // On Home these two utility destinations move to the right rail. The team entry
                // remains here, so it is still reachable from every screen without duplicating the
                // profile shortcut.
                if (selectedSection != LauncherSection.HOME) {
                    RailIconItem(R.drawable.aerix_ic_settings_filled, stringResource(R.string.aerix_nav_settings),
                        settingsSelected(selectedSection), activeAccent, expandedRail, compactRail,
                        onClick = { onNavigate(LauncherSection.SETTINGS) }, guideKey = AerixGuideState.SETTINGS)
                }

                RailIconItem(R.drawable.aerix_ic_group_outlined, stringResource(R.string.aerix_nav_contribution),
                    selectedSection == LauncherSection.CONTRIBUTION || selectedSection == LauncherSection.CONTRIBUTION_DETAIL,
                    activeAccent, expandedRail, compactRail) { onNavigate(LauncherSection.CONTRIBUTION) }

                if (selectedSection != LauncherSection.HOME) {
                    AccountAvatarRailButton(
                        account = account,
                        selected = selectedSection == LauncherSection.ACCOUNTS || selectedSection == LauncherSection.ADD_ACCOUNT || selectedSection == LauncherSection.OFFLINE_ACCOUNT,
                        expanded = expandedRail,
                        compact = compactRail,
                        onClick = { onNavigate(LauncherSection.ACCOUNTS) }
                    )
                }
            }
        }
    }
}

/** Utility rail shown beside the centered character on Home. The first three match the
 * settings, profile and avatar destinations called out in the dashboard reference. */
@Composable
fun AerixHomeUtilityRail(
    selectedSection: LauncherSection,
    onNavigate: (LauncherSection) -> Unit,
    onOpenProfile: () -> Unit,
    onQuickTools: () -> Unit,
    onInstances: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = PojavAccountBridge.current
    val activeAccent = AerixThemeManager.currentAccent()
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val compactRail = configuration.screenHeightDp < AERIX_COMPACT_HEIGHT_THRESHOLD_DP
    val expandedRail = configuration.screenWidthDp >= 900 && !compactRail

    Surface(
        modifier = modifier
            .width(if (expandedRail) AerixMetrics.expandedNavigationRailWidth else AerixMetrics.navigationRailWidth)
            .fillMaxHeight()
            .padding(start = AerixSpacing.xs, end = 2.dp, top = AerixSpacing.smPlus, bottom = AerixSpacing.smPlus)
            .liquidGlass(shape = CircleShape, tint = AerixSurface.glassTint, strength = 0.94f, elevation = AerixMetrics.glassRailElevation),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(vertical = if (compactRail) AerixSpacing.xs else AerixSpacing.smPlus),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactRail) AerixSpacing.xs else AerixSpacing.sm)
        ) {
            RailIconItem(
                R.drawable.aerix_ic_settings_filled,
                stringResource(R.string.aerix_nav_settings),
                settingsSelected(selectedSection), activeAccent, expandedRail, compactRail,
                guideKey = AerixGuideState.SETTINGS
            ) { onNavigate(LauncherSection.SETTINGS) }
            RailIconItem(
                R.drawable.aerix_ic_person_outlined,
                "Profile",
                selectedSection == LauncherSection.SOCIALS,
                activeAccent, expandedRail, compactRail
            ) { onOpenProfile() }
            AccountAvatarRailButton(
                account = account,
                selected = selectedSection == LauncherSection.ACCOUNTS ||
                    selectedSection == LauncherSection.ADD_ACCOUNT ||
                    selectedSection == LauncherSection.OFFLINE_ACCOUNT,
                expanded = expandedRail,
                compact = compactRail,
                onClick = { onNavigate(LauncherSection.ACCOUNTS) }
            )

            HorizontalDivider(
                modifier = Modifier.width(28.dp).padding(vertical = AerixSpacing.xxs),
                color = AerixSurface.borderSoft
            )

            RailIconItem(
                R.drawable.aerix_ic_build_outlined,
                "Quick Tools",
                selectedSection == LauncherSection.QUICK_TOOLS,
                activeAccent, expandedRail, compactRail
            ) { onQuickTools() }
            RailIconItem(
                R.drawable.aerix_ic_videogame_asset_outlined,
                "Instances",
                selectedSection == LauncherSection.INSTANCE_SELECTOR,
                activeAccent, expandedRail, compactRail
            ) { onInstances() }
            RailIconItem(
                R.drawable.aerix_ic_dashboard_filled,
                stringResource(R.string.aerix_nav_library),
                librarySelected(selectedSection), activeAccent, expandedRail, compactRail
            ) { onNavigate(LauncherSection.LIBRARY) }
            RailIconItem(
                R.drawable.aerix_ic_sports_esports_outlined,
                "Servers",
                selectedSection == LauncherSection.SERVERS,
                activeAccent, expandedRail, compactRail
            ) { onNavigate(LauncherSection.SERVERS) }
            RailIconItem(
                R.drawable.aerix_ic_public,
                stringResource(R.string.aerix_nav_discover),
                discoverSelected(selectedSection), activeAccent, expandedRail, compactRail
            ) { onNavigate(LauncherSection.DISCOVER) }
            RailIconItem(
                R.drawable.aerix_ic_folder_outlined,
                "Files",
                selectedSection == LauncherSection.FILES,
                activeAccent, expandedRail, compactRail
            ) { onNavigate(LauncherSection.FILES) }
            RailIconItem(
                R.drawable.aerix_ic_description_outlined,
                "Logs",
                settingsSelected(selectedSection), activeAccent, expandedRail, compactRail
            ) { onNavigate(LauncherSection.LOGS) }
        }
    }
}

private fun librarySelected(section: LauncherSection): Boolean =
    section == LauncherSection.LIBRARY || section == LauncherSection.INSTANCE_EDITOR || section == LauncherSection.CONTENT

private fun discoverSelected(section: LauncherSection): Boolean =
    section == LauncherSection.DISCOVER || section == LauncherSection.DOWNLOADS

/**
 * Settings is one destination with several tabs, so its rail entry stays highlighted on every tab.
 * The former Wallpapers entry had to share the highlight with it; that entry is gone, so the
 * pairing is now simply "Settings is selected whenever Settings is open".
 */
private fun settingsSelected(section: LauncherSection): Boolean =
    section == LauncherSection.SETTINGS || section == LauncherSection.ABOUT || section == LauncherSection.LOGS

@Composable
private fun RailIconItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    accentColor: Color,
    expanded: Boolean,
    compact: Boolean,
    /** Names this item for the first-run guide, so the tour can point at it. */
    guideKey: String? = null,
    onClick: () -> Unit
) {
    // The selected item uses the current theme accent; icon, fill and border update together.
    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
    // With the animated theme on, the selected item's accent breathes and carries a glow. Off, it
    // is the identical flat accent as before - and no animation is started for unselected items.
    val motionOn = ThemeMotion.rememberEnabled()
    val liveAccent = ThemeMotion.rememberPulse(accentColor, selected, motionOn)
    val containerColor by animateColorAsState(
        targetValue = if (selected) liveAccent else AerixSurface.panel,
        animationSpec = tween(140),
        label = "railContainerColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) selectedContentColor else AerixSurface.textSecondary,
        animationSpec = tween(140),
        label = "railIconTint"
    )
    val itemWidth = when {
        expanded -> 64.dp
        compact -> 36.dp
        else -> 44.dp
    }
    val itemHeight = when {
        expanded -> 54.dp
        compact -> 36.dp
        else -> 42.dp
    }
    val itemShape = RoundedCornerShape(AerixRadii.control)
    Box(
        modifier = Modifier
            .then(if (guideKey != null) Modifier.guideAnchor(guideKey) else Modifier)
            .size(width = itemWidth, height = itemHeight)
            .clip(itemShape)
            .background(containerColor)
            .accentGlow(liveAccent, selected, motionOn)
            .accentRing(liveAccent, itemShape, selected, motionOn, AerixSpacing.hairline)
            .border(BorderStroke(AerixSpacing.hairline, if (selected) liveAccent else AerixSurface.borderSoft), itemShape)
            .bringFocusedItemIntoView()
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (expanded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(painter = painterResource(iconRes), contentDescription = label, tint = iconTint, modifier = Modifier.size(19.dp))
                Text(
                    text = label,
                    color = if (selected) iconTint else AerixSurface.textSecondary,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        } else {
            Icon(painter = painterResource(iconRes), contentDescription = label, tint = iconTint, modifier = Modifier.size(if (compact) 18.dp else 21.dp))
        }
    }
}

@Composable
private fun AccountAvatarRailButton(
    account: AerixAccount?,
    selected: Boolean,
    expanded: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val activeAccent = AerixThemeManager.currentAccent()
    val ringColor by animateColorAsState(
        targetValue = if (selected) activeAccent else AerixSurface.borderSoft,
        animationSpec = tween(150),
        label = "accountAvatarRing"
    )
    val accountsLabel = stringResource(R.string.aerix_nav_accounts)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(AerixRadii.panelSmall))
            .bringFocusedItemIntoView()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = account?.username ?: accountsLabel }
            .padding(AerixSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 32.dp else 36.dp)
                .clip(CircleShape)
                .background(AerixSurface.panel)
                .liquidGlass(shape = CircleShape, tint = activeAccent, strength = if (selected) 0.82f else 0.34f, elevation = AerixMetrics.glassSelectedElevation)
                .border(width = if (selected) 2.dp else AerixSpacing.hairline, color = ringColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AerixImageWell(
                image = account?.face,
                fallback = R.drawable.aerix_head_steve,
                size = if (compact) 26.dp else 30.dp,
                radius = 15.dp
            )
        }
        if (expanded) {
            Text(
                text = accountsLabel,
                color = if (selected) activeAccent else AerixSurface.textSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
