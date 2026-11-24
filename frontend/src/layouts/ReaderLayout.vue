<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Collection,
  Reading,
  SwitchButton,
  Tickets,
  UserFilled,
  Wallet,
} from '@element-plus/icons-vue'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const navItems = [
  { path: '/shelf', label: '我的书架', icon: Collection },
  { path: '/subscriptions', label: '章节订阅', icon: Reading },
  { path: '/tips', label: '打赏作者', icon: Tickets },
  { path: '/balance', label: '余额流水', icon: Wallet },
]

const currentTitle = computed(() => route.meta.title || '读者中心')
const displayName = computed(() => auth.state.user?.nickname || auth.state.user?.username || '读者')
const roleLabel = computed(() => ({ READER: '读者', AUTHOR: '作者', ADMIN: '管理员' }[auth.state.user?.role] || '读者'))
const isAdmin = auth.isAdmin

function logout() {
  auth.clearSession()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <el-container class="reader-shell">
    <el-aside class="reader-sidebar" width="236px">
      <div class="reader-brand">
        <div class="reader-seal">文</div>
        <div>
          <strong>文枢</strong>
          <span>读者中心</span>
        </div>
      </div>

      <p class="reader-nav-caption">阅读与交易</p>
      <el-menu :default-active="route.path" router class="reader-menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="reader-sidebar-note">
        <span class="reader-dot" aria-hidden="true"></span>
        <div>
          <strong>交易由数据库事务保障</strong>
          <span>订阅与打赏均写账户流水</span>
        </div>
      </div>
    </el-aside>

    <el-container class="reader-workspace">
      <el-header class="reader-topbar" height="72px">
        <div>
          <p class="reader-kicker">READER / 读者</p>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="reader-topbar-right">
          <el-tag v-if="isAdmin" type="warning" effect="plain" size="small">管理员</el-tag>
          <el-tag effect="plain" size="small">{{ roleLabel }}</el-tag>
          <span class="reader-divider" aria-hidden="true"></span>
          <el-avatar :size="32" class="reader-avatar">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="reader-user-name">{{ displayName }}</span>
          <el-button text :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="reader-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.reader-shell {
  min-height: 100vh;
  background: #f4f6f8;
}

.reader-sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 28px 18px 22px;
  color: #dce5f2;
  background: #17233c;
  border-right: 3px solid #c3903f;
}

.reader-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 10px 28px;
  border-bottom: 1px solid rgba(220, 229, 242, 0.13);
}

.reader-seal {
  display: grid;
  width: 40px;
  height: 46px;
  flex: 0 0 40px;
  place-items: center;
  color: #fff;
  font: 600 23px/1 Georgia, serif;
  background: #3568a8;
  border-radius: 3px 3px 10px 3px;
}

.reader-brand div:last-child {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.reader-brand strong {
  color: #fff;
  font: 600 21px/1.1 "Songti SC", "SimSun", serif;
  letter-spacing: 0.08em;
}

.reader-brand span {
  color: #8fa2bd;
  font-size: 10px;
}

.reader-nav-caption {
  padding: 26px 14px 10px;
  margin: 0;
  color: #758ba9;
  font-size: 11px;
  letter-spacing: 0.18em;
}

.reader-menu.el-menu {
  border-right: 0;
  background: transparent;
}

.reader-menu .el-menu-item {
  height: 48px;
  margin: 4px 0;
  color: #adbed3;
  border-radius: 6px;
}

.reader-menu .el-menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

.reader-menu .el-menu-item.is-active {
  color: #fff;
  background: #3568a8;
}

.reader-sidebar-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: auto;
  padding: 16px 12px 4px;
  border-top: 1px solid rgba(220, 229, 242, 0.13);
}

.reader-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  margin-top: 5px;
  background: #74b99a;
  border-radius: 50%;
}

.reader-sidebar-note div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.reader-sidebar-note strong {
  color: #cbd7e7;
  font-size: 12px;
  font-weight: 500;
}

.reader-sidebar-note span:last-child {
  color: #758ba9;
  font-size: 11px;
  line-height: 1.6;
}

.reader-workspace {
  min-width: 0;
}

.reader-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 34px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #e2e7ed;
}

.reader-kicker {
  margin-bottom: 5px;
  color: #8793a5;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.15em;
}

.reader-topbar h2 {
  margin: 0;
  color: #17233c;
  font-size: 18px;
}

.reader-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6e7b8f;
  font-size: 13px;
}

.reader-divider {
  width: 1px;
  height: 22px;
  margin: 0 4px;
  background: #e2e7ed;
}

.reader-avatar {
  color: #3568a8;
  background: #eaf1f9;
}

.reader-main {
  width: 100%;
  max-width: 1460px;
  margin: 0 auto;
  padding: 32px;
}

@media (max-width: 900px) {
  .reader-sidebar {
    width: 78px !important;
    padding: 22px 10px;
  }

  .reader-brand {
    justify-content: center;
    padding-bottom: 24px;
  }

  .reader-brand div:last-child,
  .reader-nav-caption,
  .reader-sidebar-note,
  .reader-menu .el-menu-item span {
    display: none;
  }

  .reader-menu .el-menu-item {
    justify-content: center;
    padding: 0 !important;
  }

  .reader-main {
    padding: 24px 20px;
  }

  .reader-topbar {
    padding: 0 20px;
  }
}

@media (max-width: 600px) {
  .reader-user-name,
  .reader-divider {
    display: none;
  }

  .reader-main {
    padding: 20px 14px;
  }
}
</style>
