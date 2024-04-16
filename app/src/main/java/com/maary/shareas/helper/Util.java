package com.maary.shareas.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.WindowMetrics;

import com.maary.shareas.activity.MainActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static Bitmap getBitmap(Intent intent, Context context) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            return BitmapFactory.decodeStream(inputStream);
        } else return null;
    }

    public static Point getDeviceBounds(Context context) {
        PreferencesHelper preferencesHelper = new PreferencesHelper(context);
        int device_height, device_width;

        device_height = preferencesHelper.getHeight();
        if (device_height == -1){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = ((Activity)context).getWindowManager().getMaximumWindowMetrics();
                device_height = windowMetrics.getBounds().height();
                device_width = windowMetrics.getBounds().width();
            }else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                device_height = displayMetrics.heightPixels;
                device_width = displayMetrics.widthPixels;
            }
            preferencesHelper.setWidthAndHeight(device_width, device_height);
        } else {
            device_width = preferencesHelper.getWidth();
        }
        return new Point(device_width, device_height);

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
