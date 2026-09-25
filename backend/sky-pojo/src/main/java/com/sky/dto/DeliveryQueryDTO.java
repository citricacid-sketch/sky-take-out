package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配送单分页查询 DTO
 */
@Data
public class DeliveryQueryDTO implements Serializable {

    private Long orderId;

    private Long riderId;

    // 配送状态 0待分配 1已分配 2取餐中 3配送中 4已完成 5异常
    private Integer status;

    private int page;

    private int pageSize;
}
