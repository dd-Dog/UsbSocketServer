package com.flyscale.ecserver.observer;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.flyscale.ecserver.bean.SmsBean;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.util.DDLog;

/**
 * Created by bian on 2018/12/17.
 * 短信数据库观察者
 */

public class SmsObserver extends ContentObserver {

    private ContentResolver mResolver;
    public Handler smsHandler;

    public SmsObserver(ContentResolver mResolver, Handler handler) {
        super(handler);
        this.mResolver = mResolver;
        this.smsHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        DDLog.i(SmsObserver.class, "sms database changed...");
        Cursor mCursor = mResolver.query(Uri.parse(Constants.SMS_INBOX_URI),
                new String[] { "_id", "address", "readHeadLine", "body", "thread_id" },
                //获取未读短信
                "readHeadLine=?", new String[] { "0" }, "date desc");

        if (mCursor == null) {
            return;
        } else {
            while (mCursor.moveToNext()) {
                SmsBean smsInfo = new SmsBean();

                int _inIndex = mCursor.getColumnIndex("_id");
                if (_inIndex != -1) {
                    smsInfo._id = mCursor.getString(_inIndex);
                }

                int thread_idIndex = mCursor.getColumnIndex("thread_id");
                if (thread_idIndex != -1) {
                    smsInfo.thread_id = mCursor.getString(thread_idIndex);
                }

                int addressIndex = mCursor.getColumnIndex("address");
                if (addressIndex != -1) {
                    smsInfo.smsAddress = mCursor.getString(addressIndex);
                }

                int bodyIndex = mCursor.getColumnIndex("body");
                if (bodyIndex != -1) {
                    smsInfo.smsBody = mCursor.getString(bodyIndex);
                }

                int readIndex = mCursor.getColumnIndex("readHeadLine");
                if (readIndex != -1) {
                    smsInfo.read = mCursor.getString(readIndex);
                }

                // 根据你的拦截策略，判断是否不对短信进行操作;将短信设置为已读;将短信删除
                DDLog.i(SmsObserver.class, "获取的短信内容为："+smsInfo.toString());
                Message msg = smsHandler.obtainMessage();
                smsInfo.action = 2;// 0不对短信进行操作;1将短信设置为已读;2将短信删除
                msg.obj = smsInfo;
                smsHandler.sendMessage(msg);
            }
        }
        mCursor.close();
    }
}
