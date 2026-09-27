package com.sky.config;

import com.sky.client.AiServiceClient;
import com.sky.properties.AiServiceProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * AI 服务客户端配置：构造带超时的 RestTemplate 与 AiServiceClient。
 */
@Configuration
public class AiServiceConfig {

    @Bean
    public RestTemplate aiServiceRestTemplate(AiServiceProperties aiServiceProperties) {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(aiServiceProperties.getTimeout()))
                .setReadTimeout(Duration.ofMillis(aiServiceProperties.getTimeout()))
                .build();
    }

    @Bean
    public AiServiceClient aiServiceClient(RestTemplate aiServiceRestTemplate,
                                          AiServiceProperties aiServiceProperties) {
        return new AiServiceClient(aiServiceRestTemplate, aiServiceProperties);
    }
}
