package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 关联订单
    private Long orderId;

    // 骑手
    private Long riderId;

    // 配送状态 0待分配 1已分配 2取餐中 3配送中 4已完成 5异常
    private Integer status;

    // 分配时间
    private LocalDateTime assignTime;

    // 取餐时间
    private LocalDateTime pickupTime;

    // 完成时间
    private LocalDateTime finishTime;

    // 备注
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
