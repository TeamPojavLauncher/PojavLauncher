#!/usr/bin/env python3
"""Fetch the exact upstream jars on CI and verify metadata, integrity and bundled dependencies.

Each entry is checked independently and a failure is reported as an ``::error`` annotation naming
the build and the mod, because this runs in CI where the only thing anyone reads is the annotation
list - a bare ``assert`` there says nothing but "exit code 1".
"""
import hashlib
import io
import json
import time
import traceback
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'app_pojavlauncher/src/main/assets/components/fastclient/performance-mods.json').read_text())
BUILDS = {'26.3', '26.2', '26.1.2', '1.21.11', '1.21.4', '1.21.1'}
BUILT_IN = {'minecraft', 'java', 'fabricloader', 'fabric-api', 'fabric'}

failures = []


def fail(title, message):
    failures.append(f'{title}: {message}')
    print(f'::error title={title}::{message}')


def fetch(url):
    """The response body, or None when the upstream host is having a bad day (reported as a warning)."""
    request = Request(url, headers={'User-Agent': 'AerixLauncher/CI'})
    for attempt in range(3):
        try:
            with urlopen(request, timeout=45) as response:
                return response.read(40_000_001)
        except Exception as exc:
            if attempt == 2:
                if isinstance(exc, HTTPError) and exc.code in (502, 503, 504):
                    print(f'::warning title=FastClient upstream unavailable::{url} returned HTTP {exc.code}; skipped')
                    return None
                raise
            time.sleep(2 * (attempt + 1))
    return None


if set(manifest) - {'_comment'} != BUILDS:
    fail('FastClient manifest', f'expected {sorted(BUILDS)}, found {sorted(set(manifest) - {"_comment"})}')

for game in sorted(manifest):
    if game == '_comment':
        continue
    entries = manifest[game]
    if {entry['id'] for entry in entries} != {'lithium', 'entityculling'}:
        fail(f'FastClient {game}', f'expected lithium and entityculling, found {[e["id"] for e in entries]}')
        continue
    for entry in entries:
        title = f'FastClient {game} {entry["id"]}'
        try:
            # Some upstream releases publish no asset digest, so the manifest may resolve those from
            # Modrinth at install time instead. Those are checked for shape only.
            if 'project' in entry:
                assert entry['project'] in {'lithium', 'entityculling'}, entry
                assert entry['mcVersion'], entry
                print(f'::notice title={title}::resolved from Modrinth at install time ({entry["mcVersion"]})')
                continue
            data = fetch(entry['url'])
            if data is None:
                continue
            assert len(data) <= 40_000_000, f'{len(data)} bytes is larger than the 40 MB cap'
            actual = hashlib.sha256(data).hexdigest()
            assert actual == entry['sha256'], (
                f'sha256 {actual} does not match the pinned {entry["sha256"]} for {entry["url"]}')
            with ZipFile(io.BytesIO(data)) as jar:
                metadata = json.loads(jar.read('fabric.mod.json'))
                assert metadata['id'] == entry['id'], f'jar declares {metadata.get("id")}'
                nested_ids = set()
                for nested in metadata.get('jars', []):
                    with ZipFile(io.BytesIO(jar.read(nested['file']))) as nested_jar:
                        nested_ids.add(json.loads(nested_jar.read('fabric.mod.json'))['id'])
                required = set(metadata.get('depends', {}))
                unknown = required - BUILT_IN - nested_ids
                assert not unknown, (
                    f'{metadata.get("version")} declares {sorted(unknown)}, which neither Fabric API '
                    f'nor a bundled jar provides (bundled: {sorted(nested_ids)})')
                print(f'::notice title={title}::{metadata.get("version")} '
                      f'depends={json.dumps(metadata.get("depends", {}), separators=(",", ":"))} '
                      f'nested={",".join(sorted(nested_ids))}')
        except Exception as exc:
            fail(title, f'{type(exc).__name__}: {exc}')
            traceback.print_exc()

if failures:
    print(f'FastClient download verification FAILED ({len(failures)} problems)')
    for problem in failures:
        print('  -', problem)
    raise SystemExit(1)
print('Pinned FastClient performance mod downloads verified')
