package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库作用域枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeScopeEnum {
    /**
     * 系统知识库：管理员创建和管理
     */
    SYSTEM("SYSTEM", "系统知识库"),

    /**
     * 用户知识库：普通用户创建和管理
     */
    USER("USER", "用户知识库");

    private final String value;
    private final String description;

    /**
     * 根据字符串值获取对应的枚举
     * @param value 数据库存储的值
     * @return 对应的枚举，如果找不到返回null
     */
    public static KnowledgeScopeEnum fromValue(String value) {
        for (KnowledgeScopeEnum scope : KnowledgeScopeEnum.values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        return null;
    }

    /**
     * 检查给定的字符串是否是有效的作用域值
     * @param value 要检查的值
     * @return 如果是有效作用域值返回true，否则返回false
     */
    public static boolean isValid(String value) {
        for (KnowledgeScopeEnum scope : KnowledgeScopeEnum.values()) {
            if (scope.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
