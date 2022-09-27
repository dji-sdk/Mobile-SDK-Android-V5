/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.cameracore.widget.fpvinteraction;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.AnimatorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.core.util.SettingDefinitions.ControlMode;

/**
 * Displays a focus target on the screen with a scaling animation.
 */
public class FocusTargetView extends FrameLayout {

    //region Fields
    /**
     * The default x scaling for the center meter icon.
     */
    protected static final int DEFAULT_FOCUS_TARGET_DURATION = 2500;

    private long focusTargetDuration;
    private AnimatorSet scaleAnimatorSet;
    private ControlMode controlMode;
    private Map<ControlMode, Drawable> iconMap;
    //endregion

    //region Constructor
    public FocusTargetView(@NonNull Context context) {
        super(context);
        initView();
    }

    public FocusTargetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public FocusTargetView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        focusTargetDuration = DEFAULT_FOCUS_TARGET_DURATION;
        setAutoFocusAnimator(R.animator.uxsdk_animator_scale);
        iconMap = new HashMap<>();
        iconMap.put(ControlMode.AUTO_FOCUS, getResources().getDrawable(R.drawable.uxsdk_ic_focus_target_auto));
        iconMap.put(ControlMode.AUTO_FOCUS_CONTINUE, getResources().getDrawable(R.drawable.uxsdk_ic_focus_target_auto));
        iconMap.put(ControlMode.MANUAL_FOCUS, getResources().getDrawable(R.drawable.uxsdk_ic_focus_target_manual));
    }
    //endregion

    /**
     * Show focus target, start the scaling animation, then hide the focus target. For manual focus,
     * no scaling animation is shown.
     *
     * @param x position of click.
     * @param y position of click.
     */
    public void clickEvent(float x, float y) {
        addImageBackground(controlMode);
        if (getHandler() != null) {
            if (controlMode != ControlMode.MANUAL_FOCUS && scaleAnimatorSet != null) {
                getHandler().post(scaleAnimatorSet::start);
            } else {
                getHandler().postDelayed(this::removeImageBackground, focusTargetDuration);
            }
        }
        setX(x - getWidth() / 2f);
        setY(y - getHeight() / 2f);
    }

    /**
     * Set the focus mode
     *
     * @param controlMode The control mode to set
     */
    public void setControlMode(@NonNull ControlMode controlMode) {
        this.controlMode = controlMode;
    }

    //region Customization

    /**
     * Get the drawable resource that will be displayed when the {@link ControlMode} is set to the
     * given value.
     *
     * @param controlMode A ControlMode object.
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getFocusTargetIcon(@NonNull ControlMode controlMode) {
        return iconMap.get(controlMode);
    }

    /**
     * Set the drawable resource that will be displayed when the {@link ControlMode} is set to the
     * given value.
     *
     * @param controlMode A ControlMode object.
     * @param drawable    The drawable resource for the icon.
     */
    public void setFocusTargetIcon(@NonNull ControlMode controlMode, @Nullable Drawable drawable) {
        iconMap.put(controlMode, drawable);
        if (this.controlMode == controlMode && getBackground() != null) {
            addImageBackground(controlMode);
        }
    }

    /**
     * Sets the animator for the auto focus and auto focus continuous icons.
     *
     * @param animatorId The id of the animator, or 0 to remove the animation.
     */
    public void setAutoFocusAnimator(@AnimatorRes int animatorId) {
        if (animatorId == 0) {
            scaleAnimatorSet = null;
        } else {
            scaleAnimatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), animatorId);
            scaleAnimatorSet.setTarget(this);
            scaleAnimatorSet.addListener(new AnimListener());
        }
    }

    /**
     * Gets the duration in milliseconds that the focus target will stay on the screen before
     * disappearing. This is the amount of time after the animation completes, if any.
     *
     * @return The number of milliseconds the focus target will stay on the screen.
     */
    public long getFocusTargetDuration() {
        return focusTargetDuration;
    }

    /**
     * Sets the duration in milliseconds that the focus target will stay on the screen before
     * disappearing. This is the amount of time after the animation completes, if any.
     *
     * @param duration The number of milliseconds the focus target will stay on the screen.
     */
    public void setFocusTargetDuration(long duration) {
        focusTargetDuration = duration;
    }

    /**
     * Clears the focus target icon.
     */
    public void removeImageBackground() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(null);
        } else {
            setBackground(null);
        }
    }
    //endregion

    //region Helpers
    private void addImageBackground(ControlMode controlMode) {
        setBackground(iconMap.get(controlMode));
    }

    /**
     * Listener for animation set
     * Clears out background when animations finish
     */
    private class AnimListener implements Animator.AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation) {
            // do nothing
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (getHandler() != null) {
                getHandler().postDelayed(FocusTargetView.this::removeImageBackground, focusTargetDuration);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // do nothing
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            // do nothing
        }
    }
    //endregion
}
