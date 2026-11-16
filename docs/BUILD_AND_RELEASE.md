# Aerix Nightly and public releases

All non-release updates and testing happen on the **Aerix Nightly** APK. The public
APK is only built when the owner asks for a release and confirms it.

## The Aerix Nightly APK

| | |
|---|---|
| Release tag | `debug-latest` (prerelease on GitHub Releases) |
| Direct APK | `https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/debug-latest/Aerix-Nightly.apk` |
| Commit-stamped APK | `Aerix-Nightly-<commit>.apk` on the same release |
| Checksum | `https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/debug-latest/Aerix-Nightly.apk.sha256` |
| Build info | `https://github.com/entitybrian69-bit/Aerix-launcher/releases/download/debug-latest/NIGHTLY_BUILD.txt` (commit, versionCode, sha256) |
| CI workflow | **Aerix Nightly (testing)** (`aerix-debug.yml`) |
| CI artifact | `aerix-nightly-apk` |
| Source | this branch (`version.properties` = `2.3.2`) |
| Variant | `noruntimeNightly` |
| applicationId | `net.al.aerixlauncher.debug` |
| Label | Aerix Nightly |
| Signing | `app_pojavlauncher/debug.keystore` (alias `androiddebugkey`) |

Aerix Nightly is the debug build with a different launcher label. It keeps the
same applicationId, the same signing certificate and the same monotonically
increasing versionCode as the old debug APK, so it installs straight over an
existing **Aerix Launcher (Debug)** install, keeps that app's data, and behaves
identically. It installs next to the public launcher and can never overwrite a
release APK.

The `debug-latest` tag is only the hosting anchor for the prerelease: it keeps the
release sorted below every public `v*` release and keeps `releases/latest` on the
stable build. Every APK asset on it is Aerix Nightly. On each publish the old
`Aerix-Launcher-debug*.apk` files and `DEBUG_BUILD.txt` are removed, together with
stamped nightlies from older commits, so there is only ever one testing build to
download. Old debug download links are not updated anymore.

Aerix's IDs have changed from `net.kdt.pojavlaunch` to `net.al.aerixlauncher`.
An older install remains a separate app and cannot be updated in place. On
Android 10 and newer, the optional first-run prompt or Settings import action
copies supported game data; accounts, preferences, and runtimes stay with the
old app. Keep it installed until the new one is verified. A Google Play package
ID is immutable, so publishing under the new ID requires a separate Play Console
app entry.

The `noruntime` flavor does not embed a Java runtime, keeping downloads smaller;
the launcher downloads the runtime it needs on first launch. APKs are never
committed to Git (`apk/` is ignored).

### When it is rebuilt

`.github/workflows/aerix-debug.yml` runs on every push to the working branch that
touches the app sources, Gradle files, submodules or the workflow itself. It also
has a manual trigger. The job builds the APK, checks that the package name really
is `net.al.aerixlauncher.debug`, that the APK carries the Aerix Nightly label and that it is
signed with the debug key, uploads it as an Actions artifact, and attaches
it to the `debug-latest` GitHub Release (marked as a prerelease so `releases/latest`
remains the public stable release).

### Building one locally

```bash
./gradlew :app_pojavlauncher:assembleNoruntimeNightly
# output: app_pojavlauncher/build/outputs/apk/noruntime/nightly/app_pojavlauncher-noruntime-nightly.apk
```

A local build needs JDK 21, the Android SDK with NDK `29.0.14206865`, and
initialised submodules (`git submodule update --init --recursive`).

The separate `.github/workflows/nightly.yml` is legacy: it builds a different app
(`net.al.aerixlauncher.pub`) and its publish steps only run for the upstream
repository owner. It is not the testing channel.

## Public releases

Public APKs are **not** stored in the Git repository. The universal APK is
147 MB, above GitHub's 100 MB limit for repository files, so release builds live
as assets on the GitHub Releases page and nowhere else.

Current public release: **v2.3.2** (notes in
`docs/releases/v2.3.2.md`).

The website is a second step, not part of the cut. GitHub Pages is set to *GitHub Actions*, and
`pages.yml` deploys only from `v3_openjdk` - a side branch verifies `docs/` and stops there, on purpose,
so a testing branch can never publish a build. That means the site shows whatever `v3_openjdk` last
held: a release tagged on a session branch is on Releases and in the in-app updater immediately, but the
website keeps the previous version until the branch is merged. `Release CI` now says so in its last
publish step when a cut is not yet on `v3_openjdk`. Regenerate with `python3 tools/build_site.py`
before the merge if the version rows drifted.

The release carries three builds per edition, named after `version.properties`:
`Aerix-Launcher-<version>-arm64-v8a.apk`, `-armeabi-v7a.apk` and the universal
`Aerix-Launcher-<version>.apk`, plus the three `-noruntime` equivalents.
`Aerix-Launcher.apk` and `Aerix-Launcher-noruntime.apk` are copies of the
universal builds. `SHA256SUMS` on the release carries the size and hash of every
file — take them from there, never retype them here.

The unversioned names are copies of the same files. The download buttons on the
website point at `releases/latest/download/<name>`, so a release has to carry
them alongside the versioned names the in-app updater looks for. The in-app
updater only ever considers stable `vX.Y.Z` releases; Nightly updates are
downloaded from the release page.

Release notes for each version live in `docs/releases/`.

### Releasing

`.github/workflows/release.yml` takes a confirmation input and does nothing
without it. It needs `RELEASE` typed in, builds both editions, names them after
`version.properties`, writes `SHA256SUMS`, then a second job puts them on the
Releases page as `v<version>` with `docs/releases/v<version>.md` as the notes.
It refuses to touch a tag that already exists. Where the tooling has no
Actions-write token, the same build can be started by pushing a change to that
file with `[build-release]` in the commit message.

To release a new version: raise `versionName` in `version.properties`, add
`docs/releases/v<version>.md`, and run the release workflow.

A release happens when the owner says so, and not before. Until then, changes are
verified on Aerix Nightly.
