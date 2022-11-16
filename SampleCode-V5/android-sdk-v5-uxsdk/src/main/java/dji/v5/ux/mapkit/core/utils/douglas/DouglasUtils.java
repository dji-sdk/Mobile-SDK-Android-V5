package dji.v5.ux.mapkit.core.utils.douglas;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by joeyang on 10/22/17.
 * 道格拉斯抽稀算法工具类
 * <a href="http://www.jianshu.com/p/4fd67921b743">地图轨迹抽稀</a>
 */

public class DouglasUtils {

    private static final int DEFAULT_THRESHOL = 50; // 50米阈值


    private DouglasUtils(){}
    /**
     * 将点集合 points 进行抽稀
     * @param points 抽稀前的点集合
     * @param threshold 抽稀阈值
     * @return 抽稀后的点集合
     */
    public static List<DJILatLng> compress(List<DJILatLng> points, double threshold) {
        int start = 0;
        int end = points.size() - 1;
        int size = points.size();
        List<LatLngPoint> originPoints = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            originPoints.add(new LatLngPoint(i, points.get(i)));
        }

        List<LatLngPoint> lineFilter = new ArrayList<>();
        // 压缩经纬度点
        List<LatLngPoint> latLngPoints = compressLine(originPoints.toArray(new LatLngPoint[size]), lineFilter, start, end, threshold);
        latLngPoints.add(originPoints.get(0));
        latLngPoints.add(originPoints.get(size - 1));
        // 对抽稀之后的点进行排序
        Collections.sort(latLngPoints, (o1, o2) -> o1.compareTo(o2));

        List<DJILatLng> latLngs = new ArrayList<>();
        for (LatLngPoint point : latLngPoints) {
            latLngs.add(point.latLng);
        }
        return latLngs;
    }

    /**
     * 将点集合 points 进行抽稀，默认抽稀阈值为50米
     * @param points 抽稀前的点集合
     * @return 抽稀后的点集合
     */
    public static List<DJILatLng> compress(List<DJILatLng> points) {
        return compress(points, DEFAULT_THRESHOL);
    }

    private static List<LatLngPoint> compressLine(LatLngPoint[] originalLatLngs, List<LatLngPoint> finalLatLngs, int start, int end, double dmax) {
        if (start < end) {
            // 递归进行调用筛选
            double maxDis = 0;
            int currentIndex = 0;
            for (int i = start + 1; i < end; i++) {
                double currentDist = distToSegment(originalLatLngs[start], originalLatLngs[end], originalLatLngs[i]);
                if (currentDist > maxDis) {
                    maxDis = currentDist;
                    currentIndex = i;
                }
            }

            // 若当前最大距离大于最大距离误差
            if (maxDis >= dmax) {
                // 将当前点j加入到过滤数组中
                finalLatLngs.add(originalLatLngs[currentIndex]);
                // 将原来的线段以当前点为中心拆成两段，分别进行递归处理
                compressLine(originalLatLngs, finalLatLngs, start, currentIndex, dmax);
                compressLine(originalLatLngs, finalLatLngs, currentIndex, end, dmax);
            }
        }
        return finalLatLngs;
    }

    /**
     * 计算以 start-end 为底的三角形的高
     * @param start
     * @param end
     * @param mid
     * @return
     */
    private static double distToSegment(LatLngPoint start, LatLngPoint end, LatLngPoint mid) {
        double a = Math.abs(DJIGpsUtils.distance(start.latLng, end.latLng));
        double b = Math.abs(DJIGpsUtils.distance(start.latLng, mid.latLng));
        double c = Math.abs(DJIGpsUtils.distance(mid.latLng, end.latLng));
        double p = (a + b + c) / 2.0D;
        double s = Math.sqrt(Math.abs(p * (p - a) * (p - b) * (p - c)));
        double d = s * 2.0 / a;
        return d;
    }
}
