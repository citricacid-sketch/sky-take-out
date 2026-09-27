package com.sky.config;

import com.sky.client.AiServiceClient;
import com.sky.properties.AiServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务客户端配置。
 *
 * <p>将 {@link AiServiceClient} 注册为 Spring Bean，供 Service 层注入使用。</p>
 */
@Configuration
public class AiServiceConfig {

    /**
     * 构造 AiServiceClient Bean。
     *
     * @param aiServiceProperties 配置属性（url/token/timeout）
     * @return 客户端实例
     */
    @Bean
    public AiServiceClient aiServiceClient(AiServiceProperties aiServiceProperties) {
        return new AiServiceClient(aiServiceProperties);
    }
}
