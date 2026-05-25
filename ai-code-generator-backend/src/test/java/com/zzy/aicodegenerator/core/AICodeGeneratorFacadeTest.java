package com.zzy.aicodegenerator.core;

import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AICodeGeneratorFacadeTest {
    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        String userMessage = "请生成一个简单的博客页面，要求代码不超过100行";
        aiCodeGeneratorFacade.generateAndSaveCode(userMessage, CodeGenTypeEnum.HTML, 1L);
    }

    @Test
    void generateAndSaveCodeSteam() {
        String userMessage = "请生成一个简单的博客页面，要求代码不超过200行";
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateAndSaveCodeSteam(userMessage, CodeGenTypeEnum.MULTI_FILE, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = stringFlux.collectList().block();
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }


}