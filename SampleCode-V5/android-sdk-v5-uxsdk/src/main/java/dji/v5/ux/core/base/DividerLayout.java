package dji.v5.ux.core.base;
/*
 *   WWWWWW||WWWWWW
 *    W W W||W W W
 *         ||
 *       ( OO )__________
 *        /  |           \
 *       /o o|    DJI     \
 *       \___/||_||__||_|| **
 *            || ||  || ||
 *           _||_|| _||_||
 *          (__|__|(__|__|
 *
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

public interface DividerLayout {

    /**
     * 设置顶部分割线是否可见
     * @param enabled
     */
    void setTopDividerEnable(boolean enabled);

    /**
     * 设置底部分割线是否可见
     * @param enabled
     */
    void setBottomDividerEnable(boolean enabled);

    /**
     * 设置顶部分割线颜色
     * @param color
     */
    void setTopDividerColor(int color);

    /**
     * 设置底部分割线颜色
     * @param color
     */
    void setBottomDividerColor(int color);

    /**
     * 设置顶部分割线高度
     * @param height
     */
    void setTopDividerHeight(int height);

    /**
     * 设置底部分线高度
     * @param height
     */
    void setBottomDividerHeight(int height);

    /**
     * 设置顶部分割线距离左侧的距离
     * @param marginLeft
     */
    void setTopMarginLeft(int marginLeft);

    /**
     * 设置底部分割线距离左侧的距离
     * @param marginLeft
     */
    void setBottomMarginLeft(int marginLeft);
}
