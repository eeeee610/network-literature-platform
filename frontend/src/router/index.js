import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import UserLayout from '../layouts/UserLayout.vue'

const routes = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    component: UserLayout,
    redirect: '/bookshelf',
    children: [
      {
        path: 'bookshelf',
        component: () => import('../views/BookshelfView.vue'),
        meta: { title: '我的书架' }
      },
      {
        path: 'subscription',
        component: () => import('../views/SubscriptionView.vue'),
        meta: { title: '章节订阅' }
      },
      {
        path: 'reward',
        component: () => import('../views/RewardView.vue'),
        meta: { title: '作品打赏' }
      },
      {
        path: 'account',
        component: () => import('../views/AccountView.vue'),
        meta: { title: '余额与流水' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/bookshelf'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 星阅` : '星阅 · 网络文学平台'
  if (!to.meta.public && !getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.public && getToken()) {
    return { path: '/bookshelf' }
  }
  return true
})

export default router
