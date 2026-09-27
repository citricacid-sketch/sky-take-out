package com.sky.service.impl;

import com.sky.client.AiServiceClient;
import com.sky.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 智能客服服务实现 — 委托给 Python AI 服务。
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired(required = false)
    private AiServiceClient aiServiceClient;

    @Override
    public String chat(Long userId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return "请输入您的问题，我来为您解答。";
        }

        if (aiServiceClient == null) {
            return "AI 客服暂不可用，请稍后再试。";
        }

        String traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String reply = aiServiceClient.chat(String.valueOf(userId), message, traceId);
        if (reply == null || reply.isEmpty()) {
            return "抱歉，AI 客服暂时无法处理您的请求，请稍后再试。";
        }
        return reply;
    }
}
