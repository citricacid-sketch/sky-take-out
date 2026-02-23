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
        }else {
          Long dishid = shoppingCartDTO.getDishId();
          if (dishid != null) {
              Dish dish = dishMapper.getById(dishid);
              shoppingCard.setName(dish.getName());
              shoppingCard.setImage(dish.getImage());
              shoppingCard.setAmount(dish.getPrice());
          } else {
              Long setmealId = shoppingCartDTO.getSetmealId();
              Setmeal setmeal = setmealMapper.getById(setmealId);
              shoppingCard.setName(setmeal.getName());
              shoppingCard.setImage(setmeal.getImage());
              shoppingCard.setAmount(setmeal.getPrice());
          }
            shoppingCard.setNumber(1);
            shoppingCard.setCreateTime(LocalDateTime.now());
        }

        shoppingCartMapper.insert(shoppingCard);
        //如果不存在，则添加到购物车
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        log.info("查看购物车");
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
                return list ;
    }
}
