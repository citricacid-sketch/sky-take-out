package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangpj
 * @date 2026/3/4
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "订单管理")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("取消订单");
        orderService.cancelOrder(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 各个状态订单数量统计
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        log.info("各个状态订单数量统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);

    }


    /**
     * 完成订单接口
     *
     * @param id 订单ID，通过路径变量传递
     * @return 返回操作结果
     * @PutMapping 映射HTTP PUT请求到特定处理方法
     * @ApiOperation 接口描述，用于API文档生成
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result completeOrder(@PathVariable Long id) {
        // 记录日志，表示正在执行完成订单操作
        log.info("完成订单");
        // 调用服务层方法完成订单
        orderService.completeOrder(id);
        // 返回成功结果
        return Result.success();
    }

    /**
     * 拒绝订单的接口方法
     * 使用HTTP PUT方法映射到"/rejection"路径
     *
     * @param ordersRejectionDTO 订单拒绝的数据传输对象，包含拒绝订单所需的信息
     * @return 返回操作结果，成功时返回成功状态
     */
    @PutMapping("/rejection")
    @ApiOperation("拒绝订单")
    public Result rejectionOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        // 记录拒绝订单的操作日志
        log.info("拒绝订单");
        // 调用订单服务层的拒绝订单方法处理业务逻辑
        orderService.rejectionOrder(ordersRejectionDTO);
        // 返回操作成功的结果
        return Result.success();
    }

    /**
     * 接单接口
     *
     * @param ordersConfirmDTO 包含接单所需信息的DTO对象
     * @return 返回操作结果
     * @PutMapping 映射HTTP PUT请求到特定处理方法
     * @ApiOperation 接口描述，用于API文档生成
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单");
        orderService.confirmOrder(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 查询订单
     */
    @GetMapping("details/{id}")
    @ApiOperation("查询订单")
    public Result<OrderVO> queryOrder(@PathVariable Long id) {
        log.info("查询订单");
        OrderVO orderVO = orderService.orderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 配送订单接口
     * 使用HTTP PUT方法，通过订单ID更新订单状态为配送中
     *
     * @param id 订单ID，通过路径变量传递
     * @return 返回操作结果，成功时返回success
     */
    @PutMapping("/delivery/{id}")    // 定义HTTP PUT请求映射，路径为/delivery/{id}
    @ApiOperation("配送订单")        // 接口文档说明，表示该接口用于配送订单
    public Result deliveryOrder(@PathVariable Long id) {  // 方法参数，从路径中获取订单ID
        log.info("配送订单");          // 记录日志信息，表示开始处理配送订单
        orderService.deliveryOrder(id); // 调用业务层方法执行配送订单操作
        return Result.success();      // 返回操作成功结果
    }

    /**
     * 订单搜索
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch( OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单搜索");
        PageResult orderVOPageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(orderVOPageResult);
    }

}