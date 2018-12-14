package com.flyscale.ecserver.application;

import android.app.Application;

import com.flyscale.ecserver.util.DDLog;

/**
 * Created by bian on 2018/12/10.
 */

public class ServerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DDLog.init(this);
    }
}
