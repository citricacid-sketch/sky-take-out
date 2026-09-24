package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.Dish;
import com.sky.mapper.DishFlavorMapper;
import com.sky.result.Result;
import com.sky.service.RecommendService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户端 - 推荐接口（猜你喜欢）
 *
 * @author zhangpj
 * @date 2026/9/24
 */
@RestController
@RequestMapping("/user/recommend")
@Slf4j
@Api(tags = "推荐接口")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private com.sky.mapper.DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 获取用户推荐菜品列表（猜你喜欢）
     *
     * @return 起售中的推荐菜品列表（含口味）
     */
    @GetMapping
    @ApiOperation("猜你喜欢推荐菜品")
    public Result<List<DishVO>> recommend() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取用户推荐菜品，userId={}", userId);

        // 1. 获取推荐菜品 ID 列表
        List<Long> dishIds = recommendService.recommendDishIds(userId);
        if (dishIds == null || dishIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 2. 查询菜品详情，只保留起售中的，并组装口味
        List<DishVO> result = new ArrayList<>();
        for (Long dishId : dishIds) {
            Dish dish = dishMapper.getById(dishId);
            if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
                // 跳过停售/下架菜品
                continue;
            }
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            // 查询口味
            dishVO.setFlavors(dishFlavorMapper.getByDishId(dishId));
            result.add(dishVO);
        }

        return Result.success(result);
    }
}
