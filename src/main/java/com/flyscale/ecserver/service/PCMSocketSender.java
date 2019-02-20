package com.flyscale.ecserver.service;

import com.flyscale.ecapp.IDataInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.recorder.Queue;
import com.flyscale.ecserver.util.DDLog;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by bian on 2019/1/23.
 *
 * 通过Socket发送音频数据
 */

public class PCMSocketSender extends Thread {

    private boolean mLoop = true;
    private boolean mConnected = false;
    private ServerSocket mServerSocket;
    private Socket mClientSocket;
    private Queue<byte[]> mData;
    private PCMSocketConnecteListener mPCMSocketConnecteListener;

    public PCMSocketSender(ServerSocket serverSocket) {
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
                DDLog.i(PCMSocketSender.class, "server is waiting for client...");
                mClientSocket = mServerSocket.accept();
                DDLog.i(PCMSocketSender.class, "accept a client");
                mConnected = true;
                if (mPCMSocketConnecteListener != null) {
                    mPCMSocketConnecteListener.onConnect();
                }
                OutputStream os = mClientSocket.getOutputStream();
                while (mConnected) {
                    DDLog.i(PCMSocketSender.class, "looping...");
                    byte[] bytes;
                    if (mData != null && (bytes = mData.pop()) != null) {
                        os.write(bytes);
                        if(bytes[0] == 0x7f && bytes[1] == 0x7f && bytes[2] == 0x7f && bytes[3] == 0x7f) {
                            DDLog.i(PCMSocketSender.class, "end of stream ,stop");
                            break;
                        }
                    }
                    Thread.sleep(20);
                }
                os.flush();
                os.close();
                mClientSocket.close();
                mLoop = false;
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
