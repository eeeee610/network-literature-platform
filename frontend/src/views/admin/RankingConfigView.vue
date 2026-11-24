<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, View } from '@element-plus/icons-vue'
import {
  createRanking,
  generateRanking,
  getAdminRankings,
  getRankingDetail,
  getRankingPeriods,
  updateRanking,
} from '@/api/interaction'

const rankings = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingRow = ref(null)
const submitting = ref(false)
const form = reactive({
  rankingCode: '',
  rankingName: '',
  rankingType: 'FAVORITE',
  periodType: 'DAILY',
  topN: 100,
  weightText: '{}',
  isActive: true,
})

const generateVisible = ref(false)
const generateTarget = ref(null)
const generateForm = reactive({ periodNo: '', allowHistoryOverwrite: false })
const generating = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref(null)
const detail = ref(null)
const detailPeriods = ref([])
const detailPeriod = ref('')

const typeOptions = [
  { value: 'FAVORITE', label: '收藏榜' },
  { value: 'RATING', label: '评分榜' },
  { value: 'TICKET', label: '打赏榜' },
  { value: 'SALE', label: '订阅榜' },
]
const periodOptions = [
  { value: 'DAILY', label: '日榜' },
  { value: 'WEEKLY', label: '周榜' },
  { value: 'MONTHLY', label: '月榜' },
  { value: 'ALL', label: '总榜' },
]
const metricLabels = { favorite: '收藏', rating: '评分', ticket: '打赏', sale: '订阅' }

const typeLabels = Object.fromEntries(typeOptions.map((item) => [item.value, item.label]))
const periodLabels = Object.fromEntries(periodOptions.map((item) => [item.value, item.label]))
const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建榜单配置' : '编辑榜单配置'))
const isEditing = computed(() => dialogMode.value === 'edit')

async function loadRankings() {
  loading.value = true
  try {
    rankings.value = await getAdminRankings()
  } catch {
    rankings.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingRow.value = null
  form.rankingCode = ''
  form.rankingName = ''
  form.rankingType = 'FAVORITE'
  form.periodType = 'DAILY'
  form.topN = 100
  form.weightText = '{}'
  form.isActive = true
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingRow.value = row
  form.rankingCode = row.rankingCode
  form.rankingName = row.rankingName
  form.rankingType = row.rankingType
  form.periodType = row.periodType
  form.topN = row.topN
  form.weightText = JSON.stringify(row.weightConfig || {}, null, 2)
  form.isActive = row.active
  dialogVisible.value = true
}

function parseWeight(text) {
  const source = (text || '').trim()
  if (!source) {
    return {}
  }
  let parsed
  try {
    parsed = JSON.parse(source)
  } catch {
    throw new Error('权重配置不是合法的 JSON')
  }
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('权重配置必须是 JSON 对象，例如 {"favorite": 1}')
  }
  return parsed
}

async function submitForm() {
  const rankingName = form.rankingName.trim()
  if (!rankingName) {
    ElMessage.warning('请填写榜单名称')
    return
  }
  if (!form.topN || form.topN < 1 || form.topN > 1000) {
    ElMessage.warning('展示条数上限需在 1~1000 之间')
    return
  }
  let weightConfig
  try {
    weightConfig = parseWeight(form.weightText)
  } catch (error) {
    ElMessage.warning(error.message)
    return
  }

  if (dialogMode.value === 'create' && !/^[A-Z0-9_]{2,50}$/.test(form.rankingCode.trim())) {
    ElMessage.warning('榜单编码只能由大写字母、数字和下划线组成，长度 2~50')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRanking({
        rankingCode: form.rankingCode.trim(),
        rankingName,
        rankingType: form.rankingType,
        periodType: form.periodType,
        topN: form.topN,
        weightConfig,
        isActive: form.isActive,
      })
      ElMessage.success('榜单配置已创建')
    } else {
      await updateRanking(editingRow.value.rankingId, {
        rankingName,
        topN: form.topN,
        weightConfig,
        isActive: form.isActive,
      })
      ElMessage.success('榜单配置已保存')
    }
    dialogVisible.value = false
    loadRankings()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    submitting.value = false
  }
}

