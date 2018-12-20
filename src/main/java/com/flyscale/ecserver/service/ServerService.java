package com.flyscale.ecserver.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.flyscale.ecserver.MainActivity;
import com.flyscale.ecserver.Recorder;
import com.flyscale.ecserver.SmsHandler;
import com.flyscale.ecserver.bean.CallInfo;
import com.flyscale.ecserver.bean.DeviceInfo;
import com.flyscale.ecserver.bean.EventInfo;
import com.flyscale.ecserver.bean.SimInfo;
import com.flyscale.ecserver.bean.SmsInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.observer.SmsObserver;
import com.flyscale.ecserver.telephony.Call;
import com.flyscale.ecserver.telephony.TelephonyIntents;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.JsonUtil;
import com.flyscale.ecserver.util.PhoneUtil;
import com.flyscale.ecserver.util.PreferenceUtil;
import com.flyscale.ecserver.util.QueryCompeleteCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.List;


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
    private static String mActivNumber;
    private SmsObserver mSmsObserver;
    /* 自定义ACTION常数，作为广播的Intent Filter识别常数 */
    private static String SMS_SEND_ACTION = "SMS_SEND_ACTIOIN";
    private static String SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION";
    private SmsReceiver mSmsReceiver;
    private ServiceHandler mHandler = new ServiceHandler();
    private ContentResolver mResolver;

    @Override
    public void onCreate() {
        super.onCreate();
        DDLog.i(ServerService.class, "onCreate");

    }

    /**
     * 设置前台进程
     */
    private void startForegournd() {
        Notification notification = new Notification();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        //把该service创建为前台service
        startForeground(8888, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DDLog.i(ServerReceiver.class, "onStartCommand");
        //设置为前台进程，提高优先级
        startForegournd();
        mContext = getApplicationContext();
        mRecorder = Recorder.getInstance(this);
        //注册动态广播接收器
        registerReceivers();
        //监听电话状态
        mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new MyPhoneStateListener();
        mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        //usb对象
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //监听短信
        mResolver = getContentResolver();
        mSmsObserver = new SmsObserver(mResolver, new SmsHandler(this));
//        mResolver.registerContentObserver(Uri.parse(Constants.SMS_BASE_URI), true, mSmsObserver);


        //每次重启服务，需要重启开启线程，避免因为异常导致APP退出，线程仍然空跑的问题
        stopServerTherad(false);
        startServerThread();
        return super.onStartCommand(intent, flags, startId);
    }

    private class ServiceHandler extends Handler {
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
                    case ClientListenerThread.MSG_LISTENER_THREAD_DIED:
                        startServerThread();
                        break;
                }
            }
        }
    }

    /**
     * 处理客户端发来的指令
     *
     * @param msg
     * @throws JSONException
     */
    private void handleCmdFromClient(Message msg) throws JSONException {
        String cmdStr = (String) msg.obj;
        DDLog.i(ServerService.class, "cmdStr=" + cmdStr);
        if (JsonUtil.isJson(cmdStr, 0)) {
            JSONObject cmdObj = (JSONObject) new JSONTokener(cmdStr).nextValue();
            String eventType = cmdObj.getString(Constants.CMD_EVENT_TYPE);
            DDLog.i(ServerService.class, "EventType=" + eventType);
            EventInfo eventInfo = null;
            switch (eventType) {
                case Constants.EVENT_TYPE_DIALER:   //拨号
                    String number = cmdObj.getString(Constants.CMD_CALL_NUMBER);
                    PhoneUtil.call(mContext, number);
                    break;
                case Constants.EVENT_TYPE_ENDCALL:  //挂断
                    PhoneUtil.endCall(mContext);
                    break;
                case Constants.EVENT_TYPE_ANSWERCALL:   //接听
                    if (PhoneUtil.isRinging(mContext)) {
                        PhoneUtil.answerCall(mContext);
                    } else {
                        DDLog.i(ServerService.class, "there is no incoming call");
                    }
                    break;
                case Constants.EVENT_TYPE_GETDEVICEINFO:    //获取设备信息
                    DeviceInfo deviceInfo = PhoneUtil.getDeviceInfo(mContext);
                    DDLog.i(ServerService.class, "deviceInfo Json=" + deviceInfo.toJson());
                    mServerThread.sendMsg2Client(deviceInfo.toJson());
                    break;
                case Constants.EVENT_TYPE_RECODERINFO:  //设置录音时间类型
                    //TODO
                    break;
                case Constants.EVENT_TYPE_SUFFIXNUMBER:  //拨打分机号
                    String subPhone = cmdObj.getString(Constants.CMD_EVENT_VALUE);
                    Intent dtmfBro = new Intent(Constants.PLAY_DTMF_INTENT);
                    dtmfBro.putExtra(Constants.DTMF_STR, subPhone);
                    sendBroadcast(dtmfBro);
                    break;
                case Constants.EVENT_TYPE_CALLSTATE:    //获取电话状态
                    CallInfo callInfo = PhoneUtil.getCallStateInfo(mContext);
                    mServerThread.sendMsg2Client(callInfo.toJson());
                    break;
                case Constants.EVENT_TYPE_KEYF3:    //电话手柄状态
                    boolean hookState = PhoneUtil.getHookState();
                    eventInfo = new EventInfo(Constants.EVENT_TYPE_KEYF3, String.valueOf(hookState));
                    mServerThread.sendMsg2Client(eventInfo.toJson());
                    break;
                case Constants.EVENT_TYPE_KEYCALL:   //免提状态
                    boolean handfree = PhoneUtil.getHandfree(mContext);
                    eventInfo = new EventInfo(Constants.EVENT_TYPE_KEYCALL, String.valueOf(handfree));
                    mServerThread.sendMsg2Client(eventInfo.toJson());
                    break;
                case Constants.EVENT_TYPE_SIMCARD:  //SIM卡类型
                    String plmnNumeric = PreferenceUtil.getString(mContext, Constants.SP_PLMN_NUMBER, "");
                    String operator = Constants.OPERATOR_MAP.get(plmnNumeric);
                    String simcardState = PhoneUtil.getSimcardState(mContext);
                    SimInfo simInfo = new SimInfo();
                    simInfo.SimState = simcardState;
                    simInfo.SimTypeName = operator;
                    simInfo.EventType = Constants.EVENT_TYPE_SIMCARD;
                    mServerThread.sendMsg2Client(simInfo.toJson());
                    break;
                case Constants.EVENT_TYPE_OPENSPEAKERON:   //开启免提
                    PhoneUtil.setHandfree(mContext);
                    break;
                case Constants.EVENT_TYPE_QUERYSMS: //查询系统所有短信
                    PhoneUtil.readInboxOutBoxMsg(mContext, new QueryCompeleteCallback() {
                        @Override
                        public void onQuerySuccess(String result) {
                            DDLog.i(QueryCompeleteCallback.class, "onQuerySuccess(),result=" + result);
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put(Constants.CMD_EVENT_TYPE, Constants.EVENT_TYPE_QUERYSMS);
                                jsonObject.put(Constants.CMD_EVENT_VALUE, result);
                                mServerThread.sendMsg2Client(jsonObject.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onQueryFailure(Exception e) {

                        }
                    });
                    break;
                case Constants.EVENT_TYPE_SENDSMS:  //发送短信
                    sendMsg(cmdObj.getString(Constants.CMD_EVENT_VALUE));
                    break;
                case Constants.EVENT_TYPE_INSTALLAPP:   //更新service app
                    //TODO
                    break;
                case Constants.EVENT_TYPE_HIDEDIALNUMBER:   //隐藏所有显示号码
                    //TODO
                    break;
            }
        } else {
            DDLog.i(ServerService.class, "not a valid json string!");
        }
    }

    private static void sendMsg(String cmdValue) {

        DDLog.i(ServerService.class, "sendMsg");
        String[] valueArr = cmdValue.split(";");
        if (!TextUtils.isEmpty(valueArr[0])) {
            String[] numbers = valueArr[0].split(",");
            for (int i = 0; i < numbers.length; i++) {
                DDLog.i(ServerService.class, "send address=" + numbers[i] + ",content=" + valueArr[1]);
                sendSMS(numbers[i], valueArr[1]);
            }
        }
    }


    /**
     * 发送短信
     *
     * @param phoneNumber
     * @param message
     */
    private static void sendSMS(String phoneNumber, String message) {
        DDLog.i(ServerService.class, "phoneNumber=" + phoneNumber + ",message=" + message);
        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(message)) {
            DDLog.i(ServerService.class, "phoneNumber or message should not be empty");
            mServerThread.sendMsg2Client(new EventInfo("-1").toJson());
            return;
        }
        // 建立自定义Action常数的Intent(给PendingIntent参数之用)
        Intent sendIntent = new Intent(SMS_SEND_ACTION);
        Intent deliverIntent = new Intent(SMS_DELIVERED_ACTION);
        // sentIntent参数为传送后接受的广播信息PendingIntent
        PendingIntent mSendPI = PendingIntent.getBroadcast(mContext, 0, sendIntent, 0);
        //deliveryIntent参数为送达后接受的广播信息PendingIntent
        PendingIntent mDeliverPI = PendingIntent.getBroadcast(mContext, 0, deliverIntent, 0);

        SmsManager smsManager = SmsManager.getDefault();
        // 拆分短信内容（手机短信长度限制）
        List<String> divideContents = smsManager.divideMessage(message);
        for (String text : divideContents) {
            DDLog.d(ServerService.class, "send message, text=" + text);
            smsManager.sendTextMessage(phoneNumber, null, text, mSendPI, mDeliverPI);
        }

    }

    /**
     * 发送短信广播接收器
     */
    public class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DDLog.d(ServerService.class, "onReceive,action=" + intent.getAction());
            if (intent.getAction().equals(SMS_SEND_ACTION)) {
                /* android.content.BroadcastReceiver.getResultCode()方法 */
                DDLog.d(ServerService.class, "resultcode=" + getResultCode());
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_SENDSMS, Constants.SEND_MSG_SUCCESS);
                        mServerThread.sendMsg2Client(eventInfo.toJson());
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        break;
                    default:

                        break;
                }
            } else if (intent.getAction().equals(SMS_DELIVERED_ACTION)) {
                try {
                    /* android.content.BroadcastReceiver.getResultCode()方法 */
                    DDLog.d(ServerService.class, "SMS_DELIVERED_ACTION,resultcode=" + getResultCode());
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            break;
                    }
                } catch (Exception e) {
                    e.getStackTrace();
                }
            }
        }
    }

    /**
     * 其它广播接收器
     */
    private class ServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DDLog.i(ServerReceiver.class, "action=" + intent.getAction());
            Toast.makeText(mContext, intent.getAction(), Toast.LENGTH_SHORT).show();
            if (TextUtils.equals(intent.getAction(), Constants.USB_STATE_INTENT)) {
                boolean connected = intent.getExtras().getBoolean("connected");
                DDLog.i(ServerReceiver.class, "USB connected=" + connected);
                if (connected)
                    startServerThread();
                else
                    stopServerTherad(false);
            } else if (TextUtils.equals(intent.getAction(), Constants.FLYSCALE_PHONE_STATE_INTENT)) {
                int stateFly = intent.getIntExtra("phone_state", Call.State.INVALID);
                mActivNumber = intent.getStringExtra("phone_number");
                mCurrentState = stateFly;
                String stateFlyStr = Call.STATE_MAP.get(stateFly);
                DDLog.i(ServerReceiver.class, "stateFly=" + stateFlyStr);
                /*如果新的电话为INCOMING或者是DIALING就生成一次通话ID*/
                PhoneUtil.generateCallId(mContext, mActivNumber);
                DDLog.i(ServerReceiver.class, "mOldState=" + Call.STATE_MAP.get(mOldState) + ",mCurrentState=" + Call.STATE_MAP.get(mCurrentState));
                if (mOldState == Call.State.INCOMING) {
                    if (mCurrentState == Call.State.ACTIVE) {
                        mRecorder.start(mActivNumber);
                    }
                } else if (mOldState == Call.State.ACTIVE) {
                    if (mCurrentState == Call.State.DISCONNECTED) {
                        mRecorder.stop();
                    }
                }

                if (mCurrentState == Call.State.DIALING) {
                    mRecorder.start(mActivNumber);
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
                        mRecorder.start(mActivNumber);
                    } else if (mCurrentState == Call.State.DISCONNECTED) {
                        mRecorder.stop();
                    }
                }
                mOldState = mCurrentState;
            } else if (TextUtils.equals(intent.getAction(), Constants.SMS_DELIVER_INTENT) ||
                    TextUtils.equals(intent.getAction(), Constants.SMS_RECEIVED_INTENT)) {
                getMsgFromIntent(intent.getExtras());
            } else if (TextUtils.equals(intent.getAction(), TelephonyIntents.ACTION_PLMN_INTENT)) {
                String plmnLong = intent.getStringExtra(TelephonyIntents.PLMN_LONG);
                String plmnShort = intent.getStringExtra(TelephonyIntents.PLMN_SHORT);
                String plmnNumeric = intent.getStringExtra(TelephonyIntents.PLMN_NUMERIC);
                DDLog.i(ServerReceiver.class, "plmnLong=" + plmnLong + ",plmnShort=" + plmnShort + ",plmnNumeric=" + plmnNumeric);
                PreferenceUtil.put(mContext, Constants.SP_PLMN_NUMBER, plmnNumeric);
            }
        }


    }

    /**
     * 解析短信内容
     *
     * @param bundle
     */
    private void getMsgFromIntent(Bundle bundle) {
        DDLog.i(ServerService.class, "getMsgFromIntent");
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null && pdus.length > 0) {
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    byte[] pdu = (byte[]) pdus[i];
                    messages[i] = SmsMessage.createFromPdu(pdu);
                }
                int i = 0;
                for (SmsMessage message : messages) {
                    String content = message.getMessageBody();// 得到短信内容
                    String sender = message.getOriginatingAddress();// 得到发信息的号码
                    long timestampMillis = message.getTimestampMillis();
                    SmsInfo smsInfo = new SmsInfo();
                    smsInfo.Sms = content;
                    smsInfo.PhoneNumber = sender;
                    smsInfo.Id = i + "";
                    smsInfo.Time = timestampMillis + "";
                    smsInfo.Type = SmsInfo.TYPE_RECEIVE;
                    i++;
                    mServerThread.sendMsg2Client(smsInfo.toJson());
                    DDLog.i(ServerService.class, "i=" + i + ",receive msg address=" + sender + ",content=" + content
                            + ",timestampMillis=" + timestampMillis);
                }
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

    private void stopServerTherad(boolean restart) {
        DDLog.i(ServerService.class, "stopServerTherad");
        if (mServerThread != null) {
            if (mServerThread.isAlive()) {
                mServerThread.setRestart(restart);
                mServerThread.setLoop(false);
                mServerThread.closeSocket();
                mServerThread.interrupt();
            }
        }
    }

    private void startServerThread() {
        DDLog.i(ServerService.class, "startServerThread");
        if (mServerThread == null) {
//            mServerThread = ClientListenerThread.getInstance(mHandler);
            mServerThread = new ClientListenerThread();
            mServerThread.addHandler(mHandler);
            mServerThread.start();
        } else {
            if (mServerThread.isAlive()) {
                DDLog.w(ServerService.class, "ClientListenerThread is running,only one instance is permitted");
            } else {
                mServerThread = new ClientListenerThread();
                mServerThread.addHandler(mHandler);
                mServerThread.start();
            }
        }

    }

    private void registerReceivers() {
        DDLog.i(ServerService.class, "registerReceivers");
        mServerReceiver = new ServerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.USB_STATE_INTENT);
        //平台自定义广播
        intentFilter.addAction(Constants.FLYSCALE_PHONE_STATE_INTENT);
        intentFilter.addAction(TelephonyIntents.ACTION_PLMN_INTENT);

        intentFilter.addAction(Constants.SMS_DELIVER_INTENT);
        intentFilter.addAction(Constants.SMS_RECEIVED_INTENT);
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        registerReceiver(mServerReceiver, intentFilter);

        mSmsReceiver = new SmsReceiver();
        IntentFilter smsIntentFilter = new IntentFilter();
        smsIntentFilter.addAction(SMS_DELIVERED_ACTION);
        smsIntentFilter.addAction(SMS_SEND_ACTION);
        registerReceiver(mSmsReceiver, smsIntentFilter);
    }

    @Override
    public void onDestroy() {
        DDLog.i(ServerService.class, "onDestroy()");
        super.onDestroy();
        stopServerTherad(false);
        mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(mServerReceiver);
        unregisterReceiver(mSmsReceiver);
        mResolver.unregisterContentObserver(mSmsObserver);
        getContentResolver().unregisterContentObserver(mSmsObserver);
        stopForeground(true);
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

    /**
     * 提供对外的静态方法访问当前非IDLE状态下的电话号码
     *
     * @return
     */
    public static String getActivNumber() {
        return mActivNumber;
    }
}
