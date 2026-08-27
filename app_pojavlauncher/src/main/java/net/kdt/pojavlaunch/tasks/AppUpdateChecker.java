package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Checks TeamPojavLauncher/PojavLauncher's GitHub Releases for a build newer than the one
 * currently installed, and offers to download and install it.
 * <p>
 * Ported from Vera-Firefly/Pojav-Glow-Worm's {@code com.firefly.feature.UpdateLauncher},
 * rewired to use this codebase's own networking ({@link DownloadUtils}), background
 * execution ({@code PojavApplication.sExecutorService}), progress reporting
 * ({@link ProgressLayout}) and dialog conventions instead of Glow-Worm's OkHttp client and
 * {@code ProgressDialog}.
 * <p>
 * PojavLauncher's release tags aren't a plain incrementing integer (e.g.
 * {@code gladiolus-20250117-6b0ff50-v3_openjdk}), so unlike the source implementation this
 * does not parse digits out of the tag to compare versions. Instead it treats the tag as an
 * opaque identifier and compares it against the last tag this install knows about (defaulting
 * to {@link BuildConfig#VERSION_NAME} on first run).
 */
public class AppUpdateChecker {
    private static final String GITHUB_API_LATEST_RELEASE =
            "https://api.github.com/repos/TeamPojavLauncher/PojavLauncher/releases/latest";
    private static final String PREF_KEY_KNOWN_TAG = "appUpdateKnownTag";
    private static final String PREF_KEY_IGNORED_TAG = "appUpdateIgnoredTag";

    private final Activity mActivity;
    private final File mDownloadedApk;

    public AppUpdateChecker(Activity activity) {
        mActivity = activity;
        mDownloadedApk = new File(Tools.DIR_CACHE, "app_update/update.apk");
    }

    /**
     * @param silent if {@code true}, stay quiet when already up to date, on failure, or when
     *               the user previously chose to ignore this exact release (used for the
     *               automatic startup check). If {@code false}, always give feedback and
     *               re-offer an ignored release (used for the manual "Check for updates"
     *               preference).
     */
    public void checkForUpdates(boolean silent) {
        sExecutorService.execute(() -> {
            try {
                JSONObject release = new JSONObject(DownloadUtils.downloadString(GITHUB_API_LATEST_RELEASE));
                handleRelease(release, silent);
            } catch (IOException | JSONException e) {
                if (!silent) {
                    Tools.runOnUiThread(() -> Toast.makeText(mActivity,
                            R.string.appupdate_check_failed, Toast.LENGTH_LONG).show());
                }
                Tools.showErrorRemote(e);
            }
        });
    }

    private void handleRelease(JSONObject release, boolean silent) throws JSONException {
        String tagName = release.getString("tag_name");
        String releaseName = release.optString("name", tagName);
        String releaseNotes = release.optString("body", "");

        String knownTag = LauncherPreferences.DEFAULT_PREF.getString(PREF_KEY_KNOWN_TAG, BuildConfig.VERSION_NAME);
        if (tagName.equals(knownTag)) {
            if (!silent) {
                Tools.runOnUiThread(() -> Toast.makeText(mActivity,
                        R.string.appupdate_up_to_date, Toast.LENGTH_SHORT).show());
            }
            return;
        }

        String ignoredTag = LauncherPreferences.DEFAULT_PREF.getString(PREF_KEY_IGNORED_TAG, "");
        if (silent && tagName.equals(ignoredTag)) return;

        String downloadUrl = findApkAssetUrl(release.optJSONArray("assets"));
        if (downloadUrl == null) {
            if (!silent) Tools.showErrorRemote(mActivity, R.string.appupdate_no_asset, new IOException(tagName));
            return;
        }

        Tools.runOnUiThread(() -> showUpdateDialog(tagName, releaseName, releaseNotes, downloadUrl));
    }

    /** Picks the asset matching the currently running build (debug vs release APK). */
    @Nullable
    private String findApkAssetUrl(@Nullable JSONArray assets) throws JSONException {
        if (assets == null) return null;
        String fallbackUrl = null;
        boolean wantDebug = BuildConfig.DEBUG;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name");
            if (!name.toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
            String url = asset.getString("browser_download_url");
            if (fallbackUrl == null) fallbackUrl = url;
            boolean isDebugAsset = name.toLowerCase(Locale.ROOT).contains("debug");
            if (isDebugAsset == wantDebug) return url;
        }
        return fallbackUrl;
    }

    private void showUpdateDialog(String tagName, String releaseName, String releaseNotes, String downloadUrl) {
        if (mActivity.isFinishing()) return;
        new AlertDialog.Builder(mActivity)
                .setTitle(mActivity.getString(R.string.appupdate_dialog_title, releaseName))
                .setMessage(releaseNotes.isEmpty() ?
                        mActivity.getString(R.string.appupdate_dialog_message_empty) : releaseNotes)
                .setPositiveButton(R.string.appupdate_dialog_update, (d, w) -> startDownload(tagName, downloadUrl))
                .setNeutralButton(R.string.appupdate_dialog_ignore, (d, w) ->
                        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_KEY_IGNORED_TAG, tagName).apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startDownload(String tagName, String downloadUrl) {
        ProgressLayout.setProgress(ProgressLayout.APP_UPDATE, 0, R.string.appupdate_downloading);
        sExecutorService.execute(() -> {
            try {
                DownloadUtils.downloadFileMonitored(downloadUrl, mDownloadedApk, null, (curr, max) -> {
                    int percent = max > 0 ? (int) (100L * curr / max) : 0;
                    ProgressLayout.setProgress(ProgressLayout.APP_UPDATE, percent, R.string.appupdate_downloading);
                });
                ProgressLayout.clearProgress(ProgressLayout.APP_UPDATE);
                LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_KEY_KNOWN_TAG, tagName).apply();
                Tools.runOnUiThread(this::promptInstall);
            } catch (IOException e) {
                ProgressLayout.clearProgress(ProgressLayout.APP_UPDATE);
                Tools.showErrorRemote(e);
            }
        });
    }

    private void promptInstall() {
        if (mActivity.isFinishing()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !mActivity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.appupdate_dialog_title_permission)
                    .setMessage(R.string.appupdate_permission_required)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + mActivity.getPackageName()));
                        mActivity.startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        installApk();
    }

    private void installApk() {
        Uri apkUri = FileProvider.getUriForFile(mActivity,
                mActivity.getApplicationContext().getPackageName() + ".fileprovider", mDownloadedApk);
        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mActivity.startActivity(installIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Tools.showErrorRemote(e);
        }
    }
}
