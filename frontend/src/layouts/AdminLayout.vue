<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  CollectionTag,
  DataAnalysis,
  DocumentChecked,
  Histogram,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const menus = [
  { path: '/admin/dashboard', label: '数据统计', icon: DataAnalysis },
  { path: '/admin/audits', label: '审核管理', icon: DocumentChecked },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/tags', label: '标签管理', icon: CollectionTag },
  { path: '/admin/rankings', label: '榜单配置', icon: Histogram },
]
const currentTitle = computed(() => route.meta.title || '运营后台')
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
          <strong>基础框架已就绪</strong>
          <span>当前使用模拟数据</span>
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
          <div class="user-badge" aria-hidden="true">D</div>
          <span class="user-name">模块 D</span>
        </div>
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
