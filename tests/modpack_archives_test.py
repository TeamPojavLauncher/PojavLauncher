#!/usr/bin/env python3
"""Check every catalogued Team Aerix pack archive the way the launcher will read it.

The failures this exists to catch are the ones that only show up on the device, in the game, with
no useful message: a mod folder with two jars of the same mod (Fabric Loader refuses to start), a
mod on the wrong Minecraft version (mixin crash on launch), or a missing dependency (the game boots
into an error screen). All three are cheap to detect here and expensive to discover in a bug report.
"""
import io
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOGUE = json.loads((ROOT / "modpacks/modpacks.json").read_text())
# Provided by the loader and the game itself, or by the Fabric API jar the installer downloads.
BUILT_IN = {"minecraft", "fabricloader", "java", "fabric"}

problems = []


def check(condition, message):
    if not condition:
        problems.append(message)


def parts(version):
    """Comparable tuple for a Minecraft-style version: 1.21 -> (1, 21, 0), 26.2 -> (26, 2, 0)."""
    numbers = []
    for piece in str(version).split("-")[0].split("."):
        numbers.append(int(piece) if piece.isdigit() else 0)
    while len(numbers) < 3:
        numbers.append(0)
    return tuple(numbers[:3])


def satisfies(version, predicate):
    """Whether a Minecraft version satisfies a Fabric version predicate.

    Supports what mods actually publish: `*`, an exact version, `1.21.x` wildcards, `~1.21` and
    `^1.21`, and comparisons (`>=`, `<=`, `>`, `<`, `=`), combined with spaces (all must hold).
    A list is an OR of predicates, which is how Fabric reads it too.
    """
    if isinstance(predicate, list):
        return any(satisfies(version, item) for item in predicate)
    predicate = str(predicate).strip()
    if predicate in ("", "*"):
        return True
    have = parts(version)
    for token in predicate.split():
        if token in ("*", ""):
            continue
        # Fabric allows a suffix after the numbers - ">=1.20-", ">=1.20.2-beta.2", "<1.21.2-" -
        # where the trailing "-" means "any patch of", so the numbers before it are what matters.
        token = token.split("-")[0].rstrip("+.")
        match = re.fullmatch(r"(>=|<=|>|<|=|~|\^)?\s*([0-9]+(?:\.[0-9]+)*(?:\.[xX*])?)", token)
        if not match:
            return False
        operator, target = match.group(1) or "=", match.group(2)
        if target.endswith((".x", ".X", ".x", ".*")):
            base = parts(target.rsplit(".", 1)[0] + ".0")
            if not (have[:2] == base[:2]):
                return False
            continue
        want = parts(target)
        if operator == "=":
            # Fabric treats 1.21 as 1.21.0 for comparison, so a patch release satisfies it.
            if have != want:
                return False
        elif operator == ">=":
            if not have >= want:
                return False
        elif operator == "<=":
            if not have <= want:
                return False
        elif operator == ">":
            if not have > want:
                return False
        elif operator == "<":
            if not have < want:
                return False
        elif operator in ("~", "^"):
            if not have >= want:
                return False
            # ~1.21 stays inside 1.21; ^1.21 allows any 1.x.
            ceiling = (want[0], want[1] + 1, 0) if operator == "~" else (want[0] + 1, 0, 0)
            if not have < ceiling:
                return False
    return True


def metadata(jar_bytes):
    with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
        return json.loads(jar.read("fabric.mod.json"))


for pack in CATALOGUE["packs"]:
    for version in pack.get("versions") or [pack]:
        name = version.get("file") or ""
        if not name or name.startswith("http"):
            continue
        archive = ROOT / "modpacks" / name
        check(archive.is_file(), f"{pack['id']}: {name} is not committed")
        if not archive.is_file():
            continue

        with zipfile.ZipFile(archive) as zip_file:
            entries = [n for n in zip_file.namelist() if not n.endswith("/")]
            roots = {n.split("/")[0] for n in entries}
            check(len(roots) == 1, f"{name}: expected one instance folder, found {sorted(roots)[:4]}")

            manifests = [n for n in entries if n.endswith("mojo_instance.json")]
            check(manifests, f"{name}: no mojo_instance.json, so the launcher cannot install it")
            if not manifests:
                continue
            manifest = json.loads(zip_file.read(manifests[0]))
            version_id = manifest.get("versionId", "")
            check(re.fullmatch(r"fabric-loader-\d+\.\d+\.\d+-\d+\.\d+(\.\d+)?", version_id),
                  f"{name}: versionId {version_id!r} is not fabric-loader-<loader>-<game>")
            mc_version = version_id.split("-")[-1] if version_id else ""
            check(version.get("mcVersion") in (None, "", mc_version),
                  f"{name}: catalogue says {version.get('mcVersion')}, archive says {mc_version}")

            mod_entries = [n for n in entries if "/mods/" in n and n.endswith(".jar")]
            check(mod_entries, f"{name}: ships no mods at all")

            mods, nested_ids, about = {}, set(), {}
            for entry in mod_entries:
                short = entry.split("/")[-1]
                try:
                    info = metadata(zip_file.read(entry))
                except KeyError:
                    problems.append(f"{name}: {short} is not a Fabric mod")
                    continue
                mod_id = info.get("id")
                if mod_id in mods:
                    problems.append(
                        f"{name}: two jars declare the mod id {mod_id!r} ({mods[mod_id]} and {short}) "
                        f"- Fabric Loader will refuse to start")
                mods[mod_id], about[mod_id] = short, info
                # A jar can bundle other mods (JiJ); the loader flattens those into the mod list, so
                # they satisfy dependencies for every mod, not only for their parent. They live
                # inside the outer jar, so they are read from there and not from the archive.
                with zipfile.ZipFile(io.BytesIO(zip_file.read(entry))) as nested:
                    for bundled in info.get("jars", []):
                        try:
                            nested_ids.add(metadata(nested.read(bundled["file"]))["id"])
                        except KeyError:
                            pass

            for mod_id, info in about.items():
                declared = (info.get("depends") or {}).get("minecraft")
                if declared is not None and not satisfies(mc_version, declared):
                    problems.append(
                        f"{name}: {mod_id} {info.get('version')} declares minecraft {declared!r}, "
                        f"which {mc_version} does not satisfy")
                # A dependency of "minecraft" already declared is the point above.
                for dependency in (info.get("depends") or {}):
                    if dependency in BUILT_IN or dependency in mods or dependency in nested_ids:
                        continue
                    # Fabric API ships every one of its modules in the single jar installed with
                    # the pack, and each module's id is "fabric-*".
                    if dependency.startswith("fabric-"):
                        continue
                    problems.append(f"{name}: {mod_id} needs {dependency}, which neither the pack nor "
                                    f"Fabric API provides")

if problems:
    print("modpack archive check: FAILED")
    for problem in problems:
        print("  -", problem)
    sys.exit(1)

print(f"modpack archive check: {len(CATALOGUE['packs'])} packs readable, no duplicate mods, no "
      f"wrong-version mods, no missing dependencies")
