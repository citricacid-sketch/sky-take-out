import request from './request'

/**
 * 分页查询分类
 * @param {{ name: string, type: number, page: number, pageSize: number }} params
 */
export function getCategoryPage(params) {
  return request({
    url: '/admin/category/page',
    method: 'get',
    params,
  })
}

/**
 * 新增分类
 * @param {{ type: number, name: string, sort: number }} data
 */
export function addCategory(data) {
  return request({
    url: '/admin/category',
    method: 'post',
    data,
  })
}

/**
 * 编辑分类
 * @param {{ id: number, type: number, name: string, sort: number }} data
 */
export function updateCategory(data) {
  return request({
    url: '/admin/category',
    method: 'put',
    data,
  })
}

/**
 * 删除分类
 * @param {number} id
 */
export function deleteCategory(id) {
  return request({
    url: '/admin/category',
    method: 'delete',
    params: { id },
  })
}

/**
 * 启用/禁用分类
 * @param {number} status - 0 / 1
 * @param {number} id
 */
export function updateCategoryStatus(status, id) {
  return request({
    url: `/admin/category/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/**
 * 根据类型查询分类列表
 * @param {number} type - 1 菜品分类 / 2 套餐分类
 */
export function getCategoryList(type) {
  return request({
    url: '/admin/category/list',
    method: 'get',
    params: { type },
  })
}
