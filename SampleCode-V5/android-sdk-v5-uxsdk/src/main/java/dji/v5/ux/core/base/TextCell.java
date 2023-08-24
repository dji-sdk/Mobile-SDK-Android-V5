package dji.v5.ux.core.base;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.view.IconicsImageView;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;

public class TextCell extends DividerConstraintLayout {

    protected TextView mTitle;
    protected ImageView mInfoIV;
    protected TextView mContent;
    protected TextView mSubhead;
    protected IconicsImageView mArrow;
    protected ImageView mIcon;
    protected TextView mSummary;

    public enum ArrowDirection {
        LEFT(1),
        TOP(2),
        RIGHT(3),
        BOTTOM(4),
        UNKNOWN(0);

        private int value;

        ArrowDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ArrowDirection find(int index) {
            ArrowDirection[] values = ArrowDirection.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getValue() == index) {
                    return values[i];
                }
            }
            return UNKNOWN;
        }
    }

    public TextCell(Context context) {
        this(context, null, 0);
    }

    public TextCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        inflate(context, getLayoutId(), this);
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TextCell, defStyleAttr, 0);

        mTitle = findViewById(R.id.title);
        mInfoIV = findViewById(R.id.info_iv);
        mContent = findViewById(R.id.content);
        mSubhead = findViewById(R.id.subhead);
        mArrow = findViewById(R.id.arrow);
        mIcon = findViewById(R.id.icon);
        mSummary = findViewById(R.id.summary);

        initTitleAndContent(ta);

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_infoImageView)) {
            mInfoIV.setBackground(ta.getDrawable(R.styleable.TextCell_uxsdk_text_cell_infoImageView));
            mInfoIV.setVisibility(VISIBLE);
        } else {
            mInfoIV.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_contentColor)) {
            mContent.setTextColor(ta.getColor(R.styleable.TextCell_uxsdk_text_cell_contentColor, ContextCompat.getColor(getContext(), R.color.uxsdk_dic_color_c24_white_Transparent6)));
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_contentTextSize)) {
            mContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.TextCell_uxsdk_text_cell_contentTextSize, 14));
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_arrow)) {
            int direction = ta.getInt(R.styleable.TextCell_uxsdk_text_cell_arrow, 0);
            setRightImageDirection(ArrowDirection.find(direction));
        }

        initSummary(ta);

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_subhead)) {
            String str = ta.getString(R.styleable.TextCell_uxsdk_text_cell_subhead);
            mSubhead.setVisibility(VISIBLE);
            mSubhead.setText(str);
        } else {
            mSubhead.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_drawable)) {
            String drawableStr = ta.getString(R.styleable.TextCell_uxsdk_text_cell_drawable);
            if (!TextUtils.isEmpty(drawableStr)) {
                IconicsDrawable drawable = new IconicsDrawable(getContext(), drawableStr);
                setRightImage(drawable);
            }
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_arrowRes)) {
            mArrow.setVisibility(VISIBLE);
            mArrow.setImageDrawable(ta.getDrawable(R.styleable.TextCell_uxsdk_text_cell_arrowRes));
        }
        ta.recycle();

        setMinHeight((int) AndUtil.getItemHeight(getContext()));

    }

    void initTitleAndContent(TypedArray ta) {
        mTitle.setText(ta.getString(R.styleable.TextCell_uxsdk_text_cell_title));
        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_content)) {
            mContent.setText(ta.getString(R.styleable.TextCell_uxsdk_text_cell_content));
        } else {
            mContent.setVisibility(INVISIBLE);
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_titleColor)) {
            mTitle.setTextColor(ta.getColor(R.styleable.TextCell_uxsdk_text_cell_titleColor, ContextCompat.getColor(getContext(), R.color.uxsdk_dic_color_c8_white)));
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_titleTextSize)) {
            Log.w("test", "" + ta.getDimension(R.styleable.TextCell_uxsdk_text_cell_titleTextSize, 16));
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.TextCell_uxsdk_text_cell_titleTextSize, 16));
        }
    }

    void initSummary(TypedArray ta) {
        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_summary)) {
            mSummary.setVisibility(VISIBLE);
            mSummary.setText(ta.getString(R.styleable.TextCell_uxsdk_text_cell_summary));
        } else {
            mSummary.setVisibility(GONE);
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_summaryColor)) {
            mSummary.setTextColor(ta.getColor(R.styleable.TextCell_uxsdk_text_cell_summaryColor, ContextCompat.getColor(getContext(), R.color.uxsdk_dic_color_c24_white_Transparent6)));
        }

        if (ta.hasValue(R.styleable.TextCell_uxsdk_text_cell_summaryTextSize)) {
            mSummary.setTextSize(TypedValue.COMPLEX_UNIT_PX, ta.getDimension(R.styleable.TextCell_uxsdk_text_cell_summaryTextSize, 12));
        }
    }

    /**
     * 用于修改布局，当前 TextCell 布局改动影响范围太大
     */
    protected int getLayoutId() {
        return R.layout.uxsdk_cell_text_layout;
    }

    public void setArrowResVisible(boolean visibility) {
        mArrow.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setTitleColor(@ColorInt int titleColor) {
        mTitle.setTextColor(titleColor);
    }

    public void setInfoImageVisibility(int visibility) {
        mInfoIV.setVisibility(visibility);
    }

    public void setContent(String content) {
        if (!TextUtils.isEmpty(content)) {
            mContent.setText(content);
            mContent.setVisibility(VISIBLE);
        } else {
            mContent.setVisibility(INVISIBLE);
        }
    }

    public void setContentColor(@ColorInt int contentColor) {
        mContent.setTextColor(contentColor);
    }

    public void setRightImage(IconicsDrawable icon) {
        if (icon != null) {
            mArrow.setIcon(icon);
            mArrow.setVisibility(VISIBLE);
        } else {
            mArrow.setVisibility(GONE);
        }
    }

    public void setRightImageVisibility(int visibility) {
        mArrow.setVisibility(visibility);
    }

    public void setRightImageDirection(ArrowDirection direction) {
        if (direction == null) {
            mArrow.setVisibility(GONE);
            return;
        }
        int imgRes = -1;
        switch (direction) {
            case BOTTOM: {
                imgRes = R.drawable.uxsdk_arrow_down;
                break;
            }
            case TOP: {
                imgRes = R.drawable.uxsdk_arrow_up;
                break;
            }
            case LEFT: {
                imgRes = R.drawable.uxsdk_arrow_left;
                break;
            }
            case RIGHT: {
                imgRes = R.drawable.uxsdk_arrow_right;
                break;
            }
            default: {
                break;
            }
        }
        if (imgRes != -1) {
            mArrow.setVisibility(VISIBLE);
            mArrow.setImageResource(imgRes);
        } else {
            mArrow.setVisibility(GONE);
        }
    }

    public void setIcon(@DrawableRes int resId) {
        if (resId == Resources.ID_NULL) {
            mIcon.setVisibility(GONE);
        } else {
            mIcon.setVisibility(VISIBLE);
            mIcon.setImageResource(resId);
        }
    }

    public void setIconBackground(Drawable background) {
        mIcon.setBackground(background);
    }

    public void setSummary(String summary) {
        if (TextUtils.isEmpty(summary)) {
            mSummary.setVisibility(GONE);
        } else {
            mSummary.setVisibility(VISIBLE);
            mSummary.setText(summary);
        }
    }

    public void setSummaryColor(@ColorInt int summaryColor) {
        if (summaryColor != 0) {
            mSummary.setTextColor(summaryColor);
        }
    }

    public void setInfoOnClickListener(OnClickListener listener) {
        if (mInfoIV != null) {
            mInfoIV.setOnClickListener(listener);
        }
    }

    public void setRightImageBackground(Drawable drawable) {
        mArrow.setBackground(drawable);
    }
}
