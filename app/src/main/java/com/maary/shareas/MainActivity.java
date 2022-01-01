package com.maary.shareas;

import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.hoko.blur.HokoBlur;
import com.hoko.blur.task.AsyncBlurTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
                    bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(false);
                    imageView.setImageBitmap(bitmap);

                    SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                    if (!sharedPreferences.contains(getString(R.string.enabled_history_key))) {
                        AlertDialog dialog_history = saveHistoryDialog();
                        dialog_history.show();
                    }

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

                        if (sharedPreferences.getBoolean(getString(R.string.enabled_history_key), true)) {
                            //TODO:save current wallpaper
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

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    bitmap = processed;
                                    bottomAppBarContainer.setVisibility(View.VISIBLE);
                                    bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
                                    if (item.getItemId() == R.id.blur) {
                                        blurBias = (int) slider.getValue();
                                    } else {
                                        brightnessBias = (int) slider.getValue();
                                    }
                                    dialog.dismiss();
                                }
                            });

                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    slider.setValue(0.0f);
                                }
                            });

                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    slider.setValue(0.0f);
                                    bottomAppBarContainer.setVisibility(View.VISIBLE);
                                    dialog.dismiss();
                                }
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

                    //setup AlertDialog builder
                    builder = new MaterialAlertDialogBuilder(MainActivity.this);
                    //builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.setAs);

                    String[] options = {getResources().getString(R.string.home), getResources().getString(R.string.lockscreen), getResources().getString(R.string.homeAndLockscreen)};
                    builder.setItems(options, (dialog, which) -> {
                        executorService.execute(() -> {
                            int FLAG;
                            try {
                                if (wallpaperManager.getWallpaperInfo() == null) {
                                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                        // here to request the missing permissions, and then overriding
                                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                        //                                          int[] grantResults)
                                        // to handle the case where the user grants the permission. See the documentation
                                        // for ActivityCompat#requestPermissions for more details.
                                        return;
                                    }
                                    Bitmap currentWallpaper = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                                    saveWallpaper(currentWallpaper);
                                }
                                switch (which) {
                                    case 0:
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                                        break;
                                    case 1:
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                                        break;
                                    case 2:
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_LOCK);
                                        wallpaperManager.setBitmap(bitmap, cord, true, WallpaperManager.FLAG_SYSTEM);
                                        //应对定制 Rom（如 Color OS）可能存在的魔改导致 "WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM" 参数失效的情况。
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + which);
                                }
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

    private AlertDialog saveHistoryDialog(){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(R.string.dialog_wallpaper_history)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.enabled_history_key), true);
                        editor.apply();
                        if (sharedPreferences.getBoolean(getString(R.string.enabled_history_key), false)){
                            if (ContextCompat.checkSelfPermission(
                                    getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                            }
                        }
                        //TODO:ask for permission
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.enabled_history_key), false);
                        editor.apply();
                    }
                });
        return builder.create();
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
               if (!isGranted){
                   Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
                   SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                   SharedPreferences.Editor editor = sharedPreferences.edit();
                   editor.putBoolean(getString(R.string.enabled_history_key), false);
                   editor.apply();
               }
            });

    private void saveWallpaper(Bitmap bitmap){
        Calendar calendar = Calendar.getInstance();
        String fileName = "WLP_" +
                String.valueOf(calendar.get(Calendar.YEAR) - 1900) +
                String.valueOf(calendar.get(Calendar.MONTH)) +
                String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)) +
                String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)) +
                String.valueOf(calendar.get(Calendar.MINUTE)) +
                String.valueOf(calendar.get(Calendar.MILLISECOND));
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Wallpaper History");

        final ContentResolver contentResolver = getContentResolver();
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

}
