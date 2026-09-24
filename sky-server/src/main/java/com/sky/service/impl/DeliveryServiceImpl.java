package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DeliveryQueryDTO;
import com.sky.entity.Delivery;
import com.sky.entity.Orders;
import com.sky.entity.Rider;
import com.sky.mapper.DeliveryMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.RiderMapper;
import com.sky.result.PageResult;
import com.sky.service.DeliveryService;
import com.sky.utils.BaiduMapUtilFinal;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送单服务实现
 */
@Service
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BaiduMapUtilFinal baiduMapUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void createDelivery(Long orderId) {
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setStatus(0); // 待分配
        delivery.setCreateTime(LocalDateTime.now());
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.insert(delivery);
        log.info("创建配送单成功，orderId：{}，deliveryId：{}", orderId, delivery.getId());
    }

    @Override
    @Transactional
    public Long autoAssignRider(Long deliveryId) {
        // 1. 查询配送单
        Delivery delivery = deliveryMapper.getById(deliveryId);
        if (delivery == null) {
            log.warn("配送单不存在，deliveryId：{}", deliveryId);
            return null;
        }

        // 2. 获取所有空闲骑手
        List<Rider> availableRiders = riderMapper.getAvailableRiders();
        if (availableRiders == null || availableRiders.isEmpty()) {
            log.warn("暂无空闲骑手，等待分配，deliveryId：{}", deliveryId);
            return null;
        }

        // 3. 获取订单位置（地址）
        Orders order = orderMapper.getById(delivery.getOrderId());
        Rider selectedRider = selectNearestRider(availableRiders, order);

        if (selectedRider == null) {
            // 兜底：按创建时间选第一个空闲骑手
            selectedRider = availableRiders.get(0);
        }

        // 4. 更新配送单
        delivery.setRiderId(selectedRider.getId());
        delivery.setStatus(1); // 已分配
        delivery.setAssignTime(LocalDateTime.now());
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.update(delivery);

        // 5. 更新骑手状态为配送中
        riderMapper.updateLocation(selectedRider.getId(), selectedRider.getLongitude(), selectedRider.getLatitude());
        Rider riderUpdate = Rider.builder()
                .id(selectedRider.getId())
                .status(2) // 配送中
                .updateTime(LocalDateTime.now())
                .build();
        riderMapper.update(riderUpdate);

        // 6. 通过WebSocket推送配送通知
        pushDeliveryNotification(delivery.getOrderId(), selectedRider.getName());

        log.info("自动分配骑手成功，deliveryId：{}，riderId：{}", deliveryId, selectedRider.getId());
        return selectedRider.getId();
    }

    @Override
    @Transactional
    public void manualAssignRider(Long deliveryId, Long riderId) {
        Delivery delivery = deliveryMapper.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("配送单不存在");
        }
        Rider rider = riderMapper.getById(riderId);
        if (rider == null) {
            throw new RuntimeException("骑手不存在");
        }

        // 更新配送单
        delivery.setRiderId(riderId);
        delivery.setStatus(1); // 已分配
        delivery.setAssignTime(LocalDateTime.now());
        delivery.setUpdateTime(LocalDateTime.now());
        deliveryMapper.update(delivery);

        // 更新骑手状态
        Rider riderUpdate = Rider.builder()
                .id(riderId)
                .status(2) // 配送中
                .updateTime(LocalDateTime.now())
                .build();
        riderMapper.update(riderUpdate);

        // WebSocket推送
        pushDeliveryNotification(delivery.getOrderId(), rider.getName());

        log.info("手动分配骑手成功，deliveryId：{}，riderId：{}", deliveryId, riderId);
    }

    @Override
    @Transactional
    public void updateDeliveryStatus(Long deliveryId, Integer status) {
        Delivery delivery = deliveryMapper.getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("配送单不存在");
        }

        delivery.setStatus(status);
        LocalDateTime now = LocalDateTime.now();

        // 根据状态设置对应时间
        if (status == 2) {
            // 取餐中
            delivery.setPickupTime(now);
        } else if (status == 4) {
            // 已完成
            delivery.setFinishTime(now);
            // 更新订单状态为已完成
            Orders ordersUpdate = new Orders();
            ordersUpdate.setId(delivery.getOrderId());
            ordersUpdate.setStatus(Orders.COMPLETED);
            ordersUpdate.setDeliveryTime(now);
            orderMapper.update(ordersUpdate);

            // 骑手恢复为空闲状态
            if (delivery.getRiderId() != null) {
                Rider riderUpdate = Rider.builder()
                        .id(delivery.getRiderId())
                        .status(1) // 空闲
                        .updateTime(now)
                        .build();
                riderMapper.update(riderUpdate);
            }
        }
        delivery.setUpdateTime(now);
        deliveryMapper.update(delivery);

        log.info("更新配送单状态成功，deliveryId：{}，status：{}", deliveryId, status);
    }

    @Override
    public Delivery getDeliveryByOrderId(Long orderId) {
        return deliveryMapper.getByOrderId(orderId);
    }

    @Override
    public Delivery getById(Long id) {
        return deliveryMapper.getById(id);
    }

    @Override
    public PageResult pageQuery(DeliveryQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        List<Delivery> list = deliveryMapper.list(dto);
        Page<Delivery> page = (Page<Delivery>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 贪心策略：选取距离订单最近的空闲骑手
     * 若无法计算距离（无经纬度/地址），返回null，由调用方兜底
     */
    private Rider selectNearestRider(List<Rider> riders, Orders order) {
        if (order == null) {
            return null;
        }

        // 获取订单地址
        String orderAddress = order.getAddress();
        if (orderAddress == null || orderAddress.trim().isEmpty()) {
            return null;
        }

        // 获取订单地址经纬度
        JSONObject orderLocation = baiduMapUtil.getCoordinate(orderAddress);
        if (orderLocation == null) {
            log.warn("无法获取订单位置经纬度，orderId：{}，address：{}", order.getId(), orderAddress);
            return null;
        }

        Double orderLng = orderLocation.getDouble("lng");
        Double orderLat = orderLocation.getDouble("lat");

        Rider nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Rider rider : riders) {
            // 若骑手有位置信息，计算距离；否则跳过距离计算
            if (rider.getLongitude() == null || rider.getLatitude() == null) {
                // 无位置信息时，直接作为候选（兜底）
                if (nearest == null) {
                    nearest = rider;
                }
                continue;
            }

            double distance = Math.sqrt(
                    Math.pow((rider.getLongitude() - orderLng), 2)
                            + Math.pow((rider.getLatitude() - orderLat), 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearest = rider;
            }
        }

        return nearest;
    }

    /**
     * 通过WebSocket推送配送通知
     * type=3 配送通知
     */
    private void pushDeliveryNotification(Long orderId, String riderName) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", 3); // 3代表配送通知
            map.put("orderId", orderId);
            map.put("content", "骑手【" + riderName + "】已接单，正在为您配送");
            String json = JSON.toJSONString(map);
            webSocketServer.sendToAllClient(json);
        } catch (Exception e) {
            log.error("配送通知WebSocket推送失败，orderId：{}", orderId, e);
        }
    }
}
