package com.maary.shareas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);

        Bitmap bitmap;
        int device_height = getDeviceBounds().y;
        int device_width = getDeviceBounds().x;

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        try{
            if (Intent.ACTION_SEND.equals(action) && type != null){
                if (type.startsWith("image/")){
                    bitmap = getBitmap(intent);

                    Boolean isVertical = isVertical(device_height, device_width, bitmap);
                    int bitmap_full_width = bitmap.getWidth();
                    int bitmap_full_height = bitmap.getHeight();

                    final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    //Horizontal Scroll View
                    HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
                    LinearLayout.LayoutParams hLayoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);
                    horizontalScrollView.setLayoutParams(hLayoutParams);
                    horizontalScrollView.setFillViewport(true);

                    //Vertical Scroll View
                    ScrollView verticalScrollView = new ScrollView(this);
                    LinearLayout.LayoutParams vLayoutParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    verticalScrollView.setLayoutParams(vLayoutParams);
                    verticalScrollView.setFillViewport(true);

                    //view container
                    RelativeLayout relativeLayout = new RelativeLayout(this);
                    LinearLayout.LayoutParams relativeParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    relativeLayout.setLayoutParams(relativeParams);

                    //Show Image
                    ImageView imageView = new ImageView(this);

                    LinearLayout.LayoutParams hImageLayoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);

                    ViewGroup.LayoutParams vImageLayoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);

                    if (isVertical){
                        imageView.setLayoutParams(vLayoutParams);
                    }else {
                        imageView.setLayoutParams(hLayoutParams);
                    }

                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    imageView.setAdjustViewBounds(true);
                    imageView.setId(View.generateViewId());

                    imageView.setImageBitmap(bitmap);

                    //Set Button
                    Button button = new Button(this);
                    RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    button.setText("SET");
                    button.setId(View.generateViewId());
                    button.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            //button.(16,16,16, insets.getSystemWindowInsetBottom());
                            buttonParams.setMargins(16,16,16, insets.getSystemWindowInsetBottom());
                            return insets.consumeSystemWindowInsets();
                        }
                    });
                    Bitmap finalBitmap = bitmap;
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Rect cord= new Rect(0,0,0,0);
                            if(isVertical){
                                float scale = (float) bitmap_full_height / verticalScrollView.getChildAt(0).getHeight();

                                int start = (int)(scale * verticalScrollView.getScrollY());

                                int result_width = (int) (scale * device_width);
                                int result_height = (int) (scale * device_height);
                                result_width = Math.min(result_width, bitmap_full_width);
                                result_height = Math.min(result_height, bitmap_full_height);

                                cord = new Rect(0, start, result_width, start + result_height);
                            }else{
                                float scale = (float) bitmap_full_width / horizontalScrollView.getChildAt(0).getWidth();

                                int start = (int) (scale * horizontalScrollView.getScrollX());
                                int result_width = (int) (scale * device_width);
                                int result_height = (int) (scale * device_height);
                                result_width = Math.min(result_width, bitmap_full_width);
                                result_height = Math.min(result_height, bitmap_full_height);

                                cord = new Rect(start,0,start+result_width,result_height);
                            }


                            try {
                                wallpaperManager.setBitmap(finalBitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    ConstraintLayout container = findViewById(R.id.container);

                    container.addView(relativeLayout);
                    if (isVertical){
                        relativeLayout.addView(verticalScrollView);
                        verticalScrollView.addView(imageView);
                    }else{
                        relativeLayout.addView(horizontalScrollView);
                        horizontalScrollView.addView(imageView);
                    }
                    relativeLayout.addView(button, buttonParams);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    Bitmap getBitmap(Intent intent) throws IOException {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        }
        else return null;
    }

    Point getDeviceBounds(){
        Point point = new Point();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getMaximumWindowMetrics();
            point = new Point(windowMetrics.getBounds().width(), windowMetrics.getBounds().height());
        }else{
            Display display = getWindowManager().getDefaultDisplay();
            display.getRealSize(point);
        }
        return point;
    }

    Boolean isVertical(int dheight, int dwidth, Bitmap bitmap){
        int bitmap_full_width = bitmap.getWidth();
        int bitmap_full_height = bitmap.getHeight();

        double device_scale = (double) dheight / dwidth;
        double bitmap_scale = (double) bitmap_full_height / bitmap_full_width;

        return device_scale < bitmap_scale;
    }

    public static class ShareReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }
}
