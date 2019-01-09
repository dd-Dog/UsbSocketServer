package com.flyscale.ecserver.application;

import android.app.Application;
import android.content.Intent;

import com.flyscale.ecserver.service.ServerService;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.ServiceUtil;

/**
 * Created by bian on 2018/12/10.
 */

public class ServerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        DDLog.init(this, false);
        if (!ServiceUtil.isServiceRunning(this, ServerService.class.getName())) {
            DDLog.i(ServerApplication.class, "ServerService is not running, start it");
            Intent intent = new Intent(this, ServerService.class);
            startService(intent);
        } else {
            DDLog.i(ServerApplication.class, "ServerService is already running");
        }
    }
}
