package com.flyscale.ecserver.bean;

import com.flyscale.ecserver.global.Constants;

import org.json.JSONException;
import org.json.JSONObject;


public class KeepAliveInfo extends EventInfo {

    public static final String KEEP_ALIVE_SERVER = "server";
    public static final String KEEP_ALIVE_CLIENT = "client";
    public long syn;
    public String host;

    public KeepAliveInfo(long syn, String host) {
        this.syn = syn;
        EventType = Constants.KEEP_ALIVE_TYPE;
        this.host = host;
    }

    @Override
    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("syn", syn);
            jsonObject.put("host", host);
            jsonObject.put("EventType", EventType);
            jsonObject.put("EventValue", EventValue);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
