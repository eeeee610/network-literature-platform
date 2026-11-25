<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import {
  auditChapter,
  auditNovel,
  getAuditLogs,
  getChapterAuditDetail,
  getChapterAudits,
  getNovelAudits,
} from '@/api/admin'

const activeTab = ref('novel')
const loading = reactive({ novel: false, chapter: false, log: false })
const novelRows = ref([])
const chapterRows = ref([])
const logRows = ref([])
const novelPage = reactive({ page: 1, pageSize: 10, total: 0 })
const chapterPage = reactive({ page: 1, pageSize: 10, total: 0 })
const logPage = reactive({ page: 1, pageSize: 10, total: 0 })
const novelFilters = reactive({ auditStatus: '', keyword: '', author: '' })
const chapterFilters = reactive({ novelId: '', auditStatus: '', keyword: '' })
const logFilters = reactive({ targetType: '', targetId: '', adminId: '', range: [] })

const auditingId = ref('')
const rejectVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailType = ref('novel')
const novelDetail = ref(null)
const chapterDetail = ref(null)

const auditLabels = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const auditTagTypes = { 0: 'warning', 1: 'success', 2: 'danger' }
const novelStatusLabels = { 0: '连载中', 1: '已完结', 2: '已下架' }
const chapterStatusLabels = { 0: '未发布', 1: '已发布', 2: '已下架' }
const targetTypeLabels = { NOVEL: '小说', CHAPTER: '章节', COMMENT: '评论', RATING: '评分短评' }

async function loadNovels() {
  loading.novel = true
  try {
    const data = await getNovelAudits({
      auditStatus: novelFilters.auditStatus === '' ? undefined : novelFilters.auditStatus,
      keyword: novelFilters.keyword.trim() || undefined,
      author: novelFilters.author.trim() || undefined,
      page: novelPage.page,
      pageSize: novelPage.pageSize,
    })
    novelRows.value = data.list
    novelPage.total = data.total
  } catch {
    novelRows.value = []
    novelPage.total = 0
  } finally {
    loading.novel = false
  }
}

async function loadChapters() {
  loading.chapter = true
  try {
    const data = await getChapterAudits({
      novelId: chapterFilters.novelId.trim() || undefined,
      auditStatus: chapterFilters.auditStatus === '' ? undefined : chapterFilters.auditStatus,
      keyword: chapterFilters.keyword.trim() || undefined,
      page: chapterPage.page,
      pageSize: chapterPage.pageSize,
    })
    chapterRows.value = data.list
    chapterPage.total = data.total
  } catch {
    chapterRows.value = []
    chapterPage.total = 0
  } finally {
    loading.chapter = false
  }
}

async function loadLogs() {
  loading.log = true
  try {
    const data = await getAuditLogs({
      targetType: logFilters.targetType || undefined,
      targetId: logFilters.targetId.trim() || undefined,
      adminId: logFilters.adminId.trim() || undefined,
      startTime: logFilters.range?.[0] || undefined,
      endTime: logFilters.range?.[1] || undefined,
      page: logPage.page,
      pageSize: logPage.pageSize,
    })
    logRows.value = data.list
    logPage.total = data.total
  } catch {
    logRows.value = []
    logPage.total = 0
  } finally {
    loading.log = false
  }
}

function loadActive() {
  if (activeTab.value === 'novel') return loadNovels()
  if (activeTab.value === 'chapter') return loadChapters()
  return loadLogs()
}

function search(type) {
  const state = type === 'novel' ? novelPage : type === 'chapter' ? chapterPage : logPage
  state.page = 1
  if (type === 'novel') loadNovels()
  else if (type === 'chapter') loadChapters()
  else loadLogs()
}

function reset(type) {
  if (type === 'novel') {
    Object.assign(novelFilters, { auditStatus: '', keyword: '', author: '' })
  } else if (type === 'chapter') {
    Object.assign(chapterFilters, { novelId: '', auditStatus: '', keyword: '' })
  } else {
    Object.assign(logFilters, { targetType: '', targetId: '', adminId: '', range: [] })
  }
  search(type)
}

function changePage(type, value) {
  const state = type === 'novel' ? novelPage : type === 'chapter' ? chapterPage : logPage
  state.page = value
  if (type === 'novel') loadNovels()
  else if (type === 'chapter') loadChapters()
  else loadLogs()
}

function changePageSize(type, value) {
  const state = type === 'novel' ? novelPage : type === 'chapter' ? chapterPage : logPage
  state.pageSize = value
  search(type)
}

