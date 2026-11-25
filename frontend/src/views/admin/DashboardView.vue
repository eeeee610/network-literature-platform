<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { getDashboardSummary } from '@/api/admin'

const loading = ref(true)
const error = ref('')
const summary = ref(null)
const numberFormatter = new Intl.NumberFormat('zh-CN')
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
})

const typeLabels = { NOVEL: '小说', CHAPTER: '章节', COMMENT: '评论', RATING: '评分短评' }
const typeTagTypes = { NOVEL: 'primary', CHAPTER: 'success', COMMENT: 'warning', RATING: 'danger' }

const metricCards = computed(() => {
  if (!summary.value) return []
  return [
    { key: 'users', label: '注册用户', value: numberFormatter.format(summary.value.userCount), unit: '人', note: '平台账号总量' },
    { key: 'novels', label: '作品数量', value: numberFormatter.format(summary.value.novelCount), unit: '部', note: '全部在库作品' },
    { key: 'comments', label: '评论数量', value: numberFormatter.format(summary.value.commentCount), unit: '条', note: '评论与回复总量' },
    {
      key: 'subscriptions', label: '有效订阅', value: numberFormatter.format(summary.value.subscriptionCount), unit: '笔',
      note: `实收 ${currencyFormatter.format(summary.value.subscriptionAmount)}`,
    },
    {
      key: 'tips', label: '成功打赏', value: numberFormatter.format(summary.value.tipCount), unit: '笔',
      note: `打赏 ${currencyFormatter.format(summary.value.tipAmount)}`,
    },
  ]
})

const pendingCards = computed(() => {
  if (!summary.value) return []
  const counts = summary.value.pendingCounts
  return [
    { type: 'NOVEL', count: counts.novel, route: '/admin/audits' },
    { type: 'CHAPTER', count: counts.chapter, route: '/admin/audits' },
    { type: 'COMMENT', count: counts.comment, route: '/admin/comments' },
    { type: 'RATING', count: counts.rating, route: '/admin/ratings' },
  ]
})

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadSummary() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await getDashboardSummary()
  } catch {
    error.value = '暂时无法获取运营统计，请确认后端服务及数据库连接正常。'
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<template>
  <div class="dashboard-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">OVERVIEW / 运营总览</p>
        <h1>运营数据与审核队列</h1>
        <p class="page-description">汇总用户、内容、互动与交易数据，集中查看四类待审核内容。</p>
      </div>
      <div class="heading-actions">
        <el-tag effect="plain" type="success">数据库实时统计</el-tag>
        <el-button :icon="RefreshRight" :loading="loading" @click="loadSummary">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="dashboard-alert" />

    <div v-if="loading" class="dashboard-skeleton">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="summary">
      <section class="metric-grid" aria-label="运营统计">
        <article v-for="metric in metricCards" :key="metric.key" class="metric-card">
          <p>{{ metric.label }}</p>
          <div class="metric-value">
            <strong>{{ metric.value }}</strong>
            <span>{{ metric.unit }}</span>
          </div>
          <span class="metric-caption">{{ metric.note }}</span>
        </article>
      </section>

      <section class="pending-grid" aria-label="待审核数量">
        <RouterLink v-for="item in pendingCards" :key="item.type" :to="item.route" class="pending-card">
          <span>
            <el-tag :type="typeTagTypes[item.type]" effect="light" size="small">{{ typeLabels[item.type] }}</el-tag>
            待审核
          </span>
          <strong>{{ numberFormatter.format(item.count) }}</strong>
        </RouterLink>
      </section>

      <section class="panel audit-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-eyebrow">REVIEW QUEUE</p>
            <h2>最近待审核内容</h2>
          </div>
          <span>最多显示 8 条</span>
        </div>
        <el-table :data="summary.recentPendingAudits" class="audit-table" empty-text="当前没有待审核内容">
          <el-table-column label="类型" width="100">
            <template #default="scope">
              <el-tag :type="typeTagTypes[scope.row.type] || 'info'" effect="light" size="small">
                {{ typeLabels[scope.row.type] || scope.row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="审核内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="author" label="提交人" width="130" />
          <el-table-column label="提交 / 更新时间" width="170">
            <template #default="scope"><span class="cell-sub">{{ formatTime(scope.row.submittedAt) }}</span></template>
          </el-table-column>
          <el-table-column prop="id" label="目标 ID" width="100" />
        </el-table>
        <RouterLink class="panel-link" to="/admin/audits">
          进入审核管理
          <el-icon><ArrowRight /></el-icon>
        </RouterLink>
      </section>

      <p class="data-note">数据来源：{{ summary.dataSource }}。订阅仅统计 ACTIVE 记录，打赏仅统计 SUCCESS 记录；金额为对应记录合计。</p>
    </template>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dashboard-alert {
  margin-bottom: 22px;
}

.dashboard-skeleton,
.panel {
  padding: 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card {
  position: relative;
  min-width: 0;
  padding: 22px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 7px;
}

.metric-card::before {
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  content: '';
  background: var(--accent);
}

.metric-card:nth-child(2)::before { background: #4f8876; }
.metric-card:nth-child(3)::before { background: var(--gold); }
.metric-card:nth-child(4)::before { background: #7b6b9e; }
.metric-card:nth-child(5)::before { background: #a46752; }

.metric-card p {
  margin-bottom: 13px;
  color: var(--muted);
  font-size: 13px;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-bottom: 14px;
}

.metric-value strong {
  overflow: hidden;
  color: var(--ink);
  font: 600 31px/1.1 Georgia, 'Times New Roman', serif;
  text-overflow: ellipsis;
}

.metric-value span,
.metric-caption,
.cell-sub {
  color: #8793a5;
  font-size: 11px;
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.pending-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  color: var(--muted);
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: 7px;
  text-decoration: none;
}

.pending-card:hover { border-color: var(--accent); }
.pending-card span { font-size: 12px; }
.pending-card strong { color: var(--ink); font-size: 24px; }

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.panel-heading h2 { margin: 0; color: var(--ink); font-size: 17px; }
.panel-heading > span { color: #98a2b3; font-size: 11px; }
.panel-eyebrow { margin-bottom: 6px; color: #98a2b3; font-size: 9px; letter-spacing: 0.14em; }
.audit-table { width: 100%; }

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 20px;
  color: var(--accent);
  font-size: 13px;
  text-decoration: none;
}

.data-note {
  margin: 16px 2px 0;
  color: #8793a5;
  font-size: 11px;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 800px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .pending-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .heading-actions { align-items: flex-end; flex-direction: column; }
  .metric-grid { grid-template-columns: 1fr; }
  .dashboard-skeleton, .panel { padding: 20px 16px; }
}
</style>
