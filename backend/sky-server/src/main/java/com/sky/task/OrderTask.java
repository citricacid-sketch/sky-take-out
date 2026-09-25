package com.sky.task;

import com.sky.entity.OrderAction;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.impl.OrderServiceImpl;
import com.sky.statemachine.OrderStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zhangpj
 * @date 2026/3/9
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付的订单：待付款(1) -> 已取消(6)
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeOutOrder(){
        log.info("定时任务执行 {}" ,LocalDateTime.now());

        // 查询15分钟内未支付的订单
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> orderList = orderMapper.getByStatusAndOrderTimeLt(Orders.PENDING_PAYMENT, localDateTime);

        if (orderList != null && !orderList.isEmpty()) {
            for (Orders orders : orderList)
            {
                // 状态机校验 + 获取目标状态（超时取消）
                Integer targetStatus = OrderStateMachine.execute(orders.getStatus(), OrderAction.SET_TIMEOUT);
                orders.setStatus(targetStatus);
                orders.setCancelReason("超时未支付");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单：派送中(4) -> 已完成(5)
     */
    @Scheduled(cron = "0 0 1 * * ?  ")
    public void processDeliveryOrder() {
        log.info("定时任务执行 {}", LocalDateTime.now());
        List<Orders> orderList = orderMapper.getByStatusAndOrderTimeLt(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusHours(-1));

        if (orderList != null && !orderList.isEmpty()) {
            for (Orders orders : orderList) {
                // 状态机校验 + 获取目标状态（超时完成）
                Integer targetStatus = OrderStateMachine.execute(orders.getStatus(), OrderAction.SET_TIMEOUT);
                orders.setStatus(targetStatus);
                orders.setCheckoutTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
