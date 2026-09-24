package com.sky.service;

import java.util.List;

/**
 * 推荐服务接口
 *
 * @author zhangpj
 * @date 2026/9/24
 */
public interface RecommendService {

    /**
     * 获取用户推荐菜品ID列表（默认10个）
     *
     * @param userId 用户ID
     * @return 推荐菜品ID列表
     */
    List<Long> recommendDishIds(Long userId);
}
