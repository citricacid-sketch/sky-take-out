/**
 * 工具函数
 */

// 订单状态映射
export const ORDER_STATUS = {
  1: { text: '待付款', color: '#FA5151', icon: '⏰' },
  2: { text: '待接单', color: '#FFC300', icon: '📋' },
  3: { text: '已接单', color: '#10AEFF', icon: '👨‍🍳' },
  4: { text: '派送中', color: '#10AEFF', icon: '🛵' },
  5: { text: '已完成', color: '#07C160', icon: '✅' },
  6: { text: '已取消', color: '#999999', icon: '❌' }
}

// 配送状态映射
export const DELIVERY_STATUS = {
  0: '待分配',
  1: '已分配',
  2: '取餐中',
  3: '配送中',
  4: '已完成',
  5: '异常'
}

// 格式化时间
export function formatTime(datetime) {
  if (!datetime) return ''
  const d = new Date(datetime)
  if (isNaN(d.getTime())) return datetime
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 格式化金额
export function formatPrice(price) {
  return Number(price).toFixed(2)
}

// 登录校验装饰器
export function requireLogin() {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录',
      success(res) {
        if (res.confirm) {
          uni.navigateTo({ url: '/pages/login/login' })
        }
      }
    })
    return false
  }
  return true
}
