import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'novel_platform_token'
export const USER_KEY = 'novel_platform_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || typeof body.code !== 'number') {
      const error = new Error('接口响应格式不正确')
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    if (body.code !== 0) {
      const error = new Error(body.message || '请求失败')
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return body.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || '无法连接服务器，请确认后端已启动'
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      ElMessage.error('登录已失效，请重新登录')
      if (!window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
      return Promise.reject(error)
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
