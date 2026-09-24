import request from './request'

/**
 * 营业额统计
 * @param {{ begin: string, end: string }} params - yyyy-MM-dd
 */
export function getTurnoverStatistics(params) {
  return request({
    url: '/admin/report/turnoverStatistics',
    method: 'get',
    params,
  })
}

/**
 * 用户统计
 * @param {{ begin: string, end: string }} params
 */
export function getUserStatistics(params) {
  return request({
    url: '/admin/report/userStatistics',
    method: 'get',
    params,
  })
}

/**
 * 订单统计
 * @param {{ begin: string, end: string }} params
 */
export function getOrdersStatistics(params) {
  return request({
    url: '/admin/report/ordersStatistics',
    method: 'get',
    params,
  })
}

/**
 * Top10 销量排名
 * @param {{ begin: string, end: string }} params
 */
export function getTop10(params) {
  return request({
    url: '/admin/report/top10',
    method: 'get',
    params,
  })
}

/**
 * 导出报表（Excel 下载）
 * @param {{ begin: string, end: string }} params
 */
export function exportReport(params) {
  return request({
    url: '/admin/report/export',
    method: 'get',
    params,
    responseType: 'blob',
  })
}
