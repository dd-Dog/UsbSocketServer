package com.flyscale.ecserver.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/17.
 */

public class SmsInfo extends EventInfo {

    public static final String TYPE_RECEIVE = "1";
    public static final String TYPE_SEND = "2";
    public String Id;   //短信ID
    public String Sms;  //短信内容
    public String Type; //短信类型，收-1，发-2
    public String PhoneNumber;  //号码
    public String Time; //时间
    private long date;
    private int read;
    private String person;
    private String serviceCenter;

    @Override
    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Sms", Sms);
            jsonObject.put("Id", Id);
            jsonObject.put("Type", Type);
            jsonObject.put("PhoneNumber", PhoneNumber);
            jsonObject.put("Time", Time);
            jsonObject.put("EventType", EventType);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject toJsonObj(){
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Sms", Sms);
            jsonObject.put("Id", Id);
            jsonObject.put("Type", Type);
            jsonObject.put("PhoneNumber", PhoneNumber);
            jsonObject.put("Time", Time);
            jsonObject.put("EventType", EventType);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setDate(long date) {
        this.Time = date + "";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.PhoneNumber = phoneNumber;
    }

    public void setSmsbody(String smsbody) {
        this.Sms = smsbody;
    }

    public void setType(String type) {
        this.Type = type;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public void setId(int id) {
        this.Id = id + "";
    }

    public void setService_center(String service_center) {
        this.serviceCenter = service_center;
    }
}
