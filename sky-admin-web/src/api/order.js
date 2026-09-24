import request from './request'

/**
 * 订单条件搜索（分页）
 * @param {{ number: string, phone: string, status: number, beginTime: string, endTime: string, page: number, pageSize: number }} params
 */
export function getOrderPage(params) {
  return request({
    url: '/admin/order/conditionSearch',
    method: 'get',
    params,
  })
}

/**
 * 根据 id 查询订单详情
 * @param {number} id
 */
export function getOrderDetail(id) {
  return request({
    url: `/admin/order/details/${id}`,
    method: 'get',
  })
}

/**
 * 接单
 * @param {{ id: number }} data
 */
export function confirmOrder(data) {
  return request({
    url: '/admin/order/confirm',
    method: 'put',
    data,
  })
}

/**
 * 拒单
 * @param {{ id: number, rejectionReason: string }} data
 */
export function rejectionOrder(data) {
  return request({
    url: '/admin/order/rejection',
    method: 'put',
    data,
  })
}

/**
 * 取消订单
 * @param {{ id: number, cancelReason: string }} data
 */
export function cancelOrder(data) {
  return request({
    url: '/admin/order/cancel',
    method: 'put',
    data,
  })
}

/**
 * 派送订单
 * @param {number} id
 */
export function deliveryOrder(id) {
  return request({
    url: `/admin/order/delivery/${id}`,
    method: 'put',
  })
}

/**
 * 完成订单
 * @param {number} id
 */
export function completeOrder(id) {
  return request({
    url: `/admin/order/complete/${id}`,
    method: 'put',
  })
}

/**
 * 查询订单可执行动作
 * @param {number} id
 */
export function getOrderActions(id) {
  return request({
    url: `/admin/order/${id}/actions`,
    method: 'get',
  })
}
