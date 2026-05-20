package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.Tools;

import java.lang.ref.WeakReference;

public class PermissionUtils {

    @FunctionalInterface
    public interface PermissionCallback {
        void run(boolean isAllowed);
    }


    private static ActivityResultLauncher<String> mRequestPermissionLauncher;
    private static WeakReference<PermissionCallback> mOnRequestCallback;

    public static void init(LauncherActivity activity) {
        if (mRequestPermissionLauncher != null) return;
        mRequestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if (mOnRequestCallback != null) Tools.getWeakReference(mOnRequestCallback).run(isAllowed);
                }
        );
    }

    public static boolean checkForPermission(Activity activity, int minApi, final String permission) {
        return Build.VERSION.SDK_INT < minApi ||
                ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_DENIED ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    public static void askForPermission(int minApi, PermissionCallback onCallback, final String permission) {
        if(Build.VERSION.SDK_INT < minApi) return;

        if (onCallback != null) mOnRequestCallback = new WeakReference<>(onCallback);
        mRequestPermissionLauncher.launch(permission);
    }
}
