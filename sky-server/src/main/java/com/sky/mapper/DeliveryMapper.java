package com.sky.mapper;

import com.sky.dto.DeliveryQueryDTO;
import com.sky.entity.Delivery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 配送单 Mapper
 */
@Mapper
public interface DeliveryMapper {

    /**
     * 新增配送单
     */
    void insert(Delivery delivery);

    /**
     * 修改配送单
     */
    void update(Delivery delivery);

    /**
     * 根据id查询配送单
     */
    Delivery getById(Long id);

    /**
     * 根据订单id查询配送单
     */
    Delivery getByOrderId(Long orderId);

    /**
     * 多条件分页查询配送单
     */
    List<Delivery> list(DeliveryQueryDTO dto);
}
