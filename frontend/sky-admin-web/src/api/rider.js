import request from './request'

/**
 * 新增骑手
 * @param {{ name: string, phone: string }} data
 */
export function addRider(data) {
  return request({
    url: '/admin/rider',
    method: 'post',
    data,
  })
}

/**
 * 编辑骑手
 * @param {{ id: number, name: string, phone: string, status: number }} data
 */
export function updateRider(data) {
  return request({
    url: '/admin/rider',
    method: 'put',
    data,
  })
}

/**
 * 根据 id 查询骑手
 * @param {number} id
 */
export function getRiderById(id) {
  return request({
    url: `/admin/rider/${id}`,
    method: 'get',
  })
}

/**
 * 分页查询骑手
 * @param {{ status: number, page: number, pageSize: number }} params
 */
export function getRiderPage(params) {
  return request({
    url: '/admin/rider/page',
    method: 'get',
    params,
  })
}

/**
 * 更新骑手状态
 * @param {number} status - 0 离线 / 1 空闲 / 2 配送中
 * @param {number} id
 */
export function updateRiderStatus(status, id) {
  return request({
    url: `/admin/rider/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/**
 * 更新骑手位置
 * @param {{ riderId: number, lng: number, lat: number }} data
 */
export function updateRiderLocation(data) {
  return request({
    url: '/admin/rider/location',
    method: 'post',
    params: data,
  })
}
