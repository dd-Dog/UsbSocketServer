package com.flyscale.ecserver.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bian on 2018/12/6.
 */

   /*
     * 注意: format(Date date)这个方法来自于SimpleDateFormat的父类DateFormat
     * String str1 = sdf1.format(date1);
       System.out.println("字符串类型时间:" + str1);
       // 字符串类型时间-》转换为定义格式-》日期类型时间
       Date dateF1 = sdf1.parse(str1);
       System.out.println("日期类型时间:" + dateF1);
       // **************2.关于常用格式分析*************
       System.out.println("----------常用格式分析---------");

     * y : 年
     * M : 年中的月份
     * D : 年中的天数
     * d : 月中的天数
     * w : 年中的周数
     * W : 月中的周数
     * a : 上下/下午
     * H : 一天中的小时数(0-23)
     * h : 一天中的小时数(0-12)
     * m : 小时中的分钟
     * s : 分钟钟的秒数
     * S : 毫秒数

      SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd,w,W,a,HH:mm:ss,SS");
      String str2 = sdf2.format(new Date());
      System.out.println("日期类型时间:" + str2);
      System.out.println("字符串类型时间:" + sdf2.parse(str2));
      // **************2.关于构造器使用技巧分析*************
      System.out.println("----------构造器使用技巧分析---------");

     * 构造器:
     * SimpleDateFormat();
     * SimpleDateFormat(String pattern);
     * SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols);
     * SimpleDateFormat(String pattern, Locale locale)
     */

public class DateFormatUtil {
    /**
     * @return 2018-12-06 14:52:48
     */
    public static String getTime1() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * @return 2018.12.06-14:52
     */
    public static String getTime2() {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm").format(new Date());
    }

    /**
     * @return 2018-12-06
     */
    public static String getTime3() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    /**
     * @return 2018-12-18 16:56:06,615
     */
    public static String getTime4() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SS").format(new Date());
    }
}
