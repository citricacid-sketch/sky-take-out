package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * @author zhangpj
 * @date 2026/3/11
 * 统计相关接口
 */
@Slf4j
@RestController("adminReportController")  // 使用@Slf4j注解进行日志记录，将此控制器注册为名为"adminReportController"的Bean
@RequestMapping("admin/report")          // 设置此控制器的根路径为"admin/report"
@Api(tags = "统计相关接口")               // Swagger API文档注解，标记此控制器为"统计相关接口"
public class ReportControlller { // 报告控制器类，用于处理报告相关的业务逻辑

    @Autowired                          // 自动注入ReportService实例
    private ReportService reportService;


    /**
     * 营业额统计接口
     *
     * @param begin 开始日期，格式为YYYY-MM-dd
     * @param end   结束日期，格式为YYYY-MM-dd
     * @return 返回包含营业额统计数据的Result对象，数据类型为TurnoverReportVO
     */
    @GetMapping("/turnoverStatistics")    // HTTP GET请求映射，路径为/turnoverStatistics
    @ApiOperation("营业额统计")           // Swagger API注解，说明接口功能为"营业额统计"
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        TurnoverReportVO turnoverReportVO = reportService.getTurnoverReport(begin, end);  // 调用服务层方法获取营业额统计数据
        return Result.success(turnoverReportVO);  // 返回成功结果，包含营业额统计数据
    }


    /**
     * 获取用户统计数据的接口方法
     *
     * @param begin 统计开始日期，格式为YYYY-MM-dd
     * @param end   统计结束日期，格式为YYYY-MM-dd
     * @return 返回包含用户统计数据的Result对象，数据类型为UserReportVO
     */
    @GetMapping("/userStatistics")    // HTTP GET请求映射，路径为/userStatistics
    @ApiOperation("用户统计")        // Swagger API注解，说明接口功能为"用户统计"
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        UserReportVO userReportVO = reportService.getUserReport(begin, end);  // 调用服务层方法获取用户统计数据
        return Result.success(userReportVO);  // 返回成功结果，包含用户统计数据
    }

    /**
     * 获取订单统计数据的接口方法
     *
     * @param begin 统计开始日期，格式为YYYY-MM-dd
     * @param end   统计结束日期，格式为YYYY-MM-dd
     * @return 返回包含订单统计数据的Result对象，数据类型为UserReportVO
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        // 调用服务层方法获取订单统计数据
        OrderReportVO orderReportVO = reportService.getOrderReport(begin, end);
        // 返回成功结果，包含订单统计数据
        return Result.success(orderReportVO);
    }

    /**
     * 获取用户统计数据的接口方法
     *
     * @param begin 统计开始日期，格式为YYYY-MM-dd
     * @param end   统计结束日期，格式为YYYY-MM-dd
     * @return 返回包含用户统计数据的Result对象，数据类型为UserReportVO
     */
    @GetMapping("/top10")
    @ApiOperation("销量排名")
    public Result<SalesTop10ReportVO>top10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        SalesTop10ReportVO salesTop10ReportVO = reportService.getSalesTop10Report(begin, end);
        // 返回成功结果，包含用户统计数据
        return Result.success(salesTop10ReportVO);
    }

    @GetMapping("/export")
    @ApiOperation("导出报表")
    public void export(HttpServletResponse response){
        reportService.export(response);
    }
}
