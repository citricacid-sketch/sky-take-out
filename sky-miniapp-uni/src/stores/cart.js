import { defineStore } from 'pinia'
import request from '../utils/request'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [], // { id, name, price, image, amount, flavors: [] }
    totalQuantity: 0,
    totalAmount: 0
  }),

  getters: {
    isEmpty: (state) => state.items.length === 0
  },

  actions: {
    // 加载购物车列表
    async loadCart() {
      try {
        const list = await request('/user/shoppingCart/list', 'GET')
        this.items = (list || []).map(item => ({
          id: item.id,
          dishId: item.dishId,
          setmealId: item.setmealId,
          name: item.name,
          image: item.image,
          amount: item.amount,
          number: item.number,
          flavors: item.dishFlavor ? [item.dishFlavor] : []
        }))
        this.calcTotal()
      } catch (e) {
        // 忽略加载失败
      }
    },

    // 添加商品到购物车
    async addItem(dish) {
      const payload = {
        dishId: dish.dishId,
        setmealId: dish.setmealId,
        dishFlavor: dish.flavors ? dish.flavors.join(',') : undefined,
        amount: dish.amount,
        number: 1
      }
      await request('/user/shoppingCart/add', 'POST', payload)
      await this.loadCart()
    },

    // 增加数量
    async increase(item) {
      await request('/user/shoppingCart/add', 'POST', {
        dishId: item.dishId,
        setmealId: item.setmealId,
        amount: item.amount
      })
      await this.loadCart()
    },

    // 减少数量
    async decrease(item) {
      if (item.number <= 1) {
        await request('/user/shoppingCart/sub', 'POST', {
          dishId: item.dishId,
          setmealId: item.setmealId
        })
      } else {
        await request('/user/shoppingCart/sub', 'POST', {
          dishId: item.dishId,
          setmealId: item.setmealId
        })
      }
      await this.loadCart()
    },

    // 清空购物车
    async clearCart() {
      await request('/user/shoppingCart/clean', 'DELETE')
      this.items = []
      this.totalQuantity = 0
      this.totalAmount = 0
    },

    // 计算总价
    calcTotal() {
      let qty = 0
      let amt = 0
      this.items.forEach(item => {
        qty += item.number
        amt += item.amount * item.number
      })
      this.totalQuantity = qty
      this.totalAmount = amt
    }
  }
})
