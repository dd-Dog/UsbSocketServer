package com.flyscale.ecserver.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.flyscale.ecserver.ClientListenerThread;
import com.flyscale.ecserver.Recorder;
import com.flyscale.ecserver.bean.DeviceInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.telephony.Call;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.PhoneUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by bian on 2018/12/10.
 */

public class ServerService extends Service {

    private ServerReceiver mServerReceiver;
    private static ClientListenerThread mServerThread;
    private ServerBinder mServerBinder;
    private UsbManager mUsbManager;
    private TelephonyManager mTm;
    private MyPhoneStateListener mPhoneStateListener;
    private static Context mContext;
    private Recorder mRecorder;
    private int mCurrentState;
    private int mOldState = Call.State.DISCONNECTED;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        DDLog.i(ServerService.class, "onCreate");
        registerReceivers();
        mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new MyPhoneStateListener();
        mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DDLog.i(ServerReceiver.class, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("HandlerLeak")
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                DDLog.i(ServerService.class, "handleMessage,what=" + msg.what);
                switch (msg.what) {
                    case ClientListenerThread.MSG_FROM_CLIENT:
                        try {
                            handleCmdFromClient(msg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    };

    private static void handleCmdFromClient(Message msg) throws JSONException {
        String cmdStr = (String) msg.obj;
        DDLog.i(ServerService.class, "cmdStr=" + cmdStr);
        JSONObject cmdObj = (JSONObject) new JSONTokener(cmdStr).nextValue();
        String eventType = cmdObj.getString(Constants.CMD_EVENT_TYPE);
        DDLog.i(ServerService.class, "EventType=" + eventType);
        switch (eventType) {
            case Constants.EVENT_TYPE_DIALER:
                String number = cmdObj.getString(Constants.CMD_CALL_NUMBER);
                PhoneUtil.call(mContext, number);
                break;
            case Constants.EVENT_TYPE_ENDCALL:
                PhoneUtil.endCall(mContext);
                break;
            case Constants.EVENT_TYPE_ANSWERCALL:
                if (PhoneUtil.isRinging(mContext)) {
                    PhoneUtil.answerCall(mContext);
                } else {
                }
                break;
            case Constants.EVENT_TYPE_GETDEVICEINFO:
                DeviceInfo deviceInfo = PhoneUtil.getDeviceInfo(mContext);
                DDLog.i(ServerService.class, "deviceInfo Json=" + deviceInfo.toJson());
                mServerThread.sendMsg2Client(deviceInfo.toJson());
                break;
        }
    }

    private class ServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DDLog.i(ServerReceiver.class, "action=" + intent.getAction());
            if (TextUtils.equals(intent.getAction(), "android.hardware.usb.action.USB_STATE")) {
                boolean connected = intent.getExtras().getBoolean("connected");
                DDLog.i(ServerReceiver.class, "USB connected=" + connected);
                if (connected)
                    startServerThread();
                else
                    stopServerTherad();
            } else if (TextUtils.equals(intent.getAction(), "com.android.phone.FLYSCALE_PHONE_STATE")) {
                int stateFly = intent.getIntExtra("phone_state", Call.State.INVALID);
                String number = intent.getStringExtra("phone_number");
                mCurrentState = stateFly;
                String stateFlyStr = Call.STATE_MAP.get(stateFly);
                DDLog.i(ServerReceiver.class, "stateFly=" + stateFlyStr);

                DDLog.i(ServerReceiver.class, "mOldState=" + Call.STATE_MAP.get(mOldState) + ",mCurrentState=" + Call.STATE_MAP.get(mCurrentState));
                if (mOldState == Call.State.INCOMING) {
                    if (mCurrentState == Call.State.ACTIVE) {
                        mRecorder.start(number);
                    }
                } else if (mOldState == Call.State.ACTIVE) {
                    if (mCurrentState == Call.State.DISCONNECTED) {
                        mRecorder.stop();
                    }
                }

                if (mCurrentState == Call.State.DIALING) {
                    mRecorder.start(number);
                }
                if (mOldState == Call.State.DIALING) {
                    if (mCurrentState == Call.State.ACTIVE) {
                        mRecorder.stop();
                        //删除接通之前的录音文件
                        File file = new File(mRecorder.mFileName);
                        DDLog.i(ServerReceiver.class, "remove file=" + mRecorder.mFileName);
                        if (file.exists()) {
                            file.delete();
                        }
                        while (!mRecorder.getState().isIdle()) ;
                        DDLog.i(ServerReceiver.class, "start to recording");
                        mRecorder.start(number);
                    } else if (mCurrentState == Call.State.DISCONNECTED) {
                        mRecorder.stop();
                    }
                }
                mOldState = mCurrentState;
            }
        }
    }

    public class ServerBinder extends Binder {
        public ServerService getService() {
            return ServerService.this;
        }

    }

    public void test() {
        DDLog.i(ServerBinder.class, "hello, this is ServerBinder");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        DDLog.i(ServerService.class, "onBind");
        mServerBinder = new ServerBinder();
        return mServerBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DDLog.i(ServerService.class, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        DDLog.i(ServerService.class, "onRebind");
        super.onRebind(intent);
    }

    private void stopServerTherad() {
        DDLog.i(ServerService.class, "stopServerTherad");
        if (mServerThread != null) {
            if (mServerThread.isAlive()) {
                mServerThread.interrupt();
            }
        }
    }

    private void startServerThread() {
        DDLog.i(ServerService.class, "startServerThread");
        if (mServerThread == null || !mServerThread.isAlive()) {
            mServerThread = new ClientListenerThread(ClientListenerThread.class.getSimpleName(), mHandler, true);
            mServerThread.start();
        } else {
            DDLog.w(ServerService.class, "ClientListenerThread is running,only one instance is permitted");
        }

    }

    private void registerReceivers() {
        DDLog.i(ServerService.class, "registerReceivers");
        mServerReceiver = new ServerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        intentFilter.addAction("com.android.phone.FLYSCALE_PHONE_STATE");
        registerReceiver(mServerReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        DDLog.i(ServerService.class, "onDestroy");
        super.onDestroy();
        stopServerTherad();
        mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            DDLog.i(MyPhoneStateListener.class, "onCallStateChanged");
            if (mServerThread != null) {
                mServerThread.sendMsg2Client("phone state changed...");
            }
        }
    }
}
