package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @author zhangpj
 * @date 2026/2/24
 */

@Mapper
public interface OrderMapper {


    /**
     * 向数据库中插入订单信息的方法
     *
     * @param orders 包含订单信息的对象，用于插入到数据库中
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Orders orders);
}
