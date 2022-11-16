package dji.v5.ux.accessory.item;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.IntDef;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;

import com.airbnb.lottie.LottieAnimationView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;


public class RtkGuidanceView extends ConstraintLayout implements View.OnClickListener {

    @IntDef({
            GuidanceMode.STEP_RESET,
            GuidanceMode.STEP_FIRST,
            GuidanceMode.STEP_SECOND,
            GuidanceMode.STEP_THIRD,
    })

    @Retention(RetentionPolicy.SOURCE)
    private @interface GuidanceMode {
        int STEP_RESET = 0;
        int STEP_FIRST = 1;
        int STEP_SECOND = 2;
        int STEP_THIRD = 3;

    }

    //顶部导航栏控件
    private View mNavigationLayout;
    private TextView mStepFirstTv;
    private TextView mStepSecondTv;
    private TextView mStepThirdTv;
    private ImageView mStepFirstIv;
    private ImageView mStepSecondIv;
    private View mStepStartDivider;
    private View mStepEndDivider;
    //左边动画控件
    private LottieAnimationView mLottieAnimationView;
    private View mReplayView;
    private Button mReplayBtn;
    private TextView mLottieTip;
    //左边文字控件
    private TextView mTitleTv;
    private TextView mContentTv;
    private View mImageView;
    private TextView mImageDescTv;
    private Button mPreviousBtn;
    private Button mNextBtn;

    private PopupWindow mPopupWindow;

    private int mStep; //0——重置、1——步骤1、2——步骤2、3——步骤3
    private String[] mTitleStr;
    private String[] mContentStr;
    private int[] mAnimationRes;
    private String[] mImageAssetsFolderStr;
    private boolean mStartAnimation;
    private Runnable mTipRunnable;


    public RtkGuidanceView(Context context) {
        this(context, null);
    }

