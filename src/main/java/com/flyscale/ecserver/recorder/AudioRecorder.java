package com.flyscale.ecserver.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.flyscale.ecserver.service.PCMSender;
import com.flyscale.ecserver.util.ArrayUtil;
import com.flyscale.ecserver.util.DDLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import io.kvh.media.amr.AmrEncoder;

/**
 * Created by bian on 2019/1/17.
 */

public class AudioRecorder implements PCMSender.PCMSocketConnecteListener {

    public static final int SAMPLE_RATE = 16000;
    private static final byte[] STOP_FLAG = new byte[640];
    private AudioRecord mAudioRecord;
    private State mState = State.IDLE;
    private int mMinBufferSize;
    private MediaCodec mMediaCodec;
    private ByteBuffer[] mEncodeInputBuffers;
    private ByteBuffer[] mEncodeOutputBuffers;
    private MediaCodec.BufferInfo mEncodeBufferInfo;
    private BufferedOutputStream mMCbos;
    private FileOutputStream mMCfos;
    private Queue<byte[]> mPCMCache;
    private PCMSender mPCMSender;
    private ServerSocket mServerSocket;

    public void init(ServerSocket serverSocket) {
        DDLog.i(AudioRecorder.class, "init");
        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        DDLog.i(AudioRecorder.class, "mMinBufferSize=" + mMinBufferSize);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, //采样率
                AudioFormat.CHANNEL_CONFIGURATION_MONO, //单声道
                AudioFormat.ENCODING_PCM_16BIT, //编译格式16位
                mMinBufferSize   //最小缓冲区
        );

        this.mServerSocket = serverSocket;
        mPCMCache = new Queue<>();

        //PCM帧结束标志
        STOP_FLAG[0] = 0x7F;
        STOP_FLAG[1] = 0x7F;
        STOP_FLAG[2] = 0x7F;
        STOP_FLAG[3] = 0x7F;
    }

    public void start() {
        DDLog.i(AudioRecorder.class, "start");
        if (mAudioRecord != null && mState.isIdle()) {
            mAudioRecord.startRecording();
            mState = State.RECORDING;
            startReadThread();
        }
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
        new ReadAudioDataThread().start();
    }

    @Override
    public void onConnect() {
        //PCM接收客户端已经连接
        mPCMCache.setEnabled(true);
    }

    /**
     * 读取音频数据线程
     */
    private class ReadAudioDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            String testFile = "/storage/emulated/legacy/testaudiorecord.raw";
            String testFilePlay = "/storage/emulated/legacy/testaudiorecordplay.wav";
            String pcm2Amr = "/storage/emulated/legacy/pcm2amrWithOpencoreAmr.amr";
            String pcm2aac = "/storage/emulated/legacy/pcm2aac.aac";
//            pcm2aac(pcm2aac);
            sendPCM2Client();
            pcm2amrWithOpencoreAmr(pcm2Amr);
