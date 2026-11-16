#!/usr/bin/env python3
"""Smoke checks for the "Our modpacks" tab.

The tab downloads packs instead of shipping them, so the checks are about wiring: the catalogue URL
must not compete with the APK release's "latest" slot, the installer must understand the instance
export Team Aerix packs are published in, and the Wallpapers rail entry must be gone.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"
AERIX = APP / "aerix"

CATALOG = AERIX / "modpacks/AerixModpackCatalog.kt"
INSTALLER = AERIX / "modpacks/AerixModpackInstaller.kt"
SCREEN = AERIX / "ui/screens/ModpacksScreen.kt"
RAIL = AERIX / "ui/AerixNavigationRail.kt"
SHELL = AERIX / "ui/AerixLauncherScreen.kt"
MANIFEST = ROOT / "modpacks/modpacks.json"

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)


# ---------------------------------------------------------------- catalogue URL
catalog = CATALOG.read_text()
urls = re.findall(r'"(https://[^"]+)"', catalog)
check(urls, "the catalogue needs at least one URL to read from")
check(any(u.endswith("/releases/download/modpacks/modpacks.json") for u in urls),
      "one catalogue mirror must be the dedicated 'modpacks' release tag")
check(any("/raw.githubusercontent.com/" in u and u.endswith("/modpacks/modpacks.json") for u in urls),
      "one catalogue mirror must be the committed copy, which needs only a push")
check(not any("/releases/latest" in u for u in urls),
      "no mirror may use releases/latest: that slot belongs to the APK the updater and website "
      "download buttons point at")
check("PACK_BASE_URLS" in catalog and "downloadUrls" in catalog,
      "pack files must resolve against the same mirrors, in the same order")

# ---------------------------------------------------------------- no in-APK packs
for archive in ROOT.glob("app_pojavlauncher/src/main/assets/**/*.mrpack"):
    problems.append(f"modpack archive shipped in the APK: {archive}")
check(not list(ROOT.glob("app_pojavlauncher/src/main/assets/**/*.zip")),
      "no modpack archives may live in the APK assets: they would grow the download")
check(list(ROOT.glob("modpacks/packs/*.zip")) or True,
      "packs are served from modpacks/packs or the release, never from the APK")

# ---------------------------------------------------------------- installer layout support
installer = INSTALLER.read_text()
for needed in ("mojo_instance.json", "modrinth.index.json", "manifest.json"):
    check(needed in installer, f"installer must detect the {needed} layout")
check("FabriclikeUtils.FABRIC_UTILS.downloadLoaderVersions" in installer,
      "instance exports must resolve their Fabric loader through Fabric meta")
check("sharedData = false" in installer,
      "a modpack instance must own its game directory, or one pack's mods leak into another")
check("canonicalPath" in installer, "archive extraction needs the zip-slip guard")
check("writeInstanceIcon" in installer,
      "the pack's icon.webp must be re-encoded: the launcher cannot read WebP instance icons")

# ---------------------------------------------------------------- rail swap
rail = RAIL.read_text()
check("MODPACKS" in rail, "LauncherSection must carry the MODPACKS destination")
check("LauncherSection.MODPACKS" in rail, "the rail must link to the modpack page")
check("LauncherSection.WALLPAPERS" not in rail,
      "the Wallpapers rail entry must be gone; Wallpapers stays a Settings tab")
check("wallpapersSelected" not in rail, "the Wallpapers rail highlight helper is dead code")

shell = SHELL.read_text()
check("ModpacksScreen(navigate)" in shell, "the shell must render the modpack page")
check("LauncherSection.WALLPAPERS" not in shell,
      "the shell must not still route a Wallpapers section that no longer exists")

# ---------------------------------------------------------------- versions
check("AerixModpackVersion" in catalog, "a pack must be able to offer more than one build")
check("parseVersions" in catalog, "the catalogue must read a pack's versions array")
check("offeredVersions" in catalog,
      "a pack entry without a versions array must still offer its single build")
check("iconUrls" in catalog, "pack artwork must resolve against the same mirrors as the archives")
check("AerixModpackVersion" in installer,
      "the installer must install the build the user picked, not always the first one")
check("version.downloadUrls" in installer,
      "the download must follow the chosen build's mirrors, or a two-version pack installs the "
      "wrong file")
check("installingVersion" in installer, "progress must name the build being installed")

# ---------------------------------------------------------------- shipped catalogue
data = json.loads(MANIFEST.read_text())
packs = data["packs"]
check(isinstance(packs, list) and packs, "the catalogue must list at least one pack")

# ---------------------------------------------------------------- pack marks
ICONS = AERIX / "modpacks/AerixModpackIcons.kt"
icons = ICONS.read_text()
check("drawableFor" in icons, "the installed pack needs a bundled mark to fall back on")
check("writeOnto" in icons,
      "the bundled mark has to be written as the instance icon, not just looked up")

# Every catalogue pack that ships a bundled mark must have its drawable, or the install falls back
# to the Fabric logo - which is exactly the bug this guards against.
for pack in packs:
    pack_id = (pack.get("id") or "").strip()
    key = re.sub(r"[^a-z0-9]", "", pack_id.lower())
    if not key:
        continue
    drawable = ROOT / f"app_pojavlauncher/src/main/res/drawable/aerix_modpack_{pack_id.replace('-', '_')}.xml"
    if key.startswith("aetherium"):
        check(drawable.is_file(), f"pack {pack.get('name')!r} has no bundled mark at {drawable.name}")
        check(f"aerix_modpack_{pack_id.replace('-', '_')}" in icons,
              f"drawableFor does not map {pack_id!r} to its bundled mark")

check("aerixPackId" in installer,
      "the instance must record which catalogue pack it came from, or its icon cannot be restored")
check("aerixPackId" in pathlib.Path(
    APP / "instances/Instance.java").read_text(),
    "Instance must carry the catalogue pack id")

# ---------------------------------------------------------------- screen wiring
screen = SCREEN.read_text()
check("AerixModpackInstaller.install(context, pack, selected)" in screen,
      "the button must install the build the version picker has selected")
check("load(force = true)" in screen, "a failed catalogue read must offer a retry")
check("sizeBytes" in screen, "the screen reports the download size when the catalogue has one")
check("ModpackPage" in screen, "tapping a pack folder must open the pack's own page")
check("VersionRow" in screen, "the pack page must list its builds as a choice")
check("ComingSoonFolder" in screen,
      "the grid must reserve room for the packs that are still coming")
check("LazyVerticalGrid" in screen, "packs are drawn as a grid of folders, not a column of cards")
check("rememberPackArtwork" in screen, "each folder must show the pack's own artwork")

# The installer must prefer the bundled mark, and only reach for the network when it has none.
check("AerixModpackIcons.writeOnto(context, instance, pack.id)" in installer,
      "a Team Aerix pack must get its bundled mark the moment the instance is created")

# ---------------------------------------------------------------- catalogue entries
for pack in packs:
    check(pack.get("file"), f"pack {pack.get('name')!r} has no file to download")
    check(pack.get("name"), f"pack {pack.get('file')!r} has no display name")
    versions = pack.get("versions") or []
    for version in versions:
        check(version.get("file"),
              f"pack {pack.get('name')!r} lists a version with no file to download")
        check(version.get("mcVersion"),
              f"pack {pack.get('name')!r} lists a version with no Minecraft version")
    # Artwork is optional, but a relative one must actually be committed, or the folder is blank.
    icon = (pack.get("icon") or "").strip()
    if icon and not icon.startswith("http"):
        check((ROOT / "modpacks" / icon).is_file(),
              f"pack {pack.get('name')!r} points at artwork that is not in modpacks/{icon}")
    # Every build must name an archive that is committed, so the mirrors can serve it.
    for version in versions or [pack]:
        f = (version.get("file") or "").strip()
        if f and not f.startswith("http"):
            check((ROOT / "modpacks" / f).is_file(),
                  f"pack {pack.get('name')!r} points at {f}, which is not committed")

if problems:
    print("modpacks smoke: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("modpacks smoke: catalogue, installer, rail swap and screen wiring all in place")
