/**
 * @filename 	: DJIVerticalProgressBar.java
 * @package 	: dji.pilot.publics.widget
 * @date 		: 2014年8月11日 上午11:47:16
 * @author 		: gashion.fang
 * @description : 
 *
 * Copyright (c) 2014, DJI All Rights Reserved.
 *
 */

package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class BatteryVerticalProgressBar extends ProgressBar{

    private int mH = 0;
    private int mW = 0;
    private int mOldH = 0;
    private int mOldW = 0;

    public BatteryVerticalProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
        mH = h;
        mW = w;
        mOldH = oldh;
        mOldW = oldw;
    }

    @Override
    public void setProgressDrawable(Drawable d) {
        super.setProgressDrawable(d);
        onSizeChanged(mW, mH, mOldW, mOldH);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(mW, mH, mOldW, mOldH);
    }

    @Override
    protected synchronized void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

}