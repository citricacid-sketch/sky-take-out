package com.sky.service;

/**
 * AI 数据分析助手服务
 */
public interface AIAssistantService {

    /**
     * 自然语言提问，返回分析结果
     *
     * @param question 自然语言问题
     * @return 回答文本（含数据）
     */
    String ask(String question);
}
