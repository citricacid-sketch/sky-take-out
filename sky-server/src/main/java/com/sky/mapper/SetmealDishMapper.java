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

    /**
     * 根据套餐删除套餐菜品关系
     * @param ids
     * @return
     */
    void deleteBySetmealId(List<Long> ids);

    /**
     * 根据套餐id查询套餐菜品关系
     * @param id
     * @return
     */
    List<SetmealDish> list(Long id);


}
