/**
 * Created by flyscale
 */

package com.flyscale.ecserver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.telephony.Call;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.PreferenceUtil;


public class Recorder {
    private static final String TAG = "Recorder";
    private static final boolean DBG = true;
    private static final boolean TEST_DBG = false;

    private static final int SIGNAL_STATE = 7;
    private static final int SIGNAL_ERROR = 8;
    private static final int SIGNAL_TIME = 9;
    private static final int SIGNAL_MSG = 10;

    private static final int MSG_CHECK_DISK = 10;
    private static final int MSG_START = 11;
    private static final int MSG_STOP = 12;

    private static final String DEFAULT_STORE_SUBDIR = "/voicecall";
    private static final String DEFAULT_RECORD_SUFFIX = ".amr";
    private static final int DEFAULT_OUTPUTFORMAT_TYPE = MediaRecorder.OutputFormat.AMR_NB;
    private static final String DEFAULT_MIME_TYPE = "audio/amr";
    private static final String DEFAULT_FROMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DECOLLATOR = "-";
    private static final String DEFAULT_SIM_DESCRIPTOR = "sim";

    public static final int NO_ERROR = 0;
    public static final int TYPE_ERROR_SD_ACCESS = 1;// can not access sdcard
    public static final int TYPE_ERROR_SD_NOT_EXIST = 2;// sdcard no exist
    public static final int TYPE_ERROR_SD_FULL = 3;
    public static final int TYPE_ERROR_FILE_INIT = 4;
    public static final int TYPE_ERROR_IN_RECORD = 5;// may be other application
    // is recording
    public static final int TYPE_ERROR_INTERNAL = 6;// internal error
    public static final int TYPE_MSG_PATH = 7;
    public static final int TYPE_SAVE_FAIL = 8;
    public static final int TYPE_NO_AVAILABLE_STORAGE = 9;// no available storage

    private static final long MIN_LENGTH = 512;// 512
    private static final long MINIMUM_FREE_SIZE = 6 * 1024 * 1024;
    private static final long FORCE_FREE_SIZE = MINIMUM_FREE_SIZE;
    private static final long INIT_TIME = 0;
    private static final int INVALID_SIM_SLOT_INDEX = -1;

    public String mFileName;
    private String mPhoneNumber;

    /**
     * a call back when state changed or an error occur
     **/
    public interface OnStateChangedListener {
        public void onStateChanged(State state);

        public void onTimeChanged(long time);

        public void onShowMessage(int type, String msg);
    }

    public enum State {
        IDLE, // a normal state
        ACTIVE, // when is recording
        WAITING;// a short moment when reset audio

        public boolean isActive() {
            return this == ACTIVE;
        }

        public boolean isIdle() {
            return this == IDLE;
        }

        public boolean isBlock() {
            return this == WAITING;
        }
    }

    private Context mContext;
    private static Recorder mInstance;

    private MediaRecorder mRecorder;
    private OnStateChangedListener mOnStateChangedListener;

    private State mState = State.IDLE;// to record recorder state
    private File mSampleFile = null;
    /**
     * flag a moment when the record start
     **/
    private long mStartTime = INIT_TIME;

    private AsyncThread mAsyncThread;
    private UiHandler mUiHandler;

    private Recorder(Context context) {
        mContext = context;
        //使用handlerthread，AsyncThread运行在子线程中
        HandlerThread thread = new HandlerThread("Recorder");
        thread.start();
        mAsyncThread = new AsyncThread(thread.getLooper());
        mUiHandler = new UiHandler();
    }

    private void setContext(Context context) {
        mContext = context;
    }

