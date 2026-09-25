/**
 * 配送追踪 / 智能客服 接口
 */
import request from './request'

// 查询订单配送状态
export function getDeliveryStatus(orderId) {
  return request({
    url: `/user/delivery/${orderId}`,
    method: 'GET'
  })
}

// 智能客服对话
export function chatWithAI(message) {
  return request({
    url: '/user/chat',
    method: 'POST',
    data: { message }
  })
}
