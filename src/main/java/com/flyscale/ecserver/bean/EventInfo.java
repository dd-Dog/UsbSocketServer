package com.flyscale.ecserver.bean;

/**
 * Created by bian on 2018/12/10.
 */

public class EventInfo {
    public String EventType;
    public String EventValue;

    @Override
    public String toString() {
        return "EventInfo{" +
                "EventType='" + EventType + '\'' +
                ", EventValue='" + EventValue + '\'' +
                '}';
    }
}
