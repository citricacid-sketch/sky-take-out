package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增/修改骑手用 DTO
 */
@Data
public class RiderDTO implements Serializable {

    private Long id;

    private String name;

    private String phone;

    // 状态 0离线 1空闲 2配送中
    private Integer status;
}
