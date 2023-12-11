package dji.sampleV5.aircraft.util;

import android.animation.Animator;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * 动画工具类
 * @author eleven
 */
public class AnimationUtils {

    private AnimationUtils(){
        throw new IllegalStateException("Utility class");
    }
    public static void springView(View view, DynamicAnimation.ViewProperty property, float scale) {
        springView(view, property, scale, 1.0f);
    }

    public static void springView(View view, DynamicAnimation.ViewProperty property, float scale, float startValue) {
        SpringAnimation animation = new SpringAnimation(view, property, scale);
        animation.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
        animation.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        animation.setStartValue(startValue);
        animation.start();
    }

    public static ViewPropertyAnimator scaleView(View view, float scaleX, float scaleY,
                                                 float alpha, long duration,
                                                 Animator.AnimatorListener listener) {
        view.setVisibility(View.VISIBLE);
        view.setScaleX(scaleX);
        view.setScaleY(scaleY);
        view.setAlpha(alpha);
        return view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(duration).setListener(listener);
    }
}
