package net.kdt.pojavlaunch.game.renderer;

import android.util.Log;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.utils.DateUtils;

import java.io.File;
import java.util.Locale;

/**
 * Which renderer a game version runs on when the player did not pin one.
 *
 * The rule is written once, here, instead of being repeated in the settings page, the instance
 * editor and the launch path with three chances to disagree:
 *
 * <ul>
 *   <li><b>1.17 and newer</b> start on {@code LTW} - the OpenGL 3 core renderer built for that era.</li>
 *   <li><b>below 1.17</b> start on {@code Emesis} - one library that loads a gl4es core for the old
 *       versions, so it covers 1.16.5 and everything under it.</li>
 * </ul>
 *
 * This only ever supplies a <i>default</i>. A renderer the player picked - per instance or globally -
 * is never overridden by this class; {@code GameRunner} asks about explicit choices first.
 */
public final class RendererDefaults {
    private static final String TAG = "RendererDefaults";

    /**
     * The first major version that wants LTW: 1.17. The same boundary {@code GameRunner} uses for the
     * OpenGL 3 core context switch (the day before 21w10a, its first snapshot).
     */
    public static final int FIRST_LTW_MAJOR = 1;
    public static final int FIRST_LTW_MINOR = 17;

    private RendererDefaults() {
    }

    /**
     * The renderer a version id should start on when nobody chose one.
     *
     * @param versionId the instance's version id ("1.16.5", "1.20.1-forge-47.2.0", ...), may be null
     * @return {@link Renderers#LTW_RENDERER} for 1.17+, {@link Renderers#EMESIS_RENDERER} below it
     */
    public static String tagForVersion(String versionId) {
        return isLtwEra(null, versionId) ? Renderers.LTW_RENDERER : Renderers.EMESIS_RENDERER;
    }

    /**
     * The renderer a version should start on when nobody chose one, preferring what the version's own
     * metadata says over what its name suggests.
     *
     * A snapshot id ("24w14a") carries no number this can read, but its json carries its release date,
     * and the date is the same boundary the core-context switch uses.
     *
     * @param version metadata of the version, or null when it could not be read
     * @param versionId the id it was read from, used whenever the metadata cannot answer
     */
    public static String tagForVersion(JVersionList.Version version, String versionId) {
        return isLtwEra(version, versionId) ? Renderers.LTW_RENDERER : Renderers.EMESIS_RENDERER;
    }

