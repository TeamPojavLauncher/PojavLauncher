package net.kdt.pojavlaunch.multirt;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.dialogs.PackageSelector;
import net.kdt.pojavlaunch.dialogs.PackageAdapter;

public class MultiRTConfigDialog extends PackageSelector {
    public void show(){
        refresh();
        mDialog.show();
    }
    @SuppressLint("NotifyDataSetChanged") //only used to completely refresh the list, it is necessary
    public void refresh() {
        RecyclerView.Adapter<?> adapter = mDialogView.getAdapter();
        if(adapter != null) adapter.notifyDataSetChanged();
    }
    protected int getTitleRes() { return R.string.multirt_config_title; }
    protected int getPositiveButtonText() { return R.string.multirt_config_add; }
    protected int getNeutralButtonText() { return R.string.multirt_delete_runtime; }
    protected int getEditingButtonLabel() { return R.string.multirt_config_setdefault; }
    protected PackageAdapter<?> createAdapter(Context context, PackageSelector dialog) { return new RTRecyclerViewAdapter(dialog); }
    protected void onPositive(Context context, ActivityResultLauncher<Object> launcher) { launcher.launch(null); }
}
