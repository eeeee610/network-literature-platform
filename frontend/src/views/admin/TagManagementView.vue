<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { createTag, deleteTag, getAdminTags, updateTag } from '@/api/admin'

const filters = reactive({ tagName: '', isActive: '' })
const rows = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref('')
const saving = ref(false)
const operatingId = ref('')
const form = reactive({ tagName: '', isActive: 1 })

async function loadTags() {
  loading.value = true
  try {
    const data = await getAdminTags({
      tagName: filters.tagName.trim() || undefined,
      isActive: filters.isActive === '' ? undefined : filters.isActive,
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
  loadTags()
}

function reset() {
  filters.tagName = ''
  filters.isActive = ''
  search()
}

function changePage(value) {
  page.value = value
  loadTags()
}

function changePageSize(value) {
  pageSize.value = value
  page.value = 1
  loadTags()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = ''
  form.tagName = ''
  form.isActive = 1
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.tagId
  form.tagName = row.tagName
  form.isActive = row.isActive
  dialogVisible.value = true
}

async function submit() {
  const tagName = form.tagName.trim()
  if (!tagName) {
    ElMessage.warning('请填写标签名称')
    return
  }
  if (tagName.length > 32) {
    ElMessage.warning('标签名称不能超过 32 字')
    return
  }
  saving.value = true
  try {
    const payload = { tagName, isActive: form.isActive }
    if (dialogMode.value === 'create') {
      await createTag(payload)
      ElMessage.success('标签已创建')
    } else {
      await updateTag(editingId.value, payload)
      ElMessage.success('标签已更新')
    }
    dialogVisible.value = false
    await loadTags()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    saving.value = false
  }
}

async function toggleActive(row) {
  const next = row.isActive === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定${next === 1 ? '启用' : '停用'}标签“${row.tagName}”吗？`,
      '标签状态变更',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  operatingId.value = row.tagId
  try {
    await updateTag(row.tagId, { tagName: row.tagName, isActive: next })
    ElMessage.success(next === 1 ? '标签已启用' : '标签已停用')
    await loadTags()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    operatingId.value = ''
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `物理删除后不可恢复，确定删除标签“${row.tagName}”吗？`,
      '删除标签',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  operatingId.value = row.tagId
  try {
    await deleteTag(row.tagId)
    ElMessage.success('标签已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    await loadTags()
  } catch {
    // 已被作品引用等冲突由后端返回明确提示
  } finally {
    operatingId.value = ''
  }
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(loadTags)
</script>

<template>
  <div class="tag-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">CONTENT / 标签维护</p>
        <h1>作品标签管理</h1>
        <p class="page-description">新增、修改、查询和启停标签。已被作品引用的标签不可物理删除，可改为停用。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="RefreshRight" :loading="loading" @click="loadTags">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
      </div>
    </div>

    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="search">
        <el-form-item label="标签名称">
          <el-input v-model="filters.tagName" placeholder="按名称模糊查询" clearable style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="启停状态">
          <el-select v-model="filters.isActive" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="tagId" class="tag-table">
        <el-table-column prop="tagId" label="标签 ID" width="100" />
        <el-table-column prop="tagName" label="标签名称" min-width="200">
          <template #default="scope"><span class="tag-name">{{ scope.row.tagName }}</span></template>
        </el-table-column>
        <el-table-column prop="novelCount" label="关联作品" width="110" align="center">
          <template #default="scope">{{ scope.row.novelCount }} 部</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ scope.row.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :disabled="operatingId === scope.row.tagId" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link :type="scope.row.isActive === 1 ? 'warning' : 'success'" :disabled="operatingId === scope.row.tagId" @click="toggleActive(scope.row)">
              {{ scope.row.isActive === 1 ? '停用' : '启用' }}
            </el-button>
            <el-tooltip :disabled="scope.row.novelCount === 0" content="已被作品引用，请改为停用" placement="top">
              <span>
                <el-button link type="danger" :disabled="scope.row.novelCount > 0 || operatingId === scope.row.tagId" @click="remove(scope.row)">删除</el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" :page-sizes="[10, 20, 50]" @current-change="changePage" @size-change="changePageSize" />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增标签' : '编辑标签'" width="440px">
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="标签名称" required>
          <el-input v-model="form.tagName" maxlength="32" show-word-limit placeholder="最多 32 字" autofocus />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.heading-actions { display: flex; gap: 8px; }
.filter-card,
.table-card {
  margin-bottom: 20px;
  padding: 22px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}
.filter-card { border-top: 3px solid var(--gold); }
.filter-form { display: flex; flex-wrap: wrap; }
.filter-form :deep(.el-form-item) { margin-bottom: 4px; margin-right: 10px; }
.table-card { padding: 18px 20px 22px; }
.tag-table { width: 100%; }
.tag-name { color: var(--ink); font-size: 13px; font-weight: 600; }
.cell-sub { color: #8793a5; font-size: 11px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 18px; }
@media (max-width: 600px) {
  .filter-card, .table-card { padding: 16px 14px; }
  .pagination-bar { justify-content: center; }
}
</style>
