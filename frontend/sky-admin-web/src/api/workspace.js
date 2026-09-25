import request from './request'

/**
 * 工作台 - 今日营业数据
 */
export function getBusinessData() {
  return request({
    url: '/admin/workspace/businessData',
    method: 'get',
  })
}

/**
 * 工作台 - 订单概览
 */
export function getOrderOverview() {
  return request({
    url: '/admin/workspace/overviewOrders',
    method: 'get',
  })
}

/**
 * 工作台 - 菜品总览
 */
export function getDishOverview() {
  return request({
    url: '/admin/workspace/overviewDishes',
    method: 'get',
  })
}

/**
 * 工作台 - 套餐总览
 */
export function getSetmealOverview() {
  return request({
    url: '/admin/workspace/overviewSetmeals',
    method: 'get',
  })
}
