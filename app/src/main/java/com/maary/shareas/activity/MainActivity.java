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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
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
import com.maary.shareas.R;
import com.maary.shareas.WallpaperViewModel;
import com.maary.shareas.fragment.EditorFragment;
import com.maary.shareas.helper.PreferencesHelper;
import com.maary.shareas.helper.Util;
import com.maary.shareas.helper.Util_Files;
import com.maary.shareas.view.ScrollableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static final int MENU_RESET = 0;
    Bitmap bitmap;
    Rect cord;
    MaterialAlertDialogBuilder builder;
    Palette.Swatch vibrant;
    Palette.Swatch dominant;
    Palette.Swatch muted;
    Intent intent;
    Snackbar snackbarReturnHome;

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

        try {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    bitmap = Util.getBitmap(intent, MainActivity.this);
                    if (bitmap == null) {
                        return;
                    }
                }
            }

            WallpaperViewModel viewModel = new ViewModelProvider(this).get(WallpaperViewModel.class);

            Point deviceBounds = Util.getDeviceBounds(MainActivity.this);
            int device_height = deviceBounds.y;
            int device_width = deviceBounds.x;

            //Parent layout
            ConstraintLayout container = findViewById(R.id.container);
            //parent layout of bottomAppBar
            CoordinatorLayout bottomAppBarContainer = findViewById(R.id.bottomAppBarContainer);
            //progressBar usd in wallpaper setting process

            BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

            FloatingActionButton fab = findViewById(R.id.fab);
            ScrollableImageView imageView = findViewById(R.id.main_view);
            //image ratio > device ratio?
            Boolean isVertical = Util.isVertical(device_height, device_width, bitmap);

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            //show image to imageview
            int bitmap_full_width = bitmap.getWidth();
            int bitmap_full_height = bitmap.getHeight();
            int desired_width;
            int desired_height;

            if (isVertical) {
                desired_width = device_width;
                float scale = (float) device_width / bitmap_full_width;
                desired_height = (int) (scale * bitmap_full_height);
            } else {
                desired_height = device_height;
                float scale = (float) device_height / bitmap_full_height;
                desired_width = (int) (scale * bitmap_full_width);
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, desired_width, desired_height, true);

            viewModel.setBitmap(bitmap);
            viewModel.getViewerStateLiveData().observe(this, state -> {
                imageView.setImageBitmap(Objects.requireNonNull(viewModel.getDisplayBitmap()));
            });
            viewModel.getCurrentBitmapStateLiveData().observe(this, state -> {
                imageView.setImageBitmap(Objects.requireNonNull(viewModel.getDisplayBitmap()));
                fab.setImageResource(viewModel.getFabResource());
            });
            viewModel.getInEditorLiveData().observe(this, inEditor -> {
                if (inEditor) {
                    bottomAppBarContainer.setVisibility(View.INVISIBLE);
                    loadFragment(new EditorFragment());
                } else {
                    bottomAppBarContainer.setVisibility(View.VISIBLE);
                    removeFragment();
                }
            });


            imageView.setOnImageClickListener(v -> {
                viewModel.currentBitmapToggle();
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

            });

            //setup the fab click listener
            fab.setOnClickListener(view -> {
                cord = imageView.getVisibleRect();
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
                    viewModel.startEditing();
                } else if (item.getItemId() == R.id.reset) {
                    viewModel.restoreChanges();
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
                    if (new PreferencesHelper(this).getSettingsHistory()) {
                        //若已经选择保存选项，弹出「设置图片为」选项之前保存图片
                        if (checkPermission()) {
                            Bitmap currentWallpaper = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
                            Util_Files.saveWallpaper(currentWallpaper, this);
                        } else {
                            Snackbar.make(container, R.string.no_permission, Snackbar.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    switch (which) {
                        case 0 -> {
                            wallpaperManager.setBitmap(viewModel.getBitmapHome(), cord, true, WallpaperManager.FLAG_SYSTEM);
                        }
                        case 1 -> {
                            wallpaperManager.setBitmap(viewModel.getBitmapLock(), cord, true, WallpaperManager.FLAG_LOCK);
                        }
                        case 2 -> {
                            wallpaperManager.setBitmap(viewModel.getBitmapHome(), cord, true, WallpaperManager.FLAG_SYSTEM);
                            wallpaperManager.setBitmap(viewModel.getBitmapLock(), cord, true, WallpaperManager.FLAG_LOCK);
                        }
                        case 3 -> {
                            Uri myImageFileUri = viewModel.getBitmapUri(getApplicationContext(), Objects.requireNonNull(getExternalCacheDir()));
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
                        default -> throw new IllegalStateException("Unexpected value: " + which);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }));

            bottomAppBarContainer.bringToFront();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean checkPermission() {
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

    public static class ShareReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.editor_container, fragment);
        transaction.addToBackStack(null); // 可选，用于返回栈管理
        transaction.commit();
    }

    private void removeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment f: fragmentManager.getFragments()){
            transaction.remove(f);
        }
        transaction.commit();
        fragmentManager.popBackStack();
    }
}
