package com.flyscale.ecserver.util;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Created by bian on 2018/8/21.
 */

public class ServiceUtil {
    private static final String TAG = "ServiceUtil";

    /**
     * 用来判断服务是否运行.
     *
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context context, String className) {
        DDLog.i(ServiceUtil.class, "isServiceRunning, className=" + className);
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(30);
        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                isRunning = true;
                break;
            }
        }
        DDLog.i(ServiceUtil.class,"isRunning=" + isRunning);
        return isRunning;
    }
}
