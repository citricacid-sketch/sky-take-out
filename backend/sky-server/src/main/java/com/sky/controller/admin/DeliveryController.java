package com.sky.controller.admin;

import com.sky.dto.DeliveryQueryDTO;
import com.sky.entity.Delivery;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DeliveryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 配送单管理（管理端）
 */
@RestController
@RequestMapping("/admin/delivery")
@Slf4j
@Api(tags = "配送单管理")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    /**
     * 分页查询配送单列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询配送单列表")
    public Result<PageResult> page(DeliveryQueryDTO deliveryQueryDTO) {
        log.info("分页查询配送单列表：{}", deliveryQueryDTO);
        PageResult pageResult = deliveryService.pageQuery(deliveryQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询配送单详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询配送单详情")
    public Result<Delivery> getById(@PathVariable Long id) {
        log.info("根据id查询配送单详情：{}", id);
        Delivery delivery = deliveryService.getById(id);
        return Result.success(delivery);
    }

    /**
     * 手动分配骑手
     */
    @PostMapping("/{id}/assign")
    @ApiOperation("手动分配骑手")
    public Result assignRider(@PathVariable("id") Long deliveryId, @RequestParam Long riderId) {
        log.info("手动分配骑手：deliveryId={}, riderId={}", deliveryId, riderId);
        deliveryService.manualAssignRider(deliveryId, riderId);
        return Result.success();
    }

    /**
     * 更新配送单状态
     */
    @PostMapping("/{id}/status/{status}")
    @ApiOperation("更新配送单状态")
    public Result updateStatus(@PathVariable("id") Long deliveryId, @PathVariable Integer status) {
        log.info("更新配送单状态：deliveryId={}, status={}", deliveryId, status);
        deliveryService.updateDeliveryStatus(deliveryId, status);
        return Result.success();
    }
}
