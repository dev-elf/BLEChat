package dnay2k.com.w_hackathon_sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;


import dnay2k.com.w_hackathon_sample.blechatservice.service.BTCTemplateService;
import dnay2k.com.w_hackathon_sample.blechatservice.utils.AppSettings;
import dnay2k.com.w_hackathon_sample.blechatservice.utils.Constants;
import dnay2k.com.w_hackathon_sample.blechatservice.utils.Logs;
import dnay2k.com.w_hackathon_sample.blechatservice.utils.RecycleUtils;

/**
 * Created by dnay2 on 2016-07-20.
 */
public class MainActivity extends Activity {

    //Debugging
    private static final String TAG = "MainAtivity";

    //Context, System
    private Context mContext;
    private BTCTemplateService mService;
    private ActivityHandler mActivityHandler;

    //Global

    //UI stuff
    private ImageView mImageBT = null;
    private TextView mTextStatus = null;

    //BLE Chat
//    private TextView mTextChat;
//    private EditText mEditChat;
//    private Button mSendBtn;
//    private IFragmentListener mFragmentListener = null;

    private TextView temperText, humidityText, todayText;
    private TextView[] personText = new TextView[3];
    private ImageView[] btnImg = new ImageView[3];

    //Refresh timer
    private Timer mRefreshTimer = null;

    private static int originalTextColor = 0;
    private static final int grayTextColor = 0xff555555;
    private static Animation[] dizzy_lr  = new Animation[3];
    private static int nowPerson = -1;
    private static int cntTime=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this; // getApplicationContext();
        mActivityHandler = new ActivityHandler();
        AppSettings.initializeAppSettings(mContext);
        setContentView(R.layout.activity_main);

        for(int i =0 ; i<3;i++){
            dizzy_lr[i] = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.dizzy_lr);
            dizzy_lr[i].setRepeatMode(Animation.REVERSE);
            dizzy_lr[i].setRepeatCount(10);
        }


        //set up views
        mImageBT = (ImageView) findViewById(R.id.status_title);
        mImageBT.setImageDrawable(getDrawable(android.R.drawable.presence_invisible));
        mTextStatus = (TextView) findViewById(R.id.status_text);
        mTextStatus.setText(R.string.bt_state_init);

        //set up temperature and humidity
        todayText = (TextView) findViewById(R.id.todayText);
        temperText = (TextView) findViewById(R.id.temperText);
        humidityText = (TextView) findViewById(R.id.humuidityText);
        originalTextColor = temperText.getCurrentTextColor();

        String str = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(System.currentTimeMillis()));
        int month = Integer.parseInt(str.substring(4, 6));
        int day = Integer.parseInt(str.substring(6));
        todayText.setText(str.substring(0, 4) + "년 " + month + "월 " + day + "일");

        btnImg[0] = (ImageView) findViewById(R.id.childImg);
        btnImg[1] = (ImageView) findViewById(R.id.womanImg);
        btnImg[2] = (ImageView) findViewById(R.id.manImg);
        personText[0] = (TextView) findViewById(R.id.childText);
        personText[1] = (TextView) findViewById(R.id.womanText);
        personText[2] = (TextView) findViewById(R.id.manText);

//        for(int i=0; i<btnImg.length;i++){
//            Picasso.with(getApplicationContext()).load(imgaAddress[i]).fit().into(btnImg[i]);
//        }


        //set up Chat
//        mTextChat = (TextView) findViewById(R.id.chatText);
//        mTextChat.setVerticalScrollBarEnabled(true);
//        mTextChat.setMovementMethod(new ScrollingMovementMethod());
//
//        mEditChat = (EditText) findViewById(R.id.chatEdit);
//        mEditChat.setOnEditorActionListener(mWriteListener);

