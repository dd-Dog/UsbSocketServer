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
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    ClientListenerThread serverThread;
    private ServerConnection mServerConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        serverThread = new ClientListenerThread(ClientListenerThread.class.getSimpleName(), true);
//        serverThread.start();

        mServerConnection = new ServerConnection();
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent,mServerConnection, Context.BIND_AUTO_CREATE);
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
        unbindService(mServerConnection);
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
