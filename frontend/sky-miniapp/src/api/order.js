/**
 * 订单相关接口
 */
import request from './request'

// 提交订单
export function submitOrder(data) {
  return request({
    url: '/user/order/submit',
    method: 'POST',
    data
  })
}

// 订单支付（模拟）
export function payOrder(data) {
  return request({
    url: '/user/order/payment',
    method: 'PUT',
    data
  })
}

// 查询历史订单
export function getHistoryOrders(params) {
  return request({
    url: '/user/order/historyOrders',
    method: 'GET',
    data: params
  })
}

// 查询订单明细
export function getOrderDetail(id) {
  return request({
    url: `/user/order/orderDetail/${id}`,
    method: 'GET'
  })
}

// 取消订单
export function cancelOrder(id) {
  return request({
    url: `/user/order/cancel/${id}`,
    method: 'PUT'
  })
}

// 再来一单
export function repetitionOrder(id) {
  return request({
    url: `/user/order/repetition/${id}`,
    method: 'POST'
  })
}

// 催单
export function reminderOrder(id) {
  return request({
    url: `/user/order/reminder/${id}`,
    method: 'GET'
  })
}

// 查询订单可执行动作
export function getOrderActions(id) {
  return request({
    url: `/user/order/${id}/actions`,
    method: 'GET'
  })
}
