package dji.v5.ux.core.ui.setting.taplayout;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.ContextUtil;
import dji.v5.ux.R;


public class QTabView extends TabView {
    private Context mContext;
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mBadge;
    private int mMinHeight;
    private TabIcon mTabIcon;
    private TabTitle mTabTitle;
    private boolean mChecked;
    private LinearLayout mContainer;
    private GradientDrawable gd;

    public QTabView(Context context) {
        super(context);
        mContext = context;
        gd = new GradientDrawable();
        gd.setColor(0xFFE84E40);
        mMinHeight = dp2px(30);
        mTabIcon = new TabIcon.Builder().build();
        mTabTitle = new TabTitle.Builder(context).build();
        initView();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, new int[]{android.R.attr.state_checked});
        }
        return drawableState;
    }

    private void initView() {
        initContainer();
        initIconView();
        initTitleView();
        initBadge();
        addView(mContainer);
        addView(mBadge);
    }

    private void initContainer() {
        mContainer = new LinearLayout(mContext);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setMinimumHeight(mMinHeight);
        mContainer.setPadding(dp2px(5), dp2px(5), dp2px(5), dp2px(5));
        mContainer.setGravity(Gravity.CENTER);
    }

    private void initBadge() {
        mBadge = new TextView(mContext);
        FrameLayout.LayoutParams
                params2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params2.gravity = Gravity.RIGHT | Gravity.TOP;
        params2.setMargins(0, dp2px(5), dp2px(5), 0);
        mBadge.setLayoutParams(params2);
        mBadge.setGravity(Gravity.CENTER);
        mBadge.setTextColor(0xFFFFFFFF);
        mBadge.setTextSize(9);
        setBadge(0);
    }

    private void initTitleView() {
        if (mTitle != null) {
            mContainer.removeView(mTitle);
        }
        mTitle = new TextView(mContext);
        LinearLayout.LayoutParams
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        mTitle.setLayoutParams(params);
        mTitle.setTextColor(mTabTitle.mColorNormal);
        mTitle.setTextSize(mTabTitle.mTitleTextSize);
        mTitle.setText(mTabTitle.mContent);
        mTitle.setGravity(Gravity.LEFT);
        mTitle.setSingleLine();
        mTitle.setEllipsize(TextUtils.TruncateAt.END);
        mTitle.setPadding(dp2px(5), dp2px(5), dp2px(5), dp2px(5));
        requestContainerLayout(mTabIcon.mIconGravity);
    }

    private void initIconView() {
        if (mIcon != null) {
            mContainer.removeView(mIcon);
        }
        mIcon = new ImageView(mContext);
        int dp = AndUtil.isTabletDevice(ContextUtil.getContext()) ? 10 : 7;
        mIcon.setPadding(dp2px(dp), dp2px(dp), dp2px(dp), dp2px(dp));
        mIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.uxsdk_38_dp),
                getResources().getDimensionPixelSize(R.dimen.uxsdk_38_dp));
        mIcon.setLayoutParams(params);
        if (mTabIcon.mNormalIcon != 0) {
            mIcon.setImageResource(mTabIcon.mNormalIcon);
        } else {
            mIcon.setVisibility(View.GONE);
        }
        if (mTabIcon.mBackground != 0) {
            mIcon.setBackgroundResource(mTabIcon.mBackground);
        }
        requestContainerLayout(mTabIcon.mIconGravity);
    }

    private void setBadgeImp(int num) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mBadge.getLayoutParams();
        if (num <= 9) {
            lp.width = dp2px(12);
            lp.height = dp2px(12);
            gd.setShape(GradientDrawable.OVAL);
            mBadge.setPadding(0, 0, 0, 0);
        } else {
            lp.width = FrameLayout.LayoutParams.WRAP_CONTENT;
            lp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            mBadge.setPadding(dp2px(3), 0, dp2px(3), 0);
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(dp2px(6));
        }
        mBadge.setLayoutParams(lp);
        mBadge.setBackgroundDrawable(gd);
        mBadge.setText(String.valueOf(num));
        mBadge.setVisibility(View.VISIBLE);
    }

    @Override
    public QTabView setBadge(int num) {
        if (num > 0) {
            setBadgeImp(num);
        } else {
            mBadge.setText("");
            mBadge.setVisibility(View.GONE);
        }
        return this;
    }

    public QTabView setIcon(TabIcon icon) {
        if (icon != null) {
            mTabIcon = icon;
        }
        initIconView();
        setChecked(mChecked);
        return this;
    }

    public QTabView setTitle(TabTitle title) {
        if (title != null) {
            mTabTitle = title;
        }
        initTitleView();
        setChecked(mChecked);
        return this;
    }

    public QTabView setBackground(int resId) {
        super.setBackgroundResource(resId);
        return this;
    }

    private void requestContainerLayout(int gravity) {
        mContainer.removeAllViews();
        switch (gravity) {
            case Gravity.LEFT:
                mContainer.setOrientation(LinearLayout.HORIZONTAL);
                setIconAndTitle();
                break;
            case Gravity.TOP:
                mContainer.setOrientation(LinearLayout.VERTICAL);
                setIconAndTitle();

                break;
            case Gravity.RIGHT:
                mContainer.setOrientation(LinearLayout.HORIZONTAL);
                setTitleAndICon();
                break;
            case Gravity.BOTTOM:
                mContainer.setOrientation(LinearLayout.VERTICAL);
                setTitleAndICon();
                break;
            case Gravity.CENTER:
                mContainer.setOrientation(LinearLayout.VERTICAL);
                if (mIcon != null) {
                    mContainer.addView(mIcon);
                }
                if (mTitle != null && !TextUtils.isEmpty(mTitle.getText().toString())) {
                    mContainer.addView(mTitle);
                }
                break;
            default:
                break;
        }
    }

    private void setIconAndTitle() {
        if (mIcon != null) {
            mContainer.addView(mIcon);
        }
        if (mTitle != null && !TextUtils.isEmpty(mTitle.getText().toString())) {
            mContainer.addView(mTitle);
        }
    }

    private void setTitleAndICon() {
        if (mTitle != null) {
            mContainer.addView(mTitle);
        }
        if (mIcon != null) {
            mContainer.addView(mIcon);
        }
    }

    protected int dp2px(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        refreshDrawableState();
        setSelected(mChecked);
        if (mChecked) {
            mTitle.setTextColor(mTabTitle.mColorSelected);
            if (mTabIcon.mSelectedIcon != 0) {
                mIcon.setVisibility(View.VISIBLE);
                mIcon.setImageResource(mTabIcon.mSelectedIcon);
            } else {
                mIcon.setVisibility(View.GONE);
            }
        } else {
            mTitle.setTextColor(mTabTitle.mColorNormal);
            if (mTabIcon.mNormalIcon != 0) {
                mIcon.setVisibility(View.VISIBLE);
                mIcon.setImageResource(mTabIcon.mNormalIcon);
            } else {
                mIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    public static class TabIcon {
        public int mSelectedIcon;
        public int mNormalIcon;
        public int mIconGravity;
        public int mIconWidth;
        public int mIconHeight;
        public int mBackground;

        private TabIcon(int mSelectedIcon, int mNormalIcon, int mIconGravity, int mIconWidth, int mIconHeight, int mBackground) {
            this.mSelectedIcon = mSelectedIcon;
            this.mNormalIcon = mNormalIcon;
            this.mIconGravity = mIconGravity;
            this.mIconWidth = mIconWidth;
            this.mIconHeight = mIconHeight;
            this.mBackground = mBackground;
        }

        public static class Builder {
            private int mSelectedIcon;
            private int mNormalIcon;
            private int mIconGravity;
            private int mIconWidth;
            private int mIconHeight;
            private int mBackground;

            public Builder() {
                mSelectedIcon = 0;
                mNormalIcon = 0;
                mIconWidth = FrameLayout.LayoutParams.WRAP_CONTENT;
                mIconHeight = FrameLayout.LayoutParams.WRAP_CONTENT;
                mIconGravity = Gravity.LEFT;
                mBackground = 0;
            }

            public Builder setIcon(int selectIconResId, int normalIconResId) {
                mSelectedIcon = selectIconResId;
                mNormalIcon = normalIconResId;
                return this;
            }

            public Builder setBackground(int background) {
                mBackground = background;
                return this;
            }

            public Builder setIconSize(int width, int height) {
                mIconWidth = width;
                mIconHeight = height;
                return this;
            }

            public Builder setIconGravity(int gravity) {
                if (gravity != Gravity.LEFT && gravity != Gravity.RIGHT
                        && gravity != Gravity.TOP && gravity != Gravity.BOTTOM && gravity != Gravity.CENTER) {
                    throw new IllegalStateException("iconGravity only support Gravity.LEFT " +
                            "or Gravity.RIGHT or Gravity.TOP or Gravity.BOTTOM");
                }
                mIconGravity = gravity;
                return this;
            }

            public TabIcon build() {
                return new TabIcon(mSelectedIcon, mNormalIcon, mIconGravity, mIconWidth, mIconHeight, mBackground);
            }
        }
    }

    public static class TabTitle {
        public int mColorSelected;
        public int mColorNormal;
        public int mTitleTextSize;
        public String mContent;

        private TabTitle(int mColorSelected, int mColorNormal, int mTitleTextSize, String mContent) {
            this.mColorSelected = mColorSelected;
            this.mColorNormal = mColorNormal;
            this.mTitleTextSize = mTitleTextSize;
            this.mContent = mContent;
        }

        public static class Builder {
            private int mColorSelected;
            private int mColorNormal;
            private int mTitleTextSize;
            private String mContent;

            public Builder(Context context) {
                this.mColorSelected = context.getResources().getColor(R.color.uxsdk_blue_highlight);
                this.mColorNormal = 0xFF757575;
                this.mTitleTextSize = 16;
                this.mContent = null;
            }

            public Builder setTextColor(int colorSelected, int colorNormal) {
                mColorSelected = colorSelected;
                mColorNormal = colorNormal;
                return this;
            }

            public Builder setTextSize(int sizeSp) {
                mTitleTextSize = sizeSp;
                return this;
            }

            public Builder setContent(String content) {
                mContent = content;
                return this;
            }

            public TabTitle build() {
                return new TabTitle(mColorSelected, mColorNormal, mTitleTextSize, mContent);
            }
        }
    }
}