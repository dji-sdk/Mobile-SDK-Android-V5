package dji.v5.ux.mapkit.core.models.annotations;

import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

/**
 * Created by dickensdai 8/9/18
 * 圆圈组群
 */
public interface DJIGroupCircle {

    /**
     * 移除所有的圆圈
     */
    void remove();

    /**
     * 设置layer的visible
     * @param visible
     */
    void setVisible(boolean visible);

    /**
     * 获取是否可见
     * @return
     */
    boolean isVisible();

    /**
     * 获取层级
     * @param zIndex
     */
    void setZIndex(float zIndex);

    /**
     * 设置层级
     * @return
     */
    float getZIndex();

    /**
     * 设置圆的填充色
     * @param color
     */
    void setFillColor(int color);

    /**
     * 设置圆边线的颜色
     * @param color
     */
    void setStrokeColor(int color);

    /**
     * 设置所有圆的中心以及半径
     * @param centers
     * @param radius
     */
    void setCircles(List<DJILatLng> centers, List<Double> radius);
    /**
     * 获取Groups的配置
     * @return
     */
    DJIGroupCircleOptions getOptions();
}
