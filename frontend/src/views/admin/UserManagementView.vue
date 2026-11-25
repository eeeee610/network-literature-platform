<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { changeUserStatus, getAdminUsers } from '@/api/admin'
import { currentUser } from '@/api/auth'

const filters = reactive({ keyword: '', role: '', status: '' })
const rows = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const changingId = ref('')

const roleLabels = { READER: '读者', AUTHOR: '作者', ADMIN: '管理员' }
const roleTagTypes = { READER: 'info', AUTHOR: 'primary', ADMIN: 'warning' }
const statusLabels = { ACTIVE: '正常', LOCKED: '已锁定', DISABLED: '已停用' }
const statusTagTypes = { ACTIVE: 'success', LOCKED: 'warning', DISABLED: 'danger' }

async function loadUsers() {
  loading.value = true
  try {
    const data = await getAdminUsers({
      keyword: filters.keyword.trim() || undefined,
      role: filters.role || undefined,
      status: filters.status || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    rows.value = data.list
    total.value = data.total
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadUsers()
}

function reset() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  search()
}

function changePage(value) {
  page.value = value
  loadUsers()
}

function changePageSize(value) {
  pageSize.value = value
  page.value = 1
  loadUsers()
}

function isSelf(row) {
  return String(currentUser()?.userId || '') === String(row.userId)
}

async function updateStatus(row, status) {
  if (row.status === status) return
  try {
    await ElMessageBox.confirm(
      `确定将用户“${row.nickname || row.username}”设为${statusLabels[status]}吗？`,
      '账号状态变更',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  changingId.value = row.userId
  try {
    await changeUserStatus(row.userId, status)
    ElMessage.success('用户状态已更新')
    await loadUsers()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    changingId.value = ''
  }
}

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(loadUsers)
</script>

<template>
  <div class="user-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">USER / 用户治理</p>
        <h1>用户查询与状态管理</h1>
        <p class="page-description">检索平台账号并设置正常、锁定或停用状态；不修改角色、余额及交易数据。</p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading" @click="loadUsers">刷新</el-button>
    </div>

    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="search">
        <el-form-item label="用户关键词">
          <el-input v-model="filters.keyword" placeholder="用户名或昵称" clearable style="width: 190px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="全部" clearable style="width: 120px">
            <el-option label="读者" value="READER" />
            <el-option label="作者" value="AUTHOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已锁定" value="LOCKED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="userId" class="user-table">
        <el-table-column prop="userId" label="用户 ID" width="92" />
        <el-table-column label="用户" min-width="170">
          <template #default="scope">
            <span class="cell-strong">{{ scope.row.nickname }}</span>
            <span class="cell-sub">@{{ scope.row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="190">
          <template #default="scope">
            <span class="cell-text">{{ scope.row.email || '未填写邮箱' }}</span>
            <span class="cell-sub">{{ scope.row.phone || '未填写手机号' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="scope">
            <el-tag :type="roleTagTypes[scope.row.role] || 'info'" effect="plain" size="small">
              {{ roleLabels[scope.row.role] || scope.row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusTagTypes[scope.row.status] || 'info'" effect="light" size="small">
              {{ statusLabels[scope.row.status] || scope.row.status }}
            </el-tag>
            <span v-if="isSelf(scope.row)" class="cell-sub">当前账号</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="150">
          <template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.lastLoginAt) }}</span></template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="174" fixed="right">
          <template #default="scope">
            <span v-if="isSelf(scope.row)" class="cell-sub">不可锁定或停用本人</span>
            <div v-else class="row-actions">
              <el-button v-if="scope.row.status !== 'ACTIVE'" link type="success" :disabled="changingId === scope.row.userId" @click="updateStatus(scope.row, 'ACTIVE')">恢复</el-button>
              <el-button v-if="scope.row.status !== 'LOCKED'" link type="warning" :disabled="changingId === scope.row.userId" @click="updateStatus(scope.row, 'LOCKED')">锁定</el-button>
              <el-button v-if="scope.row.status !== 'DISABLED'" link type="danger" :disabled="changingId === scope.row.userId" @click="updateStatus(scope.row, 'DISABLED')">停用</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" :page-sizes="[10, 20, 50]" @current-change="changePage" @size-change="changePageSize" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.filter-card,
.table-card {
  margin-bottom: 20px;
  padding: 22px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}
.filter-card { border-top: 3px solid var(--accent); }
.filter-form { display: flex; flex-wrap: wrap; }
.filter-form :deep(.el-form-item) { margin-bottom: 4px; margin-right: 10px; }
.table-card { padding: 18px 20px 22px; }
.user-table { width: 100%; }
.cell-strong { display: block; color: var(--ink); font-size: 13px; font-weight: 600; }
.cell-text { display: block; color: #4b5565; font-size: 12px; }
.cell-sub { display: block; color: #8793a5; font-size: 11px; }
.row-actions { display: flex; flex-wrap: wrap; gap: 3px 7px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 18px; }
@media (max-width: 600px) {
  .filter-card, .table-card { padding: 16px 14px; }
  .pagination-bar { justify-content: center; }
}
</style>