//            writeData2File(testFile);
            copyWaveFile(testFile, testFilePlay);//给裸数据加上头文件
        }
    }

    /**
     * 把PCM数据发送给客户端
     */
    private void sendPCM2Client() {
        DDLog.i(AudioRecorder.class, "sendPCM2Client");
        mPCMSender = new PCMSender(mServerSocket);
        mPCMSender.setPCMData(mPCMCache);
        mPCMSender.start();
        mPCMSender.setOnPCMSocketConnecteListener(this);
    }

    private void pcm2aac(String filePath) {
        start(filePath);
        short[] in = new short[160];
        while (mState.isRecording()) {
            int read = mAudioRecord.read(in, 0, in.length);
            DDLog.i(AudioRecorder.class, "pcm2aac,readsize=" + read);
            byte[] inBytes = toByteArray(in);
            flow(inBytes, inBytes.length);
        }
        stop();
    }

    public static byte[] toByteArray(short[] src) {

        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }

        return dest;
    }

    /**
     * 使用MediaCodec进行编码
     *
     * @param filePath
     */
    public void start(String filePath) {
        try {
            mMCfos = new FileOutputStream(filePath);
            mMCbos = new BufferedOutputStream(mMCfos, 200 * 1024);

            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AMR_NB, SAMPLE_RATE, 1);
//            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 100);//比特率

            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AMR_NB);
            mMediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mMediaCodec == null) {
            DDLog.e(AudioRecorder.class, "create mediaEncode failed");
            return;
        }

        //调用MediaCodec的start()方法，此时MediaCodec处于Executing状态
        mMediaCodec.start();
    }

    public void flow(byte[] bytes, int size) {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        //通过getInputBuffers()方法和getOutputBuffers()方法获取缓存队列
        mEncodeInputBuffers = mMediaCodec.getInputBuffers();
        mEncodeOutputBuffers = mMediaCodec.getOutputBuffers();
        //用于存储ByteBuffer的信息
        mEncodeBufferInfo = new MediaCodec.BufferInfo();

        //首先通过dequeueInputBuffer(long timeoutUs)请求一个输入缓存，timeoutUs代表等待时间，设置为-1代表无限等待
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);

        //返回的整型变量为请求到的输入缓存的index，通过getInputBuffers()得到的输入缓存数组,再用index和输入缓存数组即可得到当前请求的输入缓存
        if (inputBufferIndex >= 0) {
            inputBuffer = mEncodeInputBuffers[inputBufferIndex];
            //使用之前要clear一下，避免之前的缓存数据影响当前数据
            inputBuffer.clear();
            //把数据添加到输入缓存中，
            inputBuffer.put(bytes);
            //并调用queueInputBuffer()把缓存数据入队
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0);
        }
        //通过dequeueOutputBuffer(BufferInfo info, long timeoutUs)来请求一个输出缓存,传入一个上面的BufferInfo对象
        outputIndex = mMediaCodec.dequeueOutputBuffer(mEncodeBufferInfo, 10000);
        //然后通过返回的index得到输出缓存，并通过BufferInfo获取ByteBuffer的信息
        while (outputIndex >= 0) {
            outBitSize = mEncodeBufferInfo.size;

            //添加ADTS头,ADTS头包含了AAC文件的采样率、通道数、帧数据长度等信息。
            outPacketSize = outBitSize + 7;//7为ADTS头部的大小
            outputBuffer = mEncodeOutputBuffers[outputIndex];//拿到输出Buffer
            outputBuffer.position(mEncodeBufferInfo.offset);
            outputBuffer.limit(mEncodeBufferInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS 代码后面会贴上
            outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中偏移量offset=7
            outputBuffer.position(mEncodeBufferInfo.offset);
            //showLog("outPacketSize:" + outPacketSize + " encodeOutBufferRemain:" + outputBuffer.remaining());
            try {
                mMCbos.write(chunkAudio, 0, chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
            } catch (IOException e) {
                e.printStackTrace();
            }
            //releaseOutputBuffer方法必须调用
            mMediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mMediaCodec.dequeueOutputBuffer(mEncodeBufferInfo, 10000);

        }
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
        AmrEncoder.init(0);
        int mode = AmrEncoder.Mode.MR122.ordinal();
        //每次读取320个PCM采样，最终经过下采样转为160个采样，压缩为一个AMR帧
        short[] in = new short[320];//short array read from AudioRecorder, length 160
        byte[] out = new byte[32];//output amr frame, length 32

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        //写入文件头先
        FileOutputStream fos = null;
        FileOutputStream fosRaw = null;
        //0x23 21 41 4d 52 2d 57 42 0a AMR_WB
        final byte[] AMR_HEAD = new byte[]{0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A};
        mPCMCache.clear();
        try {
            String testFile = "/storage/emulated/legacy/testaudiorecord.raw";
//            fosRaw = new FileOutputStream(testFile);

            fos = new FileOutputStream(path, true);
            fos.write(AMR_HEAD, 0, AMR_HEAD.length);
            while (mState.isRecording()) {
                int read = mAudioRecord.read(in, 0, in.length);
                DDLog.i(AudioRecorder.class, "pcm2amrWithOpencoreAmr,readsize=" + read);

                //PCM数据转为byte数组，并添加到缓存队列,注意这里需要使用小端模式
                mPCMCache.push(ArrayUtil.toByteArraySmallEnd(in));
//                fosRaw.write(ArrayUtil.toByteArraySmallEnd(in));

                short[] downIn = downSample(in);
                int byteEncoded = AmrEncoder.encode(mode, downIn, out);
                fos.write(out, 0, out.length);
            }
            mPCMCache.push(STOP_FLAG);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert fos != null;
                fos.flush();
                fos.close();
//                fosRaw.flush();
//                fosRaw.close();
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
        for (int i = 0,j=0; i < in.length; i += 2,j++) {
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

    enum State {
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
