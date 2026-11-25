<script setup>
import { provide, ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  clearNovelToken,
  getNovelToken,
  setNovelToken,
} from '@/api/novel'

const token = ref(getNovelToken())
const dialogVisible = ref(false)
const tokenInput = ref('')

function openTokenDialog() {
  tokenInput.value = token.value
  dialogVisible.value = true
}

function saveToken() {
  const value = tokenInput.value.trim()
  if (value) {
    setNovelToken(value)
    token.value = value
    ElMessage.success('令牌已保存')
  } else {
    clearNovelToken()
    token.value = ''
    ElMessage.info('已清除本地令牌')
  }
  dialogVisible.value = false
}

function clearInput() {
  tokenInput.value = ''
}

provide('novelToken', token)
provide('openNovelTokenDialog', openTokenDialog)
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
          <el-tag v-if="token" type="success" effect="plain" size="small">已配置令牌</el-tag>
          <el-tag v-else type="info" effect="plain" size="small">未配置令牌</el-tag>
          <el-button size="small" @click="openTokenDialog">令牌</el-button>
        </div>
      </div>
    </header>

    <main class="site-main">
      <RouterView />
    </main>

    <footer class="site-footer">
      <span>B 作品与内容模块 · 接口联调演示</span>
      <span>数据来自 /api/novel</span>
    </footer>

    <el-dialog v-model="dialogVisible" title="设置访问令牌" width="460px" append-to-body>
      <p class="token-hint">临时：联调时替换为 A 的登录</p>
      <el-input
        v-model="tokenInput"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="粘贴 A 模块登录后返回的 JWT"
      />
      <p class="token-note">
        令牌仅保存在本机 localStorage（键名 novel_token），用于作者接口的
        Authorization 请求头。
      </p>
      <template #footer>
        <el-button text @click="clearInput">清空输入</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveToken">保存</el-button>
      </template>
    </el-dialog>
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

.token-hint {
  margin-bottom: 12px;
  padding: 9px 12px;
  color: #8a611f;
  font-size: 12px;
  line-height: 1.6;
  background: #fdf6e8;
  border-left: 3px solid var(--gold);
  border-radius: 4px;
}

.token-note {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.7;
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
