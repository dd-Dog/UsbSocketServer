package com.flyscale.ecserver.util;

/**
 * Created by bian on 2019/2/14.
 */

public class ArrayUtil {
    /**
     * byte数组转short数组 每两个byte转为一个short类型
     * 小端模式：数据的高字节保存在内存的高地址中
     * @param src
     * @return
     */
    public static short[] toShortArraySmallEnd(byte[] src) {

        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) (src[i * 2 + 1] << 8 | src[2 * i] & 0xff);
        }
        return dest;
    }

    /**
     *short数组转为byte数组
     * 小端模式：数据的高字节保存在内存的高地址中
     * @param src
     * @return
     */
    public static byte[] toByteArraySmallEnd(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2 + 1] = (byte) (src[i] >> 8);
            dest[i * 2 ] = (byte) (src[i] >> 0);
        }

        return dest;
    }
    /**
     * byte数组转short数组 每两个byte转为一个short类型
     * 大端模式：数据的高字节保存在内存的低地址中，而数据的低字节保存在内存的高地址中
     * @param src
     * @return
     */
    public static short[] toShortArrayBigEnd(byte[] src) {

        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) (src[i * 2] << 8 | src[2 * i + 1] & 0xff);
        }
        return dest;
    }

    /**
     *short数组转为byte数组
     * 大端模式：数据的高字节保存在内存的低地址中，而数据的低字节保存在内存的高地址中
     * @param src
     * @return
     */
    public static byte[] toByteArrayBigEnd(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }

        return dest;
    }
}
