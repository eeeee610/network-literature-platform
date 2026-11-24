<script setup>
import { computed, onMounted, ref } from 'vue'
import { RefreshRight, Trophy } from '@element-plus/icons-vue'
import { isAdmin } from '@/api/auth'
import { getRankingDetail, getRankings, getRankingPeriods } from '@/api/interaction'

const rankings = ref([])
const activeCode = ref('')
const periods = ref([])
const activePeriod = ref('')
const detail = ref(null)
const loadingList = ref(true)
const loadingDetail = ref(false)
const error = ref('')

const activeConfig = computed(
  () => rankings.value.find((item) => item.rankingCode === activeCode.value) || null,
)
const items = computed(() => detail.value?.items || [])
const hasPeriods = computed(() => periods.value.length > 0)

const typeLabels = { FAVORITE: '收藏榜', RATING: '评分榜', TICKET: '打赏榜', SALE: '订阅榜' }
const periodLabels = { DAILY: '日榜', WEEKLY: '周榜', MONTHLY: '月榜', ALL: '总榜' }
const metricLabels = { favorite: '收藏', rating: '评分', ticket: '打赏', sale: '订阅' }

async function loadRankings() {
  loadingList.value = true
  error.value = ''
  try {
    rankings.value = await getRankings()
    if (rankings.value.length > 0) {
      await selectRanking(rankings.value[0].rankingCode)
    }
  } catch {
    error.value = '暂时无法获取榜单，请确认后端服务已启动并完成 C 模块建表。'
  } finally {
    loadingList.value = false
  }
}

