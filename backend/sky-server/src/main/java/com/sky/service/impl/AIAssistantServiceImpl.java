package com.sky.service.impl;

import com.sky.client.AiServiceClient;
import com.sky.service.AIAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 数据分析助手服务实现。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>校验输入（空问题拦截）</li>
 *   <li>调用 Python ai-service /api/v1/analysis/ask (NL2SQL)</li>
 *   <li>失败降级（返回友好提示）</li>
 * </ol>
 *
 * <p>NL2SQL 流程由 Python 端完成：自然语言 → SQL → 执行 → 中文总结。</p>
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

        // 生成 traceId 便于日志追踪
        String traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String answer = aiServiceClient.analysisAsk(question, traceId);
        if (answer == null || answer.isEmpty()) {
            return "抱歉，AI 助手暂时无法处理您的请求，请稍后再试。";
        }
        return answer;
    }
}
