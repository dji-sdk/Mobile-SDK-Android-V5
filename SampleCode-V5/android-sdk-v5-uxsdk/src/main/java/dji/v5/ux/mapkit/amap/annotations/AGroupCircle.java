package dji.v5.ux.mapkit.amap.annotations;

import com.amap.api.maps.model.Circle;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircle;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircleOptions;

import java.util.List;

/**
 * Created by dickensdai 10/9/18
 * 高德地图的groupCircle代理类
 */
public class AGroupCircle implements DJIGroupCircle {


    /**
     * 用一个list来保存所有添加的circle
     */
    private List<Circle> circles;

    public AGroupCircle(List<Circle> circles) {
        this.circles = circles;
    }
    @Override
    public void remove() {
        for (Circle circle : circles) {
            circle.remove();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        for (Circle circle : circles) {
            circle.setVisible(visible);
        }
    }

    @Override
    public boolean isVisible() {
        return circles.get(0).isVisible();
    }

    @Override
    public void setZIndex(float zIndex) {
        for (Circle circle : circles) {
            circle.setZIndex(zIndex);
        }
    }

    @Override
    public float getZIndex() {
        return circles.get(0).getZIndex();
    }

    @Override
    public void setFillColor(int color) {
        for (Circle circle : circles) {
            circle.remove();
        }
    }

    @Override
    public void setStrokeColor(int color) {
        for (Circle circle : circles) {
            circle.setStrokeColor(color);
        }
    }

    @Override
    public void setCircles(List<DJILatLng> centers, List<Double> radius) {
        return;
    }

    @Override
    public DJIGroupCircleOptions getOptions() {
        return null;
    }
}
