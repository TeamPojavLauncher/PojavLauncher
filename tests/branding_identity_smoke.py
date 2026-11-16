"""Checks public branding and package configuration without an Android SDK."""
from pathlib import Path
import struct
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher"
RES = APP / "src/main/res"
ACTIVE_PACKAGES = {
    "net.al.aerixlauncher",
    "net.al.aerixlauncher.debug",
    "net.al.aerixlauncher.pub",
}
LEGACY_PACKAGES = {
    "net.kdt.pojavlaunch",
    "net.kdt.pojavlaunch.debug",
    "net.kdt.pojavlaunch.pub",
}


class BrandingIdentitySmoke(unittest.TestCase):
    def test_application_ids_and_provider_authorities(self):
        gradle = (APP / "build.gradle").read_text()
        self.assertIn('applicationId "net.al.aerixlauncher"', gradle)
        self.assertNotIn('applicationId "net.kdt.pojavlaunch"', gradle)
        for package in ACTIVE_PACKAGES:
            self.assertIn("'application_package', '" + package + "'", gradle)
        for authority in (
            "net.al.aerixlauncher.scoped.gamefolder",
            "net.al.aerixlauncher.scoped.gamefolder.debug",
            "net.al.aerixlauncher.pub.scoped.gamefolder",
        ):
            self.assertIn("'storageProviderAuthorities', '" + authority + "'", gradle)
        # The source namespace remains stable; applicationId controls the installed APK identity.
        self.assertIn("namespace 'net.kdt.pojavlaunch'", gradle)

        debug_workflow = (ROOT / ".github/workflows/aerix-debug.yml").read_text()
        self.assertIn("applicationId net.al.aerixlauncher.debug confirmed", debug_workflow)
        self.assertIn("package: net.al.aerixlauncher.debug", debug_workflow)
        play_workflow = (ROOT / ".github/workflows/android.yml").read_text()
        self.assertIn("packageName: net.al.aerixlauncher", play_workflow)

    def test_existing_signing_and_debug_version_configuration(self):
        gradle = (APP / "build.gradle").read_text()
        self.assertIn('storeFile file("debug.keystore")', gradle)
        # The release key must NOT be named as a file in the tree. `storeFile file("aerix_release.p12")`
        # used to sit here, and this test asserted it - the suite was enforcing the leak, which is how
        # a shipped key stays public long after anyone remembers it was committed. A pinned assertion
        # is only as good as the judgement behind it.
        self.assertNotIn('storeFile file("aerix_release.p12")', gradle)
        self.assertIn('System.getenv("AERIX_KEYSTORE_FILE")', gradle)
        self.assertIn("signingConfig signingConfigs.customDebug", gradle)
        self.assertIn("signingConfig signingConfigs.aerixRelease", gradle)
        self.assertIn('onVariants(selector().withBuildType("debug"))', gradle)
        self.assertIn("output.versionCode.set(AERIX_DEBUG_VERSION_CODE)", gradle)

    def test_current_and_previous_apps_can_still_be_discovered(self):
        manifest = ET.parse(APP / "src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        packages = {p.get(ns + "name") for p in manifest.findall("./queries/package")}
        self.assertTrue((ACTIVE_PACKAGES | LEGACY_PACKAGES) <= packages)

        activity = (APP / "src/main/java/net/kdt/pojavlaunch/LauncherActivity.java").read_text()
        for package in LEGACY_PACKAGES:
            self.assertIn(f'"{package}"', activity)
        for package in ACTIVE_PACKAGES:
            self.assertNotIn(f'"{package}"', activity)
        self.assertIn("getLaunchIntentForPackage(packageName)", activity)
        self.assertIn("R.string.migration_notice", activity)

        angle_provider = (
            APP / "src/main/java/net/kdt/pojavlaunch/mobileglues/settings/AngleProvider.kt"
        ).read_text()
        for package in ACTIVE_PACKAGES | LEGACY_PACKAGES:
            self.assertIn(f'"{package}"', angle_provider)

    def test_localized_app_labels_and_notifications(self):
        count = 0
        visible_keys = {"app_name", "app_short_name", "error_fatal", "lazy_service_default_title",
                        "storage_required", "modpack_install_notification_title", "notification_permission_dialog_text",
                        "forge_dl_no_installer", "create_profile_modded_versions"}
        for file in RES.glob("values*/strings.xml"):
            root = ET.parse(file).getroot()
            for item in root.findall("string"):
                if item.get("name") not in visible_keys:
                    continue
                text = "".join(item.itertext())
                self.assertNotRegex(text.lower(), r"pojav", f"{file}: {item.get('name')}")
            count += 1
        self.assertGreaterEqual(count, 50)
        strings = ET.parse(RES / "values/strings.xml").getroot()
        self.assertEqual(strings.find("string[@name='lazy_service_default_title']").text,
                         "Aerix Foreground Service")

    def test_notification_icon_is_used_and_valid_png(self):
        for service in ("GameService", "ProgressService"):
            code = (APP / f"src/main/java/net/kdt/pojavlaunch/services/{service}.java").read_text()
            self.assertIn("setSmallIcon(R.drawable.notif_icon)", code)
            self.assertIn("R.string.lazy_service_default_title", code)
        data = (RES / "drawable-nodpi/notif_icon.png").read_bytes()
        self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
        self.assertEqual(struct.unpack(">II", data[16:24]), (96, 96))


if __name__ == "__main__":
    unittest.main()
