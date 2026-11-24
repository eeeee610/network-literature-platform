<script setup>
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, Message, Reading, User } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const mode = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({
  account: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
})

const loginRules = {
  account: [{ required: true, message: '请输入用户名、邮箱或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '3-50 位字母、数字或下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

function targetPath() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return redirect || auth.homePath()
}

async function submitLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await login(loginForm)
    auth.setSession(data)
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)
    await router.replace(targetPath())
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await register(registerForm)
    auth.setSession(data)
    ElMessage.success('注册成功，已自动登录')
    await router.replace('/shelf')
  } finally {
    loading.value = false
  }
}

async function changeMode(next) {
  await nextTick()
  if (next === 'login') {
    registerFormRef.value?.clearValidate()
  } else {
    loginFormRef.value?.clearValidate()
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-story">
      <div class="story-brand">
        <div class="story-seal">文</div>
        <span>文枢 · 网络文学创作与阅读平台</span>
      </div>
      <p class="story-kicker">READER &amp; TRADE</p>
      <h1>把喜欢的故事<br />放进自己的书架</h1>
      <p class="story-copy">
        注册后即可管理书架、订阅 VIP 章节、向作者打赏，并查看每一笔余额变动。
        订阅扣费与打赏入账由数据库事务和账户流水共同保障。
      </p>
      <div class="story-features">
        <div><span>01</span>书架与阅读进度</div>
        <div><span>02</span>章节订阅防重复扣费</div>
        <div><span>03</span>打赏作者与余额流水</div>
      </div>
      <div class="story-foot">
        <el-icon><Reading /></el-icon>
        <span>课程设计 · A 用户与交易模块</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="card-heading">
          <p class="card-kicker">MEMBER ACCESS</p>
          <h2>{{ mode === 'login' ? '登录读者账户' : '创建读者账户' }}</h2>
          <p>{{ mode === 'login' ? '登录后继续阅读和交易' : '注册后自动登录并进入书架' }}</p>
        </div>

        <el-tabs v-model="mode" class="auth-tabs" @tab-change="changeMode">
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" size="large">
              <el-form-item label="账号" prop="account">
                <el-input v-model="loginForm.account" placeholder="用户名 / 邮箱 / 手机号" autocomplete="username">
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  autocomplete="current-password"
                  @keyup.enter="submitLogin"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-button type="primary" class="submit-button" :loading="loading" @click="submitLogin">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" size="large">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" placeholder="3-50 位字母、数字或下划线" autocomplete="username">
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  show-password
                  placeholder="至少 8 位"
                  autocomplete="new-password"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <div class="form-row">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="registerForm.nickname" placeholder="默认同用户名" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="registerForm.phone" placeholder="选填" autocomplete="tel">
                    <template #prefix><el-icon><Iphone /></el-icon></template>
                  </el-input>
                </el-form-item>
              </div>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="registerForm.email" placeholder="选填" autocomplete="email">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-button type="primary" class="submit-button" :loading="loading" @click="submitRegister">
                注册并登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <p class="card-note">登录即代表你使用课程设计演示账户；请勿填写真实敏感密码。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(440px, 0.95fr);
  min-height: 100vh;
  background: #f3f5f8;
}

.login-story {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 9vw;
  overflow: hidden;
  color: #e7eef8;
  background:
    radial-gradient(circle at 88% 18%, rgba(195, 144, 63, 0.22), transparent 32%),
    linear-gradient(145deg, #101b30 0%, #17233c 58%, #1d3150 100%);
}

.login-story::after {
  position: absolute;
  right: -110px;
  bottom: -140px;
  width: 440px;
  height: 440px;
  content: "";
  border: 1px solid rgba(195, 144, 63, 0.28);
  border-radius: 50%;
  box-shadow: 0 0 0 54px rgba(195, 144, 63, 0.05), 0 0 0 108px rgba(195, 144, 63, 0.035);
}

.story-brand {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-bottom: 66px;
  color: #cbd7e7;
  font-size: 13px;
}

.story-seal {
  display: grid;
  width: 42px;
  height: 48px;
  place-items: center;
  color: #fff;
  font: 600 24px/1 "Songti SC", "SimSun", serif;
  background: #3568a8;
  border-radius: 3px 3px 11px 3px;
}

.story-kicker,
.card-kicker {
  margin-bottom: 14px;
  color: #c3903f;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.login-story h1 {
  max-width: 560px;
  margin-bottom: 24px;
  color: #fff;
  font: 600 clamp(34px, 4.2vw, 58px)/1.25 "Songti SC", "SimSun", serif;
  letter-spacing: 0.02em;
}

.story-copy {
  max-width: 520px;
  margin-bottom: 38px;
  color: #b4c4d9;
  font-size: 14px;
  line-height: 2;
}

.story-features {
  display: grid;
  max-width: 500px;
  gap: 13px;
}

.story-features div {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 13px;
  color: #dce5f2;
  font-size: 13px;
  border-bottom: 1px solid rgba(220, 229, 242, 0.1);
}

.story-features span {
  color: #c3903f;
  font: 600 12px/1 Georgia, serif;
}

.story-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 54px;
  color: #758ba9;
  font-size: 11px;
}

.login-panel {
  display: grid;
  padding: 42px;
  place-items: center;
}

.login-card {
  width: min(100%, 460px);
  padding: 36px 40px 28px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-top: 3px solid #3568a8;
  border-radius: 9px;
  box-shadow: 0 24px 70px rgba(23, 35, 60, 0.1);
}

.card-heading {
  margin-bottom: 18px;
}

.card-heading h2 {
  margin-bottom: 8px;
  color: #17233c;
  font: 600 28px/1.2 "Songti SC", "SimSun", serif;
}

.card-heading p:last-child {
  margin-bottom: 0;
  color: #8793a5;
  font-size: 12px;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.auth-tabs :deep(.el-tabs__item) {
  font-size: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit-button {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  font-size: 14px;
  letter-spacing: 0.08em;
}

.card-note {
  margin: 20px 0 0;
  color: #98a2b3;
  font-size: 10px;
  line-height: 1.7;
  text-align: center;
}

@media (max-width: 1000px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-story {
    min-height: 360px;
    padding: 44px 8vw;
  }

  .story-brand {
    margin-bottom: 34px;
  }

  .login-story h1 {
    font-size: 38px;
  }

  .story-copy,
  .story-features,
  .story-foot {
    display: none;
  }

  .login-panel {
    padding: 30px 18px 44px;
  }
}

@media (max-width: 520px) {
  .login-card {
    padding: 28px 22px 22px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .card-heading h2 {
    font-size: 24px;
  }
}
</style>
