package com.zzy.aicodegenerator.exception;

import lombok.Data;
/**
 * 业务异常类，继承自RuntimeException
 */
@Data
public class BusinessException extends RuntimeException{
    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数，使用错误码枚举类
     * @param errorCode 错误码枚举类
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.code = errorCode.getCode();
    }
    /**
     * 构造函数，使用自定义错误码和错误信息
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
