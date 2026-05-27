package com.zzy.aicodegenerator.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .ttl(ttl);
        if (StringUtils.hasText(password)) {
            builder.user(StringUtils.hasText(username) ? username : "default")
                    .password(password);
        }
        return builder.build();
    }
}
