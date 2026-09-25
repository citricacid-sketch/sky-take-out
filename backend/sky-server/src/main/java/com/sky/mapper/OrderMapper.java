package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderDailyReportVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 根据订单状态和订单时间获取订单数量
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLt(Integer status, LocalDateTime orderTime);

    /**
     * 根据订单状态和订单时间获取订单数量
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据时间范围和订单状态获取订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计指定时间区间内的销量排名前十
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getGoodsSales(LocalDateTime begin, LocalDateTime end);

    /**
     * 按日分组统计订单数据（日期、总订单数、有效订单数、营业额），用于报表导出
     * @param begin
     * @param end
     * @return
     */
    List<OrderDailyReportVO> getDailyOrderStats(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询用户历史订单中点过的菜品ID，按出现次数降序取TopN（用于推荐）
     * @param userId 用户ID
     * @param limit 返回数量上限
     * @return 菜品ID列表
     */
    List<Long> getFrequentlyOrderedDishIds(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 通用只读查询方法（用于 AI 数据分析助手）
     * 注意：调用方需确保 SQL 已做安全校验（仅 SELECT、仅白名单表）
     * @param sql SQL 查询语句
     * @return 查询结果列表，每行是一个 Map（列名 -> 值）
     */
    List<Map<String, Object>> executeQuery(@Param("sql") String sql);

    /**
     * 查询用户最近若干条订单（按下单时间倒序）
     * @param userId 用户 ID
     * @param limit 返回条数
     * @return 订单列表
     */
    List<Orders> getRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
