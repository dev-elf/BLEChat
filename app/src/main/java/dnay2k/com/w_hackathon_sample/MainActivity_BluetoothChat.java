package dnay2k.com.w_hackathon_sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dnay2k.com.w_hackathon_sample.bluetoothservice.BluetoothService;

/**
 * Created by dnay2 on 2016-07-22.
 */
public class MainActivity_BluetoothChat extends Activity {
    private static final String TAG = "Main";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private Button btn_Connect;
    private TextView txt_Result;

    private BluetoothService btService = null;

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onCreate");

        setContentView(R.layout.main);

//        btn_Connect = (Button) findViewById(R.id.btn_connect);
//        txt_Result = (TextView) findViewById(R.id.txt_Result);

        btn_Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btService.getDeviceState()){
                    btService.enableBluetooth();
                } else {
                    finish();
                }
            }
        });

        //BluetoothService
        if(btService == null){
            btService = new BluetoothService(this, mHandler);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult " + resultCode);

        switch (requestCode){
            case REQUEST_CONNECT_DEVICE:
                //When the request to enable Bluetooth returns
                if(resultCode == Activity.RESULT_OK){
                    //Next Step
//                    btService.scanDevice();
                    btService.getDeviceInfo(data);
                }
                break;

            case REQUEST_ENABLE_BT:
                //When the request to enable Bluetooth returns
                if(resultCode == Activity.RESULT_OK){
                    //확인 눌렀을 때
                    //Next Step
                    btService.scanDevice();
                } else {
                    //취소 눌렀을 때때
                    Log.d(TAG, "Bluetooth is not enabled");
                }
                break;
        }
//        super.onActivityResult(requestCode, resultCode, data);
    }
}
