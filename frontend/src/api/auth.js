import request, {
  TOKEN_KEY,
  USER_KEY,
  getStoredUser,
  getToken,
  safeRemoveItem,
  safeSetItem,
} from './request'

export function isLoggedIn() {
  return Boolean(getToken())
}

export function isAdmin() {
  return getStoredUser()?.role === 'ADMIN'
}

export function currentUser() {
  return getStoredUser()
}

export function setSession(loginData) {
  safeSetItem(TOKEN_KEY, loginData.token)
  safeSetItem(
    USER_KEY,
    JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      nickname: loginData.nickname,
      avatarUrl: loginData.avatarUrl,
      role: loginData.role,
    }),
  )
}

export function clearSession() {
  safeRemoveItem(TOKEN_KEY)
  safeRemoveItem(USER_KEY)
}

export function updateStoredUser(profile) {
  const current = getStoredUser() || {}
  safeSetItem(USER_KEY, JSON.stringify({ ...current, ...profile }))
}

export async function login(payload) {
  const data = await request.post('/user/auth/login', payload)
  setSession(data)
  return data
}

export async function register(payload) {
  const data = await request.post('/user/auth/register', payload)
  setSession(data)
  return data
}

export async function logout() {
  try {
    await request.post('/user/auth/logout')
  } finally {
    clearSession()
  }
}
