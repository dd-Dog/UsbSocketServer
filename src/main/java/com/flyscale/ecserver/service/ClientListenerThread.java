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
    private static final long MSG_NOTIFY_DIED_DELAYED = 100;    //延时发送线程死亡消息
    private boolean mLoop = true;
    private ArrayList<Handler> mHandlerList = new ArrayList<>();
    public static final int MSG_FROM_CLIENT = 1001;
    private Socket mClientSocket;
    private ServerSocket mServerSocket;
    private final KeepAlive mKeepAliver;
    private static ClientListenerThread sInstance;
    public static final int MSG_LISTENER_THREAD_DIED = 1002;
    private boolean mRestart = true;

    public ClientListenerThread() {
        super(ClientListenerThread.class.getSimpleName());
        mKeepAliver = new KeepAlive();
        sInstance = this;
    }

    public void setLoop(boolean mLoop) {
        DDLog.i(ClientListenerThread.class, "setLoop");
        this.mLoop = mLoop;
    }

    public void closeSocket() {
        DDLog.i(ClientListenerThread.class, "closeSocket");
        if (mServerSocket != null) {
            if (!mServerSocket.isClosed()) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setRestart(boolean restart) {
        DDLog.i(ClientListenerThread.class, "setRestart,retart=" + restart);
        mRestart = restart;
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

    public void removeHandler(Handler handler) {
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
                        dos.write(msg.getBytes("UTF-8"));//保证收发一致UTF-8
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
            int buffSize = 1024;
            byte[] buffer = new byte[buffSize];
            DDLog.i(ClientListenerThread.class, "waiting for client...");
            mClientSocket = mServerSocket.accept();
            DDLog.i(ClientListenerThread.class, "accept");
            mKeepAliver.setListenerThread(sInstance);
//            mKeepAliver.startKeepAlive();
            while (mLoop) {
                DataInputStream inputStream = new DataInputStream(mClientSocket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(mClientSocket.getOutputStream());

                int len = -1;
                int sumLen = 0;
                /*开始读取并拼接字符串到sb中*/
                StringBuilder sb = new StringBuilder();
                //如果读取的字符个数超过了缓冲区大小，就继续读
                while ((len = inputStream.read(buffer)) >= buffSize) {
                    DDLog.i(ClientListenerThread.class, "len=" + len);
                    String str = new String(buffer, 0, len, "UTF-8");
                    sb.append(str);
                    sumLen += len;
                }
                sumLen += len;
                if (sumLen > 0) {
                    //最后一次读的一般小于缓冲区大小
                    String str = new String(buffer, 0, len, "UTF-8");
                    sb.append(str);
                    /*拼接字符串完成*/
                    DDLog.i(ClientListenerThread.class, "read sumLen=" + sumLen);
                    final String text = sb.toString();
                    DDLog.i(ClientListenerThread.class, "receiver from client: " + text);
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
                } else {
                    DDLog.w(ClientListenerThread.class, "client maybe got an eror,close socket!");
                    mLoop = false;
                }
//                Thread.sleep(1000);
            }
            mClientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mServerSocket != null) {
                try {
                    if (mRestart) {
                        //发出通知，线程即将结束,主要用来重启线程
                        for (Handler handler : mHandlerList) {
                            Message message = handler.obtainMessage();
                            message.what = MSG_LISTENER_THREAD_DIED;
                            handler.sendMessageDelayed(message, MSG_NOTIFY_DIED_DELAYED);
                        }
                    }
                    mServerSocket.close();
                    mHandlerList.clear();
                    mKeepAliver.stopKeepAlive();
                    DDLog.e(ClientListenerThread.class, "close socket!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
