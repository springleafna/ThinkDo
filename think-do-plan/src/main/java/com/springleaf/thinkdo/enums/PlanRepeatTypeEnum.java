package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计划重复类型枚举
 */
@Getter
@AllArgsConstructor
public enum PlanRepeatTypeEnum {

    /**
     * 不重复
     */
    NONE(0, "不重复"),

    /**
     * 每天
     */
    DAILY(1, "每天"),

    /**
     * 每周
     */
    WEEKLY(2, "每周"),

    /**
     * 每月
     */
    MONTHLY(3, "每月"),

    /**
     * 每年
     */
    YEARLY(4, "每年"),

    /**
     * 工作日
     */
    WORKDAYS(5, "工作日");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanRepeatTypeEnum getByCode(Integer code) {
        if (code == null) {
            return NONE;
        }
        for (PlanRepeatTypeEnum repeatTypeEnum : values()) {
            if (repeatTypeEnum.getCode().equals(code)) {
                return repeatTypeEnum;
            }
        }
        return NONE;
    }

    /**
     * 校验重复类型是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanRepeatTypeEnum repeatTypeEnum : values()) {
            if (repeatTypeEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
