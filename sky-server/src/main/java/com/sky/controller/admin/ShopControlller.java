package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author zhangpj
 * @date 2026/2/10
 */
@RestController(" adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺管理")
@Slf4j
public class ShopControlller {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     *
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setShopStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态:{}", status == 1 ? "营业中" : "打样中");
        redisTemplate.opsForValue().set(KEY, status);
        log.info("设置成功");
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     *
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getShopStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取营业状态成功，结果：{}", shopStatus == 1 ? "营业中" : "打样中");
        return Result.success(shopStatus);
    }
}
