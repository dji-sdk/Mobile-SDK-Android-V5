package dji.v5.ux.core.base;
/*
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;


public class TabGroupWidget extends RadioGroup implements RadioGroup.OnCheckedChangeListener{

    private int mPadding;
    private int mTextSize;
    private int mTextColor;
    private CoverStyle mCoverStyle;
    private int mCheckedIndex;
    private boolean mFixedSize;
    private boolean mTextBold;
    private List<String> mTabs;
    private OnTabChangeListener mOnTabChangeListener;


    public TabGroupWidget(Context context) {
        this(context, null);
    }

    public TabGroupWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        super.setOnCheckedChangeListener(this);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TabGroupWidget, 0, 0);
        mFixedSize = ta.getBoolean(R.styleable.TabGroupWidget_uxsdk_fixedSize, true);
        mTextSize = ta.getDimensionPixelSize(R.styleable.TabGroupWidget_uxsdk_textSize, AndUtil.dip2px(getContext(), 9));
        mTextColor = ta.getColor(R.styleable.TabGroupWidget_uxsdk_textColor, AndUtil.getResColor(R.color.uxsdk_dic_color_c9_blue));
        mTextBold = ta.getBoolean(R.styleable.TabGroupWidget_uxsdk_textBold, false);
        mCoverStyle = CoverStyle.index(ta.getInt(R.styleable.TabGroupWidget_uxsdk_coverStyle, CoverStyle.BLUE.ordinal()));
        mPadding = ta.getDimensionPixelSize(R.styleable.TabGroupWidget_uxsdk_tabsPadding, (int) context.getResources().getDimension(R.dimen.uxsdk_3_dp));

        // 可以设置自定义背景色
        mTabs = new ArrayList<String>();
        if (ta.hasValue(R.styleable.TabGroupWidget_uxsdk_tabs)) {
            CharSequence[] entries = ta.getTextArray(R.styleable.TabGroupWidget_uxsdk_tabs);
            if (entries != null && entries.length > 0) {
                for (CharSequence s: entries) {
                    mTabs.add(s.toString());
                }
                setTabs();
            }
        }

        ta.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && mFixedSize && mTabs.size() > 0) {
            // 为了适配crystalsky，不能用weight
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int eachWidth = width / mTabs.size();

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)child.getLayoutParams();
                layoutParams.width = eachWidth;
                child.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setEnabled(enabled);
        }
    }


    @Override
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        LogUtils.e("TabGroupWidget", "Please use setOnTabChangeListener instead.");
    }

    private void setTabs() {

        removeAllViews();
        for (int i = 0, size = mTabs.size(); i < size; ++i) {
            RadioButton tab = new RadioButton(getContext());
            tab.setId(i);
            tab.setButtonDrawable(null);
            tab.setGravity(Gravity.CENTER);
            tab.setText(mTabs.get(i));
            tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
            tab.setPadding(mPadding, mPadding, mPadding, mPadding);
            tab.setSingleLine(true);
            tab.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            if (mTextBold) {
                tab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }

            initTabBackground(tab, i, size);


            if (i == mCheckedIndex) {
                tab.setChecked(true);
            }
            setTextColorByType(tab, i == mCheckedIndex);
            addView(tab);
        }
    }

    private void initTabBackground(RadioButton tab, int i, int size) {
        if (i == 0) {
            tab.setBackgroundResource(mCoverStyle.mResStart);
        } else if (i == size - 1) {
            tab.setBackgroundResource(mCoverStyle.mResEnd);
        } else {
            tab.setBackgroundResource(mCoverStyle.mResMid);
        }
    }

    private void setTextColorByType(RadioButton tab, boolean checked) {
        if (checked) {
            tab.setTextColor(AndUtil.getResColor(mCoverStyle.mResTextColorChecked));
        } else {
            tab.setTextColor(mTextColor);
        }
    }

    public void setItems(List<String> items) {
        mTabs.clear();
        if (items != null && !items.isEmpty()) {
            mTabs.addAll(items);
        }
        setTabs();
    }

    public void setItems(String[] items) {
        mTabs.clear();
        if (items != null && items.length > 0) {
            mTabs.addAll(Arrays.asList(items));
        }
        setTabs();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        RadioButton curChild;
        for (int i = 0, childCount = getChildCount(); i < childCount; ++i) {
            curChild = (RadioButton) getChildAt(i);
            if (curChild.getId() == checkedId) {
                setTextColorByType(curChild, true);

                if (i != mCheckedIndex) {
                    RadioButton oldChild = (RadioButton) getChildAt(mCheckedIndex);
                    setTextColorByType(oldChild, false);
                    if (mOnTabChangeListener != null) {
                        mOnTabChangeListener.onTabChanged(mCheckedIndex, i);
                    }
                    mCheckedIndex = i;
                }
                break;
            }
        }
    }

    public void setOnTabChangeListener(OnTabChangeListener onTabChangeListener) {
        mOnTabChangeListener = onTabChangeListener;
    }

    public int getCheckedIndex() {
        return mCheckedIndex;
    }

    public void setCheckedIndex(int checkedIndex) {
        if (checkedIndex < getChildCount()) {
            RadioButton tab = (RadioButton) getChildAt(checkedIndex);
            tab.setChecked(true);
        }
    }

    public void updateTabDefaultNames(int size){
        for(int i = size ; i < getChildCount() ; i ++){
            RadioButton tab = (RadioButton) getChildAt(i);
            tab.setVisibility(GONE);
        }
    }

    public void updateTabNames(List<String> names){
        if(names == null){
            return;
        }

        int index = 0;
        for(String name : names){
            if (index < getChildCount()) {
                RadioButton tab = (RadioButton) getChildAt(index);
                tab.setText(name);
                tab.setVisibility(VISIBLE);
            }
            index ++;
        }

        for(int i = index ; i < getChildCount() ; i ++){
            RadioButton tab = (RadioButton) getChildAt(i);
            tab.setVisibility(GONE);
        }
    }

    public enum CoverStyle {
        /**
         * 选中颜色为蓝色
         */
        BLUE(R.color.uxsdk_dic_color_c8_white, R.drawable.uxsdk_tab_item_start_blue_style, R.drawable.uxsdk_tab_item_mid_blue_style, R.drawable.uxsdk_tab_item_end_blue_style),
        /**
         * 选中颜色为白色
         */
        WHITE(R.color.uxsdk_dic_color_c4_black, R.drawable.uxsdk_tab_item_start_white_style, R.drawable.uxsdk_tab_item_mid_white_style, R.drawable.uxsdk_tab_item_end_white_style),
        /**
         * 视觉 2.0 新增白色样式，未选中是背景没有描边
         */
        WHITE_V2(R.color.uxsdk_black_95_percent, R.drawable.uxsdk_tab_item_start_white_v2_style, R.drawable.uxsdk_tab_item_mid_white_v2_style, R.drawable.uxsdk_tab_item_end_white_v2_style),

        /**
         * 黑色主题
         */
        BLACK(R.color.uxsdk_white, R.drawable.uxsdk_tab_item_start_black_style, R.drawable.uxsdk_tab_item_mid_black_style, R.drawable.uxsdk_tab_item_end_black_style);

        private final int mResTextColorChecked;
        private final int mResStart;
        private final int mResMid;
        private final int mResEnd;

        CoverStyle(@ColorRes int resTextColorChecked, @DrawableRes int resStart, @DrawableRes int resMid, @DrawableRes int redEnd) {
            mResTextColorChecked = resTextColorChecked;
            mResStart = resStart;
            mResMid = resMid;
            mResEnd = redEnd;
        }

        private static CoverStyle index(int index) {
            CoverStyle[] values = values();
            if (index < 0 || index >= values.length) {
                return CoverStyle.BLUE;
            }
            return values[index];
        }
    }

    public interface OnTabChangeListener {
        /**
         * Tab的选中状态改变事件通知
         * @param oldIndex 上一个选中状态的index
         * @param newIndex 当前选中的index
         */
        void onTabChanged(int oldIndex, int newIndex);
    }
}
