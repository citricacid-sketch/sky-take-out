package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 大模型客户端（OpenAI 兼容接口）
 */
@Slf4j
@Component
public class LlmClient {

    private static final int TIMEOUT_MSEC = 30 * 1000;

    @Autowired
    private LlmProperties llmProperties;

    /**
     * 调用 LLM 聊天接口
     *
     * @param messages 消息列表，每个消息包含 role 和 content
     * @return 模型回复内容
     */
    public String chat(List<Map<String, String>> messages) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            // 构造请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", llmProperties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", llmProperties.getMaxTokens());
            requestBody.put("temperature", llmProperties.getTemperature());

            String requestJson = requestBody.toJSONString();
            log.info("LLM 请求: {}", requestJson);

            // 创建 POST 请求
            HttpPost httpPost = new HttpPost(llmProperties.getBaseUrl());

            // 设置请求头
            Map<String, String> headers = llmProperties.getHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            // 设置请求体
            StringEntity entity = new StringEntity(requestJson, "utf-8");
            entity.setContentEncoding("utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);

            // 设置超时
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_MSEC)
                    .setConnectionRequestTimeout(TIMEOUT_MSEC)
                    .setSocketTimeout(TIMEOUT_MSEC)
                    .build();
            httpPost.setConfig(config);

            // 执行请求
            response = httpClient.execute(httpPost);
            String responseJson = EntityUtils.toString(response.getEntity(), "UTF-8");

            log.info("LLM 响应: {}", responseJson);

            // 解析响应
            return parseResponse(responseJson);
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (Exception e) {
                log.error("关闭 HTTP 连接失败", e);
            }
        }
    }

    /**
     * 解析 OpenAI 兼容接口响应
     *
     * @param responseJson JSON 响应字符串
     * @return 模型回复内容
     */
    private String parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isEmpty()) {
            throw new RuntimeException("LLM 返回空响应");
        }

        JSONObject jsonObject = JSON.parseObject(responseJson);

        // 检查是否有错误
        if (jsonObject.containsKey("error")) {
            JSONObject error = jsonObject.getJSONObject("error");
            throw new RuntimeException("LLM 返回错误: " + error.getString("message"));
        }

        // 提取 choices[0].message.content
        JSONArray choices = jsonObject.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM 返回结果为空");
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            throw new RuntimeException("LLM 返回格式异常");
        }

        return message.getString("content");
    }
}
