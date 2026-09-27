package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI 服务连接配置
 */
@Component
@ConfigurationProperties(prefix = "sky.ai.service")
@Data
public class AiServiceProperties {

    /**
     * AI 服务提供方: python (默认) / java
     */
    private String provider = "python";

    /**
     * Python AI 服务地址
     */
    private String url = "http://127.0.0.1:8000";

    /**
     * 内部认证令牌 (与 Python AI_SERVICE_TOKEN 保持一致)
     */
    private String token = "";

    /**
     * 调用超时 (毫秒)，LLM 通常 5-30s
     */
    private Integer timeout = 30000;
}
