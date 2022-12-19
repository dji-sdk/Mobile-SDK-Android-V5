package dji.v5.ux.core.ui.setting.dialog;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/12/16
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class ActivityLifecycleCallbacksAdapter implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        //do nothing
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        //do nothing
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        //do nothing
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        //do nothing
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        //do nothing
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        //do nothing
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        //do nothing

    }
}
