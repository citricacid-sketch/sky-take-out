import request from './request'

/**
 * 套餐分页查询
 * @param {{ name: string, page: number, pageSize: number }} params
 */
export function getSetmealPage(params) {
  return request({
    url: '/admin/setmeal/page',
    method: 'get',
    params,
  })
}

/**
 * 新增套餐
 * @param {*} data SetmealDTO
 */
export function addSetmeal(data) {
  return request({
    url: '/admin/setmeal',
    method: 'post',
    data,
  })
}

/**
 * 编辑套餐
 * @param {*} data SetmealDTO (含 id)
 */
export function updateSetmeal(data) {
  return request({
    url: '/admin/setmeal',
    method: 'put',
    data,
  })
}

/**
 * 批量删除套餐
 * @param {string} ids - 逗号分隔的 id
 */
export function deleteSetmeal(ids) {
  return request({
    url: '/admin/setmeal',
    method: 'delete',
    params: { ids },
  })
}

/**
 * 根据 id 查询套餐（含关联菜品）
 * @param {number} id
 */
export function getSetmealById(id) {
  return request({
    url: `/admin/setmeal/${id}`,
    method: 'get',
  })
}

/**
 * 启停售套餐
 * @param {number} status - 0 / 1
 * @param {number} id
 */
export function updateSetmealStatus(status, id) {
  return request({
    url: `/admin/setmeal/status/${status}`,
    method: 'post',
    params: { id },
  })
}