    /** Whether a version belongs to the 1.17+ (OpenGL 3 core) era. */
    public static boolean isLtwEra(JVersionList.Version version, String versionId) {
        if (version != null) {
            try {
                // 1.17 is the first release that requires a core profile, so "core context" is
                // exactly the same question, answered from the game's original release date.
                return !DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2021, 3, 9);
            } catch (Throwable error) {
                // A version that is not installed yet, or a json that cannot be re-read, still has
                // its number in the id.
                Log.w(TAG, "No release date for " + versionId
                        + "; deciding the renderer from the version name", error);
            }
        }
        String number = minecraftNumber(versionId);
        if (number == null) {
            // Nothing to read at all: FastClient and BTA profiles name their own version. 1.17 is
            // where the compatibility profile stops being the safe answer, so an unknown version
            // keeps the renderer that still speaks it.
            return false;
        }
        int[] parts = parseMajorMinor(number);
        if (parts == null) return false;
        return parts[0] > FIRST_LTW_MAJOR
                || (parts[0] == FIRST_LTW_MAJOR && parts[1] >= FIRST_LTW_MINOR);
    }

    /**
     * The Minecraft number inside a version id, in the shapes the launcher produces:
     * {@code 1.16.5}, {@code 1.20.1-forge-47.2.0}, {@code fabric-loader-0.16.9-1.21.4},
     * {@code quilt-loader-0.24.0-1.20.6}, {@code neoforge-21.1.77}, {@code 1.20.1-OptiFine_HD_U_I6}.
     *
     * The loader's own version is always a {@code 0.x} number (Fabric, Quilt, Legacy Fabric) and is
     * skipped; the first remaining dotted number is the game's, because every launcher names the
     * profile after the game before it names anything else.
     */
    static String minecraftNumber(String versionId) {
        if (versionId == null) return null;
        String id = versionId.toLowerCase(Locale.ENGLISH);
        // Better Than Adventure numbers its own game 7.x while being a 1.7.3 Beta fork, so its
        // number says nothing about which renderer can run it. Every BTA profile is named
        // "bta-<version>" by BTADownloadTask, which is what this reads.
        if (id.startsWith("bta")) return null;
        int i = 0;
        while (i < id.length()) {
            if (!Character.isDigit(id.charAt(i))) {
                i++;
                continue;
            }
            int start = i;
            while (i < id.length() && (Character.isDigit(id.charAt(i)) || id.charAt(i) == '.')) i++;
            String candidate = trimTrailingDots(id.substring(start, i));
            // A game version always has a major and a minor. A lone number is a build number, an API
            // level or an artefact version, and says nothing about the game.
            if (candidate.indexOf('.') > 0 && !candidate.startsWith("0.")) return candidate;
        }
        return null;
    }

    private static String trimTrailingDots(String number) {
        int end = number.length();
        while (end > 0 && number.charAt(end - 1) == '.') end--;
        return number.substring(0, end);
    }

    private static int[] parseMajorMinor(String number) {
        String[] split = number.split("\\.");
        if (split.length < 2) return null;
        try {
            return new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1])};
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /**
     * The renderer to actually launch with: the version's default when this build and device carry
     * the libraries for it, otherwise the other bundled renderer, otherwise the historical fallback.
     *
     * The bundled libraries decide this. A build stripped of Emesis cannot be pointed at it and
     * still launch, so the answer is chosen from what is on disk rather than promised to the player.
     *
     * @param version metadata of the version, or null
     * @param versionId the instance's version id
     */
    public static String launchTagForVersion(JVersionList.Version version, String versionId) {
        String preferred = tagForVersion(version, versionId);
        if (isBundled(preferred)) return preferred;
        String other = Renderers.LTW_RENDERER.equals(preferred)
                ? Renderers.EMESIS_RENDERER : Renderers.LTW_RENDERER;
        if (isBundled(other)) {
            Log.w(TAG, preferred + " is not packaged in this build; using " + other);
            return other;
        }
        Log.w(TAG, "Neither " + preferred + " nor " + other + " is packaged here; keeping GL4ES");
        return Renderers.GL4ES_RENDERER;
    }

    /**
     * Whether the libraries a renderer needs are packaged in this build. Only the two automatic
     * renderers are known here; anything else is the caller's business.
     */
    public static boolean isBundled(String rendererTag) {
        // Before Tools.initStorageConstants runs there is no directory to look in. Both renderers
        // are packaged for every ABI this repository builds, so an unknown directory assumes this
        // build carries them; the launch path still checks the library before switching, and a
        // renderer that really is missing is replaced there rather than promised here.
        if (Tools.NATIVE_LIB_DIR == null) return true;
        if (Renderers.EMESIS_RENDERER.equals(rendererTag)) {
            // Every core the Emesis library loads has to be there too, or it cannot start.
            return new File(Tools.NATIVE_LIB_DIR, "libemesis_renderer.so").isFile()
                    && new File(Tools.NATIVE_LIB_DIR, "libemesis_legacy.so").isFile()
                    && new File(Tools.NATIVE_LIB_DIR, "libemesis_modern.so").isFile();
        }
        if (Renderers.LTW_RENDERER.equals(rendererTag)) {
            return new File(Tools.NATIVE_LIB_DIR, "libltw.so").isFile();
        }
        return false;
    }

    /**
     * Whether a renderer is one of the launcher's automatic picks - the two this class chooses
     * between, plus the historic auto defaults (GL4ES, and MobileGlues where it was picked for the
     * device rather than by the player).
     *
     * Only an automatic pick may be replaced by the version rule. A renderer the player deliberately
     * chose - Zink, Proton, SFPEW, an external plugin - is left exactly as it is, because "my game
     * runs on Zink" is a decision about the phone, not about the Minecraft version.
     */
    public static boolean appliesToAutoSelection(RenderSpec spec) {
        if (spec == null) return false;
        String tag = spec.tag();
        return Renderers.LTW_RENDERER.equals(tag)
                || Renderers.EMESIS_RENDERER.equals(tag)
                || Renderers.GL4ES_RENDERER.equals(tag)
                || Renderers.MOBILEGLUES_RENDERER.equals(tag);
    }
}
