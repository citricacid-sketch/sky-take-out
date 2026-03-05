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
     *
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 更新购物车
     *
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void update(ShoppingCart cart);

    /**
     * 插入购物车数据
     *
     * @param shoppingCard
     */
    @Insert("insert into shopping_cart (name, image,user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) values" +
            " (#{name}, #{image}, #{userId},#{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCard);

    /**
     * 根据ID删除记录
     *
     * @param id 要删除记录的ID
     */
    void deleteById(Long id);

    /**
     * 删除指定用户的方法
     *
     * @param userId 要删除的用户ID
     */
    void delete(Long userId);

    /**
     * 批量插入购物车数据的方法
     *
     * @param shoppingCartList 购物车对象列表，包含需要批量插入的购物车信息
     *                         该方法用于一次性向数据库中插入多条购物车记录，提高数据插入效率
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
