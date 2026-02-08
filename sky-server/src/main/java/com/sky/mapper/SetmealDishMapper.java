package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 
 * @author zhangpj
 * @date 2026/2/8
 *
 */
@Mapper
public interface SetmealDishMapper {

    /**
     * 批量插入套餐菜品数据
     * @param setmealDishes
     */
    void insert(List<SetmealDish> setmealDishes);
}
