package com.springleaf.thinkdo.enums;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(0, "操作成功"),
    FAIL(1, "操作失败"),

    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_ALREADY_EXIST(1004, "用户已存在"),

    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),

    PARAM_ERROR(400, "参数错误"),
    SYSTEM_ERROR(500, "系统错误");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
