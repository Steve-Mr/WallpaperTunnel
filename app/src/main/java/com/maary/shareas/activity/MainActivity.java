package com.maary.shareas.activity;

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
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.maary.shareas.R;
import com.maary.shareas.WallpaperViewModel;
import com.maary.shareas.databinding.ActivityMainBinding;
import com.maary.shareas.fragment.EditorFragment;
import com.maary.shareas.helper.PreferencesHelper;
import com.maary.shareas.helper.Util;
import com.maary.shareas.helper.Util_Files;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Triple;

public class MainActivity extends AppCompatActivity {

    static final int MENU_RESET = 0;
    Rect cord;
    MaterialAlertDialogBuilder builder;
    Palette.Swatch vibrant;
    Palette.Swatch dominant;
    Palette.Swatch muted;

    private ActivityMainBinding binding;

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.indicatorContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top; // * 2; //+ mlp.topMargin;
            v.setLayoutParams(mlp);
            return windowInsets;
        });


        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

        if (!Intent.ACTION_SEND.equals(action) || type == null) return;
        if (!type.startsWith("image/")) return;
        Bitmap bitmap = Util.getBitmap(intent, MainActivity.this);
        if (bitmap == null) return;

        WallpaperViewModel viewModel = new ViewModelProvider(this).get(WallpaperViewModel.class);

        viewModel.setBitmapRaw(bitmap, this);
        viewModel.getViewerStateLiveData().observe(this, state -> binding.mainView.setImageBitmap(Objects.requireNonNull(viewModel.getDisplayBitmap())));
        viewModel.getCurrentBitmapStateLiveData().observe(this, state -> {
            binding.mainView.setImageBitmap(Objects.requireNonNull(viewModel.getDisplayBitmap()));
            binding.indicatorCurrentState.setIcon(ContextCompat.getDrawable(getApplicationContext(), viewModel.getFabResource()));
        });
        viewModel.getInEditorLiveData().observe(this, inEditor -> {
            if (inEditor) {
                binding.indicatorCurrentState.setVisibility(View.INVISIBLE);
                binding.bottomAppBarContainer.setVisibility(View.INVISIBLE);
            } else {
                binding.indicatorCurrentState.setVisibility(View.VISIBLE);
                binding.bottomAppBarContainer.setVisibility(View.VISIBLE);
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        binding.mainView.setOnImageClickListener(v -> viewModel.currentBitmapToggle());

        binding.indicatorCurrentState.setOnClickListener( v -> viewModel.currentBitmapToggle());
        binding.indicatorCurrentState.setBackgroundColor(viewModel.getTertiaryColorAlt(getApplicationContext()));
        binding.indicatorCurrentState.setIconTint(ColorStateList.valueOf(viewModel.getTertiaryColor(getApplicationContext())));

        Palette.from(bitmap).generate(palette -> {
            // Access the colors from the palette
            assert palette != null;
            vibrant = palette.getVibrantSwatch();
            muted = palette.getMutedSwatch();
            dominant = palette.getDominantSwatch();

            if (vibrant != null) {
                binding.fab.setBackgroundTintList(ColorStateList.valueOf(vibrant.getRgb()));
            } else {
                binding.fab.setBackgroundColor(palette.getVibrantColor(getColor(R.color.colorAccent)));
            }

        });

        //setup the fab click listener
        binding.fab.setOnClickListener(view -> {
            cord = binding.mainView.getVisibleRect();
            builder.setTitle(R.string.setAs);
            binding.bottomAppBar.getMenu().getItem(MENU_RESET).setEnabled(true);
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        binding.fab.setOnLongClickListener(view -> {
            cord = null;
            builder.setTitle(R.string.setFullAs);
            AlertDialog dialog = builder.create();
            dialog.show();
            return false;
        });

        //TODO:ADD zoom (if possible

        Snackbar snackbarReturnHome = Snackbar.make(binding.container, getString(R.string.wallpaper_setted), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.gohome), v -> returnToHomeScreen());

        WallpaperManager.OnColorsChangedListener wallpaperChangedListener = new WallpaperManager.OnColorsChangedListener() {
            @Override
            public void onColorsChanged(@Nullable WallpaperColors colors, int which) {
                wallpaperManager.removeOnColorsChangedListener(this);
                snackbarReturnHome.show();
            }
        };

        wallpaperManager.addOnColorsChangedListener(wallpaperChangedListener, new Handler(Looper.getMainLooper()));

        if (viewModel.isAlignmentNeeded(this)) {
            binding.bottomAppBar.getMenu().getItem(2).setIcon(viewModel.getAlignmentIconResource(this));
        } else {
            binding.bottomAppBar.getMenu().getItem(2).setVisible(false);
        }
        //set bottomAppBar menu item
        //tap blur and brightness button will disable other menu item
        binding.bottomAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.edit) {
//                viewModel.startEditing();
                loadFragment(new EditorFragment());
            } else if (item.getItemId() == R.id.reset) {
                viewModel.restoreChanges();
            } else if (item.getItemId() == R.id.alignment_center) {
                Triple<Integer, Integer, Integer> param = viewModel.getCenterAlignParam(this);
                if (param != null) {
                    binding.mainView.scrollImageTo(param.getFirst(), param.getSecond(), param.getThird());
                }
            }
            return true;
        });

        ActionMenuItemView resetItem = binding.bottomAppBar.findViewById(R.id.reset);
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
//        builder.setTitle(R.string.setAs);

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
                        Bitmap currentWallpaper = ((BitmapDrawable) Objects.requireNonNull(wallpaperManager.getDrawable())).getBitmap();
                        Util_Files.saveWallpaper(currentWallpaper, this);
                    } else {
                        Snackbar.make(binding.container, R.string.no_permission, Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }

                switch (which) {
                    case 0 -> wallpaperManager.setBitmap(viewModel.getBitmapHome(), cord, true, WallpaperManager.FLAG_SYSTEM);
                    case 1 -> wallpaperManager.setBitmap(viewModel.getBitmapLock(), cord, true, WallpaperManager.FLAG_LOCK);
                    case 2 -> {
                        wallpaperManager.setBitmap(viewModel.getBitmapHome(), cord, true, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(viewModel.getBitmapLock(), cord, true, WallpaperManager.FLAG_LOCK);
                    }
                    case 3 -> {
                        Uri myImageFileUri = viewModel.getBitmapUri(getApplicationContext(), Objects.requireNonNull(getExternalCacheDir()));
                        shareUri(getApplicationContext(), myImageFileUri);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + which);
                }
            } catch (IOException e) {
                Log.e("ERROR", e.toString());
            }

        }));

        binding.bottomAppBarContainer.bringToFront();

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

    private void shareUri(Context context, Uri uri) {
        Intent sendIntent = new Intent(Intent.ACTION_ATTACH_DATA);
        sendIntent.setDataAndType(uri, "image/*");
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
}
