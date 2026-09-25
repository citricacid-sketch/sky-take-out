package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单可执行动作详情（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionDetailVO {

    /**
     * 动作编码（PAY/CANCEL/CONFIRM/REJECT/DELIVER/COMPLETE）
     */
    private String action;

    /**
     * 中文标签
     */
    private String label;

    /**
     * 执行时是否需要填写原因
     */
    private Boolean needReason;
}
