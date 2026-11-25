<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading, Search, SwitchButton, Trophy, User } from '@element-plus/icons-vue'
import { clearSession, currentUser, isAdmin, isLoggedIn } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const user = ref(currentUser())
const loggedIn = computed(() => isLoggedIn())
const admin = computed(() => isAdmin())

const navItems = [
  { path: '/rankings', label: '榜单广场', icon: Trophy },
]
const novelIdInput = ref('')
const searchLoading = ref(false)
const searchInputRef = ref(null)

function goNovel() {
  const value = novelIdInput.value.trim()
  if (!/^\d+$/.test(value)) {
    ElMessage.warning('请输入数字作品 ID')
    return
  }
  searchLoading.value = true
  router.push(`/novels/${value}`).finally(() => {
    searchLoading.value = false
  })
}

function handleCommand(command) {
  if (command === 'logout') {
    clearSession()
    user.value = null
    router.push('/login')
    return
  }
  if (command === 'admin') {
    router.push('/admin/dashboard')
    return
  }
  if (command === 'reader') {
    router.push('/rankings')
  }
}

function roleLabel(role) {
  return { READER: '读者', AUTHOR: '作者', ADMIN: '管理员' }[role] || '读者'
}

const activePath = computed(() => (route.path.startsWith('/novels') ? '/novels' : route.path))
</script>

<template>
  <div class="reader-shell">
    <header class="reader-header">
      <div class="reader-header-inner">
        <RouterLink class="reader-brand" to="/rankings">
          <span class="reader-seal" aria-hidden="true">文</span>
          <span class="reader-brand-copy">
            <strong>文枢书院</strong>
            <small>网络文学创作与阅读平台</small>
          </span>
        </RouterLink>

        <nav class="reader-nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="reader-nav-link"
            :class="{ active: activePath === item.path }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </RouterLink>
          <button
            type="button"
            class="reader-nav-link nav-button"
            :class="{ active: activePath === '/novels' }"
            @click="searchInputRef?.focus()"
          >
            <el-icon><Reading /></el-icon>
            作品互动
          </button>
        </nav>

        <form class="novel-search" @submit.prevent="goNovel">
          <el-input
            ref="searchInputRef"
            v-model="novelIdInput"
            size="small"
            placeholder="输入作品 ID"
            :prefix-icon="Search"
            clearable
            aria-label="输入作品 ID 前往作品互动页"
          />
          <el-button size="small" type="primary" :loading="searchLoading" native-type="submit">
            进入
          </el-button>
        </form>

        <div class="reader-account">
          <template v-if="loggedIn && user">
            <el-dropdown trigger="click" @command="handleCommand">
              <button type="button" class="account-trigger">
                <span class="account-badge">{{ (user.nickname || user.username || '读').slice(0, 1) }}</span>
                <span class="account-copy">
                  <strong>{{ user.nickname || user.username }}</strong>
                  <small>{{ roleLabel(user.role) }}</small>
                </span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="admin" command="admin">进入运营后台</el-dropdown-item>
                  <el-dropdown-item command="reader">榜单广场</el-dropdown-item>
                  <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button size="small" :icon="User" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="reader-main">
      <RouterView />
    </main>

    <footer class="reader-footer">
      <span>文枢书院 · 互动与榜单</span>
      <span>评论、评分与榜单数据由 C 模块公共过程统一维护</span>
    </footer>
  </div>
</template>

<style scoped>
.reader-shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background: #f5f2ea;
}

.reader-header {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 253, 248, 0.96);
  border-bottom: 1px solid #e6dfd2;
  backdrop-filter: blur(8px);
}

.reader-header-inner {
  display: flex;
  max-width: 1240px;
  min-height: 72px;
  align-items: center;
  gap: 26px;
  margin: 0 auto;
  padding: 0 28px;
}

.reader-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--ink);
  text-decoration: none;
}

.reader-seal {
  display: grid;
  width: 38px;
  height: 44px;
  place-items: center;
  color: #fff;
  font: 600 22px/1 "Songti SC", "SimSun", serif;
  background: var(--ink);
  border-radius: 3px 3px 10px 3px;
  box-shadow: inset 0 -3px 0 rgba(195, 144, 63, 0.85);
}

.reader-brand-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.reader-brand-copy strong {
  font: 600 19px/1.1 "Songti SC", "SimSun", serif;
  letter-spacing: 0.12em;
}

.reader-brand-copy small {
  color: #948a79;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.reader-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.reader-nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 13px;
  color: #6e6455;
  font-size: 13px;
  text-decoration: none;
  border-radius: 5px;
  transition: color 0.2s ease, background 0.2s ease;
}

.reader-nav-link:hover,
.reader-nav-link.active {
  color: var(--ink);
  background: #f0e9db;
}

.nav-button {
  background: transparent;
  border: 0;
  cursor: pointer;
}

.novel-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.novel-search :deep(.el-input) {
  width: 168px;
}

.reader-account {
  display: flex;
  align-items: center;
}

.account-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 9px 5px 5px;
  background: #f7f2e9;
  border: 1px solid #e6dfd2;
  border-radius: 999px;
  cursor: pointer;
}

.account-trigger:hover {
  border-color: #cdbfa4;
}

.account-badge {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #fff;
  font: 600 14px/1 Georgia, serif;
  background: var(--accent);
  border-radius: 50%;
}

.account-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-right: 5px;
  text-align: left;
}

.account-copy strong {
  max-width: 120px;
  overflow: hidden;
  color: var(--ink);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-copy small {
  color: #948a79;
  font-size: 10px;
}

.reader-main {
  width: 100%;
  max-width: 1240px;
  flex: 1;
  margin: 0 auto;
  padding: 38px 28px 56px;
}

.reader-footer {
  display: flex;
  max-width: 1240px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 auto;
  padding: 22px 28px 30px;
  color: #9b9284;
  font-size: 11px;
  border-top: 1px solid #e6dfd2;
}

@media (max-width: 1080px) {
  .reader-header-inner {
    flex-wrap: wrap;
    gap: 14px;
    padding: 12px 20px;
  }

  .novel-search {
    order: 4;
    width: 100%;
    margin-left: 0;
  }

  .novel-search :deep(.el-input) {
    flex: 1;
    width: auto;
  }
}

@media (max-width: 720px) {
  .reader-nav,
  .reader-brand-copy small,
  .account-copy {
    display: none;
  }

  .reader-main {
    padding: 26px 16px 40px;
  }

  .reader-footer {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px 16px;
  }
}
</style>
