package com.yinyxn.music;

/**
 * Created by yinyxn on 2015/12/24.
 * 时间格式化工具类
 */
public class  TimeUtil {

    public static String formatDuration(long duration) {
        long t = duration / 1000;
        int m = (int) (t / 60);
        int s = (int) (t % 60);

        return String.format("%2d:%02d",m,s);
    }
}
