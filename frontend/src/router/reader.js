import { createRouter, createWebHashHistory } from 'vue-router'
import ReaderLayout from '@/layouts/ReaderLayout.vue'

const TOKEN_KEY = 'novel_token'
const USER_KEY = 'novel_user'

function currentUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

function loginPath() {
  return { path: '/login' }
}

const routes = [
  {
    path: '/login',
    name: 'ReaderLogin',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录注册', public: true },
  },
  {
    path: '/',
    component: ReaderLayout,
    redirect: '/shelf',
    children: [
      {
        path: 'shelf',
        name: 'ReaderShelf',
        component: () => import('@/views/reader/ShelfView.vue'),
        meta: { title: '我的书架' },
      },
      {
        path: 'subscriptions',
        name: 'ReaderSubscriptions',
        component: () => import('@/views/reader/SubscriptionView.vue'),
        meta: { title: '章节订阅' },
      },
      {
        path: 'tips',
        name: 'ReaderTips',
        component: () => import('@/views/reader/TipView.vue'),
        meta: { title: '打赏作者' },
      },
      {
        path: 'balance',
        name: 'ReaderBalance',
        component: () => import('@/views/reader/BalanceView.vue'),
        meta: { title: '余额流水' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => (localStorage.getItem(TOKEN_KEY) ? '/shelf' : '/login'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const user = currentUser()

  if (to.meta.public) {
    if (to.path === '/login' && token) {
      return '/shelf'
    }
    return true
  }

  if (!token) {
    return loginPath()
  }

  if (!user) {
    localStorage.removeItem(TOKEN_KEY)
    return loginPath()
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 文枢读者中心` : '文枢读者中心'
})

export default router
