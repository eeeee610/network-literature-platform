<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { isAdmin, login, register } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '', email: '', phone: '' })
const isRegister = computed(() => route.path === '/register')

const rules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    ...(isRegister.value
      ? [{ pattern: /^[A-Za-z0-9_]{3,50}$/, message: '请输入 3～50 位字母、数字或下划线', trigger: 'blur' }]
      : []),
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    ...(isRegister.value
      ? [{ min: 8, max: 72, message: '密码长度须为 8～72 位', trigger: 'blur' }]
      : []),
  ],
  nickname: isRegister.value
    ? [{ required: true, message: '请输入昵称', trigger: 'blur' }]
    : [],
}))

watch(isRegister, () => formRef.value?.clearValidate())

async function submit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const payload = { username: form.username.trim(), password: form.password }
    if (isRegister.value) {
      await register({
        ...payload,
        nickname: form.nickname.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
      })
    } else {
      await login(payload)
    }
    ElMessage.success(isRegister.value ? '注册成功，已为你登录' : '登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect.startsWith('/')) {
      router.replace(redirect)
    } else {
      router.replace(isAdmin() ? '/admin/dashboard' : '/account')
    }
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <section class="login-story">
      <div class="story-brand">
        <span class="story-seal" aria-hidden="true">文</span>
        <div>
          <strong>文枢书院</strong>
          <span>网络文学创作与阅读平台</span>
        </div>
      </div>
      <div class="story-copy">
        <p class="story-eyebrow">READ · RATE · RANK</p>
        <h1>以书评为楼层，<br />让每一次阅读留下回响。</h1>
        <p class="story-text">
          登录后可参与作品评论盖楼、五星评分与短评，运营人员可进入后台完成评论审核、
          短评审核与榜单期次生成。
        </p>
      </div>
      <ul class="story-points">
        <li><span>01</span>两级评论盖楼，支持点赞与回复</li>
        <li><span>02</span>一至五星评分，短评审核后公开展示</li>
        <li><span>03</span>收藏、评分、打赏、订阅四类榜单</li>
      </ul>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <p class="login-eyebrow">{{ isRegister ? 'JOIN US / 注册' : 'SIGN IN / 登录' }}</p>
        <h2>{{ isRegister ? '创建读者账号' : '进入文枢书院' }}</h2>
        <p class="login-hint">
          {{ isRegister ? '注册即创建个人书架和零钱账户，初始余额为 0。' : '使用平台账号登录，管理员账号将进入运营后台。' }}
        </p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item v-if="isRegister" label="昵称" prop="nickname">
            <el-input v-model="form.nickname" size="large" maxlength="50" placeholder="用于评论区与打赏记录" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              :prefix-icon="User"
              placeholder="请输入用户名"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="请输入密码"
              :autocomplete="isRegister ? 'new-password' : 'current-password'"
              @keyup.enter="submit"
            />
          </el-form-item>
          <template v-if="isRegister">
            <el-form-item label="邮箱（选填）" prop="email">
              <el-input v-model="form.email" size="large" type="email" placeholder="name@example.com" />
            </el-form-item>
            <el-form-item label="手机号（选填）" prop="phone">
              <el-input v-model="form.phone" size="large" maxlength="20" placeholder="用于账号资料识别" />
            </el-form-item>
          </template>
          <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="submit">
            {{ isRegister ? '注册并登录' : '登录' }}
          </el-button>
        </el-form>

        <div class="login-footer">
          <RouterLink :to="isRegister ? '/login' : '/register'">
            {{ isRegister ? '已有账号，去登录' : '新读者，创建账号' }}
          </RouterLink>
          <RouterLink to="/rankings">先逛逛榜单广场</RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  background: #f5f2ea;
}

.login-story {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 54px 64px;
  color: #dfe7f2;
  background:
    radial-gradient(circle at 12% 18%, rgba(195, 144, 63, 0.16), transparent 34%),
    radial-gradient(circle at 82% 76%, rgba(53, 104, 168, 0.3), transparent 42%),
    var(--ink);
}

.story-brand {
  display: flex;
  align-items: center;
  gap: 13px;
}

.story-seal {
  display: grid;
  width: 44px;
  height: 52px;
  place-items: center;
  color: #fff;
  font: 600 25px/1 "Songti SC", "SimSun", serif;
  background: var(--accent);
  border-radius: 3px 3px 12px 3px;
  box-shadow: inset 0 -3px 0 rgba(195, 144, 63, 0.9);
}

.story-brand div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.story-brand strong {
  color: #fff;
  font: 600 21px/1.1 "Songti SC", "SimSun", serif;
  letter-spacing: 0.14em;
}

.story-brand span {
  color: #8fa2bd;
  font-size: 11px;
}

.story-copy {
  max-width: 520px;
}

.story-eyebrow {
  margin-bottom: 18px;
  color: #c8a367;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.24em;
}

.story-copy h1 {
  margin-bottom: 22px;
  color: #fff;
  font: 600 38px/1.42 "Songti SC", "SimSun", serif;
  letter-spacing: 0.02em;
}

.story-text {
  margin: 0;
  color: #a9b9cf;
  font-size: 13px;
  line-height: 2;
}

.story-points {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.story-points li {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #b8c6d9;
  font-size: 12px;
}

.story-points span {
  color: #c8a367;
  font: 600 12px/1 Georgia, serif;
}

.login-panel {
  display: grid;
  place-items: center;
  padding: 40px 32px;
}

.login-card {
  width: 100%;
  max-width: 430px;
  padding: 38px 38px 30px;
  background: #fff;
  border: 1px solid #e6dfd2;
  border-top: 3px solid var(--accent);
  border-radius: 8px;
  box-shadow: 0 18px 50px rgba(23, 35, 60, 0.09);
}

.login-eyebrow {
  margin-bottom: 10px;
  color: #9b9284;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.login-card h2 {
  margin-bottom: 10px;
  color: var(--ink);
  font: 600 26px/1.3 "Songti SC", "SimSun", serif;
}

.login-hint {
  margin-bottom: 26px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

.login-submit {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 0.2em;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 24px;
  padding-top: 18px;
  color: #9b9284;
  font-size: 11px;
  border-top: 1px solid #eee8dc;
}

.login-footer a {
  color: var(--accent);
  text-decoration: none;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-story {
    gap: 34px;
    padding: 36px 28px;
  }

  .story-copy h1 {
    font-size: 29px;
  }
}

@media (max-width: 560px) {
  .login-card {
    padding: 28px 22px 24px;
  }

  .login-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
