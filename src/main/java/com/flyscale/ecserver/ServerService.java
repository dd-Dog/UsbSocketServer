package com.flyscale.ecserver;

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

import com.flyscale.ecserver.util.DDLog;

/**
 * Created by bian on 2018/12/10.
 */

public class ServerService extends Service {

    private USBReceiver mServerReceiver;
    private ClientListenerThread mServerThread;
    private ServerBinder mServerBinder;
    private UsbManager mUsbManager;
    private TelephonyManager mTm;
    private MyPhoneStateListener mPhoneStateListener;


    @Override
    public void onCreate() {
        super.onCreate();
        DDLog.i(ServerService.class, "onCreate");
        registerReceivers();
        mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new MyPhoneStateListener();
        mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DDLog.i(USBReceiver.class, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                DDLog.i(ServerService.class, "handleMessage,what=" + msg.what);
                switch (msg.what) {
                    case ClientListenerThread.MSG_FROM_CLIENT:
                        String cmdStr = (String) msg.obj;
                        DDLog.i(ServerService.class, "cmdStr=" + cmdStr);
                        break;
                }
            }
        }
    };

    private class USBReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DDLog.i(USBReceiver.class, "action=" + intent.getAction());
            if (TextUtils.equals(intent.getAction(), UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
            } else if (TextUtils.equals(intent.getAction(), UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
            } else if (TextUtils.equals(intent.getAction(), "android.hardware.usb.action.USB_STATE")) {
                boolean connected = intent.getExtras().getBoolean("connected");
                DDLog.i(USBReceiver.class, "USB connected=" + connected);
                if (connected)
                    startServerThread();
                else
                    stopServerTherad();
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
        mServerReceiver = new USBReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
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
            if (mServerThread != null){
                mServerThread.sendMsg2Client("phone state changed...");
            }
        }
    }
}