async function selectRanking(code) {
  activeCode.value = code
  activePeriod.value = ''
  detail.value = null
  periods.value = []
  loadingDetail.value = true
  try {
    periods.value = await getRankingPeriods(code, { limit: 20 })
    if (periods.value.length > 0) {
      activePeriod.value = periods.value[0].periodNo
      await loadDetail()
    }
  } catch {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function loadDetail() {
  if (!activeCode.value) {
    return
  }
  loadingDetail.value = true
  try {
    detail.value = await getRankingDetail(activeCode.value, {
      periodNo: activePeriod.value || undefined,
      limit: 100,
    })
  } catch {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

function changePeriod(periodNo) {
  activePeriod.value = periodNo
  loadDetail()
}

function typeLabel(type) {
  return typeLabels[type] || type
}

function periodLabel(type) {
  return periodLabels[type] || type
}

function metricEntries(item) {
  return Object.entries(item.metricSnapshot || {}).map(([key, value]) => ({
    key,
    label: metricLabels[key] || key,
    value,
  }))
}

function rankClass(rankNo) {
  if (rankNo === 1) {
    return 'rank-badge rank-first'
  }
  if (rankNo === 2) {
    return 'rank-badge rank-second'
  }
  if (rankNo === 3) {
    return 'rank-badge rank-third'
  }
  return 'rank-badge'
}

function formatScore(score) {
  const number = Number(score)
  if (Number.isNaN(number)) {
    return '-'
  }
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

onMounted(loadRankings)
</script>

<template>
  <div class="ranking-page">
    <header class="page-heading">
      <div>
        <p class="page-eyebrow">RANKINGS / 榜单广场</p>
        <h1>本期榜单</h1>
        <p class="page-description">
          榜单由 C 模块公共过程生成，按收藏、评分、打赏、订阅四类指标取数，并保留每期指标快照。
        </p>
      </div>
      <el-button :icon="RefreshRight" :loading="loadingList" @click="loadRankings">刷新榜单</el-button>
    </header>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="ranking-alert"
    >
      <el-button size="small" :icon="RefreshRight" @click="loadRankings">重新加载</el-button>
    </el-alert>

    <div v-if="loadingList" class="ranking-skeleton">
      <el-skeleton :rows="6" animated />
    </div>

    <section v-else-if="rankings.length === 0" class="empty-board">
      <el-icon :size="34"><Trophy /></el-icon>
      <h2>暂无启用榜单</h2>
      <p>需要先在运营后台创建榜单配置并生成期次，榜单才会展示在这里。</p>
      <RouterLink v-if="isAdmin()" to="/admin/rankings">
        <el-button type="primary" plain>前往榜单配置</el-button>
      </RouterLink>
    </section>

    <div v-else class="ranking-layout">
      <aside class="ranking-tabs" aria-label="榜单切换">
        <button
          v-for="ranking in rankings"
          :key="ranking.rankingCode"
          type="button"
          class="ranking-tab"
          :class="{ active: ranking.rankingCode === activeCode }"
          @click="selectRanking(ranking.rankingCode)"
        >
          <span class="tab-name">{{ ranking.rankingName }}</span>
          <span class="tab-meta">
            {{ typeLabel(ranking.rankingType) }} · {{ periodLabel(ranking.periodType) }}
          </span>
          <span class="tab-period">
            {{ ranking.latestPeriodNo ? `最新 ${ranking.latestPeriodNo}` : '尚未生成' }}
          </span>
        </button>
      </aside>

      <section class="ranking-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-eyebrow">CURRENT PERIOD</p>
            <h2>{{ activeConfig?.rankingName || '榜单' }}</h2>
            <div class="panel-tags">
              <el-tag effect="plain" size="small">{{ typeLabel(activeConfig?.rankingType) }}</el-tag>
              <el-tag effect="plain" type="warning" size="small">
                {{ periodLabel(activeConfig?.periodType) }}
              </el-tag>
              <el-tag effect="plain" type="info" size="small">
                展示前 {{ activeConfig?.topN }} 名
              </el-tag>
            </div>
          </div>
          <div class="period-picker">
            <span>期次</span>
            <el-select
              v-if="hasPeriods"
              :model-value="activePeriod"
              size="small"
              style="width: 150px"
              @change="changePeriod"
            >
              <el-option
                v-for="period in periods"
                :key="period.periodNo"
                :label="`${period.periodNo}（${period.itemCount} 条）`"
                :value="period.periodNo"
              />
            </el-select>
            <el-tag v-else effect="plain" size="small" type="info">暂无期次</el-tag>
          </div>
        </div>

        <el-skeleton v-if="loadingDetail" :rows="6" animated />

        <template v-else-if="items.length > 0">
          <el-table :data="items" class="ranking-table" row-key="novelId">
            <el-table-column label="名次" width="92" align="center">
              <template #default="scope">
                <span :class="rankClass(scope.row.rankNo)">{{ scope.row.rankNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作品" min-width="240">
              <template #default="scope">
                <RouterLink class="novel-link" :to="`/novels/${scope.row.novelId}`">
                  {{ scope.row.novelTitle }}
                </RouterLink>
                <span class="novel-id">ID {{ scope.row.novelId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="榜单得分" width="150" align="right">
              <template #default="scope">
                <strong class="score-value">{{ formatScore(scope.row.score) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="指标快照" min-width="230">
              <template #default="scope">
                <div class="metric-chips">
                  <span v-for="metric in metricEntries(scope.row)" :key="metric.key" class="metric-chip">
                    {{ metric.label }} {{ metric.value }}
                  </span>
                  <span v-if="metricEntries(scope.row).length === 0" class="metric-empty">-</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="期次" width="110" align="center">
              <template #default="scope">
                <span class="period-text">{{ scope.row.periodNo || detail?.periodNo }}</span>
              </template>
            </el-table-column>
          </el-table>

          <p class="panel-note">
            榜单为生成时刻的指标快照，后续互动数据变化不会回溯修改历史期次。
          </p>
        </template>

        <el-empty
          v-else
          :description="hasPeriods ? '本期暂无上榜作品' : '该榜单尚未生成任何期次'"
        />
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 26px;
}

.page-eyebrow {
  margin-bottom: 8px;
  color: #9b9284;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.page-heading h1 {
  margin: 0 0 10px;
  color: var(--ink);
  font: 600 32px/1.25 "Songti SC", "SimSun", serif;
  letter-spacing: 0.03em;
}

.page-description {
  max-width: 700px;
  margin: 0;
  color: #7d7466;
  font-size: 13px;
  line-height: 1.9;
}

.ranking-alert {
  margin-bottom: 20px;
}

.ranking-skeleton,
.empty-board,
.ranking-panel {
  padding: 30px;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(60, 48, 30, 0.045);
}

.empty-board {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 30px;
  color: #b0894a;
  text-align: center;
}

.empty-board h2 {
  margin: 0;
  color: var(--ink);
  font: 600 20px/1.4 "Songti SC", "SimSun", serif;
}

.empty-board p {
  margin: 0 0 8px;
  color: #8a8172;
  font-size: 13px;
}

.ranking-layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.ranking-tabs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ranking-tab {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 15px 17px;
  text-align: left;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-left: 3px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ranking-tab:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(60, 48, 30, 0.07);
}

.ranking-tab.active {
  background: #fdf8ee;
  border-left-color: var(--gold);
}

.tab-name {
  color: var(--ink);
  font: 600 16px/1.3 "Songti SC", "SimSun", serif;
}

.tab-meta {
  color: #8a8172;
  font-size: 11px;
}

.tab-period {
  color: #b0894a;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.ranking-panel {
  min-width: 0;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.panel-eyebrow {
  margin-bottom: 6px;
  color: #a89c88;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.panel-heading h2 {
  margin: 0 0 10px;
  color: var(--ink);
  font: 600 22px/1.3 "Songti SC", "SimSun", serif;
}

.panel-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.period-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8a8172;
  font-size: 11px;
}

.ranking-table {
  --el-table-border-color: #eee8dc;
  --el-table-header-bg-color: #faf6ee;
  --el-table-row-hover-bg-color: #fdf9f1;
}

.rank-badge {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #5c5344;
  font: 600 13px/1 Georgia, serif;
  background: #f1ebdf;
  border-radius: 50%;
}

.rank-first {
  color: #7a5517;
  background: linear-gradient(145deg, #f6e3b5, #d8ab54);
  box-shadow: 0 3px 8px rgba(180, 136, 57, 0.3);
}

.rank-second {
  color: #4c5763;
  background: linear-gradient(145deg, #eef1f4, #c3ccd6);
}

.rank-third {
  color: #6b4a32;
  background: linear-gradient(145deg, #eddccd, #c9a27f);
}

.novel-link {
  display: block;
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}

.novel-link:hover {
  color: var(--accent);
}

.novel-id {
  color: #a89c88;
  font-size: 10px;
}

.score-value {
  color: var(--ink);
  font: 600 17px/1 Georgia, "Times New Roman", serif;
}

.metric-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.metric-chip {
  padding: 2px 8px;
  color: #7d7466;
  font-size: 11px;
  background: #f6f1e7;
  border-radius: 999px;
}

.metric-empty,
.period-text {
  color: #a89c88;
  font-size: 11px;
}

.panel-note {
  margin: 20px 0 0;
  padding-top: 16px;
  color: #a89c88;
  font-size: 11px;
  line-height: 1.8;
  border-top: 1px solid #eee8dc;
}

@media (max-width: 960px) {
  .ranking-layout {
    grid-template-columns: 1fr;
  }

  .ranking-tabs {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .ranking-tab {
    min-width: 168px;
  }
}

@media (max-width: 600px) {
  .page-heading {
    flex-direction: column;
  }

  .ranking-skeleton,
  .ranking-panel {
    padding: 20px 14px;
  }

  .panel-heading {
    flex-direction: column;
  }
}
</style>
