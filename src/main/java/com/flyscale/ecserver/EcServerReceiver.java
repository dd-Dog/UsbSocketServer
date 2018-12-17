package com.flyscale.ecserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.flyscale.ecserver.service.ServerService;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.ServiceUtil;


/**
 * Created by bian on 2018/12/6.
 */

public class EcServerReceiver extends BroadcastReceiver {
    private static final String TAG = "EcServerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        DDLog.init(context);
        String action = intent.getAction();
        String dataString = intent.getDataString();
        DDLog.i(EcServerReceiver.class, "action=" + action + ",dataString=" + dataString);
        if (TextUtils.equals(action, "android.intent.action.BOOT_COMPLETED")) {
            boolean serviceRunning = ServiceUtil.isServiceRunning(context, ServerService.class.getName());
            DDLog.i(EcServerReceiver.class, "serviceRunning=" + serviceRunning);
            if (!ServiceUtil.isServiceRunning(context, ServerService.class.getName())){
                DDLog.i(EcServerReceiver.class, "ServerService is not running, start it");
                Intent service = new Intent(context, ServerService.class);
                context.startService(service);
            }else {
                DDLog.i(EcServerReceiver.class, "ServerService is already running");
            }
        }
    }
}
