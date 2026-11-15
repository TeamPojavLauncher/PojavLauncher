#!/usr/bin/env python3
"""Ending the game must not take the launcher down with it.

The game runs in its own process - `AndroidManifest.xml` gives `GameActivity`
`android:process=":game"` - so when a game session ends, that process has to end too. That part is
correct. What was not correct is *how* it ended.

`Tools.restartLauncherActivity` only asks Android to bring the launcher back; it does not wait.
`Tools.fullyExit` is `Process.killProcess(myPid())`. Called back to back, the kill can win the race,
Android drops the pending start, and the launcher never comes back.

From the player's side that is not "the game closed". The whole app vanished - no dialog, no error,
nothing in `latestcrash.txt` because nothing threw. Reopen it, launch again, and the same thing
happens, which is what makes it read as an unfixable crash rather than a dropped intent.

The codebase already knew the race: `GameActivity.switchAccount` waits 700 ms and says why ("Let the
launch request reach the system before this process dies, so the switch cannot be swallowed on the
way out"), and both `PojavLaunchBridge` exits do the same. But the two paths that end an ordinary
game - `GameRunner.launchGame`, and `Tools.dialogForceClose` behind the in-game Force close item -
both killed immediately. The delay was invisible at the call site and easy to leave out, so it is
now behind one helper that cannot be half-applied.

Pinned here:

  1. the game really is a separate process, which is what makes a dropped relaunch look like the
     whole app dying;
  2. `fullyExit` is still an immediate kill - the helper guards the callers, not the helper;
  3. one helper owns the gap, and it restarts before it schedules the kill;
  4. the two ordinary game-exit paths go through the helper;
  5. no caller anywhere restarts the launcher and then kills it outright;
  6. the paths that already got this right still wait, on the same number.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"
failures = []


def read(rel: str) -> str:
    return (ROOT / rel).read_text()


def check(condition: bool, message: str) -> None:
    if not condition:
        failures.append(message)


def locate(source: str, needle: str, what: str) -> int:
    if needle not in source:
        check(False, f"{what}: `{needle}` is not in the file, so the structure this test measures is gone")
        return 0
    return source.index(needle)


HELPER = "restartLauncherActivityThenExit"

# ---- 1. the game is its own process ----
manifest = read("app_pojavlauncher/src/main/AndroidManifest.xml")
game_decl = manifest[locate(manifest, "net.kdt.pojavlaunch.game.GameActivity", "AndroidManifest.xml"):]
check('android:process=":game"' in game_decl.split("/>")[0],
      "GameActivity must declare android:process=\":game\". If the game ever moves back into the "
      "launcher process, killing it takes the launcher with it directly and the gap below is "
      "solving a different problem.")

# ---- 2. fullyExit is still an immediate kill ----
tools = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java")
check("public static void fullyExit()" in tools
      and "Process.killProcess(android.os.Process.myPid())" in tools,
      "Tools.fullyExit must remain an immediate Process.killProcess. This guards the callers; if the "
      "helper itself learned to wait, rewrite this test rather than relax it.")

# ---- 3. one helper owns the gap ----
check("public static final long RELAUNCH_GRACE_MS = 700L;" in tools,
      "Tools must define RELAUNCH_GRACE_MS = 700L. That is the value GameActivity.switchAccount and "
      "PojavLaunchBridge already use; a second, different number means the paths drift apart and the "
      "next person has to rediscover which one is right.")

helper_at = locate(tools, f"public static void {HELPER}(Context context) {{", "Tools")
if helper_at:
    body = tools[helper_at:helper_at + 400]
    restart_at = locate(body, "restartLauncherActivity(context);", "the relaunch helper")
    delayed_at = locate(body, "MAIN_HANDLER.postDelayed(Tools::fullyExit, RELAUNCH_GRACE_MS);", "the relaunch helper")
    check(restart_at > 0 and delayed_at > 0 and restart_at < delayed_at,
          f"Tools.{HELPER} must call restartLauncherActivity BEFORE postDelayed(fullyExit). The other "
          f"way round there is nothing left in this process to deliver the relaunch.")
    check("Tools.fullyExit();" not in body,
          f"Tools.{HELPER} must schedule fullyExit, never call it directly - a direct call is the "
          f"race this helper exists to remove.")

# ---- 4. the two ordinary game-exit paths use it ----
runner = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/GameRunner.java")
check(f"Tools.{HELPER}(activity);" in runner,
      f"GameRunner.launchGame must end with Tools.{HELPER}(activity). This is the path every normal "
      f"game exit takes, so an immediate kill here is what makes the whole app disappear.")

force_close_at = locate(tools, "public static void dialogForceClose(Context ctx)", "Tools")
if force_close_at:
    body = tools[force_close_at:force_close_at + 700]
    check(f"Tools.{HELPER}(ctx);" in body,
          "Tools.dialogForceClose - the in-game Force close item - must use the same helper. It is the "
          "button a player reaches for when a game is already stuck, and killing immediately there "
          "leaves them with no launcher to come back to.")

# ---- 5. nobody restarts and then kills outright ----
for path in SRC.rglob("*.java"):
    text = path.read_text()
    if "restartLauncherActivity" not in text:
        continue
    for match in re.finditer(r"restartLauncherActivity\w*\([^;]*\);", text):
        window = text[match.end():match.end() + 400]
        if re.search(r"^\s*Tools\.fullyExit\(\);", window, re.MULTILINE):
            failures.append(
                f"{path.name}: a restartLauncherActivity call is followed immediately by "
                f"Tools.fullyExit(). Use Tools.{HELPER}(context) instead - the relaunch can be "
                f"dropped and the launcher never comes back.")

# ---- 6. the paths that already got it right still wait ----
game_activity = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/game/GameActivity.java")
bridge = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/bridge/PojavLaunchBridge.kt")
check("postDelayed(Tools::fullyExit, 700L)" in game_activity,
      "GameActivity.switchAccount must still wait 700 ms - it is the precedent this fix copies, and "
      "its comment is the only written explanation of the race.")
check(bridge.count("postDelayed({ Tools.fullyExit() }, 700L)") >= 2,
      "PojavLaunchBridge must still delay both of its exits; they are the other two ways out of the "
      "game process that restart the launcher.")

if failures:
    print("FAIL game_exit_relaunch_smoke")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("OK game_exit_relaunch_smoke: the launcher survives the end of a game")
