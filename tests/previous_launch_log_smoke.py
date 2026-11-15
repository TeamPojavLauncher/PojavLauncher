#!/usr/bin/env python3
"""A game that dies on startup must not have its own log destroyed by the retry.

The report from the field is always the same shape: the game crashes the instant it is launched, the
player taps launch again because nothing else is on screen, and there is then no log to send. It reads
as a missing report. It is not one - the report existed, and the second launch deleted it.

`Logger.begin` opens the log with `O_WRONLY | O_TRUNC`:

    latestlog_fd = open(logFilePath, O_WRONLY | O_TRUNC);   // jni/stdio_is.c

which is correct for a log you are tailing live, and `GameActivity.onCreate` calls it on every launch,
before anything else runs. So the window in which a startup crash can be read is exactly as long as the
player takes to retry. Every retry erases the only copy.

The fix moves the previous launch's log aside before the truncation happens. Pinned here:

  1. the truncation is still in the native logger - this is a workaround for it, not a rewrite of it;
  2. `GameActivity` archives before `Logger.begin`, never after;
  3. archiving an empty or missing log is a no-op, so a first launch creates no junk;
  4. a failure to archive cannot stop the launch - the workaround must not become the crash;
  5. the archive is bounded, because a crash loop must not fill the disk;
  6. the archived logs are reachable from the in-app log viewer, not only from a file manager.
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
    """Index that reports a missing anchor as a failure instead of a traceback."""
    if needle not in source:
        check(False, f"{what}: `{needle}` is not in the file, so the structure this test measures is gone")
        return 0
    return source.index(needle)


# ---- 1. the cause is still there, and named ----
native = read("app_pojavlauncher/src/main/jni/stdio_is.c")
check("O_WRONLY | O_TRUNC" in native,
      "jni/stdio_is.c no longer opens the log with O_TRUNC. If the truncation is really gone the "
      "archive in GameActivity is dead weight - delete PreviousLaunchLog and this test with it.")

archiver = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/log/PreviousLaunchLog.java")
check("O_TRUNC" in archiver,
      "PreviousLaunchLog must say why it exists: the O_TRUNC in jni/stdio_is.c. Without that the next "
      "reader deletes it as redundant.")

# ---- 2. the archive happens before the truncation ----
game = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/game/GameActivity.java")
archive_at = locate(game, "PreviousLaunchLog.archive(", "GameActivity")
begin_at = locate(game, "Logger.begin(", "GameActivity")
check(archive_at > 0 and begin_at > 0 and archive_at < begin_at,
      "GameActivity must call PreviousLaunchLog.archive BEFORE Logger.begin. After it there is nothing "
      "left to archive - Logger.begin has already truncated the file.")

check("latestLogFile.getParentFile()" in game,
      "GameActivity must archive into the folder that actually holds latestlog.txt, i.e. its parent. "
      "A hardcoded path here drifts from Tools.DIR_GAME_HOME the moment that moves.")

# ---- 3. nothing to keep means no junk ----
check("!latest.isFile() || latest.length() <= 0" in archiver,
      "archive() must return early when latestlog.txt is missing or empty, otherwise every first launch "
      "creates a zero-byte archive and the viewer fills with blank tabs.")

# ---- 4. the workaround cannot become the crash ----
check(re.search(r"public static File archive\(File gameHome, long whenMs\) \{\s*\n\s*if \(gameHome == null\) return null;\s*\n\s*try \{",
                archiver) is not None,
      "archive(gameHome, whenMs) must wrap its whole body in a try that returns null. A failed archive "
      "has to cost the player nothing - this code runs at the exact moment a launch is already failing.")
check("} catch (Throwable ignored) {" in archiver and "return null;" in archiver,
      "archive() must swallow Throwable, not just IOException: a failure here would be thrown out of "
      "GameActivity.onCreate and replace the crash being diagnosed with a new one.")

# ---- 5. bounded, so a crash loop cannot fill the disk ----
check("public static final int KEEP = 5;" in archiver,
      "PreviousLaunchLog.KEEP must exist: an instant crash is retried over and over, and an unbounded "
      "archive would grow once per retry forever.")
check("files.length <= KEEP" in archiver,
      "prune() must stop as soon as the archive is within KEEP instead of walking the whole folder.")
check("for (int i = KEEP; i < files.length; i++)" in archiver,
      "prune() must delete from KEEP onwards after sorting newest first, so the newest KEEP survive.")
check("Long.compare(b.lastModified(), a.lastModified())" in archiver,
      "prune() must sort newest first before cutting; the opposite order deletes the crash just taken "
      "and keeps the five oldest instead.")

# ---- 6. reachable from the log viewer ----
viewer = read("app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/screens/LogScreen.kt")
check("PreviousLaunchLog.list(" in viewer,
      "LogScreen must list the archived launches. A log the player cannot open from inside the launcher "
      "might as well not exist - that is the complaint this fixes.")
check("previousLaunchLabel(" in viewer,
      "LogScreen must label each archive with when it was taken; a tab called 'aerix-log-...' tells the "
      "player nothing about which crash they are looking at.")

list_at = locate(viewer, "PreviousLaunchLog.list(", "LogScreen")
launch_status_at = locate(viewer, "LaunchDiagnostics.FILE_NAME", "LogScreen")
check(list_at > 0 and launch_status_at > 0 and list_at < launch_status_at,
      "The archived launches must sit next to the live log in the tab list, not after the per-instance "
      "crash reports - they are the same kind of thing and the player will look for them there.")

if failures:
    print("FAIL previous_launch_log_smoke")
    for failure in failures:
        print(" -", failure)
    sys.exit(1)

print("OK previous_launch_log_smoke: a startup crash survives the retry that follows it")
