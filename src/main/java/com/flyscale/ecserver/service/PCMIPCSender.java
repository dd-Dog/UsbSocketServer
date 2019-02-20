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
 * <p>
 * 通过Binder发送音频数据
 */

public class PCMIPCSender extends Thread {

    private boolean mLoop = true;
    private Queue<byte[]> mData;
    private IDataInfo mIDataInfo;

    public PCMIPCSender(IDataInfo iDataInfo) {
        mIDataInfo = iDataInfo;
    }

    public void setLoop(boolean loop){
        DDLog.i(PCMIPCSender.class, "loop=" + loop);
        mLoop = loop;
    }
    /**
     * 设置要发送的数据
     */
    public void setPCMData(Queue queue) {
        DDLog.i(PCMIPCSender.class, "setPCMData,queue=" + queue);
        mData = queue;
    }

    @Override
    public void run() {
        DDLog.i(PCMIPCSender.class, "run");
        super.run();
        try {
            while (mLoop) {
                byte[] bytes;
                if (mData != null && (bytes = mData.pop()) != null && mIDataInfo != null) {
                    DDLog.i(PCMIPCSender.class, "send data,length=" + bytes.length);
                    mIDataInfo.getAudioData(bytes, bytes.length);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setIDataInfo(IDataInfo iDataInfo) {
        mIDataInfo = iDataInfo;
    }

}
