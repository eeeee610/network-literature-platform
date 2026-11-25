import userRequest from './userRequest'

export function login(payload) {
  return userRequest.post('/auth/login', payload)
}

export function register(payload) {
  return userRequest.post('/auth/register', payload)
}

export function getMe() {
  return userRequest.get('/auth/me')
}
