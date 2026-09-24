import { BASE_URL } from './config'

/**
 * 统一请求封装
 * @param {string} url 请求路径（不含 BASE_URL）
 * @param {string} method 请求方法
 * @param {data} data 请求数据
 * @returns {Promise} 返回 data 字段内容
 */
const request = (url, method = 'GET', data = {}) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'authentication': token,
        'Content-Type': 'application/json'
      },
      success: (res) => {
        const { statusCode, data: resData } = res
        if (statusCode === 401 || (resData && resData.code === 401)) {
          // 未登录或 token 过期，跳转登录页
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('未登录'))
          return
        }
        if (resData.code === 1) {
          resolve(resData.data)
        } else {
          uni.showToast({ title: resData.msg || '请求失败', icon: 'none' })
          reject(new Error(resData.msg || '请求失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
