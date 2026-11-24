<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Reading, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSubscriptions, subscribeChapter } from '@/api/user'

const loading = ref(false)
const subscribing = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const form = reactive({ chapterId: '' })
const lastResult = ref(null)

function formatTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function money(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

async function load() {
  loading.value = true
  try {
    const data = await getSubscriptions({ page: page.value, pageSize: pageSize.value })
    items.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function subscribe() {
  const chapterId = form.chapterId.trim()
  if (!chapterId) {
    ElMessage.warning('请输入章节 ID')
    return
  }
  subscribing.value = true
  try {
    const data = await subscribeChapter({ chapterId })
    lastResult.value = data
    if (data.alreadySubscribed) {
      ElMessage.info('该章节已订阅，本次未重复扣费')
    } else {
      ElMessage.success(`订阅成功，当前余额 ${money(data.balanceAfter)}`)
    }
    form.chapterId = ''
    page.value = 1
    await load()
  } finally {
    subscribing.value = false
  }
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="subscription-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">CHAPTER ACCESS / 章节</p>
        <h1>章节订阅</h1>
        <p class="page-description">
          订阅价格由后端读取 chapter.vip_price，校验章节已发布且审核通过后扣费。重复订阅不会二次扣款。
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="subscribe-panel">
      <div class="subscribe-copy">
        <p class="panel-kicker">SUBSCRIBE A CHAPTER</p>
        <h2>订阅一个 VIP 章节</h2>
        <p>输入 B 模块的章节 ID。读取页完成后，会由阅读页直接发起订阅。</p>
      </div>
      <div class="subscribe-form">
        <el-input v-model="form.chapterId" placeholder="例如 20001" clearable @keyup.enter="subscribe">
          <template #prefix><el-icon><Reading /></el-icon></template>
        </el-input>
        <el-button type="primary" :loading="subscribing" @click="subscribe">订阅章节</el-button>
      </div>
    </section>

    <el-alert
      v-if="lastResult"
      :title="lastResult.alreadySubscribed ? '已订阅：' + (lastResult.message || '未重复扣费') : '订阅成功：' + (lastResult.message || '')"
      :type="lastResult.alreadySubscribed ? 'info' : 'success'"
      show-icon
      :closable="true"
      class="result-alert"
      @close="lastResult = null"
    />

    <section class="list-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">MY SUBSCRIPTIONS</p>
          <h2>订阅记录 <span>{{ total }}</span></h2>
        </div>
      </div>

      <el-table v-loading="loading" :data="items" row-key="chapterId" class="subscription-table">
        <el-table-column label="章节" min-width="260">
          <template #default="scope">
            <div class="chapter-cell">
              <span class="chapter-no">{{ scope.row.chapterNo }}</span>
              <div>
                <strong>{{ scope.row.chapterTitle }}</strong>
                <small>{{ scope.row.novelTitle }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标价" width="110">
          <template #default="scope">{{ money(scope.row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="实付" width="110">
          <template #default="scope">
            <strong class="paid">{{ money(scope.row.paidAmount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="104">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain" size="small">
              {{ scope.row.status === 'ACTIVE' ? '已订阅' : '已退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订阅时间" width="176">
          <template #default="scope">{{ formatTime(scope.row.subscribedAt) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有订阅记录" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="changePage"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.subscription-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.page-eyebrow,
.panel-kicker {
  margin-bottom: 7px;
  color: #8793a5;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.16em;
}

.page-heading h1 {
  margin-bottom: 9px;
  color: #17233c;
  font: 600 30px/1.2 "Songti SC", "SimSun", serif;
}

.page-description {
  max-width: 720px;
  margin-bottom: 0;
  color: #6e7b8f;
  font-size: 13px;
  line-height: 1.8;
}

.subscribe-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  padding: 22px 24px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-left: 3px solid #674ea7;
  border-radius: 8px;
}

.subscribe-panel h2,
.panel-heading h2 {
  margin: 0 0 7px;
  color: #17233c;
  font-size: 17px;
}

.subscribe-panel p:last-child {
  margin-bottom: 0;
  color: #8793a5;
  font-size: 12px;
}

.subscribe-form {
  display: flex;
  width: min(100%, 390px);
  gap: 10px;
}

.subscribe-form .el-input {
  flex: 1;
}

.result-alert {
  margin: 0;
}

.list-panel {
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-radius: 8px;
}

.panel-heading {
  margin-bottom: 16px;
}

.panel-heading h2 span {
  color: #674ea7;
  font: 600 14px/1 Georgia, serif;
}

.chapter-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chapter-no {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: #674ea7;
  font: 600 12px/1 Georgia, serif;
  background: #f0edf8;
  border-radius: 50%;
}

.chapter-cell div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.chapter-cell strong {
  overflow: hidden;
  color: #1d2939;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-cell small {
  color: #8793a5;
  font-size: 11px;
}

.paid {
  color: #674ea7;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .subscribe-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .subscribe-form {
    width: 100%;
  }

  .list-panel {
    padding: 18px 14px;
  }
}
</style>
