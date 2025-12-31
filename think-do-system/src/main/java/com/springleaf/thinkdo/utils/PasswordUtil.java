package com.springleaf.thinkdo.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码加密工具类
 */
public final class PasswordUtil {

    /**
     * 加密密码
     *
     * @param password 原密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 验证密码
     *
     * @param password       原密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
