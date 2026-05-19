package com.zzy.aicodegenerator.exception;

import lombok.Getter;
/**
 * 错误码枚举类
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGGED_IN(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限访问"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部错误"),
    OPERATION_ERROR(50001, "业务操作失败");
    /**
     * 错误码
     */
    private final int code;
    /**
     * 错误信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
