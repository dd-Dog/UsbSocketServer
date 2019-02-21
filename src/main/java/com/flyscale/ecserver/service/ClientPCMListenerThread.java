package com.flyscale.ecserver.service;

import android.os.Handler;
import android.os.Message;

import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.util.DDLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by bian on 2019/2/21.
 */

public class ClientPCMListenerThread extends Thread {

    private final Handler mHandler;
    private ServerSocket mPCMServerSocket;
    private CopyOnWriteArrayList<Socket> mClientSockets;
    public static final int MSG_CLIENT_CHANGED = 6001;
    private static final int MSG_CLIENT_KEELP_ALIVE = 6002;

    private boolean mLoop = true;
    private final KeepAliveHandler mKeepAliveHandler;

    public ClientPCMListenerThread(Handler handler) {
        try {
            mPCMServerSocket = new ServerSocket(Constants.LOCAL_PORT_STREAM);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mClientSockets = new CopyOnWriteArrayList<>();
        mHandler = handler;
        mKeepAliveHandler = new KeepAliveHandler();
        mKeepAliveHandler.sendEmptyMessageDelayed(MSG_CLIENT_KEELP_ALIVE, 5000);
    }

    private class KeepAliveHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CLIENT_KEELP_ALIVE) {
                if (mClientSockets != null) {
                    DDLog.i(ClientListenerThread.class, "check clients connect status,size=" +mClientSockets.size());
                    for (Socket socket : mClientSockets) {
                        try {
                            //发送紧急字符，测试客户端的连通性
                            socket.sendUrgentData(0);
                        } catch (IOException e) {
                            e.printStackTrace();
                            mClientSockets.remove(socket);
                            DDLog.i(ClientPCMListenerThread.class, "remove one, current sockets count=" + mClientSockets.size());
                        }
                    }
                    mKeepAliveHandler.sendEmptyMessageDelayed(MSG_CLIENT_KEELP_ALIVE, 5000);
                }

            }
        }
    }

    @Override
    public void run() {
        super.run();
        while (mLoop) {
            try {
                DDLog.i(ClientPCMListenerThread.class, "server is waiting for client...");
                Socket socket = mPCMServerSocket.accept();
                DDLog.i(ClientPCMListenerThread.class, "accept a client");
                addClient(socket);

                Message message = mHandler.obtainMessage();
                message.obj = mClientSockets;
                message.what = MSG_CLIENT_CHANGED;
                mHandler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addClient(Socket socket) {
        DDLog.i(ClientPCMListenerThread.class, "addClient");
        if (socket == null) return;
        InetAddress inetAddress = socket.getInetAddress();
        int port = socket.getPort();
        DDLog.i(ClientPCMListenerThread.class, "addressStr=" + inetAddress + ",port=" + port);
        boolean exists = false;
        for (Socket s : mClientSockets) {
            if (s.getInetAddress().getAddress().equals(inetAddress.getAddress()) && s.getPort() == socket.getPort()) {
                exists = true;
                DDLog.i(ClientPCMListenerThread.class, "socket exists int list!");
            }
        }
        if (!exists){
            mClientSockets.add(socket);
            DDLog.i(ClientListenerThread.class, "add one,socket count=" + mClientSockets.size());
        }
    }
}
