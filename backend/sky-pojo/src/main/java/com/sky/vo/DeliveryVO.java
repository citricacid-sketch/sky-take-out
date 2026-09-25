package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送追踪 VO（用户端返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 配送单id
    private Long id;

    // 关联订单id
    private Long orderId;

    // 骑手id
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

    // 骑手姓名
    private String riderName;

    // 骑手手机号
    private String riderPhone;

    // 骑手当前经度
    private Double riderLongitude;

    // 骑手当前纬度
    private Double riderLatitude;
}
