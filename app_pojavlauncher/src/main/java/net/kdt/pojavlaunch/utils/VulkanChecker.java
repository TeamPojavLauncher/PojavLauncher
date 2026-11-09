package net.kdt.pojavlaunch.utils;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

public final class VulkanChecker {
    private static boolean sLibraryLoaded = false;

    static {
        try {
            System.loadLibrary("vulkan_checker");
            sLibraryLoaded = true;
        } catch (UnsatisfiedLinkError ignored) {
            sLibraryLoaded = false;
        }
    }

    private VulkanChecker() {
    }

    /**
     * Reads the Vulkan version, device extensions and required features from the device.
     * @return the capabilities, or null when the loader or device could not be queried
     */
    @Nullable
    public static VulkanCapabilities checkCapabilities() {
        if (!sLibraryLoaded) return null;
        try {
            return nativeCheckVulkan();
        } catch (UnsatisfiedLinkError ignored) {
            return null;
        }
    }

    @Keep
    private static native VulkanCapabilities nativeCheckVulkan();
}
