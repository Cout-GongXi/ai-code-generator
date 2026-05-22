package com.zzy.aicodegenerator.ai;

import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * AI代码生成服务接口
 *
 * @author zzy
 * @date 2024/6/10
 */
public interface AICodeGeneratorService {
    /**
     * 生成html代码
     *
     * @param userMessages 生成代码的提示信息
     * @return 生成的代码
     */
    @SystemMessage(fromResource = "prompts/html_code_generation_prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessages);


    /**
     * 生成多文件代码
     *
     * @param userMessages 生成代码的提示信息
     * @return 生成的代码
     */
    @SystemMessage(fromResource = "prompts/multi_file_code_generation_prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessages);

    /**
     * 生成html代码
     *
     * @param userMessages 生成代码的提示信息
     * @return 生成的代码
     */
    @SystemMessage(fromResource = "prompts/html_code_generation_prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessages);


    /**
     * 生成多文件代码
     *
     * @param userMessages 生成代码的提示信息
     * @return 生成的代码
     */
    @SystemMessage(fromResource = "prompts/multi_file_code_generation_prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessages);
}
