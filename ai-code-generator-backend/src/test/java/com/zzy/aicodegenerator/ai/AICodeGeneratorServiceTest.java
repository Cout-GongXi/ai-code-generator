package com.zzy.aicodegenerator.ai;

import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AICodeGeneratorServiceTest {
    @Resource
    private AICodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("请生成一个博客，包含一个标题和一个段落。 代码不超过 20 行");
        Assertions.assertNotNull(result);
//        System.out.println("生成的HTML代码：\n" + result);
    }

    @Test
    void generateMultiFileCode(){
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("请生成一个简单的多文件代码，包含HTML、CSS和JavaScript。 代码不超过 50 行");
        Assertions.assertNotNull(result);
    }
}
