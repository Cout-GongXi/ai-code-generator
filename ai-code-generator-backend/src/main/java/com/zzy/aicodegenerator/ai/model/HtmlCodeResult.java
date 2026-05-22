package com.zzy.aicodegenerator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * Html代码生成结果
 *
 * @author zzy
 * @date 2024/6/17
 */
@Data
@Description("Html代码生成结果")
public class HtmlCodeResult {
    @Description("生成的Html代码")
    private String htmlCode;

    @Description("生成的Html代码的描述")
    private String description;
}
