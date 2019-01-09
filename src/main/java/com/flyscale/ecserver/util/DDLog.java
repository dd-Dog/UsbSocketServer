package com.flyscale.ecserver.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.flyscale.ecserver.global.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.flyscale.ecserver.util.DateFormatUtil.getTime4;

/**
 * Created by bian on 2018/12/10.
 */

public class DDLog {

    public static boolean APP_DBG = true; // 是否是debug模式
    private static final String LOG_ROOT_PATH = "/storage/emulated/legacy";

    private static final String CALL_LOG_FILE = LOG_ROOT_PATH + File.separator + Constants.LOG_RELA_PATH +
            File.separator + Constants.LOG_FILE_NAME;
    private static final String PRE_CALL_LOG_FILE = LOG_ROOT_PATH + File.separator + Constants.LOG_RELA_PATH +
            File.separator + Constants.PRELOG_FILE_NAME;

    public static void init(Context context, boolean bootComplete) {
        Log.i("DDLog", "init, bootComplete=" + bootComplete);
        APP_DBG = isApkDebugable(context);
        DDLog.i(DDLog.class, "EcServer::APP_DBG=" + APP_DBG);
        APP_DBG = true;
        //检查并重命名LOG文件
        String logPath = LOG_ROOT_PATH + File.separator + Constants.LOG_RELA_PATH;
        try {
            File file = new File(logPath);
            if (file.exists() && file.isDirectory()) {
                File logFile = new File(CALL_LOG_FILE);
                if (!logFile.exists()) {
                    createFile(CALL_LOG_FILE, true);
                } else {
                    Log.i("DDLog", "log file already exists!!!");
                    if (bootComplete) {
                        //如果是开机重启则重新命名文件
                        File preFile = new File(PRE_CALL_LOG_FILE);
                        if (preFile.exists()) {
                            boolean delete = preFile.delete();
                            Log.i("DDLog", "prelog file delete" + (delete ? "success!" : "failed!"));
                        }
                        boolean renameTo = logFile.renameTo(preFile);
                        Log.i("DDLog", "prelog file save" + (renameTo ? "success!" : "failed!"));
                        //创建新的Log文件
                        createFile(CALL_LOG_FILE, true);
                    }
                }
            } else {
                boolean mkdirs = file.mkdirs();
                if (mkdirs) {
                    Log.i("DDLog", "create log dir " + logPath + " success!!!");
                    createFile(CALL_LOG_FILE, true);
                } else {
                    Log.e("DDLog", "create log file failed!!!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建新文件
     *
     * @param logFile
     * @param removeOld 如果文件已经存在是否强制删除旧文件，再创建新文件
     * @throws IOException
     */
    private static boolean createFile(String logFile, boolean removeOld) throws IOException {
        File file = new File(logFile);
        if (file.exists() && !removeOld) {
            return false;
        }
        Log.i("DDLog", "log file" + logFile + " does not exists!!!");
        boolean newFile = file.createNewFile();
        if (!newFile) {
            Log.e("DDLog", "create log file failed!!!");
            return false;
        } else {
            Log.i("DDLog", "create log file success!!!");
            return true;
        }
    }

    private static void writeStr(String file, String data) {
        FileWriter fileWriter = null;
        try {
            String logPath = LOG_ROOT_PATH + File.separator + Constants.LOG_RELA_PATH;
            File path = new File(logPath);
            if (path.exists() && path.isDirectory()) {
                createFile(CALL_LOG_FILE, false);
            }else {
                boolean mkdirs = path.mkdirs();
                if (mkdirs) {
                    Log.i("DDLog", "create log dir " + logPath + " success!!!");
                    createFile(CALL_LOG_FILE, true);
                } else {
                    Log.e("DDLog", "create log file failed!!!");
                    return;
                }
            }
            File logFile = new File(file);
            if (!logFile.exists()) {
                Log.w("DDLog", "log file does not exists!!!");
                boolean newFile = logFile.createNewFile();
                if (!newFile) {
                    Log.e("DDLog", "create log file failed!!!");
                    return;
                }
            }
            fileWriter = new FileWriter(file, true);
            fileWriter.append(data);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 但是当我们没在AndroidManifest.xml中设置其debug属性时:
     * 使用Eclipse运行这种方式打包时其debug属性为true,使用Eclipse导出这种方式打包时其debug属性为法false.
     * 在使用ant打包时，其值就取决于ant的打包参数是release还是debug.
     * 因此在AndroidMainifest.xml中最好不设置android:debuggable属性置，而是由打包方式来决定其值.
     *
     * @param context
     * @return
     */
    public static boolean isApkDebugable(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void i(Class c, String msg) {
        if (APP_DBG) {
            Log.i(c.getSimpleName(), msg);
            String str = getFormatLine(c.getSimpleName(), msg);
            writeStr(CALL_LOG_FILE, str);
        }
    }

    private static String getFormatLine(String simpleName, String msg) {
        return getTime4() + "[" + simpleName + "]" + ":" + msg + "\n";
    }

    public static void d(Class c, String msg) {
        if (APP_DBG) {
            Log.d(c.getSimpleName(), msg);
            String str = getFormatLine(c.getSimpleName(), msg);
            writeStr(CALL_LOG_FILE, str);
        }
    }


    public static void w(Class c, String msg) {
        if (APP_DBG) {
            Log.w(c.getSimpleName(), msg);
            String str = getFormatLine(c.getSimpleName(), msg);
            writeStr(CALL_LOG_FILE, str);
        }
    }


    public static void e(Class c, String msg) {
        if (APP_DBG) {
            Log.e(c.getSimpleName(), msg);
            String str = getFormatLine(c.getSimpleName(), msg);
            writeStr(CALL_LOG_FILE, str);
        }
    }


}