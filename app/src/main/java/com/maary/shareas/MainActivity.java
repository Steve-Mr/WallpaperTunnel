package com.maary.shareas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        try{
            if (Intent.ACTION_SEND.equals(action) && type != null){
                if (type.startsWith("image/")){
                    handleImage(intent);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("done");
        registerReceiver(receiver, intentFilter);

    }

    void handleImage(Intent intent){
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(imageUri != null){
            Intent sendIntent = new Intent(Intent.ACTION_ATTACH_DATA);
            sendIntent.setDataAndType(imageUri, "image/*");
            sendIntent.putExtra("mimeType", "image/*");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent receiver = new Intent(this, ShareReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                    receiver,PendingIntent.FLAG_UPDATE_CURRENT);

            startActivity(Intent.createChooser(
                    sendIntent
                    , "Set as:"
                    , pendingIntent.getIntentSender()
            ));
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MainActivity.super.finishAndRemoveTask();
        }
    };

    public static class ShareReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent local = new Intent();
            local.setAction("done");
            context.sendBroadcast(local);
        }
    }
}
