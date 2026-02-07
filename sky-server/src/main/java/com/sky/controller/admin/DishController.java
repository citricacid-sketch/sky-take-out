package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@Api(tags = "菜品相关接口")
@Slf4j
@RestController
@RequestMapping ("/admin/dish")
public class DishController {

    @Autowired
   private DishService dishService;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody  DishDTO dishDTO) {
        log.info("新增菜品");
        dishService.saveWithFlavor(dishDTO);
        return null;
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping ("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page( DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询:{}", dishPageQueryDTO);
       PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
       return Result.success(pageResult);
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除");
        dishService.delete(ids);
        return Result.success();
    }
}
