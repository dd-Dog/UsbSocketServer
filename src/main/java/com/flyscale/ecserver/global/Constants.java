package com.flyscale.ecserver.global;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by bian on 2018/12/10.
 */

public class Constants {

    /*EventType EC定义*/
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
    public static final String CALL_STATE_RINGING_OUT = "3"; //正在拨号
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
    public static final String PHONE_STATE_DISCONNECTING = "8";     //正在挂断
    public static final String PHONE_STATE_DISCONNECT = "9";    //已经挂断

    public static final String CMD_EVENT_TYPE = "EventType";//事件类型
    public static final String CMD_EVENT_VALUE = "EventValue";//事件携带的相关信息

    public static final String CMD_CALL_NUMBER = "CallNumber";




    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";


    /*sharedpreference命名*/
    public static final String AT_CLIENT_SP = "at_client_sp";
    /*保存在sp中的通话ID*/
    public static final String SP_CALL_ID = "sp_call_id";
    /*保存在sp中的录音文件路径*/
    public static final String SP_RECORDER_PATH = "sp_recorder_path";

    public static final String FLAG_CALL_STATE = "call_state";

    /*映射的本地端口号*/
    public static final int LOCAL_PORT = 8000;

    /*上传音频流服务的本地端口号*/
    public static final int LOCAL_PORT_STREAM = 9000;

    /*信息数据库base URI*/
    public static final String SMS_BASE_URI = "content://sms";
    /*心跳保活确认消息，只在测试中使用 bianjb*/
    public static final String ACK = "ack";

    /*定义一些指令 @author bianjb*/
    public static final String EVENT_TYPE_PLAY2CALL = "102";//通话播放MP3
    public static final String EVENT_TYPE_STOP_PLAY2CALL = "112";//通话播放MP3
    public static final String EVENT_TYPE_SEND_FILE = "103";//发送文件给客户端
    public static final String EVENT_TYPE_GET_APPINFO = "104";
    public static final String KEEP_ALIVE_TYPE = "101"; //保活


    /*收件箱uri*/
    public static final String SMS_INBOX_URI = "content://sms/inbox";
    /*定义短信发送结果的广播*/
    public static final String SMS_DELIVER_INTENT = "android.provider.Telephony.SMS_DELIVER";
    /*定义对方接收到短信的广播*/
    public static final String SMS_RECEIVED_INTENT = "android.provider.Telephony.SMS_RECEIVED";
    /*系统中关于电话状态的广播*/
    public static final String FLYSCALE_PHONE_STATE_INTENT = "com.android.phone.FLYSCALE_PHONE_STATE";
    /*USB状态变化的广播*/
    public static final String USB_STATE_INTENT = "android.hardware.usb.action.USB_STATE";

    /*隐藏电话号码的广播，具体逻辑由系统完成*/
    public static final String ACTION_HIDE_NUMBER = "com.flyscale.ecserver.HIDE_NUMBER";
    /*{ACTION_HIDE_NUMBER}广播中携带是否要隐藏电话号码的标志，0表示显示，1表示 隐藏*/
    public static final String HIDE_NUMBER = "hide_number";
    public static final String HIDE_NUMBER_UNABLED = "0";
    public static final String HIDE_NUMBER_ENABLED = "1";

    /*拨号状态广播*/
    public static final String ACTION_HOOK_STATE = "com.flyscale.dialer.ACTION_HOOK_STATE";
    /*免提状态广播*/
    public static final String ACTION_HANDFREE_STATE = "com.flyscale.dialer.ACTION_HANDFREE_STATE";

    /*通话中播放指定音频*/
    public static final String ACTION_PLAY_SOUND_2MIC = "com.flyscale.ecserver.PLAY_SOUND_2MIC";
    /*升级ECServer的广播，由ECHelper完成*/
    public static final String ACTION_UPDATE_APP = "com.flyscale.ecserver.UPDATE_APP";
    /*由ECHelper发来的升级ECServer的结果广播*/
    public static final String ACTION_UPDATE_RESULT = "com.flyscale.echelper.UPDATE_RESULT";
    /*升级的结果，1表示成功，0表示失败 EC定义*/
    public static final String UPDATE_APP_SUCCESS = "1";
    public static final String UPDATE_APP_FAILED = "0";

    /*播放DTMF音的广播，用于拨打分机号，需要在phone进程中完成，由 {DTMF_STR}携带分机号*/
    public static final String PLAY_DTMF_INTENT = "com.flyscale.ecserver.PLAY_DTMF";
    /*要播放的DTMF数字*/
    public static final String DTMF_STR = "dtmf_str";

    /*电话桌面状态：3001表示拨号状态*/
    public static final String MODE_DIAL_NUM = "3001";
    /*电话桌面状态：3002表示IDLE状态*/
    public static final String MODE_IDLE = "3002";


    /*短信发送结果*/
    public static final String SEND_MSG_SUCCESS = "1";

    /*{TelephonyIntents.ACTION_PLMN_INTENT}广播中携带的plmn，再转换成运营商标识 */
    public static final String SP_PLMN_NUMBER = "sp_plmn_number";
    /*运营商定义 EC定义*/
    public static final HashMap<String, String>  OPERATOR_MAP = new HashMap<>();
    private static final String CHINA_MOBILE = "1";
    private static final String CHINA_UNICOM = "2";
    private static final String CHINA_TELECOM = "3";

    /*录音文件存储路径*/
    public static final String RECORDER_ROOT_PATH = "/storage/emulated/legacy";

    /*LOG存储相对路径*/
    public static final String LOG_RELA_PATH = "pcservice";
    /*当前log文件名*/
    public static final String LOG_FILE_NAME = "pcservice.log";
    /*重启前一次的log文件名*/
    public static final String PRELOG_FILE_NAME = "pcservice_pre.log";


    /*不同运营商对应的mcc mnc*/
    static {
        OPERATOR_MAP.put("46000", CHINA_MOBILE);
        OPERATOR_MAP.put("46001", CHINA_UNICOM);
        OPERATOR_MAP.put("46002", CHINA_MOBILE);
        OPERATOR_MAP.put("46003", CHINA_TELECOM);
        OPERATOR_MAP.put("46005", CHINA_TELECOM);
        OPERATOR_MAP.put("46006", CHINA_UNICOM);
        OPERATOR_MAP.put("46007", CHINA_MOBILE);
        OPERATOR_MAP.put("46009", CHINA_UNICOM);
        OPERATOR_MAP.put("46011", CHINA_TELECOM);
        OPERATOR_MAP.put("45431", CHINA_TELECOM);
        OPERATOR_MAP.put("45403", CHINA_TELECOM);
        OPERATOR_MAP.put("20404", CHINA_TELECOM);
    }


}
