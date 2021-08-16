package com.maary.shareas;

import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.hoko.blur.HokoBlur;
import com.hoko.blur.task.AsyncBlurTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static final int FLAG_FAB = 0;
    static final int FLAG_BLUR = 1;
    static final int FLAG_BRIGHTNESS = 2;
    Bitmap bitmap;
    Bitmap blur;
    Bitmap brightness;
    Rect cord;
    int blurBias = 0;
    int brightnessBias = 0;
    int FLAG = 0;
    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Point deviceBounds = Util.getDeviceBounds(MainActivity.this);
        int device_height = deviceBounds.y;
        int device_width = deviceBounds.x;

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        try {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {

                    bitmap = Util.getBitmap(intent, MainActivity.this);

                    final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    //Parent layout
                    ConstraintLayout container = findViewById(R.id.container);
                    //parent layout of bottomAppBar
                    CoordinatorLayout coordinatorLayout = findViewById(R.id.bottomAppBarContainer);
                    //progressBar usd in wallpaper setting process
                    ProgressBar progressBar = findViewById(R.id.progress_circular);

                    BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

                    FloatingActionButton fab = findViewById(R.id.fab);
                    //make image preview scrollable. parent of imageView.
                    ScrollView verticalScrollView = new ScrollView(this);
                    HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
                    //preview image
                    ImageView imageView = new ImageView(this);
                    //image ratio > device ratio?
                    Boolean isVertical = Util.isVertical(device_height, device_width, bitmap);
                    //slider bar for blur & brightness
                    Slider slider = new Slider(this);

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    //setup the fab click listener
                    //FLAG: set cropped image as wallpaper
                    //FLAG_BLUR: finish blur setting
                    //FLAG_BRIGHTNESS: finish brightness setting
                    fab.setOnClickListener(view -> {
                        switch (FLAG){
                            case FLAG_FAB:
                                if (isVertical){
                                    int start = verticalScrollView.getScrollY();
                                    cord = new Rect(0, start, device_width, start + device_height);
                                }else {
                                    int start = horizontalScrollView.getScrollX();
                                    cord = new Rect(start, 0, start + device_width, device_height);
                                }
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                break;
                            case FLAG_BLUR:
                                bitmap = blur;
                                coordinatorLayout.removeView(slider);
                                fab.setImageResource(R.drawable.ic_vertical);
                                fab.setContentDescription(getResources().getString(R.string.setAsWallpaper));
                                bottomAppBar.getMenu().getItem(0).setEnabled(true);
                                bottomAppBar.getMenu().getItem(1).setEnabled(true);
                                bottomAppBar.getMenu().getItem(2).setEnabled(true);
                                FLAG = FLAG_FAB;
                                break;
                            case FLAG_BRIGHTNESS:
                                bitmap = brightness;

                                coordinatorLayout.removeView(slider);
                                fab.setImageResource(R.drawable.ic_vertical);
                                fab.setContentDescription(getResources().getString(R.string.setAsWallpaper));
                                bottomAppBar.getMenu().getItem(0).setEnabled(true);
                                bottomAppBar.getMenu().getItem(1).setEnabled(true);
                                bottomAppBar.getMenu().getItem(2).setEnabled(true);
                                FLAG = FLAG_FAB;
                                break;
                        }
                    });

                    //set up slider
                    CoordinatorLayout.LayoutParams sliderParams = new CoordinatorLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    sliderParams.setAnchorId(R.id.fab);
                    sliderParams.anchorGravity = Gravity.START;
                    sliderParams.gravity = Gravity.START;
                    slider.setLabelBehavior(LABEL_GONE);
                    slider.setTickVisible(false);
                    slider.setOnApplyWindowInsetsListener((v, insets) -> {
                        sliderParams.setMargins(
                                insets.getSystemWindowInsetLeft(),
                                16,
                                insets.getSystemWindowInsetRight(),
                                16);
                        return insets.consumeSystemWindowInsets();
                    });

                    slider.addOnChangeListener((slider1, value, fromUser) -> {

                        if (FLAG == FLAG_BLUR){
                            HokoBlur.with(getApplicationContext())
                                    .radius((int) value)
                                    .asyncBlur(bitmap, new AsyncBlurTask.Callback() {
                                        @Override
                                        public void onBlurSuccess(Bitmap bitmap) {
                                            blur = bitmap;
                                            imageView.setImageBitmap(bitmap);
                                        }

                                        @Override
                                        public void onBlurFailed(Throwable error) {

                                        }
                                    });
                        }else if (FLAG == FLAG_BRIGHTNESS){
                            new Thread(() -> {
                                brightness = Util.adjustBrightness(bitmap, (int)value);

                                runOnUiThread(() -> imageView.setImageBitmap(brightness));
                            }).start();
                        }

                    });

                    slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                        @Override
                        public void onStartTrackingTouch(@NonNull Slider slider) {

                        }

                        @Override
                        public void onStopTrackingTouch(@NonNull Slider slider) {
                            if (FLAG == FLAG_BLUR){
                                blurBias = (int) slider.getValue();
                            }else if (FLAG == FLAG_BRIGHTNESS){
                                brightnessBias = (int) slider.getValue();
                            }
                        }
                    });

                    //set bottomAppBar menu item
                    //tap blur and brightness button will disable other menu item
                    bottomAppBar.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.blur){
                            FLAG = FLAG_BLUR;
                            fab.setImageResource(R.drawable.ic_done);
                            fab.setContentDescription(getResources().getString(R.string.finish_blur));
                            bottomAppBar.getMenu().getItem(0).setEnabled(false);
                            bottomAppBar.getMenu().getItem(1).setEnabled(false);
                            bottomAppBar.getMenu().getItem(2).setEnabled(false);
                            slider.setValueFrom(0);
                            slider.setValueTo(20);
                            slider.setStepSize(1);
                            slider.setValue(blurBias);
                            coordinatorLayout.addView(slider, sliderParams);
                        }else if (item.getItemId() == R.id.brightness){
                            FLAG = FLAG_BRIGHTNESS;
                            fab.setImageResource(R.drawable.ic_done);
                            fab.setContentDescription(getResources().getString(R.string.finish_brightness));
                            bottomAppBar.getMenu().getItem(0).setEnabled(false);
                            bottomAppBar.getMenu().getItem(1).setEnabled(false);
                            bottomAppBar.getMenu().getItem(2).setEnabled(false);
                            slider.setValueFrom(-50);
                            slider.setValueTo(50);
                            slider.setStepSize(1);
                            slider.setValue(brightnessBias);
                            coordinatorLayout.addView(slider, sliderParams);
                        }else if (item.getItemId() == R.id.horizontal){
                            cord = null;
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        return true;
                    });

                    //Show Image
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    imageView.setAdjustViewBounds(true);
                    imageView.setId(View.generateViewId());

                    //show image to imageview
                    int bitmap_full_width = bitmap.getWidth();
                    int bitmap_full_height = bitmap.getHeight();
                    int desired_width;
                    int desired_height;
                    if (isVertical){
                        desired_width = device_width;
                        float scale = (float) device_width/bitmap_full_width;
                        desired_height = (int)(scale * bitmap_full_height);

                        bitmap = Bitmap.createScaledBitmap(bitmap, desired_width, desired_height, true);

                        //Vertical Scroll View
                        LinearLayout.LayoutParams vLayoutParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        );
                        verticalScrollView.setLayoutParams(vLayoutParams);
                        verticalScrollView.setFillViewport(true);

                        ViewGroup.LayoutParams vImageLayoutParams =
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                        imageView.setLayoutParams(vLayoutParams);

                        container.addView(verticalScrollView, vImageLayoutParams);
                        verticalScrollView.addView(imageView);

                    }else{
                        desired_height = device_height;
                        float scale = (float)device_height/bitmap_full_height;
                        desired_width = (int)(scale * bitmap_full_width);

                        bitmap = Bitmap.createScaledBitmap(bitmap, desired_width, desired_height, true);

                        //Horizontal Scroll View
                        LinearLayout.LayoutParams hLayoutParams =
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT);
                        horizontalScrollView.setLayoutParams(hLayoutParams);
                        horizontalScrollView.setFillViewport(true);

                        LinearLayout.LayoutParams hImageLayoutParams =
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT);
                        imageView.setLayoutParams(hLayoutParams);

                        container.addView(horizontalScrollView, hImageLayoutParams);
                        horizontalScrollView.addView(imageView);

                    }

                    imageView.setImageBitmap(bitmap);

                    //setup AlertDialog builder
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.setAs);

                    String[] options = {getResources().getString(R.string.home), getResources().getString(R.string.lockscreen), getResources().getString(R.string.homeAndLockscreen)};
                    builder.setItems(options, (dialog, which) -> {
                        executorService.execute(() -> {
                            int FLAG;
                            switch (which) {
                                case 0: FLAG = WallpaperManager.FLAG_SYSTEM; break;
                                case 1: FLAG = WallpaperManager.FLAG_LOCK; break;
                                case 2: FLAG = WallpaperManager.FLAG_SYSTEM| WallpaperManager.FLAG_LOCK; break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + which);
                            }
                            try {
                                wallpaperManager.setBitmap(bitmap, cord, true, FLAG);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            handler.post(() -> {
                                dialog.cancel();
                                imageView.setColorFilter(Color.rgb(123, 123, 123), PorterDuff.Mode.MULTIPLY);
                                progressBar.bringToFront();

                            });
                        });
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_MAIN);
                        i.addCategory(Intent.CATEGORY_HOME);
                        MainActivity.this.startActivity(i);
                        finish();
                    });

                    //disable gesture fore slider bar
                    List<Rect> rects = new ArrayList<>();
                    int wrapSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    slider.measure(wrapSpec, wrapSpec);
                    bottomAppBar.measure(wrapSpec, wrapSpec);
                    rects.add(new Rect(0, device_height-(slider.getMeasuredHeight()+bottomAppBar.getMeasuredHeight()*2), device_width, device_height));
                    Log.e("rect",String.valueOf(device_height-(slider.getMeasuredHeight()+bottomAppBar.getMeasuredHeight())*2));


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        container.setSystemGestureExclusionRects(rects);
                    }

                    coordinatorLayout.bringToFront();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
