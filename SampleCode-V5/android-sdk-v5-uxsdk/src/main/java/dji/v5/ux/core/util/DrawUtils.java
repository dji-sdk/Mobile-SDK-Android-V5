package dji.v5.ux.core.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.LruCache;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import dji.v5.utils.common.ContextUtil;


public class DrawUtils {

    /**
     * VectorDrawable比较特殊, VMRuntime.RegisterNativeAllocation, 会触发FinalizerReference.awaitFinialization, 需要尽量避免VectorDrawable的频繁创建
     * VerctorDrawale大小不好计算，就缓存30个左右
     */
    private static final LruCache<Integer, VectorDrawable> vectorDrawableLruCache = new LruCache<>(30);
    private static final LruCache<Integer, Bitmap> bitmapLruCache = new BitmapLruCache(1024 * 1024);

    private DrawUtils() {
    }

    public static synchronized Bitmap drawableRes2Bitmap(@DrawableRes int resId) {
        Bitmap bitmap = bitmapLruCache.get(resId);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = drawable2Bitmap(getDrawable(resId));
            bitmapLruCache.put(resId, bitmap);
        }
        return bitmap;
    }

    public static synchronized Drawable getDrawable(int resId) {
        Drawable drawable = vectorDrawableLruCache.get(resId);
        if (null == drawable) {
            drawable = ContextUtil.getContext().getResources().getDrawable(resId);
            if (drawable instanceof VectorDrawable) {
                vectorDrawableLruCache.put(resId, (VectorDrawable) drawable);
            }
        }
        return drawable;
    }

    public static synchronized Bitmap drawable2Bitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int width = drawable.getMinimumWidth();
        int height = drawable.getMinimumHeight();
        // 取 drawable 的颜色格式
        Bitmap.Config config =
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }


    private static class BitmapLruCache extends LruCache<Integer, Bitmap> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public BitmapLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(Integer key, Bitmap value) {
            return value.getByteCount();
        }
    }
}
