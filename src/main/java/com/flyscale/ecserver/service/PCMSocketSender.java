package com.flyscale.ecserver.service;

import com.flyscale.ecserver.recorder.Queue;
import com.flyscale.ecserver.util.DDLog;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by bian on 2019/1/23.
 * <p>
 * 通过Socket发送音频数据
 */

public class PCMSocketSender extends Thread {

    private boolean mLoop = true;
    private Queue<byte[]> mData;
    private PCMSocketConnecteListener mPCMSocketConnecteListener;
    private CopyOnWriteArrayList<Socket> mClientSockets;
    private Object mLock = new Object();

    public PCMSocketSender(CopyOnWriteArrayList<Socket> sockets) {
        mClientSockets = sockets;
    }

    /**
     * 设置要发送的数据
     */
    public void setPCMData(Queue queue) {
        mData = queue;
        if (mClientSockets != null && mClientSockets.size() > 0 && mData != null){
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            while (mLoop) {
                DDLog.i(PCMSocketSender.class, "looping...");
                if (mClientSockets == null || mClientSockets.size() <= 0 || mData == null) {
                    DDLog.i(PCMIPCSender.class, "waiting...");
                    synchronized (mLock) {
                        mLock.wait();
                    }
                }
                byte[] bytes;
                if (mData != null && (bytes = mData.pop()) != null) {
                    for (Socket socket : mClientSockets) {
                        OutputStream os = socket.getOutputStream();
                        os.write(bytes);
                        os.flush();
//                        os.close();
                    }
                    if (bytes[0] == 0x7f && bytes[1] == 0x7f && bytes[2] == 0x7f && bytes[3] == 0x7f) {
                        DDLog.i(PCMSocketSender.class, "end of stream ,stop");
                        break;
                    }
                }
                Thread.sleep(20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnPCMSocketConnecteListener(PCMSocketConnecteListener listener) {
        mPCMSocketConnecteListener = listener;
    }

    public void notifySocketChanged(CopyOnWriteArrayList<Socket> sockets) {
        DDLog.i(PCMSocketSender.class, "notifySocketChanged");
        mClientSockets = sockets;
        if (mClientSockets != null && mClientSockets.size() > 0 && mData != null){
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    public interface PCMSocketConnecteListener {
        void onConnect();
    }
}
