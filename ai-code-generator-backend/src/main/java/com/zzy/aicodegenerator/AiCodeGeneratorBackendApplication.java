package com.zzy.aicodegenerator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zzy.aicodegenerator.mapper")
public class AiCodeGeneratorBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeGeneratorBackendApplication.class, args);
    }

}
