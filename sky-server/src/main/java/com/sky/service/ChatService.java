package com.sky.service;

/**
 * 智能客服服务
 */
public interface ChatService {

    /**
     * 用户发送消息，返回客服回复
     *
     * @param userId  用户 ID
     * @param message 用户消息
     * @return 客服回复文本
     */
    String chat(Long userId, String message);
}
