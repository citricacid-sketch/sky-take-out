import request from './request'

/**
 * 获取店铺营业状态
 */
export function getShopStatus() {
  return request({
    url: '/admin/shop/status',
    method: 'get',
  })
}

/**
 * 设置店铺营业状态
 * @param {number} status - 1 营业 / 0 打烊
 */
export function setShopStatus(status) {
  return request({
    url: `/admin/shop/${status}`,
    method: 'put',
  })
}
