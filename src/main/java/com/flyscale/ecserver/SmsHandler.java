package com.flyscale.ecserver;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.flyscale.ecserver.bean.SmsBean;

/**
 * Created by bian on 2018/12/17.
 */

public class SmsHandler extends Handler {
    private Context mcontext;

    public SmsHandler(Context context) {
        this.mcontext = context;
    }

    @Override
    public void handleMessage(Message msg) {
        SmsBean smsInfo = (SmsBean) msg.obj;

        if (smsInfo.action == 1) {
            ContentValues values = new ContentValues();
            values.put("readHeadLine", "1");
            mcontext.getContentResolver().update(Uri.parse("content://sms/inbox"),
                    values, "thread_id=?", new String[]{smsInfo.thread_id});
        } else if (smsInfo.action == 2) {
            Uri mUri = Uri.parse("content://sms/");
            mcontext.getContentResolver().delete(mUri, "_id=?", new String[]{smsInfo._id});
        }
    }
}
