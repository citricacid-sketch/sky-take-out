package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangpj
 * @date 2026/2/24
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理地址簿为空。购物车为空的异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //查询购物车数据是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        //设置用户id
        orders.setUserId(userId);
        //设置下单时间
        orders.setOrderTime(LocalDateTime.now());
        // 设置订单号
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        //设置订单状态为待付款
        orders.setStatus(Orders.PENDING_PAYMENT);
        //设置支付状态为未支付
        orders.setPayStatus(Orders.UN_PAID);

        //设置收货人信息：从地址簿中获取收货人姓名、电话和地址
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName()) +
                (addressBook.getCityName() == null ? "" : addressBook.getCityName()) +
                (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName()) +
                (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        orderMapper.insert(orders);

        //向订单明细表插入一条或多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            // 订单明细
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            //设置订单id
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车
        shoppingCartMapper.delete(userId);

        //封装并返回订单提交结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        //// 当前登录用户id
        //Long userId = BaseContext.getCurrentId();
        //User user = userMapper.getByid(userId);
        //
        ////调用微信支付接口，生成预支付交易单
        //JSONObject jsonObject = weChatPayUtil.pay(
        //        ordersPaymentDTO.getOrderNumber(), //商户订单号
        //        new BigDecimal(0.01), //支付金额，单位 元
        //        "苍穹外卖订单", //商品描述
        //        user.getOpenid() //微信用户的openid
        //);
        //
        //if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //    throw new OrderBusinessException("该订单已支付");
        //}

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "SUCCESS");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        Integer OrderPaidStatus = Orders.PAID;
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;

        //发现没有支付时间
        LocalDateTime check_out_time = LocalDateTime.now();

        //获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        log.info("调用updateStatus方法,用于替换微信支付更新数据库的问题");
        orderMapper.updateStatus(orderNumber, OrderPaidStatus, OrderStatus, check_out_time);
         return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersPage = orderMapper.pagequery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if (ordersPage != null && ordersPage.getTotal() > 0) {
            for (Orders orders : ordersPage) {
                Long id = orders.getId();
                //查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
            return new PageResult(ordersPage.getTotal(), list);
    }

    @Override
    /**
     * 获取订单详情的方法
     * @param id 订单ID
     * @return OrderVO 订单详情视图对象
     */
    public OrderVO orderDetail(Long id) {
        // 根据订单ID查询订单基本信息
        Orders orders = orderMapper.getById(id);
        // 根据订单ID查询订单详情列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 创建订单视图对象
        OrderVO orderVO = new OrderVO();
        // 将订单基本信息复制到订单视图对象中
        BeanUtils.copyProperties(orders, orderVO);
        // 设置订单详情列表到订单视图对象中
        orderVO.setOrderDetailList(orderDetailList);
        // 返回订单详情视图对象
        return orderVO;
    }

    /**
     * 取消订单
     * @param id 要取消项的唯一标识符
     */
    @Override
    public void cancel(Long id) {
        // 根据订单ID查询订单
        Orders orders = orderMapper.getById(id);
        // 设置订单状态为取消
        orders.setStatus(Orders.CANCELLED);
        // 更新订单状态
        orderMapper.update(orders);
    }
}
