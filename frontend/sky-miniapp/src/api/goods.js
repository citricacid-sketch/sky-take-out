/**
 * 分类、菜品、套餐相关接口
 */
import request from './request'

// 查询分类列表 type: 1菜品分类 2套餐分类
export function getCategoryList(type) {
  return request({
    url: '/user/category/list',
    method: 'GET',
    data: type ? { type } : {}
  })
}

// 根据分类id查询菜品（含口味）
export function getDishList(categoryId) {
  return request({
    url: '/user/dish/list',
    method: 'GET',
    data: { categoryId }
  })
}

// 根据分类id查询套餐
export function getSetmealList(categoryId) {
  return request({
    url: '/user/setmeal/list',
    method: 'GET',
    data: { categoryId }
  })
}

// 根据套餐id查询包含的菜品列表
export function getSetmealDishList(id) {
  return request({
    url: `/user/setmeal/dish/${id}`,
    method: 'GET'
  })
}
