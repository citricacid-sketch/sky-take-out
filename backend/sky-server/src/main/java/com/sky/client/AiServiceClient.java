package com.sky.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.properties.AiServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Python AI 服务 HTTP 客户端。
 *
 * <p>封装对 Python ai-service 的三个端点调用：</p>
 * <ul>
 *   <li>{@code /api/v1/chat} - 客服对话</li>
 *   <li>{@code /api/v1/analysis/ask} - NL2SQL 数据分析</li>
 *   <li>{@code /api/v1/order/plan} - 点餐推荐</li>
 * </ul>
 *
 * <p>认证方式：{@code X-Internal-Token} header（HMAC 校验）。</p>
 *
 * <p>失败降级：网络异常/非 2xx 返回 {@code null}，由调用方处理。</p>
 */
@Slf4j
public class AiServiceClient {

    private static final String PATH_CHAT = "/api/v1/chat";
    private static final String PATH_ANALYSIS = "/api/v1/analysis/ask";
    private static final String PATH_ORDER_PLAN = "/api/v1/order/plan";

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 构造客户端（使用默认 RestTemplate）。 */
    public AiServiceClient(AiServiceProperties properties) {
        this(properties, new RestTemplate());
    }

    /** 构造客户端（注入 RestTemplate，用于测试）。 */
    public AiServiceClient(AiServiceProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /** 判断当前是否为 Python provider。 */
    public boolean isPythonProvider() {
        return "python".equalsIgnoreCase(properties.getProvider());
    }

    /**
     * 客服对话（简单版）。
     *
     * @param userId 用户 ID
     * @param message 用户消息
     * @param traceId 追踪 ID（日志用）
     * @return AI 回复文本，失败返回 null
     */
    public String chat(String userId, String message, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("message", message);
        return postForText(PATH_CHAT, body, traceId);
    }

    /**
     * 客服对话（带上下文）。
     *
     * @param body 完整请求体（含 userId/message/context）
     * @param traceId 追踪 ID
     * @return AI 回复文本
     */
    public String chatWithBody(Map<String, Object> body, String traceId) {
        return postForText(PATH_CHAT, body, traceId);
    }

    /**
     * NL2SQL 数据分析。
     *
     * @param question 自然语言问题
     * @param traceId 追踪 ID
     * @return AI 分析结论
     */
    public String analysisAsk(String question, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        return postForText(PATH_ANALYSIS, body, traceId);
    }

    /**
     * 点餐推荐。
     *
     * @param people 人数
     * @param budget 预算（0=不限）
     * @param tastesJson 口味偏好 JSON 数组
     * @param excludesJson 忌口 JSON 数组
     * @param traceId 追踪 ID
     * @return 推荐结果 JSON 字符串
     */
    public String orderPlan(int people, double budget, String tastesJson, String excludesJson, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("people", people);
        body.put("budget", budget);
        body.put("tastes", parseJsonArray(tastesJson));
        body.put("excludes", parseJsonArray(excludesJson));
        Map<String, Object> resp = postForMap(PATH_ORDER_PLAN, body, traceId);
        if (resp == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(resp);
        } catch (Exception e) {
            log.error("序列化点餐推荐结果失败", e);
            return null;
        }
    }

    // ===== internal =====

    /** 从响应中提取文本字段（兼容 reply / answer）。 */
    private String postForText(String path, Map<String, Object> body, String traceId) {
        Map<String, Object> resp = postForMap(path, body, traceId);
        if (resp == null) {
            return null;
        }
        // 兼容 chat 端点（reply）和 analysis 端点（answer）
        Object text = resp.get("reply");
        if (text == null) {
            text = resp.get("answer");
        }
        return text == null ? null : text.toString();
    }

    /**
     * 发送 POST 请求并解析 JSON 响应。
     *
     * <p>header 自动携带 {@code X-Internal-Token} 和 {@code X-Trace-Id}。</p>
     *
     * @param path 端点路径
     * @param body 请求体
     * @param traceId 追踪 ID（可为 null）
     * @return 解析后的 Map，失败返回 null
     */
    private Map<String, Object> postForMap(String path, Map<String, Object> body, String traceId) {
        String url = properties.getUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", properties.getToken());
        if (traceId != null) {
            headers.set("X-Trace-Id", traceId);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("AiServiceClient 调用 {} (traceId={})", path, traceId);
            var response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            }
            log.warn("AiServiceClient {} 返回非 2xx: {}", path, response.getStatusCode());
            return null;
        } catch (RestClientException e) {
            log.error("AiServiceClient {} 调用失败: {}", path, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("AiServiceClient {} 解析失败", path, e);
            return null;
        }
    }

    /** 解析 JSON 数组字符串，失败返回空列表。 */
    private Object parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return new java.util.ArrayList<>();
        }
    }
}
