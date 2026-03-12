package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhangpj
 * @date 2026/3/11
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 重写父类或接口中的方法，用于获取营业额报告数据
     * 该方法返回一个营业额报告的值对象(VO)，用于展示营业额相关信息
     *
     * @return TurnoverReportVO 包含营业额报告信息的值对象，目前实现返回null
     */
    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {

        //用户存放从begin到end的每天的日期列表,用逗号隔开
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.equals(end)) {
            // 将日期递增一天，继续循环
            begin = begin.plusDays(1);
            // 将当前日期添加到日期列表中
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        //用户存放从begin到end的每天的营业额列表
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endDateTime);
            map.put("status", 5);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        //封装
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }


    /**
     * 获取用户报告的方法
     *
     * @param begin 开始日期，包含在报告范围内
     * @param end   结束日期，包含在报告范围内
     * @return UserReportVO 用户报告对象，包含日期列表等信息
     */
    @Override
    public UserReportVO getUserReport(LocalDate begin, LocalDate end) {
        // 创建一个LocalDate列表，用于存储日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        while (!begin.equals(end)) {
            // 将日期递增一天，继续循环
            begin = begin.plusDays(1);
            // 将当前日期添加到日期列表中
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("end", endDateTime);
            //总用户数量
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            map.put("begin", beginTime);
            //新增用户数量
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    /**
     * 获取指定日期范围内的订单报告
     * @param begin 开始日期，包含在内
     * @param end 结束日期，包含在内
     * @return OrderReportVO 包含日期列表、订单数量列表、有效订单数量列表、总订单数、有效订单数和订单完成率的报告对象
     */
    public OrderReportVO getOrderReport(LocalDate begin, LocalDate end) {
        // 创建一个LocalDate列表，用于存储日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        // 循环遍历从开始日期到结束日期之间的所有日期
        while (!begin.equals(end)) {
            // 将日期递增一天，继续循环
            begin = begin.plusDays(1);
            // 将当前日期添加到日期列表中
            dateList.add(begin);
        }


        // 初始化订单数量列表和有效订单数量列表
        List<Integer> orderList = new ArrayList<>();
        List<Integer> orderAmountList = new ArrayList<>();
        // 遍历日期列表，获取每天的订单数量和有效订单数量
        for (LocalDate date : dateList) {
            // 获取当天的开始时间（0点0分0秒）
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            // 获取当天的结束时间（23点59分59秒）
            LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);
            // 获取当天所有订单的数量
            Integer ordersCount = getOrderCount(beginTime, endDateTime, null);
            // 获取当天已完成订单的数量
            Integer validOrdersCount = getOrderCount(beginTime, endDateTime, Orders.COMPLETED);
            // 将订单数量添加到订单列表中
            orderList.add(ordersCount);
            // 将有效订单数量添加到有效订单列表中
            orderAmountList.add(validOrdersCount);

        }
        //订单总数
        Integer totalorderCount = orderList.stream().reduce(Integer::sum).get();
        //有效订单数
        Integer validorderCount = orderAmountList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderRate = 0.0;
        if (totalorderCount != 0) {
            orderRate = (double) validorderCount / totalorderCount * 100;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderList, ","))
                .validOrderCountList(StringUtils.join(orderAmountList, ","))
                .totalOrderCount(totalorderCount)
                .validOrderCount(validorderCount)
                .orderCompletionRate(orderRate)
                .build();
    }

    @Override
    /**
     * 获取指定时间范围内的销售排行榜前10的商品数据
     * @param begin 开始日期，包含该日期
     * @param end 结束日期，包含该日期
     * @return SalesTop10ReportVO 包含商品名称列表和销售数量列表的对象
     */
    public SalesTop10ReportVO getSalesTop10Report(LocalDate begin, LocalDate end) {

        // 将开始日期转换为当天的最小时间（0点0分0秒）
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        // 将结束日期转换为当天的最大时间（23点59分59秒）
        LocalDateTime endDateTime = LocalDateTime.of(end, LocalTime.MAX);
        // 查询指定时间范围内的商品销售数据，获取销售前10的商品
        List<GoodsSalesDTO> salesTop10 = orderMapper.getGoodsSales(beginTime, endDateTime);

        // 使用Stream API提取商品名称列表
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        // 使用Stream API提取商品销售数量列表
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");


        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();

        //List<String> goodsNameList = new ArrayList<>();
        //List<Integer> goodsSalesList = new ArrayList<>();
        //for (GoodsSalesDTO goodsSalesDTO : salesTop10) {
        //    goodsNameList.add(goodsSalesDTO.getName());
        //    goodsSalesList.add(goodsSalesDTO.getNumber());
        //}
        //return SalesTop10ReportVO.builder()
        //        .nameList(StringUtils.join(goodsNameList, ","))
        //        .numberList(StringUtils.join(goodsSalesList, ","))
        //        .build();
    }

    /**
     * 根据时间范围和订单状态获取订单数量
     *
     * @param beginTime   开始时间
     * @param endDateTime 结束时间
     * @param status      订单状态
     * @return 符合条件的订单数量
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endDateTime, Integer status) {
        // 创建一个HashMap用于存储查询参数
        Map map = new HashMap<>();
        // 将开始时间参数存入map
        map.put("begin", beginTime);
        // 将结束时间参数存入map
        map.put("end", endDateTime);
        // 如果订单状态不为空，则将订单状态参数存入map
        if (status != null) {
            map.put("status", status);
        }
        // 调用orderMapper的countByMap方法查询符合条件的订单数量
        Integer orderCount = orderMapper.countByMap(map);
        // 返回查询结果
        return orderCount;
    }
}
