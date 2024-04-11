package com.maary.shareas.activity;

import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.palette.graphics.Palette;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.hoko.blur.HokoBlur;
import com.hoko.blur.task.AsyncBlurTask;
import com.maary.shareas.helper.PreferencesHelper;
import com.maary.shareas.R;
import com.maary.shareas.helper.Util;
import com.maary.shareas.helper.Util_Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static final int MENU_RESET = 0;
    //请求权限
    Bitmap bitmap;
    Bitmap processed;
    Bitmap blurProcessed;
    Bitmap brightnessProcessed;
    Bitmap raw;
    Rect cord;
    int blurBias = 0;
    int brightnessBias = 0;
    MaterialAlertDialogBuilder builder;
    Boolean applyEditToLock = true;
    Boolean applyEditToHome = true;
    Boolean isProcessed = false;

    Boolean currentImageViewIsHome = true;
    int device_height, device_width;

    Palette.Swatch vibrant;
    Palette.Swatch dominant;
    Palette.Swatch muted ;

    Intent intent;

    Snackbar snackbarReturnHome;


    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        PreferencesHelper preferencesHelper = new PreferencesHelper(this);

        device_height = preferencesHelper.getHeight();
        if (device_height == -1){
            Point deviceBounds = Util.getDeviceBounds(MainActivity.this);
            device_height = deviceBounds.y;
            device_width = deviceBounds.x;

            preferencesHelper.setWidthAndHeight(device_width, device_height);
        } else {
            device_width = preferencesHelper.getWidth();
        }

        intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

        try {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    bitmap = Util.getBitmap(intent, MainActivity.this);
                }
            }

            if (bitmap != null) {
                //Parent layout
                ConstraintLayout container = findViewById(R.id.container);
                //parent layout of bottomAppBar
                CoordinatorLayout bottomAppBarContainer = findViewById(R.id.bottomAppBarContainer);
                //progressBar usd in wallpaper setting process

                BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

                FloatingActionButton fab = findViewById(R.id.fab);
                //make image preview scrollable. parent of imageView.
                ScrollView verticalScrollView = new ScrollView(this);
                HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
                //preview image
                ImageView imageView = new ImageView(this);
                //image ratio > device ratio?
                Boolean isVertical = Util.isVertical(device_height, device_width, bitmap);

                ExecutorService executorService = Executors.newSingleThreadExecutor();

                //Show Image
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setAdjustViewBounds(true);
                imageView.setId(View.generateViewId());

                //show image to imageview
                int bitmap_full_width = bitmap.getWidth();
                int bitmap_full_height = bitmap.getHeight();
                int desired_width;
                int desired_height;

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

                if (isVertical) {
                    desired_width = device_width;
                    float scale = (float) device_width / bitmap_full_width;
                    desired_height = (int) (scale * bitmap_full_height);

                    verticalScrollView.setFillViewport(true);
                    container.addView(verticalScrollView, layoutParams);
                    verticalScrollView.addView(imageView, layoutParams);
                } else {
                    desired_height = device_height;
                    float scale = (float) device_height / bitmap_full_height;
                    desired_width = (int) (scale * bitmap_full_width);

                    horizontalScrollView.setFillViewport(true);
                    container.addView(horizontalScrollView, layoutParams);
                    horizontalScrollView.addView(imageView, layoutParams);
                }

                bitmap = Bitmap.createScaledBitmap(bitmap, desired_width, desired_height, true);
                processed = bitmap;

                raw = bitmap;
                imageView.setImageBitmap(bitmap);

                imageView.setOnClickListener(v -> {
                    if (!isProcessed || applyEditToHome == applyEditToLock) {
                        if (currentImageViewIsHome) {
                            currentImageViewIsHome = false;
                            fab.setImageResource(R.drawable.ic_lockscreen);
                        } else {
                            currentImageViewIsHome = true;
                            fab.setImageResource(R.drawable.ic_vertical);
                        }
                    }
                    if (applyEditToHome) {
                        if (currentImageViewIsHome) {
                            imageView.setImageBitmap(raw);
                            currentImageViewIsHome = false;
                            fab.setImageResource(R.drawable.ic_lockscreen);
                        } else {
                            imageView.setImageBitmap(bitmap);
                            currentImageViewIsHome = true;
                            fab.setImageResource(R.drawable.ic_vertical);
                        }
                    }
                    if (applyEditToLock) {
                        if (currentImageViewIsHome) {
                            imageView.setImageBitmap(bitmap);
                            currentImageViewIsHome = false;
                            fab.setImageResource(R.drawable.ic_lockscreen);
                        } else {
                            imageView.setImageBitmap(raw);
                            currentImageViewIsHome = true;
                            fab.setImageResource(R.drawable.ic_vertical);
                        }
                    }
                });

                Palette.from(bitmap).generate(palette -> {
                    // Access the colors from the palette
                    assert palette != null;
                    vibrant = palette.getVibrantSwatch();
                    muted = palette.getMutedSwatch();
                    dominant = palette.getDominantSwatch();

                    if (vibrant != null) {
                        fab.setBackgroundTintList(ColorStateList.valueOf(vibrant.getRgb()));
                    } else {
                        fab.setBackgroundColor(palette.getVibrantColor(getColor(R.color.colorAccent)));
                    }
                    if (muted != null) {
                        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(muted.getRgb()));
                    }


                });

                //setup the fab click listener
                fab.setOnClickListener(view -> {
                    if (isVertical) {
                        int start = verticalScrollView.getScrollY();
                        cord = new Rect(0, start, device_width, start + device_height);
                    } else {
                        int start = horizontalScrollView.getScrollX();
                        cord = new Rect(start, 0, start + device_width, device_height);
                    }
                    bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                });

                fab.setOnLongClickListener(view -> {
                    cord = null;
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return false;
                });

                //TODO:ADD zoom (if possible

                WallpaperManager.OnColorsChangedListener wallpaperChangedListener = new WallpaperManager.OnColorsChangedListener() {
                    @Override
                    public void onColorsChanged(@Nullable WallpaperColors colors, int which) {
                        wallpaperManager.removeOnColorsChangedListener(this);
                        snackbarReturnHome.show();
                    }
                };

                snackbarReturnHome = Snackbar.make(container, getString(R.string.wallpaper_setted), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.gohome), v -> returnToHomeScreen());

                wallpaperManager.addOnColorsChangedListener(wallpaperChangedListener, null);

                //set bottomAppBar menu item
                //tap blur and brightness button will disable other menu item

                bottomAppBar.setOnMenuItemClickListener(item -> {

                    if (item.getItemId() == R.id.edit) {
                        bottomAppBarContainer.setVisibility(View.INVISIBLE);
                        AlertDialog dialog;

                        dialog = createSliderDialog();
                        Objects.requireNonNull(dialog.getWindow())
                                .clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        dialog.getWindow().setGravity(Gravity.BOTTOM);
                        dialog.setCancelable(false);

                        // 获取 Drawable 对象
                        Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.dialog_background);

                        // 复制 Drawable 对象，以便进行修改
                        assert drawable != null;
                        Drawable modifiedDrawable = drawable.getConstantState().newDrawable().mutate();
                        modifiedDrawable.setTint(muted.getRgb());

                        dialog.getWindow().setBackgroundDrawable(modifiedDrawable);
                        dialog.show();

                        TextView title_blur_view = dialog.findViewById(R.id.title_blur);
                        TextView title_brightness_view = dialog.findViewById(R.id.title_brightness);
                        assert title_blur_view != null;
                        title_blur_view.setTextColor(muted.getBodyTextColor());
                        assert title_brightness_view != null;
                        title_brightness_view.setTextColor(muted.getBodyTextColor());

                        Slider sliderBlur = dialog.findViewById(R.id.dialog_slider_blur);
                        assert sliderBlur != null;
                        sliderBlur.setLabelBehavior(LABEL_GONE);
                        sliderBlur.setTickVisible(false);

                        Slider sliderBrightness = dialog.findViewById(R.id.dialog_slider_brightness);
                        assert sliderBrightness != null;
                        sliderBrightness.setLabelBehavior(LABEL_GONE);
                        sliderBrightness.setTickVisible(false);

                        Chip chipLock = dialog.findViewById(R.id.chip_apply_lock);
                        Chip chipHome = dialog.findViewById(R.id.chip_apply_home);

                        assert chipLock != null;
                        assert chipHome != null;

                        if (vibrant != null) {
                            sliderBlur.setThumbTintList(ColorStateList.valueOf(vibrant.getRgb()));
                            sliderBlur.setTrackActiveTintList(ColorStateList.valueOf(vibrant.getRgb()));
                            sliderBrightness.setThumbTintList(ColorStateList.valueOf(vibrant.getRgb()));
                            sliderBrightness.setTrackActiveTintList(ColorStateList.valueOf(vibrant.getRgb()));
                        }

                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(muted.getTitleTextColor());
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(muted.getTitleTextColor());
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(muted.getTitleTextColor());

                        chipLock.setChecked(applyEditToLock);
                        chipHome.setChecked(applyEditToHome);

                        chipLock.setOnCheckedChangeListener((buttonView, isChecked) -> applyEditToLock = isChecked);

                        chipHome.setOnCheckedChangeListener((buttonView, isChecked) -> applyEditToHome = isChecked);

                        sliderBlur.setValueFrom(0);
                        sliderBlur.setValueTo(30);
                        sliderBlur.setStepSize(1);
                        sliderBlur.setValue(blurBias);

                        sliderBrightness.setValueFrom(-50);
                        sliderBrightness.setValueTo(50);
                        sliderBrightness.setStepSize(1);
                        sliderBrightness.setValue(brightnessBias);

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                            bitmap = processed;
                            isProcessed = true;
                            bottomAppBarContainer.setVisibility(View.VISIBLE);
                            bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
                            blurBias = (int) sliderBlur.getValue();
                            brightnessBias = (int) sliderBrightness.getValue();
                            if (applyEditToHome == applyEditToLock) {
                                imageView.setImageBitmap(bitmap);
                            } else if (applyEditToHome) {
                                imageView.setImageBitmap(bitmap);
                            } else if (applyEditToLock) {
                                imageView.setImageBitmap(bitmap);
                                currentImageViewIsHome = false;
                                fab.setImageResource(R.drawable.ic_lockscreen);
                                //todo: set fab icon
                            }

                            dialog.dismiss();
                        });

                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                            sliderBlur.setValue(0.0f);
                            sliderBrightness.setValue(0.0f);
                            bitmap = raw;
                            isProcessed = false;
                            applyEditToLock = applyEditToHome = true;
                            chipHome.setChecked(true);
                            chipLock.setChecked(applyEditToLock);
                            imageView.setImageBitmap(raw);
                            Log.v("WALLP", "SET RAW NEUTRAL");

                        });

                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                            bitmap = raw;
                            blurBias = 0;
                            brightnessBias = 0;
                            isProcessed = false;
                            applyEditToLock = applyEditToHome = true;
                            chipHome.setChecked(true);
                            chipLock.setChecked(applyEditToLock);
                            imageView.setImageBitmap(raw);
                            Log.v("WALLP", "SET RAW NEGATIVE");
                            bottomAppBarContainer.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        });

                        sliderBlur.addOnChangeListener((slider1, value, fromUser) -> {
                            Bitmap toProcess = bitmap;
                            if (brightnessProcessed != null) {
                                toProcess = brightnessProcessed;
                            }
                            HokoBlur.with(getApplicationContext())
                                    .radius((int) value)
                                    .sampleFactor(1.0f)
                                    .forceCopy(true)
                                    .asyncBlur(toProcess, new AsyncBlurTask.Callback() {
                                        @Override
                                        public void onBlurSuccess(Bitmap bitmap) {
                                            Log.v("WALLP", "BLURRRRRRRRRRRING");
                                            processed = bitmap;
                                            blurProcessed = bitmap;
                                            imageView.setImageBitmap(bitmap);
                                            if (value == 0.0f && sliderBrightness.getValue() == 0.0f) {
                                                imageView.setImageBitmap(bitmap);
                                                Log.v("WALLP", "SET RAW IN BLUR");
                                            }
                                        }

                                        @Override
                                        public void onBlurFailed(Throwable error) {

                                        }
                                    });

                        });
                        sliderBrightness.addOnChangeListener((slider1, value, fromUser) -> {
                            Bitmap toProcess = bitmap;
                            if (blurProcessed != null) {
                                toProcess = blurProcessed;
                            }
                            Bitmap finalToProcess = toProcess;
                            new Thread(() -> {
                                processed = Util.adjustBrightness(finalToProcess, (int) value);
                                brightnessProcessed = processed;
                                Log.v("WALLP", "BRIGHTTTTTTTTTTING");

                                runOnUiThread(() -> {
                                    imageView.setImageBitmap(processed);
                                    if (value == 0.0f && sliderBlur.getValue() == 0.0f) {
                                        imageView.setImageBitmap(bitmap);
                                        Log.v("WALLP", "SET RAW IN BRIG");
                                    }
                                });
                            }).start();

                        });
                    } else if (item.getItemId() == R.id.reset) {
                        bitmap = raw;
                        blurBias = 0;
                        brightnessBias = 0;
                        cord = null;
                        isProcessed = false;
                        applyEditToLock = applyEditToHome = true;
                        imageView.setImageBitmap(bitmap);
                        if (isVertical) {
                            verticalScrollView.post(() -> verticalScrollView.scrollTo(0, 0));
                        } else {
                            horizontalScrollView.post(() -> horizontalScrollView.scrollTo(0, 0));
                        }
                    }
                    return true;
                });

                ActionMenuItemView resetItem = bottomAppBar.findViewById(R.id.reset);
                resetItem.setOnLongClickListener(view -> {
                    Intent intent1 = new Intent(getApplicationContext(), HistoryActivity.class);
                    startActivity(intent1);
                    return true;
                });

                Context context = DynamicColors.wrapContextIfAvailable(
                        MainActivity.this
                );

                //setup AlertDialog builder
                builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(R.string.setAs);

                String[] options = {
                        getResources().getString(R.string.home),
                        getResources().getString(R.string.lockscreen),
                        getResources().getString(R.string.homeAndLockscreen),
                        getResources().getString(R.string.use_others)};

                builder.setItems(options, (dialog, which) -> executorService.execute(() -> {
                    try {
                        if (new PreferencesHelper(this).getSettingsHistory()){
                            //若已经选择保存选项，弹出「设置图片为」选项之前保存图片
                            if (checkPermission()) {
                                Bitmap currentWallpaper = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                                Util_Files.saveWallpaper(currentWallpaper, this);
                            }else {
                                Snackbar.make(container, R.string.no_permission, Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        }

                        switch (which) {
                            case 0 -> {
                                wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                            }
                            case 1 -> {
                                wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                            }
                            case 2 -> {
                                if (applyEditToLock) {
                                    wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                                } else {
                                    wallpaperManager.setBitmap(raw, cord, true, WallpaperManager.FLAG_LOCK);
                                }
                                if (applyEditToHome) {
                                    wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                                } else {
                                    wallpaperManager.setBitmap(raw, cord, true, WallpaperManager.FLAG_SYSTEM);
                                }
                            }
                            //应对定制 Rom（如 Color OS）可能存在的魔改导致 "WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM" 参数失效的情况。
                            case 3 -> {
                                shareBitmap(bitmap, getApplicationContext());
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + which);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }));

                bottomAppBarContainer.bringToFront();
            }
                } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Boolean hasStoragePermission = ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            Boolean hasExternalStorageManagerPermission = Environment.isExternalStorageManager();
            return hasStoragePermission && hasExternalStorageManagerPermission;
        } else {
            return ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void returnToHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //用于亮度/模糊的 Slider Bar 对话框
    @SuppressLint("InflateParams")
    private AlertDialog createSliderDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);//, R.style.TransparentDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.layout_dialog_adjustment, null))
                .setPositiveButton(R.string.save, null)
                .setNeutralButton(R.string.reset, null)
                .setNegativeButton(R.string.cancel, null);

        return builder.create();
    }



    private void shareBitmap(@NonNull Bitmap bitmap, Context context) {
        //---Save bitmap to external cache directory---//
        //get cache directory
        File cachePath = new File(getExternalCacheDir(), "my_images/");
        cachePath.mkdirs();

        //create png file
        File file = new File(cachePath, "Image_123.png");
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //---Share File---//
        //get file uri
        Uri myImageFileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

        Intent sendIntent = new Intent(Intent.ACTION_ATTACH_DATA);
        sendIntent.setDataAndType(myImageFileUri, "image/*");
        sendIntent.putExtra("mimeType", "image/*");
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent receiver = new Intent(context, ShareReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                receiver, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        startActivity(Intent.createChooser(
                sendIntent
                , getString(R.string.use_others)
                , pendingIntent.getIntentSender()
        ));
    }

    public static class ShareReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }
}
