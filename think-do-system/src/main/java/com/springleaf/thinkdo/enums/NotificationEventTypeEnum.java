package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息通知事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationEventTypeEnum {
    /**
     * 处理完成
     */
    COMPLETED(1, "处理完成"),

    /**
     * 1天提醒
     */
    REMIND_1_DAY(10, "1天提醒"),

    /**
     * 3天提醒
     */
    REMIND_3_DAYS(20, "3天提醒"),

    /**
     * 已逾期
     */
    OVERDUE(30, "已逾期");

    private final Integer value;
    private final String description;

    /**
     * 根据数值获取对应的枚举
     *
     * @param value 数据库存储的值
     * @return 对应的枚举，如果找不到返回null
     */
    public static NotificationEventTypeEnum fromValue(Integer value) {
        for (NotificationEventTypeEnum type : NotificationEventTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查给定的值是否是有效的事件类型
     *
     * @param value 要检查的值
     * @return 如果是有效事件类型返回true，否则返回false
     */
    public static boolean isValid(Integer value) {
        for (NotificationEventTypeEnum type : NotificationEventTypeEnum.values()) {
            if (type.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
