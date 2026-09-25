package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.ActionDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 订单动作接口
 */
@Api(tags = "用户端-订单动作")
@Slf4j
@RestController("userOrderActionController")
@RequestMapping("/user/order")
public class OrderActionController {

    @Autowired
    private OrderService orderService;

    /**
     * 查询当前用户针对指定订单可执行的动作
     * 前端根据返回的 actions 动态渲染操作按钮
     *
     * @param id 订单 ID
     * @return 可执行动作列表
     */
    @GetMapping("/{id}/actions")
    @ApiOperation("查询订单可执行动作")
    public Result<List<ActionDetailVO>> getAllowedActions(@PathVariable Long id) {
        log.info("查询订单可执行动作：{}", id);
        List<ActionDetailVO> actions = orderService.getUserOrderActions(id);
        return Result.success(actions);
    }
}
