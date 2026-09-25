/**
 * 店铺相关接口
 */
import request from './request'

// 获取店铺营业状态 1营业/0打烊
export function getShopStatus() {
  return request({
    url: '/user/shop/status',
    method: 'GET'
  })
}
