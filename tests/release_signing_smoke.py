#!/usr/bin/env python3
"""A release signing key must never be a file in a public repository.

This started as a user complaint: "anyone who installs our app gets the virus popup". While checking
what a scanner might be reacting to, the manifest turned out to be only half the story - the other half
was `app_pojavlauncher/aerix_release.p12`, tracked in git, with its password in the file next to it:

    storePassword System.getenv("AERIX_KEYSTORE_PASSWORD") ?: "aerix-release"

and a CI job that never set `AERIX_KEYSTORE_PASSWORD`. So every published release fell through to the
default, signed with a key any visitor could download. A signing key is not a build convenience, it is
the only thing telling a phone that an APK came from us: whoever has it can publish an update that
every installed copy accepts, with any code inside.

Pinned here, cheapest-first:

  1. no keystore in the repository, and none ignored-but-lurking;
  2. no fallback password in any signing config - a missing secret must produce an unsigned APK that
     CI refuses, never a silently public-key-signed APK that CI publishes;
  3. the ban list exists, is a real fingerprint, and the workflow actually reads it;
  4. the release job unpacks a key from secrets, re-signs before checksums, and gates on signers;
  5. the debug key stays public and stays out of release signing.
"""

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app_pojavlauncher/build.gradle"
RELEASE_WF = ROOT / ".github/workflows/release.yml"
BAN_LIST = ROOT / "tests/leaked_cert_fingerprint.txt"
failures = []


def check(condition: bool, message: str) -> None:
    if not condition:
        failures.append(message)


gradle = GRADLE.read_text()

# ---- 1. no keys in the tree ----
tracked = subprocess.run(["git", "ls-files"], cwd=ROOT, capture_output=True, text=True)
if tracked.returncode != 0:
    check(False, "git ls-files failed, so the keystore sweep could not run - do not treat this as a pass")
    tracked_files = []
else:
    tracked_files = tracked.stdout.split()
# A keystore in a public repo is a leak only when its password is public too. The four upstream *.jks
# files are useless without a secret, and Android CI signs the Play .aab with one of them, so they are
# allowed. A *.p12 is the shape OUR leaked key had, and nothing legitimate needs one in the tree.
leaked = [f for f in tracked_files if f.endswith((".p12", ".pkcs12"))]
check(not leaked, f"release keystores must not be tracked in git: {leaked}")
check("app_pojavlauncher/aerix_release.p12" not in tracked_files,
      "the retired key that signed v2.3.0-v2.3.2 must stay out of the tree permanently")
in_tree = [f for f in tracked_files if f.endswith(".jks")]
for jks in in_tree:
    body = (ROOT / jks).name
    check(body != "aerix_release.p12", f"{jks}: unexpected")
gradle_for_pw = GRADLE.read_text()
for m in re.finditer(r'(\w+) \{\n(?:[^\n]*\n)*?\s+storeFile file\("([^"]+)"\)', gradle_for_pw):
    name, fname = m.group(1), m.group(2)
    check(name == "customDebug" or fname != "aerix_release.p12",
          f"{name} points at an in-tree keystore that used to be signed with a published password")
DEBUG_KEY = "app_pojavlauncher/debug.keystore"
check(DEBUG_KEY in tracked_files,
      f"{DEBUG_KEY} should still be tracked - the AOSP debug key is public by design and the "
      "debug/nightly builds depend on it")
other_keystores = [f for f in tracked_files if f.endswith(".keystore") and f != DEBUG_KEY]
check(not other_keystores, f"other .keystore files are tracked: {other_keystores}")

# ---- 2. no fallback passwords ----
check('?: "aerix-release"' not in gradle,
      "a signing config with a default password will sign a public release with a public key the "
      "moment CI forgets a secret")
# Walk the signingConfigs blocks by name: only the debug key may have its password in the file,
# because that password is public knowledge by design and signs nothing anyone installs on purpose.
blocks = gradle.split("signingConfigs {", 1)[1].split("\n    }\n", 1)[0]
for match in re.finditer(r"\n        (\w+) \{\n", blocks):
    name = match.group(1)
    end = blocks.find("\n        }", match.end())
    body = blocks[match.end():end if end > 0 else None]
    for pw in re.findall(r"(?:storePassword|keyPassword)\s+([^\n]+)", body):
        literal = pw.strip().startswith('"')
        check(not literal or name == "customDebug",
              f"signing config {name} hard-codes a password ({pw.strip()}); a release key's password "
              f"must come from the environment so it can never be published by accident")
