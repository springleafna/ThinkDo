package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 每日清单执行状态枚举
 */
@Getter
@AllArgsConstructor
public enum PlanExecutionStatusEnum {

    /**
     * 未完成
     */
    NOT_COMPLETED(0, "未完成"),

    /**
     * 已完成
     */
    COMPLETED(1, "已完成");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PlanExecutionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return NOT_COMPLETED;
        }
        for (PlanExecutionStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return NOT_COMPLETED;
    }

    /**
     * 校验状态是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (PlanExecutionStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
