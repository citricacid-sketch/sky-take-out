package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author zhangpj
 * @date 2026/3/11
 */
@Service
public interface ReportService {

    /**
     * 营业额统计一定时间内的数据
     *
     * @return
     */
    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

    /**
     * 用户统计一定时间内的数据
     */
    UserReportVO getUserReport(LocalDate begin, LocalDate end);

    /**
     * 根据指定的开始日期和结束日期获取订单报告
     *
     * @param begin 报告开始的日期，包含在查询范围内
     * @param end   报告结束的日期，包含在查询范围内
     * @return OrderReportVO 包含指定时间段内订单相关数据的报告对象
     */
    OrderReportVO getOrderReport(LocalDate begin, LocalDate end);

    /**
     * 获取指定时间段内的销售排行榜前十数据
     *
     * @param begin 开始日期，包含该日期
     * @param end   结束日期，包含该日期
     * @return SalesTop10ReportVO 包含销售排行榜前十数据的视图对象
     */
    SalesTop10ReportVO getSalesTop10Report(LocalDate begin, LocalDate end);
}
