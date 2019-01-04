package com.flyscale.ecserver.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.ITelephony;
import com.flyscale.ecserver.bean.CallInfo;
import com.flyscale.ecserver.bean.DeviceInfo;
import com.flyscale.ecserver.bean.SmsInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.service.ServerService;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bian on 2018/8/25.
 */

public class PhoneUtil {

    private static final String TAG = "PhoneUtil";

    public static final int EMPTY = 1001;
    public static final int NUMBER_CHAR_INVALID = 1002;
    public static final int NUMBER_INVALID = 1003;
    public static final int OK = 0;

    /**
     * 接听电话
     *
     * @param context
     */
    public static void answerCall(Context context) {
        Log.d(TAG, "answerCall()");
        try {
            // 获取getITelephony的方法对象
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            iTelephony.answerRingingCall();// 挂断电话 权限
            // android.permission.CALL_PHONE
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 挂断电话
     *
     * @param context
     */
    public static void endCall(Context context) {
        Log.d(TAG, "endCall()");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            iTelephony.endCall();// 挂断电话 权限 android.permission.CALL_PHONE
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isRinging(Context context) {
        boolean isRinging = false;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            isRinging = iTelephony.isRinging();
        } catch (Exception e) {
            e.printStackTrace();
            isRinging = false;
        } finally {
            Log.d(TAG, "isRinging(),isRinging=" + isRinging);
            return isRinging;
        }
    }

    public static boolean isIdle(Context context) {
        boolean isIdle = false;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            return isIdle = iTelephony.isIdle();
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = false;
        } finally {
            Log.d(TAG, "isIdle(),isIdle=" + isIdle);
            return isIdle;
        }
    }

    public static boolean isOffhook(Context context) {
        boolean isOffhook = false;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            isOffhook = iTelephony.isOffhook();
        } catch (Exception e) {
            e.printStackTrace();
            isOffhook = false;
        } finally {
            Log.d(TAG, "isOffhook(),isOffhook=" + isOffhook);
            return isOffhook;
        }
    }

