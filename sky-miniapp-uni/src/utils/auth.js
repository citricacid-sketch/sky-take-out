/**
 * 登录相关工具函数
 */

// 保存登录态
export const setLoginSession = (userInfo) => {
  uni.setStorageSync('token', userInfo.token)
  uni.setStorageSync('userInfo', {
    id: userInfo.id,
    openid: userInfo.openid
  })
}

// 获取当前用户 token
export const getToken = () => {
  return uni.getStorageSync('token') || ''
}

// 获取当前用户信息
export const getUserInfo = () => {
  return uni.getStorageSync('userInfo') || null
}

// 是否已登录
export const isLoggedIn = () => {
  return !!getToken()
}

// 退出登录
export const logout = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({ url: '/pages/login/index' })
}
