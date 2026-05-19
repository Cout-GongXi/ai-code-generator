package com.zzy.aicodegenerator.exception;

import com.zzy.aicodegenerator.common.BaseResponse;
import com.zzy.aicodegenerator.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalException {
    /**
     * 处理业务异常，返回错误码和错误信息
     *
     * @param e 业务异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常，返回系统错误码和通用错误信息
     * @param e 运行时异常对象
     * @return 错误响应对象
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "服务器内部错误");
    }
}