    /**
     * this should be single instance so that it can not be make a new one or
     * the state of the record may be wrong
     **/
    public static Recorder getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Recorder(context);
        }
        // it can not handle the old one still or the activity will be not release
        mInstance.setContext(context);
        return mInstance;
    }

    private class UiHandler extends Handler {
        private boolean updateUi;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SIGNAL_ERROR:
                case SIGNAL_MSG:
                    signalShowMessage(msg.arg1, msg.obj != null ? String.valueOf(msg.obj) : null);
                    break;
                case SIGNAL_STATE:
                    State state = (State) msg.obj;
                    signalStateChanged(state);
                    break;
                case SIGNAL_TIME:
                    signalTimeChanged(mStartTime == INIT_TIME ? 0 : SystemClock.elapsedRealtime()
                            - mStartTime);
                    update(true);
                    break;
            }
        }

        public void update(boolean force) {
            if (updateUi) {
                if (TEST_DBG)
                    Log.d(TAG, "Update ui ");
                sendMessageDelayed(obtainMessage(SIGNAL_TIME), force ? 1000 : 0);
            }
        }

        public void setUpdate(boolean update) {
            updateUi = update;
        }

        private void signalStateChanged(State state) {
            if (mOnStateChangedListener != null) {
                mOnStateChangedListener.onStateChanged(state);
            }
        }

        private void signalTimeChanged(long time) {
            if (mOnStateChangedListener != null) {
                mOnStateChangedListener.onTimeChanged(time);
            }
        }

        private void signalShowMessage(int type, String msg) {
            if (mOnStateChangedListener != null) {
                mOnStateChangedListener.onShowMessage(type, msg);
            }
        }
    }

    ;

    private class AsyncThread extends Handler {
        private boolean checkAble;

        public AsyncThread(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            DDLog.i(Recorder.class, "currentThread=" + Thread.currentThread().getName());
            switch (what) {
                case MSG_CHECK_DISK:
                    try {// check disk now
                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                       /* if (Environment.getExternalStoragePathState().equals(
                                Environment.MEDIA_MOUNTED)) {
                            root = Environment.getExternalStoragePath().getPath();
                        } else if (Environment.getInternalStoragePathState().equals(
                                Environment.MEDIA_MOUNTED)) {
                            root = Environment.getInternalStoragePath().getPath();
                        } else {
                            return;
                        }*/
                        StatFs stat = new StatFs(root + Recorder.DEFAULT_STORE_SUBDIR);
                        boolean hasAvailableSize = checkAvailableSize(FORCE_FREE_SIZE, stat);
                        if (!hasAvailableSize) {
                            DDLog.i(Recorder.class, "check disk : hasAvailableSize = " + hasAvailableSize);
                            stop();
                            mUiHandler.removeMessages(SIGNAL_ERROR);
                            mUiHandler.obtainMessage(SIGNAL_ERROR, TYPE_ERROR_SD_FULL, 0)
                                    .sendToTarget();
                            break;// break the loop
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mUiHandler.removeMessages(SIGNAL_ERROR);
                        mUiHandler.obtainMessage(SIGNAL_ERROR, TYPE_ERROR_SD_ACCESS, 0, null)
                                .sendToTarget();
                        stop();
                        break;
                    }
//                    sleep(true);
                    break;
                case MSG_START:
                    if (mState.isIdle()) {
                        int error = startRecording(DEFAULT_OUTPUTFORMAT_TYPE,
                                DEFAULT_RECORD_SUFFIX, mContext);
                        // here not to send full space msg, it will be done when handle msg
                        // with MSG_CHECK_DISK
                        if (error != NO_ERROR && error != TYPE_ERROR_SD_FULL) {
                            mUiHandler.removeMessages(SIGNAL_ERROR);
                            mUiHandler.obtainMessage(SIGNAL_ERROR, error, 0, null).sendToTarget();
                        }
                        break;
                    }
                    DDLog.e(Recorder.class, "Start with error State : " + mState);
                    break;
                case MSG_STOP:
                    if (!mState.isIdle()) {
                        stopRecording();
                        if (mSampleFile == null || !mSampleFile.exists()) {
                            DDLog.e(Recorder.class, "error, file not exist !");
                        }
                        long fileLength = mSampleFile.length();
                        if (fileLength > MIN_LENGTH) {
                            try {
                                addToMediaDB(mSampleFile);
                            } catch (Exception e) {
                                DDLog.e(Recorder.class, "addToMediaDB happen exception : " + e);
                            }
                            String canonicalPath;
                            try {
                                canonicalPath = mSampleFile.getCanonicalPath();
                            } catch (IOException e) {
                                DDLog.e(Recorder.class, "getCanonicalPath happen IOException : " + e);
                                canonicalPath = mSampleFile.getPath();
                            }
                            mUiHandler.removeMessages(SIGNAL_MSG);
                            mUiHandler.obtainMessage(SIGNAL_MSG, TYPE_MSG_PATH, 0,
                                    mContext.getString(R.string.prompt_record_finish) + canonicalPath).sendToTarget();
                        } else {
                            mUiHandler.removeMessages(SIGNAL_MSG);
                            mUiHandler.obtainMessage(SIGNAL_MSG, TYPE_SAVE_FAIL, 0,
                                    mContext.getString(R.string.recorderr)).sendToTarget();
                            boolean isDeleteSucceed = mSampleFile.delete();
                            DDLog.e(Recorder.class, "It is so short, delete file : " + mSampleFile.getName()
                                    + ", isDeleteSucceed = " + isDeleteSucceed);
                        }
                        break;
                    }
                    DDLog.e(Recorder.class, "Stop with error State : " + mState);
                    break;
            }
        }

        private void sleep(boolean force) {
            if (checkAble) {
                Message m = obtainMessage(MSG_CHECK_DISK);
                sendMessageDelayed(m, force ? 2000 : 0);
                if (TEST_DBG)
                    DDLog.e(Recorder.class, "send check msg ");
            }
        }

        public void setCheckAble(boolean able) {
            checkAble = able;
        }
    }

    private boolean checkAvailableSize(long size, StatFs stat) {
        long available_size = stat.getBlockSize() * ((long) stat.getAvailableBlocks() - 4);
        if (available_size < size) {
            DDLog.e(Recorder.class, "Recording File aborted - not enough free space");
            return false;
        }
        return true;
    }

    private int initRecordFile() {
        DDLog.i(Recorder.class, "initRecordFile");
        File base = null;
        String root;
       /* if (Environment.getExternalStoragePathState().equals(
                Environment.MEDIA_MOUNTED)) {
            root = Environment.getExternalStoragePath().getPath();
        } else if (Environment.getInternalStoragePathState().equals(Environment.MEDIA_MOUNTED)) {
            root = Environment.getInternalStoragePath().getPath();
        } else {
            return TYPE_NO_AVAILABLE_STORAGE;
        }*/
        root = Constants.RECORDER_ROOT_PATH;
        DDLog.i(Recorder.class, "recording path=" + root);
        base = new File(root + DEFAULT_STORE_SUBDIR);
        if (!base.isDirectory() && !base.mkdir()) {
            DDLog.e(Recorder.class, "Recording File aborted - can't create base directory : " + base.getPath());
            return TYPE_ERROR_SD_ACCESS;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("'voicecall'-yyyyMMddHHmmss");
        String fileName = sdf.format(new Date());
        /* Naming record file with number and sim card feature. */
        int slotId = getSimSlotIndex();
        if (slotId >= 0) {
            fileName = DEFAULT_SIM_DESCRIPTOR + String.valueOf(slotId + 1)
                    + DEFAULT_DECOLLATOR + fileName;
        }
        String number = getNumber();
        if (number != null) {
            fileName = number + DEFAULT_DECOLLATOR + fileName;
        }
        fileName = base.getPath() + File.separator + fileName + DEFAULT_RECORD_SUFFIX;

        StatFs stat = null;
        stat = new StatFs(base.getPath());
        boolean hasAvailableSize = checkAvailableSize(MINIMUM_FREE_SIZE, stat);
        if (!hasAvailableSize) {
            // here not to send full space msg, it will be done when handle msg
            // with MSG_CHECK_DISK
            DDLog.e(Recorder.class, "Recording can not start, no enough free size!");
            return TYPE_ERROR_SD_FULL;
        }

        File outFile = new File(fileName);
        mFileName = fileName;
        try {
            if (outFile.exists()) {
                outFile.delete();
            }
            boolean bRet = outFile.createNewFile();
            if (!bRet) {
                DDLog.e(Recorder.class, "getRecordFile, fn: " + fileName + ", failed");
                return TYPE_ERROR_FILE_INIT;
            }
            PreferenceUtil.put(mContext, Constants.SP_RECORDER_PATH, fileName);
        } catch (Exception e) {
            DDLog.e(Recorder.class, "getRecordFile, fn: " + fileName + ", " + e);
            return TYPE_ERROR_FILE_INIT;
        }
        mSampleFile = outFile;
        return NO_ERROR;
    }

    private synchronized int startRecording(int outputfileformat, String extension, Context context) {
        /*boolean hasSdcard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
               || Environment.getInternalStoragePathState().equals(Environment.MEDIA_MOUNTED);
        if (!hasSdcard) {
            return TYPE_ERROR_SD_NOT_EXIST;
        }*/
        DDLog.i(Recorder.class, "startRecording");
        int error = initRecordFile();
        if (error != NO_ERROR) {
            DDLog.e(Recorder.class, "error, can not create a new file to record !");
            return error;
        }

        setState(State.WAITING);
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(outputfileformat);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());

        // Handle IOException
        try {
            mRecorder.prepare();
        } catch (IOException exception) {
            DDLog.e(Recorder.class, "IOException when recorder prepare !");
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            setState(State.IDLE);
            return TYPE_ERROR_INTERNAL;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.start();
        } catch (RuntimeException exception) {
            AudioManager audioMngr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            boolean isInCall = audioMngr.getMode() == AudioManager.MODE_IN_CALL;
            if (isInCall) {
                DDLog.e(Recorder.class, "RuntimeException when recorder start, cause : incall!");
            } else {
                DDLog.e(Recorder.class, "RuntimeException when recorder start !");
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            setState(State.IDLE);
            return isInCall ? TYPE_ERROR_IN_RECORD : TYPE_ERROR_INTERNAL;
        }
        mStartTime = SystemClock.elapsedRealtime();// start to record time
        setState(State.ACTIVE);

        // to update ui
        mUiHandler.setUpdate(true);
        mUiHandler.update(false);
        return NO_ERROR;
    }

    private synchronized void stopRecording() {
        DDLog.i(Recorder.class, "stopRecording");
        if (mRecorder == null) {
            mStartTime = INIT_TIME;
            DDLog.e(Recorder.class, "error, mRecorder is null !");
            return;
        }
        setState(State.WAITING);
        try {
            mRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mRecorder.release();
            mRecorder = null;
            mStartTime = INIT_TIME;
        }
        // should we reset the state in finally ?
        setState(State.IDLE);

        // to update ui
        mUiHandler.setUpdate(false);
    }

    /**
     * signal current state, when state changed
     **/
    private void setState(State state) {
        DDLog.e(Recorder.class, "pre State = " + mState + ", new State = " + state);
        if (state != mState) {
            mState = state;
            Message m = mUiHandler.obtainMessage(SIGNAL_STATE, mState);
            m.sendToTarget();
        }
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    /**
     * bianjb
     *
     * @return
     */
    public State getState() {
        return mState;
    }

    /**
     * to start recording not in looper thread
     **/
    public void start(String number) {
        DDLog.d(Recorder.class, "Recorder Start : state = " + mState);
        mPhoneNumber = number;
        if (mState.isIdle()) {
            mAsyncThread.removeMessages(MSG_START);
            mAsyncThread.sendEmptyMessage(MSG_START);// send start msg
            mAsyncThread.setCheckAble(true);// set disk check flag
//            mAsyncThread.sleep(false);// start check disk
        }
    }

    /**
     * to stop recording not in looper thread
     **/
    public void stop() {
        DDLog.i(Recorder.class, "Recorder Stop : state = " + mState);
        if (!mState.isIdle()) {
            mAsyncThread.removeMessages(MSG_STOP);
            mAsyncThread.sendEmptyMessage(MSG_STOP);// send stop msg
            mAsyncThread.setCheckAble(false);// stop check disk
        }
    }

    /**
     * start or stop recording
     **/
    public void toggleRecorder(String number) {
        Log.d(TAG, "Recorder toggleRecorder : state = " + mState);
        if (mState.isBlock()) {
            return;
        }
        if (mState.isActive()) {
            stop();
        } else {
            start(number);
        }
    }

    public void notifyCurrentState() {
        Message m = mUiHandler.obtainMessage(SIGNAL_STATE, mState);
        m.sendToTarget();
    }

    // ============================= save file to db
    private Uri addToMediaDB(File file) {
        Resources res = mContext.getResources();
        ContentValues cv = new ContentValues();
        long current = System.currentTimeMillis();
        long modDate = file.lastModified();

        long duration = 0;
        MediaPlayer p = new MediaPlayer();// a media player to get a duration
        try {
            p.setDataSource(file.getAbsolutePath());
            p.prepare();
            duration = p.getDuration();
            DDLog.i(Recorder.class, "Recorder duration : " + duration);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.release();
                p = null;
            }
        }

        Date date = new Date(current);
        SimpleDateFormat formatter = new SimpleDateFormat(DEFAULT_FROMAT);
        String title = formatter.format(date);

        // Lets label the recorded audio file as NON-MUSIC so that the file
        // won't be displayed automatically, except for in the playlist.
        cv.put(MediaStore.Audio.Media.IS_MUSIC, "1");
        cv.put(MediaStore.Audio.Media.DURATION, duration);
        cv.put(MediaStore.Audio.Media.TITLE, title);
        cv.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
        cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
        cv.put(MediaStore.Audio.Media.MIME_TYPE, DEFAULT_MIME_TYPE);
        cv.put(MediaStore.Audio.Media.ARTIST,
                res.getString(R.string.audio_db_artist_name));
        cv.put(MediaStore.Audio.Media.ALBUM,
                res.getString(R.string.audio_db_album_name));
        DDLog.i(Recorder.class, "Inserting audio record: " + cv.toString());
        ContentResolver resolver = mContext.getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        DDLog.i(Recorder.class, "ContentURI: " + base);
        Uri result = resolver.insert(base, cv);
        if (result == null) {
            new AlertDialog.Builder(mContext).setTitle(R.string.app_name)
                    .setMessage(R.string.error_mediadb_new_record)
                    .setPositiveButton(R.string.ok, null).setCancelable(false)
                    .show();
            DDLog.e(Recorder.class, "addToMediaDB:result == null");
            return null;
        }
        if (getPlaylistId(res) == -1) {
            createPlaylist(res, resolver);
        }
        int audioId = Integer.valueOf(result.getLastPathSegment());
        addToPlaylist(resolver, audioId, getPlaylistId(res));
        DDLog.i(Recorder.class, "addToMediaDB: success , send scanner intent!");

        // Notify those applications such as Music listening to the
        // scanner events that a recorded audio file just created.
        mContext.sendBroadcast(new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
        return result;
    }

    private int getPlaylistId(Resources res) {
        Uri uri = MediaStore.Audio.Playlists.getContentUri("external");
        final String[] ids = new String[]{
                MediaStore.Audio.Playlists._ID
        };
        final String where = MediaStore.Audio.Playlists.NAME + "=?";
        final String[] args = new String[]{
                res.getString(R.string.audio_db_playlist_name)
        };
        Cursor cursor = query(uri, ids, where, args, null);
        if (cursor == null) {
            DDLog.e(Recorder.class, "error, query returns null");
        }
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    /**
     * Add the given audioId to the play list with the given playlistId; and
     * maintain the play_order in the play list.
     */
    private void addToPlaylist(ContentResolver resolver, int audioId, long playlistId) {
        String[] cols = new String[]{
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        cur.moveToFirst();
        final int base = cur.getInt(0);
        cur.close();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
        uri = resolver.insert(uri, values);
        DDLog.i(Recorder.class, "addToPlaylist:uri=" + uri);
    }

    /**
     * Create a playlist with the given default playlist name, if no such
     * playlist exists.
     */
    private Uri createPlaylist(Resources res, ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Playlists.NAME, res.getString(R.string.audio_db_playlist_name));
        Uri uri = resolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), cv);
        if (uri == null) {
            new AlertDialog.Builder(mContext).setTitle(R.string.app_name)
                    .setMessage(R.string.error_mediadb_new_record)
                    .setPositiveButton(R.string.ok, null).setCancelable(false)
                    .show();
            DDLog.e(Recorder.class, "createPlaylist: uri == null");
        }
        return uri;
    }

    /**
     * A simple utility to do a query into the databases.
     */
    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                         String sortOrder) {
        try {
            ContentResolver resolver = mContext.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs,
                    sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    /**
     * 返回默认值0
     *
     * @return
     */
    private int getSimSlotIndex() {
        int phoneId = INVALID_SIM_SLOT_INDEX;
//        Call call = (CallList.getInstance().getActiveCall() == null) ? null :
//                CallList.getInstance().getActiveCall();
//        if (call != null) {
//            phoneId = call.getPhoneId();
//            return phoneId;
//        }
        return 0;
    }

    /**
     * 获取电话号码
     *
     * @return
     */
    private String getNumber() {
       /* Call call = CallList.getInstance().getActiveCall();
        if (call != null) {
            return call.getNumber();
        }*/
        return mPhoneNumber;
    }
}
