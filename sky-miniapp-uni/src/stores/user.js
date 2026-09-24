import { defineStore } from 'pinia'
import { isLoggedIn, setLoginSession, getUserInfo, logout as doLogout } from '../utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: getUserInfo(),
    isLogin: !!uni.getStorageSync('token')
  }),

  getters: {
    userId: (state) => state.userInfo?.id || null,
    openid: (state) => state.userInfo?.openid || null
  },

  actions: {
    // 微信登录
    async login() {
      return new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: async (loginRes) => {
            if (loginRes.code) {
              try {
                const request = (await import('../utils/request')).default
                const data = await request('/user/user/login', 'POST', { code: loginRes.code })
                this.setSession(data)
                resolve(data)
              } catch (e) {
                reject(e)
              }
            } else {
              reject(new Error('微信登录失败'))
            }
          },
          fail: reject
        })
      })
    },

    // 保存登录态
    setSession(userInfo) {
      setLoginSession(userInfo)
      this.token = userInfo.token
      this.userInfo = { id: userInfo.id, openid: userInfo.openid }
      this.isLogin = true
    },

    // 退出登录
    logout() {
      doLogout()
      this.token = ''
      this.userInfo = null
      this.isLogin = false
    }
  }
})
