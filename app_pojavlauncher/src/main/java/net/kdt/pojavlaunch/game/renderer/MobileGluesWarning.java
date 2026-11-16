package net.kdt.pojavlaunch.game.renderer;

import net.kdt.pojavlaunch.game.renderer.def.Renderers;

/**
 * The warning the launcher shows before it lets a player choose MobileGlues.
 *
 * MobileGlues is a third-party OpenGL ES wrapper whose releases move independently of this launcher,
 * and whose failures surface inside Minecraft rather than here - a black screen, a stack trace in the
 * game log, a crash on the world loading screen. It ships in the app because it is genuinely the
 * fastest option on some GPUs, not because it is the option to reach for first. So the picker says so
 * before the choice is written, every time it is made, and offers a way back.
 *
 * The text lives in one place so the dialog and the settings row can never say different things.
 */
public final class MobileGluesWarning {
    /** Whether choosing [rendererTag] is the choice this warning is about. */
    public static boolean applies(String rendererTag) {
        return Renderers.MOBILEGLUES_RENDERER.equals(rendererTag);
    }

    /**
     * Renderer offered by the "use the recommended renderer instead" button: the version rule's pick
     * for {@code versionId}, resolved against what this build actually ships, so the button can never
     * name a renderer the APK does not carry.
     */
    public static String recommendedInstead(String versionId) {
        return RendererDefaults.launchTagForVersion(null, versionId);
    }

    public static String title() {
        return "MobileGlues is experimental";
    }

    /**
     * The body of the dialog. Written as a warning, not a refusal: the choice is still allowed, and
     * the player is told exactly what to expect and what to do when it goes wrong.
     */
    public static String message() {
        return "We don't recommend MobileGlues, and it is not supported the way the built-in renderers "
                + "are.\n\n"
                + "- It is experimental. Its releases are made outside this launcher, so we cannot test "
                + "your combination of device, graphics driver and Minecraft version.\n"
                + "- It works on 1.17 and newer only. An older version will not start on it at all.\n"
                + "- Typical failures are a black screen, a crash while a world loads, or missing "
                + "textures - and they look like a broken game rather than a renderer problem.\n"
                + "- When it does fail, the launcher cannot always tell you why: expect to read the game "
                + "log to find out what happened.\n"
                + "- Its settings page is a separate app inside the launcher, and its options do not "
                + "carry over to the other renderers.\n\n"
                + "What we do recommend: Emesis runs every version, and LTW is the renderer for 1.17 and "
                + "newer - it is what an instance picks for itself. Use MobileGlues only if you have a "
                + "reason to, and be ready to switch back. Your worlds and settings are not affected "
                + "either way.";
    }

    /** Label of the button that keeps MobileGlues, so the dialog never reads as a blocked choice. */
    public static String confirmLabel() {
        return "Use it anyway";
    }

    /** Short name of a renderer tag, for button labels. */
    public static String rendererName(String rendererTag) {
        if (Renderers.EMESIS_RENDERER.equals(rendererTag)) return "Emesis";
        if (Renderers.LTW_RENDERER.equals(rendererTag)) return "LTW";
        if (Renderers.GL4ES_RENDERER.equals(rendererTag)) return "LTW Legacy";
        return rendererTag;
    }

    /**
     * Label of the button that takes [rendererTag] instead. The caller passes the tag it is going to
     * store, so the button never promises a renderer the action does not write.
     */
    public static String recommendedLabel(String rendererTag) {
        return "Use " + rendererName(rendererTag) + " instead";
    }
}
