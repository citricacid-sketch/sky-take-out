/**
 * 用户相关接口
 */
import request from './request'

// 微信登录
export function wxLogin(code) {
  return request({
    url: '/user/user/login',
    method: 'POST',
    data: { code }
  })
}

// 获取用户信息（当前接口无单独获取接口，从登录态获取）
export function getUserInfo() {
  return request({
    url: '/user/user/info',
    method: 'GET'
  })
}
