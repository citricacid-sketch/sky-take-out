import request from './request'

/**
 * 员工登录
 * @param {{ username: string, password: string }} data - password 已 MD5 加密
 */
export function login(data) {
  return request({
    url: '/admin/employee/login',
    method: 'post',
    data,
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/admin/employee/logout',
    method: 'post',
  })
}

/**
 * 分页查询员工
 * @param {{ name: string, page: number, pageSize: number }} params
 */
export function getEmployeePage(params) {
  return request({
    url: '/admin/employee/page',
    method: 'get',
    params,
  })
}

/**
 * 新增员工
 * @param {*} data EmployeeDTO
 */
export function addEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'post',
    data,
  })
}

/**
 * 编辑员工
 * @param {*} data EmployeeDTO (含 id)
 */
export function updateEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'put',
    data,
  })
}

/**
 * 根据 id 查询员工
 * @param {number} id
 */
export function getEmployeeById(id) {
  return request({
    url: `/admin/employee/${id}`,
    method: 'get',
  })
}

/**
 * 启用/禁用员工
 * @param {number} status - 0 禁用 / 1 启用
 * @param {number} id
 */
export function updateEmployeeStatus(status, id) {
  return request({
    url: `/admin/employee/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/**
 * 修改密码
 * @param {{ oldPassword: string, newPassword: string }} data
 */
export function editPassword(data) {
  return request({
    url: '/admin/employee/editPassword',
    method: 'put',
    data,
  })
}
