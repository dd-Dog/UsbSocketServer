package com.flyscale.ecserver.util;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by bian on 2018/8/17.
 */

public class StorageUtil {
    /**
     * 获得SD卡总大小
     *
     * @return
     */
    public static String getSDTotalSize(Context context) {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(context, blockSize * totalBlocks);
    }

    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    public static String getSDAvailableSize(Context context) {
        long size = getSDAvailableByteSize(context);
        return Formatter.formatFileSize(context, size);
    }

    public static long getSDAvailableByteSize(Context context) {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return blockSize * availableBlocks;
    }


    /**
     * 获得用户空间总大小
     *
     * @return
     */
    public static String getUserDataTotalSize(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(context, blockSize * totalBlocks);
    }

    /**
     * 获得用户空间可用大小
     *
     * @return
     */
    public static String getUserDataAvailableSize(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, blockSize * availableBlocks);
    }

    /**
     * @param sysPath
     * 为节点映射到的实际路径
     * @return
     */
    public static String readHeadLine(String sysPath) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("cat " + sysPath); // 此处进行读操作
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while (null != (line = br.readLine())) {
                DDLog.w(StorageUtil.class, "readHeadLine data:" + line);
                return line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            DDLog.w(StorageUtil.class, e.getMessage());
        }
        return null;
    }

}
