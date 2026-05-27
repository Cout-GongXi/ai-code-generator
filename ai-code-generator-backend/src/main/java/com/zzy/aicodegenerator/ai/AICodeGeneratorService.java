package com.zzy.aicodegenerator.ai;

import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * AI代码生成服务接口
 *
 * @author zzy
 */
public interface AICodeGeneratorService {
    /**
     * 生成html代码
     *
     * @param userMessages 生成代码的提示信息
     * @return AI 输出结果
     */
    @SystemMessage(fromResource = "prompts/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessages);


    /**
     * 生成多文件代码
     *
     * @param userMessages 生成代码的提示信息
     * @return AI 输出结果
     */
    @SystemMessage(fromResource = "prompts/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessages);

    /**
     * 生成html代码
     *
     * @param userMessages 生成代码的提示信息
     * @return AI 输出结果
     */
    @SystemMessage(fromResource = "prompts/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessages);


    /**
     * 生成多文件代码
     *
     * @param userMessages 生成代码的提示信息
     * @return AI 输出结果
     */
    @SystemMessage(fromResource = "prompts/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessages);

    /**
     * 生成 Vue 项目代码
     *
     * @param userMessages 生成代码的提示信息
     * @return AI 输出结果
     */
    @SystemMessage(fromResource = "prompts/codegen-vue-project-system-prompt.txt")
    Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessages);
}
