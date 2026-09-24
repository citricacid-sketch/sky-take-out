package com.sky.service;

import com.sky.dto.DeliveryQueryDTO;
import com.sky.entity.Delivery;
import com.sky.result.PageResult;

/**
 * 配送单服务接口
 */
public interface DeliveryService {

    /**
     * 订单接单后创建配送单（订单状态3已接单 → 创建delivery，status=0待分配）
     */
    void createDelivery(Long orderId);

    /**
     * 自动分配骑手（核心调度逻辑）
     * @return 分配到的骑手id，若无可用骑手返回null
     */
    Long autoAssignRider(Long deliveryId);

    /**
     * 手动分配骑手
     */
    void manualAssignRider(Long deliveryId, Long riderId);

    /**
     * 更新配送单状态
     */
    void updateDeliveryStatus(Long deliveryId, Integer status);

    /**
     * 根据订单id查询配送单
     */
    Delivery getDeliveryByOrderId(Long orderId);

    /**
     * 根据id查询配送单
     */
    Delivery getById(Long id);

    /**
     * 分页查询配送单
     */
    PageResult pageQuery(DeliveryQueryDTO dto);
}
