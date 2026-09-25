/**
 * 购物车相关接口
 */
import request from './request'

// 添加购物车
export function addToCart(data) {
  return request({
    url: '/user/shoppingCart/add',
    method: 'POST',
    data
  })
}

// 查看购物车
export function getCartList() {
  return request({
    url: '/user/shoppingCart/list',
    method: 'GET'
  })
}

// 清空购物车
export function cleanCart() {
  return request({
    url: '/user/shoppingCart/clean',
    method: 'DELETE'
  })
}

// 购物车减一
export function subCartItem(data) {
  return request({
    url: '/user/shoppingCart/sub',
    method: 'POST',
    data
  })
}

// 推荐菜品
export function getRecommendList() {
  return request({
    url: '/user/recommend',
    method: 'GET'
  })
}
