package dji.v5.ux.core.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;

/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */


public class TabModeWidget extends RoundedLinearLayout implements View.OnClickListener{

    private int mSelectIndex;
    private int mTextSize;
    private int mTextColor;
    private boolean mFixedSize;
    private List<String> mTabs;
    private OnTabChangeListener mOnTabChangeListener;

    public TabModeWidget(Context context) {
        this(context, null);
    }

    public TabModeWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabModeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        setBackgroundResource(R.drawable.uxsdk_selector_white_round_rect);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TabModeWidget, defStyleAttr, 0);

        mSelectIndex = ta.getInt(R.styleable.TabModeWidget_uxsdk_select, 0);
        mTextSize = ta.getDimensionPixelSize(R.styleable.TabModeWidget_uxsdk_textSize, AndUtil.dip2px(getContext(), 9));
        mTextColor = ta.getColor(R.styleable.TabModeWidget_uxsdk_textColor, Color.WHITE);
        mFixedSize = ta.getBoolean(R.styleable.TabModeWidget_uxsdk_fixedSize, false);

        // 可以设置自定义背景色
        mTabs = new ArrayList<String>();
        if (ta.hasValue(R.styleable.TabModeWidget_uxsdk_tabs)) {
            CharSequence[] entries = ta.getTextArray(R.styleable.TabModeWidget_uxsdk_tabs);
            if (entries != null && entries.length > 0) {
                for (CharSequence s: entries) {
                    mTabs.add(s.toString());
                }
                setTabs();
            }
        }
        ta.recycle();
    }

    private void setTabs() {
        for (int i = 0; i < mTabs.size(); ++i) {
            TextView tab;
            if (i == mSelectIndex) {
                tab = new TabBuilder().setTabName(mTabs.get(i))
                                      .setTextSize(mTextSize)
                                      .setTextColor(Color.BLACK)
                                      .setBackground(Color.WHITE)
                                      .build();
            } else {
                tab = new TabBuilder().setTabName(mTabs.get(i))
                                      .setTextSize(mTextSize)
                                      .setTextColor(mTextColor)
                                      .setBackground(Color.TRANSPARENT)
                                      .build();
            }
            tab.setOnClickListener(this);
            addView(tab);

            if (mFixedSize) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)tab.getLayoutParams();
                if (params != null) {
                    params.weight = 1;
                    params.width = 0;
                    tab.setLayoutParams(params);
                }
            }
        }
        invalidate();
    }

    public void setTabs(int select, List<String> tabs) {
        removeAllViews();
        mTabs.clear();
        mTabs.addAll(tabs);
        mSelectIndex = select;
        setTabs();
    }

    public int getTabSize(){
        return mTabs.size();
    }

    public void setCurrentTab(int tab) {
        View child = getChildAt(tab);
        if (child != null) {
            updateSelectedView(child);
        }
    }

    public int getCurrentTab() {
        return mSelectIndex;
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        mOnTabChangeListener = listener;
    }

    private void updateSelectedView(View view) {
        if (view == null) {
            return;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            TextView child = (TextView) getChildAt(i);
            if (child == null) {
                continue;
            }
            if (child == view) {
                child.setBackgroundColor(Color.WHITE);
                child.setTextColor(Color.BLACK);
                mSelectIndex = i;
            } else {
                child.setTextColor(mTextColor);
                child.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        invalidate();
    }
    @Override
    public void onClick(View view) {
        if (! isEnabled()) {
            return;
        }
        int oldIndex = mSelectIndex;
        updateSelectedView(view);
        if (mOnTabChangeListener != null) {
            mOnTabChangeListener.onTabChanged(oldIndex, mSelectIndex);
        }
    }

    protected class TabBuilder {
        String tabName;
        int textSize;
        int textColor;
        int background;

        public TabBuilder setTabName(String tabName) {
            this.tabName = tabName;
            return this;
        }

        public TabBuilder setTextSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        public TabBuilder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public TabBuilder setBackground(int background) {
            this.background = background;
            return this;
        }

        public TextView build() {
            TextView tab = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.uxsdk_widget_tab_item, null);
            tab.setText(tabName);
            tab.setTextColor(textColor);
            tab.setBackgroundColor(background);
            return tab;
        }
    }

    public interface OnTabChangeListener {
        void onTabChanged(int oldIndex, int newIndex);
    }
}
