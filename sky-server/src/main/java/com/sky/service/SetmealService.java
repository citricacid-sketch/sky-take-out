package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/8
 */
public interface SetmealService {
    /**
     * 新增套餐
     * @param setmealDTO
     */
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
    void delete(List<Long> ids);


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);


}
