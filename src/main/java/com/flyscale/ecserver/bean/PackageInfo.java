package com.flyscale.ecserver.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/27.
 */

public class PackageInfo extends EventInfo {

    public String versionName;
    public String processName;
    public String dataDir;


    @Override
    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("versionName", versionName);
            jsonObject.put("processName", processName);
            jsonObject.put("dataDir", dataDir);
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
        return "PackageInfo{" +
                "versionName='" + versionName + '\'' +
                ", processName='" + processName + '\'' +
                ", EventType='" + EventType + '\'' +
                ", dataDir='" + dataDir + '\'' +
                ", EventValue='" + EventValue + '\'' +
                '}';
    }
}
