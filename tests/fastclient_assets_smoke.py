#!/usr/bin/env python3
"""Validate the creator-provided builds and their launcher integration without Android."""
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'modpacks/clients'
JAVA = ROOT / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch'
# version -> (sha256, minimum Fabric Loader, minecraft dependency, minimum java, java release)
BUILDS = {
    '26.3': ('abbccbf0b51a03593cc902f51074001c00a33aca6b3c739ea16d89318df4b97b', '>=0.19.5', '~26.3', '>=25', 25),
    '26.2': ('4dd9bf7655ac659f18372231d1b600a0bd5c47b361671d9bf64173441fe1e853', '>=0.19.3', '~26.2', '>=25', 25),
    '26.1.2': ('3d6d7c4b0d4155fb566510e3079ff9440c59a01c89c85e107a56c749afe2d26c', '>=0.18.5', '~26.1', '>=25', 25),
    '1.21.11': ('0606953355f851548fad0cd04adc06938ddf98ba7f435d96f5385a568d825420', '>=0.18.4', '~1.21.11', '>=21', 21),
    '1.21.4': ('42b6b99f78f7ecabf4c7db722941d6d5a64ae5316e9994d1331bd91f4af374ca', '>=0.18.4', '~1.21.4', '>=21', 21),
    '1.21.1': ('2c7620b0d1dc8e912119df40de8f3fa29a9f2034132e1a29a72920bc43106d1d', '>=0.18.4', '~1.21.1', '>=21', 21),
}
installer = (JAVA / 'aerix/fastclient/FastClientInstaller.kt').read_text()

# The jars are downloaded, never built into the APK: six builds would add ~28 MB to every install.
assets = ROOT / 'app_pojavlauncher/src/main/assets/components/fastclient'
assert not list(assets.glob('*.jar')), "FastClient jars must not be packaged in the APK"
assert 'context.assets.open(build.asset)' not in installer, "the client jar must not be read from assets"
assert 'downloadClient(build, client)' in installer, "the client jar must be downloaded"
assert 'BASE_URLS' in installer and 'fastclient-hud-' in installer

for version, (sha, loader, minecraft, java, java_level) in BUILDS.items():
    path = BASE / f'fastclient-hud-{version}.jar'
    assert path.is_file(), version
    assert hashlib.sha256(path.read_bytes()).hexdigest() == sha, version
    with zipfile.ZipFile(path) as jar:
        assert jar.testzip() is None, version
        metadata = json.loads(jar.read('fabric.mod.json'))
        # 3.0 renamed the mod id from "fasteclient" to "fastclient-hud"; the installer accepts
        # both, so a build from either series installs instead of reporting bad metadata.
        assert metadata['id'] in ('fastclient-hud', 'fasteclient'), metadata['id']
        assert metadata['depends']['fabricloader'] == loader, version
        assert metadata['depends']['minecraft'] == minecraft, version
        assert metadata['depends']['java'] == java, version
        assert metadata['depends']['fabric-api'] == '*'
        assert any(name.startswith('LICENSE_') for name in jar.namelist())
    assert sha in installer and f'FastClientBuild("{version}"' in installer, version
    assert f'FastClientBuild("{version}", "{loader[2:]}", {java_level},' in installer, version

manifest = json.loads((assets / 'performance-mods.json').read_text())
assert set(manifest) - {'_comment'} == set(BUILDS), set(manifest)
for version, mods in manifest.items():
    if version == '_comment':
        continue
    assert {m['id'] for m in mods} == {'lithium', 'entityculling'}
    for mod in mods:
        if 'project' in mod:
            # Resolved from Modrinth at install time, because the upstream release publishes no
            # digest to pin. The alternative - inventing a hash - would not verify anything.
            assert mod['mcVersion'] == version
        else:
            assert mod['url'].startswith('https://github.com/') and len(mod['sha256']) == 64, mod
assert 'downloadPerformanceMods(context, build.minecraft, staging)' in installer
assert 'downloadModrinthMod(' in installer
assert 'performanceMods.forEach' in installer
assert 'instance.sharedData = false' in installer
assert 'instance.isAerixClient = false' in installer
assert 'overwrite = false' in installer
assert 'SHA-512' in installer and 'cdn.modrinth.com' in installer

# Each build's own armorial is written onto its instance, so six installs do not all wear one mark.
assert "CLIENT_MOD_IDS" in installer, 'the installer must check the jar is really FastClient'
assert '"fastclient-hud"' in installer, 'the 3.0 mod id must be accepted'
assert 'assets/fastclient-hud/icon.png' in installer, "the jar's own logo must become the instance icon"
provider = (JAVA / 'instances/InstanceIconProvider.java').read_text()
assert 'R.drawable.fastclient_instance' in provider and 'fastClientVersion' in provider
assert 'LauncherSection.FAST_CLIENT' not in (JAVA / 'aerix/ui/AerixNavigationRail.kt').read_text()

assert 'ClientHubScreen(activity, clientOwnerPage)' in (JAVA / 'aerix/ui/AerixLauncherScreen.kt').read_text()
hub = (JAVA / 'aerix/ui/screens/ClientHubScreen.kt').read_text()
assert 'ClientOwnerPage.MX_INSPIRE -> FastClientScreen(activity, onBack = back)' in hub
assert 'ClientOwnerPage.SAHIL -> AerixClientScreen(activity, onBack = back)' in hub
assert 'onClick = { onPageChange(ClientOwnerPage.SAHIL) }' in hub
assert 'onClick = { onPageChange(ClientOwnerPage.MX_INSPIRE) }' in hub
assert 'BackHandler(enabled = page != ClientOwnerPage.OWNERS' in hub
assert 'Modifier.size(76.dp).clip(CircleShape)' in hub
assert 'label = "Aerix client"' in hub and 'label = "Fast client 3.0"' in hub

# The bundled static mark is the creator's current artwork, and it is the fallback only: it is one
# image shared by six builds, so a per-instance icon is what actually shows.
icon = (ROOT / 'app_pojavlauncher/src/main/res/drawable-nodpi/fastclient_instance.png').read_bytes()
with zipfile.ZipFile(BASE / 'fastclient-hud-26.3.jar') as jar:
    assert icon == jar.read('assets/fastclient-hud/icon.png')

for name in ('client_owner_sahil.jpg', 'client_owner_mx_inspire.jpg'):
    data = (ROOT / 'app_pojavlauncher/src/main/res/drawable-nodpi' / name).read_bytes()
    assert data[:2] == b'\xff\xd8' and len(data) > 1000, name
for name in ('AerixClientScreen.kt', 'FastClientScreen.kt'):
    assert 'ClientOwnerBackButton(onBack)' in (JAVA / 'aerix/ui/screens' / name).read_text()
screen = (JAVA / 'aerix/ui/screens/FastClientScreen.kt').read_text()
assert 'FastClientInstaller.install(activity, build)' in screen
assert 'PojavLaunchBridge.play(activity)' in screen
assert 'Thanks Mx inspire for supporting and giving us permission to use their client' in screen
assert 'https://m.youtube.com/channel/UCjC7cPjoCxQycsw0DITzLQA' in screen
assert 'https://discord.gg/Z6aCUNfPJH' in screen
print('FastClient asset integrity and navigation smoke test passed')
