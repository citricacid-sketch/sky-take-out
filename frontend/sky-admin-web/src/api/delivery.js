import request from './request'

/**
 * 分页查询配送单
 * @param {{ orderId: number, riderId: number, status: number, page: number, pageSize: number }} params
 */
export function getDeliveryPage(params) {
  return request({
    url: '/admin/delivery/page',
    method: 'get',
    params,
  })
}

/**
 * 根据 id 查询配送单详情
 * @param {number} id
 */
export function getDeliveryById(id) {
  return request({
    url: `/admin/delivery/${id}`,
    method: 'get',
  })
}

/**
 * 分配骑手
 * @param {number} id - 配送单 id
 * @param {number} riderId
 */
export function assignRider(id, riderId) {
  return request({
    url: `/admin/delivery/${id}/assign`,
    method: 'post',
    params: { riderId },
  })
}

/**
 * 更新配送单状态
 * @param {number} id - 配送单 id
 * @param {number} status
 */
export function updateDeliveryStatus(id, status) {
  return request({
    url: `/admin/delivery/${id}/status/${status}`,
    method: 'post',
  })
}
