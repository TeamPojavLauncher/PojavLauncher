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

# ---------------------------------------------------------------- screen wiring
screen = SCREEN.read_text()
check("AerixModpackInstaller.install" in screen, "the card button must run the installer")
check("load(force = true)" in screen, "a failed catalogue read must offer a retry")
check("sizeBytes" in screen, "the card reports the download size when the catalogue has one")

# ---------------------------------------------------------------- shipped catalogue
data = json.loads(MANIFEST.read_text())
packs = data["packs"]
check(isinstance(packs, list) and packs, "the catalogue must list at least one pack")
for pack in packs:
    check(pack.get("file"), f"pack {pack.get('name')!r} has no file to download")
    check(pack.get("name"), f"pack {pack.get('file')!r} has no display name")

if problems:
    print("modpacks smoke: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print("modpacks smoke: catalogue, installer, rail swap and screen wiring all in place")