//        mSendBtn = (Button) findViewById(R.id.sendBtn);
//        mSendBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String message = mEditChat.getText().toString();
//                if(message != null && message.length()>0)
//                    sendMessage(message);
//
//            }
//        });


        doStartService();

    }

    public void onControlSize(View v) {
        if (nowPerson != v.getId()) {
            switch (v.getId()) {
                case R.id.chileBtn:
                    sendMessage("2");
                    btnImg[0].startAnimation(dizzy_lr[0]);
                    break;
                case R.id.womanBtn:
                    sendMessage("1");
                    btnImg[1].startAnimation(dizzy_lr[1]);
                    break;
                case R.id.manBtn:
                    sendMessage("3");
                    btnImg[2].startAnimation(dizzy_lr[2]);
                    break;
            }
            nowPerson = v.getId();
        }


    }

    /**
     * private method 채팅이 가능하도록 만드는 메소드
     * //여기부터 채팅용 메소드
     */

    // The action listener for the EditText widget, to listen for the return key
            //채팅 상황에서  Send 버튼을 눌렀을 때 발생하는 이벤트
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        if (message != null && message.length() > 0)
                            sendMessage(message);
                    }
                    return true;
                }
            };

    // Sends user message to remote
    // 디바이스로 데이터 전송
    private void sendMessage(String message) {
        if (message == null || message.length() < 1)
            return;
        Log.d("test", "message : " + message);
        CommunicateWithArduino(2, 0, 0, message, null, null);
        // send to remote

        // show on text view
//        if(mTextChat != null) {
//            mTextChat.append("\nSend: ");
//            mTextChat.append(message);
//            int scrollamout = mTextChat.getLayout().getLineTop(mTextChat.getLineCount()) - mTextChat.getHeight();
//            if (scrollamout > mTextChat.getHeight())
//                mTextChat.scrollTo(0, scrollamout);
//        }
//
//        mEditChat.setText("");
    }

    public void CommunicateWithArduino(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
        Log.d("test", "msgType is " + msgType);
        switch (msgType) {
            case 1:
                if (mService != null)
                    mService.startServiceMonitoring();
                break;
            case 2:
                if (mService != null && arg2 != null)
                    mService.sendMessageToRemote(arg2);
            default:
                break;
        }
    }

    private static final int NEW_LINE_INTERVAL = 1000;
    private long mLastReceivedTime = 0L;

    // Show messages from remote
    //디바이스로부터 전송받은 데이터를 보여주는 화면
    public void showMessage(String message) {
        if (message != null && message.length() > 0) {
            long current = System.currentTimeMillis();

//            if(current - mLastReceivedTime > NEW_LINE_INTERVAL) {
//                mTextChat.append("\nRcv: ");
//            }
//            mTextChat.append(message);
//            int scrollamout = mTextChat.getLayout().getLineTop(mTextChat.getLineCount()) - mTextChat.getHeight();
//            if (scrollamout > mTextChat.getHeight())
//                mTextChat.scrollTo(0, scrollamout);

            StringTokenizer st = new StringTokenizer(message, "\n", false);
            if (st.countTokens() == 3) {
                int temper = Integer.parseInt(st.nextToken().substring(0, 2));
                int hum = Integer.parseInt(st.nextToken().substring(0, 2));
                temperText.setText(temper + "도");
                humidityText.setText(hum + "%");
                initView();
                if (temper >= 30) temperText.setTextColor(0xfffc6376);

            } else {
                int a = Integer.parseInt(st.nextToken().substring(0, 1));

            }
            switch (Integer.parseInt(st.nextToken().substring(0, 1))) {
                case 0:
                    //어린이
                    btnImg[0].setColorFilter(0x00000000);
                    personText[0].setTextColor(originalTextColor);
                    nowPerson = R.id.chileBtn;
                    break;
                case 1:
                    //여자
                    btnImg[1].setColorFilter(0x00000000);
                    personText[1].setTextColor(originalTextColor);
                    nowPerson = R.id.womanBtn;
                    break;
                case 2:
                    //남자
                    btnImg[2].setColorFilter(0x00000000);
                    personText[2].setTextColor(originalTextColor);
                    nowPerson = R.id.manBtn;
                    break;
            }


            mLastReceivedTime = current;
        }
    }

    private void initView() {
        for (int i = 0; i < 3; i++) {
            btnImg[i].setColorFilter(0x55000000);
            personText[i].setTextColor(grayTextColor);
            temperText.setTextColor(originalTextColor);
        }
    }

    /**
     * ////여기까지 채팅용 메소드
     */


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        //Stop the timer
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        finalizeActivity();
    }

    public void onClickMethod(View v) {
        switch (v.getId()) {
            case R.id.action_scan:
                // Launch the DeviceListActivity to see devices and do scan
                doScan();
                break;
//            case R.id.action_discoverable:
//                // Ensure this device is discoverable by others
//                ensureDiscoverable();
//                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finalizeActivity();
    }

    long pressedTime = 2000;

    @Override
    public void onBackPressed() {
        if (pressedTime < System.currentTimeMillis() - 2000) {
            pressedTime = System.currentTimeMillis();
            Toast.makeText(MainActivity.this, "뒤로 한번 더 눌러 종료합니다.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
            finalizeActivity();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*****************************************************
     *	Private methods
     ******************************************************/

    /**
     * Service connection
     */
    private ServiceConnection mServiceConn = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "Activity - Service connected");

            mService = ((BTCTemplateService.ServiceBinder) binder).getService();

            // Activity couldn't work with mService until connections are made
            // So initialize parameters and settings here. Do not initialize while running onCreate()
            initialize();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    /**
     * Start service if it's not running
     */
    private void doStartService() {
        Log.d(TAG, "# Activity - doStartService()");
        startService(new Intent(this, BTCTemplateService.class));
        bindService(new Intent(this, BTCTemplateService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stop the service
     */
    private void doStopService() {
        Log.d(TAG, "# Activity - doStopService()");
        if (mService != null)
            mService.finalizeService();
        stopService(new Intent(this, BTCTemplateService.class));
    }

    /**
     * Initialization / Finalization
     */
    private void initialize() {
        Logs.d(TAG, "# Activity - initialize()");

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bt_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mService.setupService(mActivityHandler);

        // If BT is not on, request that it be enabled.
        // RetroWatchService.setupBT() will then be called during onActivityResult
        if (!mService.isBluetoothEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        }

        // Load activity reports and display
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
        }

        // Use below timer if you want scheduled job
        //mRefreshTimer = new Timer();
        //mRefreshTimer.schedule(new RefreshTimerTask(), 5*1000);
    }

    private void finalizeActivity() {
        Logs.d(TAG, "# Activity - finalizeActivity()");

        if (!AppSettings.getBgService()) {
            doStopService();
        } else {
        }

        // Clean used resources
        RecycleUtils.recursiveRecycle(getWindow().getDecorView());
        System.gc();
    }

    /**
     * Launch the DeviceListActivity to see devices and do scan
     */
    private void doScan() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
    }

    /**
     * Ensure this device is discoverable by others
     */
    private void ensureDiscoverable() {
        if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(intent);
        }
    }


    /*****************************************************
     *	Public classes
     ******************************************************/

    /**
     * Receives result from external activity
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logs.d(TAG, "onActivityResult requestCode : " + requestCode + " resultCode " + resultCode);

        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    Log.d(TAG, "the resultCode is -1");
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Attempt to connect to the device
                    if (address != null && mService != null) {
                        mService.connectDevice(address);
                        Log.d(TAG, "connectDevice(" + address + ")");
                    }

                }
                break;

            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a BT session
                    mService.setupBLE();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Logs.e(TAG, "BT is not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
        }    // End of switch(requestCode)
    }


    /*****************************************************
     * Handler, Callback, Sub-classes
     ******************************************************/

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // Receives BT state messages from service
                // and updates BT state UI
                case Constants.MESSAGE_BT_STATE_INITIALIZED:
                    mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " +
                            getResources().getString(R.string.bt_state_init));
                    mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
                    break;
                case Constants.MESSAGE_BT_STATE_LISTENING:
                    mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " +
                            getResources().getString(R.string.bt_state_wait));
                    mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
                    break;
                case Constants.MESSAGE_BT_STATE_CONNECTING:
                    mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " +
                            getResources().getString(R.string.bt_state_connect));
                    mImageBT.setImageDrawable(getDrawable(android.R.drawable.presence_away));
                    break;
                case Constants.MESSAGE_BT_STATE_CONNECTED:
                    if (mService != null) {
                        String deviceName = mService.getDeviceName();
                        if (deviceName != null) {
                            mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " +
                                    getResources().getString(R.string.bt_state_connected) + " " + deviceName);
                            mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_online));
                        } else {
                            mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " +
                                    getResources().getString(R.string.bt_state_connected) + " no name");
                            mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_online));
                        }
                    }
                    break;
                case Constants.MESSAGE_BT_STATE_ERROR:
                    mTextStatus.setText(getResources().getString(R.string.bt_state_error));
                    mImageBT.setImageDrawable(getDrawable(android.R.drawable.presence_busy));
                    break;

                // BT Command status
                case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
                    mTextStatus.setText(getResources().getString(R.string.bt_cmd_sending_error));
                    mImageBT.setImageDrawable(getDrawable(android.R.drawable.presence_busy));
                    break;

                ///////////////////////////////////////////////
                // When there's incoming packets on bluetooth
                // do the UI works like below
                ///////////////////////////////////////////////
                case Constants.MESSAGE_READ_CHAT_DATA:
                    if (msg.obj != null) {
                        showMessage((String) msg.obj);
                    }
                    break;
                default:
                    break;
            }

            super.handleMessage(msg);
        }
    }    // End of class ActivityHandler

    /**
     * Auto-refresh Timer
     */
    private class RefreshTimerTask extends TimerTask {
        public RefreshTimerTask() {
        }

        public void run() {
            mActivityHandler.post(new Runnable() {
                public void run() {
                    // TODO:
                    mRefreshTimer = null;
                }
            });
        }
    }


}
