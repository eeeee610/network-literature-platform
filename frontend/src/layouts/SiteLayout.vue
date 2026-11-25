<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { ElMessage } from 'element-plus'
import { currentUser, isLoggedIn, logout } from '@/api/auth'

const loggedIn = computed(() => isLoggedIn())
const viewer = computed(() => currentUser())

async function handleLogout() {
  try {
    await logout()
    ElMessage.success('已退出登录')
  } finally {
    window.location.href = '/rankings'
  }
}
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="header-inner">
        <RouterLink to="/bookstore" class="site-brand">
          <span class="brand-seal" aria-hidden="true">文</span>
          <span class="brand-text">
            <strong>文枢书城</strong>
            <small>网络文学创作与阅读平台</small>
          </span>
        </RouterLink>

        <nav class="site-nav">
          <RouterLink to="/bookstore">书城</RouterLink>
          <RouterLink to="/author">创作台</RouterLink>
        </nav>

        <div class="header-actions">
          <template v-if="loggedIn">
            <RouterLink to="/account" class="user-link">{{ viewer?.nickname || viewer?.username || '我的书院' }}</RouterLink>
            <el-button size="small" @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <RouterLink to="/login"><el-button size="small">登录</el-button></RouterLink>
            <RouterLink to="/register"><el-button size="small" type="primary">注册</el-button></RouterLink>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <RouterView />
    </main>

    <footer class="site-footer">
      <span>网络文学创作与阅读平台</span>
      <span>以书评为楼层，让每一次阅读留下回响</span>
    </footer>
  </div>
</template>

<style scoped>
.site-shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background: #f3f5f8;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(8px);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 28px;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 14px 24px;
}

.site-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.brand-seal {
  display: grid;
  width: 36px;
  height: 42px;
  place-items: center;
  color: #fff;
  font: 600 21px/1 "Songti SC", "SimSun", serif;
  background: var(--accent);
  border-radius: 3px 3px 10px 3px;
  box-shadow: inset 0 -3px 0 rgba(0, 0, 0, 0.13);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-text strong {
  color: var(--ink);
  font: 600 18px/1.2 "Songti SC", "SimSun", serif;
  letter-spacing: 0.06em;
}

.brand-text small {
  color: var(--muted);
  font-size: 10px;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.site-nav a {
  padding: 8px 16px;
  color: #4a5a70;
  font-size: 14px;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s ease, color 0.2s ease;
}

.site-nav a:hover {
  color: var(--accent);
  background: var(--accent-light);
}

.site-nav a.router-link-active {
  color: var(--accent);
  font-weight: 600;
  background: var(--accent-light);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.site-main {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 30px 24px 56px;
  flex: 1;
}

.site-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 18px 24px 28px;
  color: #8793a5;
  font-size: 12px;
  border-top: 1px solid var(--line);
}

.user-link {
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.user-link:hover {
  text-decoration: underline;
}

@media (max-width: 720px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 12px 16px;
    padding: 12px 16px;
  }

  .site-nav {
    order: 3;
    width: 100%;
  }

  .site-main {
    padding: 20px 16px 40px;
  }

  .site-footer {
    padding: 16px;
  }
}
</style>
