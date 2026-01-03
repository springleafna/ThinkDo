package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 计划状态枚举
 */
@Getter
@AllArgsConstructor
public enum PlanStatusEnum {

    /**
     * 未完成
     */
    NOT_STARTED(0, "未完成"),

    /**
     * 已完成
     */
    COMPLETED(1, "已完成");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanStatusEnum getByCode(Integer code) {
        if (code == null) {
            return NOT_STARTED;
        }
        for (PlanStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return NOT_STARTED;
    }

    /**
     * 校验状态是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
