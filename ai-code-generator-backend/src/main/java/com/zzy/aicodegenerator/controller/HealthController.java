package com.zzy.aicodegenerator.controller;

import com.zzy.aicodegenerator.common.BaseResponse;
import com.zzy.aicodegenerator.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    /**
     * 健康检查接口，返回字符串 "ok"
     *
     * @return 健康检查结果
     */
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
