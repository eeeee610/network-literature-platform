<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">星阅</div>
      <p class="auth-subtitle">登录后继续你的阅读、订阅与打赏</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>
      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { setAuth } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: ''
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await login({ username: form.username.trim(), password: form.password })
    setAuth(data.token, data.user)
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/bookshelf'
    router.replace(redirect)
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.submit {
  width: 100%;
  margin-top: 8px;
}
</style>
