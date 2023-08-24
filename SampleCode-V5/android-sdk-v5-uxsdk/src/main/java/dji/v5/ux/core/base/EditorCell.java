package dji.v5.ux.core.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;


import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;

/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

public class EditorCell extends DividerConstraintLayout {

    protected EditText mEditor;
    protected TextView mSummary;
    protected TextView mTitle;
    protected TextView mTips;

    private int min;
    private int max;
    private  boolean needChangeValue = false;

    private String mOldValue;
    private OnValueChangedListener mListener;

    private boolean isFocusChangeValueCallBack = true;

    public EditorCell(Context context) {
        this(context, null);
    }

    public EditorCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditorCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_cell_editor_layout, this, true);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EditorCell, defStyleAttr, 0);

        mEditor = (EditText) findViewById(R.id.editor);
        mEditor.setSaveEnabled(false);
        mSummary = (TextView) findViewById(R.id.summary);
        mTitle = (TextView) findViewById(R.id.title);
        mTips = (TextView) findViewById(R.id.tips);

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_title)) {
            mTitle.setText(ta.getString(R.styleable.EditorCell_uxsdk_title));
        } else {
            mTitle.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_summary)) {
            mSummary.setText(ta.getString(R.styleable.EditorCell_uxsdk_summary));
        } else {
            mSummary.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_tips)) {
            mTips.setText(ta.getString(R.styleable.EditorCell_uxsdk_tips));
        } else {
            mTips.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_tipsTextSize)) {

            mTips.setTextSize(TypedValue.COMPLEX_UNIT_PX,ta.getDimensionPixelSize(R.styleable.EditorCell_uxsdk_tipsTextSize, getResources().getDimensionPixelSize(R.dimen.uxsdk_horizontal_margin_medium)));
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_titleTextSize)) {
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimensionPixelSize(R.styleable.EditorCell_uxsdk_titleTextSize, getResources().getDimensionPixelSize(R.dimen.uxsdk_dic_text_size_18sp)));
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_summaryTextSize)) {
            mTips.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimensionPixelSize(R.styleable.EditorCell_uxsdk_summaryTextSize, getResources().getDimensionPixelSize(R.dimen.uxsdk_dic_text_size_16sp)));
        }

        min = ta.getInt(R.styleable.EditorCell_uxsdk_minValue, -1);
        max = ta.getInt(R.styleable.EditorCell_uxsdk_maxValue, -1);

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_value)) {
            mEditor.setText(ta.getString(R.styleable.EditorCell_uxsdk_value));
        }

        if (ta.hasValue(R.styleable.EditorCell_uxsdk_layout)) {
            int layout = ta.getResourceId(R.styleable.EditorCell_uxsdk_layout, 0);
            if (layout > 0) {
                View view = LayoutInflater.from(context).inflate(layout, null);
                addView(view);
            }
        }

        ta.recycle();
        setRegion();
        setMinHeight((int) AndUtil.getItemHeight(getContext()));
    }

    public void setValue(int value) {
        mEditor.setText(String.valueOf(value));
        mOldValue = null;
    }

    public int getValue() {
        try {
            return Integer.parseInt(mEditor.getText().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    protected void setRegion() {
        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //add log
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mOldValue == null) {
                    mOldValue = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && !"".equals(s.toString()) && (min != -1 || max != -1)) {

                        int num;
                        try {
                            num = Integer.parseInt(s.toString());
                        } catch (NumberFormatException e) {
                            num = min - 1;
                        }

                        if (num < min || num > max) {
                            mEditor.setTextColor(ContextCompat.getColor(getContext(), R.color.uxsdk_dic_color_c13_red));
                        } else {
                            mEditor.setTextColor(ContextCompat.getColor(getContext(), R.color.uxsdk_edit_cell_text_color));
                        }

                }
            }
        });

        setClickLister();

        setOnfocusChange();

    }

    private void setOnfocusChange() {
        mEditor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && mOldValue != null && isFocusChangeValueCallBack) {
                mEditor.setText(mOldValue);
                mOldValue = null;
            }
        });
    }

    public void setClickLister(){
        mEditor.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                AndUtil.hideSoftInput(v);
                int markVal;
                try {
                    markVal = Integer.parseInt(mEditor.getText().toString());
                } catch (NumberFormatException e) {
                    markVal = 0;
                }
                setEditText( markVal);
                mEditor.setSelection(mEditor.getText().length());
                if (mListener != null) {
                    mListener.onValueChanged(EditorCell.this, markVal, getValue());
                }
                mOldValue = null;
                mEditor.clearFocus();
            }
            return false;
        });

    }

    private void setEditText(int markVal) {
        if (needChangeValue) {
            if (markVal < min) {
                mEditor.setText(String.valueOf(min));
            } else if (markVal > max) {
                mEditor.setText(String.valueOf(max));
            }
        }
    }


    public void needChangeValueWhenOutofLimit(boolean enable){
        needChangeValue  = enable;
    }

    public void setTips(String tips) {
        mTips.setText(tips);
    }

    public void setMaxValue(int max) {
        this.max = max;
    }

    public int getMaxValue(){
        return max;
    }

    public void setMinValue(int min) {
        this.min = min;
    }

    public int getMinValue(){
        return min;
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mListener = listener;
    }

    public boolean isFocusChangeValueCallBack() {
        return isFocusChangeValueCallBack;
    }

    public void setEnable(boolean enable) {
        mEditor.setEnabled(enable);
    }

    public void setFocusChangeValueCallBack(boolean isFocusChangeValueCallBack) {
        this.isFocusChangeValueCallBack = isFocusChangeValueCallBack;
    }

    public interface OnValueChangedListener {
        /**
         *
         * @param inputValue  原始输入值
         * @param validValue  实际有效值，如果输入值超过最小值或最大值则改为最小值或最大值
         */
        void onValueChanged(EditorCell cell, int inputValue, int validValue);
    }
}
