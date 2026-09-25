/**
 * 购物车状态管理（全局共享，与后端同步）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCartList, addToCart, subCartItem, cleanCart } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([]) // 购物车商品列表
  const loading = ref(false)

  // 总数量
  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.number, 0)
  })

  // 总金额
  const totalAmount = computed(() => {
    return items.value.reduce((sum, item) => sum + Number(item.amount) * item.number, 0).toFixed(2)
  })

  // 从后端拉取购物车
  async function fetchCart() {
    loading.value = true
    try {
      const data = await getCartList()
      // 后端返回的 amount 是单价，number 是数量
      items.value = data || []
    } catch (e) {
      // 未登录时静默忽略
      items.value = []
    } finally {
      loading.value = false
    }
  }

  // 添加商品
  async function add(dishId, setmealId, dishFlavor) {
    await addToCart({ dishId, setmealId, dishFlavor })
    await fetchCart()
  }

  // 减一个商品
  async function sub(dishId, setmealId, dishFlavor) {
    await subCartItem({ dishId, setmealId, dishFlavor })
    await fetchCart()
  }

  // 清空
  async function clear() {
    await cleanCart()
    items.value = []
  }

  return {
    items,
    loading,
    totalCount,
    totalAmount,
    fetchCart,
    add,
    sub,
    clear
  }
})
