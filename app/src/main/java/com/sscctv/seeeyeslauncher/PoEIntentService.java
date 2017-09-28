package com.sscctv.seeeyeslauncher;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.sscctv.seeeyes.VideoSource;
import com.sscctv.seeeyes.ptz.LevelMeterListener;
import com.sscctv.seeeyes.ptz.McuControl;
import net.biyee.android.utility;
import java.io.FileOutputStream;
import java.io.IOException;
import static com.sscctv.seeeyes.VideoSource.IPC;
import static java.lang.String.format;

public class PoEIntentService extends IntentService {
    private McuControl mMcuControl;
    private VideoSource mSource;
    static final String sLogTag = "PoeService";
    public static final String sAction = "net.biyee.poe.action";
    private final String sBroadcast = "net.biyee.onviferenterprise.PlayVideoActivity";
    private BroadcastReceiver mReceiver = null;
    private TextView mPoeLevel, mFocusLevel, mFocusPoeLevel ;
    private View mPoeView, mFocusPoeView;
    private WindowManager mManager, sManager;
    private WindowManager.LayoutParams mParams, sParams;
    private float mTouchX, mTouchY;
    private int mViewX, mViewY;
    private int mValue, sValue, mFocus;
    private int mFocusLevelMax;
    private boolean pfview = true;
    private boolean isMove = false;
    private boolean mInit;
    public static int iFOREGROUND_SERVICE = 101;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isMove = false;
                    mTouchX = event.getRawX(); mTouchY = event.getRawY();
                    mViewX = mParams.x; mViewY = mParams.y;
                    mViewX = sParams.x; mViewY = sParams.y;
                    break;
                case MotionEvent.ACTION_UP:
                    resetFocusLevel();
                    break;
                case MotionEvent.ACTION_MOVE:
                    isMove = true;
                    int x = (int) (event.getRawX() - mTouchX); int y = (int) (event.getRawY() - mTouchY);
                    final int num = 5;
                    if ((x > -num && x < num) && (y > -num && y < num)) {
                        isMove = false;
                        break;
                    }
                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    sParams.x = mViewX + x;
                    sParams.y = mViewY +y;
                    mManager.updateViewLayout(mPoeView, mParams);
                    sManager.updateViewLayout(mFocusPoeView,sParams);
                    break;
            }
            return true;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater poe_Inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPoeView = poe_Inflater.inflate(R.layout.poe_on_view, null);
        mPoeLevel = (TextView) mPoeView.findViewById(R.id.mp_poe_level);
        mPoeView.setOnTouchListener(mViewTouchListener);
        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                                                    WindowManager.LayoutParams.WRAP_CONTENT, 0, 20,
                                                    WindowManager.LayoutParams.TYPE_PHONE,
                                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                                    PixelFormat.TRANSLUCENT);
        mParams.gravity =  Gravity.TOP | Gravity.CENTER;
        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mPoeView, mParams);
        mPoeView.setVisibility(View.INVISIBLE);

        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mFocusPoeView = mInflater.inflate(R.layout.poe_foucs_view, null);
        mFocusPoeLevel = (TextView) mFocusPoeView.findViewById(R.id.focus_poe_level);
        mFocusLevel = (TextView) mFocusPoeView.findViewById(R.id.focus_level);
        //mFocusPoeView.setOnTouchListener(mViewTouchListener);
        sParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, 50, 20,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        sParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        sManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sManager.addView(mFocusPoeView, sParams);
        mFocusPoeView.setVisibility(View.INVISIBLE);
    }

    public PoEIntentService() {
        super("PoEIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        utility.LOG = true;
        utility.logd(sLogTag, "Start service in foreground ");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(sAction);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_poe);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("PoE")
                .setTicker("PoE")
                .setContentText("Running PoE service")
                .setSmallIcon(R.drawable.ic_action_poe)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 32, 32, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
        startForeground(iFOREGROUND_SERVICE, notification);

        if (mMcuControl == null) {
            mMcuControl = new McuControl();
            try {
                mSource = new VideoSource(IPC);
                mSource.pseDisable();
                mSource.vpEnable();
                pollPoE(mMcuControl, mSource);
                utility.logd(sLogTag, "Started PoE voltage polling by PoE.pollPoE()");
            } catch (Exception ex) {
                utility.logd(sLogTag, "Exception in PoEVoltageDisplayFragment.onResume:" + utility.getStackTrace(ex));
            }
        }
        registerReceiver();
        pfview = false;
        return START_STICKY;
    }

    private void registerReceiver() {
        if(mReceiver != null)
            return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(sBroadcast);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("state");
                String focus = intent.getStringExtra("focus");
                if(state != null && state.equals("resume")){
                    pfview = true;
                    mPoeView.setVisibility(View.INVISIBLE);
                    mFocusPoeView.setVisibility(View.VISIBLE);
                    setTpMode(true);
                    mSource.setCvbsOut(!mInit);
                    try {
                        mMcuControl.startLevelMeter();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    utility.logd(sLogTag, "Resume pfLevel " + pfview);
                }

                if(state != null && state.equals("pause")) {
                    pfview = false;
                    mFocusPoeView.setVisibility(View.INVISIBLE);
                    mPoeView.setVisibility(View.VISIBLE);
                    setTpMode(false);
                    mSource.setCvbsOut(mInit);
                    try {
                        mMcuControl.stopLevelMeter();
                        mMcuControl.startPoeCheck();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    utility.logd(sLogTag, "Pause pfLevel " + pfview);

                }
                if(focus != null && focus.equals("set")){
                    resetFocusLevel();
                    utility.logd(sLogTag, "Focus Reset");
                }
            }
        };
        this.registerReceiver(this.mReceiver, filter);
    }

    private void unregisterReceiver() {
        if(mReceiver != null){
            this.unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver();
       // Toast.makeText(this, "PoE Service Destroy", Toast.LENGTH_SHORT).show();
        if(mPoeView != null){
            mManager.removeView(mPoeView);
        }
        super.onDestroy();
    }

    private static void setTpMode(boolean value) {
        try {
            FileOutputStream file = new FileOutputStream("/sys/class/misc/tp2802dev/run");
            file.write((value ? "6" : "0").getBytes());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pollPoE(final McuControl mMcuControl, final VideoSource mSource) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                    try {
                        mMcuControl.start(mSource.getSourceId());
                        mMcuControl.startPoeCheck();
                        //mSource.poeVoltCheckOn();
                        mMcuControl.addReceiveBufferListener(new LevelMeterListener() {
                            @Override
                            public void onLevelChanged(final int level, final int value) {
                                try {
                                    switch (level) {
                                        case MASTER_CHECK_POE:
                                            mValue = value;
                                            break;
                                        case SUB_CHECK_POE:
                                            sValue = value;
                                            break;
                                        case LEVEL_FOCUS:
                                            mFocus = value;
                                            break;
                                    }
//                                    utility.logd(sLogTag, ("PoE Volt: "+ mValue + " . " + sValue + " V "));
//                                    utility.logd(sLogTag, ("Pse State = "+ mSource.getPseState() + " Vp State = " + mSource.getVpState()));
                                    Message poe_msg = poe_handler.obtainMessage();
                                    Message poe_focus_msg = poe_focus_handler.obtainMessage();
                                    if ((mValue != 0) && pfview) {
                                        mSource.vpDisable();
                                        poe_focus_handler.sendMessage(poe_msg);
//                                        utility.logd(sLogTag, ("Number 1 state"));
                                    } else if ((mValue != 0) && !pfview){
                                        mSource.vpDisable();
                                        poe_handler.sendMessage(poe_focus_msg);
//                                        utility.logd(sLogTag, ("Number 2 state"));
                                    } else if ((mValue == 0) && pfview){
                                        poe_focus_handler.sendMessage(poe_msg);
//                                        utility.logd(sLogTag, ("Number 3 state"));
                                    } else if ((mValue == 0) && !pfview){
                                        mSource.vpEnable();
                                        poe_handler.sendMessage(poe_msg);
//                                        utility.logd(sLogTag, ("Number 4 state"));
                                    } else if (mValue == 0){
                                        mSource.pseEnable();
                                        mSource.vpEnable();
                                        poe_handler.sendMessage(poe_msg);
//                                        utility.logd(sLogTag, ("Number 5 state"));

                                    }
                                } catch (Exception ex) {
                                    utility.logExceptionAsync(PoEIntentService.this, "Exception in onLevelChanged():", ex);
                                    mInit = true;
                                }
                            }
                        });
                    } catch (Exception ex) {
                        utility.logExceptionAsync(PoEIntentService.this, "Exception in xxx():", ex);
                    }
                }
        }).start();
    }

    final Handler poe_handler = new Handler() {
        public void handleMessage(Message msg) {
            mPoeView.setVisibility(View.VISIBLE);
            switch (mSource.getPseState()) {
                case 0:
                    if(mValue == 0) {
                        mPoeLevel.setText("PoE 48V OUT");
//                        utility.logd(sLogTag, "Home Menu, PSE 0(ON), Not Use PoE CHeck");
                    } else {
                        mPoeLevel.setText(format("PoE : %02d.%02d V", mValue, sValue));
//                        utility.logd(sLogTag, "mValue = " + mValue + " " + "sValue = " + sValue);
//                        utility.logd(sLogTag, "Home Menu, PSE 0(ON), Use PoE Check");
                    }
                    break;
                case 1:
                    if(mValue == 0) {
                        mPoeLevel.setText("PoE OFF");
//                        utility.logd(sLogTag, "Home Menu, PSE 1(OFF)");
                    }
                    break;
            }
        }
    };

    final Handler poe_focus_handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (mSource.getPseState()) {
                case 0:
                    mFocusPoeView.setVisibility(View.VISIBLE);
                    if(mValue == 0) {
                        mFocusPoeLevel.setText("PoE   : 48V OUT");
//                        utility.logd(sLogTag, "Focus Home Menu, PSE 0(ON), Not Use PoE CHeck");
                        updateFocusLevel(mFocus);
                    } else {
                        mFocusPoeLevel.setText(format("PoE   : %02d.%02d V", mValue, sValue));
//                        utility.logd(sLogTag, "Focus Home Menu, PSE 0(ON), Use PoE CHeck");
                        updateFocusLevel(mFocus);
                    }
                    break;
                case 1:
                    mFocusPoeView.setVisibility(View.VISIBLE);
                    if(mValue == 0) {
                        mFocusPoeLevel.setText("PoE   : OFF");
//                        utility.logd(sLogTag, "Focus Home Menu, PSE 1(OFF), Not Use PoE CHeck");
                        updateFocusLevel(mFocus);
                    } else {
                        mFocusPoeLevel.setText("PoE   : OFF");
//                        utility.logd(sLogTag, "Focus Home Menu, PSE 01(OFF), Use PoE CHeck");
                        updateFocusLevel(mFocus);
                    }
                    break;
            }
        }
    };

    public void updateFocusLevel(int focusLevel) {
        if (mFocusLevelMax < focusLevel) {
            mFocusLevelMax = focusLevel;
        }
        mFocusLevel.setText(format("FOCUS : %03d/%03d", focusLevel, mFocusLevelMax));
//        utility.logd(sLogTag, "Focus Level =  " + focusLevel);
    }

    public void resetFocusLevel() {
        mFocusLevelMax = 0;
        mFocusLevel.setText(format("FOCUS : %03d/%03d", 0, 0));
    }
    @Override
    protected void onHandleIntent(Intent intent) {
    }
}
