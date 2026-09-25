package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 大模型相关配置属性
 */
@Component
@ConfigurationProperties(prefix = "sky.llm")
@Data
public class LlmProperties {

    /**
     * LLM API 密钥
     */
    private String apiKey;

    /**
     * LLM API 基础地址（OpenAI 兼容接口）
     */
    private String baseUrl = "https://api.openai.com/v1/chat/completions";

    /**
     * 模型名称
     */
    private String model = "gpt-3.5-turbo";

    /**
     * 最大生成 token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 温度参数，控制生成随机性
     */
    private Double temperature = 0.3;

    /**
     * 构造请求头 Map
     *
     * @return 包含认证和 Content-Type 的请求头
     */
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }
}
