package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐预热定时任务
 * <p>
 * 每天凌晨 2 点预热所有近 7 天活跃用户的推荐缓存
 * </p>
 *
 * @author zhangpj
 * @date 2026/9/24
 */
@Component
@Slf4j
public class RecommendTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RecommendService recommendService;

    /**
     * 每天凌晨 2 点执行：预热近 7 天活跃用户的推荐缓存
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void warmUpRecommendCache() {
        log.info("开始预热用户推荐缓存，time={}", LocalDateTime.now());
        try {
            // 查询近 7 天有订单的用户（通过分页查询条件筛选）
            com.sky.dto.OrdersPageQueryDTO queryDTO = new com.sky.dto.OrdersPageQueryDTO();
            queryDTO.setBeginTime(LocalDateTime.now().minusDays(7));
            queryDTO.setEndTime(LocalDateTime.now());
            queryDTO.setStatus(Orders.COMPLETED);
            queryDTO.setPage(1);
            queryDTO.setPageSize(1000);
            com.github.pagehelper.Page<Orders> page = orderMapper.pagequery(queryDTO);

            List<Orders> orders = page.getResult();
            if (orders == null || orders.isEmpty()) {
                log.info("近 7 天无活跃用户，跳过预热");
                return;
            }

            // 去重后逐个预热
            java.util.Set<Long> userIds = new java.util.HashSet<>();
            for (Orders order : orders) {
                if (order.getUserId() != null) {
                    userIds.add(order.getUserId());
                }
            }

            int count = 0;
            for (Long userId : userIds) {
                try {
                    recommendService.recommendDishIds(userId);
                    count++;
                } catch (Exception e) {
                    log.warn("预热用户推荐缓存失败，userId={}", userId, e);
                }
            }
            log.info("用户推荐缓存预热完成，共处理 {} 个用户", count);
        } catch (Exception e) {
            log.error("预热用户推荐缓存异常", e);
        }
    }
}
