import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000,
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
    const message = error.response?.data?.message || '无法连接服务器，请确认后端已启动'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
