package com.flyscale.ecserver.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.ITelephony;
import com.flyscale.ecserver.bean.CallInfo;
import com.flyscale.ecserver.bean.DeviceInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.service.ServerService;

import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
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
        Log.d(TAG, "answerCall");
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
        Log.d(TAG, "endCall");
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
        Log.d(TAG, "isRinging");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            return iTelephony.isRinging();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isIdle(Context context) {
        Log.d(TAG, "isIdle");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            return iTelephony.isIdle();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isOffhook(Context context) {
        Log.d(TAG, "isOffhook");
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // 获取getITelephony的方法对象
            Method getITelephonyMethod = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephony iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyManager);
            return iTelephony.isOffhook();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 来电静音
     *
     * @param context
     */
    public static void silenceRinger(Context context) {
        Log.d(TAG, "silenceRinger");
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
        Log.d(TAG, "dial");
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
        DDLog.i(PhoneUtil.class, "getRamInfo");
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
            callInfo.CallNumber = ServerService.getActivNumber();
            callInfo.CallTime = "";
            callInfo.CallId = getCallId(context);
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
        DDLog.i(PhoneUtil.class, "generateCallId");
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
        DDLog.i(PhoneUtil.class, "getCallId");
        return PreferenceUtil.getString(context, Constants.SP_CALL_ID, "");
    }

    /**
     * 设置免提
     *
     * @param mContext
     */
    public static void setHandfree(Context mContext) {
        DDLog.i(PhoneUtil.class, "setHandfree");
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int oldMode = am.getMode();
        am.setMode(AudioManager.MODE_IN_CALL);
        am.setSpeakerphoneOn(true);
    }

}
