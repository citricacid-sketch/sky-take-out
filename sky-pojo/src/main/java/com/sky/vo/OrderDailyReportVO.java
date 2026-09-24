package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日订单统计（用于报表导出，一次 GROUP BY 查询返回多日数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDailyReportVO {
    /**
     * 日期
     */
    private LocalDate date;
    /**
     * 当日总订单数
     */
    private Integer orderCount;
    /**
     * 当日已完成订单数（有效订单）
     */
    private Integer validOrderCount;
    /**
     * 当日营业额（已完成订单金额）
     */
    private Double turnover;
}
