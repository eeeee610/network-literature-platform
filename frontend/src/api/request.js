import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '../utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function redirectToLogin() {
  clearAuth()
  const current = window.location.pathname + window.location.search
  if (!current.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || typeof body.code === 'undefined') {
      ElMessage.error('服务响应格式异常')
      return Promise.reject(new Error('invalid response'))
    }
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 40100) {
      redirectToLogin()
    } else {
      ElMessage.error(body.message || '请求失败')
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const body = error.response && error.response.data
    if (body && body.code === 40100) {
      redirectToLogin()
    } else {
      ElMessage.error((body && body.message) || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
