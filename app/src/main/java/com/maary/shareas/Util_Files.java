package com.maary.shareas;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class Util_Files {

    static String customDir = Environment.DIRECTORY_PICTURES + File.separator + "Wallpaper History";

    public static void saveWallpaper(Bitmap bitmap, Activity activity){
        Calendar calendar = Calendar.getInstance();
        String fileName = "WLP_" +
                (calendar.get(Calendar.YEAR) - 1900) +
                calendar.get(Calendar.MONTH) +
                calendar.get(Calendar.DAY_OF_MONTH) +
                calendar.get(Calendar.HOUR_OF_DAY) +
                calendar.get(Calendar.MINUTE) +
                calendar.get(Calendar.MILLISECOND);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, customDir);

        final ContentResolver contentResolver = activity.getContentResolver();
        Uri uri;

        final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        uri = contentResolver.insert(contentUri, contentValues);

        try {
            final OutputStream outputStream = contentResolver.openOutputStream(uri);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IOException("failed to save bitmap");
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Uri saveEditedWallpaper(Bitmap bitmap, Activity activity){
        Calendar calendar = Calendar.getInstance();
        String fileName = "image";
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, customDir);

        final ContentResolver contentResolver = activity.getContentResolver();
        Uri uri;

        final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        uri = contentResolver.insert(contentUri, contentValues);

        try {
            final OutputStream outputStream = contentResolver.openOutputStream(uri);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IOException("failed to save bitmap");
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uri;
    }

    public static Uri getWallpapersList(){
        ArrayList<Uri> uriArrayList = new ArrayList<>();
        File file = new File(customDir);

        Uri collection;
        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        System.out.println(collection);

        return collection;
    }
}
