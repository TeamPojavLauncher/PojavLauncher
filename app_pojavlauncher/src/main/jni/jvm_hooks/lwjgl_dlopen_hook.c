//
// Created by maks on 06.01.2025.
//

#include "jvm_hooks.h"

#include <android/api-level.h>

#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>

#define TAG __FILE_NAME__
#include <log.h>

#include "../pojavexec.h"


static const char* gles_symbol_fallbacks[] = {
        "glGetActiveUniformsiv",
        "glGetActiveUniformName",
        "glGetUniformIndices",
        "glGetActiveUniformBlockiv",
        "glGetActiveUniformBlockName",
        "glGetIntegeri_v",
        NULL
};

static int should_fallback_to_gles(const char* name) {
    for(int i = 0; gles_symbol_fallbacks[i] != NULL; i++) {
        if(strcmp(name, gles_symbol_fallbacks[i]) == 0) return 1;
    }
    return 0;
}

static void* get_gles_symbol(const char* name) {
    static void* gles_handle = NULL;
    if(gles_handle == NULL) {
        gles_handle = dlopen("libGLESv2.so", RTLD_NOW | RTLD_LOCAL);
        if(gles_handle == NULL) {
            printf("LWJGL linkerhook: failed to open libGLESv2.so for %s: %s\n", name, dlerror());
            return NULL;
        }
    }
    void* symbol = dlsym(gles_handle, name);
    if(symbol != NULL) {
        printf("LWJGL linkerhook: resolved missing renderer symbol %s from libGLESv2.so\n", name);
    }
    return symbol;
}

/**
 * Basically a verbatim implementation of ndlopen(), found at
 * https://github.com/PojavLauncherTeam/lwjgl3/blob/3.3.1/modules/lwjgl/core/src/generated/c/linux/org_lwjgl_system_linux_DynamicLinkLoader.c#L11
 * but with our own additions for stuff like vulkanmod.
 */
static jlong ndlopen_bugfix(__attribute__((unused)) JNIEnv *env,
                     __attribute__((unused)) jclass class,
                     jlong filename_ptr,
                     jint jmode) {
    const char* filename = (const char*) filename_ptr;

    // Oveeride vulkan loading to let us load vulkan ourselves
    if(strstr(filename, "libvulkan.so") == filename) {
        printf("LWJGL linkerhook: replacing load for libvulkan.so with custom driver\n");
        return (jlong) pojavexec_loadVulkanDriver();
    }
    // Load renderer using egl_acquire
    if(strstr(filename, "libGLMojo.so") == filename) {
        printf("LWJGL linkerhook: replacing OpenGL with renderspec driver\n");
        const pojavexec_renderspec_t *rspec = pojavexec_getRenderSpec();
        return (jlong) rspec->egl_acquire(rspec->renderer_path ? rspec->renderer_path : rspec->egl_path);
    }

    // This hook also serves the task of mitigating a bug: the idea is that since, on Android 10 and
    // earlier, the linker doesn't really do namespace nesting.
    // It is not a problem as most of the libraries are in the launcher path, but when you try to run
    // VulkanMod which loads shaderc outside of the default jni libs directory through this method,
    // it can't load it because the path is not in the allowed paths for the anonymous namesapce.
    // This method fixes the issue by being in libpojavexec, and thus being in the classloader namespace

    int mode = (int)jmode;
    return (jlong) dlopen(filename, mode);
}

static jlong ndlsym_bugfix(__attribute__((unused)) JNIEnv *env,
                    __attribute__((unused)) jclass class,
                    jlong handle_ptr,
                    jlong name_ptr) {
    void* handle = (void*) handle_ptr;
    const char* name = (const char*) name_ptr;
    void* symbol = dlsym(handle, name);
    if(symbol == NULL && should_fallback_to_gles(name)) {
        symbol = get_gles_symbol(name);
    }
    return (jlong) symbol;
}

/**
 * Install the LWJGL dlopen hook. This allows us to mitigate linker bugs and add custom library overrides.
 */
void installLwjglDlopenHook(JNIEnv *env) {
    LOGI("Installing LWJGL dlopen() hook");
    jclass dynamicLinkLoader = (*env)->FindClass(env, "org/lwjgl/system/linux/DynamicLinkLoader");
    if(dynamicLinkLoader == NULL) {
        LOGE("Failed to find the target class");
        (*env)->ExceptionClear(env);
        return;
    }
    JNINativeMethod ndlopenMethod[] = {
            {"ndlopen", "(JI)J", &ndlopen_bugfix},
            {"ndlsym", "(JJ)J", &ndlsym_bugfix}
    };
    if((*env)->RegisterNatives(env, dynamicLinkLoader, ndlopenMethod, 2) != 0) {
        LOGE("Failed to register the hooked method");
        (*env)->ExceptionClear(env);
    }
}
