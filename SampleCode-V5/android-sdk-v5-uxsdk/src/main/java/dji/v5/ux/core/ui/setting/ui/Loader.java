package dji.v5.ux.core.ui.setting.ui;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import dji.v5.utils.common.LogUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;


/**
 * 初始化加载，将页面初始化抽象为data和view
 * view初始化在ui线程 onCreateView()，data初始化在work线程 onCreateData() 中执行
 * ui和work线程是并发执行，当ui与work线程都准备好之后，最后刷新view onRefreshView()
 *
 * @author young.huang
 */
public class Loader {

    private static final String TAG = Loader.class.getSimpleName();

    private LoaderListener listener;

    private boolean isStopLoad = false;

    private Loader() {

    }

    public static Loader createLoader() {
        return new Loader();
    }

    public Loader setListener(LoaderListener listener) {
        this.listener = listener;
        return this;
    }

    @SuppressLint("CheckResult")
    public void start() {
        if (listener == null) {
            LogUtils.e(TAG, "loader listener is not set!!!");
            return;
        }
        Flowable<Integer> uiTask = Flowable.fromCallable(() -> {
            if (hostIsDead()) {
                cancel();
                return 0;
            }
            listener.onCreateView();
            return 1;
        }).subscribeOn(AndroidSchedulers.mainThread());

        Flowable<Integer> dataTask = Flowable.fromCallable(() -> {
            if (hostIsDead()) {
                cancel();
                return 0;
            }
            listener.onCreateData();
            return 1;
        }).subscribeOn(Schedulers.io());

        Flowable.zip(uiTask, dataTask, (o1, o2) -> o1 + o2).observeOn(AndroidSchedulers.mainThread()).subscribe(o -> {
            if (hostIsDead()) {
                cancel();
                return;
            }
            listener.onRefreshView();
            cancel();
        });
        return;
    }

    private boolean hostIsDead() {
        if (listener == null) {
            return true;
        }
        LoaderListener l = listener;
        if (l instanceof Fragment) {
            return ((Fragment) l).isDetached();
        }
        if (l instanceof Activity) {
            return ((Activity) l).isFinishing() || ((Activity) l).isDestroyed();
        }

        return isStopLoad;
    }

    public void cancel() {
        listener = null;
        isStopLoad = true;
    }

    public interface LoaderListener {
        @UiThread
        void onCreateView();
        @WorkerThread
        void onCreateData();
        @UiThread
        void onRefreshView();
    }
}
