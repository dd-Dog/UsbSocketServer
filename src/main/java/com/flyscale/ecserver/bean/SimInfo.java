package com.flyscale.ecserver.bean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bian on 2018/12/10.
 */

public class SimInfo extends EventInfo {

    /**
     * SimTypeName=1：中国移动
     SimTypeName=2：中国联通
     SimTypeName=3：中国电信
     */
    public String SimTypeName;
    /**
     * SimState =“0”    Sim 卡状态未知
     SimState =“1”  未插入 Sim 卡
     SimState =“2”  需要 pin 解锁
     SimState =“3”  需要 puk 解锁
     SimState =“4”  需要 networkpin 解锁
     SimState =“5”  良好
     */
    public String SimState;

    @Override
    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("EventType", EventType);
            jsonObject.put("SimTypeName", SimTypeName);
            jsonObject.put("SimState", SimState);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
