<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { auditComment, changeCommentStatus, getAdminComments } from '@/api/interaction'

const filters = reactive({
  novelId: '',
  status: '',
  auditStatus: '',
  keyword: '',
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref([])
const loading = ref(false)

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejectSubmitting = ref(false)

const statusLabels = { 1: '正常', 2: '已隐藏', 3: '已删除' }
const statusTagTypes = { 1: 'success', 2: 'info', 3: 'danger' }
const auditLabels = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const auditTagTypes = { 0: 'warning', 1: 'success', 2: 'danger' }

async function loadComments() {
  loading.value = true
  try {
    const data = await getAdminComments({
      novelId: filters.novelId.trim() || undefined,
      status: filters.status || undefined,
      auditStatus: filters.auditStatus === '' ? undefined : filters.auditStatus,
      keyword: filters.keyword.trim() || undefined,
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

function handleSearch() {
  page.value = 1
  loadComments()
}

function handleReset() {
  filters.novelId = ''
  filters.status = ''
  filters.auditStatus = ''
  filters.keyword = ''
  page.value = 1
  loadComments()
}

function changePage(value) {
  page.value = value
  loadComments()
}

function changePageSize(value) {
  pageSize.value = value
  page.value = 1
  loadComments()
}

async function approve(row) {
  try {
    await ElMessageBox.confirm('审核通过后该评论将公开展示，确定通过吗？', '审核通过', {
      type: 'success',
      confirmButtonText: '通过',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await auditComment(row.commentId, { action: 1 })
    ElMessage.success('已审核通过')
    loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  }
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('驳回时必须填写审核原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await auditComment(rejectTarget.value.commentId, { action: 2, reason })
    ElMessage.success('已驳回该评论')
    rejectVisible.value = false
    loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    rejectSubmitting.value = false
  }
}

async function updateStatus(row, status, confirmText) {
  try {
    await ElMessageBox.confirm(confirmText, '状态变更', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await changeCommentStatus(row.commentId, status)
    ElMessage.success('状态已更新')
    loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  }
}

function formatTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(loadComments)
</script>

<template>
  <div class="comment-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">INTERACTION / 评论审核</p>
        <h1>评论审核与治理</h1>
        <p class="page-description">
          按作品、状态、审核状态与关键词检索评论，完成审核通过、驳回、隐藏、恢复与软删除。审核与日志写入在同一事务内提交。
        </p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading" @click="loadComments">刷新</el-button>
    </div>

    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="作品 ID">
          <el-input v-model="filters.novelId" placeholder="如 1001" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="评论状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 118px">
            <el-option label="正常" value="1" />
            <el-option label="已隐藏" value="2" />
            <el-option label="已删除" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="filters.auditStatus" placeholder="全部" clearable style="width: 118px">
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已驳回" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="评论内容" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="commentId" class="comment-table">
        <el-table-column prop="commentId" label="评论 ID" width="92" />
        <el-table-column label="作品 / 章节" min-width="150">
          <template #default="scope">
            <span class="cell-strong">{{ scope.row.novelTitle || scope.row.novelId }}</span>
            <span class="cell-sub">
              {{ scope.row.chapterId ? `章节 ${scope.row.chapterId}` : '书评' }}
              <template v-if="scope.row.parentId"> · 回复 #{{
                scope.row.parentId
              }}</template>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评论人" width="120">
          <template #default="scope">
            <span class="cell-strong">{{ scope.row.nickname || scope.row.userId }}</span>
            <span class="cell-sub">UID {{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="互动" width="100" align="center">
          <template #default="scope">
            <span class="cell-sub">赞 {{ scope.row.likeCount }} · 回 {{ scope.row.replyCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="statusTagTypes[scope.row.status] || 'info'" effect="light" size="small">
              {{ statusLabels[scope.row.status] || scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="92" align="center">
          <template #default="scope">
            <el-tag :type="auditTagTypes[scope.row.auditStatus] || 'info'" effect="light" size="small">
              {{ auditLabels[scope.row.auditStatus] || scope.row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发表时间" width="150">
          <template #default="scope">
            <span class="cell-sub">{{ formatTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="218" fixed="right">
          <template #default="scope">
            <div class="row-actions">
              <el-button
                v-if="scope.row.auditStatus !== 1"
                link
                type="success"
                @click="approve(scope.row)"
              >
                通过
              </el-button>
              <el-button
                v-if="scope.row.auditStatus !== 2"
                link
                type="danger"
                @click="openReject(scope.row)"
              >
                驳回
              </el-button>
              <el-button
                v-if="scope.row.status === 1"
                link
                type="warning"
                @click="updateStatus(scope.row, 2, '隐藏后该评论将不再公开展示，确定隐藏吗？')"
              >
                隐藏
              </el-button>
              <el-button
                v-if="scope.row.status === 2"
                link
                type="primary"
                @click="updateStatus(scope.row, 1, '恢复后该评论将重新公开展示，确定恢复吗？')"
              >
                恢复
              </el-button>
              <el-button
                v-if="scope.row.status !== 3"
                link
                type="danger"
                @click="updateStatus(scope.row, 3, '删除为软删除，评论将不再展示，确定删除吗？')"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          :page-sizes="[10, 20, 50]"
          @current-change="changePage"
          @size-change="changePageSize"
        />
      </div>
    </section>

    <el-dialog v-model="rejectVisible" title="驳回评论" width="460px">
      <p class="dialog-note">
        驳回评论 #{{ rejectTarget?.commentId }}，审核原因将写入审核日志。
      </p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="255"
        show-word-limit
        placeholder="请填写驳回原因（必填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
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

.filter-card {
  border-top: 3px solid var(--accent);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 6px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 6px;
  margin-right: 10px;
}

.table-card {
  padding: 18px 20px 22px;
}

.comment-table {
  width: 100%;
}

.cell-strong {
  display: block;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.cell-sub {
  display: block;
  color: #8793a5;
  font-size: 11px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 6px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.dialog-note {
  margin: 0 0 12px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

@media (max-width: 600px) {
  .filter-card,
  .table-card {
    padding: 16px 14px;
  }

  .pagination-bar {
    justify-content: center;
  }
}
</style>
