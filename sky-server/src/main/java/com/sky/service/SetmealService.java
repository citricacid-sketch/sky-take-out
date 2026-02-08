package com.sky.service;

import com.sky.dto.SetmealDTO;

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


}
