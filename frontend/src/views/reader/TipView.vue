<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createTip, getTips } from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const lastResult = ref(null)
const form = reactive({
  novelId: '',
  chapterId: '',
  amount: 5,
  message: '',
})

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
    const data = await getTips({ page: page.value, pageSize: pageSize.value })
    items.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.novelId.trim()) {
    ElMessage.warning('请输入小说 ID')
    return
  }
  submitting.value = true
  try {
    const data = await createTip({
      novelId: form.novelId.trim(),
      chapterId: form.chapterId.trim() || null,
      amount: form.amount,
      message: form.message.trim(),
    })
    lastResult.value = data
    if (data.duplicate) {
      ElMessage.info('该请求已处理，本次未重复扣款')
    } else {
      ElMessage.success(`打赏成功，当前余额 ${money(data.fromBalanceAfter)}`)
    }
    form.chapterId = ''
    form.message = ''
    page.value = 1
    await load()
  } finally {
    submitting.value = false
  }
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="tip-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">SUPPORT / 支持作者</p>
        <h1>打赏作者</h1>
        <p class="page-description">
          打赏会从你的余额扣除并全额进入作者账户，同时生成 TIP_OUT / TIP_IN 两条账户流水。
          收款人由后端按 novel.author_id 校验，不能指定给非作者账户。
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="tip-layout">
      <div class="tip-form-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">SEND A TIP</p>
            <h2>给作品一点鼓励</h2>
          </div>
          <el-icon class="tip-icon"><Tickets /></el-icon>
        </div>

        <el-form label-position="top" class="tip-form">
          <el-form-item label="小说 ID" required>
            <el-input v-model="form.novelId" placeholder="例如 10001" clearable />
          </el-form-item>
          <el-form-item label="章节 ID（选填）">
            <el-input v-model="form.chapterId" placeholder="不填表示整本书打赏" clearable />
          </el-form-item>
          <el-form-item label="打赏金额（元）" required>
            <el-input-number
              v-model="form.amount"
              :min="0.01"
              :max="100000"
              :precision="2"
              :step="1"
              controls-position="right"
              class="amount-input"
            />
          </el-form-item>
          <el-form-item label="留言">
            <el-input
              v-model="form.message"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="写一句想对作者说的话（选填）"
            />
          </el-form-item>
          <el-button type="warning" class="submit-button" :loading="submitting" @click="submit">
            确认打赏
          </el-button>
        </el-form>

        <p class="tip-note">当前为课程演示环境，不接真实支付；余额不足会由数据库事务整体回滚。</p>
      </div>

      <aside class="tip-guide">
        <p class="panel-kicker">HOW IT WORKS</p>
        <h2>资金路径</h2>
        <ol>
          <li><span>01</span>校验作品与作者归属</li>
          <li><span>02</span>锁定打赏者与作者账户</li>
          <li><span>03</span>扣打赏者余额、加作者余额</li>
          <li><span>04</span>写入 TIP_OUT / TIP_IN 两条流水</li>
        </ol>
        <div class="guide-note">
          <strong>数据库保证</strong>
          <span>双方账户按 user_id 顺序加锁，避免交叉死锁；任一步失败全部回滚。</span>
        </div>
      </aside>
    </section>

    <el-alert
      v-if="lastResult"
      :title="lastResult.duplicate ? '重复请求：' + (lastResult.message || '未重复扣款') : '打赏成功：' + (lastResult.message || '')"
      :type="lastResult.duplicate ? 'info' : 'success'"
      show-icon
      :closable="true"
      class="result-alert"
      @close="lastResult = null"
    />

    <section class="list-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">MY TIPS</p>
          <h2>我的打赏 <span>{{ total }}</span></h2>
        </div>
      </div>

      <el-table v-loading="loading" :data="items" row-key="tipId" class="tip-table">
        <el-table-column label="作品" min-width="220">
          <template #default="scope">
            <div class="book-copy">
              <strong>{{ scope.row.novelTitle }}</strong>
              <small>{{ scope.row.chapterTitle || '整本书打赏' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="120">
          <template #default="scope">{{ scope.row.authorName }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="scope">
            <strong class="amount">{{ money(scope.row.amount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="留言" min-width="200" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.message || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'info'" effect="plain" size="small">
              {{ scope.row.status === 'SUCCESS' ? '成功' : '已退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="176">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有打赏记录" />
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
.tip-page {
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
  max-width: 760px;
  margin-bottom: 0;
  color: #6e7b8f;
  font-size: 13px;
  line-height: 1.8;
}

.tip-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(260px, 0.65fr);
  gap: 20px;
}

.tip-form-panel,
.tip-guide,
.list-panel {
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-radius: 8px;
}

.tip-form-panel {
  border-top: 3px solid #bf9000;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 0;
  color: #17233c;
  font-size: 18px;
}

.tip-icon {
  color: #bf9000;
  font-size: 26px;
}

.tip-form :deep(.el-form-item__label) {
  color: #475467;
  font-size: 12px;
}

.amount-input {
  width: 190px;
}

.submit-button {
  width: 160px;
}

.tip-note {
  margin: 16px 0 0;
  color: #98a2b3;
  font-size: 10px;
  line-height: 1.7;
}

.tip-guide {
  background: #fbfaf6;
  border-top: 3px solid #c3903f;
}

.tip-guide h2 {
  margin: 0 0 18px;
  color: #17233c;
  font-size: 18px;
}

.tip-guide ol {
  display: grid;
  gap: 14px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.tip-guide li {
  display: flex;
  align-items: center;
  gap: 11px;
  color: #475467;
  font-size: 12px;
}

.tip-guide li span {
  color: #c3903f;
  font: 600 11px/1 Georgia, serif;
}

.guide-note {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #e8e1d2;
}

.guide-note strong {
  color: #7b5a1c;
  font-size: 12px;
}

.guide-note span {
  color: #8793a5;
  font-size: 10px;
  line-height: 1.7;
}

.result-alert {
  margin: 0;
}

.list-panel h2 span {
  color: #bf9000;
  font: 600 14px/1 Georgia, serif;
}

.book-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.book-copy strong {
  overflow: hidden;
  color: #1d2939;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-copy small {
  color: #8793a5;
  font-size: 11px;
}

.amount {
  color: #bf9000;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 960px) {
  .tip-layout {
    grid-template-columns: 1fr;
  }

  .tip-guide {
    display: none;
  }
}

@media (max-width: 760px) {
  .list-panel {
    padding: 18px 14px;
  }
}
</style>
