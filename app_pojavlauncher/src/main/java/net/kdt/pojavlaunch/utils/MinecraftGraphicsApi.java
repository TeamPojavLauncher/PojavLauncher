package net.kdt.pojavlaunch.utils;

import androidx.annotation.NonNull;

/** Applies the Minecraft graphics backend preference for a game directory. */
public final class MinecraftGraphicsApi {
    public static final String DEFAULT = "default";
    public static final String DEFAULT_OPENGL = "default_opengl";
    public static final String OPENGL = "opengl";
    public static final String VULKAN = "vulkan";

    private static final String OPTION = "preferredGraphicsBackend";

    private MinecraftGraphicsApi() {
    }

    /**
     * Writes the preferred graphics backend into the game's options file.
     * DEFAULT leaves the options file untouched, and DEFAULT_OPENGL only fills in OpenGL
     * when the game has no backend set yet.
     */
    public static void apply(@NonNull String gameDirectory, @NonNull String mode) {
        if (DEFAULT.equals(mode)) return;

        MCOptionUtils.load(gameDirectory);
        if (DEFAULT_OPENGL.equals(mode) && MCOptionUtils.get(OPTION) != null) return;

        String backend = VULKAN.equals(mode) ? "\"vulkan\"" : "\"opengl\"";
        MCOptionUtils.set(OPTION, backend);
        MCOptionUtils.save();
    }
}
