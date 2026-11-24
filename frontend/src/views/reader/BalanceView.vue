<script setup>
import { onMounted, ref } from 'vue'
import { Money, Refresh, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { getBalance, getTransactions } from '@/api/user'

const loading = ref(false)
const summary = ref(null)
const transactions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const bizLabels = {
  RECHARGE: '充值',
  SUBSCRIBE: '章节订阅',
  TIP_OUT: '打赏支出',
  TIP_IN: '打赏收入',
  REFUND: '退款',
  WITHDRAW: '提现',
  ADJUST_IN: '调增',
  ADJUST_OUT: '调减',
}

function money(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    const [balance, pageData] = await Promise.all([
      getBalance(),
      getTransactions({ page: page.value, pageSize: pageSize.value }),
    ])
    summary.value = balance
    transactions.value = pageData.list || []
    total.value = pageData.total || 0
  } finally {
    loading.value = false
  }
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="balance-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">ACCOUNT / 账户</p>
        <h1>余额与流水</h1>
        <p class="page-description">
          余额变化全部来自账户流水。订阅扣费、打赏支出和作者入账均记录 balance_before / balance_after，可用于逐笔对账。
        </p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="balance-grid">
      <article class="balance-card main">
        <div class="card-icon"><el-icon><Wallet /></el-icon></div>
        <p>可用余额</p>
        <strong>{{ money(summary?.balance) }}</strong>
        <span>元 · 由 user_account.balance 提供</span>
      </article>
      <article class="balance-card">
        <div class="card-icon income"><el-icon><TrendCharts /></el-icon></div>
        <p>累计入账</p>
        <strong>{{ money(summary?.totalIncome) }}</strong>
        <span>含充值、作者打赏收入等</span>
      </article>
      <article class="balance-card">
        <div class="card-icon expense"><el-icon><Money /></el-icon></div>
        <p>累计支出</p>
        <strong>{{ money(summary?.totalConsumption) }}</strong>
        <span>含订阅、打赏支出等</span>
      </article>
      <article class="balance-card">
        <div class="card-icon count">
          <span>{{ summary?.transactionCount ?? 0 }}</span>
        </div>
        <p>流水笔数</p>
        <strong>{{ summary?.transactionCount ?? 0 }}</strong>
        <span>账户流水只追加，不修改</span>
      </article>
    </section>

    <section class="list-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">TRANSACTION LEDGER</p>
          <h2>账户流水 <span>{{ total }}</span></h2>
        </div>
      </div>

      <el-table v-loading="loading" :data="transactions" row-key="txnNo" class="ledger-table">
        <el-table-column label="时间" width="176">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="业务" width="128">
          <template #default="scope">
            <span class="biz-label">{{ bizLabels[scope.row.bizType] || scope.row.bizType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="88">
          <template #default="scope">
            <el-tag :type="scope.row.direction === 'IN' ? 'success' : 'warning'" effect="plain" size="small">
              {{ scope.row.direction === 'IN' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="scope">
            <strong :class="scope.row.direction === 'IN' ? 'amount-in' : 'amount-out'">
              {{ scope.row.direction === 'IN' ? '+' : '-' }}{{ money(scope.row.amount) }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column label="记账后余额" width="130" align="right">
          <template #default="scope">{{ money(scope.row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="180" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.remark || '—' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有账户流水" />
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

    <p class="ledger-note">
      对账规则：账户余额应等于全部流水净额；每笔 balance_after 应等于 balance_before ± amount。充值、提现接口暂未实现。
    </p>
  </div>
</template>

<style scoped>
.balance-page {
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

.balance-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr;
  gap: 16px;
}

.balance-card {
  position: relative;
  padding: 21px 22px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-radius: 8px;
}

.balance-card.main {
  border-top: 3px solid #3568a8;
}

.balance-card p {
  margin: 14px 0 5px;
  color: #8793a5;
  font-size: 11px;
}

.balance-card strong {
  display: block;
  color: #17233c;
  font: 600 28px/1.1 Georgia, "Times New Roman", serif;
}

.balance-card.main strong {
  color: #3568a8;
}

.balance-card > span {
  display: block;
  margin-top: 8px;
  color: #98a2b3;
  font-size: 9px;
}

.card-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #3568a8;
  background: #eaf1f9;
  border-radius: 5px;
}

.card-icon.income {
  color: #36765f;
  background: #edf6f2;
}

.card-icon.expense {
  color: #a15c1a;
  background: #fdf1e5;
}

.card-icon.count {
  color: #674ea7;
  background: #f0edf8;
  font: 600 12px/1 Georgia, serif;
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

.panel-heading h2 {
  margin: 0;
  color: #17233c;
  font-size: 18px;
}

.panel-heading h2 span {
  color: #3568a8;
  font: 600 14px/1 Georgia, serif;
}

.biz-label {
  color: #475467;
  font-size: 12px;
}

.amount-in {
  color: #36765f;
}

.amount-out {
  color: #a15c1a;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.ledger-note {
  margin: 0;
  color: #98a2b3;
  font-size: 10px;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .balance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .balance-grid {
    grid-template-columns: 1fr;
  }

  .list-panel {
    padding: 18px 14px;
  }
}
</style>
