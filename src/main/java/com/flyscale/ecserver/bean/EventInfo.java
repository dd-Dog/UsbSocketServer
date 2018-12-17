package com.flyscale.ecserver.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/10.
 */

public class EventInfo {
    public String EventType;
    public String EventValue;

    public EventInfo() {
    }

    public EventInfo(String eventType, String eventValue) {
        EventType = eventType;
        EventValue = eventValue;
    }

    public EventInfo(String eventType) {
        EventType = eventType;
    }

    public String toJson(){
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("EventType", EventType);
            jsonObject.put("EventValue", EventValue);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public String toString() {
        return "EventInfo{" +
                "EventType='" + EventType + '\'' +
                ", EventValue='" + EventValue + '\'' +
                '}';
    }
}
