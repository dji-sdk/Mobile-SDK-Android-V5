package dji.v5.ux.core.ui.setting.dialog;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;


public class BaseDialog extends Dialog {

    private static boolean sCanShowDialog = true;

    @Nullable
    private OnDismissListener mOnDismissListener;

    @Nullable
    private OnShowListener mOnShowListener;

    public BaseDialog(Context context) {
        super(context);
        initListeners();
    }

    public BaseDialog(Context context, int themeResId) {
        super(context, themeResId);
        initListeners();
    }

    protected BaseDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initListeners();
    }

    private void initListeners() {
        super.setOnDismissListener(dialog -> {
            unregisterActivityLifecycleCallbacks(getBaseContext());
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        });
        super.setOnShowListener(dialog -> {
            registerActivityLifecycleCallbacks(getBaseContext());
            if (mOnShowListener != null) {
                mOnShowListener.onShow(dialog);
            }
        });
        setOnKeyListener(new DispatchDialogKeyEventToActivityListener(getContext()));
    }

    @Override
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    @Override
    public void setOnShowListener(@Nullable OnShowListener listener) {
        mOnShowListener = listener;
    }

    @Override
    protected void onStart() {
        Window window = this.getWindow();
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            hideSystemUI(window);
        }
        super.onStart();
    }

    protected void hideSystemUI(Window window) {
        if (window == null) {
            return;
        }
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        window.getDecorView().setSystemUiVisibility(uiOptions);
    }

    protected void resizeWindow() {
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = getDialogWidth();
            params.height = getDialogHeight();
            params.gravity = Gravity.CENTER_VERTICAL;
            window.setAttributes(params);
        }
        checkIsSuitableHeight();
    }

    protected void checkIsSuitableHeight() {
        Window window = getWindow();
        if (window != null) {
            View view = window.getDecorView();
            view.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            int dialogH = view.getMeasuredHeight();
            int dialogMaxH = getDialogMaxHeight();
            if (dialogH > dialogMaxH) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER_VERTICAL;
                window.setAttributes(params);
            }
        }
    }

    public int getDialogWidth() {
        return (int) getContext().getResources().getDimension(R.dimen.uxsdk_320_dp);
    }

    public int getDialogHeight() {
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public int getDialogMaxHeight() {
        return AndUtil.getLandScreenHeight(getContext());
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        resizeWindow();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        resizeWindow();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        resizeWindow();
    }

    @Override
    public void show() {
        if (isCanShowDialog() && !isActivityFinish()) {
            super.show();
        }
    }

    @Override
    public void dismiss() {
        try {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                AndroidSchedulers.mainThread().scheduleDirect(() -> dismiss());
                return;
            }
            super.dismiss();
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
    }

    private boolean isActivityFinish() {
        if (getContext() instanceof Activity) {
            if (((Activity) getContext()).isFinishing()) {
                return true;
            }
        } else if (getContext() instanceof ContextWrapper) {
            Context context = ((ContextWrapper) getContext()).getBaseContext();
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCanShowDialog() {
        return sCanShowDialog;
    }

    public static void setCanShowDialog(boolean sCanShowDialog) {
        BaseDialog.sCanShowDialog = sCanShowDialog;
    }

    private Context getBaseContext() {
        Context context = getContext();
        if (context instanceof ContextThemeWrapper) {
            context = ((ContextThemeWrapper) context).getBaseContext();
        }
        return context;
    }

    private Application getApplication(Context context) {
        Application app = null;
        if (context instanceof AppCompatActivity) {
            app = ((AppCompatActivity) context).getApplication();
        } else if (context instanceof Activity) {
            app = ((Activity) context).getApplication();
        }
        return app;
    }

    private void unregisterActivityLifecycleCallbacks(Context context) {
        Application app = getApplication(context);
        if (app == null) {
            return;
        }
        if (mActivityLifecycleCallbacks != null) {
            app.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
            mActivityLifecycleCallbacks = null;
        }
    }

    // 之前直接在Constructor里面注册ActivityLifecycleCallbacks，
    // 如果在View里面创建了一个dialog但是view被destroy了而activity还没有，就会导致内存泄漏
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

    private void registerActivityLifecycleCallbacks(Context context) {
        Application app = getApplication(context);
        if (app == null) {
            return;
        }

        final Application finalApp = app;
        unregisterActivityLifecycleCallbacks(context);

        mActivityLifecycleCallbacks=new ActivityLifecycleCallbacksAdapter(){
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (context == activity) {
                    if (isShowing()) {
                        dismiss();
                    }
                    finalApp.unregisterActivityLifecycleCallbacks(this);
                }
            }
        };

        app.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

}
