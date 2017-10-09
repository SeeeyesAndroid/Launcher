package com.sscctv.seeeyeslauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.sscctv.seeeyes.VideoSource;
import com.sscctv.seeeyes.ptz.McuControl;

import net.biyee.android.utility;

import java.io.IOException;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SeeEyesLauncher";
    private static final String EXTRA_SOURCE = "com.sscctv.seeeyesmonitor.source";
    private static final String SOURCE_SDI = "sdi";
    private static final String SOURCE_HDMI = "hdmi";
    private static final String SOURCE_AHD = "ahd";
    private static final String SOURCE_TVI = "tvi";
    private static final String SOURCE_CVI = "cvi";
    private static final String SOURCE_CVBS = "cvbs";
    private final String closeBroadcast = "net.biyee.onviferenterprise.OnviferActivity";
    private BroadcastReceiver mReceiver = null;
    private McuControl mMcuControl;
    private VideoSource mSource;
    private boolean catchValue = false;

    private static final int[] BUTTONS = {
            R.id.ip_button,
            R.id.sdi_button,
            R.id.hdmi_button,
            R.id.apps_button,
            R.id.ahd_button,
            R.id.tvi_button,
            R.id.cvi_button,
            R.id.cvbs_button
    };
    private Button mButton, mButton1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        doFirst();
        if (getIntent().hasCategory(Intent.CATEGORY_HOME)) {
            Log.i(TAG, "-------------- Run as Launcher --------------");
        } else {
            Log.i(TAG, "-------------- Run as normal app -------------- ");
        }

        for (int btnID : BUTTONS) {
            ImageButton btn = (ImageButton) findViewById(btnID);
            btn.setOnClickListener(this);
        }


//    mButton = (Button)findViewById(R.id.asd);
//    mButton1 = (Button)findViewById(R.id.asdf);
//       mButton.setOnClickListener(this);
//       mButton1.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        PackageManager pm = getPackageManager();
        Intent intent = null;

        switch (v.getId()) {
            case R.id.ip_button:
                startService();
                intent = pm.getLaunchIntentForPackage("net.biyee.onviferenterprise");
                break;

            case R.id.sdi_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_SDI);
                break;

            case R.id.hdmi_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_HDMI);
                break;

            case R.id.apps_button:
                launchApps();
                break;

            case R.id.ahd_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_AHD);
                break;

            case R.id.tvi_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_TVI);
                break;

            case R.id.cvi_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_CVI);
                break;

            case R.id.cvbs_button:
                stopService();
                intent = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA);
                intent.putExtra(EXTRA_SOURCE, SOURCE_CVBS);
                break;

        }

        if (intent != null) {
            startActivity(intent);
        }
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "-------------- Launcher Start --------------");
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "-------------- Launcher Pause --------------");
        super.onPause();
        unregisterReciver();
    }

    private void registerReceiver() {
        if(mReceiver != null)
            return;
        Log.d(TAG, "registerRecevier start");
        IntentFilter closeOE = new IntentFilter();
        closeOE.addAction(closeBroadcast);

        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("state");
                Log.d(TAG, "Close Screen Broadcast : " + state);

                if(state.equals("close")) {
                   stopService();
                    Log.d(TAG, "Close OE");
                }
            }
        };
        this.registerReceiver(this.mReceiver, closeOE);
    }

    private void unregisterReciver() {
        if(mReceiver != null) {
            this.unregisterReceiver(mReceiver);
        }
    }

    private void startService() {
        if (!catchValue) {
            catchValue = true;
            Intent poeIntent = new Intent(this, PoEIntentService.class);
            startService(poeIntent);
            Log.d(TAG, "Start Catch Value = " + catchValue);
        }
    }

    private void stopService() {
        if (catchValue) {
            Intent poeIntent = new Intent(this, PoEIntentService.class);
            stopService(poeIntent);
            Log.d(TAG, "Stop Catch Value = " + catchValue);
        }
        catchValue = false;
    }

    private void launchApps() {
        Intent intent = new Intent(this, AppsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (!getIntent().hasCategory(Intent.CATEGORY_HOME)) {
            super.onBackPressed();
        }
    }

    private boolean doFirst() {
        SharedPreferences pref = getSharedPreferences("doFirst", MODE_PRIVATE);
        boolean doFirst = pref.getBoolean("doFirst", true);

        if (doFirst) {
            Intent intent = new Intent(this, LanguageSettings.class);
            startActivity(intent);

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("doFirst", false);
            editor.apply();
        }
        return doFirst;
    }

}