async function approve(type, row) {
  const id = type === 'novel' ? row.novelId : row.chapterId
  try {
    await ElMessageBox.confirm(
      type === 'novel'
        ? '通过仅更新作品审核状态，不改变连载或下架状态。确定通过吗？'
        : '通过仅更新章节审核状态，不会自动发布章节。确定通过吗？',
      '审核通过',
      { type: 'success', confirmButtonText: '通过', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  auditingId.value = `${type}-${id}`
  try {
    if (type === 'novel') await auditNovel(id, { action: 1 })
    else await auditChapter(id, { action: 1 })
    ElMessage.success('已审核通过')
    if (type === 'novel') await loadNovels()
    else await loadChapters()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    auditingId.value = ''
  }
}

function openReject(type, row) {
  rejectTarget.value = { type, row }
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('驳回时必须填写审核原因')
    return
  }
  const { type, row } = rejectTarget.value
  const id = type === 'novel' ? row.novelId : row.chapterId
  rejectSubmitting.value = true
  try {
    if (type === 'novel') await auditNovel(id, { action: 2, reason })
    else await auditChapter(id, { action: 2, reason })
    ElMessage.success('已驳回并记录审核日志')
    rejectVisible.value = false
    if (type === 'novel') await loadNovels()
    else await loadChapters()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    rejectSubmitting.value = false
  }
}

function openNovelDetail(row) {
  detailType.value = 'novel'
  novelDetail.value = row
  chapterDetail.value = null
  detailVisible.value = true
}

async function openChapterDetail(row) {
  detailType.value = 'chapter'
  novelDetail.value = null
  chapterDetail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    chapterDetail.value = await getChapterAuditDetail(row.chapterId)
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(loadNovels)
</script>

<template>
  <div class="audit-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">AUDIT / 内容审核</p>
        <h1>内容审核与审核记录</h1>
        <p class="page-description">处理小说、章节审核，统一追溯四类审核日志。状态更新与日志写入在同一事务内提交。</p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading[activeTab]" @click="loadActive">刷新</el-button>
    </div>

    <div class="quick-links">
      <span>C 模块审核入口</span>
      <RouterLink to="/admin/comments">评论审核</RouterLink>
      <RouterLink to="/admin/ratings">评分短评审核</RouterLink>
    </div>

    <el-tabs v-model="activeTab" class="audit-tabs" @tab-change="loadActive">
      <el-tab-pane label="小说审核" name="novel">
        <section class="filter-card">
          <el-form :inline="true" class="filter-form" @submit.prevent="search('novel')">
            <el-form-item label="审核状态">
              <el-select v-model="novelFilters.auditStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="待审核" value="0" /><el-option label="已通过" value="1" /><el-option label="已驳回" value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="作品关键词">
              <el-input v-model="novelFilters.keyword" placeholder="作品名称" clearable style="width: 170px" />
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="novelFilters.author" placeholder="昵称或用户名" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="search('novel')">查询</el-button>
              <el-button @click="reset('novel')">重置</el-button>
            </el-form-item>
          </el-form>
        </section>
        <section class="table-card">
          <el-table v-loading="loading.novel" :data="novelRows" row-key="novelId" class="audit-table">
            <el-table-column prop="novelId" label="作品 ID" width="90" />
            <el-table-column label="作品" min-width="200">
              <template #default="scope">
                <span class="cell-strong">{{ scope.row.title }}</span>
                <span class="cell-sub">{{ scope.row.description || '暂无简介' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作者" width="130">
              <template #default="scope"><span class="cell-strong">{{ scope.row.authorName }}</span><span class="cell-sub">UID {{ scope.row.authorId }}</span></template>
            </el-table-column>
            <el-table-column prop="totalWords" label="总字数" width="90" align="right" />
            <el-table-column label="连载状态" width="92" align="center">
              <template #default="scope">{{ novelStatusLabels[scope.row.status] || scope.row.status }}</template>
            </el-table-column>
            <el-table-column label="审核状态" width="96" align="center">
              <template #default="scope"><el-tag :type="auditTagTypes[scope.row.auditStatus] || 'info'" effect="light" size="small">{{ auditLabels[scope.row.auditStatus] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="更新时间" width="150">
              <template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.updatedAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openNovelDetail(scope.row)">详情</el-button>
                <el-button v-if="scope.row.auditStatus !== 1" link type="success" :disabled="auditingId === `novel-${scope.row.novelId}`" @click="approve('novel', scope.row)">通过</el-button>
                <el-button v-if="scope.row.auditStatus !== 2" link type="danger" :disabled="auditingId === `novel-${scope.row.novelId}`" @click="openReject('novel', scope.row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar"><el-pagination background layout="total, sizes, prev, pager, next" :total="novelPage.total" :page-size="novelPage.pageSize" :current-page="novelPage.page" :page-sizes="[10, 20, 50]" @current-change="changePage('novel', $event)" @size-change="changePageSize('novel', $event)" /></div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="章节审核" name="chapter">
        <section class="filter-card chapter-filter">
          <el-form :inline="true" class="filter-form" @submit.prevent="search('chapter')">
            <el-form-item label="作品 ID"><el-input v-model="chapterFilters.novelId" placeholder="如 1001" clearable style="width: 130px" /></el-form-item>
            <el-form-item label="审核状态">
              <el-select v-model="chapterFilters.auditStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="待审核" value="0" /><el-option label="已通过" value="1" /><el-option label="已驳回" value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="章节关键词"><el-input v-model="chapterFilters.keyword" placeholder="章节标题" clearable style="width: 180px" /></el-form-item>
            <el-form-item><el-button type="primary" :icon="Search" @click="search('chapter')">查询</el-button><el-button @click="reset('chapter')">重置</el-button></el-form-item>
          </el-form>
        </section>
        <section class="table-card">
          <el-table v-loading="loading.chapter" :data="chapterRows" row-key="chapterId" class="audit-table">
            <el-table-column prop="chapterId" label="章节 ID" width="90" />
            <el-table-column label="所属作品 / 章节" min-width="240">
              <template #default="scope"><span class="cell-strong">{{ scope.row.novelTitle }}</span><span class="cell-sub">第 {{ scope.row.chapterNo }} 章 · {{ scope.row.title }}</span></template>
            </el-table-column>
            <el-table-column prop="authorName" label="作者" width="120" />
            <el-table-column prop="wordCount" label="字数" width="85" align="right" />
            <el-table-column label="类型" width="75" align="center"><template #default="scope"><el-tag :type="scope.row.isVip === 1 ? 'warning' : 'info'" effect="plain" size="small">{{ scope.row.isVip === 1 ? 'VIP' : '免费' }}</el-tag></template></el-table-column>
            <el-table-column label="发布状态" width="90" align="center"><template #default="scope">{{ chapterStatusLabels[scope.row.status] || scope.row.status }}</template></el-table-column>
            <el-table-column label="审核状态" width="96" align="center">
              <template #default="scope"><el-tag v-if="scope.row.auditStatus !== null" :type="auditTagTypes[scope.row.auditStatus] || 'info'" effect="light" size="small">{{ auditLabels[scope.row.auditStatus] }}</el-tag><span v-else class="cell-sub">未提交</span></template>
            </el-table-column>
            <el-table-column label="更新时间" width="150"><template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.updatedAt) }}</span></template></el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openChapterDetail(scope.row)">正文</el-button>
                <el-button v-if="scope.row.auditStatus !== 1" link type="success" :disabled="auditingId === `chapter-${scope.row.chapterId}`" @click="approve('chapter', scope.row)">通过</el-button>
                <el-button v-if="scope.row.auditStatus !== 2" link type="danger" :disabled="auditingId === `chapter-${scope.row.chapterId}`" @click="openReject('chapter', scope.row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar"><el-pagination background layout="total, sizes, prev, pager, next" :total="chapterPage.total" :page-size="chapterPage.pageSize" :current-page="chapterPage.page" :page-sizes="[10, 20, 50]" @current-change="changePage('chapter', $event)" @size-change="changePageSize('chapter', $event)" /></div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="审核记录" name="log">
        <section class="filter-card log-filter">
          <el-form :inline="true" class="filter-form" @submit.prevent="search('log')">
            <el-form-item label="目标类型">
              <el-select v-model="logFilters.targetType" placeholder="全部" clearable style="width: 130px">
                <el-option label="小说" value="NOVEL" /><el-option label="章节" value="CHAPTER" /><el-option label="评论" value="COMMENT" /><el-option label="评分短评" value="RATING" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标 ID"><el-input v-model="logFilters.targetId" clearable style="width: 120px" /></el-form-item>
            <el-form-item label="管理员 ID"><el-input v-model="logFilters.adminId" clearable style="width: 120px" /></el-form-item>
            <el-form-item label="审核时间">
              <el-date-picker v-model="logFilters.range" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 330px" />
            </el-form-item>
            <el-form-item><el-button type="primary" :icon="Search" @click="search('log')">查询</el-button><el-button @click="reset('log')">重置</el-button></el-form-item>
          </el-form>
        </section>
        <section class="table-card">
          <el-table v-loading="loading.log" :data="logRows" row-key="auditId" class="audit-table">
            <el-table-column prop="auditId" label="日志 ID" width="90" />
            <el-table-column label="目标" min-width="150"><template #default="scope"><span class="cell-strong">{{ targetTypeLabels[scope.row.targetType] || scope.row.targetType }}</span><span class="cell-sub">ID {{ scope.row.targetId }}</span></template></el-table-column>
            <el-table-column label="管理员" width="140"><template #default="scope"><span class="cell-strong">{{ scope.row.adminName }}</span><span class="cell-sub">UID {{ scope.row.adminId }}</span></template></el-table-column>
            <el-table-column label="操作" width="90" align="center"><template #default="scope"><el-tag :type="scope.row.auditAction === 1 ? 'success' : 'danger'" effect="light" size="small">{{ scope.row.auditAction === 1 ? '通过' : '驳回' }}</el-tag></template></el-table-column>
            <el-table-column label="审核原因" min-width="220" show-overflow-tooltip><template #default="scope">{{ scope.row.auditReason || '—' }}</template></el-table-column>
            <el-table-column label="审核时间" width="170"><template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.createdAt) }}</span></template></el-table-column>
          </el-table>
          <div class="pagination-bar"><el-pagination background layout="total, sizes, prev, pager, next" :total="logPage.total" :page-size="logPage.pageSize" :current-page="logPage.page" :page-sizes="[10, 20, 50]" @current-change="changePage('log', $event)" @size-change="changePageSize('log', $event)" /></div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectVisible" :title="rejectTarget?.type === 'novel' ? '驳回作品' : '驳回章节'" width="460px">
      <p class="dialog-note">驳回原因将写入审核日志，且不能为空。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请填写驳回原因（必填）" />
      <template #footer><el-button @click="rejectVisible = false">取消</el-button><el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="detailType === 'novel' ? '作品审核详情' : '章节正文审核'" width="720px" top="6vh">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailType === 'novel' && novelDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作品 ID">{{ novelDetail.novelId }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ novelDetail.authorName }}（{{ novelDetail.authorId }}）</el-descriptions-item>
            <el-descriptions-item label="作品名称" :span="2">{{ novelDetail.title }}</el-descriptions-item>
            <el-descriptions-item label="审核状态"><el-tag :type="auditTagTypes[novelDetail.auditStatus]" size="small">{{ auditLabels[novelDetail.auditStatus] }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="连载状态">{{ novelStatusLabels[novelDetail.status] }}</el-descriptions-item>
          </el-descriptions>
          <h3 class="detail-section-title">作品简介</h3>
          <p class="novel-description">{{ novelDetail.description || '暂无简介' }}</p>
        </template>
        <template v-if="detailType === 'chapter' && chapterDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="所属作品">{{ chapterDetail.chapter.novelTitle }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ chapterDetail.chapter.authorName }}</el-descriptions-item>
            <el-descriptions-item label="章节" :span="2">第 {{ chapterDetail.chapter.chapterNo }} 章 · {{ chapterDetail.chapter.title }}</el-descriptions-item>
            <el-descriptions-item label="字数">{{ chapterDetail.chapter.wordCount }}</el-descriptions-item>
            <el-descriptions-item label="发布状态">{{ chapterStatusLabels[chapterDetail.chapter.status] }}</el-descriptions-item>
          </el-descriptions>
          <h3 class="detail-section-title">章节正文</h3>
          <article class="chapter-content">{{ chapterDetail.content }}</article>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.quick-links {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 13px 18px;
  color: #8793a5;
  background: #f6f8fb;
  border: 1px solid var(--line);
  border-radius: 7px;
  font-size: 12px;
}
.quick-links a { color: var(--accent); font-weight: 600; text-decoration: none; }
.audit-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.filter-card,
.table-card {
  margin-bottom: 18px;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}
.filter-card { border-top: 3px solid var(--accent); }
.chapter-filter { border-top-color: #4f8876; }
.log-filter { border-top-color: var(--gold); }
.filter-form { display: flex; flex-wrap: wrap; }
.filter-form :deep(.el-form-item) { margin-bottom: 4px; margin-right: 10px; }
.table-card { padding: 16px 18px 20px; }
.audit-table { width: 100%; }
.cell-strong { display: block; color: var(--ink); font-size: 13px; font-weight: 600; }
.cell-sub { display: block; overflow: hidden; color: #8793a5; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 18px; }
.dialog-note { margin: 0 0 12px; color: var(--muted); font-size: 12px; }
.detail-body { min-height: 120px; }
.detail-section-title { margin: 22px 0 10px; color: var(--ink); font-size: 14px; }
.novel-description { margin: 0; color: #4b5565; font-size: 13px; line-height: 1.9; white-space: pre-wrap; }
.chapter-content { max-height: 46vh; padding: 18px; overflow: auto; color: #303847; background: #f8f7f4; border: 1px solid #ece9e2; border-radius: 5px; font: 14px/2 Georgia, 'Times New Roman', serif; white-space: pre-wrap; }
@media (max-width: 700px) {
  .filter-card, .table-card { padding: 15px 13px; }
  .pagination-bar { justify-content: center; }
  .quick-links { flex-wrap: wrap; }
}
</style>
