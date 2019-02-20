package com.flyscale.ecserver.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.flyscale.ecapp.IDataInfo;
import com.flyscale.ecserver.IListenService;
import com.flyscale.ecserver.MainActivity;
import com.flyscale.ecserver.recorder.AudioRecorder;
import com.flyscale.ecserver.recorder.Recorder;
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
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Time;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by bian on 2018/12/10.
 */

public class ServerService extends Service {

    //因为硬件原因导致每次HOOK状态重复上报两次，这里要过滤，500ms之内只允许发送一 次
    private static final int MSG_FILTER = 3001;
    private boolean mHookFilter = true; //是否可以向EC客户端发送消息
    private static final int MSG_FILER_DELAYED = 500;

    private ServerReceiver mServerReceiver;
    private static ClientListenerThread mServerThread;
    private ServerBinder mServerBinder;
    private UsbManager mUsbManager;
    private TelephonyManager mTm;
    private MyPhoneStateListener mPhoneStateListener;
    private static Context mContext;
    private AudioRecorder mRecorder;
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
    public static long mConnectTime;
    public static String mCallId = "-1";
    private static String mAddress;

    //保活时间间隔,超过该时间没有收到消息则断开，重新连接
    public static final int KEEP_ALIVE_INTERVAL = 40 * 1000;
    public static final int MSG_KEEP_ALIVE = 3003;
    private static final int MSG_CLIENT_TIMEOUT = 3004;
    private ServerSocket mPCMServerSocket;
    private IDataInfo mIDataInfo;


