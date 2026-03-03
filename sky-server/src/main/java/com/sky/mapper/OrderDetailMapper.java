package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/24
 */
@Mapper
public interface OrderDetailMapper {

/**
 * 批量插入订单详情列表的方法
 *
 * @param orderDetailList 订单详情列表，包含需要批量插入的所有订单详情信息
 * 该方法用于一次性插入多条订单详情记录，提高数据插入效率
 */
    void insertBatch(List<OrderDetail> orderDetailList);


    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
