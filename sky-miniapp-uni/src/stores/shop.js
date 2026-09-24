import { defineStore } from 'pinia'
import request from '../utils/request'

export const useShopStore = defineStore('shop', {
  state: () => ({
    status: 1, // 1: 营业中, 0: 打烊中
    loading: false
  }),

  getters: {
    isOpen: (state) => state.status === 1,
    statusText: (state) => state.status === 1 ? '营业中' : '打烊中'
  },

  actions: {
    // 获取店铺状态
    async fetchStatus() {
      this.loading = true
      try {
        const data = await request('/user/shop/status', 'GET')
        this.status = data.status
      } catch (e) {
        // 默认营业中
        this.status = 1
      } finally {
        this.loading = false
      }
    }
  }
})
