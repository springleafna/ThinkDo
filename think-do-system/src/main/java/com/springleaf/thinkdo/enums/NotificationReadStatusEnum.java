package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息通知已读状态枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationReadStatusEnum {
    /**
     * 未读
     */
    UNREAD(0, "未读"),

    /**
     * 已读
     */
    READ(1, "已读");

    private final Integer value;
    private final String description;

    /**
     * 根据数值获取对应的枚举
     *
     * @param value 数据库存储的值
     * @return 对应的枚举，如果找不到返回null
     */
    public static NotificationReadStatusEnum fromValue(Integer value) {
        for (NotificationReadStatusEnum status : NotificationReadStatusEnum.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查给定的值是否是有效的已读状态
     *
     * @param value 要检查的值
     * @return 如果是有效已读状态返回true，否则返回false
     */
    public static boolean isValid(Integer value) {
        for (NotificationReadStatusEnum status : NotificationReadStatusEnum.values()) {
            if (status.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
