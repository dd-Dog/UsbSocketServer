package com.flyscale.ecserver.bean;

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
