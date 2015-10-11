package com.shuaqiu.sqlighter.processor.util;

/**
 * String Utility
 * Created by shuaqiu on 2015-10-11.
 */
public class StringUtils {

    /**
     * 将字符串的首字母大写
     * @param str 字符串
     * @return 首字母大写后的字符串
     */
    public static String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
