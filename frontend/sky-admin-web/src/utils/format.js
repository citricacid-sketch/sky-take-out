/**
 * 格式化工具函数
 */

/**
 * 格式化日期时间
 * @param {string|Date|number} date
 * @param {string} format - 默认 'yyyy-MM-dd HH:mm:ss'
 */
export function formatDateTime(date, format = 'yyyy-MM-dd HH:mm:ss') {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'

  const pad = (n) => String(n).padStart(2, '0')
  const map = {
    yyyy: d.getFullYear(),
    MM: pad(d.getMonth() + 1),
    dd: pad(d.getDate()),
    HH: pad(d.getHours()),
    mm: pad(d.getMinutes()),
    ss: pad(d.getSeconds()),
  }
  return format.replace(/yyyy|MM|dd|HH|mm|ss/g, (m) => map[m])
}

/**
 * 格式化金额
 * @param {number} amount
 */
export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '¥0.00'
  return '¥' + Number(amount).toFixed(2)
}

/**
 * 订单状态文本
 */
export const orderStatusMap = {
  1: { text: '待付款', type: 'warning' },
  2: { text: '待接单', type: 'warning' },
  3: { text: '已接单', type: '' },
  4: { text: '派送中', type: 'primary' },
  5: { text: '已完成', type: 'success' },
  6: { text: '已取消', type: 'info' },
}

export function getOrderStatusText(status) {
  return orderStatusMap[status]?.text || '未知'
}

export function getOrderStatusType(status) {
  return orderStatusMap[status]?.type || 'info'
}

/**
 * 骑手状态文本
 */
export const riderStatusMap = {
  0: { text: '离线', type: 'info' },
  1: { text: '空闲', type: 'success' },
  2: { text: '配送中', type: 'primary' },
}

export function getRiderStatusText(status) {
  return riderStatusMap[status]?.text || '未知'
}

export function getRiderStatusType(status) {
  return riderStatusMap[status]?.type || 'info'
}

/**
 * 配送状态文本
 */
export const deliveryStatusMap = {
  0: { text: '待分配', type: 'info' },
  1: { text: '已分配', type: '' },
  2: { text: '取餐中', type: 'warning' },
  3: { text: '配送中', type: 'primary' },
  4: { text: '已完成', type: 'success' },
  5: { text: '异常', type: 'danger' },
}

export function getDeliveryStatusText(status) {
  return deliveryStatusMap[status]?.text || '未知'
}

export function getDeliveryStatusType(status) {
  return deliveryStatusMap[status]?.type || 'info'
}

/**
 * 分类类型文本
 */
export function getCategoryTypeText(type) {
  return type === 1 ? '菜品分类' : '套餐分类'
}
