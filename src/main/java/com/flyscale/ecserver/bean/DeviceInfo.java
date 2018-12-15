package com.flyscale.ecserver.bean;

import android.util.JsonToken;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/10.
 */

public class DeviceInfo extends EventInfo {
    public String DeviceName;// 设备名字
    public String ModelName;//设备 modelname
    public String SoftVer; // 软件版本
    public String HardwareVer;// 硬件设备版本
    public String SdkVer;//sdk 版本
    public String TotalRam;//总运行内存
    public String AvailableRam;//可用运行内存
    public String TotalRom;//总的存储空间
    public String AvailableRom; // 可用存储空间
    public String SerialNumber; // 设备序列号
    public String EventType;    //事件类型

    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("DeviceName", DeviceName);
            jsonObject.put("ModelName", ModelName);
            jsonObject.put("SoftVer", SoftVer);
            jsonObject.put("HardwareVer", HardwareVer);
            jsonObject.put("SdkVer", SdkVer);
            jsonObject.put("TotalRam", TotalRam);
            jsonObject.put("AvailableRam", AvailableRam);
            jsonObject.put("TotalRom", TotalRom);
            jsonObject.put("AvailableRom", AvailableRom);
            jsonObject.put("SerialNumber", SerialNumber);
            jsonObject.put("EventType", EventType);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "DeviceName='" + DeviceName + '\'' +
                ", ModelName='" + ModelName + '\'' +
                ", SoftVer='" + SoftVer + '\'' +
                ", HardwareVer='" + HardwareVer + '\'' +
                ", SdkVer='" + SdkVer + '\'' +
                ", TotalRam='" + TotalRam + '\'' +
                ", AvailableRam='" + AvailableRam + '\'' +
                ", TotalRom='" + TotalRom + '\'' +
                ", AvailableRom='" + AvailableRom + '\'' +
                ", SerialNumber='" + SerialNumber + '\'' +
                ", EventType='" + EventType + '\'' +
                '}';
    }
}
