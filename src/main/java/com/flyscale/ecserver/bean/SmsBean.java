package com.flyscale.ecserver.bean;

/**
 * Created by bian on 2018/12/17.
 * 该类为自己用的
 * SmsInfo是与EC通信用的，因为他们定义的字段名太胡扯了
 */

public class SmsBean extends EventInfo{

    public String _id = "";
    public String thread_id = "";//序号，同一发信人的id相同
    public String smsAddress = "";
    public String smsBody = "";
    public String read = "";    //0-未读；1-已读
    public int action = 0;// 1代表设置为已读，2表示删除短信

    @Override
    public String toJson() {
        return null;
    }

    @Override
    public String toString() {
        return "SmsBean [_id=" + _id + ", thread_id=" + thread_id
                + ", smsAddress=" + smsAddress + ", smsBody=" + smsBody
                + ", readHeadLine=" + read + ", action=" + action + "]";
    }


}
