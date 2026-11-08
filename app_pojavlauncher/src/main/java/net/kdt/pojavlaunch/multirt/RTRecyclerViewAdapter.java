package net.kdt.pojavlaunch.multirt;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.NewJREUtil;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.dialogs.PackageSelector;
import net.kdt.pojavlaunch.dialogs.PackageAdapter;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException;

import java.io.IOException;
import java.util.List;

public class RTRecyclerViewAdapter extends PackageAdapter<RTRecyclerViewAdapter.RTViewHolder> {
    private Activity mActivity;
    private boolean mIsDownloading = false;

    public RTRecyclerViewAdapter(PackageSelector dialog) { super(dialog); }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public void onBindViewHolder(@NonNull RTViewHolder holder, int position) {
        final List<Runtime> installedRuntimes = MultiRTUtils.getRuntimes();
        final List<NewJREUtil.ExternalRuntime> downloadableRuntimes = MultiRTUtils.getRuntimesToDownload();

        if (position < installedRuntimes.size()) {
            holder.bind(installedRuntimes.get(position), position);
        } else if (position < installedRuntimes.size() + downloadableRuntimes.size()) {
            int downloadPos = position - installedRuntimes.size();
            holder.bindDownloadableRuntime(downloadableRuntimes.get(downloadPos), position);
        }
    }
    protected RTViewHolder abstractHolder(View recyclableView) { return new RTViewHolder(recyclableView); }
    public int getItemCount() {
        return MultiRTUtils.getRuntimes().size() + MultiRTUtils.getRuntimesToDownload().size();
    }
    public boolean isDefaultRuntime(Runtime rt) { return LauncherPreferences.PREF_DEFAULT_RUNTIME.equals(rt.name); }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(Runtime rt){
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name;
        LauncherPreferences.DEFAULT_PREF.edit().putString("defaultRuntime",LauncherPreferences.PREF_DEFAULT_RUNTIME).apply();
        notifyDataSetChanged();
    }

    public class RTViewHolder extends AbstractHolder<Runtime> implements View.OnClickListener {
        Runtime mCurrentRuntime;
        NewJREUtil.ExternalRuntime mCurrentDownloadableRuntime;
        int mCurrentPosition;

        public RTViewHolder(View itemView) {
            super(itemView);
            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                if(mCurrentRuntime != null) {
                    setDefault(mCurrentRuntime);
                    RTRecyclerViewAdapter.this.notifyDataSetChanged();
                } else if(mCurrentDownloadableRuntime != null) {
                    downloadRuntime(mCurrentDownloadableRuntime);
                }
            });
            mDeleteButton.setOnClickListener(this);
        }

        @SuppressLint("NotifyDataSetChanged")
        private void downloadRuntime(NewJREUtil.ExternalRuntime runtime) {
            if(mActivity == null) return;
            if(mIsDownloading) return;

            mIsDownloading = true;
            mSetDefaultButton.setEnabled(false);
            mSetDefaultButton.setText(R.string.global_installing);
            runtime.isDownloading = true;

            sExecutorService.execute(() -> {
                try {
                    AssetManager assetManager = mActivity.getAssets();
                    runtime.downloadRuntime(assetManager);
                } catch (RuntimeSelectionException e) {
                    Tools.showError(mActivity, e);
                } finally {
                    mSetDefaultButton.post(() -> {
                        runtime.isDownloading = false;
                        mIsDownloading = false;
                        notifyDataSetChanged();
                    });
                }
            });
        }

        public void bind(Runtime runtime, int pos) {
            mCurrentRuntime = runtime;
            mCurrentDownloadableRuntime = null;
            mCurrentPosition = pos;

            if(runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                mNameTextView.setText(runtime.name
                        .replace(".tar.xz", "")
                        .replace("-", " "));
                mExtraNameTextView.setText(runtime.versionString);
                mExtraNameTextView.setTextColor(mDefaultColors);

                updateButtonsVisibility();

                boolean defaultRuntime = isDefaultRuntime(runtime);
                mSetDefaultButton.setEnabled(!defaultRuntime);
                mSetDefaultButton.setText(defaultRuntime ? R.string.multirt_config_setdefault_already:R.string.multirt_config_setdefault);
                return;
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.setVisibility(View.VISIBLE);
            if(runtime.versionString == null){
                mExtraNameTextView.setText(R.string.multirt_runtime_corrupt);
            }else{
                mExtraNameTextView.setText(mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch));
            }
            mNameTextView.setText(runtime.name);
            mExtraNameTextView.setTextColor(Color.RED);
            mSetDefaultButton.setVisibility(View.GONE);
        }

        public void bindDownloadableRuntime(NewJREUtil.ExternalRuntime runtime, int pos) {
            mCurrentRuntime = null;
            mCurrentDownloadableRuntime = runtime;
            mCurrentPosition = pos;

            mNameTextView.setText(runtime.name
                    .replace(".tar.xz", "")
                    .replace("-", " "));

            mExtraNameTextView.setText(R.string.global_not_installed);
            mExtraNameTextView.setTextColor(mDefaultColors);

            mSetDefaultButton.setVisibility(View.VISIBLE);
            mDeleteButton.setVisibility(View.GONE);

            if (mIsDownloading || runtime.isDownloading) {
                mSetDefaultButton.setEnabled(false);
                mSetDefaultButton.setText(R.string.global_installing);
            } else {
                mSetDefaultButton.setEnabled(true);
                mSetDefaultButton.setText(R.string.global_download);
            }
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(isEditing() ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(isEditing() ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onClick(View v) {
            if (mCurrentRuntime == null) return;

            if(MultiRTUtils.getRuntimes().size() < 2) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.global_error)
                        .setMessage(R.string.multirt_config_removeerror_last)
                        .setPositiveButton(android.R.string.ok,(adapter, which)->adapter.dismiss())
                        .show();
                return;
            }

            sExecutorService.execute(() -> {
                try {
                    MultiRTUtils.removeRuntimeNamed(mCurrentRuntime.name);
                    mDeleteButton.post(() -> {
                        if(getBindingAdapter() != null)
                            getBindingAdapter().notifyDataSetChanged();
                    });

                } catch (IOException e) {
                    Tools.showError(itemView.getContext(), e);
                }
            });
        }
    }
}
