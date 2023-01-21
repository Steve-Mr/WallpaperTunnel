package com.maary.shareas;

import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.hoko.blur.HokoBlur;
import com.hoko.blur.task.AsyncBlurTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static final int MENU_RESET = 0;
    static final int MENU_BLUR = 1;
    static final int MENU_BRIGHTNESS = 2;

    Bitmap bitmap;
    Bitmap processed;
    Bitmap raw;
    Rect cord;
    int blurBias = 0;
    int brightnessBias = 0;
    MaterialAlertDialogBuilder builder;

    int device_height,device_width;

    //TODO:change later
    int state = 0;

    String permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        if (sharedPreferences.contains(getString(R.string.device_height))){
            device_height = sharedPreferences.getInt(getString(R.string.device_height),
                    Util.getDeviceBounds(MainActivity.this).y);
            device_width = sharedPreferences.getInt(getString(R.string.device_width),
                    Util.getDeviceBounds(MainActivity.this).x);
        }else{
            Point deviceBounds = Util.getDeviceBounds(MainActivity.this);
            device_height = deviceBounds.y;
            device_width = deviceBounds.x;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.device_height), device_height);
            editor.putInt(getString(R.string.device_width), device_width);
            editor.apply();
        }

        if (Build.VERSION.SDK_INT > 32){
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }else{
             permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        //如果需要
//        if (sharedPreferences.getBoolean(getString(R.string.enabled_history_key), false)){


//        }

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
                    CoordinatorLayout bottomAppBarContainer = findViewById(R.id.bottomAppBarContainer);
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

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

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

                    raw = bitmap;
//                    bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(false);
                    imageView.setImageBitmap(bitmap);

                    //如果 SharedPreferences 里没有关于是否保存图像历史的偏好就询问是否保存
                    if (!sharedPreferences.contains(getString(R.string.enabled_history_key))) {
                        AlertDialog dialog_history = saveHistoryDialog();
                        dialog_history.show();
                    }

                    //setup the fab click listener
                    fab.setOnClickListener(view -> {
                        //TODO: temp comment
                        if (isVertical) {
                            int start = verticalScrollView.getScrollY();
                            cord = new Rect(0, start, device_width, start + device_height);
                        } else {
                            int start = horizontalScrollView.getScrollX();
                            cord = new Rect(start, 0, start + device_width, device_height);
                        }

                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getApplicationContext(), R.string.disabled_history_lacking_permission, Toast.LENGTH_SHORT).show();
                            editor.putBoolean(getString(R.string.enabled_history_key), false);
                            editor.apply();
                        }

                        bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
                        AlertDialog dialog = builder.create();
                        dialog.show();

//                        if (state == 3){
//                            Toast.makeText(getApplicationContext(), "state == 3 ", Toast.LENGTH_SHORT).show();
//                            SetImageAs(bitmap, MainActivity.this);
//                        }

                    });

                    fab.setOnLongClickListener(view -> {

                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getApplicationContext(), R.string.disabled_history_lacking_permission, Toast.LENGTH_SHORT).show();
                            editor.putBoolean(getString(R.string.enabled_history_key), false);
                            editor.apply();
                        }

                        if (sharedPreferences.getBoolean(getString(R.string.enabled_history_key), true)) {
                            Toast.makeText(this, "save wallpaper", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "not save wallpaper", Toast.LENGTH_SHORT).show();
                        }

                        cord = null;
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        return false;
                    });

                    //TODO:ADD zoom (if possible

                    //set bottomAppBar menu item
                    //tap blur and brightness button will disable other menu item
                    bottomAppBar.setOnMenuItemClickListener(item -> {

                        if (item.getItemId() == R.id.blur || item.getItemId() == R.id.brightness) {
                            bottomAppBarContainer.setVisibility(View.INVISIBLE);
                            AlertDialog dialog;

                            if (item.getItemId() == R.id.blur) {
                                dialog = createSliderDialog(R.string.blur);
                            } else {
                                dialog = createSliderDialog(R.string.brightness);
                            }
                            dialog.getWindow()
                                    .clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                            dialog.getWindow().setGravity(Gravity.BOTTOM);
                            dialog.setCancelable(false);
                            dialog.show();

                            Slider slider = dialog.findViewById(R.id.dialog_slider);
                            assert slider != null;
                            slider.setLabelBehavior(LABEL_GONE);
                            slider.setTickVisible(false);

                            if (item.getItemId() == R.id.blur) {
                                slider.setValueFrom(0);
                                slider.setValueTo(30);
                                slider.setStepSize(1);
                                slider.setValue(blurBias);
                            } else {
                                slider.setValueFrom(-50);
                                slider.setValueTo(50);
                                slider.setStepSize(1);
                                slider.setValue(brightnessBias);
                            }

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                                bitmap = processed;
                                bottomAppBarContainer.setVisibility(View.VISIBLE);
                                bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
                                if (item.getItemId() == R.id.blur) {
                                    blurBias = (int) slider.getValue();
                                } else {
                                    brightnessBias = (int) slider.getValue();
                                }
                                dialog.dismiss();
                            });

                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> slider.setValue(0.0f));

                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                                slider.setValue(0.0f);
                                bottomAppBarContainer.setVisibility(View.VISIBLE);
                                dialog.dismiss();
                            });

                            slider.addOnChangeListener((slider1, value, fromUser) -> {
                                if (item.getItemId() == R.id.blur) {
                                    HokoBlur.with(getApplicationContext())
                                            .radius((int) value)
                                            .sampleFactor(1.0f)
                                            .forceCopy(true)
                                            .needUpscale(true)
                                            .asyncBlur(bitmap, new AsyncBlurTask.Callback() {
                                                @Override
                                                public void onBlurSuccess(Bitmap bitmap) {
                                                    processed = bitmap;
                                                    imageView.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onBlurFailed(Throwable error) {

                                                }
                                            });
                                } else {
                                    new Thread(() -> {
                                        processed = Util.adjustBrightness(bitmap, (int) value);
                                        runOnUiThread(() -> imageView.setImageBitmap(processed));
                                    }).start();
                                }
                            });
                        } else if (item.getItemId() == R.id.reset) {
                            bitmap = raw;
                            blurBias = 0;
                            brightnessBias = 0;
                            cord = null;
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
                    resetItem.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                            startActivity(intent);
                            return true;
                        }
                    });

                    //setup AlertDialog builder
                    builder = new MaterialAlertDialogBuilder(MainActivity.this);
                    //builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.setAs);

                    String[] options = {
                            getResources().getString(R.string.home),
                            getResources().getString(R.string.lockscreen),
                            getResources().getString(R.string.homeAndLockscreen),
                            getResources().getString(R.string.use_others)};

                    builder.setItems(options, (dialog, which) -> {
                        executorService.execute(() -> {
                            try {
                                //若已经选择保存选项，弹出「设置图片为」选项之前保存图片
                                if (wallpaperManager.getWallpaperInfo() == null) {
                                    Log.v("WLP", permission);
                                    if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                                        Bitmap currentWallpaper = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                                        Util_Files.saveWallpaper(currentWallpaper, this);
//                                        saveWallpaper(currentWallpaper);
                                    }
//                                    else {
//                                        Toast.makeText(getApplicationContext(), R.string.no_permission, Toast.LENGTH_SHORT).show();
//                                    }
                                }

                                switch (which) {
                                    case 0:
                                        state = 0;
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                                        break;
                                    case 1:
                                        state = 1;
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                                        break;
                                    case 2:
                                        state = 2;
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                                        //应对定制 Rom（如 Color OS）可能存在的魔改导致 "WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM" 参数失效的情况。
                                        break;
                                    case 3:
                                        state = 3;
                                        shareBitmap(bitmap, getApplicationContext());
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + which);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

//                            handler.post(() -> {
//                                //dialog.cancel();
//                                imageView.setColorFilter(Color.rgb(123, 123, 123), PorterDuff.Mode.MULTIPLY);
//                                progressBar.bringToFront();
//                            });
                        });
//                        Intent i = new Intent();
//                        i.setAction(Intent.ACTION_MAIN);
//                        i.addCategory(Intent.CATEGORY_HOME);
//                        MainActivity.this.startActivity(i);
//                        finish();
                    });

                    bottomAppBarContainer.bringToFront();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static class ShareReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }

    //用于亮度/模糊的 Slider Bar 对话框
    @SuppressLint("InflateParams")
    private AlertDialog createSliderDialog(int title){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setTitle(title)
                .setView(inflater.inflate(R.layout.layout_dialog_adjustment, null))
        .setPositiveButton(R.string.save, null)
        .setNeutralButton(R.string.reset, null)
        .setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    //询问是否需要保存壁纸历史记录
    private AlertDialog saveHistoryDialog(){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(R.string.dialog_wallpaper_history)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.enabled_history_key), true);
                    editor.apply();

                    if (ContextCompat.checkSelfPermission(
                            getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED){
                        requestPermissionLauncher.launch(permission);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.enabled_history_key), false);
                    editor.apply();
                });
        return builder.create();
    }

    //请求权限
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
               if (!isGranted){
                   Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
                   SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
                   SharedPreferences.Editor editor = sharedPreferences.edit();
                   editor.putBoolean(getString(R.string.enabled_history_key), false);
                   editor.apply();
               }
            });

    Uri bitmapToUri(Bitmap bitmap, Context context){
        try {

            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File imagePath = new File(context.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.png");
        return FileProvider.getUriForFile(context, "com.maary.shareas", newFile);
    }

    private void shareBitmap(@NonNull Bitmap bitmap, Context context)
    {
        //---Save bitmap to external cache directory---//
        //get cache directory
        File cachePath = new File(getExternalCacheDir(), "my_images/");
        boolean result = cachePath.mkdirs();

        //create png file
        File file = new File(cachePath, "Image_123.png");
        FileOutputStream fileOutputStream;
        try
        {
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (IOException e)
        {
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
                receiver,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);

        startActivity(Intent.createChooser(
                sendIntent
                , getString(R.string.use_others)
                , pendingIntent.getIntentSender()
        ));
    }



}
