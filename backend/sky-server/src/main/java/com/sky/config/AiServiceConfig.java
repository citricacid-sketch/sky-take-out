package com.sky.config;

import com.sky.client.AiServiceClient;
import com.sky.properties.AiServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务客户端配置。
 */
@Configuration
public class AiServiceConfig {

    @Bean
    public AiServiceClient aiServiceClient(AiServiceProperties aiServiceProperties) {
        return new AiServiceClient(aiServiceProperties);
    }
}
