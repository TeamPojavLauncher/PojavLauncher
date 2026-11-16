#!/usr/bin/env python3
"""The "Layout transparency" control must exist, be reachable, and actually change the glass.

The setting lives on the Renderer tab, in the slot the Global Renderer card left behind. It works
by scaling the alpha of the shared surface colours, so one slider reaches every card in the
launcher without each screen having to know about it.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"
RES = ROOT / "app_pojavlauncher/src/main/res/values/aerix_strings.xml"

prefs = (JAVA / "aerix/bridge/AerixPreferences.kt").read_text()
surface = (JAVA / "aerix/theme/AerixSurface.kt").read_text()
theme = (JAVA / "aerix/theme/AerixTheme.kt").read_text()
settings = (JAVA / "aerix/ui/screens/SettingsScreen.kt").read_text()
strings = RES.read_text()

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)


# 1. A persisted preference with a sane range.
check('const val KEY_LAYOUT_TRANSPARENCY = "layoutTransparency"' in prefs,
      "the transparency must have its own preference key")
check('const val DEFAULT_LAYOUT_TRANSPARENCY = 100' in prefs,
      "the default must be the design as drawn, so nobody's launcher changes on update")
check('var layoutTransparency by mutableIntStateOf(DEFAULT_LAYOUT_TRANSPARENCY)' in prefs,
      "the preference must be Compose state so the UI follows the slider")
check('layoutTransparency = p.getInt(KEY_LAYOUT_TRANSPARENCY, DEFAULT_LAYOUT_TRANSPARENCY)'
      in prefs,
      "the transparency must be restored on launch")
check('fun saveLayoutTransparency(value: Int)' in prefs, "the transparency must be savable")
check('edit { putInt(KEY_LAYOUT_TRANSPARENCY, layoutTransparency) }' in prefs,
      "saving the transparency must persist it")

# It is a different setting from the wallpaper wash - one must not overwrite the other.
check('KEY_LAYOUT_TRANSPARENCY = "layoutTransparency"' in prefs
      and 'KEY_BACKGROUND_OPACITY = "launcherBackgroundOpacity"' in prefs,
      "layout transparency must not reuse the wallpaper visibility key")

# 2. The glass colours must be derived from it, or the slider would do nothing.
check('private fun glass(base: Color): Color' in surface,
      "surface colours must be derived through one helper")
for name, argb in (("panel", "0x66343839"), ("panelRaised", "0x80515858"),
                   ("panelTrack", "0x604A5050"), ("panelGlassTint", "0x3D596362")):
    check(f'val {name}: Color get() = glass(Color({argb}))' in surface,
          f"AerixSurface.{name} must go through glass(), not stay a fixed colour")
check('AerixPreferences.layoutTransparency' in surface,
      "the glass helper must read the preference")
check('base.alpha * percent / 100f' in surface,
      "the setting must scale the design's own alpha, so 100% is unchanged")
check('if (percent >= 100) return base' in surface,
      "100% must return the original colour untouched")

# Material's surface roles are built from the same colours, so the scheme must be rebuilt per pass.
check('private fun aerixDark(): ColorScheme' in theme,
      "the colour scheme reads the glass colours, so it must be built inside composition")
check('AerixColorTheme.AERIX -> aerixDark()' in theme,
      "the theme must call the scheme builder, not a cached property")

# 3. The control exists, on the renderer tab, as a percentage.
check('aerix_settings_layout_transparency' in strings,
      "the control needs a title string")
check('aerix_settings_layout_transparency_sub' in strings,
      "the control needs a summary string")
check('R.string.aerix_settings_layout_transparency)' in settings,
      "the renderer tab must show the control")
check('onValueChange = { AerixPreferences.saveLayoutTransparency(it) }' in settings,
      "moving the slider must save the setting")
check('range = 25..100' in settings, "the slider range must match what the preference clamps to")
check('range = 25..100' in settings and 'coerceIn(25, 100)' in prefs,
      "the slider and the preference must clamp identically")


def function_body(text, name):
    start = text.index(f"private fun {name}(")
    end = text.index("\n@Composable\nprivate fun", start) if "\n@Composable\nprivate fun" in text[start:] else len(text)
    return text[start:end]


renderer_tab = settings[settings.index("private fun RendererTabContent("):]
renderer_tab = renderer_tab[:renderer_tab.index("\n@Composable\nprivate fun")]
check('R.string.aerix_settings_layout_transparency)' in renderer_tab,
      "the control belongs on the Renderer tab")

# It sits above the renderer/driver cards, where the removed Global Renderer card was.
transparency_at = renderer_tab.index("aerix_settings_layout_transparency")
driver_at = renderer_tab.index("SelectCard") if "SelectCard" in renderer_tab else -1
check(driver_at == -1 or transparency_at < driver_at,
      "the transparency card must come before the renderer and driver cards")

# The Global Renderer section has to stay gone - the new control replaces it, it does not revive it.
check("Global Renderer" not in renderer_tab.replace("globalVulkanDriver", "").replace(
          "Global Renderer list is gone", ""),
      "the removed Global Renderer card must not come back")

for workflow in ("aerix-debug.yml", "release.yml"):
    text = (ROOT / ".github/workflows" / workflow).read_text()
    check("layout_transparency_smoke.py" in text, f"{workflow} must run this check")

if problems:
    print("layout transparency check: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("layout transparency check: the Renderer tab's transparency slider fades every glass surface")
