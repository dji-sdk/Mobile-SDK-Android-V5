package dji.v5.ux.mapkit.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 5/31/17.
 * 线段或多边形的基类，包含了点和透明度
 */
public class BasePointCollection {
    protected List<DJILatLng> points;
    private float alpha = 1.0f;

    protected BasePointCollection() {
        points = new ArrayList<>();
    }

    public List<DJILatLng> getPoints() {
        return new ArrayList<>(points);
    }

    public void setPoints(List<DJILatLng> points) {
        this.points = new ArrayList<>(points);
    }

    public void addPoint(DJILatLng point) {
        points.add(point);
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}
