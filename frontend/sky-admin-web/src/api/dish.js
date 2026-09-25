import request from './request'

/**
 * 菜品分页查询
 * @param {{ name: string, categoryId: number, status: number, page: number, pageSize: number }} params
 */
export function getDishPage(params) {
  return request({
    url: '/admin/dish/page',
    method: 'get',
    params,
  })
}

/**
 * 新增菜品
 * @param {*} data DishDTO
 */
export function addDish(data) {
  return request({
    url: '/admin/dish',
    method: 'post',
    data,
  })
}

/**
 * 编辑菜品
 * @param {*} data DishDTO (含 id)
 */
export function updateDish(data) {
  return request({
    url: '/admin/dish',
    method: 'put',
    data,
  })
}

/**
 * 批量删除菜品
 * @param {string} ids - 逗号分隔的 id，如 "1,2,3"
 */
export function deleteDish(ids) {
  return request({
    url: '/admin/dish',
    method: 'delete',
    params: { ids },
  })
}

/**
 * 根据 id 查询菜品（含口味）
 * @param {number} id
 */
export function getDishById(id) {
  return request({
    url: `/admin/dish/${id}`,
    method: 'get',
  })
}

/**
 * 启停售菜品
 * @param {number} status - 0 / 1
 * @param {number} id
 */
export function updateDishStatus(status, id) {
  return request({
    url: `/admin/dish/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/**
 * 根据分类 id 查询菜品列表
 * @param {number} categoryId
 */
export function getDishListByCategory(categoryId) {
  return request({
    url: '/admin/dish/list',
    method: 'get',
    params: { categoryId },
  })
}

/**
 * 查询所有起售菜品列表
 */
export function getDishList() {
  return request({
    url: '/admin/dish/list',
    method: 'get',
  })
}

/**
 * 图片上传
 * @param {File} file
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
