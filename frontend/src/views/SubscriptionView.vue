<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">章节订阅</h2>
        <p class="page-desc">VIP 章节按章订阅，余额支付，订阅成功后可在阅读页解锁全文</p>
      </div>
    </div>

    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>订阅章节</span>
          <div class="inline-form">
            <el-input
              v-model="chapterInput"
              placeholder="输入章节 ID"
              clearable
              style="width: 200px"
              @keyup.enter="onQuery"
            />
            <el-button type="primary" plain @click="onQuery">查询</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="empty-tip">加载中…</div>
      <div v-else-if="!info" class="empty-tip">输入章节 ID 查询章节订阅信息</div>
      <div v-else class="purchase">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="作品">{{ info.novelTitle }}</el-descriptions-item>
          <el-descriptions-item label="章节">
            第 {{ info.chapterNo }} 章 · {{ info.chapterTitle }}
          </el-descriptions-item>
          <el-descriptions-item label="章节类型">
            <el-tag v-if="info.isVip === 1" type="warning" effect="plain">VIP 章节</el-tag>
            <el-tag v-else type="success" effect="plain">免费章节</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订阅价格">
            <span v-if="info.isVip === 1" class="money">￥{{ formatMoney(info.vipPrice) }}</span>
            <span v-else>免费</span>
          </el-descriptions-item>
          <el-descriptions-item label="订阅状态">
            <el-tag v-if="info.subscribed" type="success" effect="plain">已订阅</el-tag>
            <el-tag v-else type="info" effect="plain">未订阅</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订阅时间">
            {{ formatTime(info.subscribedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="purchase-actions">
          <el-button
            type="primary"
            :loading="subscribing"
            :disabled="info.subscribed || info.isVip !== 1"
            @click="onSubscribe"
          >
            {{ actionText }}
          </el-button>
          <span class="muted">订阅扣款后将写入账户流水，重复提交不会重复扣款</span>
        </div>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <template #header>
        <span>我的订阅记录</span>
      </template>
      <el-table v-loading="listLoading" :data="records" stripe>
        <el-table-column label="作品" prop="novelTitle" min-width="160" />
        <el-table-column label="章节" min-width="200">
          <template #default="{ row }">第 {{ row.chapterNo }} 章 · {{ row.chapterTitle }}</template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">￥{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="实付" width="100" align="right">
          <template #default="{ row }">
            <span class="money">￥{{ formatMoney(row.paidAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ACTIVE' ? '生效中' : '已退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订阅时间" width="180">
          <template #default="{ row }">{{ formatTime(row.subscribedAt) }}</template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">还没有订阅记录</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadRecords"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getChapterPurchaseInfo, listSubscriptions, subscribeChapter } from '../api/subscription'
import { formatMoney, formatTime, requestNo } from '../utils/format'

const route = useRoute()
const info = ref(null)
const loading = ref(false)
const subscribing = ref(false)
const chapterInput = ref(typeof route.query.chapterId === 'string' ? route.query.chapterId : '')

const records = ref([])
const total = ref(0)
const listLoading = ref(false)
const query = reactive({ page: 1, pageSize: 10 })

const actionText = computed(() => {
  if (!info.value) return '订阅本章'
  if (info.value.subscribed) return '已订阅'
  if (info.value.isVip !== 1) return '免费章节无需订阅'
  return `支付 ￥${formatMoney(info.value.vipPrice)} 订阅本章`
})

async function onQuery() {
  const chapterId = chapterInput.value.trim()
  if (!chapterId) {
    ElMessage.warning('请输入章节 ID')
    return
  }
  loading.value = true
  try {
    info.value = await getChapterPurchaseInfo(chapterId)
  } catch (e) {
    info.value = null
  } finally {
    loading.value = false
  }
}

async function onSubscribe() {
  if (!info.value || info.value.subscribed) return
  subscribing.value = true
  try {
    const data = await subscribeChapter({
      novelId: info.value.novelId,
      chapterId: info.value.chapterId,
      requestNo: requestNo()
    })
    ElMessage.success(`订阅成功，实付 ￥${formatMoney(data.paidAmount)}，余额 ￥${formatMoney(data.balance)}`)
    info.value.subscribed = true
    info.value.subscribedAt = data.subscribedAt
    query.page = 1
    loadRecords()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    subscribing.value = false
  }
}

async function loadRecords() {
  listLoading.value = true
  try {
    const data = await listSubscriptions(query)
    records.value = data.list
    total.value = data.total
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    listLoading.value = false
  }
}

function onSizeChange() {
  query.page = 1
  loadRecords()
}

onMounted(() => {
  loadRecords()
  if (chapterInput.value) {
    onQuery()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.inline-form {
  display: flex;
  gap: 8px;
}

.purchase-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