    /**
     * 来电静音
     *
     * @param context
     */
    public static void silenceRinger(Context context) {
        Log.d(TAG, "silenceRinger()");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);// 设置私有方法可以访问
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            iTelephony.silenceRinger();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示拨号界面
     *
     * @param context
     */
    public static void dial(Context context, String number) {
        Log.d(TAG, "dial()");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);// 执行方法获取ITelephony的对象
            iTelephony.dial(number);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拨打电话
     *
     * @param context
     */
    public static void call(Context context, String number) {
        DDLog.i(PhoneUtil.class, "call,number=" + number);
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("PrivateApi") Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);// 执行方法获取ITelephony的对象
            iTelephony.call(context.getPackageName(), number);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回电话状态
     * {@link TelephonyManager#CALL_STATE_RINGING} 1
     * {@link TelephonyManager#CALL_STATE_OFFHOOK}  2
     * {@link TelephonyManager#CALL_STATE_IDLE} 0
     */
    public static int getPhoneState(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getCallState();
    }

    public static void dialNumber(Context context, String num) {
        Intent call = new Intent(Constants.ACTION_CALL_PRIVILEGED, Uri.parse("tel:" + num));
        call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(call);
    }


    public static int checkNum(String num) {
        if (TextUtils.isEmpty(num)) {
            return EMPTY;
        }
        Pattern patern = Pattern.compile("[0-9*#+ABC]*");
        Matcher matcher = patern.matcher(num);
        if (matcher.matches()) {
            return OK;
        }
        return NUMBER_INVALID;
    }

    /**
     * 获取上次拨出电话号码
     */
    public static String getLastOutNumber(Context context) {
        @SuppressLint("MissingPermission") Cursor query = context.getContentResolver().query(Calls.CONTENT_URI, new String[]{"number", "date"},
                "type=2", null, "date desc");
        if (query != null) {
            if (query.moveToFirst()) {
                return query.getString(query.getColumnIndex("number"));
            }
        }
        return null;
    }


    public static DeviceInfo getDeviceInfo(Context context) {
        DDLog.i(PhoneUtil.class, "getDeviceInfo()");
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.DeviceName = Build.DEVICE;
        deviceInfo.ModelName = Build.MODEL;
        deviceInfo.SoftVer = Build.VERSION.RELEASE;
        deviceInfo.HardwareVer = Build.HARDWARE;
        deviceInfo.SdkVer = Build.VERSION.SDK_INT + "";
        String[] ramInfo = getRamInfo(context);
        deviceInfo.TotalRam = ramInfo[0];
        deviceInfo.AvailableRam = ramInfo[1];
        deviceInfo.TotalRom = StorageUtil.getUserDataTotalSize(context);
        deviceInfo.AvailableRom = StorageUtil.getUserDataAvailableSize(context);
        deviceInfo.SerialNumber = getSerialNumber(context);
        deviceInfo.EventType = Constants.EVENT_TYPE_GETDEVICEINFO;
        return deviceInfo;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String getSerialNumber(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }

    public static String[] getRamInfo() {
        DDLog.i(PhoneUtil.class, "getRamInfo()");
        String[] meminfo = new String[2];
        try {
            FileReader fileReader = new FileReader("/proc/meminfo");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            for (int i = 0; i < 2; i++) {
                String line = bufferedReader.readLine();
                //  '\\s'表示空格,回车,换行等空白符,+号表示一个或多个
//                String[] split = line.split("//s+");
                String[] split = line.split(":");
                meminfo[i] = split[1].trim();
                DDLog.i(PhoneUtil.class, "mem=" + split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return meminfo;
    }

    public static String[] getRamInfo(Context context) {
        DDLog.i(PhoneUtil.class, "getRamInfo");
        String[] meminfo = new String[2];
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        assert am != null;
        am.getMemoryInfo(memoryInfo);
        meminfo[0] = memoryInfo.totalMem / 1024 + " KB";
        meminfo[1] = memoryInfo.availMem / 1024 + " KB";
        DDLog.i(PhoneUtil.class, "totalMem=" + meminfo[0]);
        DDLog.i(PhoneUtil.class, "availMem=" + meminfo[1]);
        return meminfo;
    }

    public static CallInfo getCallStateInfo(Context context) {
        DDLog.i(PhoneUtil.class, "getCallStateInfo");
        CallInfo callInfo = new CallInfo();
        callInfo.CallState = getPhoneState(context) + "";
        callInfo.PhoneState = getPhoneState(context) + "";
        if (isIdle(context)) {
            callInfo.CallNumber = "";
            callInfo.CallTime = "";
        } else {
            callInfo.CallNumber = ServerService.getAddress();
            callInfo.CallTime = ServerService.mConnectTime + "";
//            callInfo.CallId = ServerService.mCallId +"";
            callInfo.RecoderPath = PreferenceUtil.getString(context, Constants.SP_RECORDER_PATH, "");
        }
        callInfo.EventType = Constants.EVENT_TYPE_CALLSTATE;

        return callInfo;
    }

    /**
     * 生成call id
     *
     * @param context
     * @param activNumber
     */
    public static void generateCallId(Context context, String activNumber) {
        DDLog.i(PhoneUtil.class, "generateCallId()");
        String time = System.currentTimeMillis() + "";
        String callId = activNumber + "#" + time;
        PreferenceUtil.put(context, Constants.SP_CALL_ID, callId);
    }

    /**
     * 获取call id
     * 如果当前状态为IDLE，会返回上次的call id
     *
     * @param context
     */
    public static String getCallId(Context context) {
        DDLog.i(PhoneUtil.class, "getCallId()");
        return PreferenceUtil.getString(context, Constants.SP_CALL_ID, "");
    }

    /**
     * 设置免提
     *
     * @param mContext
     */
    public static void setHandfree(Context mContext) {
        DDLog.i(PhoneUtil.class, "setHandfree()");
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int oldMode = am.getMode();
        am.setMode(AudioManager.MODE_IN_CALL);
        am.setSpeakerphoneOn(true);
    }

    /**
     * @return true-放下 false-拿起
     */
    public static boolean getHookState() {
        String stateStr = StorageUtil.readHeadLine("dev/flyscale_misc");
        boolean state = true;
        if (TextUtils.equals("0", stateStr)) {
            state = true;
        } else if (TextUtils.equals("1", stateStr)) {
            state = false;
        }
        DDLog.i(PhoneUtil.class, "stateStr=" + stateStr);
        return state;
    }

    /**
     * 获取免提状态
     *
     * @param context
     * @return
     */
    public static boolean getHandfree(Context context) {

        boolean handFree = false;
        boolean hookState = getHookState();
        if (hookState) { //如果手柄未抬起，且当前处于拨号界面或者通话中，则视为免提状态
            //获取当前最上层的Activity
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            assert am != null;
            //获取最位于栈顶的task的info
            ActivityManager.RunningTaskInfo runningTaskInfo = am.getRunningTasks(1).get(0);
            String className = runningTaskInfo.topActivity.getClassName();
            DDLog.i(PhoneUtil.class, "className=" + className);
            if (TextUtils.equals(className, "com.android.dialer.DialtactsActivity")) {
                Cursor alarms = context.getContentResolver().query(Uri.parse("content://com.android.dialer.provider/settings"), null, null, null,
                        null);
                if (alarms != null) {
                    while (alarms.moveToNext()) {
                        String dialtacts_content = alarms.getString(alarms.getColumnIndex("dialtacts_content"));
                        DDLog.i(PhoneUtil.class, "dialtacts_content=" + dialtacts_content);
                        if (TextUtils.equals(Constants.MODE_IDLE, dialtacts_content)) {
                            //如果是IDLE
                            handFree = false;
                        } else if (TextUtils.equals(Constants.MODE_DIAL_NUM, dialtacts_content)) {
                            //如果是拨号界面
                            handFree = true;
                        }
                    }
                } else {
                    handFree = true;
                }
            } else if (isOffhook(context) || isRinging(context)) {
                handFree = true;
            } else {
                handFree = false;
            }
        } else {//如果手柄抬起状态，则判断当前是SPEAKER状态视为抬起免提
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            assert am != null;
            handFree = am.isSpeakerphoneOn();
        }
        DDLog.i(PhoneUtil.class, "getHandfree(),handFree=" + handFree);
        return handFree;
    }

    /**
     * 获取Sim卡状态
     *
     * @param context
     * @return
     */
    public static String getSimcardState(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        assert tm != null;
        int simState = tm.getSimState();
        DDLog.i(PhoneUtil.class, "getSimcardState(),simState=" + simState);
        return simState + "";
    }

    /**
     * 读取收件箱和发件箱的短信
     * 并生成json数据
     * @param context
     */
    public static void readInboxOutBoxMsg(final Context context, final QueryCompeleteCallback callback) {
        DDLog.i(PhoneUtil.class, "readInboxOutBoxMsg()");
        ThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<SmsInfo> inboxSmsInfo = SmsUtil.getSmsInfo(context, SmsUtil.SMS_URI_INBOX);
                ArrayList<SmsInfo> outboxSmsInfo = SmsUtil.getSmsInfo(context, SmsUtil.SMS_URI_SEND);
                List<Map> list = new ArrayList<Map>();
                JSONObject jsonObject = new JSONObject();
//                JSONArray jsonArray = new JSONArray();

                Map<String, String> map = null;
                for (SmsInfo smsInfo : inboxSmsInfo) {
                    list.add(smsInfo.toMap());
                }
                for (SmsInfo smsInfo : outboxSmsInfo) {
                    list.add(smsInfo.toMap());
                }
                try {
                    jsonObject.put(Constants.CMD_EVENT_VALUE, list);
                    jsonObject.put(Constants.CMD_EVENT_TYPE, Constants.EVENT_TYPE_QUERYSMS);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (callback != null){
                    callback.onQuerySuccess(jsonObject.toString());
                }
            }
        });
    }
}