    public RtkGuidanceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RtkGuidanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_rtk_guidance_layout, this, true);
        initView();
        initWidgetParams();
        initParams(context);
        initListener();
    }

    private void initView() {
        //顶部导航栏控件
        mNavigationLayout = findViewById(R.id.rtk_guidance_step_navigation);
        mStepFirstTv = findViewById(R.id.rtk_guidance_step_first_number);
        mStepSecondTv = findViewById(R.id.rtk_guidance_step_second_number);
        mStepThirdTv = findViewById(R.id.rtk_guidance_step_third_number);
        mStepFirstIv = findViewById(R.id.rtk_guidance_step_first_image);
        mStepSecondIv = findViewById(R.id.rtk_guidance_step_second_image);
        mStepStartDivider = findViewById(R.id.rtk_guidance_start_divider);
        mStepEndDivider = findViewById(R.id.rtk_guidance_end_divider);

        //左边动画控件
        mLottieAnimationView = findViewById(R.id.rtk_guidance_lottie_animation);
        mReplayView = findViewById(R.id.rtk_guidance_replay_view);
        mReplayBtn = findViewById(R.id.rtk_guidance_replay_btn);
        mLottieTip = findViewById(R.id.rtk_guidance_lottie_tip);

        //左边文字控件
        mTitleTv = findViewById(R.id.rtk_guidance_step_title);
        mContentTv = findViewById(R.id.rtk_guidance_step_content);
        mImageView = findViewById(R.id.rtk_guidance_step_image);
        mImageDescTv = findViewById(R.id.rtk_guidance_step_image_desc);
        mPreviousBtn = findViewById(R.id.rtk_guidance_step_previous);
        mNextBtn = findViewById(R.id.rtk_guidance_step_next);
    }

    private void initListener() {
        findViewById(R.id.rtk_guidance_close).setOnClickListener(this);
        findViewById(R.id.rtk_guidance_step_previous).setOnClickListener(this);
        findViewById(R.id.rtk_guidance_step_next).setOnClickListener(this);
        findViewById(R.id.rtk_guidance_replay_btn).setOnClickListener(this);
    }

    private void initWidgetParams() {
        setLayoutParams(new Constraints.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setBackgroundResource(R.color.uxsdk_white);
        mLottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                updateReplayView(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                updateReplayView(true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //动画取消,无需操作
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                updateReplayView(false);
            }
        });
    }

    private void initParams(Context context) {
        mStep = GuidanceMode.STEP_FIRST;
        mTitleStr = context.getResources().getStringArray(R.array.uxsdk_rtk_guidance_title);
        mContentStr = context.getResources().getStringArray(R.array.uxsdk_rtk_guidance_content);
        mAnimationRes = new int[]{
                R.raw.uxsdk_rtk_guidance_reset,
                R.raw.uxsdk_rtk_guidance_step1,
                R.raw.uxsdk_rtk_guidance_step2,
                R.raw.uxsdk_rtk_guidance_step3
        };
        mImageAssetsFolderStr = new String[]{
                "rtk_guidance_reset_images",
                "rtk_guidance_step1_images",
                "rtk_guidance_step2_images",
                "rtk_guidance_step3_images"
        };
    }

    private void updateView() {
        //更新视图可见性
        updateVisibility();
        //更新顶部导航栏
        if (mStep != GuidanceMode.STEP_RESET) {
            mStepFirstTv.setVisibility(mStep <= GuidanceMode.STEP_FIRST ? VISIBLE : GONE);
            mStepSecondTv.setVisibility(mStep <= GuidanceMode.STEP_SECOND ? VISIBLE : GONE);

            updateSelectedTopView(mStepSecondTv, mStep < GuidanceMode.STEP_SECOND);
            updateSelectedTopView(mStepThirdTv, mStep < GuidanceMode.STEP_THIRD);

            mStepFirstIv.setVisibility(mStep > GuidanceMode.STEP_FIRST ? VISIBLE : GONE);
            mStepSecondIv.setVisibility(mStep > GuidanceMode.STEP_SECOND ? VISIBLE : GONE);

            mStepStartDivider.setBackgroundResource(mStep == GuidanceMode.STEP_FIRST ? R.color.uxsdk_gray : R.color.uxsdk_blue);
            mStepEndDivider.setBackgroundResource(mStep <= GuidanceMode.STEP_SECOND ? R.color.uxsdk_gray : R.color.uxsdk_blue);
        }
        //更新左边动画
        mLottieAnimationView.setImageAssetsFolder(mImageAssetsFolderStr[mStep]);
        mLottieAnimationView.setAnimation(mAnimationRes[mStep]);
        mLottieAnimationView.playAnimation();
        //在步骤2中更新右边动画中 tip 视图的显示
        updateLottieTipView();
        //更新右边文案
        mTitleTv.setText(mTitleStr[mStep]);
        mContentTv.setText(mContentStr[mStep]);
        mNextBtn.setText(StringUtils.getResStr(mStep == GuidanceMode.STEP_RESET
                || mStep == GuidanceMode.STEP_THIRD ? R.string.uxsdk_rtk_guidance_btn_finish : R.string.uxsdk_rtk_guidance_btn_next));
    }

    private void updateVisibility() {
        mNavigationLayout.setVisibility(mStep == GuidanceMode.STEP_RESET ? INVISIBLE : VISIBLE);
        mImageView.setVisibility(mStep == GuidanceMode.STEP_SECOND ? VISIBLE : INVISIBLE);
        mImageDescTv.setVisibility(mStep == GuidanceMode.STEP_SECOND ? VISIBLE : INVISIBLE);
        mPreviousBtn.setVisibility(mStep == GuidanceMode.STEP_RESET
                || mStep == GuidanceMode.STEP_FIRST ? GONE : VISIBLE);
    }

    /**
     * 更新顶部步骤view
     *
     * @param textView   步骤view
     * @param isSelected 是否被选中
     */
    private void updateSelectedTopView(TextView textView, boolean isSelected) {
        textView.setBackgroundResource(isSelected ?
                R.drawable.uxsdk_bg_rtk_guidance_step_oval_gray :
                R.drawable.uxsdk_bg_rtk_guidance_step_oval_blue_solid);
        textView.setTextColor(isSelected ?
                getResColor(R.color.uxsdk_black) :
                getResColor(R.color.uxsdk_white));
    }

    /**
     * 更新动画上方提示语
     */
    private void updateLottieTipView() {
        removeCallbacks(mTipRunnable);
        mLottieTip.setVisibility(GONE);
        if (mStep == GuidanceMode.STEP_RESET) {
            delayedUpdateResetTipView();
        }
    }

    /**
     * 延迟更新动画上方提示语——重置成功
     */
    private void delayedUpdateResetTipView() {
        mTipRunnable = () -> {
            mLottieTip.setText(R.string.uxsdk_rtk_guidance_reset_tip);
            mLottieTip.setVisibility(mStartAnimation ? VISIBLE : GONE);
            if (mStartAnimation) {
                mStartAnimation = false;
                postDelayed(mTipRunnable, 2500);
            }
        };
        mStartAnimation = true;
        //重置在14秒后显示重置成功
        postDelayed(mTipRunnable, 14000);
    }

    public void updateReplayView(boolean isShow) {
        int visible = isShow ? VISIBLE : GONE;
        mReplayView.setVisibility(visible);
        mReplayBtn.setVisibility(visible);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mTipRunnable);
        mStep = GuidanceMode.STEP_FIRST;
        //停止动画
        if (mLottieAnimationView != null && mLottieAnimationView.isAnimating()) {
            mLottieAnimationView.cancelAnimation();
        }
        super.onDetachedFromWindow();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rtk_guidance_close) {
            closePopupWindow();
        } else if (id == R.id.rtk_guidance_step_previous) {
            mStep--;
            updateView();
        } else if (id == R.id.rtk_guidance_step_next) {
            if (mStep != GuidanceMode.STEP_RESET && mStep != GuidanceMode.STEP_THIRD) {
                mStep++;
                updateView();
            } else {
                closePopupWindow();
            }
        } else if (id == R.id.rtk_guidance_replay_btn) {
            mLottieAnimationView.playAnimation();
            updateLottieTipView();
        }
    }


    private void closePopupWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }

    public void showPopupWindow(View parent) {
        showPopupWindow(parent, GuidanceMode.STEP_FIRST);
    }

    public void showPopupWindow(View parent, int guidanceMode) {
        if (guidanceMode < GuidanceMode.STEP_RESET || guidanceMode > GuidanceMode.STEP_THIRD) {
            return;
        }
        mStep = guidanceMode;
        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(this, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, true);
        }
        updateView();
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);
    }


    public int getResColor(@ColorRes int resId) {
        if (getContext() != null) {
            return getContext().getResources().getColor(resId);
        }
        return 0;
    }
}
