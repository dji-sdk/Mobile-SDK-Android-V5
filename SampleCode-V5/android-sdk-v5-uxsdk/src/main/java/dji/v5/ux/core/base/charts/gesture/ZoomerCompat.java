package dji.v5.ux.core.base.charts.gesture;

import android.content.Context;
import android.os.SystemClock;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class ZoomerCompat {
    private Interpolator mInterpolator = new DecelerateInterpolator();
    private long mAnimationDurationMillis = 200L;
    private boolean mFinished = true;
    private float mCurrentZoom;
    private long mStartRTC;
    private float mEndZoom;

    public ZoomerCompat(Context context) {
        //do nothing
    }

    public void forceFinished(boolean finished) {
        this.mFinished = finished;
    }

    public void abortAnimation() {
        this.mFinished = true;
        this.mCurrentZoom = this.mEndZoom;
    }

    public void startZoom(float endZoom) {
        this.mStartRTC = SystemClock.elapsedRealtime();
        this.mEndZoom = endZoom;
        this.mFinished = false;
        this.mCurrentZoom = 1.0F;
    }

    public boolean computeZoom() {
        if (this.mFinished) {
            return false;
        } else {
            long tRTC = SystemClock.elapsedRealtime() - this.mStartRTC;
            if (tRTC >= this.mAnimationDurationMillis) {
                this.mFinished = true;
                this.mCurrentZoom = this.mEndZoom;
                return false;
            } else {
                float t = (float)tRTC * 1.0F / (float)this.mAnimationDurationMillis;
                this.mCurrentZoom = this.mEndZoom * this.mInterpolator.getInterpolation(t);
                return true;
            }
        }
    }

    public float getCurrZoom() {
        return this.mCurrentZoom;
    }
}

