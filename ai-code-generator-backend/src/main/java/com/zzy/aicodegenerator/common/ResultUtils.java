package com.zzy.aicodegenerator.common;

import com.zzy.aicodegenerator.exception.ErrorCode;

public class ResultUtils {
    /**
     * 成功响应，使用默认的状态码200和消息"success"，并包含数据
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的BaseResponse对象
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, data, "success");
    }

    /**
     * 错误响应，使用指定的错误码和错误信息
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 错误的BaseResponse对象
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 错误响应，使用指定的错误码枚举类
     *
     * @param errorCode 错误码枚举类
     * @param <T>       数据类型
     * @return 错误的BaseResponse对象
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 错误响应，使用指定的错误码枚举类和自定义错误信息
     *
     * @param errorCode 错误码枚举类
     * @param message   自定义错误信息
     * @param <T>       数据类型
     * @return 错误的BaseResponse对象
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
