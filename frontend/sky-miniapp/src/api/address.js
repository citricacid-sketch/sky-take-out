/**
 * 地址簿相关接口
 */
import request from './request'

// 查询当前用户所有地址
export function getAddressList() {
  return request({
    url: '/user/addressBook/list',
    method: 'GET'
  })
}

// 查询默认地址
export function getDefaultAddress() {
  return request({
    url: '/user/addressBook/default',
    method: 'GET'
  })
}

// 根据id查询地址
export function getAddressById(id) {
  return request({
    url: `/user/addressBook/${id}`,
    method: 'GET'
  })
}

// 新增地址
export function addAddress(data) {
  return request({
    url: '/user/addressBook',
    method: 'POST',
    data
  })
}

// 修改地址
export function updateAddress(data) {
  return request({
    url: '/user/addressBook',
    method: 'PUT',
    data
  })
}

// 设置默认地址
export function setDefaultAddress(data) {
  return request({
    url: '/user/addressBook/default',
    method: 'PUT',
    data
  })
}

// 删除地址
export function deleteAddress(id) {
  return request({
    url: '/user/addressBook',
    method: 'DELETE',
    data: { id }
  })
}
