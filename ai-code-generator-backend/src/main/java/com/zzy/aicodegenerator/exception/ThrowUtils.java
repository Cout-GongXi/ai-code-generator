package com.zzy.aicodegenerator.exception;
/**
 * 抛出异常的工具类
 */
public class ThrowUtils {
    /**
     * 如果条件为true，则抛出指定的运行时异常
     *
     * @param condition        条件
     * @param runtimeException 运行时异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }
    /**
     * 如果条件为true，则抛出指定的业务异常
     *
     * @param condition 条件
     * @param errorCode 业务异常错误码枚举类
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }
    /**
     * 如果条件为true，则抛出指定的业务异常，并使用自定义错误信息
     *
     * @param condition 条件
     * @param errorCode 业务异常错误码枚举类
     * @param message   自定义错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }


}
