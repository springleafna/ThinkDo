package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 便签背景颜色枚举
 */
@Getter
@AllArgsConstructor
public enum BackgroundColorEnum {

    /**
     * 黄色
     */
    YELLOW("#fef9e3", "黄色"),

    /**
     * 蓝色
     */
    BLUE("#f4f8fe", "蓝色"),

    /**
     * 紫色
     */
    PURPLE("#f9f3ff", "紫色"),

    /**
     * 粉色
     */
    PINK("#fff1f2", "粉色"),

    /**
     * 绿色
     */
    GREEN("#e8fcf2", "绿色");

    private final String code;
    private final String description;

    /**
     * 根据颜色代码获取枚举
     */
    public static BackgroundColorEnum fromCode(String code) {
        if (code == null) {
            return YELLOW;
        }
        for (BackgroundColorEnum color : values()) {
            if (color.code.equals(code)) {
                return color;
            }
        }
        return YELLOW;
    }

    /**
     * 判断是否为有效的颜色代码
     */
    public static boolean isValidColor(String code) {
        if (code == null) {
            return false;
        }
        for (BackgroundColorEnum color : values()) {
            if (color.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有颜色代码列表
     */
    public static List<String> getAllColorCodes() {
        return Arrays.stream(values())
                .map(BackgroundColorEnum::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取默认颜色代码
     */
    public static String getDefaultColorCode() {
        return YELLOW.getCode();
    }
}
