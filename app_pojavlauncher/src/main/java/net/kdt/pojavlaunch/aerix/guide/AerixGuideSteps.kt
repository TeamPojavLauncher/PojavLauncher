/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The steps of the first-run guide, and the state that shows it once.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences

/**
 * Whether the tour is on screen.
 *
 * It is held out here rather than inside a composable so that switching sections, or the theme
 * rebuilding the shell when a wallpaper is applied, cannot dismiss it half way through.
 */
object AerixGuideState {
    /** Names the components the tour highlights; [guideAnchor] publishes into it. */
    const val CREATE = "create"
    const val LIBRARY = "library"
    const val PLAY = "play"
    /** The "Our modpacks" rail entry. */
    const val MODPACKS = "modpacks"
    const val SETTINGS = "settings"

    var running by mutableStateOf(false)
        private set
    /** Bumped by "Show the guide again" so a running tour can be restarted from the first step. */
    var runId by mutableStateOf(0)
        private set

    /** First launch: the tour has never been completed or skipped. */
    fun startOnFirstRun() {
        if (!AerixPreferences.guidePlayed) {
            running = true
            runId++
        }
    }

    fun replay() {
        running = true
        runId++
    }

    fun finish() {
        running = false
        AerixPreferences.saveGuidePlayed(true)
    }
}

/**
 * The tour's steps: an introduction, the five places that matter, and a closing card.
 *
 * Same shape as Zalith Launcher 2's main-screen guide - a welcome, a step per highlighted control,
 * and a note about the one action people miss first time round.
 */
@Composable
fun rememberGuideSteps(): List<GuideStep> {
    // The strings are resolved here, in the composable, and handed to the plain list below: a
    // stringResource call cannot live inside a remember lambda.
    val appName = stringResource(R.string.app_name)
    return listOf(
        GuideStep(
            anchor = null,
            title = stringResource(R.string.aerix_guide_welcome_title, appName),
            body = stringResource(R.string.aerix_guide_welcome_body)
        ),
        GuideStep(
            anchor = AerixGuideState.CREATE,
            title = stringResource(R.string.aerix_guide_create_title),
            body = stringResource(R.string.aerix_guide_create_body)
        ),
        GuideStep(
            anchor = AerixGuideState.LIBRARY,
            title = stringResource(R.string.aerix_guide_library_title),
            body = stringResource(R.string.aerix_guide_library_body)
        ),
        GuideStep(
            anchor = AerixGuideState.PLAY,
            title = stringResource(R.string.aerix_guide_play_title),
            body = stringResource(R.string.aerix_guide_play_body)
        ),
        GuideStep(
            anchor = AerixGuideState.SETTINGS,
            title = stringResource(R.string.aerix_guide_renderer_title),
            body = stringResource(R.string.aerix_guide_renderer_body)
        ),
        GuideStep(
            anchor = AerixGuideState.MODPACKS,
            title = stringResource(R.string.aerix_guide_modpacks_title),
            body = stringResource(R.string.aerix_guide_modpacks_body)
        ),
        GuideStep(
            anchor = null,
            title = stringResource(R.string.aerix_guide_end_title),
            body = stringResource(R.string.aerix_guide_end_body),
            isLast = true
        )
    )
}
