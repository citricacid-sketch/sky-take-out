package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/23
 */
@Mapper
public interface ShoppingCartMapper {
    /**
     * 根据用户id查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list (ShoppingCart shoppingCart);

    /**
     * 更新购物车
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void update(ShoppingCart cart);

    /**
     * 插入购物车数据
     * @param shoppingCard
     */
    @Insert("insert into shopping_cart (name, image,user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) values" +
            " (#{name}, #{image}, #{userId},#{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCard);

}
