package com.sky.controller.user;

import com.sky.entity.Delivery;
import com.sky.entity.Rider;
import com.sky.result.Result;
import com.sky.service.DeliveryService;
import com.sky.service.RiderService;
import com.sky.vo.DeliveryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配送追踪（用户端）
 */
@RestController("userDeliveryController")
@RequestMapping("/user/delivery")
@Slf4j
@Api(tags = "用户端配送追踪")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private RiderService riderService;

    /**
     * 查询订单配送状态（含骑手信息、当前位置）
     */
    @GetMapping("/{orderId}")
    @ApiOperation("查询订单配送状态")
    public Result<DeliveryVO> getDeliveryByOrderId(@PathVariable Long orderId) {
        log.info("查询订单配送状态：orderId={}", orderId);
        Delivery delivery = deliveryService.getDeliveryByOrderId(orderId);
        if (delivery == null) {
            return Result.success(null);
        }

        DeliveryVO deliveryVO = new DeliveryVO();
        deliveryVO.setId(delivery.getId());
        deliveryVO.setOrderId(delivery.getOrderId());
        deliveryVO.setRiderId(delivery.getRiderId());
        deliveryVO.setStatus(delivery.getStatus());
        deliveryVO.setAssignTime(delivery.getAssignTime());
        deliveryVO.setPickupTime(delivery.getPickupTime());
        deliveryVO.setFinishTime(delivery.getFinishTime());
        deliveryVO.setRemark(delivery.getRemark());

        // 查询骑手信息
        if (delivery.getRiderId() != null) {
            Rider rider = riderService.getById(delivery.getRiderId());
            if (rider != null) {
                deliveryVO.setRiderName(rider.getName());
                deliveryVO.setRiderPhone(rider.getPhone());
                deliveryVO.setRiderLongitude(rider.getLongitude());
                deliveryVO.setRiderLatitude(rider.getLatitude());
            }
        }

        return Result.success(deliveryVO);
    }
}
