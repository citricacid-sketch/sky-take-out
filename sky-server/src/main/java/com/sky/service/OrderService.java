package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.stereotype.Service;

/**
 * 
 * @author zhangpj
 * @date 2026/2/24
 */
@Service
public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
   OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
}
