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
 */
@Slf4j
public class AiServiceClient {

    private static final String PATH_CHAT = "/api/v1/chat";
    private static final String PATH_ANALYSIS = "/api/v1/analysis/ask";
    private static final String PATH_ORDER_PLAN = "/api/v1/order/plan";

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(AiServiceProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    public boolean isPythonProvider() {
        return "python".equalsIgnoreCase(properties.getProvider());
    }

    public String chat(String userId, String message, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("message", message);
        return postForText(PATH_CHAT, body, traceId);
    }

    public String chatWithBody(Map<String, Object> body, String traceId) {
        return postForText(PATH_CHAT, body, traceId);
    }

    public String analysisAsk(String question, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        return postForText(PATH_ANALYSIS, body, traceId);
    }

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
        // 始终发送 token（无论是否为空，Python 端会处理）
        headers.set("X-Internal-Token", properties.getToken());
        if (traceId != null) {
            headers.set("X-Trace-Id", traceId);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("AiServiceClient 调用 {} token={}", path, properties.getToken());
            var response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), new TypeReference<>() {
                });
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

    private Object parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return new java.util.ArrayList<>();
        }
    }
}
