package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 骑手
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rider implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 姓名
    private String name;

    // 手机号
    private String phone;

    // 状态 0离线 1空闲 2配送中
    private Integer status;

    // 当前经度
    private Double longitude;

    // 当前纬度
    private Double latitude;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
