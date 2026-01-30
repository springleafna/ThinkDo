package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息通知业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationBizTypeEnum {
    /**
     * 计划
     */
    PLAN(1, "计划"),

    /**
     * 知识库
     */
    KNOWLEDGE(2, "知识库");

    private final Integer value;
    private final String description;

    /**
     * 根据数值获取对应的枚举
     *
     * @param value 数据库存储的值
     * @return 对应的枚举，如果找不到返回null
     */
    public static NotificationBizTypeEnum fromValue(Integer value) {
        for (NotificationBizTypeEnum type : NotificationBizTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查给定的值是否是有效的业务类型
     *
     * @param value 要检查的值
     * @return 如果是有效业务类型返回true，否则返回false
     */
    public static boolean isValid(Integer value) {
        for (NotificationBizTypeEnum type : NotificationBizTypeEnum.values()) {
            if (type.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
