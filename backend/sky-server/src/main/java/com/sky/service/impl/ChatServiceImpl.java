package com.sky.service.impl;

import com.sky.client.AiServiceClient;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智能客服服务实现。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>校验输入（空消息拦截）</li>
 *   <li>构造用户上下文（最近 3 笔订单 + 店铺状态）</li>
 *   <li>调用 Python ai-service /api/v1/chat</li>
 *   <li>失败降级（返回友好提示）</li>
 * </ol>
 *
 * <p>上下文注入让 AI 能回答"我的订单到哪了"等个性化问题。</p>
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired(required = false)
    private AiServiceClient aiServiceClient;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String chat(Long userId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return "请输入您的问题，我来为您解答。";
        }

        if (aiServiceClient == null) {
            return "AI 客服暂不可用，请稍后再试。";
        }

        // 构造带上下文的 body（userId + message + context）
        Map<String, Object> body = new HashMap<>();
        body.put("userId", String.valueOf(userId));
        body.put("message", message);
        body.put("context", buildContext(userId));

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String reply = aiServiceClient.chatWithBody(body, traceId);
        if (reply == null || reply.isEmpty()) {
            return "抱歉，AI 客服暂时无法处理您的请求，请稍后再试。";
        }
        return reply;
    }

    /**
     * 构造用户上下文：最近 3 笔订单 + 店铺状态。
     *
     * <p>异常不影响主流程（降级为无上下文）。</p>
     *
     * @param userId 用户 ID
     * @return context Map（可能包含 recentOrders / shopStatus）
     */
    private Map<String, Object> buildContext(Long userId) {
        Map<String, Object> context = new HashMap<>();
        try {
            List<Orders> recentOrders = orderMapper.getRecentByUserId(userId, 3);
            if (recentOrders != null && !recentOrders.isEmpty()) {
                context.put("recentOrders", recentOrders);
            }
        } catch (Exception e) {
            log.warn("注入订单上下文失败, userId={}", userId, e);
        }
        try {
            Integer shopStatus = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
            if (shopStatus != null) {
                context.put("shopStatus", shopStatus == 1 ? "营业中" : "打烊中");
            }
        } catch (Exception e) {
            log.warn("注入店铺状态失败", e);
        }
        return context;
    }
}
