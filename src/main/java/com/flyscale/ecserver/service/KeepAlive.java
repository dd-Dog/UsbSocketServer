package com.flyscale.ecserver.service;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.flyscale.ecserver.bean.KeepAliveInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.JsonUtil;

import org.json.JSONObject;
import org.json.JSONTokener;


public class KeepAlive {
    private Handler mHandler;
    private State mState;   //当前连接状态
    private State mKeepAlivePkgState;   //最近一次发送保活数据包的状态
    private ClientListenerThread mListenerThread;
    //保活间隔
    private static final int KEEP_ALIVE_INTERVAL = 3 * 1000;
    //保活超时时间
    private static final int KEEP_ALIVE_TIMEOUT = 3 * 1000;

    //发送保活
    private static final int MSG_KEEP_ALVIE_SEND = 2001;
    //接收保活
    private static final int MSG_KEEP_ALVIE_RECEV = 2002;
    //超时消息
    private static final int MSG_KEEP_ALVIE_TIMEOUT = 2003;
    private KeepAliveInfo mLastKeepAlivePkg;

    public KeepAlive() {
        mHandler = new KeepAliveHandler();
    }

    public void setListenerThread(ClientListenerThread thread){
        mListenerThread = thread;
        mListenerThread.addHandler(mHandler);
    }


    private class KeepAliveHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_KEEP_ALVIE_SEND:
                    DDLog.i(KeepAlive.class, "MSG_KEEP_ALVIE_SEND");
                    mLastKeepAlivePkg = new KeepAliveInfo(System.currentTimeMillis(), KeepAliveInfo.KEEP_ALIVE_SERVER);
                    //1.发送一个保活数据包
                    mListenerThread.sendMsg2Client(mLastKeepAlivePkg.toJson());
                    //2.发送超时接收的消息
                    mHandler.sendEmptyMessageDelayed(MSG_KEEP_ALVIE_TIMEOUT, KEEP_ALIVE_TIMEOUT);
                    //3.设置数据包的状态已发送，等待回应
                    mKeepAlivePkgState = State.WAITING_RESPONSE;

                    //4.延时发送下一个数据包
                    sendEmptyMessageDelayed(MSG_KEEP_ALVIE_SEND, KEEP_ALIVE_INTERVAL);
                    break;
                case MSG_KEEP_ALVIE_RECEV:
                    DDLog.i(KeepAlive.class, "MSG_KEEP_ALVIE_RECEV");
                    //接收到响应包
                    //1.设置当前数据包状态：RESPONSED
                    mKeepAlivePkgState = State.RESPONSED;
                    //2.设置当前连接状态ALIVE
                    mState = State.ALIVE;
                    //取消发送超时消息
                    removeMessages(MSG_KEEP_ALVIE_TIMEOUT);
                    break;
                case MSG_KEEP_ALVIE_TIMEOUT:
                    DDLog.i(KeepAlive.class, "MSG_KEEP_ALVIE_TIMEOUT");
                    //保活超时
                    //1.设置数据包状态：NO_RESPONSE
                    mKeepAlivePkgState = State.NO_RESPONSE;
                    //2.设置当前连接状态：DIED
                    mState = State.DIED;
                    //3.取消发送保活数据包
                    removeMessages(MSG_KEEP_ALVIE_SEND);
                    //4.删除handler
                    mListenerThread.removeHandler(mHandler);
                    break;
                case ClientListenerThread.MSG_FROM_CLIENT:
                    String cmdStr = (String) msg.obj;
                    DDLog.i(KeepAlive.class, "cmdStr=" + cmdStr);
                    try {
                        if (JsonUtil.isJson(cmdStr, 0)) {
                            JSONObject cmdObj = (JSONObject) new JSONTokener(cmdStr).nextValue();
                            String eventType = cmdObj.getString(Constants.CMD_EVENT_TYPE);
                            DDLog.i(ServerService.class, "EventType=" + eventType);
                            if (eventType.equals(Constants.KEEP_ALIVE_TYPE)) {
                                if (TextUtils.equals(cmdObj.getString("host"), KeepAliveInfo.KEEP_ALIVE_CLIENT) &&
                                        TextUtils.equals(mLastKeepAlivePkg.syn + "", cmdObj.getString("syn"))) {
                                    sendEmptyMessage(MSG_KEEP_ALVIE_RECEV);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }
    }

    public void startKeepAlive() {
        DDLog.i(KeepAlive.class, "startKeepAlive");
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(MSG_KEEP_ALVIE_SEND, KEEP_ALIVE_INTERVAL);
        }
    }

    public void stopKeepAlive() {
        DDLog.i(KeepAlive.class, "stopKeepAvlive");
        if (mHandler != null) {
            mHandler.removeMessages(MSG_KEEP_ALVIE_SEND);
            mHandler.removeMessages(MSG_KEEP_ALVIE_TIMEOUT);
            mHandler.removeMessages(MSG_KEEP_ALVIE_RECEV);
        }
    }

    public boolean isClientAlive() {
        DDLog.i(KeepAlive.class, "isClientAlive,mState=" + mState);
        return mState == State.ALIVE;
    }

    public State getState() {
        return mState;
    }

    enum State {
        ALIVE,
        DIED,
        WAITING_RESPONSE,
        RESPONSED,
        NO_RESPONSE
    }


}
