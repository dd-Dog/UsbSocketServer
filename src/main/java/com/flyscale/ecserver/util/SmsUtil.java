package com.flyscale.ecserver.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.flyscale.ecserver.bean.SmsInfo;

import java.util.ArrayList;


/**
 * Created by MrBian on 2018/1/13.
 */

public class SmsUtil {

    /*
    * _id：短信序号，如100 　　
* 　　thread_id：对话的序号，如100，与同一个手机号互发的短信，其序号是相同的 　　
* 　　address：发件人地址，即手机号，如+8613811810000 　　
* 　　person：发件人，如果发件人在通讯录中则为具体姓名，陌生人为null 　　
* 　　date：日期，long型，如1256539465022，可以对日期显示格式进行设置 　　
* 　　protocol：协议0SMS_RPOTO短信，1MMS_PROTO彩信
* 　　read：是否阅读0未读，1已读 　　
* 　　status：短信状态-1接收，0complete,64pending,128failed 　　
* 　　type：1：inbox  2：sent 3：draft  4：outbox  5：failed  6：queued 　　 　　
* 　　body：短信具体内容 　　
* 　　service_center：短信服务中心号码编号，如+8613800755500
     */
    private static final String TAG = "SmsUtil";
    /**
     * 所有的短信
     */
    public static final String SMS_URI_ALL = "content://sms/";
    /**
     * 收件箱短信
     */
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    /**
     * 已发送短信
     */
    public static final String SMS_URI_SEND = "content://sms/sent";
    /**
     * 草稿箱短信
     */
    public static final String SMS_URI_DRAFT = "content://sms/draft";
    /**
     * 发送失败
     */
    public static final String SMS_URI_FAILED = "content://sms/failed";
    /**
     * 待发送列表
     */
    public static final String SMS_URI_QUEUED = "content://sms/queued";

    /**
     * 获取短信的各种信息
     */
    public static ArrayList<SmsInfo> getSmsInfo(Context context, String uri) {
        ArrayList<SmsInfo> infos = new ArrayList<SmsInfo>();
        String[] projection = new String[]{"_id", "address", "person",
                "body", "date", "type", "read", "service_center"};
        if (TextUtils.equals(uri, SMS_URI_DRAFT)) {
            projection = new String[]{"_id", "address", "person",
                    "body", "date", "type", "read", "service_center"};
        }
        Cursor cusor = context.getContentResolver().query(Uri.parse(uri), projection, null, null,
                "date desc");
        int id = cusor.getColumnIndex("_id");
        int phoneNumberColumn = cusor.getColumnIndex("address");
        int smsbodyColumn = cusor.getColumnIndex("body");
        int dateColumn = cusor.getColumnIndex("date");
        int typeColumn = cusor.getColumnIndex("type");
        int read = cusor.getColumnIndex("read");
        int person = cusor.getColumnIndex("person");
        int service_center = cusor.getColumnIndex("service_center");
        while (cusor.moveToNext()) {
            SmsInfo smsinfo = new SmsInfo();
            smsinfo.setDate(cusor.getLong(dateColumn));
            smsinfo.setPhoneNumber(cusor.getString(phoneNumberColumn));
            smsinfo.setSmsbody(cusor.getString(smsbodyColumn));
            smsinfo.setType(cusor.getString(typeColumn));
            smsinfo.setRead(cusor.getInt(read));
            smsinfo.setPerson(cusor.getString(person));
            smsinfo.setId(cusor.getInt(id));
            smsinfo.setService_center(cusor.getString(service_center));
            infos.add(smsinfo);
        }
        cusor.close();
        DDLog.d(SmsUtil.class, "uri=" + uri + ",infos=" + infos);
        return infos;
    }
/*
    public static void setRead(Activity activity, String uri, String id) {
        Log.d(TAG, "id=" + id + ", uri=" + uri);
        if (!SmsWriteOpUtil.isWriteEnabled(activity)) {
            SmsWriteOpUtil.setWriteEnabled(activity, true);
        }
        ContentValues values = new ContentValues();
        values.put("read", "1");
        int update = activity.getContentResolver().update(Uri.parse(uri), values,
                "_id=?", new String[]{id});
        Log.d(TAG, "udpate=" + update);

    }*/
/*
    public static boolean delete(Activity activity, String uri, String smsId) {

        if (!SmsWriteOpUtil.isWriteEnabled(activity)) {
            SmsWriteOpUtil.setWriteEnabled(activity, true);
        }
        Log.d(TAG, "uri=" + uri);
        int delete = activity.getContentResolver().delete(Uri.parse(SMS_URI_ALL), "_id=?", new String[]{smsId});
        Log.d(TAG, "delete=" + delete);
        return delete != 0;
    }*/

