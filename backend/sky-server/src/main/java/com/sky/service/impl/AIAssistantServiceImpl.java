package com.sky.service.impl;

import com.sky.client.AiServiceClient;
import com.sky.service.AIAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 数据分析助手服务实现 — 委托给 Python AI 服务。
 */
@Service
@Slf4j
public class AIAssistantServiceImpl implements AIAssistantService {

    @Autowired(required = false)
    private AiServiceClient aiServiceClient;

    @Override
    public String ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题。";
        }

        if (aiServiceClient == null) {
            return "AI 服务未配置，请联系管理员。";
        }

        String traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String answer = aiServiceClient.analysisAsk(question, traceId);
        if (answer == null || answer.isEmpty()) {
            return "抱歉，AI 助手暂时无法处理您的请求，请稍后再试。";
        }
        return answer;
    }
}
