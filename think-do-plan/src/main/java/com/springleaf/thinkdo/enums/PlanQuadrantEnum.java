package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 四象限状态枚举
 */
@Getter
@AllArgsConstructor
public enum PlanQuadrantEnum {

    /**
     * 无
     */
    NONE(0, "无"),

    /**
     * 重要且紧急
     */
    IMPORTANT_URGENT(1, "重要且紧急"),

    /**
     * 重要不紧急
     */
    IMPORTANT_NOT_URGENT(2, "重要不紧急"),

    /**
     * 紧急不重要
     */
    URGENT_NOT_IMPORTANT(3, "紧急不重要"),

    /**
     * 不重要不紧急
     */
    NOT_IMPORTANT_NOT_URGENT(4, "不重要不紧急");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanQuadrantEnum getByCode(Integer code) {
        if (code == null) {
            return NONE;
        }
        for (PlanQuadrantEnum quadrantEnum : values()) {
            if (quadrantEnum.getCode().equals(code)) {
                return quadrantEnum;
            }
        }
        return NONE;
    }

    /**
     * 校验四象限状态是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanQuadrantEnum quadrantEnum : values()) {
            if (quadrantEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
