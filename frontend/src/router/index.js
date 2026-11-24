import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import ReaderLayout from '@/layouts/ReaderLayout.vue'
import { isAdmin, isLoggedIn } from '@/api/auth'

const adminMeta = { area: 'admin', requiresAuth: true, requiresAdmin: true }

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', area: 'reader' },
  },
  {
    path: '/',
    component: ReaderLayout,
    redirect: '/rankings',
    children: [
      {
        path: 'rankings',
        name: 'RankingBoard',
        component: () => import('@/views/reader/RankingBoardView.vue'),
        meta: { title: '榜单广场', area: 'reader' },
      },
      {
        path: 'novels/:novelId',
        name: 'NovelInteraction',
        component: () => import('@/views/reader/NovelInteractionView.vue'),
        meta: { title: '作品互动', area: 'reader' },
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: adminMeta,
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { ...adminMeta, title: '数据统计' },
      },
      {
        path: 'audits',
        name: 'AdminAudits',
        component: () => import('@/views/admin/AuditManagementView.vue'),
        meta: { ...adminMeta, title: '审核管理' },
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/views/admin/CommentManagementView.vue'),
        meta: { ...adminMeta, title: '评论审核' },
      },
      {
        path: 'ratings',
        name: 'AdminRatings',
        component: () => import('@/views/admin/RatingAuditView.vue'),
        meta: { ...adminMeta, title: '短评审核' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { ...adminMeta, title: '用户管理' },
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('@/views/admin/TagManagementView.vue'),
        meta: { ...adminMeta, title: '标签管理' },
      },
      {
        path: 'rankings',
        name: 'AdminRankings',
        component: () => import('@/views/admin/RankingConfigView.vue'),
        meta: { ...adminMeta, title: '榜单配置' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/rankings',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = isLoggedIn()

  if (to.meta.requiresAuth && !loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !isAdmin()) {
    return loggedIn
      ? { path: '/rankings' }
      : { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && loggedIn) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : ''
    if (redirect.startsWith('/')) {
      return redirect
    }
    return isAdmin() ? { path: '/admin/dashboard' } : { path: '/rankings' }
  }
  return true
})

router.afterEach((to) => {
  const brand = to.meta.area === 'admin' ? '文枢运营台' : '文枢书院'
  document.title = to.meta.title ? `${to.meta.title} · ${brand}` : brand
})

export default router
