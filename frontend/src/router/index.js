import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/',
    redirect: '/admin/dashboard',
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
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
    redirect: '/admin/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 文枢运营台` : '文枢运营台'
})

export default router
