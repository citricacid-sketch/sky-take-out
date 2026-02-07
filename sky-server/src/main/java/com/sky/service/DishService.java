package com.sky.service;

import com.sky.dto.DishDTO;

/**
 * 
 * @author zhangpj
 * @date 2026/2/7
 */public interface DishService {
    /**
     * 新增菜品
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);
}
