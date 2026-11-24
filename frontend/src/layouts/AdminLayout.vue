<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatLineSquare,
  CollectionTag,
  DataAnalysis,
  DocumentChecked,
  Histogram,
  House,
  Star,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { clearSession, currentUser } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const user = ref(currentUser())

const menus = [
  { path: '/admin/dashboard', label: '数据统计', icon: DataAnalysis },
  { path: '/admin/audits', label: '审核管理', icon: DocumentChecked },
  { path: '/admin/comments', label: '评论审核', icon: ChatLineSquare },
  { path: '/admin/ratings', label: '短评审核', icon: Star },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/tags', label: '标签管理', icon: CollectionTag },
  { path: '/admin/rankings', label: '榜单配置', icon: Histogram },
]
const currentTitle = computed(() => route.meta.title || '运营后台')
const displayName = computed(() => user.value?.nickname || user.value?.username || '运营管理员')
const avatarText = computed(() => displayName.value.slice(0, 1))

function roleLabel(role) {
  return { READER: '读者', AUTHOR: '作者', ADMIN: '管理员' }[role] || '管理员'
}

function handleCommand(command) {
  if (command === 'logout') {
    clearSession()
    ElMessage.success('已退出登录')
    router.push('/login')
    return
  }
  if (command === 'reader') {
    router.push('/rankings')
  }
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <div class="brand-seal" aria-hidden="true">文</div>
        <div class="brand-copy">
          <strong>文枢运营台</strong>
          <span>网络文学创作与阅读平台</span>
        </div>
      </div>

      <div class="nav-caption">工作台</div>
      <el-menu :default-active="route.path" router class="nav-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span class="menu-label">{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-note">
        <span class="status-dot" aria-hidden="true"></span>
        <div>
          <strong>C 模块已接入</strong>
          <span>评论 · 评分 · 榜单</span>
        </div>
      </div>
    </el-aside>

    <el-container class="workspace">
      <el-header class="topbar" height="72px">
        <div>
          <p class="topbar-kicker">OPERATIONS / 运营</p>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="topbar-right">
          <el-tag effect="plain" size="small">开发环境</el-tag>
          <span class="topbar-divider" aria-hidden="true"></span>
          <el-dropdown trigger="click" @command="handleCommand">
            <button type="button" class="account-trigger">
              <span class="user-badge" aria-hidden="true">{{ avatarText }}</span>
              <span class="user-copy">
                <strong>{{ displayName }}</strong>
                <small>{{ roleLabel(user?.role) }}</small>
              </span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="reader" :icon="House">进入读者前台</el-dropdown-item>
                <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.account-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 10px 4px 4px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.account-trigger:hover {
  background: #f5f8fb;
  border-color: var(--line);
}

.user-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  text-align: left;
}

.user-copy strong {
  max-width: 140px;
  overflow: hidden;
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-copy small {
  color: #8793a5;
  font-size: 10px;
}

@media (max-width: 600px) {
  .user-copy {
    display: none;
  }
}
</style>
