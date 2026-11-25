<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">作品打赏</h2>
        <p class="page-desc">用余额向作者表达支持，打赏将同时记录打赏者支出与作者收入两笔流水</p>
      </div>
      <div class="inline-form">
        <el-input
          v-model="novelInput"
          placeholder="输入作品 ID"
          clearable
          style="width: 200px"
          @keyup.enter="loadTarget"
        />
        <el-button type="primary" plain @click="loadTarget">查询作品</el-button>
      </div>
    </div>

    <el-card v-if="target" class="section-card" shadow="never">
      <div class="target">
        <div class="cover">
          <img v-if="target.coverUrl" :src="target.coverUrl" :alt="target.novelTitle" />
          <span v-else>封面</span>
        </div>
        <div class="target-info">
          <div class="target-title">{{ target.novelTitle }}</div>
          <div class="muted">作者：{{ target.authorName }}</div>
          <div class="muted">
            当前余额：<span class="money">￥{{ formatMoney(balance) }}</span>
            <el-button link type="primary" @click="router.push('/account')">去充值</el-button>
          </div>
        </div>
      </div>

      <el-divider />

      <el-form label-width="90px" class="reward-form">
        <el-form-item label="打赏金额">
          <div class="amount-row">
            <el-button
              v-for="item in quickAmounts"
              :key="item"
              :type="amount === item ? 'primary' : 'default'"
              size="small"
              @click="amount = item"
            >
              ￥{{ item }}
            </el-button>
            <el-input-number v-model="amount" :min="0.01" :max="10000" :precision="2" :step="1" />
          </div>
        </el-form-item>
        <el-form-item label="关联章节">
          <el-input v-model="chapterId" placeholder="选填，填写后打赏将关联到该章节" clearable />
        </el-form-item>
        <el-form-item label="打赏留言">
          <el-input
            v-model="message"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="写点什么鼓励作者吧（选填）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">确认打赏</el-button>
          <span class="muted">同一请求号重复提交不会重复扣款</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="我发出的打赏" name="sent">
          <el-table v-loading="loading.sent" :data="sentRows" stripe>
            <el-table-column label="作品" prop="novelTitle" min-width="150" />
            <el-table-column label="章节" min-width="160">
              <template #default="{ row }">{{ row.chapterTitle || '整本作品' }}</template>
            </el-table-column>
            <el-table-column label="作者" prop="toNickname" width="120" />
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }">
                <span class="money">￥{{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="留言" prop="message" min-width="160" show-overflow-tooltip />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>
              <div class="empty-tip">还没有发出过打赏</div>
            </template>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="sentQuery.page"
              v-model:page-size="sentQuery.pageSize"
              :total="sentTotal"
              layout="total, prev, pager, next"
              @current-change="loadSent"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我收到的打赏" name="received">
          <el-table v-loading="loading.received" :data="receivedRows" stripe>
            <el-table-column label="作品" prop="novelTitle" min-width="150" />
            <el-table-column label="章节" min-width="160">
              <template #default="{ row }">{{ row.chapterTitle || '整本作品' }}</template>
            </el-table-column>
            <el-table-column label="打赏者" prop="fromNickname" width="120" />
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }">
                <span class="money">￥{{ formatMoney(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="留言" prop="message" min-width="160" show-overflow-tooltip />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>
              <div class="empty-tip">还没有收到打赏</div>
            </template>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="receivedQuery.page"
              v-model:page-size="receivedQuery.pageSize"
              :total="receivedTotal"
              layout="total, prev, pager, next"
              @current-change="loadReceived"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAccount } from '../api/account'
import { getRewardTarget, listReceivedRewards, listSentRewards, sendReward } from '../api/reward'
import { formatMoney, formatTime, requestNo } from '../utils/format'

const route = useRoute()
const router = useRouter()
const novelInput = ref(typeof route.query.novelId === 'string' ? route.query.novelId : '')
const chapterId = ref(typeof route.query.chapterId === 'string' ? route.query.chapterId : '')
const target = ref(null)
const balance = ref(0)
const amount = ref(10)
const message = ref('')
const submitting = ref(false)
const quickAmounts = [5, 10, 20, 50]
const activeTab = ref('sent')

const sentRows = ref([])
const sentTotal = ref(0)
const sentQuery = reactive({ page: 1, pageSize: 10 })
const receivedRows = ref([])
const receivedTotal = ref(0)
const receivedQuery = reactive({ page: 1, pageSize: 10 })
const loading = reactive({ sent: false, received: false })

async function loadAccount() {
  try {
    const data = await getAccount()
    balance.value = data.balance
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

async function loadTarget() {
  const novelId = novelInput.value.trim()
  if (!novelId) {
    ElMessage.warning('请输入作品 ID')
    return
  }
  try {
    target.value = await getRewardTarget(novelId)
  } catch (e) {
    target.value = null
  }
}

async function onSubmit() {
  if (!target.value) {
    ElMessage.warning('请先查询作品')
    return
  }
  const value = Number(amount.value)
  if (!Number.isFinite(value) || value <= 0) {
    ElMessage.warning('请输入合法的打赏金额')
    return
  }
  submitting.value = true
  try {
    const data = await sendReward({
      novelId: target.value.novelId,
      chapterId: chapterId.value.trim() || null,
      amount: value,
      message: message.value.trim(),
      requestNo: requestNo()
    })
    ElMessage.success(`打赏成功，金额 ￥${formatMoney(data.amount)}，余额 ￥${formatMoney(data.balance)}`)
    balance.value = data.balance
    message.value = ''
    sentQuery.page = 1
    loadSent()
    if (activeTab.value !== 'sent') {
      activeTab.value = 'sent'
    }
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function loadSent() {
  loading.sent = true
  try {
    const data = await listSentRewards(sentQuery)
    sentRows.value = data.list
    sentTotal.value = data.total
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.sent = false
  }
}

async function loadReceived() {
  loading.received = true
  try {
    const data = await listReceivedRewards(receivedQuery)
    receivedRows.value = data.list
    receivedTotal.value = data.total
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.received = false
  }
}

function onTabChange(name) {
  if (name === 'received' && receivedRows.value.length === 0) {
    loadReceived()
  }
}

onMounted(() => {
  loadAccount()
  loadSent()
  if (novelInput.value) {
    loadTarget()
  }
})
</script>

<style scoped>
.inline-form {
  display: flex;
  gap: 8px;
}

.target {
  display: flex;
  align-items: center;
  gap: 16px;
}

.target .cover {
  width: 68px;
  height: 92px;
}

.target-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.reward-form {
  max-width: 640px;
}

.amount-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
