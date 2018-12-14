package com.flyscale.ecserver.global;

/**
 * Created by bian on 2018/12/10.
 */

public class Constants {

    //EventType
    public static final int EVENT_TYPE_DIALER = 1;  //拨打电话号码
    public static final int EVENT_TYPE_GETDEVICEINFO = 2;   //获取设备信息
    public static final int EVENT_TYPE_RECODERINFO = 3;     //设置录音的时间类型
    public static final int EVENT_TYPE_SUFFIXNUMBER = 4;    //拨打分机号
    public static final int EVENT_TYPE_ANSWERCALL = 5;  //控制接听电话
    public static final int EVENT_TYPE_ENDCALL = 6;     //控制挂断电话
    public static final int EVENT_TYPE_CALLSTATE = 7;   //返回电话状态
    public static final int EVENT_TYPE_KEYF3 = 8;       //电话手柄的动作
    public static final int EVENT_TYPE_KEYCALL = 9;     //免提动作
    public static final int EVENT_TYPE_SIMCARD = 10;    //获取 SIM 卡类型
    public static final int EVENT_TYPE_OPENSPEAKERON = 11;  //开启免提
    public static final int EVENT_TYPE_QUERYSMS = 12;   //查询系统所有短信
    public static final int EVENT_TYPE_NEWSMS = 13;     //有新短信
    public static final int EVENT_TYPE_SENDSMS = 14;        //发送短信
    public static final int EVENT_TYPE_INSTALLAPP = 15;     //更新Servic app
    public static final int EVENT_TYPE_HIDEDIALNUMBER = 16; //隐藏号码

    //CALL STATE
    public static final int CALL_STATE_IDLE = 0;    //无通话行为
    public static final int CALL_STATE_RINGING_IN = 1;  //来电响铃
    public static final int CALL_STATE_OFFHOOK_IN = 2;  //来电接听
    public static final int CALL_STATE_RINGING_OUT = 3; //去电响铃
    public static final int CALL_STATE_INCALL = 4;

    //Phone State
    public static final int PHONE_STATE_UNACCESS = 0;   //电话状态不可用
    public static final int PHONE_STATE_IDLE = 1;   //空闲状态
    public static final int PHONE_STATE_OFFHOOK_IN = 2;    //来电接听
    public static final int PHONE_STATE_RING_IN = 3;    //有来电呼入
    public static final int PHONE_STATE_WAIT =4;   //电话等待状态

    public static final int PHONE_STATE_RINGING_OUT = 5; //正在拨打电话
    public static final int PHONE_STATE_SHOW_UI = 6;    //正在显示UI
    public static final int PHONE_STATE_OFFHOOK = 7;    //正在接听
    public static final int PHONE_STATE_HANGUP = 8;     //正在挂断
    public static final int PHONE_STATE_DISCONNECT = 9;    //已经挂断
    public static final String ACK = "ack";
}
