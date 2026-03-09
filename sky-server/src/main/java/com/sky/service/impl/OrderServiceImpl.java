package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtilFinal;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private BaiduMapUtilFinal baiduMapUtil;

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

        // 校验配送范围
        String userAddress = (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName()) +
                (addressBook.getCityName() == null ? "" : addressBook.getCityName()) +
                (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName()) +
                (addressBook.getDetail() == null ? "" : addressBook.getDetail());
        
        log.info("用户收货地址：{}", userAddress);
        
        // 验证地址是否为空
        if (userAddress == null || userAddress.trim().isEmpty()) {
            throw new OrderBusinessException("收货地址不能为空");
        }

        Integer distance = baiduMapUtil.calculateDistance(userAddress);
        if (distance == null) {
            throw new OrderBusinessException("无法计算配送距离，请检查地址是否正确");
        }
        
        log.info("配送距离：{} 米", distance);
        
        // 配送范围为5公里内
        if (distance > 5000) {
            throw new OrderBusinessException(MessageConstant.OUT_OF_DELIVERY_RANGE);
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
     *
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
     *
     * @param id 要取消项的唯一标识符
     */
    @Override
    public void cancel(Long id) {
        // 根据订单ID查询订单
        Orders ordersDB = orderMapper.getById(id);
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            ////调用微信支付退款接口
            //try {
            //    weChatPayUtil.refund(
            //            ordersDB.getNumber(), //商户订单号
            //            ordersDB.getNumber(), //商户退款单号
            //            new BigDecimal(0.01),//退款金额，单位 元
            //            new BigDecimal(0.01));//原订单金额
            //} catch (Exception e) {
            //    throw new RuntimeException(e);
            //}

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id Long类型的参数，用于标识需要处理的重复项
     */
    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        // 根据订单ID查询订单
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //将原来订单对象的菜品信息复制到购物车对象
            BeanUtils.copyProperties(item, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            return shoppingCart;
        }).collect(Collectors.toList());
        // 将购物车对象保存到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 取消订单方法
     *
     * @param ordersCancelDTO 包含订单取消信息的DTO对象
     */
    @Override
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        // 根据订单ID查询订单信息
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 获取订单支付状态
        Integer payStatus = ordersDB.getPayStatus();

        // 判断订单是否已支付
        if (payStatus.equals(Orders.PAID)) {
            //用户已支付，需要退款
            //try {
            //    String refund = weChatPayUtil.refund(
            //            ordersDB.getNumber(),
            //            ordersDB.getNumber(),
            //            new BigDecimal(0.01),
            //            new BigDecimal(0.01));
            //    log.info("申请退款：{}", refund);
            //} catch (Exception e) {
            //    throw new RuntimeException(e);
            //}
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    /**
     * 订单统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    @Override
    public void completeOrder(Long id) {

        Orders ordersDB = orderMapper.getById(id);

        // 获取订单的当前状态
        Integer status = ordersDB.getStatus();
        // 检查订单是否存在，状态为配送中，且已支付
        if (ordersDB != null || !status.equals(Orders.DELIVERY_IN_PROGRESS)) {
            // 创建新的订单对象，仅更新ID和状态
            Orders orders = new Orders();
            orders.setId(ordersDB.getId());
            orders.setStatus(Orders.COMPLETED);
            // 更新订单状态为已完成
            orderMapper.update(orders);
        }

    }

    /**
     * 拒绝订单的方法
     *
     * @param ordersRejectionDTO 包含订单ID和拒绝原因的数据传输对象
     */
    @Override
    public void rejectionOrder(OrdersRejectionDTO ordersRejectionDTO) {
        // 根据订单ID查询数据库中的订单信息
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 获取订单的支付状态
        Integer payStatus = ordersDB.getPayStatus();

        // 获取订单的当前状态
        Integer status = ordersDB.getStatus();

        // 检查订单是否存在且状态为待确认
        if (ordersDB != null && status.equals(Orders.TO_BE_CONFIRMED)) {
            // 如果订单已支付，则执行退款操作（代码中被注释掉了）
            if (payStatus.equals(Orders.PAID)) {
                //try
                //    String refund = weChatPayUtil.refund(
                //            ordersDB.getNumber(),
                //            ordersDB.getNumber(),
                //            new BigDecimal(0.01),
                //            new BigDecimal(0.01));
            }
            // 创建新的订单对象用于更新
            Orders orders = new Orders();
            // 设置订单ID
            orders.setId(ordersDB.getId());
            // 更新订单状态为已取消
            orders.setStatus(Orders.CANCELLED);
            // 设置拒绝原因
            orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
            // 设置取消时间
            orders.setCancelTime(LocalDateTime.now());

            // 更新订单信息到数据库
            orderMapper.update(orders);
        }
    }

    /**
     *  确认订单的方法
     * @param id 订单的唯一标识符，用于确认指定订单
     */
    @Override
    public void confirmOrder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        Integer status = ordersDB.getStatus();
        if (ordersDB != null && status.equals(Orders.TO_BE_CONFIRMED)) {
            Orders orders = new Orders();
            orders.setId(ordersDB.getId());
            orders.setStatus(Orders.CONFIRMED);
            orderMapper.update(orders);
        }
    }

    /**
     *  派送订单
     * @param id 订单ID，用于标识需要配送的订单
     */
    @Override
    public void deliveryOrder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        Integer status = ordersDB.getStatus();
        if (ordersDB != null && status.equals(Orders.CONFIRMED)){
            Orders orders = new Orders();
            orders.setId(ordersDB.getId());
            orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
            orderMapper.update(orders);
        }
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pagequery(ordersPageQueryDTO);
         List<OrderVO> orderVOS = getOrderVOS(page);
         return new PageResult(page.getTotal(), orderVOS);
    }

    private List<OrderVO> getOrderVOS(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }


    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
