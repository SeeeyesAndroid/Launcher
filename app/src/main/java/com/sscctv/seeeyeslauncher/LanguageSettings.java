package com.sscctv.seeeyeslauncher;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Created by Jskim on 2017-09-07.
 */

public class LanguageSettings extends AppCompatActivity {

    private static final String TAG = "Language Settings";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_settings);
        Button.OnClickListener onClickListener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlarmManager systemService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                switch (v.getId()) {
                    case R.id.button_korean:
                        switchLanguage(this, new Locale("ko"));
                        systemService.setTimeZone("Asia/Seoul");
                        break;
                    case R.id.button_english:
                        switchLanguage(this, new Locale("en"));
                        systemService.setTimeZone("America/New_york");
                        break;
                    case R.id.button_japanese:
                        switchLanguage(this, new Locale("ja"));
                        systemService.setTimeZone("Asia/Seoul");
                        break;
                    case R.id.button_italian:
                        switchLanguage(this, new Locale("it"));
                        systemService.setTimeZone("Europe/Rome");
                        break;
                    case R.id.button_german:
                        switchLanguage(this, new Locale("de"));
                        systemService.setTimeZone("Europe/Berlin");
                        break;
                    case R.id.button_next:
                        launcherPage();
                        break;
                }

            }
        };
        Button language_korean = (Button)findViewById(R.id.button_korean);
        language_korean.setOnClickListener(onClickListener);
        Button language_english = (Button)findViewById(R.id.button_english);
        language_english.setOnClickListener(onClickListener);
        Button language_japanese = (Button)findViewById(R.id.button_japanese);
        language_japanese.setOnClickListener(onClickListener);
        Button language_italian = (Button)findViewById(R.id.button_italian);
        language_italian.setOnClickListener(onClickListener);
        Button language_german = (Button)findViewById(R.id.button_german);
        language_german.setOnClickListener(onClickListener);
        Button next = (Button)findViewById(R.id.button_next);
        next.setOnClickListener(onClickListener);


    }
    private void switchLanguage(View.OnClickListener con, Locale language)  {

        try{
            Locale locale = language;
            Class amnClass = Class.forName("android.app.ActivityManagerNative");
            Object amn = null;
            Configuration config = null;

            Method methodGetDefault = amnClass.getMethod("getDefault");
            methodGetDefault.setAccessible(true);
            amn = methodGetDefault.invoke(amnClass);

            Method methodGetConfiguration = amnClass.getMethod("getConfiguration");
            methodGetConfiguration.setAccessible(true);
            config = (Configuration) methodGetConfiguration.invoke(amn);

            Class configClass = config.getClass();
            Field f = configClass.getField("userSetLocale");
            f.setBoolean(config, true);

            config.locale = locale;

            Method methodUpdateConfiguration = amnClass.getMethod("updateConfiguration", Configuration.class);
            methodUpdateConfiguration.setAccessible(true);
            methodUpdateConfiguration.invoke(amn, config);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void launcherPage() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
