package com.flyscale.ecserver.bean;

/**
 * Created by bian on 2018/12/10.
 */

public abstract class EventInfo {
    public String EventType;
    public String EventValue;

    public abstract String toJson();
    @Override
    public String toString() {
        return "EventInfo{" +
                "EventType='" + EventType + '\'' +
                ", EventValue='" + EventValue + '\'' +
                '}';
    }
}
