"""Checks the version-to-renderer rule and the MobileGlues warning, without an Android device.

Two things are pinned here:

1. RendererDefaults picks LTW at 1.17 and newer and Emesis below it, for every shape of version id
   the launcher produces, and the launch path, the instance installer and the auto default all go
   through it instead of each carrying their own copy of the rule.
2. MobileGlues is never chosen silently: every picker that can write the "renderer" preference asks
   MobileGluesWarning first, and that warning says it is experimental and not recommended.

The Java class is compiled and run for real against small Android stubs, so the version parsing is
executed rather than pattern-matched: a regex that only looks right is exactly the kind of thing
that ships a 1.16.5 instance on LTW.
"""
import os
from pathlib import Path
import shlex
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
src = root / 'app_pojavlauncher/src/main/java'

STUBS = {
    'android/content/Context.java': 'package android.content; public class Context {}',
    'android/util/Log.java': '''
        package android.util;
        public class Log {
            public static int e(String a, String b) { return 0; }
            public static int i(String a, String b) { return 0; }
            public static int w(String a, String b) { return 0; }
            public static int w(String a, String b, Throwable t) { return 0; }
        }''',
    'net/kdt/pojavlaunch/Tools.java': '''
        package net.kdt.pojavlaunch;
        import java.io.File;
        public class Tools {
            public static String NATIVE_LIB_DIR = System.getProperty("aerix.nativeLibDir");
            public static boolean isValidString(String s) { return s != null && !s.isEmpty(); }
            public static JVersionList.Version getVersionInfo(String id) { return null; }
            public static JVersionList.Version getVersionInfo(String id, boolean skip) { return null; }
        }''',
    'net/kdt/pojavlaunch/JVersionList.java': '''
        package net.kdt.pojavlaunch;
        public class JVersionList {
            public static class Version {
                public String id;
                public String inheritsFrom;
                public String releaseTime;
            }
        }''',
    # The real DateUtils re-reads the version json from disk; the stub answers with the releaseTime
    # the test put on the Version object, which is all RendererDefaults asks it for.
    'net/kdt/pojavlaunch/utils/DateUtils.java': '''
        package net.kdt.pojavlaunch.utils;
        import java.util.Date;
        import java.util.GregorianCalendar;
        public class DateUtils {
            public static boolean dateBefore(java.util.Date date, int year, int month, int day) {
                return date.before(new Date(new GregorianCalendar(year, month, day).getTimeInMillis()));
            }
            public static Date getOriginalReleaseDate(net.kdt.pojavlaunch.JVersionList.Version v)
                    throws java.text.ParseException {
                return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v.releaseTime);
            }
        }''',
    'net/kdt/pojavlaunch/game/renderer/def/Renderers.java': '''
        package net.kdt.pojavlaunch.game.renderer.def;
        public final class Renderers {
            public static final String LTW_RENDERER = "opengles3_ltw";
            public static final String GL4ES_RENDERER = "opengles2";
            public static final String MOBILEGLUES_RENDERER = "opengles_mobileglues";
            public static final String EMESIS_RENDERER = "emesis";
        }''',
    'net/kdt/pojavlaunch/game/renderer/RenderSpec.java': '''
        package net.kdt.pojavlaunch.game.renderer;
        import android.content.Context;
        public interface RenderSpec {
            boolean compatibleDevice(Context context);
            String tag();
        }''',
    'net/kdt/pojavlaunch/game/renderer/GameRenderer.java': '''
        package net.kdt.pojavlaunch.game.renderer;
        import android.content.Context;
        public class GameRenderer {
            static class Spec implements RenderSpec {
                private final String tag;
                Spec(String tag) { this.tag = tag; }
                public boolean compatibleDevice(Context c) { return true; }
                public String tag() { return tag; }
            }
            public static RenderSpec getKnownRenderer(String renderer) {
                if (renderer == null) return null;
                return new Spec(renderer);
            }
        }''',
}

