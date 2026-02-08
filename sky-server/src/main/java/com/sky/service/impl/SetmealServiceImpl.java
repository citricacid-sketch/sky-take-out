package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/8
 */
@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        //先添加套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //再添加套餐菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmeal.getId());
            });
            setmealDishMapper.insert(setmealDishes);
        }else {
            //没有菜品，抛出业务异常
            throw new RuntimeException("没有菜品");
        }
        log.info("新增套餐成功");



    }
}
