package com.sky.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 推荐服务实现 - 猜你喜欢
 * <p>
 * 基于用户历史订单菜品偏好 + 热销兜底，结果缓存1小时
 * </p>
 *
 * @author zhangpj
 * @date 2026/9/24
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 推荐菜品数量（默认）
     */
    private static final int DEFAULT_RECOMMEND_SIZE = 10;

    /**
     * 用户偏好查询 TopN
     */
    private static final int FREQUENT_LIMIT = 20;

    /**
     * Redis 缓存 key 前缀
     */
    private static final String REDIS_KEY_PREFIX = "recommend_";

    /**
     * 缓存有效期（秒）- 1小时
     */
    private static final long CACHE_TTL_SECONDS = 3600L;

    @Override
    public List<Long> recommendDishIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 先查 Redis 缓存
        String key = REDIS_KEY_PREFIX + userId;
        List<Long> cached = getFromCache(key);
        if (cached != null) {
            log.info("推荐菜品命中缓存，userId={}", userId);
            return cached;
        }

        // 2. 查询用户历史订单中的菜品偏好（按出现次数降序 Top20）
        List<Long> dishIds = orderMapper.getFrequentlyOrderedDishIds(userId, FREQUENT_LIMIT);

        // 3. 冷启动兜底：无订单则取近 30 天热销 Top10
        if (dishIds == null || dishIds.isEmpty()) {
            log.info("用户无历史订单，使用热销兜底，userId={}", userId);
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime begin = end.minusDays(30);
            List<?> hotSales = orderMapper.getGoodsSales(begin, end);
            dishIds = new ArrayList<>();
            if (hotSales != null) {
                // getGoodsSales 返回的是 GoodsSalesDTO（name, number），无法直接映射到 dishId
                // 这里按名称匹配起售菜品，取 Top10
                for (Object item : hotSales) {
                    if (dishIds.size() >= DEFAULT_RECOMMEND_SIZE) {
                        break;
                    }
                    try {
                        // 通过反射或强制转换获取 name
                        String name = item.getClass().getMethod("getName").invoke(item).toString();
                        Dish condition = new Dish();
                        condition.setName(name);
                        condition.setStatus(StatusConstant.ENABLE);
                        List<Dish> dishes = dishMapper.list(condition);
                        if (dishes != null && !dishes.isEmpty()) {
                            Long id = dishes.get(0).getId();
                            if (!dishIds.contains(id)) {
                                dishIds.add(id);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("热销菜品匹配失败，跳过", e);
                    }
                }
            }
        }

        // 截断到默认推荐数量
        if (dishIds.size() > DEFAULT_RECOMMEND_SIZE) {
            dishIds = dishIds.subList(0, DEFAULT_RECOMMEND_SIZE);
        }

        // 4. 写入 Redis 缓存，TTL 1 小时
        putToCache(key, dishIds);

        return dishIds;
    }

    /**
     * 从 Redis 读取缓存
     */
    private List<Long> getFromCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            String json = value.toString();
            if (json.isEmpty() || "null".equals(json)) {
                return null;
            }
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            log.warn("读取推荐缓存失败，key={}", key, e);
            return null;
        }
    }

    /**
     * 写入 Redis 缓存
     */
    private void putToCache(String key, List<Long> dishIds) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(dishIds);
            redisTemplate.opsForValue().set(key, json);
            // 设置过期时间：通过再次调用 expire（StringRedisSerializer 下 JDK 序列化不会干扰字符串值）
            redisTemplate.expire(key, CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入推荐缓存失败，key={}", key, e);
        }
    }
}