check('System.getenv("AERIX_KEYSTORE_FILE")' in gradle,
      "aerixRelease must read the key location from the environment, so a key cannot be in the tree "
      "by accident")
check("UNSIGNED" in gradle,
      "a release build with no key should warn loudly that the APK is unsigned, because that is what "
      "the publish gate then refuses")

# ---- 3. the ban list is real ----
check(BAN_LIST.is_file(), "tests/leaked_cert_fingerprint.txt must exist - retiring a key without "
                          "banning its fingerprint means a future merge can put it back to work")
active = [l.strip() for l in BAN_LIST.read_text().splitlines() if l.strip() and not l.strip().startswith("#")]
check(len(active) >= 1, "the ban list has no live entries; comments do not block anything")
for digest in active:
    check(re.fullmatch(r"[0-9a-f]{64}", digest) is not None,
          f"ban list entry {digest!r} is not a bare lowercase SHA-256, so apksigner output will never match it")
check("fba0764460bf766ac2491c714ded1859201e5bf7ef3e8c276e49261845b279db" in active,
      "the fingerprint of the key that signed v2.3.0-v2.3.2 must be banned permanently")

# ---- 4. the release job matches the gradle side ----
wf = RELEASE_WF.read_text()
check("secrets.AERIX_KEYSTORE_P12" in wf, "the release job must fetch the key from secrets")
check("base64 -d" in wf and "RUNNER_TEMP" in wf,
      "the key must be written to the runner temp dir, never into the checkout")
unpack = wf[wf.index("Unpack the release signing key"):]
unpack = unpack[:unpack.index("Unpack the retired signing key")]
check("AERIX_KEYSTORE_P12 is not set" in unpack,
      "a release with no key configured must stop with an error, not continue unsigned")
check("::error::AERIX_KEYSTORE_PASSWORD is not set" in unpack,
      "a key without its password is the same hole wearing a hat")
check("leaked_cert_fingerprint.txt" in wf, "the workflow must read the ban list")
check("verify --print-certs" in wf, "the gate must read the real signers out of the built APK")
check("is signed by the retired key" in wf, "the gate message must say what it caught")
sign_at = wf.index("Align and sign every APK with both keys")
collect_at = wf.index("Name the assets after the version")
sums_at = wf.index("sha256sum Aerix-Launcher-*.apk")
check(sign_at < collect_at < sums_at,
      "re-signing must happen on Gradle's outputs, BEFORE 'Name the assets' copies them into out/ and "
      "hashes them: a checksum taken before a re-sign describes a file nobody downloads, and the "
      "in-app updater verifies those checksums")
sign_block = wf[sign_at:collect_at]
check("build/outputs/apk/full/release" in sign_block and "build/outputs/apk/noruntime/release" in sign_block,
      "the sign step must glob Gradle's output directories - out/Aerix-Launcher-*.apk does not exist "
      "yet at that point, and an empty glob would sign nothing without failing")
check("test \"$signed\" -ge 6" in sign_block,
      "a zero-APK re-sign must be an error, not a successful no-op")
check("--v2-signer" in wf and "aerix_legacy.p12" in wf,
      "the transition release needs two signers; Android accepts an update when any signer matches "
      "the installed app, which is the only reason anyone can update without uninstalling")
check("legacy_key_present" in wf,
      "the double-sign must be driven by whether the retired key was provided, so the step after the "
      "transition can be deleted without editing the loop")
check("carries $n signer(s)" in wf or "signer(s); the transition release" in wf,
      "a single-signed transition release must fail the build rather than brick existing installs")

# ---- 5. the debug key stays in its lane ----
check("signingConfig signingConfigs.customDebug" in gradle,
      "debug and nightly builds keep the public debug key - that is the convention, and it is what "
      "lets a testing build install over another one")
release_block = gradle[gradle.index("        release {"):]
release_block = release_block[:release_block.index("\n        }")]
check("signingConfig signingConfigs.aerixRelease" in release_block,
      "the release build type must use the rotated key, not the debug one")
check("customDebug" not in release_block,
      "a release signed with the AOSP debug key is unsigned with extra steps")

if failures:
    for f in failures:
        print(f"  FAIL {f}")
    sys.exit(1)

print("signing: no in-tree key with a public password, no fallback in any signing config, the "
      "retired fingerprint banned, the transition double-signed before checksums")
