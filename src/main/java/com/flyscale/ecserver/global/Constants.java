package com.flyscale.ecserver.global;

/**
 * Created by bian on 2018/12/10.
 */

public class Constants {

    //EventType
    public static final String EVENT_TYPE_DIALER = "1";  //拨打电话号码
    public static final String EVENT_TYPE_GETDEVICEINFO = "2";   //获取设备信息
    public static final String EVENT_TYPE_RECODERINFO = "3";     //设置录音的时间类型
    public static final String EVENT_TYPE_SUFFIXNUMBER = "4";    //拨打分机号
    public static final String EVENT_TYPE_ANSWERCALL = "5";  //控制接听电话
    public static final String EVENT_TYPE_ENDCALL = "6";     //控制挂断电话
    public static final String EVENT_TYPE_CALLSTATE = "7";   //返回电话状态
    public static final String EVENT_TYPE_KEYF3 = "8";       //电话手柄的动作
    public static final String EVENT_TYPE_KEYCALL = "9";     //免提动作
    public static final String EVENT_TYPE_SIMCARD = "10";    //获取 SIM 卡类型
    public static final String EVENT_TYPE_OPENSPEAKERON = "11";  //开启免提
    public static final String EVENT_TYPE_QUERYSMS = "12";   //查询系统所有短信
    public static final String EVENT_TYPE_NEWSMS = "13";     //有新短信
    public static final String EVENT_TYPE_SENDSMS = "14";        //发送短信
    public static final String EVENT_TYPE_INSTALLAPP = "15";     //更新Servic app
    public static final String EVENT_TYPE_HIDEDIALNUMBER = "16"; //隐藏号码

    //CALL STATE
    public static final String CALL_STATE_IDLE = "0";    //无通话行为
    public static final String CALL_STATE_RINGING_IN = "1";  //来电响铃
    public static final String CALL_STATE_OFFHOOK_IN = "2";  //来电接听
    public static final String CALL_STATE_RINGING_OUT = "3"; //去电响铃
    public static final String CALL_STATE_INCALL = "4";

    //Phone State
    public static final String PHONE_STATE_UNACCESS = "0";   //电话状态不可用
    public static final String PHONE_STATE_IDLE = "1";   //空闲状态
    public static final String PHONE_STATE_OFFHOOK_IN = "2";    //来电接听
    public static final String PHONE_STATE_RING_IN = "3";    //有来电呼入
    public static final String PHONE_STATE_WAIT = "4";   //电话等待状态

    public static final String PHONE_STATE_RINGING_OUT = "5"; //正在拨打电话
    public static final String PHONE_STATE_SHOW_UI = "6";    //正在显示UI
    public static final String PHONE_STATE_OFFHOOK = "7";    //正在接听
    public static final String PHONE_STATE_HANGUP = "8";     //正在挂断
    public static final String PHONE_STATE_DISCONNECT = "9";    //已经挂断


    public static final String KEEP_ALIVE_TYPE = "101"; //保活

    public static final String ACK = "ack";
    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";

    public static final String CMD_EVENT_TYPE = "EventType";
    public static final String CMD_EVENT_VALUE = "EventValue";
    public static final String CMD_CALL_NUMBER = "CallNumber";

    public static final String AT_CLIENT_SP = "at_client_sp";
    public static final String FLAG_CALL_STATE = "call_state";
    public static final int LOCAL_PORT = 9000;
    public static final String SP_CALL_ID = "sp_call_id";
    public static final String SP_RECORDER_PATH = "sp_recorder_path";
    public static final String SMS_BASE_URI = "content://sms";
    public static final String SMS_INBOX_URI = "content://sms/inbox";
    public static final String SMS_DELIVER_INTENT = "android.provider.Telephony.SMS_DELIVER";
    public static final String SMS_RECEIVED_INTENT = "android.provider.Telephony.SMS_RECEIVED";
    public static final String FLYSCALE_PHONE_STATE_INTENT = "com.android.phone.FLYSCALE_PHONE_STATE";
    public static final String USB_STATE_INTENT = "android.hardware.usb.action.USB_STATE";

    public static final String SEND_MSG_SUCCESS = "1";
}
