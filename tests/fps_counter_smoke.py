#!/usr/bin/env python3
"""The FPS readout must stay readable, and must be switchable from the in-game A menu.

Two complaints are locked down here:

* The counter sometimes showed "weird text". The messages it can print are now short enough for the
  pill it measures for itself, a hard cap keeps any future message inside that pill, and the text is
  drawn centred instead of riding the border. The long "reason" sentence moved to the content
  description, where screen readers can still reach it but the game screen cannot clip it.
* There was no way to turn the counter on or off from where a player notices it. The A menu grid now
  carries an "FPS Counter" tile bound to the same switch the launch screen and video options own.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"
view = (JAVA / "game/FpsView.java").read_text()
strings = (ROOT / "app_pojavlauncher/src/main/res/values/strings.xml").read_text()
overlay = (JAVA / "aerix/client/AerixClientOverlayView.kt").read_text()
store = (JAVA / "aerix/client/AerixClientStore.kt").read_text()
activity = (JAVA / "game/GameActivity.java").read_text()

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)


# 1. Nothing the pill can print may exceed the width it measures for itself.
values = dict(
    line.split(">", 1) for line in strings.splitlines()
    if line.strip().startswith("<string name=\"fps_overlay_")
)
limits = {"fps_overlay_waiting": 30, "fps_overlay_unavailable": 30,
          "fps_overlay_stalled": 30, "fps_overlay_value": 30,
          "fps_overlay_value_hz": 30, "fps_overlay_unavailable_reason": 200}
for name, limit in limits.items():
    text = next(v for k, v in values.items() if f'"{name}"' in k).split("</string>")[0]
    # A format string counts as its longest plausible filled-in form.
    rendered = (text.replace("%1$d", "1000").replace("%2$d", "1000")
                    .replace("%3$d", "144"))
    if name != "fps_overlay_unavailable_reason":
        check(len(rendered) <= limit, f"{name} is {len(rendered)} characters; keep it under the cap")
check("the game is not presenting frames" not in strings,
      "the long explanatory sentence must not be drawn in the pill")

# 2. The pill enforces a cap and says where the reason went.
check("private static final int MAX_TEXT_CHARS = 30;" in view,
      "FpsView must cap its text, so a bad measurement cannot draw off screen")
check("private String fit(String value)" in view, "the cap must be applied by one helper")
check("value = fit(value);" in view, "updateText must run every message through the cap")
check("offsetByCodePoints" in view,
      "the truncation must not split a surrogate pair, which draws as a box")
check("\\u2026" in view, "truncation must be visible as an ellipsis, not silent")
reason_at = view.index("setContentDescription(getContext().getString(R.string.fps_overlay_unavailable_reason)")
reason_text = view[reason_at:view.index(";", reason_at)]
check("R.string.fps_overlay_unavailable_reason" in reason_text,
      "the full reason must still be reachable, via the content description")
# updateText rewrites the description, so the reason must be set after it or it is lost.
unavailable_at = view.index("updateText(getContext().getString(R.string.fps_overlay_unavailable));")
check(unavailable_at < reason_at,
      "the reason must be set after updateText, which would otherwise overwrite it")
check("updateText(getContext().getString(R.string.fps_overlay_unavailable))" in view,
      "the pill itself must show the short message")

# 3. Drawing must match the measured size: centred, with no left-padding offset.
check("textPaint.setTextAlign(Paint.Align.CENTER);" in view,
      "the text must be centred, not left-aligned inside a pill sized to the text")
check("canvas.drawText(text, getWidth() / 2f," in view,
      "the draw x must be the centre the alignment expects")
check("getPaddingLeft(), -textPaint" not in view,
      "the old left-aligned draw, which rode the border, must be gone")

# 4. The A menu grid carries an FPS Counter tile bound to the real switch.
tile = overlay[overlay.index('id = "fps_counter"'):]
tile = tile[:tile.index("ClientSquareModule(")]
check('title = "FPS Counter"' in tile, "the tile must be called FPS Counter")
check("FPS Counter" in overlay, "the grid must list it")
check("AerixClientStore.fpsOverlayEnabled" in tile,
      "the tile must read the same switch the launcher shows")
check("AerixClientStore.fpsOverlayEnabled = !AerixClientStore.fpsOverlayEnabled" in tile,
      "tapping it must toggle the counter on and off")
check("AerixClientStore.persist()" in tile, "the choice must survive the session")
check('id = "fps_counter"' in overlay and '"fps"' not in tile,
      "the new tile must not be confused with the in-HUD performance pill")

# 5. That switch must be what actually shows or hides the readout.
check("AerixClientStore.INSTANCE.getFpsOverlayEnabled()" in activity,
      "GameActivity must show the readout according to the switch the tile toggles")
check("var fpsOverlayEnabled by mutableStateOf(" in store,
      "the switch must be Compose state, so the tile updates when it flips")
check('putBoolean("fpsOverlayEnabled", fpsOverlayEnabled)' in store,
      "the switch must be saved")

if problems:
    print("fps counter check: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("fps counter check: the readout stays inside its pill, and the A menu can turn it on and off")
