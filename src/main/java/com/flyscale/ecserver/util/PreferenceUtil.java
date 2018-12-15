package com.flyscale.ecserver.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.flyscale.ecserver.global.Constants;


/**
 * Created by MrBian on 2017/11/23.
 */

public class PreferenceUtil {

    @SuppressLint("CommitPrefEdits")
    public static void put(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP,
                Context.MODE_PRIVATE);
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, value);
            editor.commit();
        }
    }

    public static void put(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP,
                Context.MODE_PRIVATE);
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(key, value);
            editor.commit();
        }
    }

    public static void put(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP,
                Context.MODE_PRIVATE);
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(key, value);
            editor.commit();
        }
    }

    public static int getInt(Context context, String key, int defValue) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP, Context.MODE_PRIVATE);
        int str = sp.getInt(key, defValue);
        return str;
    }
    public static long getLong(Context context, String key, long defValue) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP, Context.MODE_PRIVATE);
        long str = sp.getLong(key, defValue);
        return str;
    }

    public static String getString(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(Constants.AT_CLIENT_SP,
                Context.MODE_PRIVATE);
        String str = sp.getString(key, defValue);
        return str;
    }
}
