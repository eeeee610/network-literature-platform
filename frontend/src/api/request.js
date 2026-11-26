import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'novel_platform_token'
export const USER_KEY = 'novel_platform_user'

// VSCode 集成浏览器等 webview 环境可能禁用或隔离 localStorage，
// 此时降级到内存存储，保证当前会话内登录流程可用。
const memoryStore = new Map()

function safeGetItem(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return memoryStore.has(key) ? memoryStore.get(key) : null
  }
}

export function safeSetItem(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    memoryStore.set(key, value)
  }
}

export function safeRemoveItem(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // 忽略：存储不可用时仅清理内存
  }
  memoryStore.delete(key)
}

export function getToken() {
  return safeGetItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  try {
    return JSON.parse(safeGetItem(USER_KEY) || 'null')
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
      safeRemoveItem(TOKEN_KEY)
      safeRemoveItem(USER_KEY)
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