async function toggleActive(row, value) {
  try {
    await updateRanking(row.rankingId, {
      rankingName: row.rankingName,
      topN: row.topN,
      weightConfig: row.weightConfig || {},
      isActive: value,
    })
    row.active = value
    ElMessage.success(value ? '榜单已启用' : '榜单已停用')
  } catch {
    loadRankings()
  }
}

function suggestPeriod(periodType, date = new Date()) {
  if (periodType === 'ALL') {
    return 'ALL'
  }
  const pad = (number) => String(number).padStart(2, '0')
  if (periodType === 'DAILY') {
    return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`
  }
  if (periodType === 'MONTHLY') {
    return `${date.getFullYear()}${pad(date.getMonth() + 1)}`
  }
  const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = target.getUTCDay() || 7
  target.setUTCDate(target.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil(((target - yearStart) / 86400000 + 1) / 7)
  return `${target.getUTCFullYear()}W${pad(weekNo)}`
}

function openGenerate(row) {
  generateTarget.value = row
  generateForm.periodNo = suggestPeriod(row.periodType)
  generateForm.allowHistoryOverwrite = false
  generateVisible.value = true
}

async function submitGenerate() {
  const periodNo = generateForm.periodNo.trim()
  if (!periodNo) {
    ElMessage.warning('请填写期次号')
    return
  }
  generating.value = true
  try {
    const data = await generateRanking(generateTarget.value.rankingId, {
      periodNo,
      allowHistoryOverwrite: generateForm.allowHistoryOverwrite,
    })
    ElMessage.success(`期次 ${data.periodNo} 已生成，共 ${data.itemCount} 条榜单条目`)
    generateVisible.value = false
    loadRankings()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    generating.value = false
  }
}

async function openDetail(row) {
  detailRow.value = row
  detail.value = null
  detailPeriods.value = []
  detailPeriod.value = row.latestPeriodNo || ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailPeriods.value = await getRankingPeriods(row.rankingCode, { limit: 20 })
    await loadDetail()
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function loadDetail() {
  if (!detailRow.value) {
    return
  }
  detailLoading.value = true
  try {
    detail.value = await getRankingDetail(detailRow.value.rankingCode, {
      periodNo: detailPeriod.value || undefined,
      limit: detailRow.value.topN,
    })
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function changeDetailPeriod(value) {
  detailPeriod.value = value
  loadDetail()
}

async function confirmGenerateInfo(row) {
  try {
    await ElMessageBox.confirm(
      `将按当前 ${typeLabels[row.rankingType]} 指标生成「${row.rankingName}」的期次快照，是否继续？`,
      '生成榜单期次',
      { type: 'info', confirmButtonText: '去生成', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  openGenerate(row)
}

function weightSummary(row) {
  const entries = Object.entries(row.weightConfig || {})
  if (entries.length === 0) {
    return '默认权重 1.0'
  }
  return entries.map(([key, value]) => `${metricLabels[key] || key}×${value}`).join(' · ')
}

function metricEntries(item) {
  return Object.entries(item.metricSnapshot || {}).map(([key, value]) => ({
    key,
    label: metricLabels[key] || key,
    value,
  }))
}

function formatScore(score) {
  const number = Number(score)
  if (Number.isNaN(number)) {
    return '-'
  }
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
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

onMounted(loadRankings)
</script>

<template>
  <div class="ranking-admin-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">INTERACTION / 榜单配置</p>
        <h1>榜单配置与期次生成</h1>
        <p class="page-description">
          维护收藏、评分、打赏、订阅四类榜单的展示名次与指标权重，并按周期生成期次快照。历史期次默认禁止覆盖，仅修复时显式开启。
        </p>
      </div>
      <div class="heading-actions">
        <el-button :icon="RefreshRight" :loading="loading" @click="loadRankings">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建榜单</el-button>
      </div>
    </div>

    <section class="table-card">
      <el-table v-loading="loading" :data="rankings" row-key="rankingId" class="ranking-table">
        <el-table-column label="榜单" min-width="190">
          <template #default="scope">
            <span class="cell-strong">{{ scope.row.rankingName }}</span>
            <span class="cell-code">{{ scope.row.rankingCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="指标 / 周期" width="150">
          <template #default="scope">
            <el-tag effect="plain" size="small">{{ typeLabels[scope.row.rankingType] }}</el-tag>
            <el-tag effect="plain" type="warning" size="small" class="type-tag">
              {{ periodLabels[scope.row.periodType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示上限" width="92" align="center">
          <template #default="scope">
            <span class="cell-strong">前 {{ scope.row.topN }} 名</span>
          </template>
        </el-table-column>
        <el-table-column label="权重配置" min-width="190">
          <template #default="scope">
            <span class="cell-sub">{{ weightSummary(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最新期次" width="170">
          <template #default="scope">
            <template v-if="scope.row.latestPeriodNo">
              <span class="cell-strong">{{ scope.row.latestPeriodNo }}</span>
              <span class="cell-sub">{{ scope.row.latestItemCount }} 条上榜</span>
            </template>
            <span v-else class="cell-sub">尚未生成</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="82" align="center">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.active"
              size="small"
              @change="(value) => toggleActive(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="scope">
            <span class="cell-sub">{{ formatTime(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="View" @click="openDetail(scope.row)">详情</el-button>
            <el-button link @click="openEdit(scope.row)">编辑</el-button>
            <el-button
              link
              type="warning"
              :disabled="!scope.row.active"
              @click="confirmGenerateInfo(scope.row)"
            >
              生成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rankings.length === 0" description="暂无榜单配置，先创建收藏/评分/打赏/订阅榜单" />
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form label-width="96px" class="config-form">
        <el-form-item label="榜单编码" required>
          <el-input
            v-model="form.rankingCode"
            :disabled="isEditing"
            placeholder="如 FAVORITE_WEEKLY"
            maxlength="50"
          />
          <p v-if="!isEditing" class="field-hint">大写字母、数字与下划线，长度 2~50，创建后不可修改。</p>
        </el-form-item>
        <el-form-item label="榜单名称" required>
          <el-input v-model="form.rankingName" placeholder="如 周收藏榜" maxlength="100" />
        </el-form-item>
        <el-form-item label="指标类型" required>
          <el-select v-model="form.rankingType" :disabled="isEditing" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期" required>
          <el-select v-model="form.periodType" :disabled="isEditing" style="width: 100%">
            <el-option
              v-for="item in periodOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <p v-if="isEditing" class="field-hint">指标类型与周期创建后不可修改，避免历史快照失真。</p>
        </el-form-item>
        <el-form-item label="展示上限" required>
          <el-input-number v-model="form.topN" :min="1" :max="1000" :step="10" />
          <p class="field-hint">单期最多收录的作品数量，1~1000。</p>
        </el-form-item>
        <el-form-item label="权重配置">
          <el-input
            v-model="form.weightText"
            type="textarea"
            :rows="4"
            placeholder='{"favorite": 1}'
          />
          <p class="field-hint">
            指标值 × 对应权重作为榜单得分；键名使用小写指标（favorite/rating/ticket/sale），缺省按 1.0 计。
          </p>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isActive" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generateVisible" title="生成榜单期次" width="500px" destroy-on-close>
      <div v-if="generateTarget" class="generate-info">
        <p>
          <strong>{{ generateTarget.rankingName }}</strong>
          <span>{{ typeLabels[generateTarget.rankingType] }} · {{ periodLabels[generateTarget.periodType] }}</span>
        </p>
        <p class="field-hint">
          期次格式：日榜 yyyymmdd，周榜 yyyyWww（如 2026W47），月榜 yyyymm，总榜 ALL。
        </p>
      </div>
      <el-form label-width="96px">
        <el-form-item label="期次号" required>
          <el-input v-model="generateForm.periodNo" maxlength="20" placeholder="请输入期次号" />
        </el-form-item>
        <el-form-item label="历史覆盖">
          <el-switch
            v-model="generateForm.allowHistoryOverwrite"
            active-text="允许覆盖"
            inactive-text="禁止覆盖"
          />
        </el-form-item>
      </el-form>
      <p class="field-hint overwrite-hint">
        常规生成请保持禁止覆盖；仅在修复历史期次时开启，且同一期次的数据会被重新计算写入。
      </p>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailVisible"
      :title="`${detailRow?.rankingName || '榜单'} · 期次详情`"
      width="820px"
      destroy-on-close
    >
      <div class="detail-toolbar">
        <span class="detail-label">期次</span>
        <el-select
          v-if="detailPeriods.length > 0"
          v-model="detailPeriod"
          size="small"
          style="width: 170px"
          @change="changeDetailPeriod"
        >
          <el-option
            v-for="period in detailPeriods"
            :key="period.periodNo"
            :label="`${period.periodNo}（${period.itemCount} 条）`"
            :value="period.periodNo"
          />
        </el-select>
        <el-tag v-else effect="plain" size="small" type="info">暂无期次</el-tag>
      </div>

      <el-skeleton v-if="detailLoading" :rows="5" animated />

      <template v-else-if="detail?.items?.length">
        <el-table :data="detail.items" row-key="novelId" max-height="420">
          <el-table-column label="名次" width="80" align="center">
            <template #default="scope">
              <span class="rank-no">{{ scope.row.rankNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="作品" min-width="190">
            <template #default="scope">
              <span class="cell-strong">{{ scope.row.novelTitle }}</span>
              <span class="cell-sub">ID {{ scope.row.novelId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="120" align="right">
            <template #default="scope">
              <strong class="score-value">{{ formatScore(scope.row.score) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="指标快照" min-width="200">
            <template #default="scope">
              <div class="metric-chips">
                <span v-for="metric in metricEntries(scope.row)" :key="metric.key" class="metric-chip">
                  {{ metric.label }} {{ metric.value }}
                </span>
                <span v-if="metricEntries(scope.row).length === 0" class="cell-sub">-</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="该期次暂无上榜作品" />
    </el-dialog>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  gap: 10px;
}

.table-card {
  padding: 18px 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-top: 3px solid var(--gold);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.035);
}

.ranking-table {
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
  line-height: 1.7;
}

.cell-code {
  display: block;
  color: #a89c88;
  font: 600 10px/1.6 Georgia, serif;
  letter-spacing: 0.06em;
}

.type-tag {
  margin-left: 4px;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.field-hint {
  width: 100%;
  margin: 6px 0 0;
  color: #98a2b3;
  font-size: 11px;
  line-height: 1.7;
}

.generate-info {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f7f9fb;
  border-radius: 6px;
}

.generate-info p:first-child {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 0;
}

.generate-info strong {
  color: var(--ink);
  font-size: 14px;
}

.generate-info span {
  color: #8793a5;
  font-size: 11px;
}

.overwrite-hint {
  margin-top: -8px;
  padding-left: 96px;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-label {
  color: #8793a5;
  font-size: 12px;
}

.rank-no {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  color: #5c5344;
  font: 600 12px/1 Georgia, serif;
  background: #f1ebdf;
  border-radius: 50%;
}

.score-value {
  color: var(--ink);
  font: 600 15px/1 Georgia, "Times New Roman", serif;
}

.metric-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.metric-chip {
  padding: 1px 8px;
  color: #7d7466;
  font-size: 11px;
  background: #f6f1e7;
  border-radius: 999px;
}

@media (max-width: 600px) {
  .page-heading {
    flex-direction: column;
  }

  .table-card {
    padding: 14px;
  }

  .overwrite-hint {
    padding-left: 0;
  }
}
</style>
