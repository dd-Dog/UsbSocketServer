package com.flyscale.ecserver.recorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;

import com.xr.service.IDataInfo;
import com.flyscale.ecserver.global.Constants;
import com.flyscale.ecserver.service.PCMIPCSender;
import com.flyscale.ecserver.service.PCMSocketSender;
import com.flyscale.ecserver.util.ArrayUtil;
import com.flyscale.ecserver.util.DDLog;
import com.flyscale.ecserver.util.PreferenceUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import io.kvh.media.amr.AmrEncoder;

/**
 * Created by bian on 2019/1/17.
 */

public class AudioRecorder implements PCMSocketSender.PCMSocketConnecteListener {

    private static final String DEFAULT_STORE_SUBDIR = "/voicecall";
    public static final int TYPE_ERROR_SD_ACCESS = 5001;// can not access sdcard
    public static final int START_SUCCESS = 5002;
    public static final int START_FAILED = 5003;
    private static final String DEFAULT_SIM_DESCRIPTOR = "sim";
    private static final String DEFAULT_DECOLLATOR = "-";
    private static final String DEFAULT_RECORD_SUFFIX = ".amr";
    private static final int PCM_POINTS_EACH_AMR_FRAME = 160;
    private static final int AMR_FRAME_BYTES = 32;


    public static final int SAMPLE_RATE = 16000;
    private static final byte[] STOP_FLAG = new byte[640];
    private static AudioRecorder mInstance;
    private final Context mContext;
    private AudioRecord mAudioRecord;
    public State mState = State.IDLE;
    private int mMinBufferSize;
    private MediaCodec mMediaCodec;
    private ByteBuffer[] mEncodeInputBuffers;
    private ByteBuffer[] mEncodeOutputBuffers;
    private MediaCodec.BufferInfo mEncodeBufferInfo;
    private BufferedOutputStream mMCbos;
    private FileOutputStream mMCfos;
    private Queue<byte[]> mPCMSocketCache;
    private PCMSocketSender mPCMSocketSender;
    public String mFileName;
    private static IDataInfo mIDataInfo;
    private PCMIPCSender mPCMIPCSender;
    private Queue<byte[]> mPCMIPCCache;
    private static CopyOnWriteArrayList<Socket> mPCMClientSockets;

    private AudioRecorder(Context context) {
        mContext = context;
    }

    public static AudioRecorder getInstance(Context context, ServerSocket serverSocket) {
        if (mInstance == null) {
            mInstance = new AudioRecorder(context);
        }
        return mInstance;
    }

    public static AudioRecorder getInstance(Context context, IDataInfo iDataInfo, CopyOnWriteArrayList<Socket> sockets) {
        if (mInstance == null) {
            mInstance = new AudioRecorder(context);
            mIDataInfo = iDataInfo;
            mPCMClientSockets = sockets;
        }
        return mInstance;
    }

    public void init() {
        DDLog.i(AudioRecorder.class, "init");
        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        DDLog.i(AudioRecorder.class, "mMinBufferSize=" + mMinBufferSize);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, //采样率
                AudioFormat.CHANNEL_CONFIGURATION_MONO, //单声道
                AudioFormat.ENCODING_PCM_16BIT, //编译格式16位
                mMinBufferSize   //最小缓冲区
        );


        mPCMSocketCache = new Queue<>();
        mPCMIPCCache = new Queue<>();
        if (mIDataInfo != null) {//如果 Binder已经连接则可以开始缓存
            mPCMIPCCache.setEnabled(true);
        }
        if (mPCMClientSockets != null && mPCMClientSockets.size() > 0){
            mPCMSocketCache.setEnabled(true);
        }

