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
check('equippedCapeId = p.getString("equippedCapeId", CAPE_NONE) ?: CAPE_NONE' in store,
      "reading the preference must default to none as well")
check('crimson_sovereign' not in store,
      "no specific cape may be hardcoded as the starting choice")

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

if problems:
    print("cape default check: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("cape default check: no cape is worn until the player equips one, and every cape UI can say none")
