package com.flyscale.ecserver.service;

import com.EC.service.IDataInfo;
import com.flyscale.ecserver.recorder.Queue;
import com.flyscale.ecserver.util.DDLog;


/**
 * Created by bian on 2019/1/23.
 * <p>
 * 通过Binder发送音频数据
 */

public class PCMIPCSender extends Thread {

    private boolean mLoop = true;
    private Queue<byte[]> mData;
    private IDataInfo mIDataInfo;
    private Object mLock = new Object();

    public PCMIPCSender(IDataInfo iDataInfo) {
        mIDataInfo = iDataInfo;

    }

    public void setLoop(boolean loop) {
        DDLog.i(PCMIPCSender.class, "loop=" + loop);
        mLoop = loop;
    }

    /**
     * 设置要发送的数据
     */
    public void setPCMData(Queue queue) {
        DDLog.i(PCMIPCSender.class, "setPCMData,queue=" + queue);
        mData = queue;
        if (mIDataInfo != null && mData != null) {
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    @Override
    public void run() {
        DDLog.i(PCMIPCSender.class, "run");
        super.run();
        try {
            while (mLoop) {
//                DDLog.i(PCMIPCSender.class, "looping...");

                //IDataInfo和缓存都不为空的时候才进行数据的发送，否则等待
                if (mIDataInfo == null || mData == null) {
                    DDLog.i(PCMIPCSender.class, "waiting...");
                    synchronized (mLock) {
                        mLock.wait();
                    }
                }
                byte[] bytes;
                if (mData != null && (bytes = mData.pop()) != null && mIDataInfo != null) {
                    DDLog.i(PCMIPCSender.class, "send data,length=" + bytes.length);
                    mIDataInfo.getAudioData(bytes, bytes.length);
                    if (bytes[0] == 0x7f && bytes[1] == 0x7f && bytes[2] == 0x7f && bytes[3] == 0x7f) {
                        DDLog.i(PCMIPCSender.class, "end of stream ,stop");
                        mLoop = false;
                        break;
                    }
                }
                Thread.sleep(20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setIDataInfo(IDataInfo iDataInfo) {
        mIDataInfo = iDataInfo;
        if (mIDataInfo != null && mData != null) {
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

}