        //PCM帧结束标志
        STOP_FLAG[0] = 0x7F;
        STOP_FLAG[1] = 0x7F;
        STOP_FLAG[2] = 0x7F;
        STOP_FLAG[3] = 0x7F;
    }

    public int start(String number) {
        DDLog.i(AudioRecorder.class, "start,mState=" + mState);
        String root = Constants.RECORDER_ROOT_PATH;
        DDLog.i(AudioRecorder.class, "recording path=" + root);
        File base = new File(root + DEFAULT_STORE_SUBDIR);
        if (!base.isDirectory() && !base.mkdir()) {
            DDLog.e(Recorder.class, "Recording File aborted - can't create base directory : " + base.getPath());
            return TYPE_ERROR_SD_ACCESS;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("'voicecall'-yyyyMMddHHmmss");
        String fileName = sdf.format(new Date());
        /* Naming record file with number and sim card feature. */
        int slotId = 0;
        if (slotId >= 0) {
            fileName = DEFAULT_SIM_DESCRIPTOR + String.valueOf(slotId + 1)
                    + DEFAULT_DECOLLATOR + fileName;
        }
        if (number != null) {
            fileName = number + DEFAULT_DECOLLATOR + fileName;
        }
        fileName = base.getPath() + File.separator + fileName + DEFAULT_RECORD_SUFFIX;
        mFileName = fileName;
        DDLog.i(AudioRecorder.class, "mFileName=" + mFileName);

        if (mAudioRecord == null) {
            init();
        }
        if (mAudioRecord != null && mState.isIdle()) {
            mAudioRecord.startRecording();
            mState = State.RECORDING;
            startReadThread();
            return START_SUCCESS;
        }
        return START_FAILED;
    }

    public void stop() {
        DDLog.i(AudioRecorder.class, "stop");
        if (mAudioRecord != null && mState.isRecording()) {
            mState = State.IDLE;//停止文件写入
            mAudioRecord.stop();
            mAudioRecord.release();//释放资源
            mAudioRecord = null;
        }
    }

    private void startReadThread() {
        DDLog.i(AudioRecorder.class, "startReadThread");
        new ReadAudioDataThread().start();
    }

    @Override
    public void onConnect() {
        //PCM接收客户端已经连接，则开始缓存PCM数据
        mPCMSocketCache.setEnabled(true);
    }

    public void setIDataInfo(IDataInfo iDataInfo) {
        DDLog.i(AudioRecorder.class, "setIDataInfo");
        mIDataInfo = iDataInfo;
        if (mPCMIPCSender != null) {
            mPCMIPCSender.setIDataInfo(mIDataInfo);
        }

        if (mIDataInfo != null) {//如果Binder已经连接，则可以开始缓存
            mPCMIPCCache.setEnabled(true);
        }else{
            mPCMSocketCache.setEnabled(false);
        }
    }

    /**
     * 客户端连接有变化
     *
     * @param sockets
     */
    public void notifySocketChanged(CopyOnWriteArrayList<Socket> sockets) {
        DDLog.i(AudioRecorder.class, "notifySocketChanged");
        mPCMClientSockets = sockets;
        if (mPCMClientSockets != null && mPCMClientSockets.size()>0){
            if (!mPCMSocketCache.isEnabled()){
                mPCMSocketCache.setEnabled(true);
            }
        }
        if (mPCMSocketSender != null)
            mPCMSocketSender.notifySocketChanged(sockets);
    }


    /**
     * 读取音频数据线程
     */
    private class ReadAudioDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            sendPCM2Client();
            pcm2amrWithOpencoreAmr(mFileName);
        }
    }

    /**
     * 把PCM数据发送给客户端
     */
    private void sendPCM2Client() {
        DDLog.i(AudioRecorder.class, "sendPCM2Client");
        //Socket发送线程
        mPCMSocketSender = new PCMSocketSender(mPCMClientSockets);
        mPCMSocketSender.setPCMData(mPCMSocketCache);
        mPCMSocketSender.start();
        mPCMSocketSender.setOnPCMSocketConnecteListener(this);

        //Binder发送线程
        mPCMIPCSender = new PCMIPCSender(mIDataInfo);
        mPCMIPCSender.setPCMData(mPCMIPCCache);
        mPCMIPCSender.start();
    }


    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 8; // 44.1KHz
        int chanCfg = 1; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public void end() {
        try {
            if (mMCbos != null) {
                mMCbos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mMCbos != null) {
                try {
                    mMCbos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mMCbos = null;
                }
            }
        }

        try {
            if (mMCfos != null) {
                mMCfos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mMCfos = null;
        }

        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    /**
     * 使用opencore-amr-android编译库进行编码
     *
     * @param path
     */
    private void pcm2amrWithOpencoreAmr(String path) {
        PreferenceUtil.put(mContext, Constants.SP_RECORDER_PATH, path);
        AmrEncoder.init(0);
        int mode = AmrEncoder.Mode.MR122.ordinal();
        //每次读取1280个PCM采样，最终经过下采样转为640个采样，压缩为4个AMR帧
        short[] in = new short[PCM_POINTS_EACH_AMR_FRAME * 8];//short array read from AudioRecorder
        byte[] out = new byte[AMR_FRAME_BYTES];//output amr frame, length 32

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        //写入文件头先
        FileOutputStream fos = null;
        FileOutputStream fosRaw = null;
        //0x23 21 41 4d 52 2d 57 42 0a AMR_WB
        final byte[] AMR_HEAD = new byte[]{0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A};
        mPCMSocketCache.clear();
        mPCMIPCCache.clear();
        try {

            fos = new FileOutputStream(path, true);
            fos.write(AMR_HEAD, 0, AMR_HEAD.length);
            while (mState.isRecording()) {
                int read = mAudioRecord.read(in, 0, in.length);
//                DDLog.i(AudioRecorder.class, "pcm2amrWithOpencoreAmr,readsize=" + read + ",in.length=" + in.length + "-------");

                //PCM数据转为byte数组，并添加到缓存队列,注意这里需要使用小端模式
                mPCMSocketCache.push(ArrayUtil.toByteArraySmallEnd(in));
                mPCMIPCCache.push(ArrayUtil.toByteArraySmallEnd(in));

                short[] downIn = downSample(in);
                for (int i = 0; i < 4; i++) {
                    short[] tmp = new short[PCM_POINTS_EACH_AMR_FRAME];
                    System.arraycopy(downIn, i * PCM_POINTS_EACH_AMR_FRAME, tmp, 0, PCM_POINTS_EACH_AMR_FRAME);
                    int byteEncoded = AmrEncoder.encode(mode, tmp, out);
                    fos.write(out, 0, out.length);
                }
            }
            mPCMSocketCache.push(STOP_FLAG);
            mPCMIPCCache.push(STOP_FLAG);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert fos != null;
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AmrEncoder.exit();
        }

    }

    /**
     * 下采样 16K-8K
     *
     * @param in
     * @return
     */
    private short[] downSample(short[] in) {
        short[] down = new short[in.length / 2];
        for (int i = 0, j = 0; i < in.length; i += 2, j++) {
            down[j] = in[i];
        }
        return down;
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     * 所以如果需要能够播放，要加上AMR文件头
     */
    private void writeData2File(String filePath) {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[mMinBufferSize];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
            while (mState.isRecording()) {
                readsize = mAudioRecord.read(audiodata, 0, mMinBufferSize);
                DDLog.i(AudioRecorder.class, "writeData2File,readsize=" + readsize);
                if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                    fos.write(audiodata);
                }
            }
            fos.close();// 关闭写入流
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        DDLog.i(AudioRecorder.class, "copyWaveFile");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE * channels / 8;
        byte[] data = new byte[mMinBufferSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public enum State {
        IDLE,
        RECORDING;

        public boolean isIdle() {
            return this == IDLE;
        }

        public boolean isRecording() {
            return this == RECORDING;
        }

    }
}
