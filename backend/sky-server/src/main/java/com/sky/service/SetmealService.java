package com.sky.service;

import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    PageResult pageQuery(Integer page, Integer pageSize, String name);

    /**
     * 批量删除
     * @param ids
     */
    @Transactional
    void delete(List<Long> ids);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    @AutoFill(value = OperationType.UPDATE)
    void update(SetmealDTO setmealDTO);

    /**
     * 批量起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
