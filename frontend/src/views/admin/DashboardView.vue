<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, CircleCheck, RefreshRight } from '@element-plus/icons-vue'
import { getDashboardSummary } from '@/api/admin'

const loading = ref(true)
const error = ref('')
const summary = ref(null)
const numberFormatter = new Intl.NumberFormat('zh-CN')

function auditTagType(type) {
  return { 小说: 'primary', 章节: 'success', 评论: 'warning' }[type] || 'info'
}

async function loadSummary() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await getDashboardSummary()
  } catch {
    error.value = '暂时无法获取模拟统计，请确认后端服务已在 8080 端口启动。'
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
        <h1>今日运营速览</h1>
        <p class="page-description">集中查看内容审核与平台互动概况，为 A / B / C 模块开发保留统一协作入口。</p>
      </div>
      <el-tag effect="plain" type="warning">模拟数据</el-tag>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="dashboard-alert">
      <el-button size="small" :icon="RefreshRight" @click="loadSummary">重新加载</el-button>
    </el-alert>

    <div v-if="loading" class="dashboard-skeleton">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="summary">
      <section class="metric-grid" aria-label="运营统计">
        <article v-for="metric in summary.metrics" :key="metric.key" class="metric-card">
          <p>{{ metric.label }}</p>
          <div class="metric-value">
            <strong>{{ numberFormatter.format(metric.value) }}</strong>
            <span>{{ metric.unit }}</span>
          </div>
          <span class="metric-caption">DASHBOARD / MOCK</span>
        </article>
      </section>

      <section class="dashboard-grid">
        <div class="panel audit-panel">
          <div class="panel-heading">
            <div>
              <p class="panel-eyebrow">REVIEW QUEUE</p>
              <h2>待审核摘要</h2>
            </div>
            <span>最近 3 条</span>
          </div>
          <el-table :data="summary.pendingAudits" class="audit-table">
            <el-table-column prop="type" label="类型" width="82">
              <template #default="scope">
                <el-tag :type="auditTagType(scope.row.type)" effect="light" size="small">
                  {{ scope.row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="审核内容" min-width="210" show-overflow-tooltip />
            <el-table-column prop="author" label="提交人" width="100" />
            <el-table-column prop="submittedAt" label="提交时间" width="156" />
          </el-table>
          <RouterLink class="panel-link" to="/admin/audits">
            进入审核管理
            <el-icon><ArrowRight /></el-icon>
          </RouterLink>
        </div>

        <aside class="panel status-panel">
          <div class="panel-heading">
            <div>
              <p class="panel-eyebrow">FOUNDATION</p>
              <h2>框架状态</h2>
            </div>
          </div>
          <div class="api-status">
            <el-icon :size="20"><CircleCheck /></el-icon>
            <div>
              <strong>前后端通信正常</strong>
              <span>统一 JSON 响应已解析</span>
            </div>
          </div>
          <dl class="status-list">
            <div><dt>数据模式</dt><dd>{{ summary.dataSource }}</dd></div>
            <div><dt>接口地址</dt><dd>/api/admin/dashboard/summary</dd></div>
            <div><dt>业务范围</dt><dd>仅基础框架验证</dd></div>
          </dl>
          <p class="status-note">当前统计不代表真实数据库数据，后续由对应模块替换为正式接口。</p>
        </aside>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-alert {
  margin-bottom: 22px;
}

.dashboard-skeleton {
  padding: 34px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.metric-card {
  position: relative;
  padding: 23px 24px 20px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 7px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}

.metric-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  content: "";
  background: var(--accent);
}

.metric-card:nth-child(2)::before {
  background: #4f8876;
}

.metric-card:nth-child(3)::before {
  background: var(--gold);
}

.metric-card:nth-child(4)::before {
  background: #7b6b9e;
}

.metric-card p {
  margin-bottom: 13px;
  color: var(--muted);
  font-size: 13px;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}

.metric-value strong {
  color: var(--ink);
  font: 600 34px/1.1 Georgia, "Times New Roman", serif;
  letter-spacing: -0.02em;
}

.metric-value span {
  color: #8793a5;
  font-size: 12px;
}

.metric-caption,
.panel-eyebrow {
  color: #98a2b3;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.8fr);
  gap: 22px;
}

.panel {
  min-width: 0;
  padding: 25px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: 17px;
}

.panel-heading > span {
  color: #98a2b3;
  font-size: 11px;
}

.panel-eyebrow {
  margin-bottom: 6px;
}

.audit-table {
  width: 100%;
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 20px;
  color: var(--accent);
  font-size: 13px;
  text-decoration: none;
}

.api-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  color: #36765f;
  background: #edf6f2;
  border-radius: 6px;
}

.api-status div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.api-status strong {
  font-size: 13px;
}

.api-status span {
  color: #648577;
  font-size: 11px;
}

.status-list {
  margin: 20px 0;
}

.status-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  font-size: 11px;
  border-bottom: 1px solid #edf0f3;
}

.status-list dt {
  flex: 0 0 auto;
  color: #8793a5;
}

.status-list dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: #344054;
  text-align: right;
}

.status-note {
  margin-bottom: 0;
  color: #8793a5;
  font-size: 11px;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 20px 16px;
  }
}
</style>
