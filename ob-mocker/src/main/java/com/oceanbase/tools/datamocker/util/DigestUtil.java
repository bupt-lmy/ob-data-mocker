package com.oceanbase.tools.datamocker.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 字符串摘要文本生成工具类
 *
 * @author yh263208
 * @date 2021-01-14 17:06
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DigestUtil {
    /**
     * 获取token字符串，获取数据摘要
     *
     * @param info 输入字符串
     * @return 返回摘要字符串
     */
    public static String getToken(String info) throws NoSuchAlgorithmException {
        MessageDigest diggest = MessageDigest.getInstance("md5");
        byte[] buffer = diggest.digest(info.getBytes());
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(buffer);
    }

    /**
     * 获取token字节数组，获取数据摘要
     *
     * @param info 输入字节数组
     * @return 返回摘要字节数组
     */
    public static byte[] getToken(byte[] info) throws NoSuchAlgorithmException {
        MessageDigest diggest = MessageDigest.getInstance("md5");
        return diggest.digest(info);
    }
}
