import request, { TOKEN_KEY, USER_KEY, getStoredUser, getToken } from './request'

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
  localStorage.setItem(TOKEN_KEY, loginData.token)
  localStorage.setItem(
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
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function updateStoredUser(profile) {
  const current = getStoredUser() || {}
  localStorage.setItem(USER_KEY, JSON.stringify({ ...current, ...profile }))
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
