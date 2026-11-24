import request from './request'

export function login(payload) {
  return request.post('/auth/login', payload)
}

export function register(payload) {
  return request.post('/auth/register', payload)
}

export function getMe() {
  return request.get('/auth/me')
}
