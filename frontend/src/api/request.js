import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'novel_token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
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
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('novel_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    const message = error.response?.data?.message || '无法连接服务器，请确认后端已启动'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
