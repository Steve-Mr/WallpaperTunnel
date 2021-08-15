package com.maary.shareas;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.hoko.blur.HokoBlur;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Bitmap bitmap;
    Bitmap blur;
    Bitmap brightness;

    Button button;
    Button buttonFull;
    ToggleButton blurButton;
    ToggleButton brightnessButton;

    private static final int READ_EXTERNAL_STORAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);


        int device_height = getDeviceBounds().y;
        int device_width = getDeviceBounds().x;

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        try {
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
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

                    LinearLayout functionBar = new LinearLayout(this);
                    RelativeLayout.LayoutParams functionBarParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    functionBar.setOrientation(LinearLayout.HORIZONTAL);
                    functionBar.setId(View.generateViewId());
                    functionBar.setLayoutParams(functionBarParams);

                    HorizontalScrollView functionScrollView = new HorizontalScrollView(this);
                    RelativeLayout.LayoutParams functionScrollViewParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    functionScrollViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    functionScrollViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    functionScrollView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            //button.(16,16,16, insets.getSystemWindowInsetBottom());
                            functionScrollViewParams.setMargins(16, 16, 16, insets.getSystemWindowInsetBottom());
                            return insets.consumeSystemWindowInsets();
                        }
                    });
                    //functionScrollView.setLayoutParams(functionScrollViewParams);
                    functionScrollView.setFillViewport(false);
                    functionScrollView.setId(View.generateViewId());

                    //Show Image
                    ImageView imageView = new ImageView(this);

                    LinearLayout.LayoutParams hImageLayoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);

                    ViewGroup.LayoutParams vImageLayoutParams =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);

                    if (isVertical) {
                        imageView.setLayoutParams(vLayoutParams);
                    } else {
                        imageView.setLayoutParams(hLayoutParams);
                    }

                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    imageView.setAdjustViewBounds(true);
                    imageView.setId(View.generateViewId());

                    imageView.setImageBitmap(bitmap);

                    //XmlPullParser parser = Resources.getSystem().getXml(R.xml.customSeekbar);
                    SeekBar blurSeekbar = new SeekBar(this);
                    RelativeLayout.LayoutParams blurSeekbarParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    blurSeekbarParams.addRule(RelativeLayout.ABOVE, functionScrollView.getId());
                    blurSeekbar.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            blurSeekbarParams.setMargins(
                                    insets.getSystemWindowInsetLeft() + 16,
                                    16,
                                    insets.getSystemWindowInsetRight() + 16,
                                    16);
                            return insets.consumeSystemWindowInsets();
                        }
                    });
                    blurSeekbar.setMax(25);
                    //blurSeekbar.setBackgroundColor(Color.WHITE);
                    //blurSeekbar.getProgressDrawable().setColorFilter();
                    blurSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            blur = HokoBlur.with(getApplicationContext()).radius(progress).blur(bitmap);
                            imageView.setImageBitmap(blur);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

                    SeekBar brightnessSeekbar = new SeekBar(this);
                    RelativeLayout.LayoutParams brightnessSeekbarParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    brightnessSeekbarParams.addRule(RelativeLayout.ABOVE, functionScrollView.getId());
                    brightnessSeekbar.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            blurSeekbarParams.setMargins(
                                    insets.getSystemWindowInsetLeft() + 16,
                                    16,
                                    insets.getSystemWindowInsetRight() + 16,
                                    16);
                            return insets.consumeSystemWindowInsets();
                        }
                    });
                    brightnessSeekbar.setMax(101);
                    brightnessSeekbar.setProgress(51);
                    brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            brightness = adjustBrightness(bitmap, progress - 51);
                            imageView.setImageBitmap(brightness);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

                    //Set Button
                    button = new Button(this);
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    button.setText("SET");
                    button.setId(View.generateViewId());
                    button.setLayoutParams(buttonParams);
                    //Bitmap finalBitmap = bitmap;
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Rect cord = new Rect(0, 0, 0, 0);
                            if (isVertical) {
                                float scale = (float) bitmap_full_height / verticalScrollView.getChildAt(0).getHeight();

                                int start = (int) (scale * verticalScrollView.getScrollY());

                                int result_width = (int) (scale * device_width);
                                int result_height = (int) (scale * device_height);
                                result_width = Math.min(result_width, bitmap_full_width);
                                result_height = Math.min(result_height, bitmap_full_height);

                                cord = new Rect(0, start, result_width, start + result_height);
                            } else {
                                float scale = (float) bitmap_full_width / horizontalScrollView.getChildAt(0).getWidth();

                                int start = (int) (scale * horizontalScrollView.getScrollX());
                                int result_width = (int) (scale * device_width);
                                int result_height = (int) (scale * device_height);
                                result_width = Math.min(result_width, bitmap_full_width);
                                result_height = Math.min(result_height, bitmap_full_height);

                                cord = new Rect(start, 0, start + result_width, result_height);
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Set AS");

                            String[] options = {"home", "lockscreen", "home & lockscreen"};
                            Rect finalCord = cord;
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            try {
                                                wallpaperManager.setBitmap(bitmap, finalCord, true, WallpaperManager.FLAG_SYSTEM);
                                                break;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        case 1:
                                            try {
                                                wallpaperManager.setBitmap(bitmap, finalCord, true, WallpaperManager.FLAG_LOCK);
                                                break;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        case 2:
                                            try {
                                                wallpaperManager.setBitmap(bitmap, finalCord, true, WallpaperManager.FLAG_SYSTEM|WallpaperManager.FLAG_LOCK);
                                                break;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                    }
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();

                        }
                    });

                    blurButton = new ToggleButton(this);
                    LinearLayout.LayoutParams blurButtonParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    blurButton.setId(View.generateViewId());
                    blurButton.setLayoutParams(blurButtonParams);
                    blurButton.setText("BLUR");
                    blurButton.setTextOff("BLUR");
                    blurButton.setTextOn("OK");
                    blurButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                blurButton.setText("OK");
                                relativeLayout.addView(blurSeekbar, blurSeekbarParams);
                                button.setClickable(false); button.setPressed(true);
                                buttonFull.setClickable(false); buttonFull.setPressed(true);
                                brightnessButton.setClickable(false); brightnessButton.setPressed(true);
                            } else {
                                blurButton.setText("BLUR");
                                bitmap = blur;
                                relativeLayout.removeView(blurSeekbar);
                                button.setClickable(true); button.setPressed(false);
                                buttonFull.setClickable(true); buttonFull.setPressed(false);
                                brightnessButton.setClickable(true); brightnessButton.setPressed(false);
                            }
                        }
                    });

                    brightnessButton = new ToggleButton(this);
                    LinearLayout.LayoutParams brightnessButtonParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    brightnessButton.setId(View.generateViewId());
                    brightnessButton.setLayoutParams(brightnessButtonParams);
                    brightnessButton.setText("BRIGHTNESS");
                    brightnessButton.setTextOff("BRIGHTNESS");
                    brightnessButton.setTextOn("OK");
                    //blurButton.setBackgroundColor(Color.TRANSPARENT);
                    brightnessButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                brightnessButton.setText("OK");
                                relativeLayout.addView(brightnessSeekbar, brightnessSeekbarParams);
                                button.setClickable(false); button.setPressed(true);
                                buttonFull.setClickable(false); buttonFull.setPressed(true);
                                blurButton.setClickable(false); blurButton.setPressed(true);
                            } else {
                                brightnessButton.setText("BRIGHTNESS");
                                bitmap = brightness;
                                relativeLayout.removeView(brightnessSeekbar);
                                button.setClickable(true); button.setPressed(false);
                                buttonFull.setClickable(true); buttonFull.setPressed(false);
                                blurButton.setClickable(true); blurButton.setPressed(false);
                            }
                        }
                    });

                    buttonFull = new Button(this);
                    LinearLayout.LayoutParams buttonFullParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    buttonFull.setText("SET FULL");
                    buttonFull.setLayoutParams(buttonFullParams);
                    buttonFull.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Set As");

                            String[] options = {"Home", "Lockscreen", "Home & Lockscreen"};
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case 0:
                                            try {
                                                wallpaperManager.setBitmap(bitmap,null,true, WallpaperManager.FLAG_SYSTEM);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 1:
                                            try {
                                                wallpaperManager.setBitmap(bitmap,null,true, WallpaperManager.FLAG_LOCK);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case 2:
                                            try {
                                                wallpaperManager.setBitmap(bitmap,null,true, WallpaperManager.FLAG_SYSTEM|WallpaperManager.FLAG_LOCK);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                    }
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });

                    ConstraintLayout container = findViewById(R.id.container);

                    container.addView(relativeLayout);
                    if (isVertical) {
                        relativeLayout.addView(verticalScrollView);
                        verticalScrollView.addView(imageView);
                    } else {
                        relativeLayout.addView(horizontalScrollView);
                        horizontalScrollView.addView(imageView);
                    }
                    relativeLayout.addView(functionScrollView, functionScrollViewParams);
                    functionScrollView.addView(functionBar);
                    functionBar.addView(button);
                    functionBar.addView(blurButton);
                    functionBar.addView(brightnessButton);
                    functionBar.addView(buttonFull);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Bitmap getBitmap(Intent intent) throws IOException {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } else return null;
    }

    Point getDeviceBounds() {
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getMaximumWindowMetrics();
            point = new Point(windowMetrics.getBounds().width(), windowMetrics.getBounds().height());
        } else {
            Display display = getWindowManager().getDefaultDisplay();
            display.getRealSize(point);
        }
        return point;
    }

    Boolean isVertical(int dheight, int dwidth, Bitmap bitmap) {
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

    public static class ShareReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }
}
