import { computed, reactive } from 'vue'

const TOKEN_KEY = 'novel_token'
const USER_KEY = 'novel_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
})

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(state.token))
  const isAdmin = computed(() => state.user?.role === 'ADMIN')

  function setSession(data) {
    state.token = data.token
    state.user = data.user
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  }

  function clearSession() {
    state.token = ''
    state.user = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function homePath() {
    return '/shelf'
  }

  return { state, isLoggedIn, isAdmin, setSession, clearSession, homePath }
}
