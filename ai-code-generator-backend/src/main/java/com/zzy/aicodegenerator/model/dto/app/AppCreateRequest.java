package com.zzy.aicodegenerator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppCreateRequest implements Serializable {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用初始提示的 prompt（必填）
     */
    private String initPrompt;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
