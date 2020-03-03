package org.apache.dubbo.admin.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author frank woo(吴峻申) <br>
 * email:<a href="mailto:frank_wjs@hotmail.com">frank_wjs@hotmail.com</a> <br>
 * @date 2020/3/3 2:09 下午 <br>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Sha256Utils {
    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 利用java原生的类实现SHA256加密
     *
     * @param str 明文字符串
     * @return 密文字符串
     */
    public static String getSha256(String str) {
        if (str == null) {
            return null;
        }

        md.update(str.getBytes(StandardCharsets.UTF_8));

        return convertToString(md.digest());
    }

    /**
     * 将byte转为16进制
     *
     * @param bytes byte数组
     * @return 字符串
     */
    private static String convertToString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        String temp;
        for (byte b : bytes) {
            temp = Integer.toHexString(b & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuilder.append("0");
            }
            stringBuilder.append(temp);
        }
        return stringBuilder.toString();
    }
}
