package com.flyscale.ecserver.bean;

/**
 * Created by bian on 2018/12/10.
 */

public class CallInfo extends EventInfo {
    public String CallState;    //来电去电状态
    public String PhoneState;   //返回电话状态:响铃,接听,挂断等状态
    public String CallNumber;   //设置拨打电话号码
    public String CallTime;     //返回接通电话时间（暂时未返回时间）
    public String CallId;       //电话 id
    public String RecoderPath;  //返回录音文件路径
    public String EventType;    //事件类型
}
