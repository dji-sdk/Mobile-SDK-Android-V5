package dji.v5.ux.core.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import dji.v5.utils.common.LogUtils;

/**
 * 高程误差处理类
 */
public class GeoidManager {
    private static final String TAG = GeoidManager.class.getSimpleName();
    private RandomAccessFile mRandomAccessFile;
    private MappedByteBuffer mMappedByteBuffer;
    private final byte[] mBuffer = new byte[2];
    private static final int MINIMUM_SIZE = 5;

    private GeoidManager(){}

    private static class Holder {
        private static final GeoidManager INSTANCE = new GeoidManager();
    }

    public static GeoidManager getInstance() {
        return Holder.INSTANCE;
    }

    private void closeGeoid() throws IOException {
        if (mRandomAccessFile != null) {
            mRandomAccessFile.close();
        }
        mRandomAccessFile = null;
        if (mMappedByteBuffer != null) {
            mMappedByteBuffer.clear();
        }
    }

    /**
     * description: 读取高程误差二进制文件
     *
     * @param path
     */
    public void openGeoid96M150(String path) {
        try {
            closeGeoid();
            mRandomAccessFile = new RandomAccessFile(new File(path), "r");
            FileChannel fileChannel = mRandomAccessFile.getChannel();
            mMappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        } catch (Exception e) {
            LogUtils.d(TAG, "openGeoid96M150 error: " + e.getMessage());
        }
    }

    private short fGet2b(int offset) {
        mMappedByteBuffer.position(offset);
        mMappedByteBuffer.get(mBuffer);
        return (short) ((toUnsignedInt(mBuffer[0]) << 8) + toUnsignedInt(mBuffer[1]));
    }

    private double interpb(double[] y, double a, double b) {
        if (y == null || y.length != 4) {
            return 2000.0;
        }
        return y[0] * (1.0 - a) * (1.0 - b) + y[1] * a * (1.0 - b) + y[2] * (1.0 - a) * b + y[3] * a * b;
    }

    /**
     * @param lat 经度
     * @param lon 纬度
     * @return 高程误差
     */
    public double geoidhEgm96(double lat, double lon) {
        try {
            final double lon0 = 0.0, lat0 = 90.0, dlon = 15.0 / 60.0, dlat = -15.0 / 60.0;
            final int nlon = 1440, nlat = 721;
            double a, b, geoidH;
            int i1, i2, j1, j2;
            double[] y = new double[4];

            if (mRandomAccessFile == null || mMappedByteBuffer == null) {
                return 0.0;
            }

            a = (lon - lon0) / dlon;
            b = (lat - lat0) / dlat;
            i1 = (int)a;
            a -= i1;
            i2 = i1 < nlon - 1 ? i1 + 1 : 0;
            j1 = (int)b;
            b -= j1;
            j2 = j1 < nlat - 1 ? j1 + 1 : j1;
            y[0] = fGet2b(2 * (i1 + j1 * nlon)) * 0.01;
            y[1] = fGet2b(2 * (i2 + j1 * nlon)) * 0.01;
            y[2] = fGet2b(2 * (i1 + j2 * nlon)) * 0.01;
            y[3] = fGet2b(2 * (i2 + j2 * nlon)) * 0.01;

            geoidH = interpb(y, a, b);
            if (Math.abs(geoidH) > 200.0) {
                geoidH = 0.0;
            }
            return geoidH;
        } catch (Exception e) {
            LogUtils.d(TAG, "geoidhEgm96 error: " + e.getMessage());
        }
        return 0;
    }

    private int toUnsignedInt(byte b) {
        return ((int) b) & 0xff;
    }

    public int getMinimumSize() {
        return MINIMUM_SIZE;
    }
}
