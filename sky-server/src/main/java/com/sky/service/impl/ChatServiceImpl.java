package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ChatService;
import com.sky.utils.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 智能客服服务实现
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final String CHAT_HISTORY_KEY = "chat_history_";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final int HISTORY_TTL_MINUTES = 30;
    private static final int CONTEXT_ORDER_COUNT = 3;
    private static final String SYSTEM_PROMPT = "你是苍穹外卖的智能客服助手。你可以帮助用户：\n"
            + "1. 查询订单状态与历史订单\n"
            + "2. 解答退款、取消订单等售后问题\n"
            + "3. 推荐菜品、介绍套餐\n"
            + "4. 解答配送范围、配送时间等问题\n"
            + "请注意：你只能执行查询和引导操作，不能执行任何写操作（如下单、修改订单、删除数据等）。\n"
            + "回答要简洁友好，使用中文。";

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String chat(Long userId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return "请输入您的问题，我来为您解答。";
        }

        // 1. 取历史消息
        List<Map<String, String>> history = getHistory(userId);

        // 2. 组装发送给 LLM 的消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统 prompt（每次注入，放在最前）
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPromptWithContext(userId));
        messages.add(systemMessage);

        // 历史消息
        messages.addAll(history);

        // 用户新消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);

        // 3. 调用 LLM
        String reply = llmClient.chat(messages);

        // 4. 保存本轮用户消息和助手回复到 Redis 历史
        saveMessage(userId, "user", message);
        saveMessage(userId, "assistant", reply);

        return reply;
    }

    /**
     * 获取用户会话历史
     */
    private List<Map<String, String>> getHistory(Long userId) {
        String key = CHAT_HISTORY_KEY + userId;
        try {
            List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
            List<Map<String, String>> history = new ArrayList<>();
            if (rawList != null) {
                for (Object item : rawList) {
                    history.add((Map<String, String>) item);
                }
            }
            return history;
        } catch (Exception e) {
            log.warn("读取聊天历史失败，userId：{}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存单条消息到 Redis 历史
     */
    private void saveMessage(Long userId, String role, String content) {
        String key = CHAT_HISTORY_KEY + userId;
        try {
            Map<String, String> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            redisTemplate.opsForList().rightPush(key, message);
            // 保留最近 MAX_HISTORY_SIZE 条
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_HISTORY_SIZE) {
                redisTemplate.opsForList().trim(key, size - MAX_HISTORY_SIZE, -1);
            }
            // 刷新 TTL
            redisTemplate.expire(key, HISTORY_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("保存聊天历史失败，userId：{}", userId, e);
        }
    }

    /**
     * 构造带上下文的系统 prompt
     */
    private String buildSystemPromptWithContext(Long userId) {
        StringBuilder contextBuilder = new StringBuilder(SYSTEM_PROMPT);

        // 注入用户最近 3 笔订单
        try {
            List<Orders> recentOrders = orderMapper.getRecentByUserId(userId, CONTEXT_ORDER_COUNT);
            if (recentOrders != null && !recentOrders.isEmpty()) {
                contextBuilder.append("\n\n该用户最近的订单信息：\n");
                for (Orders order : recentOrders) {
                    contextBuilder.append("- 订单号：").append(order.getNumber())
                            .append("，状态：").append(resolveStatus(order.getStatus()))
                            .append("，金额：").append(order.getAmount())
                            .append("，下单时间：").append(order.getOrderTime())
                            .append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("注入订单上下文失败，userId：{}", userId, e);
        }

        // 注入店铺状态
        try {
            Integer shopStatus = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
            if (shopStatus != null) {
                contextBuilder.append("\n当前店铺状态：")
                        .append(shopStatus == 1 ? "营业中" : "打烊中")
                        .append("\n");
            }
        } catch (Exception e) {
            log.warn("注入店铺状态失败", e);
        }

        return contextBuilder.toString();
    }

    /**
     * 解析订单状态为中文描述
     */
    private String resolveStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "待付款";
            case 2:
                return "待接单";
            case 3:
                return "已接单";
            case 4:
                return "派送中";
            case 5:
                return "已完成";
            case 6:
                return "已取消";
            default:
                return "未知";
        }
    }
}
