package com.zzy.aicodegenerator.core;

import cn.hutool.json.JSONUtil;
import com.zzy.aicodegenerator.ai.AICodeGeneratorService;
import com.zzy.aicodegenerator.ai.AICodeGeneratorServiceFactory;
import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import com.zzy.aicodegenerator.ai.model.message.AiResponseMessage;
import com.zzy.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.zzy.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.zzy.aicodegenerator.constant.AppConstant;
import com.zzy.aicodegenerator.core.builder.VueProjectBuilder;
import com.zzy.aicodegenerator.core.parser.CodeParserExecutor;
import com.zzy.aicodegenerator.core.saver.CodeFileSaverExecutor;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，提供统一的接口供外部调用，隐藏内部复杂的实现细节。
 */
@Slf4j
@Service
public class AICodeGeneratorFacade {
    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage     用户输入的消息
     * @param codeGenTypeEnum 代码生成类型枚举
     * @param appId           应用ID
     * @return 生成的代码字符串
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据 AppId 获取对应的 AI 代码生成器服务
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        // 根据不同的代码生成类型调用不同的生成方法
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMsg = "不支持的代码生成类型: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMsg);
            }
        };
    }

    /**
     * 统一入口，根据类型生成并保存代码(流式)
     *
     * @param userMessage     用户输入的消息
     * @param codeGenTypeEnum 代码生成类型枚举
     * @param appId           应用ID
     * @return 生成的代码字符串
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据 AppId 获取对应的 AI 代码生成器服务
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        // 根据不同的代码生成类型调用不同的生成方法
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processCodeStream(tokenStream, appId);
            }
            default -> {
                String errorMsg = "不支持的代码生成类型: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMsg);
            }
        };
    }

    /**
     * 将TokenStream转换为流式响应，并传递工具调用消息
     *
     * @param tokenStream 代码流
     * @return 流式响应
     */
    private Flux<String> processCodeStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            }).onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                sink.next(JSONUtil.toJsonStr(toolRequestMessage));
            }).onToolExecuted((toolExecution) -> {
                ToolExecutedMessage toolExecuteMessage = new ToolExecutedMessage(toolExecution);
                sink.next(JSONUtil.toJsonStr(toolExecuteMessage));
            }).onCompleteResponse((chatResponse) -> {
                // 执行 Vue 项目构建 （同步执行，确保预览时项目就绪）
                String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                vueProjectBuilder.buildProjectAsync(projectPath);
                sink.complete();
            }).onError((error) -> {
                error.printStackTrace();
                sink.error(error);
            }).start();
        });
    }

    /**
     * 通用的流式处理逻辑
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型枚举
     * @param appId       应用ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {

        // 字符串拼接器，用于当流式返回所有代码之后，在保存代码
        StringBuilder codeBuilder = new StringBuilder();

        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        // 当流式返回完成后，保存代码
                        String completeCode = codeBuilder.toString();
                        // 解析完整的代码内容
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 将解析后的代码保存到文件
                        File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("多文件代码生成并保存完成，文件路径: {}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存多文件代码时发生错误: {}", e.getMessage());
                    }
                });
    }

}
