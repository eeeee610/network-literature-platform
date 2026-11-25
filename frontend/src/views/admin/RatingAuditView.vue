<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { auditReview, getAdminReviews } from '@/api/interaction'

const filters = reactive({ reviewAuditStatus: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref([])
const loading = ref(false)

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejectSubmitting = ref(false)

const auditLabels = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const auditTagTypes = { 0: 'warning', 1: 'success', 2: 'danger' }
const statusLabels = { 1: '正常', 2: '隐藏', 3: '删除' }

async function loadReviews() {
  loading.value = true
  try {
    const data = await getAdminReviews({
      reviewAuditStatus: filters.reviewAuditStatus === '' ? undefined : filters.reviewAuditStatus,
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
  loadReviews()
}

function handleReset() {
  filters.reviewAuditStatus = ''
  page.value = 1
  loadReviews()
}

function changePage(value) {
  page.value = value
  loadReviews()
}

function changePageSize(value) {
  pageSize.value = value
  page.value = 1
  loadReviews()
}

async function approve(row) {
  try {
    await ElMessageBox.confirm('审核通过后该短评将公开展示，确定通过吗？', '审核通过', {
      type: 'success',
      confirmButtonText: '通过',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await auditReview(row.ratingId, { action: 1 })
    ElMessage.success('已审核通过')
    loadReviews()
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
    await auditReview(rejectTarget.value.ratingId, { action: 2, reason })
    ElMessage.success('已驳回该短评')
    rejectVisible.value = false
    loadReviews()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    rejectSubmitting.value = false
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

onMounted(loadReviews)
</script>

<template>
  <div class="rating-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">INTERACTION / 短评审核</p>
        <h1>评分短评审核</h1>
        <p class="page-description">
          审核读者在评分时填写的短评。修改短评会自动重置为待审核；评分统计不受短评审核影响。
        </p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading" @click="loadReviews">刷新</el-button>
    </div>

    <section class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="审核状态">
          <el-select v-model="filters.reviewAuditStatus" placeholder="全部" clearable style="width: 130px">
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已驳回" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="ratingId" class="review-table">
        <el-table-column prop="ratingId" label="评分 ID" width="92" />
        <el-table-column label="作品" min-width="160">
          <template #default="scope">
            <RouterLink class="novel-link" :to="`/novels/${scope.row.novelId}`">
              {{ scope.row.novelTitle || scope.row.novelId }}
            </RouterLink>
            <span class="cell-sub">ID {{ scope.row.novelId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分人" width="130">
          <template #default="scope">
            <span class="cell-strong">{{ scope.row.nickname || scope.row.userId }}</span>
            <span class="cell-sub">UID {{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150" align="center">
          <template #default="scope">
            <el-rate :model-value="scope.row.score" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="reviewContent" label="短评内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="记录状态" width="92" align="center">
          <template #default="scope">
            <el-tag type="info" effect="plain" size="small">
              {{ statusLabels[scope.row.status] || scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="短评审核" width="100" align="center">
          <template #default="scope">
            <el-tag
              v-if="scope.row.reviewAuditStatus !== null && scope.row.reviewAuditStatus !== undefined"
              :type="auditTagTypes[scope.row.reviewAuditStatus] || 'info'"
              effect="light"
              size="small"
            >
              {{ auditLabels[scope.row.reviewAuditStatus] || scope.row.reviewAuditStatus }}
            </el-tag>
            <span v-else class="cell-sub">无短评</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="scope">
            <span class="cell-sub">{{ formatTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.reviewAuditStatus !== 1"
              link
              type="success"
              @click="approve(scope.row)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.reviewAuditStatus !== 2"
              link
              type="danger"
              @click="openReject(scope.row)"
            >
              驳回
            </el-button>
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

    <el-dialog v-model="rejectVisible" title="驳回短评" width="460px">
      <p class="dialog-note">
        驳回评分 #{{ rejectTarget?.ratingId }} 的短评，审核原因将写入审核日志。
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
  border-top: 3px solid var(--gold);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 4px;
  margin-right: 10px;
}

.table-card {
  padding: 18px 20px 22px;
}

.review-table {
  width: 100%;
}

.novel-link {
  display: block;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.novel-link:hover {
  color: var(--accent);
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