HARNESS = '''
import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.game.renderer.RendererDefaults;
import net.kdt.pojavlaunch.game.renderer.MobileGluesWarning;

public class RendererDefaultsSmoke {
    static int failures = 0;

    static void check(boolean ok, String what) {
        if (ok) return;
        failures++;
        System.out.println("FAIL: " + what);
    }

    static void expectLtw(String versionId) {
        String tag = RendererDefaults.tagForVersion(versionId);
        check("opengles3_ltw".equals(tag), versionId + " should pick LTW, picked " + tag);
        check(RendererDefaults.isLtwEra(null, versionId), versionId + " should be the LTW era");
    }

    static void expectEmesis(String versionId) {
        String tag = RendererDefaults.tagForVersion(versionId);
        check("emesis".equals(tag), versionId + " should pick Emesis, picked " + tag);
        check(!RendererDefaults.isLtwEra(null, versionId), versionId + " should be the Emesis era");
    }

    public static void main(String[] args) {
        // Plain vanilla releases on both sides of the line. 1.17 is inclusive on the LTW side.
        expectEmesis("1.16.5");
        expectEmesis("1.16");
        expectEmesis("1.12.2");
        expectEmesis("1.8.9");
        expectEmesis("1.7.10");
        expectLtw("1.17");
        expectLtw("1.17.1");
        expectLtw("1.18.2");
        expectLtw("1.20.1");
        expectLtw("1.21.4");
        expectLtw("1.21.11");

        // Loader profiles name the game after themselves; the loader's own 0.x must never win.
        expectEmesis("fabric-loader-0.16.9-1.16.5");
        expectLtw("fabric-loader-0.16.9-1.21.4");
        expectEmesis("1.16.5-forge-36.2.39");
        expectLtw("1.20.1-forge-47.2.0");
        expectEmesis("quilt-loader-0.24.0-1.16.4");
        expectLtw("quilt-loader-0.24.0-1.20.6");
        expectLtw("neoforge-21.1.77");
        expectLtw("1.20.1-OptiFine_HD_U_I6");
        expectEmesis("1.12.2-OptiFine_HD_U_G5");

        // A loader's build number is not a game version, and neither is a lone number.
        expectEmesis("bta-7.3");
        expectEmesis("");
        check(RendererDefaults.tagForVersion((String) null) != null,
                "a null version id still has to answer");

        // Snapshots carry no readable number: their release date decides, which is the same
        // boundary the core-context switch uses. DateUtils.dateBefore takes a ZERO-BASED month, so
        // 2021, 3, 9 is 9 April 2021 - and the fixture has to sit on the correct side of it.
        JVersionList.Version snapshot = new JVersionList.Version();
        snapshot.id = "24w14a";
        snapshot.releaseTime = "2021-06-08T00:00:00+00:00";
        check("opengles3_ltw".equals(RendererDefaults.tagForVersion(snapshot, "24w14a")),
                "a 1.17-cycle snapshot should pick LTW");
        snapshot.releaseTime = "2020-01-01T00:00:00+00:00";
        check("emesis".equals(RendererDefaults.tagForVersion(snapshot, "24w14a")),
                "a pre-1.17 snapshot should pick Emesis");
        // The boundary itself: the day before is Emesis, the day of is LTW.
        snapshot.releaseTime = "2021-04-08T00:00:00+00:00";
        check("emesis".equals(RendererDefaults.tagForVersion(snapshot, "24w14a")),
                "the day before the 1.17 boundary is still the Emesis era");
        snapshot.releaseTime = "2021-04-09T00:00:00+00:00";
        check("opengles3_ltw".equals(RendererDefaults.tagForVersion(snapshot, "24w14a")),
                "the boundary day itself is the LTW era");

        // The warning itself.
        check(MobileGluesWarning.applies("opengles_mobileglues"), "the warning applies to MobileGlues");
        check(!MobileGluesWarning.applies("emesis"), "the warning does not apply to Emesis");
        check(!MobileGluesWarning.applies(null), "the warning does not apply to nothing");
        check(MobileGluesWarning.message().toLowerCase().contains("experimental"),
                "the warning says it is experimental");
        check(MobileGluesWarning.message().toLowerCase().contains("don't recommend"),
                "the warning says it is not recommended");
        check(MobileGluesWarning.recommendedInstead("1.16.5").equals("emesis"),
                "the recommended alternative below 1.17 is Emesis");
        check(MobileGluesWarning.recommendedInstead("1.20.1").equals("opengles3_ltw"),
                "the recommended alternative at 1.17+ is LTW");
        // The button label is built from the tag the action stores, so the two cannot disagree.
        check(MobileGluesWarning.recommendedLabel("opengles3_ltw").contains("LTW"),
                "the alternative button names LTW when it stores LTW");
        check(MobileGluesWarning.recommendedLabel("emesis").contains("Emesis"),
                "the alternative button names Emesis when it stores Emesis");
        check(RendererDefaults.launchTagForVersion(null, "1.20.1").equals("opengles3_ltw")
                        || RendererDefaults.launchTagForVersion(null, "1.20.1").equals("emesis"),
                "the resolved alternative is always one of the two bundled renderers");

        if (failures > 0) {
            System.out.println(failures + " checks failed");
            System.exit(1);
        }
        System.out.println("Renderer version rule + MobileGlues warning smoke test passed");
    }
}
'''

