/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The warning every renderer picker shows before MobileGlues is chosen.
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

package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.kdt.pojavlaunch.aerix.bridge.AerixColorTheme
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.game.renderer.MobileGluesWarning

/**
 * Shown the moment somebody picks MobileGlues, before the choice is written.
 *
 * Three ways out, in the order a player needs them: keep MobileGlues because they meant it, take the
 * renderer recommended for this version instead, or go back without changing anything. The warning
 * is a warning, not a refusal - the choice stays theirs, it just stops being a surprise.
 *
 * @param versionId the version the picker belongs to, used to name the recommended alternative
 * @param onConfirm the player kept MobileGlues; write the choice here
 * @param onRecommended the player took the alternative; [MobileGluesWarning.recommendedInstead]
 * names the renderer to store
 * @param onDismiss the player backed out; nothing is written
 */
@Composable
fun MobileGluesWarningDialog(
    versionId: String?,
    onConfirm: () -> Unit,
    onRecommended: (recommendedTag: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Resolved once, here: the label and the button that writes the value are the same decision.
    val recommendedTag = MobileGluesWarning.recommendedInstead(versionId.orEmpty())
    // Read the same way AerixThemeManager.currentAccent() does, without a composable call, because
    // this file only needs the colour for two buttons.
    val accent = if (AerixPreferences.colorTheme == AerixColorTheme.CUSTOM) {
        Color(AerixPreferences.customColor)
    } else {
        MaterialTheme.colorScheme.primary
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AerixSurface.panelRaised,
        title = { Text(MobileGluesWarning.title(), color = AerixSurface.textPrimary) },
        text = {
            Text(
                text = MobileGluesWarning.message(),
                color = AerixSurface.textSecondary,
                // The message is long on purpose: a warning nobody reads is not a warning. It
                // scrolls rather than being trimmed, and the buttons stay on screen.
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = MobileGluesWarning.confirmLabel(),
                    color = accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onRecommended(recommendedTag) }) {
                Text(
                    text = MobileGluesWarning.recommendedLabel(recommendedTag),
                    color = accent
                )
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = AerixSurface.textSecondary)
            }
        }
    )
}
