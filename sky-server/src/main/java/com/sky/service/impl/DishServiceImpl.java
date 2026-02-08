package com.sky.service.impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/7
 */
@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品和菜品口味
     *
     * @param dishDTO
     */
    @Override
    // 开启事务, 保证数据一致性
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("新增菜品");

        //插入一条菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        //插入N条菜品口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        List<DishVO> records = page.getResult();
        long total = page.getTotal();
        PageResult pageResult = new PageResult(total, records);
        return pageResult;
    }


    /**
     * 批量删除菜品
     *
     * @param ids
     */

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //当前菜品是否可以删除——菜品是否在售
        if (ids != null && ids.size() > 0) {
            for (Long id : ids) {
                Dish dish = dishMapper.getById(id);
                if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                    //当前菜品处于启售状态，不能删除
                    throw new RuntimeException(MessageConstant.DISH_ON_SALE);
                }
            }
        }
        //当前菜品是否被套餐关联
        if (ids != null && ids.size() > 0) {
            for (Long id : ids) {
                Long count = setmealMapper.countByDishId(id);
                if (count > 0) {
                    //当前菜品被套餐关联，不能删除
                    throw new RuntimeException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
                }
            }
        }
        //删除菜品——删除菜品口味——删除菜品图片
        dishMapper.delete(ids);
        dishFlavorMapper.deleteByDishId(ids);


    }

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //查询菜品数据
        Dish dish = dishMapper.getById(id);
        //查询菜品口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        //删除菜品口味数据
        dishFlavorMapper.deleteByDishId(Arrays.asList(dishDTO.getId()));
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            //给菜品口味对象 dishFlavor 设置菜品id
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //插入菜品口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 批量起售停售
     *
     * @param status
     * @param id
     */
    @Override
    public void setStatus(Integer status, Long id) {
        dishMapper.setStatus(status, id);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        log.info("根据分类id查询菜品");
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }


}
