package com.flyscale.ecserver.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/17.
 */

public class SmsInfo extends EventInfo {

    public static final String TYPE_RECEIVE = "1";
    public static final String TYPE_SEND = "2";
    public String Id;
    public String Sms;
    public String Type;
    public String PhoneNumber;
    public String Time;

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
}
