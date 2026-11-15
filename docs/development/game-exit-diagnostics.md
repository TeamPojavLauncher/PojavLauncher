# Game Exit And Crash Logs

Two separate defects that produce one identical support thread: *the game crashes
the instant it launches, the whole app disappears, there is nothing to send, and
it happens every time.*

Neither is the game crashing. Both are what happens **around** it.

---

## 1. The launcher never came back

`GameActivity` runs in its own process:

```xml
<activity android:name="net.kdt.pojavlaunch.game.GameActivity"
          android:process=":game" ... />
```

so ending a game session has to end that process. The bug was the order.

`Tools.restartLauncherActivity` only *asks* Android to bring the launcher back —
it does not wait. `Tools.fullyExit` is `Process.killProcess(myPid())`. Called back
to back, the kill can win the race, Android drops the pending start, and the
launcher never returns.

Nothing threw, so there is no `latestcrash.txt` and no dialog. The app simply was
not there any more. Reopen it, launch again, same thing — which reads exactly like
an unfixable crash.

The codebase already knew about this race. `GameActivity.switchAccount` waits
700 ms and says so:

> Let the launch request reach the system before this process dies, so the switch
> cannot be swallowed on the way out.

Both `PojavLaunchBridge` exits do the same. The two paths that end an *ordinary*
game did not:

- `GameRunner.launchGame` — every normal game exit
- `Tools.dialogForceClose` — the in-game **Force close** item, which is precisely
  the button a player reaches for when a game is already stuck

Because the delay is invisible at the call site, it is now behind one helper that
cannot be half-applied:

```java
Tools.RELAUNCH_GRACE_MS            // 700L, the value every exit path shares
Tools.restartLauncherActivityThenExit(context)
```

---

## 2. The retry destroyed the log

`Logger.begin` opens the game log with truncate:

```c
latestlog_fd = open(logFilePath, O_WRONLY | O_TRUNC);   /* jni/stdio_is.c */
```

`GameActivity.onCreate` calls it on every launch, before instance, renderer and UI
setup, so an early failure cannot be mistaken for output from a previous session.
Sound reasoning — but it means the only copy of a startup crash lives exactly as
long as the player takes to tap launch again. A game that dies on startup is
always retried at once, because there is nothing else on screen.

`PreviousLaunchLog` moves the live log aside first:

```
<game home>/latestlog.txt                                     the launch in progress
<game home>/aerix-crash-logs/aerix-log-<yyyyMMdd-HHmmss>.txt  the one before it
```

`GameActivity.onCreate` calls `PreviousLaunchLog.archive` immediately before
`Logger.begin`, so **ordering is the whole mechanism**. Archives are reachable
from **Settings → Log** as tabs next to the live log, labelled with when the
launch happened, and are plain files in the game home for a file manager.

Decisions worth knowing before changing this:

- **Every launch is archived, not just crashes.** Nothing decides whether the
  previous launch failed. Guessing wrong in that direction loses the evidence this
  exists to keep; five small text files is a cheap price for never guessing.
- **Bounded at 5** (`PreviousLaunchLog.KEEP`). A crash loop retries indefinitely.
- **`archive` never throws.** It runs at the exact moment a launch is already
  failing. A failure returns `null` and the launch proceeds.
- **Pure `java.io`, no Android types**, so the rotation is testable against a
  temporary directory.

---

## Tests

| Test | Guards |
| --- | --- |
| `tests/game_exit_relaunch_smoke.py` | the `:game` process declaration; one helper owns the gap; both ordinary exit paths use it; no caller restarts and then kills outright; the paths that already waited still wait on the same number |
| `tests/previous_launch_log_smoke.py` | archive before `Logger.begin`; empty-file no-op; the bound; the swallow-`Throwable`; the viewer lists the archives |

Both run in `aerix-debug.yml` and `release.yml`.

`previous_launch_log_smoke` asserts the `O_TRUNC` in `stdio_is.c` is still there.
That archive is a workaround for it, not a replacement: if the truncation ever
goes, delete `PreviousLaunchLog` and its test rather than leaving dead weight.

---

## What this does not do

Neither change fixes a game that crashes on startup. They fix what the player
experiences around it — the launcher vanishing instead of returning, and the trace
being deleted before anyone could read it. A startup crash now leaves its log
behind no matter how many times the player retries, which is the difference
between a support thread that can be diagnosed and one that cannot.