    public static String getAddress() {
        return mAddress;
    }

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
        try {
            mPCMServerSocket = new ServerSocket(Constants.LOCAL_PORT_STREAM);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder = AudioRecorder.getInstance(this, mPCMServerSocket, mIDataInfo);
        mRecorder.init();
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

        startPCMSender();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 开启线程，建立发送音频数据流的socket连接
     */
    private void startPCMSender() {

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
                    case MSG_FILTER:
                        mHookFilter = true;//设置为true
                        break;
                    case MSG_KEEP_ALIVE:
                        removeMessages(MSG_CLIENT_TIMEOUT);
                        sendEmptyMessageDelayed(MSG_CLIENT_TIMEOUT, KEEP_ALIVE_INTERVAL);
                        DDLog.i(ServerService.class, "reset client timeout 40s!");
                        break;
                    case MSG_CLIENT_TIMEOUT:
//                        stopServerTherad(true);
//                        DDLog.i(ServerService.class, "client timeout,stop and restart listener!");
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
                    notifyPhoneState(mCurrentState, true);
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
                            mServerThread.sendMsg2Client(result);
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
//                    String apkPath = cmdObj.getString(Constants.CMD_EVENT_VALUE);
                    Intent updateIntent = new Intent(Constants.ACTION_UPDATE_APP);
                    updateIntent.putExtra("path", "/storage/emulated/legacy/XRPCAndroidService.apk");
                    updateIntent.putExtra("pkgname", getPackageName());
                    DDLog.i(ServerService.class, "pkgname=" + getPackageName());
                    sendBroadcast(updateIntent);
                    break;
                case Constants.EVENT_TYPE_HIDEDIALNUMBER:   //隐藏所有显示号码
                    String hideStr = cmdObj.getString(Constants.CMD_EVENT_VALUE);
                    boolean hide = false;
                    if (hideStr != null) {
                        if (hideStr.equals(Constants.HIDE_NUMBER_ENABLED)) {
                            hide = true;
                        } else if (hideStr.equals(Constants.HIDE_NUMBER_UNABLED)) {
                            hide = false;
                        }
                        Intent hideNumber = new Intent(Constants.ACTION_HIDE_NUMBER);
                        hideNumber.putExtra(Constants.HIDE_NUMBER, hide);
                        sendBroadcast(hideNumber);
                    }

                    Settings.Global.putInt(getContentResolver(), Constants.HIDE_NUMBER, hide ? 1 : 0);
                    int anInt = Settings.Global.getInt(getContentResolver(), Constants.HIDE_NUMBER, 0);
                    DDLog.i(ServerService.class, "hide_number=" + anInt);
                    break;
                case Constants.EVENT_TYPE_PLAY2CALL:
                case Constants.EVENT_TYPE_AUTO_REPLY_AUDIO:
                    if (PhoneUtil.isOffhook(this)) {
                        String audioPath = cmdObj.getString(Constants.CMD_EVENT_VALUE);
//                        String test = "/storage/emulated/legacy/shuaicongge.mp3";
                        Intent playSound = new Intent(Constants.ACTION_PLAY_SOUND_2MIC);
                        playSound.putExtra("action", true);
                        playSound.putExtra("audio_path", audioPath);
                        sendBroadcast(playSound);
                        DDLog.i(ServerService.class, "send broadcast to play sound 2 microphone");
                    }
                    break;
                case Constants.EVENT_TYPE_OUTTIME_GETCALL:
                    //TODO 超时自动接听，机器人回复
                    break;
                case Constants.EVENT_TYPE_CHANGE_MANUAL_WORK:

                    break;
                case Constants.EVENT_TYPE_STOP_PLAY2CALL:
                    DDLog.i(ServerService.class, "EVENT_TYPE_STOP_PLAY2CALL");
                    Intent playSound = new Intent(Constants.ACTION_PLAY_SOUND_2MIC);
                    playSound.putExtra("action", false);
                    sendBroadcast(playSound);
                    break;
                case Constants.EVENT_TYPE_SEND_FILE:
                    String filePath = cmdObj.getString(Constants.CMD_EVENT_VALUE);
                    mServerThread.sendFile(filePath);
                    DDLog.i(ServerService.class, "start download server");
                    EventInfo ok = new EventInfo(Constants.EVENT_TYPE_SEND_FILE, "ok");
                    mServerThread.sendMsg2Client(ok.toJson());
                    break;
                case Constants.EVENT_TYPE_GET_APPINFO:
                    PackageManager pm = getPackageManager();
                    com.flyscale.ecserver.bean.PackageInfo packageBean = new com.flyscale.ecserver.bean.PackageInfo();
                    try {
                        PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
                        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                        packageBean.dataDir = applicationInfo.dataDir;
                        packageBean.processName = applicationInfo.processName;
                        packageBean.versionName = packageInfo.versionName;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        mServerThread.sendMsg2Client(packageBean.toJson());
                        DDLog.i(ServerService.class, "packageBean=" + packageBean.toJson());
                    }
                    break;
                default:
                    mServerThread.sendMsg2Client(Constants.ACK);
                    break;
            }
        } else {
            DDLog.i(ServerService.class, "not a valid json string!");
            mServerThread.sendMsg2Client(Constants.ACK);
        }
    }


    private static void sendMsg(String cmdValue) {
        DDLog.i(ServerService.class, "sendMsg");
        String[] valueArr = cmdValue.split(";");
        if (!TextUtils.isEmpty(valueArr[0])) {
            String[] numbers = valueArr[0].split(",");
            for (String number : numbers) {
                DDLog.i(ServerService.class, "send address=" + number + ",content=" + valueArr[1]);
                sendSMS(number, valueArr[1]);
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
            if (Objects.equals(intent.getAction(), SMS_SEND_ACTION)) {
                /* android.content.BroadcastReceiver.getResultCode()方法 */
                DDLog.d(ServerService.class, "resultcode=" + getResultCode());
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_SENDSMS, Constants.SEND_MSG_SUCCESS);
                        mServerThread.sendMsg2Client(eventInfo.toJson());
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        break;
                    default:

                        break;
                }
            } else if (Objects.equals(intent.getAction(), SMS_DELIVERED_ACTION)) {
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
            if (TextUtils.equals(intent.getAction(), Constants.ACTION_HOOK_STATE)) {
                String hook_state = intent.getStringExtra("hook_state");
                DDLog.i(ServerReceiver.class, "hook_state=" + hook_state);
                EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_KEYF3, String.valueOf(hook_state));
                if (mHookFilter) {
                    mServerThread.sendMsg2Client(eventInfo.toJson());
                    mHookFilter = false;
                    mHandler.sendEmptyMessageDelayed(MSG_FILTER, 500);
                } else {
                    DDLog.i(ServerReceiver.class, "hook_state is ignored!");
                }

            } else if (TextUtils.equals(intent.getAction(), Constants.ACTION_HANDFREE_STATE)) {
                String handfree = intent.getStringExtra("handfree_state");
                DDLog.i(ServerReceiver.class, "handfree_state=" + handfree);
                EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_KEYCALL, handfree);
                mServerThread.sendMsg2Client(eventInfo.toJson());
            } else if (TextUtils.equals(intent.getAction(), Constants.USB_STATE_INTENT)) {
                boolean connected = intent.getExtras().getBoolean("connected");
                DDLog.i(ServerReceiver.class, "USB connected=" + connected);
                if (connected)
                    startServerThread();
                else
                    stopServerTherad(false);
            } else if (TextUtils.equals(intent.getAction(), Constants.FLYSCALE_PHONE_STATE_INTENT)) {
                int stateFly = intent.getIntExtra("phone_state", Call.State.INVALID);
                mActivNumber = intent.getStringExtra("phone_number");
                mAddress = intent.getStringExtra("address");
                mCallId = intent.getStringExtra("call_id");
                long connectRealTime = intent.getLongExtra("connectRealTime", 0);
                mConnectTime = intent.getLongExtra("connectTime", 0);

                mCurrentState = stateFly;
                String stateFlyStr = Call.STATE_MAP.get(stateFly);
                DDLog.i(ServerReceiver.class, "stateFly=" + stateFlyStr + ",mActivNumber=" + mActivNumber + ",mCallId=" + mCallId + ",address=" + mAddress);
                DDLog.i(ServerReceiver.class, "connectRealTime=" + connectRealTime + ",connectTime=" + mConnectTime);
                DDLog.i(ServerReceiver.class, "mOldState=" + Call.STATE_MAP.get(mOldState) + ",mCurrentState=" + Call.STATE_MAP.get(mCurrentState));
                if (mOldState == Call.State.INCOMING) {
                    if (mCurrentState == Call.State.ACTIVE) {
                        mRecorder.start(mAddress);
                    }
                } else if (mOldState == Call.State.ACTIVE) {
                    if (mCurrentState == Call.State.DISCONNECTED || mCurrentState == Call.State.DISCONNECTING) {
                        mRecorder.stop();
                    }
                }

                if (mCurrentState == Call.State.DIALING) {
                    mRecorder.start(mAddress);
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
//                        while (!mRecorder.mState.isRecording()) ;
                        DDLog.i(ServerReceiver.class, "start to recording");
                        mRecorder.start(mAddress);
                    } else if (mCurrentState == Call.State.DISCONNECTED || mCurrentState == Call.State.DISCONNECTING) {
                        mRecorder.stop();
                    }
                }
                mOldState = mCurrentState;
                notifyPhoneState(mCurrentState, true);
                if (mCurrentState == Call.State.DISCONNECTED) {
                    //如果是已经挂断状态，在通知状态后把当前状态改为IDLE
                    mCurrentState = Call.State.IDLE;
                }
            } else if (TextUtils.equals(intent.getAction(), Constants.SMS_DELIVER_INTENT) ||
                    TextUtils.equals(intent.getAction(), Constants.SMS_RECEIVED_INTENT)) {
                getMsgFromIntent(intent.getExtras());
            } else if (TextUtils.equals(intent.getAction(), TelephonyIntents.ACTION_PLMN_INTENT)) {
                String plmnLong = intent.getStringExtra(TelephonyIntents.PLMN_LONG);
                String plmnShort = intent.getStringExtra(TelephonyIntents.PLMN_SHORT);
                String plmnNumeric = intent.getStringExtra(TelephonyIntents.PLMN_NUMERIC);
                DDLog.i(ServerReceiver.class, "plmnLong=" + plmnLong + ",plmnShort=" + plmnShort + ",plmnNumeric=" + plmnNumeric);
                PreferenceUtil.put(mContext, Constants.SP_PLMN_NUMBER, plmnNumeric);
            } else if (TextUtils.equals(intent.getAction(), Constants.ACTION_UPDATE_RESULT)) {
                int result = intent.getIntExtra("result", 1000);
                DDLog.i(ServerService.class, "update result=" + result);
                EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_INSTALLAPP);
                switch (result) {
                    case 0:
                        eventInfo.EventValue = Constants.UPDATE_APP_SUCCESS;
                        break;
                    default:
                        eventInfo.EventValue = Constants.UPDATE_APP_FAILED;
                        break;
                }
                mServerThread.sendMsg2Client(eventInfo.toJson());
            } else if (TextUtils.equals(intent.getAction(), Constants.ACTION_PLAY_SOUND_2MIC_RESULT)) {
                EventInfo result = new EventInfo();
                result.EventType = Constants.EVENT_TYPE_AUTO_REPLY_OVER;
                mServerThread.sendMsg2Client(result.toJson());
            }
        }

    }

    /**
     * 通知EC客户端电话状态发生改变
     *
     * @param state
     * @param withCallId
     */
    private void notifyPhoneState(int state, boolean withCallId) {
        DDLog.i(ServerService.class, "notifyPhoneState,state=" + state);
        String ecPhoneState = Constants.PHONE_STATE_IDLE;
        String ecCallState = Constants.CALL_STATE_IDLE;
        switch (state) {
            case Call.State.ACTIVE://电话接通
                ecCallState = Constants.CALL_STATE_OFFHOOK_IN;
                ecPhoneState = Constants.PHONE_STATE_OFFHOOK;
                break;
            case Call.State.CALL_WAITING://电话等待
                ecCallState = Constants.CALL_STATE_RINGING_IN;
                ecPhoneState = Constants.PHONE_STATE_WAIT;
                break;
            case Call.State.CONFERENCED://电话会议
                break;
            case Call.State.DIALING://拨号中
                ecCallState = Constants.CALL_STATE_RINGING_OUT;
                ecPhoneState = Constants.PHONE_STATE_RINGING_OUT;
                break;
            case Call.State.DISCONNECTED://已挂断
                ecCallState = Constants.CALL_STATE_RINGING_OUT;
                ecPhoneState = Constants.PHONE_STATE_DISCONNECT;
                break;
            case Call.State.DISCONNECTING://正在挂断
                ecPhoneState = Constants.PHONE_STATE_DISCONNECTING;
                ecCallState = Constants.CALL_STATE_RINGING_OUT;
                break;
            case Call.State.IDLE://待机状态
                ecCallState = Constants.CALL_STATE_IDLE;
                ecPhoneState = Constants.PHONE_STATE_IDLE;
                break;
            case Call.State.INCOMING://有来电
                ecCallState = Constants.CALL_STATE_RINGING_IN;
                ecPhoneState = Constants.PHONE_STATE_RING_IN;
                break;
            case Call.State.ONHOLD://
                return;
            case Call.State.REDIALING://重拨

                return;
            default:
                return;
        }
        CallInfo callStateInfo = PhoneUtil.getCallStateInfo(this, state);
        callStateInfo.PhoneState = ecPhoneState;
        callStateInfo.CallState = ecCallState;
        if (withCallId) {
            callStateInfo.CallId = mCallId;
        } else {
            callStateInfo.CallId = "";
        }
        mServerThread.sendMsg2Client(callStateInfo.toJson());
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


    //public class ServerBinder extends IListenService.
    public class ServerBinder extends IListenService.Stub {
        private ServerService getService() {
            return ServerService.this;
        }

        @Override
        public void setCallBack(IDataInfo info) throws RemoteException {
            DDLog.i(ServerBinder.class, "setCallBack,thread=" + Thread.currentThread().getName());
            //设置回调
            mIDataInfo = info;
            if (mRecorder != null) {
                mRecorder.setIDataInfo(mIDataInfo);
            }
        }

        @Override
        public void setVoiceData(String data) throws RemoteException {
            DDLog.i(ServerBinder.class, "setVoiceData,data=" + data);
            EventInfo eventInfo = new EventInfo(Constants.EVENT_TYPE_SET_VOICE_TO_STR, data);
            if (mServerThread != null) {
                mServerThread.sendMsg2Client(eventInfo.toJson());
            }
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            DDLog.i(ServerBinder.class, "onTransact");
            //权限验证
            int check = checkCallingPermission(Constants.BIND_SERVICE_PERMISSION);
            if (check == PackageManager.PERMISSION_DENIED) {
                DDLog.w(ServerBinder.class, "permission denied!");
                return false;
            }
            //包名验证
            String packageName = null;
            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
            if (packages != null && packages.length > 0) {
                packageName = packages[0];
                DDLog.i(ServerBinder.class, "calling packageName=" + packageName);
            }
            assert packageName != null;
            if (!packageName.startsWith("com")) {
                DDLog.w(ServerBinder.class, "package is not granted!");
                return false;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        DDLog.i(ServerService.class, "onBind");
//        int check = checkCallingPermission(Constants.BIND_SERVICE_PERMISSION);
//        if (check == PackageManager.PERMISSION_DENIED){
//            DDLog.i(ServerService.class, "permission denied!");
//            return null;
//        }

        if (mServerBinder == null)
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

    /**
     * @param restart 是否重启线程
     */
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
        intentFilter.addAction(Constants.ACTION_UPDATE_RESULT);
        intentFilter.addAction(Constants.ACTION_HANDFREE_STATE);
        intentFilter.addAction(Constants.ACTION_HOOK_STATE);
        intentFilter.addAction(Constants.ACTION_PLAY_SOUND_2MIC_RESULT);
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
