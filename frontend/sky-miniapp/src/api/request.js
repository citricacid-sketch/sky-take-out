/**
 * 苍穹外卖 - 请求封装
 * 统一返回格式: { code, data, msg }，code=1 为成功
 *
 * 基础地址统一由 src/config/env.js 根据运行平台（h5/mp）决定。
 */
import { BASE_URL } from '@/config/env'

const AUTH_HEADER = 'authentication'

function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header[AUTH_HEADER] = token
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: 15000,
      success(res) {
        const { statusCode, data: respData } = res
        if (statusCode === 200 && respData && respData.code === 1) {
          resolve(respData.data)
        } else if (statusCode === 401 || (respData && respData.code === 401)) {
          // token 失效，清除登录状态
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          reject(new Error('未登录或登录已过期'))
        } else {
          const msg = (respData && respData.msg) || '请求失败'
          uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail(err) {
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
export { BASE_URL }
