#!/usr/bin/env python3
"""Fetch the exact upstream jars on CI and verify metadata, integrity and bundled dependencies."""
import hashlib
import io
import json
import time
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'app_pojavlauncher/src/main/assets/components/fastclient/performance-mods.json').read_text())
BUILDS = {'26.3', '26.2', '26.1.2', '1.21.11', '1.21.4', '1.21.1'}
assert set(manifest) - {'_comment'} == BUILDS, set(manifest)
for game, entries in manifest.items():
    if game == '_comment':
        continue
    assert {entry['id'] for entry in entries} == {'lithium', 'entityculling'}
    for entry in entries:
        # Some upstream releases publish no asset digest, so the manifest may resolve them from
        # Modrinth at install time instead. Those are checked for shape only; the pinned ones are
        # downloaded and hashed here.
        if 'project' in entry:
            assert entry['project'] in {'lithium', 'entityculling'}, entry
            assert entry['mcVersion'], entry
            continue
        req = Request(entry['url'], headers={'User-Agent': 'AerixLauncher/CI'})
        data = None
        for attempt in range(3):
            try:
                with urlopen(req, timeout=45) as response:
                    data = response.read(40_000_001)
                break
            except Exception as exc:
                if attempt == 2:
                    if isinstance(exc, HTTPError) and exc.code in (502, 503, 504):
                        print(f"::warning title=FastClient upstream unavailable::{game} {entry['id']} returned HTTP {exc.code}; artifact check skipped")
                        break
                    print(f"::error title=FastClient download::{game} {entry['id']} failed: {type(exc).__name__} {exc}")
                    raise
                time.sleep(2 * (attempt + 1))
        if data is None:
            continue
        assert len(data) <= 40_000_000
        actual = hashlib.sha256(data).hexdigest()
        assert actual == entry['sha256'], (
            f"{game} {entry['id']}: sha256 {actual} does not match the pinned "
            f"{entry['sha256']} for {entry['url']}")
        with ZipFile(io.BytesIO(data)) as jar:
            metadata = json.loads(jar.read('fabric.mod.json'))
            assert metadata['id'] == entry['id'], (game, entry['id'], metadata.get('id'))
            nested_ids = set()
            for nested in metadata.get('jars', []):
                with ZipFile(io.BytesIO(jar.read(nested['file']))) as nested_jar:
                    nested_ids.add(json.loads(nested_jar.read('fabric.mod.json'))['id'])
            required = set(metadata.get('depends', {}))
            unknown = required - {'minecraft', 'java', 'fabricloader', 'fabric-api', 'fabric'} - nested_ids
            assert not unknown, (
                f"{game} {entry['id']} {metadata.get('version')} declares {sorted(unknown)}, "
                f"which neither Fabric API nor a bundled jar provides")
            print(f"::notice title=FastClient mod metadata::{game} {entry['id']} {metadata.get('version')} depends={json.dumps(metadata.get('depends', {}), separators=(',', ':'))} nested={','.join(sorted(nested_ids))}")
print('Pinned FastClient performance mod downloads verified')
