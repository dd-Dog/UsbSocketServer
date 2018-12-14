package com.flyscale.ecserver;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by bian on 2018/12/10.
 * 接收客户端连接线程
 */

public class ClientListenerThread extends Thread {
    boolean isLoop = true;
    private Handler mHandler;
    public static final int MSG_FROM_CLIENT = 1001;
    private Socket mClientSocket;
    private ServerSocket mServerSocket;

    public ClientListenerThread(String name, Handler mHandler, boolean isLoop) {
        super(name);
        this.mHandler = mHandler;
        this.isLoop = isLoop;
    }

    public void sendMsg2Client(final String msg) {
        DDLog.i(ClientListenerThread.class, "sendMsg2Client,msg=" + msg);
        if (TextUtils.isEmpty(msg)) {
            DDLog.w(ClientListenerThread.class, "can not send empty msg to client!");
            return;
        }
        if (mClientSocket != null && !mClientSocket.isClosed()) {
            ThreadPool.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());
                        dos.write(msg.getBytes("UTF-8"));
                        dos.flush();
                        DDLog.i(ClientListenerThread.class , "send complete");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void run() {
        DDLog.i(ClientListenerThread.class, "start thread");

        try {
            mServerSocket = new ServerSocket(9000);
            byte[] buffer = new byte[1024];
            mClientSocket = mServerSocket.accept();
            DDLog.i(ClientListenerThread.class, "accept");
            while (isLoop) {
                DataInputStream inputStream = new DataInputStream(mClientSocket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(mClientSocket.getOutputStream());

                int len = inputStream.read(buffer);
                if (len > 0) {
                    final String text = new String(buffer, 0, len);
                    DDLog.i(ClientListenerThread.class, "text=" + text);
                    if (mHandler != null) {
                        Message message = mHandler.obtainMessage();
                        message.obj = text;
                        message.what = MSG_FROM_CLIENT;
                        mHandler.sendMessage(message);
                    }
                    String echo = Constants.ACK;
                    outputStream.write(echo.getBytes("UTF-8"));
                    outputStream.flush();
                }
                Thread.sleep(1000);
            }
            mClientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
