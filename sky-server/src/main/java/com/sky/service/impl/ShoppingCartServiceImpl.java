package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.DishService;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/23
 */
@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        //判断购物车中的是否有菜品
        ShoppingCart shoppingCard = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCard);
        Long userId = BaseContext.getCurrentId();
        log.info("用户id：{}", userId);
        shoppingCard.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCard);

        //如果存在，则数量加1

        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.update(cart);
        } else {
            shoppingCard.setNumber(1);
            shoppingCard.setCreateTime(LocalDateTime.now());

            Long dishid = shoppingCartDTO.getDishId();
            if (dishid != null) {
                Dish dish = dishMapper.getById(dishid);
                shoppingCard.setName(dish.getName());
                shoppingCard.setImage(dish.getImage());
                shoppingCard.setAmount(dish.getPrice());
                shoppingCard.setDishFlavor(shoppingCartDTO.getDishFlavor());
            } else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCard.setName(setmeal.getName());
                shoppingCard.setImage(setmeal.getImage());
                shoppingCard.setAmount(setmeal.getPrice());
            }

            shoppingCartMapper.insert(shoppingCard);
        }
    }

    /**
     * 查看购物车
     *
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        log.info("查看购物车");
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        log.info("清空购物车");
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.delete(userId);

    }

    /**
     * 删除购物车
     *
     * @param shoppingCartDTO
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        log.info("减少购物车：{}", shoppingCartDTO);
        Long userId = BaseContext.getCurrentId();
        log.info("用户id：{}", userId);
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            Integer number = cart.getNumber();
            if (number > 1) {
                cart.setNumber(number - 1);
                shoppingCartMapper.update(cart);
            } else {
                shoppingCartMapper.deleteById(cart.getId());
            }
        }
    }


}
