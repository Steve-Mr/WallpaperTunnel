package com.maary.shareas.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.WindowMetrics;

import java.io.IOException;

public class Util {

    public static Bitmap getBitmap(Intent intent, Context context) throws IOException {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
        } else return null;
    }

    public static Point getDeviceBounds(Context context) {
        WindowMetrics windowMetrics;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowMetrics = ((Activity)context).getWindowManager().getMaximumWindowMetrics();
            return new Point(windowMetrics.getBounds().width(), windowMetrics.getBounds().height());
        }else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            return new Point(width, height);
        }
    }

    public static Boolean isVertical(int dheight, int dwidth, Bitmap bitmap) {
        int bitmap_full_width = bitmap.getWidth();
        int bitmap_full_height = bitmap.getHeight();

        double device_scale = (double) dheight / dwidth;
        double bitmap_scale = (double) bitmap_full_height / bitmap_full_width;

        return device_scale < bitmap_scale;
    }

    public static Bitmap adjustBrightness(Bitmap mBitmap, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        1, 0, 0, 0, brightness,
                        0, 1f, 0, 0, brightness,
                        0, 0, 1f, 0, brightness,
                        0, 0, 0, 1f, 0
                });
        Bitmap mEnhancedBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap
                .getConfig());
        Canvas canvas = new Canvas(mEnhancedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(mBitmap, 0, 0, paint);
        return mEnhancedBitmap;
    }
}
