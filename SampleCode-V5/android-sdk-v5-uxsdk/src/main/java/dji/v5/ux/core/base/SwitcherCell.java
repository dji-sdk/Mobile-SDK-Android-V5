package dji.v5.ux.core.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.view.ViewCompat;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.util.ViewUtil;



/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

/**
 * @author Luca.Wu
 * <p>Created by luca on 2017/1/16.</p>
 */

public class SwitcherCell extends DividerConstraintLayout {

    protected ToggleButton mSwitcher;
    protected TextView mSummary;
    protected TextView mTitle;
    protected TextView mDesc;
    protected View mDisableHintView;
    protected String mDisableHintText;
    private View mInfoIcon;

    protected String mPopupHintText;
    private OnCheckedChangedListener mListener;
    private OnClickListener mPopupHintClickListener;
    private OnClickListener mDisableClickListener;

    public SwitcherCell(Context context) {
        this(context, null);
    }

    public SwitcherCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitcherCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_cell_switcher_layout, this, true);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwitcherCell, defStyleAttr, 0);

        mSwitcher = (ToggleButton) findViewById(R.id.toggle_switcher);
        mSummary = (TextView) findViewById(R.id.summary);
        mTitle = (TextView) findViewById(R.id.title);
        mDesc = (TextView) findViewById(R.id.desc);
        mDisableHintView = findViewById(R.id.disable_hint_view);
        mInfoIcon = findViewById(R.id.info_icon);

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_title)) {
            mTitle.setText(ta.getString(R.styleable.SwitcherCell_uxsdk_title));
        } else {
            mTitle.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_titleTextSize)) {
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.SwitcherCell_uxsdk_titleTextSize, 16));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_titleColor)) {
            mTitle.setTextColor(ta.getColor(R.styleable.SwitcherCell_uxsdk_titleColor, getResources().getColor(R.color.uxsdk_dic_color_c24_white_Transparent6)));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_summary)) {
            mSummary.setText(ta.getString(R.styleable.SwitcherCell_uxsdk_summary));
        } else {
            mSummary.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_summaryTextSize)) {
            mSummary.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.SwitcherCell_uxsdk_summaryTextSize, 14));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_summaryColor)) {
            mSummary.setTextColor(ta.getColor(R.styleable.SwitcherCell_uxsdk_summaryColor, getResources().getColor(R.color.uxsdk_dic_color_c8_white)));
        }

        setupDesc(ta);

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_checked)) {
            mSwitcher.setChecked(ta.getBoolean(R.styleable.SwitcherCell_uxsdk_checked, false));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_switchToggleBg)) {
            mSwitcher.setBackgroundResource(ta.getResourceId(R.styleable.SwitcherCell_uxsdk_switchToggleBg, R.drawable.uxsdk_toggle_green_selector));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_layout)) {
            int layout = ta.getResourceId(R.styleable.SwitcherCell_uxsdk_layout, 0);
            if (layout > 0) {
                View view = LayoutInflater.from(context).inflate(layout, null);
                addView(view);
            }
        }

        setupInfoView(ta);

        ta.recycle();

        // 避免View再次被初始化时默认执行一次onCheckedChanged方法
        ViewCompat.setSaveFromParentEnabled(this, false);
        mSwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mListener != null) {
                mListener.onCheckedChanged(SwitcherCell.this, isChecked);
            }
        });

        setMinHeight((int) AndUtil.getItemHeight(getContext()));

        setListeners();
    }

    private void setupDesc(TypedArray ta) {
        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_desc)) {
            mDesc.setText(ta.getString(R.styleable.SwitcherCell_uxsdk_desc));
        } else {
            mDesc.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_descColor)) {
            mDesc.setTextColor(ta.getColor(R.styleable.SwitcherCell_uxsdk_descColor, getResources().getColor(R.color.uxsdk_dic_color_c24_white_Transparent6)));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_descTextSize)) {
            mDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimensionPixelSize(R.styleable.SwitcherCell_uxsdk_descTextSize, getResources().getDimensionPixelSize(R.dimen.uxsdk_dic_text_size_16sp)));
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_descMarginVertical)) {
            int margin = ta.getDimensionPixelSize(R.styleable.SwitcherCell_uxsdk_descMarginVertical, getResources().getDimensionPixelSize(R.dimen.uxsdk_dic_text_size_16sp));
            ViewGroup.LayoutParams lp = mDesc.getLayoutParams();
            if (lp instanceof MarginLayoutParams) {
                MarginLayoutParams mlp = (MarginLayoutParams) lp;
                mlp.topMargin = margin;
                mlp.bottomMargin = margin;
                mDesc.setLayoutParams(mlp);
            }
        }
    }

    private void setupInfoView(TypedArray ta){
        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_disableHint)) {
            mDisableHintText = ta.getString(R.styleable.SwitcherCell_uxsdk_disableHint);
        }

        boolean showInfoIcon = false;
        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_showInfoHintIcon)) {
            showInfoIcon = ta.getBoolean(R.styleable.SwitcherCell_uxsdk_showInfoHintIcon, false);
        }

        if (ta.hasValue(R.styleable.SwitcherCell_uxsdk_infoPopupHint)) {
            mPopupHintText = ta.getString(R.styleable.SwitcherCell_uxsdk_infoPopupHint);
        }

        mInfoIcon.setVisibility(showInfoIcon || !TextUtils.isEmpty(mPopupHintText) ? VISIBLE : GONE);
    }

    private void setListeners() {
        mDisableHintView.setOnClickListener(v -> {
            // 如果控件置灰且有设置提示语，点击时有相关提示
            if (mSwitcher.isEnabled()) {
                return;
            }
            if (mDisableClickListener != null) {
                mDisableClickListener.onClick(v);
            } else if (!TextUtils.isEmpty(mDisableHintText) && !mSwitcher.isEnabled()) {
                ViewUtil.showToast(getContext() , mDisableHintText , Toast.LENGTH_SHORT);
            }
        });
        mInfoIcon.setOnClickListener(v -> {
            if (mPopupHintClickListener != null) {
                mPopupHintClickListener.onClick(v);
            } else if (!TextUtils.isEmpty(mPopupHintText)) {
                // xml 有配置静态的文案则可以直接显示

                ViewUtil.showToast(getContext() , mPopupHintText , Toast.LENGTH_SHORT);
            }
        });
    }

    public void setChecked(boolean checked) {
        mSwitcher.setChecked(checked);
    }

    public boolean isChecked() {
        return mSwitcher.isChecked();
    }

    public void setOnCheckedChangedListener(OnCheckedChangedListener listener) {
        mListener = listener;
    }

    /**
     * 重新设置值，不进行回调
     * 在部分使用场景，切换开关状态之后需要检查状态，检查失败需要重置状态，此时不希望有回调
     *
     * @param checked
     */
    public void resetValue(boolean checked) {
        OnCheckedChangedListener listener = mListener;
        mListener = null;
        setChecked(checked);
        mListener = listener;
    }

    public interface OnCheckedChangedListener {
        /**
         * Dispatch the event of ToggleButton
         * @param cell instance of view
         * @param isChecked checked or not
         */
        void onCheckedChanged(SwitcherCell cell, boolean isChecked);
    }

    public void updateSummaryText(String text) {
        if (mSummary != null) {
            if (mSummary.getVisibility() != VISIBLE) {
                mSummary.setVisibility(VISIBLE);
            }
            mSummary.setText(text);
        }
    }

    public void updateDesc(int text) {
        if (mDesc != null) {
            mDesc.setText(text);
        }
    }

    public void updateDesc(CharSequence desc) {
        mDesc.setText(desc);
        if (desc instanceof SpannableString) {
            mDesc.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    public void updateEnabledStatus(boolean enabled) {
        this.setEnabled(enabled);
        int textColor = enabled ? getContext().getResources().getColor(R.color.uxsdk_white) : getContext().getResources().getColor(R.color.uxsdk_gray);
        mSummary.setTextColor(textColor);
        mTitle.setTextColor(textColor);
        mInfoIcon.setAlpha(enabled ? 1f : 0.5f);
        // HYAPP-10169
        // mDesc.setTextColor(textColor);
        setToggleEnabled(enabled);
        mDisableHintView.setVisibility(enabled ? GONE : VISIBLE);
        if (!enabled) {
            mDisableHintView.setMinimumHeight(getMinHeight());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mSwitcher.setEnabled(enabled);
    }

    public void setToggleEnabled(boolean enabled) {
        mSwitcher.setEnabled(enabled);
        mSwitcher.setAlpha(enabled ? 1f : 0.5f);
    }

    public void setDisableHint(String disableHint) {
        mDisableHintText = disableHint;
    }

    public void setPopupHint(String popupHint) {
        mPopupHintText = popupHint;
    }

    public void setOnDisableHintClickListener(OnClickListener listener) {
        mDisableClickListener = listener;
    }

    public void setOnPopupHintClickListener(OnClickListener listener) {
        mPopupHintClickListener = listener;
    }
}
