package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;
import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

/**
 * Created by dickensdai on 8/9/18.
 * DJIGroupCircle的属性
 */
public class DJIGroupCircleOptions {

    private List<DJILatLng> centers;
    private List<Double> radius;
    private float strokeWidth;
    private int strokeColor;
    private int fillColor;
    private float alpha;
    private float zIndex;

    /**
     * 设置圆心
     * @param centers 一组圆心
     * @return
     */
    public DJIGroupCircleOptions centers(List<DJILatLng> centers) {
        this.centers = centers;
        return this;
    }

    /**
     * 设置半径
     * @param radius 单位是米
     * @return
     */
    public DJIGroupCircleOptions radius(List<Double> radius) {
        this.radius = radius;
        return this;
    }

    /**
     * 设置层级
     * @param zIndex
     * @return
     */
    public DJIGroupCircleOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
     * 设置圆周线宽
     * @param width 宽度像素值
     * @return
     */
    public DJIGroupCircleOptions strokeWidth(float width) {
        this.strokeWidth = width;
        return this;
    }

    /**
     * 设置圆周线的颜色
     * @param color 线颜色
     * @return
     */
    public DJIGroupCircleOptions strokeColor(@ColorInt int color) {
        this.strokeColor = color;
        return this;
    }

    /**
     * 设置圆的填充颜色
     * @param color 填充颜色
     * @return
     */
    public DJIGroupCircleOptions fillColor(@ColorInt int color) {
        this.fillColor = color;
        return this;
    }

    /**
     * 设置alpha
     * @param alpha
     * @return
     */
    public DJIGroupCircleOptions alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * 获取圆心地理坐标
     * @return
     */
    public List<DJILatLng> getCenters() {
        return centers;
    }

    /**
     * 获取半径（单位米）
     * @return
     */
    public List<Double> getRadius() {
        return radius;
    }

    /**
     * 获取圆周线宽（像素）
     * @return
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * 获取圆周线颜色
     * @return
     */
    public @ColorInt int getStrokeColor() {
        return strokeColor;
    }

    /**
     * 获取圆填充颜色
     * @return
     */
    public @ColorInt int getFillColor() {
        return fillColor;
    }

    /**
     * 获取alpha
     * @return
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * 获取层级
     * @return
     */
    public float getZIndex() {
        return zIndex;
    }

}
