package com.flyscale.ecserver.service;

import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.recorder.Queue;
import com.flyscale.ecserver.util.DDLog;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by bian on 2019/1/23.
 */

public class PCMSender extends Thread {

    private boolean mLoop = true;
    private boolean mConnected = true;
    private ServerSocket mServerSocket;
    private Socket mClientSocket;
    private Queue<byte[]> mData;
    private PCMSocketConnecteListener mPCMSocketConnecteListener;

    public PCMSender(ServerSocket serverSocket) {
        mServerSocket = serverSocket;
    }

    /**
     * 设置要发送的数据
     */
    public void setPCMData(Queue queue) {
        mData = queue;
    }

    @Override
    public void run() {
        super.run();
        try {
            if (mServerSocket == null){
                mServerSocket = new ServerSocket(Constants.LOCAL_PORT_STREAM);
            }
            while (mLoop) {
                DDLog.i(PCMSender.class, "server is waiting for client...");
                mClientSocket = mServerSocket.accept();
                DDLog.i(PCMSender.class, "accept a client");
                if (mPCMSocketConnecteListener != null) {
                    mPCMSocketConnecteListener.onConnect();
                }
                OutputStream os = mClientSocket.getOutputStream();
                while (mConnected) {
                    byte[] bytes;
                    if (mData != null && (bytes = mData.pop()) != null) {
                        os.write(bytes);
                    }
                }
                os.flush();
                os.close();
                mClientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnPCMSocketConnecteListener(PCMSocketConnecteListener listener) {
        mPCMSocketConnecteListener = listener;
    }

    public interface PCMSocketConnecteListener {
        void onConnect();
    }
}
