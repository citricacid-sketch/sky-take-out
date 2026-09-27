package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI 服务连接配置属性。
 *
 * <p>对应 application.yml 中的 {@code sky.ai.service.*} 配置项。</p>
 *
 * <p>默认值已设置，确保零配置即可启动（开发模式）。</p>
 */
@Component
@ConfigurationProperties(prefix = "sky.ai.service")
@Data
public class AiServiceProperties {

    /**
     * AI 服务提供方: python (默认)。
     *
     * <p>预留 java 选项用于回滚（需配合旧实现）。</p>
     */
    private String provider = "python";

    /**
     * Python AI 服务地址。
     *
     * <p>默认 http://127.0.0.1:8000。</p>
     */
    private String url = "http://127.0.0.1:8000";

    /**
     * 内部认证令牌（与 Python AI_SERVICE_TOKEN 保持一致）。
     *
     * <p>通过 X-Internal-Token header 发送，Python 端 HMAC 校验。</p>
     */
    private String token = "";

    /**
     * 调用超时（毫秒），LLM 通常 5-30s。
     */
    private Integer timeout = 30000;
}
