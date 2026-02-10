package com.sky.controller.user;

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
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺管理")
@Slf4j
public class ShopControlller {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取店铺营业状态
     *
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getShopStatus() {
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get( KEY);
        log.info("获取营业状态成功，结果：{}", shopStatus == 1 ? "打烊中" : "营业中");
        return Result.success(shopStatus);
    }
}
