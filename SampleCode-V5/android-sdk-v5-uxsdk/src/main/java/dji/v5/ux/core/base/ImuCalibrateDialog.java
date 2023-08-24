package dji.v5.ux.core.base;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.dialog.BaseDialog;



/*
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

/**
 * <p>Created by luca on 2017/4/19.</p>
 */

public class ImuCalibrateDialog extends BaseDialog implements ImuCalView.OnImuCalListener {

    public ImuCalibrateDialog(Context context) {
        this(context, R.style.NoTitleDialog);
    }

    public ImuCalibrateDialog(Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {
        ImuCalView imuCalView = new ImuCalView(getContext());
        imuCalView.setOnImuCalListener(this);
        setContentView(imuCalView);
    }

    @Override
    public void onClose(int arg1) {
        dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.width = WindowManager.LayoutParams.MATCH_PARENT;
        attrs.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(attrs);
    }

    @Override
    public void show() {

        Window window = getWindow();
        // Set the dialog to not focusable.
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Show the dialog with NavBar hidden.
        super.show();

        // Set the dialog to focusable again.
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
