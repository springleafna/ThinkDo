package com.springleaf.thinkdo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记收藏状态枚举
 */
@Getter
@AllArgsConstructor
public enum NoteFavoritedEnum {

    /**
     * 未收藏
     */
    NOT_FAVORITED(0, "未收藏"),

    /**
     * 已收藏
     */
    FAVORITED(1, "已收藏");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static NoteFavoritedEnum getByCode(Integer code) {
        if (code == null) {
            return NOT_FAVORITED;
        }
        for (NoteFavoritedEnum favoritedEnum : values()) {
            if (favoritedEnum.getCode().equals(code)) {
                return favoritedEnum;
            }
        }
        return NOT_FAVORITED;
    }

    /**
     * 校验状态是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return true;
        }
        for (NoteFavoritedEnum favoritedEnum : values()) {
            if (favoritedEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取相反的状态
     */
    public NoteFavoritedEnum toggle() {
        return this == NOT_FAVORITED ? FAVORITED : NOT_FAVORITED;
    }
}
