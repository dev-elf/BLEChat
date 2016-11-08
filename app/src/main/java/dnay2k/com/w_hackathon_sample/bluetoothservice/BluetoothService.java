package dnay2k.com.w_hackathon_sample.bluetoothservice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import dnay2k.com.w_hackathon_sample.DeviceListActivity;

/**
 * Created by dnay2 on 2016-07-20.
 * 블루투스2.0버젼을 사용할때 사용되는 메소드들이 들어있는 곳입니다.
 */
public class BluetoothService {
    //Debugging

    private static final String TAG = "BluetoothService";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private int mState;

    private static final int STATE_NONE = 0;        //we're doing nothing
    private static final int STATE_LISTEN = 1;      //now listening for incoming connections
    private static final int STATE_CONNECTING = 2;  //now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3;   //now connected to a remote device


    //REFCOMM Protocol
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;

    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;

    private Activity mActivity;
    private Handler mHandler;

    //Constructors

    public BluetoothService(Activity ac, Handler h){
        mActivity = ac;
        mHandler = h;

        //BluetoothAdapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }



    public boolean getDeviceState(){
        Log.d(TAG, "Check the Bluetooth support");

        if(btAdapter == null){
            Log.d(TAG, "Bluetooth is not available");

            return false;
        } else {
            Log.d(TAG, "Bluetooth is available");

            return true;
        }
    }

    public void enableBluetooth(){
        Log.i(TAG, "Check the enabled Bluetooth");

        if(btAdapter.isEnabled()){
            Log.d(TAG, "Bluetooth Enable Now");
            scanDevice();
        } else {
            Log.d(TAG, "Bluetooth Enable Request");

            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);
        }
    }

    public void scanDevice(){
        Log.d(TAG, "Scan Device");

        Intent serverIntent = new Intent(mActivity, DeviceListActivity.class);
        mActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

    }

    public void getDeviceInfo(Intent data){
        //Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        Log.d(TAG,"Get Device Info \n" + "address : " + address);

        connect(device);
    }

    /**
     *
     * 기기를 연결을 하기 위해 반복되는 쓰레드
     *
     */

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            mmDevice = device;
            BluetoothSocket tmp = null;

            //디바이스 정보를 얻어서 BluetoothSocket 생성
            try{
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (IOException e){
                Log.e(TAG, "create() failed", e);
                e.printStackTrace();
            }

            mmSocket = tmp;
        }

        public void run(){
            Log.i(TAG, "Begin mConnectThread");
            setName("ConnectThread");

            //연결을 시도하기 전에는 항상 기기 검색을 중지한다.
            //기기 검색이 계속되면 연결속도가 느려지기 때문이다.
            btAdapter.cancelDiscovery();

            //BluetoothSocket 연결 시도
            try{
                //BluetoothSocket 연결시도에 대한 return 값은 success 또는 exception이다.
                mmSocket.connect();
                Log.d(TAG, "Connect Success");
            } catch (IOException e){
                e.printStackTrace();
                connectionFailed();     //연결 실패시 불러오는 메소드
                Log.d(TAG, "Connect Fail");

                //Socket을 닫는다.
                try{
                    mmSocket.close();
                }catch (IOException e2){
                    Log.e(TAG, "Unable to close() socket during connection failure", e2);
                }
                //연결중? 혹은 연결 대기상태인 메소드를 호출하다.
                BluetoothService.this.start();
                return;
            }

            //ConnectTHread 클래스를 reset한다.
            synchronized (BluetoothService.this){
                mConnectThread = null;
            }

            //ConnectThread 시작한다.
            connected(mmSocket, mmDevice);
        }

        public void cancel(){
            try{
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * 연결되었을 때 작동하는 쓰레드
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private final OutputStream mmOutputStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //BluetoothSocket의 inputStream과 outputStream
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e){
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInputStream = tmpIn;
            mmOutputStream = tmpOut;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            //Keep listening to the InputStream While connected
            while(true){
                try{
                    //InputStream으로부터 값을 받는 읽는 부분(값을 받는다.)
                    bytes = mmInputStream.read(buffer);
                    Log.d("bluetooth","inputStream : "+bytes);

                } catch (IOException e){
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer The bytes to write
         */

        public void write(byte[] buffer){
            try{
                //값을 쓰는 부분(데이터 송신)
                mmOutputStream.write(buffer);
            } catch(IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    //Bluetooth 상태 set
    private synchronized void setState(int state){
        Log.d(TAG, "setState() " + mState + " -> "+ state);
        mState = state;
    }

    //Bluetooth 상태 get
    private synchronized int getState(){
        return mState;
    }

    public synchronized void start(){
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if(mConnectThread == null){

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any thread currently running a connection
        if(mConnectedThread == null){

        }else{
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    //ConnectThread 초기화 device의 모든 연결 제거
    public synchronized void connect(BluetoothDevice device){
        Log.d(TAG, "connect to :"+ device);

        //Cancel any thread attempting to make a connection
        if(mState == STATE_CONNECTING){
            if(mConnectThread == null){

            }else {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        //Cancel any thread currently running a connection
        if(mConnectedThread == null){

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);

        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    //ConnectedThread 초기화
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        Log.d(TAG, "connected");

        //cancel the thread that completed the connection
        if(mConnectThread == null){

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any thread currently running a connection
        if(mConnectedThread == null){

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    //모든 thread
    public synchronized void stop(){
        Log.d(TAG, "stop");

        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] out){//Create temporary object
        ConnectedThread r; //Synchronize a copy of the ConnectedThread
        synchronized (this){
            if(mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        } //Perform the write unsynchronized r.write(out);
        Log.d("bluetooth","write : " + out);
        r.write(out);
    }

    //연결실패시
    private void connectionFailed(){
        setState(STATE_LISTEN);
    }

    //연결이 끊겼을  때;
    private void connectionLost(){
        setState(STATE_LISTEN);
    }
}
