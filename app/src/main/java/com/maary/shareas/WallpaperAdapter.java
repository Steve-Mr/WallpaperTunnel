package com.maary.shareas;

import android.content.Context;
import android.media.Image;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

public class WallpaperAdapter extends ArrayAdapter<Image> {
    public WallpaperAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }
}
