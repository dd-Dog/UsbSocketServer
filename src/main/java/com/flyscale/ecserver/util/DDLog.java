package com.flyscale.ecserver.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * Created by bian on 2018/12/10.
 */

public class DDLog {

    public static boolean APP_DBG = false; // 是否是debug模式

    public static void init(Context context) {
        APP_DBG = isApkDebugable(context);
    }

    /**
     * 但是当我们没在AndroidManifest.xml中设置其debug属性时:
     * 使用Eclipse运行这种方式打包时其debug属性为true,使用Eclipse导出这种方式打包时其debug属性为法false.
     * 在使用ant打包时，其值就取决于ant的打包参数是release还是debug.
     * 因此在AndroidMainifest.xml中最好不设置android:debuggable属性置，而是由打包方式来决定其值.
     *
     * @param context
     * @return
     */
    public static boolean isApkDebugable(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void i(Class c, String msg) {
        if (APP_DBG)
            Log.i(c.getSimpleName(), msg);
    }

    public static void d(Class c, String msg) {
        if (APP_DBG)
            Log.d(c.getSimpleName(), msg);
    }


    public static void w(Class c, String msg) {
        if (APP_DBG)
            Log.w(c.getSimpleName(), msg);
    }


    public static void e(Class c, String msg) {
        Log.e(c.getSimpleName(), msg);
    }


}