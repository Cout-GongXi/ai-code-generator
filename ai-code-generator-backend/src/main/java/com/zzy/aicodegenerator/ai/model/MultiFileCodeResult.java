package com.zzy.aicodegenerator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件代码生成结果
 *
 * @author zzy
 * @date 2024/6/17
 */
@Data
@Description("多文件代码生成结果")
public class MultiFileCodeResult {
    /**
     * 生成的HTML代码
     */
    @Description("生成的HTML代码")
    private String htmlCode;

    /**
     * 生成的CSS代码
     */
    @Description("生成的CSS代码")
    private String cssCode;

    /**
     * 生成的JavaScript代码
     */
    @Description("生成的JavaScript代码")
    private String jsCode;

    /**
     * 描述
     */
    @Description("描述")
    private String description;
}
