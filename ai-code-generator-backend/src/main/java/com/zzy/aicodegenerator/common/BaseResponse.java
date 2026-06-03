package com.zzy.aicodegenerator.common;

import com.zzy.aicodegenerator.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用的API响应类，包含错误码、数据和错误信息
 * @param <T> 数据类型
 */
@Data
//@NoArgsConstructor
public class BaseResponse<T> implements Serializable {
    private int code;

    private T data;

    private String message;
    /**
     * 构造函数，使用自定义错误码、数据和错误信息
     * @param code 错误码
     * @param data 数据
     * @param message 错误信息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
    /**
     * 构造函数，使用错误码和错误信息
     * @param code 错误码
     * @param message 错误信息
     */
    public BaseResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
    /**
     * 构造函数，使用错误码枚举类
     * @param errorCode 错误码枚举类
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
