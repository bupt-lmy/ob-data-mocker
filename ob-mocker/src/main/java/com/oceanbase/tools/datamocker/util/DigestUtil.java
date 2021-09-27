package com.oceanbase.tools.datamocker.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * String summary text generation tool class
 *
 * @author yh263208
 * @date 2021-01-14 17:06
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DigestUtil {
    /**
     * Get the token string, get the data summary
     *
     * @param info Input string
     * @return Return summary string
     */
    public static String getToken(String info) throws NoSuchAlgorithmException {
        MessageDigest diggest = MessageDigest.getInstance("md5");
        byte[] buffer = diggest.digest(info.getBytes());
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(buffer);
    }

    /**
     * Get token byte array, get data summary
     *
     * @param info Input byte array
     * @return Return summary byte array
     */
    public static byte[] getToken(byte[] info) throws NoSuchAlgorithmException {
        MessageDigest diggest = MessageDigest.getInstance("md5");
        return diggest.digest(info);
    }
}
