<template>
  <el-container class="shell">
    <el-header class="header">
      <div class="header-inner">
        <div class="brand" @click="router.push('/bookshelf')">星阅</div>
        <el-menu
          class="nav"
          mode="horizontal"
          :default-active="activePath"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/bookshelf">我的书架</el-menu-item>
          <el-menu-item index="/subscription">章节订阅</el-menu-item>
          <el-menu-item index="/reward">作品打赏</el-menu-item>
          <el-menu-item index="/account">余额与流水</el-menu-item>
        </el-menu>
        <div class="user-area">
          <el-tag size="small" :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-name">{{ user.nickname || user.username }}<span class="caret">▾</span></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="account">余额与流水</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuth, getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const user = ref(getUser() || {})

const activePath = computed(() => route.path)
const roleLabel = computed(() => {
  const role = user.value.role
  if (role === 'ADMIN') return '管理员'
  if (role === 'AUTHOR') return '作者'
  return '读者'
})
const roleTagType = computed(() => {
  if (user.value.role === 'ADMIN') return 'danger'
  if (user.value.role === 'AUTHOR') return 'warning'
  return 'success'
})

function onCommand(command) {
  if (command === 'logout') {
    clearAuth()
    ElMessage.success('已退出登录')
    router.replace('/login')
    return
  }
  if (command === 'account') {
    router.push('/account')
  }
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
}

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0;
}

.header-inner {
  max-width: 1120px;
  height: 60px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--brand);
  cursor: pointer;
  white-space: nowrap;
}

.nav {
  flex: 1;
  border-bottom: none;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.user-name {
  cursor: pointer;
  font-size: 14px;
  color: var(--text-main);
  outline: none;
}

.caret {
  margin-left: 4px;
  color: var(--text-sub);
}

.main {
  padding: 0;
}
</style>
