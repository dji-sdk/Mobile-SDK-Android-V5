package dji.v5.ux.core.base;
/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;



import java.util.ArrayList;
import java.util.List;

import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.ContextUtil;
import dji.v5.ux.R;

/**
 * <p>Created by luca on 2017/1/19.</p>
 */

public class TabSelectCell extends DividerConstraintLayout {

    TabModeWidget mTabWidget;
    TextView mSummary;
    TextView mTitle;
    TextView mDesc;
    private OnTabChangeListener mChangeListener;

    public TabSelectCell(Context context) {
        this(context, null);
    }

    public TabSelectCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabSelectCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_cell_tab_select_layout, this, true);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TabSelectCell, defStyleAttr, 0);

        mTabWidget = (TabModeWidget) findViewById(R.id.tab_switcher);
        mSummary = (TextView) findViewById(R.id.summary);
        mTitle = (TextView) findViewById(R.id.title);
        mDesc = findViewById(R.id.desc);

        if (ta.hasValue(R.styleable.TabSelectCell_uxsdk_title)) {
            mTitle.setText(ta.getString(R.styleable.TabSelectCell_uxsdk_title));
        } else {
            mTitle.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.TabSelectCell_uxsdk_summary)) {
            mSummary.setText(ta.getString(R.styleable.TabSelectCell_uxsdk_summary));
        } else {
            mSummary.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.TabSelectCell_uxsdk_desc)) {
            mDesc.setText(ta.getString(R.styleable.TabSelectCell_uxsdk_desc));
        } else {
            mDesc.setVisibility(GONE);
        }

        int selectIndex = ta.getInt(R.styleable.TabSelectCell_uxsdk_select, 0);

        // 可以设置自定义背景色
        List<String> tabs = new ArrayList<String>();
        if (ta.hasValue(R.styleable.TabSelectCell_uxsdk_tabs)) {
            CharSequence[] entries = ta.getTextArray(R.styleable.TabSelectCell_uxsdk_tabs);
            if (entries != null && entries.length > 0) {
                for (CharSequence s: entries) {
                    tabs.add(s.toString());
                }
                mTabWidget.setTabs(selectIndex, tabs);
            }
        }
        ta.recycle();

        mTabWidget.setCurrentTab(selectIndex);
        mTabWidget.setOnTabChangeListener((oldIndex, newIndex) -> {
            if (mChangeListener != null) {
                mChangeListener.onTabChanged(TabSelectCell.this, oldIndex, newIndex);
            }
        });

        setMinHeight((int) AndUtil.getItemHeight(ContextUtil.getContext()));
        setClickable(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTabWidget.setEnabled(enabled);
    }

    public void addTabs(int select, List<String> tabs) {
        mTabWidget.setTabs(select, tabs);
    }

    public void setCurrentTab(int tab) {
        mTabWidget.setCurrentTab(tab);
    }
    public void setCurrentDes(int resId) {
        mDesc.setText(resId);
    }
    public int getTabSize(){
        return mTabWidget.getTabSize();
    }

    public int getCurrentTab() {
        return mTabWidget.getCurrentTab();
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        mChangeListener = listener;
    }

    public interface OnTabChangeListener {
        void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex);
    }
}
