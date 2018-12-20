package com.flyscale.ecserver;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.flyscale.ecserver.service.ClientListenerThread;
import com.flyscale.ecserver.service.ServerService;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.JsonUtil;
import com.flyscale.ecserver.util.ServiceUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    ClientListenerThread serverThread;
    private ServerConnection mServerConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        serverThread = new ClientListenerThread(ClientListenerThread.class.getSimpleName(), true);
//        serverThread.start();

//        mServerConnection = new ServerConnection();
//        Intent intent = new Intent(this, ServerService.class);
//        bindService(intent,mServerConnection, Context.BIND_AUTO_CREATE);
//        DDLog.i(MainActivity.class, "SDTotal=" + StorageUtil.getSDTotalSize(this));
//        DDLog.i(MainActivity.class, "SDAvail=" + StorageUtil.getSDAvailableSize(this));
//        DDLog.i(MainActivity.class, "PhoneTotal=" + StorageUtil.getUserDataTotalSize(this));
        DDLog.i(MainActivity.class, "isJson=" + JsonUtil.isJson("{\"CallNumber\":\"13043467225\",\"EventType\":\"1\"}", 0));
        DDLog.i(MainActivity.class, "isJson=" + JsonUtil.isJson("{\"CallNumber\":\"13043467225\"\"EventType\":\"1\"}", 0));

        TextView tv = findViewById(R.id.tv);
        CharSequence text = tv.getText();
        DDLog.i(MainActivity.class, "text=" + text);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ServiceUtil.isServiceRunning(this, ServerService.class.getName())) {
            DDLog.i(MainActivity.class, "ServerService is not running, start it");
            Intent intent = new Intent(this, ServerService.class);
            startService(intent);
        } else {
            DDLog.i(MainActivity.class, "ServerService is already running");
        }
    }

    private class ServerConnection implements ServiceConnection {

        private ServerService mServerService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServerService = ((ServerService.ServerBinder) service).getService();
            mServerService.test();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onBindingDied(ComponentName name) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    Handler handler = new Handler() {

        @SuppressLint("NewApi")
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), msg.getData().getString("MSG", "Toast"),
                    Toast.LENGTH_SHORT).show();
        }
    };

}
