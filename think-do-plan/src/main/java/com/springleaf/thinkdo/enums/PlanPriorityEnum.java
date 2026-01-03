package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计划优先级枚举
 */
@Getter
@AllArgsConstructor
public enum PlanPriorityEnum {

    /**
     * 低优先级
     */
    LOW(1, "低"),

    /**
     * 中优先级
     */
    MEDIUM(2, "中"),

    /**
     * 高优先级
     */
    HIGH(3, "高");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanPriorityEnum getByCode(Integer code) {
        if (code == null) {
            return MEDIUM;
        }
        for (PlanPriorityEnum priorityEnum : values()) {
            if (priorityEnum.getCode().equals(code)) {
                return priorityEnum;
            }
        }
        return MEDIUM;
    }

    /**
     * 校验优先级是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanPriorityEnum priorityEnum : values()) {
            if (priorityEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