    public static int[] getInBoxSmsCount(Activity activity, String uri) {
        int[] count = new int[2];
        int allCount = 0;
        int unReadCount = 0;
        String[] projection = new String[]{"_id", "read"};
        Cursor cusor = activity.getContentResolver().query(Uri.parse(uri), projection, null, null,
                "date desc");
        allCount = cusor.getCount();
        if (cusor != null) {
            while (cusor.moveToNext()) {
                int read = cusor.getInt(cusor.getColumnIndex("read"));
                if (read == 0) {
                    unReadCount++;
                }
            }
            cusor.close();
        }
        count[0] = allCount;
        count[1] = unReadCount;
        return count;
    }

    public static int getDraftBoxCount(Activity activity, String uri) {
        int allCount = 0;
        String[] projection = new String[]{"_id"};
        Cursor cusor = activity.getContentResolver().query(Uri.parse(uri), projection, null, null,
                "date desc");
        allCount = cusor.getCount();
        cusor.close();
        return allCount;
    }

    public static int getSendBoxCount(Activity activity, String uri) {
        return getDraftBoxCount(activity, uri);
    }


    /***
     * 1)	byte[] smsc : 短信服务中心的地址，个人觉得在拷贝到SIM卡过程中能够为空。
     * 2)	byte[] pdu : 中文翻译是协议数据单元，这个參数最为重要，一会我们会做具体地解释说明。
     * 3)	int status : 短信存储在Icc卡上的状态。有4种状态，1是已读，3是未读，5是已发送。7是未发送。
     */
//    public boolean writeSMStoIcc(byte[] smsc, byte[] pdu, int status) {
////		mKeyboardHelper = new ReflectionInternal(this,"android.telephony.SmsManager");
//        //调用类，声明类，mKeyboardView。mPasswordEntry，为须要传递的參数
////		mKeyboardHelper.setInt("copyMessageToIcc", 0);
//        boolean flag = false;
//        SmsManager newSmsManager = SmsManager.getDefault();
//        try {
//            Class smsManagerClass = Class.forName("android.telephony.SmsManager");
//            Method localMethod = smsManagerClass.getMethod("copyMessageToIcc", new Class[]{byte[]
//                    .class, byte[].class, Integer.TYPE});
//            Object[] arrayList = new Object[3];
//            arrayList[0] = smsc;
//            arrayList[1] = pdu;
//            arrayList[2] = status;
//            try {
//                flag = ((Boolean) localMethod.invoke(newSmsManager, arrayList)).booleanValue();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException ex) {
//            ex.printStackTrace();
//        }
//        return flag;
//    }

    /**
     * android 读sim卡短信
     */
//    public static ArrayList<SmsMessage> getSmsList() {
//        ArrayList<SmsMessage> list = new ArrayList<SmsMessage>();
//        SmsManager newSmsManager = SmsManager.getDefault();
//        try {
//            Class<?> smsManagerClass = Class.forName("android.telephony.SmsManager");
//            Method localMethod = smsManagerClass.getMethod("getAllMessagesFromIcc", null);
//            try {
//                list = (ArrayList<SmsMessage>) localMethod.invoke(newSmsManager, null);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException ex) {
//            ex.printStackTrace();
//        }
//
//        return list;
//    }

    /*public static boolean addDraft(Activity activity, String uri, String msg) {
        ContentValues values = new ContentValues();
        values.put("type", 3);
        values.put("date", System.currentTimeMillis());
        values.put("body", msg);
        Uri insert = activity.getContentResolver().insert(Uri.parse(uri), values);

        DraftDAO draftDAO = new DraftDAO(activity);
        draftDAO.insert(msg);
        Log.d(TAG, "addDraft::body=" + msg + "insert=" + insert);
        return insert != null;
    }*/


/*    public static boolean addDraft(Activity activity, String msg) {
        DraftDAO draftDAO = new DraftDAO(activity);
        boolean insert = draftDAO.insert(msg);
        Log.d(TAG, "addDraft::body=" + msg + "insert=" + insert);
        return insert;
    }*/


    public static ArrayList<SmsInfo> getDrafts(Activity activity, String uri) {
        ArrayList<SmsInfo> infos = new ArrayList<SmsInfo>();
        String[] projection = new String[]{"_id", "body"};
        Cursor cusor = activity.getContentResolver().query(Uri.parse(uri), projection, null, null,
                "date desc");
        int smsbodyColumn = cusor.getColumnIndex("body");
        if (cusor != null) {
            while (cusor.moveToNext()) {
                SmsInfo smsinfo = new SmsInfo();
                smsinfo.setSmsbody(cusor.getString(smsbodyColumn));
                infos.add(smsinfo);
            }
            cusor.close();
        }
        return infos;
    }
}
