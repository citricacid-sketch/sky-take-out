package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.OrderDetail;
import com.sky.result.PageResult;
import com.sky.vo.ActionDetailVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/24
 */
@Service
public interface OrderService {
    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 根据订单ID获取订单详情的方法
     *
     * @param id 订单ID，用于唯一标识一个订单
     * @return OrderVO 订单详情视图对象，包含订单的完整信息
     */
    OrderVO orderDetail(Long id);

    /**
     * 取消操作的方法
     *
     * @param id 要取消项的唯一标识符
     */
    void cancel(Long id);

    /**
     * 处理重复项的方法
     *
     * @param id Long类型的参数，用于标识需要处理的重复项
     */
    void repetition(Long id);

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    void cancelOrder(OrdersCancelDTO ordersCancelDTO);

    /**
     * 获取订单统计信息的方法
     * 该方法用于查询并返回订单相关的统计数据
     *
     * @return OrderStatisticsVO 包含订单统计信息的值对象，可能包含订单总数、订单金额、订单状态分布等数据
     */
    OrderStatisticsVO statistics();

    /**
     * 完成订单的方法
     *
     * @param id 订单ID，用于标识需要完成的订单
     */
    void completeOrder(Long id);


    /**
     * 拒绝订单的方法
     *
     * @param ordersRejectionDTO 包含拒绝订单所需信息的DTO对象
     */
    void rejectionOrder(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 接收订单的方法
     *
     * @param
     */
    void confirmOrder(OrdersConfirmDTO ordersConfirmDTO);


    /**
     * 配送订单方法
     *
     * @param id 订单ID，用于标识需要配送的订单
     */
    void deliveryOrder(Long id);


    /**
     * 根据条件进行分页查询订单信息
     *
     * @param ordersPageQueryDTO 订单分页查询条件数据传输对象，包含查询参数和分页信息
     * @return PageResult 分页查询结果，包含订单数据列表和分页信息
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 提醒功能方法
     *
     * @param id 用户或事件的唯一标识符，使用Long类型以支持大数值
     */
    void reminder(Long id);

    /**
     * 查询用户端针对指定订单可执行的动作列表
     *
     * @param id 订单 ID
     * @return 可执行动作列表
     */
    List<ActionDetailVO> getUserOrderActions(Long id);

    /**
     * 查询管理端针对指定订单可执行的动作列表
     *
     * @param id 订单 ID
     * @return 可执行动作列表
     */
    List<ActionDetailVO> getAdminOrderActions(Long id);
}
