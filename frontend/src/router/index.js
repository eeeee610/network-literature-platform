import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
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

function homePath() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    return '/login'
  }
  return currentUser()?.role === 'ADMIN' ? '/admin/dashboard' : '/shelf'
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录注册', public: true },
  },
  {
    path: '/',
    component: ReaderLayout,
    redirect: () => homePath(),
    children: [
      {
        path: 'shelf',
        name: 'Shelf',
        component: () => import('@/views/reader/ShelfView.vue'),
        meta: { title: '我的书架' },
      },
      {
        path: 'subscriptions',
        name: 'Subscriptions',
        component: () => import('@/views/reader/SubscriptionView.vue'),
        meta: { title: '章节订阅' },
      },
      {
        path: 'tips',
        name: 'Tips',
        component: () => import('@/views/reader/TipView.vue'),
        meta: { title: '打赏作者' },
      },
      {
        path: 'balance',
        name: 'Balance',
        component: () => import('@/views/reader/BalanceView.vue'),
        meta: { title: '余额流水' },
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '数据统计' },
      },
      {
        path: 'audits',
        name: 'AdminAudits',
        component: () => import('@/views/admin/AuditManagementView.vue'),
        meta: { title: '审核管理' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('@/views/admin/TagManagementView.vue'),
        meta: { title: '标签管理' },
      },
      {
        path: 'rankings',
        name: 'AdminRankings',
        component: () => import('@/views/admin/RankingConfigView.vue'),
        meta: { title: '榜单配置' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => homePath(),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const user = currentUser()

  if (to.meta.public) {
    if (to.path === '/login' && token) {
      return user?.role === 'ADMIN' ? '/admin/dashboard' : '/shelf'
    }
    return true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.matched.some((record) => record.meta.requiresAdmin) && user?.role !== 'ADMIN') {
    return '/shelf'
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 文枢` : '文枢 · 网络文学平台'
})

export default router
