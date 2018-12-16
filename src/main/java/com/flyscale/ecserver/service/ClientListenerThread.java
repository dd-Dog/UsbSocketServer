package com.flyscale.ecserver.service;

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
import java.util.ArrayList;

/**
 * Created by bian on 2018/12/10.
 * 接收客户端连接线程
 */

public class ClientListenerThread extends Thread {
    boolean isLoop = true;
    private ArrayList<Handler> mHandlerList = new ArrayList<>();
    public static final int MSG_FROM_CLIENT = 1001;
    private Socket mClientSocket;
    private ServerSocket mServerSocket;
    private final KeepAlive mKeepAliver;
    private static ClientListenerThread sInstance;

    public ClientListenerThread() {
        super(ClientListenerThread.class.getSimpleName());
        mKeepAliver = new KeepAlive();
        sInstance = this;
    }

    /**
     * @param handler 需要设置回调的handler
     * @return
     */
//    public static ClientListenerThread getInstance(Handler handler) {
        /*DDLog.i(ClientListenerThread.class, "getInstance()");
        if (sInstance == null) {
            sInstance = new ClientListenerThread(ClientListenerThread.class.getSimpleName());
        } else if (sInstance.getState() == State.NEW) {
            sInstance.start();
        } else if (sInstance.getState() == State.RUNNABLE || sInstance.getState() == State.BLOCKED ||
                sInstance.getState() == State.TIMED_WAITING || sInstance.getState() == State.WAITING) {
        } else {
            sInstance = new ClientListenerThread(ClientListenerThread.class.getSimpleName());
        }
        sInstance.addHandler(handler);
        return sInstance;*/

//        return SingletonHolder.sInstance;
//    }

    /**
     * 静态内部类保存线程的实例对象
     */
    private static class SingletonHolder {
//        private static ClientListenerThread sInstance = new ClientListenerThread(ClientListenerThread.class.getSimpleName());

        /*
     * 判断线程状态，如果没有在运行，就要重新创建新的线程对象
     *
     * @return 返回的线程已经开始执行run方法

        private static ClientListenerThread getInstance(Handler handler) {
//            DDLog.i(SingletonHolder.class, "getInstance()");
            if (sInstance.getState() == State.NEW) {
                sInstance.start();
            } else if (sInstance.getState() == State.RUNNABLE || sInstance.getState() == State.BLOCKED ||
                    sInstance.getState() == State.TIMED_WAITING || sInstance.getState() == State.WAITING) {
            } else {
                sInstance = new ClientListenerThread(ClientListenerThread.class.getSimpleName());
            }
            sInstance.addHandler(handler);
            return sInstance;
        }*/
    }

    /**
     * 添加Handler
     *
     * @param handler
     */
    public void addHandler(Handler handler) {
        DDLog.i(ClientListenerThread.class, "addHandler");
        mHandlerList.add(handler);
    }

    public void removeHandler(Handler handler){
        DDLog.i(ClientListenerThread.class, "removeHandler");
        mHandlerList.remove(handler);
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
                        DDLog.i(ClientListenerThread.class, "send complete");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            DDLog.w(ClientListenerThread.class, "no client connected!");
        }
    }

    @Override
    public void run() {
        DDLog.i(ClientListenerThread.class, "start thread");

        try {
            mServerSocket = new ServerSocket(Constants.LOCAL_PORT);
            byte[] buffer = new byte[1024];
            DDLog.i(ClientListenerThread.class, "waiting for client...");
            mClientSocket = mServerSocket.accept();
            DDLog.i(ClientListenerThread.class, "accept");
            mKeepAliver.setListenerThread(sInstance);
            mKeepAliver.startKeepAlive();
            while (isLoop) {
                DataInputStream inputStream = new DataInputStream(mClientSocket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(mClientSocket.getOutputStream());

                int len = inputStream.read(buffer);
                if (len > 0) {
                    final String text = new String(buffer, 0, len);
                    DDLog.i(ClientListenerThread.class, "text=" + text);
                    if (mHandlerList != null) {
                        for (Handler handler : mHandlerList) {
                            Message message = handler.obtainMessage();
                            message.obj = text;
                            message.what = MSG_FROM_CLIENT;
                            handler.sendMessage(message);
                        }
                    }
                    String echo = Constants.ACK;
                    outputStream.write(echo.getBytes("UTF-8"));
                    outputStream.flush();
                }
//                Thread.sleep(1000);
            }
            mClientSocket.close();
            mHandlerList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                    mHandlerList.clear();
                    mKeepAliver.stopKeepAlive();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
