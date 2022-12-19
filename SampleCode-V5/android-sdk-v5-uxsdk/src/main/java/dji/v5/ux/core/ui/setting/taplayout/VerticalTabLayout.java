package dji.v5.ux.core.ui.setting.taplayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import dji.v5.ux.R;


public class VerticalTabLayout extends ScrollView {
    private Context mContext;
    private TabStrip mTabStrip;
    private int mColorIndicator;
    private TabView mSelectedTab;
    private int mTabMargin;
    private int mIndicatorWidth;
    private int mIndicatorGravity;
    private float mIndicatorCorners;
    private int mTabMode;
    private int mTabHeight;

    public static int TAB_MODE_FIXED = 10;
    public static int TAB_MODE_SCROLLABLE = 11;

    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    protected TabAdapter mTabAdapter;

    private OnTabSelectedListener mTabSelectedListener;
    private OnTabPageChangeListener mTabPageChangeListener;
    private DataSetObserver mPagerAdapterObserver;

    public VerticalTabLayout(Context context) {
        this(context, null);
    }

    public VerticalTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setFillViewport(true);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalTabLayout);
        mColorIndicator = typedArray.getColor(R.styleable.VerticalTabLayout_uxsdk_indicator_color,
                context.getResources().getColor(R.color.uxsdk_blue_highlight));
        mIndicatorWidth = (int) typedArray.getDimension(R.styleable.VerticalTabLayout_uxsdk_indicator_width, dp2px(3));
        mIndicatorCorners = typedArray.getDimension(R.styleable.VerticalTabLayout_uxsdk_indicator_corners, 0);
        mIndicatorGravity = typedArray.getInteger(R.styleable.VerticalTabLayout_uxsdk_indicator_gravity, Gravity.LEFT);
        mTabMargin = (int) typedArray.getDimension(R.styleable.VerticalTabLayout_uxsdk_tab_margin, 0);
        mTabMode = typedArray.getInteger(R.styleable.VerticalTabLayout_uxsdk_tab_mode, TAB_MODE_FIXED);
        mTabHeight = (int) typedArray.getDimension(R.styleable.VerticalTabLayout_uxsdk_tab_height, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            removeAllViews();
        }
        initTabStrip();
    }

    private void initTabStrip() {
        mTabStrip = new TabStrip(mContext);
        addView(mTabStrip, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void removeAllTabs() {
        // fix Attempt to invoke virtual method 'void android.view.View.unFocus(android.view.View)' on a null object reference
        mTabStrip.removeAllViews();
        mSelectedTab = null;
    }

    public TabView getTabAt(int position) {
        return (TabView) mTabStrip.getChildAt(position);
    }

    private void addTabWithMode(TabView tabView) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        initTabWithMode(params);
        mTabStrip.addView(tabView, params);
        if (mTabStrip.indexOfChild(tabView) == 0) {
            tabView.setChecked(true);
            params = (LinearLayout.LayoutParams) tabView.getLayoutParams();
            if (mTabMode == TAB_MODE_SCROLLABLE) {
                params.height = mTabHeight;
            }
            params.setMargins(0, 0, 0, 0);
            tabView.setLayoutParams(params);
            mSelectedTab = tabView;
        }
    }

    private void initTabWithMode(LinearLayout.LayoutParams params) {
        if (mTabMode == TAB_MODE_FIXED) {
            params.height = 0;
            params.weight = 1.0f;
            params.setMargins(0, 0, 0, 0);
        } else if (mTabMode == TAB_MODE_SCROLLABLE) {
            params.height = mTabHeight;
            params.weight = 0f;
            params.setMargins(0, mTabMargin, 0, 0);
        }
    }

    private void scrollTab(final TabView tabView) {
        tabView.post(() -> {
            int y = getScrollY();
            int tabTop = tabView.getTop() + tabView.getHeight() / 2 - y;
            int target = getHeight() / 2;
            smoothScrollBy(0, tabTop - target);
        });
    }

    public void addTab(TabView tabView) {
        if (tabView != null) {
            addTabWithMode(tabView);
            tabView.setOnClickListener(view -> {
                int position = mTabStrip.indexOfChild(view);
                if (position >= 0 && position < mTabStrip.getChildCount()) {
                    setTabSelected(position);
                }
            });
        } else {
            throw new IllegalStateException("tabview can't be null");
        }
    }

    public void setTabSelected(int position) {
        TabView view = getTabAt(position);
        if (view != mSelectedTab) {
            mSelectedTab.setChecked(false);
            view.setChecked(true);
            if (mViewPager == null) {
                mTabStrip.moveIndicator(position);
            }
            mSelectedTab = view;
            notifyTabSelected(view, position);
            scrollTab(mSelectedTab);
        }
    }

    private void notifyTabSelected(TabView view, int position) {
        if (mTabSelectedListener != null) {
            mTabSelectedListener.onTabSelected(view, position);
        }
    }

    public void setTabBadge(int position, int num) {
        getTabAt(position).setBadge(num);
    }

    public void setTabMode(int mode) {
        if (mode != TAB_MODE_FIXED && mode != TAB_MODE_SCROLLABLE) {
            throw new IllegalStateException("only support TAB_MODE_FIXED or TAB_MODE_SCROLLABLE");
        }
        if (mode == mTabMode) {
            return;
        }
        mTabMode = mode;
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View view = mTabStrip.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            initTabWithMode(params);
            if (i == 0) {
                params.setMargins(0, 0, 0, 0);
            }
            view.setLayoutParams(params);
        }
        mTabStrip.invalidate();
        mTabStrip.post(() -> mTabStrip.updataIndicatorMargin());
    }

    /**
     * only in TAB_MODE_SCROLLABLE mode will be supported
     *
     * @param margin margin
     */
    public void setTabMargin(int margin) {
        if (margin == mTabMargin) {
            return;
        }
        mTabMargin = margin;
        if (mTabMode == TAB_MODE_FIXED) {
            return;
        }
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View view = mTabStrip.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.setMargins(0, i == 0 ? 0 : mTabMargin, 0, 0);
            view.setLayoutParams(params);
        }
        mTabStrip.invalidate();
        mTabStrip.post(() -> mTabStrip.updataIndicatorMargin());
    }

    /**
     * only in TAB_MODE_SCROLLABLE mode will be supported
     *
     * @param height height
     */
    public void setTabHeight(int height) {
        if (height == mTabHeight) {
            return;
        }
        mTabHeight = height;
        if (mTabMode == TAB_MODE_FIXED) {
            return;
        }
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View view = mTabStrip.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.height = mTabHeight;
            view.setLayoutParams(params);
        }
        mTabStrip.invalidate();
        mTabStrip.post(() -> mTabStrip.updataIndicatorMargin());
    }

    public void setIndicatorColor(int color) {
        mColorIndicator = color;
        mTabStrip.invalidate();
    }

    public void setIndicatorWidth(int width) {
        mIndicatorWidth = width;
        mTabStrip.setIndicatorGravity();
    }

    public void setIndicatorCorners(int corners) {
        mIndicatorCorners = corners;
        mTabStrip.invalidate();
    }

    /**
     * @param gravity only support Gravity.LEFT,Gravity.RIGHT,Gravity.FILL
     */
    public void setIndicatorGravity(int gravity) {
        if (gravity == Gravity.LEFT || gravity == Gravity.RIGHT || Gravity.FILL == gravity) {
            mIndicatorGravity = gravity;
            mTabStrip.setIndicatorGravity();
        } else {
            throw new IllegalStateException("only support Gravity.LEFT,Gravity.RIGHT,Gravity.FILL");
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mTabSelectedListener = listener;
    }

    public void setTabAdapter(TabAdapter adapter) {
        removeAllTabs();
        if (adapter != null) {
            mTabAdapter = adapter;
            for (int i = 0; i < adapter.getCount(); i++) {
                addTab(new QTabView(mContext).setIcon(adapter.getIcon(i))
                        .setTitle(adapter.getTitle(i)).setBadge(adapter.getBadge(i))
                        .setBackground(adapter.getBackground(i)));
            }
        } else {
            removeAllTabs();
        }
    }

    public void setupWithViewPager(@Nullable final ViewPager viewPager) {
        if (mViewPager != null && mTabPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mTabPageChangeListener);
        }

        if (viewPager != null) {
            final PagerAdapter adapter = viewPager.getAdapter();
            if (adapter == null) {
                throw new IllegalArgumentException("ViewPager does not have a PagerAdapter set");
            }

            mViewPager = viewPager;

            if (mTabPageChangeListener == null) {
                mTabPageChangeListener = new OnTabPageChangeListener();
            }
            viewPager.addOnPageChangeListener(mTabPageChangeListener);

            setOnTabSelectedListener((tab, position) -> viewPager.setCurrentItem(position, false));

            setPagerAdapter(adapter, true);
        } else {
            mViewPager = null;
            setOnTabSelectedListener(null);
            setPagerAdapter(null, true);
        }
    }

    private void setPagerAdapter(@Nullable final PagerAdapter adapter, final boolean addObserver) {
        if (mPagerAdapter != null && mPagerAdapterObserver != null) {
            mPagerAdapter.unregisterDataSetObserver(mPagerAdapterObserver);
        }

        mPagerAdapter = adapter;

        if (addObserver && adapter != null) {
            if (mPagerAdapterObserver == null) {
                mPagerAdapterObserver = new PagerAdapterObserver();
            }
            adapter.registerDataSetObserver(mPagerAdapterObserver);
        }

        populateFromPagerAdapter();
    }

    private void populateFromPagerAdapter() {
        removeAllTabs();
        if (mPagerAdapter != null) {
            final int adapterCount = mPagerAdapter.getCount();
            for (int i = 0; i < adapterCount; i++) {
                addTab(i);
            }
            // Make sure we reflect the currently set ViewPager item
            if (mViewPager != null && adapterCount > 0) {
                final int curItem = mViewPager.getCurrentItem();
                if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
                    setTabSelected(curItem);
                }
            }
            return;
        }
        removeAllTabs();

    }

    private void addTab(int i) {
        if (mPagerAdapter instanceof TabAdapter) {
            mTabAdapter = (TabAdapter) mPagerAdapter;
            addTab(new QTabView(mContext).setIcon(mTabAdapter.getIcon(i))
                    .setTitle(mTabAdapter.getTitle(i)).setBadge(mTabAdapter.getBadge(i))
                    .setBackground(mTabAdapter.getBackground(i)));
        } else {
            String title = mPagerAdapter.getPageTitle(i) == null ? "tab" + i : mPagerAdapter.getPageTitle(i).toString();
            addTab(new QTabView(mContext).setTitle(
                    new QTabView.TabTitle.Builder(mContext).setContent(title).build()));
        }
    }

    private int getTabCount() {
        return mTabStrip.getChildCount();
    }

    private int getSelectedTabPosition() {
        //        if (mViewPager != null) return mViewPager.getCurrentItem();
        int index = mTabStrip.indexOfChild(mSelectedTab);
        return index == -1 ? 0 : index;
    }


    private class TabStrip extends LinearLayout {
        private float mIndicatorY;
        private int mIndicatorX;
        private int mLastWidth;
        private int mIndicatorHeight;
        private Paint mIndicatorPaint;

        public TabStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            setOrientation(LinearLayout.VERTICAL);
            mIndicatorPaint = new Paint();
            mIndicatorGravity = mIndicatorGravity == 0 ? Gravity.LEFT : mIndicatorGravity;
            setIndicatorGravity();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (getChildCount() > 0) {
                View childView = getChildAt(0);
                mIndicatorHeight = childView.getMeasuredHeight();
            }
        }

        public void updataIndicatorMargin() {
            int index = getSelectedTabPosition();
            mIndicatorY = calcIndicatorY(index);
            invalidate();
        }


        public void setIndicatorGravity() {
            if (mIndicatorGravity == Gravity.LEFT) {
                mIndicatorX = 0;
                if (mLastWidth != 0) {
                    mIndicatorWidth = mLastWidth;
                }
                setPadding(mIndicatorWidth, 0, 0, 0);
            } else if (mIndicatorGravity == Gravity.RIGHT) {
                if (mLastWidth != 0) {
                    mIndicatorWidth = mLastWidth;
                }
                setPadding(0, 0, mIndicatorWidth, 0);
            } else if (mIndicatorGravity == Gravity.FILL) {
                mIndicatorX = 0;
                setPadding(0, 0, 0, 0);
            }
            post(() -> {
                if (mIndicatorGravity == Gravity.RIGHT) {
                    mIndicatorX = getWidth() - mIndicatorWidth;
                } else if (mIndicatorGravity == Gravity.FILL) {
                    mLastWidth = mIndicatorWidth;
                    mIndicatorWidth = getWidth();
                }
                invalidate();
            });
        }

        private float calcIndicatorY(float offset) {
            if (mTabMode == TAB_MODE_FIXED) {
                return offset * mIndicatorHeight;
            }
            return offset * (mIndicatorHeight + mTabMargin);
        }

        public void moveIndicator(float offset) {
            mIndicatorY = calcIndicatorY(offset);
            invalidate();
        }

        public void moveIndicator(final int index) {
            final float target = calcIndicatorY(index);
            if (mIndicatorY == target) {
                return;
            }
            post(() -> {
                ValueAnimator anime = ValueAnimator.ofFloat(mIndicatorY, target);
                anime.addUpdateListener(animation -> {
                    float value = Float.parseFloat(animation.getAnimatedValue().toString());
                    mIndicatorY = value;
                    invalidate();
                });
                anime.setDuration(200).start();
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mIndicatorPaint.setColor(mColorIndicator);
            RectF r = new RectF(mIndicatorX * 1f, mIndicatorY * 1f,
                    mIndicatorX + mIndicatorWidth * 1f, mIndicatorY + mIndicatorHeight * 1f);
            if (mIndicatorCorners != 0) {
                canvas.drawRoundRect(r, mIndicatorCorners, mIndicatorCorners, mIndicatorPaint);
            } else {
                canvas.drawRect(r, mIndicatorPaint);
            }
        }

    }

    protected int dp2px(float dp) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private class OnTabPageChangeListener implements ViewPager.OnPageChangeListener {

        public OnTabPageChangeListener() {
            //do nothing
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            //do nothing
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            mTabStrip.moveIndicator(positionOffset + position);
        }

        @Override
        public void onPageSelected(int position) {
            if (position != getSelectedTabPosition()) {
                setTabSelected(position);
            }
        }
    }

    private class PagerAdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            populateFromPagerAdapter();
        }

        @Override
        public void onInvalidated() {
            populateFromPagerAdapter();
        }
    }

    public interface OnTabSelectedListener {

        void onTabSelected(TabView tab, int position);

    }
}
