package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计划类型枚举
 */
@Getter
@AllArgsConstructor
public enum PlanTypeEnum {

    /**
     * 普通计划
     */
    NORMAL(0, "普通计划"),

    /**
     * 四象限计划
     */
    QUADRANT(1, "四象限计划"),

    /**
     * 每日计划
     */
    DAILY(2, "每日计划");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanTypeEnum getByCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (PlanTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return NORMAL;
    }

    /**
     * 校验类型是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
