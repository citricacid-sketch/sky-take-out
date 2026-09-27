package com.sky.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.properties.AiServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Python AI 服务 HTTP 客户端。
 * 封装 chat / analysis / order-plan 三个端点，含超时、traceId、降级。
 */
@Slf4j
public class AiServiceClient {

    private static final String PATH_CHAT = "/api/v1/chat";
    private static final String PATH_ANALYSIS = "/api/v1/analysis/ask";
    private static final String PATH_ORDER_PLAN = "/api/v1/order/plan";

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(RestTemplate restTemplate, AiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 客服对话。
     *
     * @return reply, 失败返回 null (由调用方降级)
     */
    public String chat(String userId, String message, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("message", message);
        return postForText(PATH_CHAT, body, traceId);
    }

    /**
     * NL2SQL 数据分析。
     *
     * @return 中文总结, 失败返回 null
     */
    public String analysisAsk(String question, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        return postForText(PATH_ANALYSIS, body, traceId);
    }

    /**
     * 点餐推荐。
     *
     * @return JSON 字符串, 失败返回 null
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

    public boolean isPythonProvider() {
        return "python".equalsIgnoreCase(properties.getProvider());
    }

    public String getProvider() {
        return properties.getProvider();
    }

    // ===== internal =====

    private String postForText(String path, Map<String, Object> body, String traceId) {
        Map<String, Object> resp = postForMap(path, body, traceId);
        if (resp == null) {
            return null;
        }
        Object reply = resp.get("reply");
        return reply == null ? null : reply.toString();
    }

    private Map<String, Object> postForMap(String path, Map<String, Object> body, String traceId) {
        String url = properties.getUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getToken() != null && !properties.getToken().isEmpty()) {
            headers.set("Authorization", properties.getToken());
        }
        if (traceId != null) {
            headers.set("X-Trace-Id", traceId);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("AiServiceClient 调用 {} (traceId={})", path, traceId);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
                });
            }
            log.warn("AiServiceClient {} 返回非 2xx: {}", path, response.getStatusCode());
            return null;
        } catch (ResourceAccessException e) {
            log.error("AiServiceClient {} 网络异常 (traceId={}): {}", path, traceId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("AiServiceClient {} 调用失败 (traceId={})", path, traceId, e);
            return null;
        }
    }

    private Object parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return new java.util.ArrayList<>();
        }
    }
}
