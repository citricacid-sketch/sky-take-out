import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.token = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 文件下载（导出 Excel）直接返回 response
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 1) {
      return res.data
    } else {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  (async (error) => {
    if (error.response) {
      const { status, data: respData } = error.response
      if (status === 401 || (respData && respData.code === 401)) {
        const useUserStore = (await import('@/stores/user')).useUserStore
        useUserStore().logout()
        removeToken()
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
        return Promise.reject(new Error('未登录'))
      }
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  })
)

export default service