with tempfile.TemporaryDirectory() as tmp:
    tmp = Path(tmp)
    for name, content in STUBS.items():
        target = tmp / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)
    (tmp / 'RendererDefaultsSmoke.java').write_text(HARNESS)
    paths = [str(p) for p in tmp.rglob('*.java')]
    paths += [
        str(src / 'net/kdt/pojavlaunch/game/renderer/RendererDefaults.java'),
        str(src / 'net/kdt/pojavlaunch/game/renderer/MobileGluesWarning.java'),
    ]
    subprocess.run(shlex.split(os.environ.get('JAVAC', 'javac'))
                   + ['-source', '8', '-target', '8', '-nowarn', '-d', str(tmp), *paths], check=True)
    subprocess.run(shlex.split(os.environ.get('JAVA', 'java'))
                   + ['-cp', str(tmp), 'RendererDefaultsSmoke'], check=True)

# --- wiring: the rule is applied where the renderer is actually chosen -----------------------------

runner = (src / 'net/kdt/pojavlaunch/utils/jre/GameRunner.java').read_text()
prefs = (src / 'net/kdt/pojavlaunch/prefs/LauncherPreferences.java').read_text()
create = (src / 'net/kdt/pojavlaunch/aerix/bridge/PojavCreateInstanceBridge.kt').read_text()
settings = (src / 'net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt').read_text()
editor = (src / 'net/kdt/pojavlaunch/aerix/ui/screens/InstanceEditorScreen.kt').read_text()
classic = (src / 'net/kdt/pojavlaunch/prefs/screens/LauncherPreferenceVideoFragment.java').read_text()
dialog = (src / 'net/kdt/pojavlaunch/aerix/ui/screens/MobileGluesWarningDialog.kt').read_text()

# The launch path asks the rule, and only for a renderer the player did not pin.
assert 'RendererDefaults.launchTagForVersion(versionInfo, versionId)' in runner, \
    'the launch path must ask RendererDefaults for the version renderer'
assert 'RendererDefaults.appliesToAutoSelection(renderer)' in runner, \
    'the version rule must not replace a renderer the player chose'
assert '!Tools.isValidString(instance.renderer)' in runner, \
    'an explicit per-instance renderer still wins'
# The old symptom-by-symptom switches survive as the last word, so they were not deleted.
assert 'isGl4esCompatible(versionInfo)' in runner and 'isCompatContext(versionInfo)' in runner

# A new instance is created with its version renderer already written.
assert 'RendererDefaults.launchTagForVersion(null, versionId)' in create, \
    'a newly created instance must carry the renderer its version calls for'
assert 'applyVersionRenderer(instance)' in create
# A FastClient profile is named after Fabric, not the game, so it pins the renderer from the build.
fast = (src / 'net/kdt/pojavlaunch/aerix/fastclient/FastClientInstaller.kt').read_text()
assert 'launchTagForVersion(null, build.minecraft)' in fast, \
    'a FastClient instance must carry the renderer its Minecraft build calls for'

# The launcher's own automatic pick is no longer MobileGlues.
default_body = prefs.split('public static String defaultRendererId')[1].split('\n    }')[0]
assert 'MOBILEGLUES_RENDERER' not in default_body, \
    'the automatic default must not be MobileGlues - it is the choice the launcher warns about'
assert 'RendererDefaults.isBundled' in default_body

# Every picker goes through the warning before it writes the preference. The renderer tile grid
# and the plugin cards were removed from the tab, so the renderer picker is the Global Renderer
# list: it is the only place left in Settings that can choose MobileGlues, and it must warn.
pickers = settings.count('MobileGluesWarning.applies')
assert pickers >= 1, 'the Global Renderer list must warn before it writes MobileGlues'
# The writes that follow the warning (confirm, and "use the recommended one") are the only other
# renderer writes on the page, and both are unreachable until the dialog has been answered.
writes = settings.count('setMiraiGlobalRenderer(')
assert writes <= 3, f'Settings gained an unwarned renderer write ({writes} total)'
assert settings.count('pendingMobileGlues') >= 3, 'the pick must be held, not applied, until asked'
assert 'MobileGluesWarningDialog(' in settings and 'MobileGluesWarningDialog(' in editor
assert 'MobileGluesWarning.applies(entry.backendRendererId)' in editor
assert 'setOnPreferenceChangeListener' in classic and 'showMobileGluesWarning' in classic
assert 'return false;' in classic.split('showMobileGluesWarning(chosen);')[1][:80], \
    'the classic list must refuse the write so the warning can ask first'
assert 'MobileGluesWarning.message()' in dialog

# The classic per-instance editor writes the renderer on Save, so that is where it asks.
fragment = (src / 'net/kdt/pojavlaunch/fragments/InstanceEditorFragment.java').read_text()
assert 'askAboutMobileGlues(this::saveAndLeave)' in fragment, \
    'saving an instance on MobileGlues must ask first'
assert 'MobileGluesWarning.message()' in fragment

print('Renderer defaults wiring smoke test passed')
