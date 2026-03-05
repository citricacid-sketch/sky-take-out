package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * @author zhangpj
 * @date 2026/2/24
 */

@Mapper
public interface OrderMapper {

    /**
     * 向数据库中插入订单信息的方法
     *
     * @param orders 包含订单信息的对象，用于插入到数据库中
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 修改订单状态
     *
     * @param orderNumber
     * @param orderPaidStatus
     * @param orderStatus
     * @param checkOutTime
     */
    @Update("update orders set status = #{orderStatus}, pay_status = #{orderPaidStatus}, checkout_time = #{checkOutTime} where number = #{orderNumber}")
    void updateStatus(String orderNumber, Integer orderPaidStatus, Integer orderStatus, LocalDateTime checkOutTime);

    /**
     * 分页查询订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pagequery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单ID获取订单信息
     *
     * @param id 订单ID，Long类型
     * @return 返回对应的订单对象，Orders类型
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 获取订单统计信息的方法
     * 该方法用于返回订单相关的统计数据，通过返回一个OrderStatisticsVO对象来封装这些数据
     *
     * @return OrderStatisticsVO 包含订单统计信息的值对象，可能包含订单总数、订单金额、订单状态分布等信息
     */
    Integer countByStatus(Integer toBeConfirmed);
}
