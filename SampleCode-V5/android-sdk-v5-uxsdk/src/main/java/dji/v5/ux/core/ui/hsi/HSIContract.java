package dji.v5.ux.core.ui.hsi;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public interface HSIContract {

    public interface HSIContainer {
        int getWidth();

        int getHeight();

        float getCurrentDegree();

        int getAircraftSize();

        int getVisibleDistanceInHsiInMeters();

        float getCalibrationAreaWidth();

        float getDegreeIndicatorHeight();

        float getCompassBitmapOffset();

        //void addDJIKey(DJIKey key);

        void updateWidget();

        View getView();
    }

    public interface HSILayer {
        void onStart();

        void onStop();

        void draw(Canvas canvas, Paint paint, int compassSize);

        /**
         * 飞机当前模式
         * @param fpv true: fpv ，false: liveview
         */
        default void enterFpvMode(boolean fpv){}

        //boolean transformValue(DJIKey key, Object value);
    }

}
