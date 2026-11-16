#!/usr/bin/env python3
"""A cape must only ever be worn because the player put it on.

The bug this locks down: an Aerix cape was equipped by default, and the launcher also served it to
the game in place of the account's own cape. So a player who had never opened the capes page saw a
cape, and a player who *had* equipped their own cape still saw the launcher's.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"

store = (JAVA / "aerix/client/AerixClientStore.kt").read_text()
cosmetics = (JAVA / "aerix/client/AerixClientCosmetics.kt").read_text()
skin_server = (JAVA / "aerix/bridge/AerixSkinServer.kt").read_text()
overlay = (JAVA / "aerix/client/AerixClientOverlayView.kt").read_text()
stage = (JAVA / "aerix/client/LiquidCrystalComponents.kt").read_text()
profile = (JAVA / "aerix/ui/screens/SocialProfileScreen.kt").read_text()

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)


# 1. Nothing is equipped unless the player equipped it.
check('const val CAPE_NONE = "none"' in store, "AerixClientStore must name the unequipped state")
check('var equippedCapeId by mutableStateOf(CAPE_NONE)' in store,
      "the default cape must be none, not a cape nobody chose")
check('p.getString("equippedCapeId", CAPE_NONE)' in store,
      "reading the preference must default to none as well")
# The one tolerated mention of a named cape is the migration's record of what the old build wrote.
for line in store.splitlines():
    code = line.strip()
    if "crimson_sovereign" in code and not code.startswith(("*", "//", "/*")):
        check('LEGACY_DEFAULT_CAPE' in code,
              "a named cape may only appear as the legacy default the migration clears")

# 2. Resolving an unequipped cape yields null - never a substitute.
check('fun effectiveCapeSpec(): ClientCapeSpec?' in cosmetics,
      "effectiveCapeSpec must be nullable so 'none' is representable")
check('if (AerixClientStore.equippedCapeId == AerixClientStore.CAPE_NONE) return null' in cosmetics,
      "'none' must resolve to no cape")
check('return capes.first()' not in cosmetics,
      "an unusable or unequipped cape must not silently become the first cape in the list")
check('val spec = effectiveCapeSpec() ?: return@runCatching null' in cosmetics,
      "no cape equipped must mean no cape bytes, not a generated one")

# 3. The cape handed to the game must be the player's own when they equipped none.
check('AerixClientCosmetics.resolveInGameCapeBytes(context)' in skin_server,
      "the skin server reads the launcher's cape choice")
check('} else {\n            net.kdt.pojavlaunch.aerix.client.AerixClientCosmetics.resolveInGameCapeBytes(context)\n        }' in skin_server,
      "the launcher's cape is used only when the account has no imported cape of its own")

# 4. Stale frames from a previous choice must be cleared, or the mod keeps drawing them.
check('framesDir.listFiles()?.forEach { it.delete() }' in cosmetics,
      "taking a cape off must delete the frames the previous cape left in the instance")
check('No cape equipped; removed the frames of the previous one' in cosmetics,
      "that case must be reported, not silent")

# 5. Every cape UI has to be able to say 'no cape' and to survive a null selection.
for name, text in (("in-game capes page", overlay), ("Aerix profile page", profile)):
    check('No cape' in text, f"the {name} must offer a way to wear nothing")
    check('CAPE_NONE' in text, f"the {name} must set the unequipped state, not a cape id")
    check('.effectiveCapeSpec().id' not in text,
          f"the {name} dereferences a cape that may be absent")
check('activeCape?.id == spec.id' in overlay, "the capes grid must handle no selection")
check('capeSpec: ClientCapeSpec?' in stage, "the 3D preview must draw a bare back when nothing is worn")
check('capeSpec?.let { capeSpec ->' in stage, "the cape geometry must be skipped when nothing is worn")

# 6. The in-game mod must stop drawing a cape whose frames the launcher removed - otherwise taking
#    a cape off only takes effect on the next launch.
bridge = (ROOT / "aerix_client_mod/src/main/java/aerix/client/mod/ModernCapeBridge.java").read_text()
check('if (frameStamp != Long.MIN_VALUE) {' in bridge,
      "the bridge must drop the frames it holds in memory when the folder goes away")
check('frameImages.clear()' in bridge, "the held frames must actually be released")

# 7. An install that already saved the old default must have it cleared once. Changing the default
#    is not enough: the old build wrote its choice into the prefs file, and that value wins.
check('MIGRATION_CAPE_DEFAULT' in store,
      "a one-time migration is needed for installs that already saved the old default")
check('private const val LEGACY_DEFAULT_CAPE = "crimson_sovereign"' in store,
      "the migration must name the exact value the old build wrote")
check('if (p.getBoolean(MIGRATION_CAPE_DEFAULT, false))' in store,
      "the migration must run only until its flag is set")
check('if (storedCape == LEGACY_DEFAULT_CAPE) CAPE_NONE else storedCape' in store,
      "the migration clears only the unchosen legacy default, keeping a cape the player picked")
check('.putString("equippedCapeId", migrated)' in store,
      "the cleared value must be written back, so the prefs file stops holding the old cape")
check('p.edit()' in store and 'putBoolean(MIGRATION_CAPE_DEFAULT, true)' in store,
      "the migration must record that it ran, or it would undo the player's next choice")

# 8. The 3D preview must actually erase a cape it already drew. loadCape(null) used to return early,
#    leaving the previous cape on the doll - so unequipping looked like it did nothing.
viewer = (ROOT / "app_pojavlauncher/src/main/assets/skinview/skinview.js").read_text()


def js_function(name):
    start = viewer.index(f"function {name}(")
    return viewer[start:viewer.index("\n}\n", start) + 3]


cape_fn = js_function("loadCape")
check('if (!capeUrl) {' in cape_fn, "loadCape must treat a null or empty URL as 'no cape'")
check('skinViewer.cape.visible = false' in cape_fn,
      "a null cape URL must hide the cape, not leave the old one drawn")
check('skinViewer.cape.visible = true' in cape_fn,
      "a successful load must re-show a cape that was hidden before")
check(cape_fn.index('skinViewer.cape.visible = false') < cape_fn.index('skinViewer.loadCape(capeUrl)'),
      "the clear must happen before the fetch, so a failed fetch cannot fall back to the old cape")

# 9. Both fixes must stay wired into CI.
for workflow in ("aerix-debug.yml", "release.yml"):
    text = (ROOT / ".github/workflows" / workflow).read_text()
    check("cape_default_smoke.py" in text, f"{workflow} must run the cape default check")

if problems:
    print("cape default check: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("cape default check: no cape is worn until the player equips one, and every cape UI can say none")
