/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.util;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;

/**
 * Utility class for converting and applying changes to Views.
 */
public final class ViewUtil {

    private ViewUtil() {
        // Util class
    }

    /**
     * Applies a tint to the ImageView.
     *
     * @param imageView The ImageView to apply the tint to.
     * @param color     The tinting color.
     */
    public static void tintImage(@NonNull ImageView imageView, @ColorInt int color) {
        if (imageView.getDrawable() == null) {
            return;
        }
        Drawable wrapDrawable = DrawableCompat.wrap(imageView.getDrawable().mutate());
        DrawableCompat.setTint(wrapDrawable, color);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            imageView.invalidate(); //Fixes invalidation bug on lollipop devices
        }
    }

    /**
     * Applies tint to ImageView
     *
     * @param imageView      The ImageView to apply tint to
     * @param colorStateList The ColorStateList representing the tint
     */
    public static void tintImage(@NonNull ImageView imageView, @NonNull ColorStateList colorStateList) {
        ImageViewCompat.setImageTintList(imageView, colorStateList);
    }

    /**
     * Converts a VectorDrawable to a Bitmap.
     *
     * @param drawable The VectorDrawable to convert.
     * @return A {@link Bitmap} object.
     */
    @NonNull
    public static Bitmap getBitmapFromVectorDrawable(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
//
//    /**
//     * Shows a toast, or logs an error if a BadTokenException is thrown.
//     *
//     * @param context  An android context object
//     * @param resId    The resource ID of the string to display
//     * @param duration How long to display the toast.
//     */
//    public static void showToast(Context context, int resId, int duration) {
//        showToast(context, context.getString(resId), duration);
//    }
//
//    /**
//     * Shows a toast, or logs an error if a BadTokenException is thrown.
//     *
//     * @param context  An android context object
//     * @param message  The string to display
//     * @param duration How long to display the toast.
//     */
//    public static void showToast(Context context, String message, int duration) {
//        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
//            ToastCompat.makeText(context, message, duration)
//                    .setBadTokenListener(toast -> DJILog.e("failed toast", message)).show();
//        } else {
//            Toast.makeText(context, message, duration).show();
//        }
//    }

}
