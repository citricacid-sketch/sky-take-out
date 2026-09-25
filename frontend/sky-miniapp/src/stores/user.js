/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wxLogin } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null) // { id, openid, nickname, avatar }

  const isLoggedIn = computed(() => !!token.value)

  // 从本地存储初始化
  function initFromStorage() {
    const savedToken = uni.getStorageSync('token')
    const savedUser = uni.getStorageSync('userInfo')
    if (savedToken) token.value = savedToken
    if (savedUser) userInfo.value = savedUser
  }

  // 微信登录
  function login() {
    return new Promise((resolve, reject) => {
      uni.login({
        provider: 'wechat',
        success(res) {
          if (!res.code) {
            reject(new Error('获取微信 code 失败'))
            return
          }
          wxLogin(res.code)
            .then(data => {
              token.value = data.token
              userInfo.value = { id: data.id, openid: data.openid }
              uni.setStorageSync('token', data.token)
              uni.setStorageSync('userInfo', userInfo.value)
              resolve(data)
            })
            .catch(reject)
        },
        fail(err) {
          reject(err)
        }
      })
    })
  }

  // 静默登录（已有 token 则跳过）
  function silentLogin() {
    if (isLoggedIn.value) return Promise.resolve()
    return login()
  }

  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  // 设置用户资料（昵称头像）
  function setUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    uni.setStorageSync('userInfo', userInfo.value)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    initFromStorage,
    login,
    silentLogin,
    logout,
    setUserInfo
  }
})
