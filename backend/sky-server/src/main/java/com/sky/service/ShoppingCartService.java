package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/23
 */
@Service
public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看购物车
     * @return
     *
     */
    List<ShoppingCart> showShoppingCart();


    void clean();

    void sub(ShoppingCartDTO shoppingCartDTO);
}