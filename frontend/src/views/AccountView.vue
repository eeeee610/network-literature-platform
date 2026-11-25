<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">余额与流水</h2>
        <p class="page-desc">查看账户余额与全部资金流水，流水只追加、不可修改</p>
      </div>
      <el-button type="primary" @click="rechargeVisible = true">模拟充值</el-button>
    </div>

    <el-card class="section-card balance-card" shadow="never">
      <div class="balance-box">
        <div>
          <div class="muted">账户余额（元）</div>
          <div class="balance">￥{{ formatMoney(account.balance) }}</div>
          <div class="muted">用户 ID：{{ account.userId || '-' }} · 更新时间：{{ formatTime(account.updatedAt) }}</div>
        </div>
        <div class="balance-hint">
          <div>余额变动均通过账户流水记录</div>
          <div>订阅、打赏会实时扣减余额</div>
        </div>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="toolbar">
        <el-select v-model="query.bizType" placeholder="业务类型" clearable style="width: 160px" @change="onFilter">
          <el-option v-for="item in bizTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.direction" placeholder="资金方向" clearable style="width: 140px" @change="onFilter">
          <el-option label="收入" value="IN" />
          <el-option label="支出" value="OUT" />
        </el-select>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ bizTypeLabel(row.bizType) }}</template>
        </el-table-column>
        <el-table-column label="方向" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'warning'" effect="plain">
              {{ row.direction === 'IN' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发生额" width="110" align="right">
          <template #default="{ row }">
            <span :class="row.direction === 'IN' ? 'income' : 'expense'">
              {{ row.direction === 'IN' ? '+' : '-' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额变动" width="170" align="right">
          <template #default="{ row }">
            {{ formatMoney(row.balanceBefore) }} → {{ formatMoney(row.balanceAfter) }}
          </template>
        </el-table-column>
        <el-table-column label="业务键" prop="bizKey" min-width="160" show-overflow-tooltip />
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <template #empty>
          <div class="empty-tip">暂无流水记录</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadTransactions"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="rechargeVisible" title="模拟充值" width="420px">
      <el-form label-position="top">
        <el-form-item label="充值金额（元）">
          <el-input-number v-model="rechargeAmount" :min="0.01" :max="10000" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <p class="muted">课程设计环境使用模拟充值，不接入第三方支付，充值后余额立即到账。</p>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="onRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAccount, listTransactions, recharge } from '../api/account'
import { BIZ_TYPE_OPTIONS, bizTypeLabel, formatMoney, formatTime } from '../utils/format'

const account = ref({})
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, pageSize: 10, bizType: '', direction: '' })
const bizTypes = BIZ_TYPE_OPTIONS

const rechargeVisible = ref(false)
const rechargeAmount = ref(50)
const recharging = ref(false)

async function loadAccount() {
  try {
    account.value = await getAccount()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

async function loadTransactions() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.bizType) params.bizType = query.bizType
    if (query.direction) params.direction = query.direction
    const data = await listTransactions(params)
    rows.value = data.list
    total.value = data.total
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function onFilter() {
  query.page = 1
  loadTransactions()
}

function onReset() {
  query.bizType = ''
  query.direction = ''
  query.page = 1
  loadTransactions()
}

function onSizeChange() {
  query.page = 1
  loadTransactions()
}

async function onRecharge() {
  if (recharging.value) return
  recharging.value = true
  try {
    const data = await recharge(Number(rechargeAmount.value))
    ElMessage.success(`充值成功，当前余额 ￥${formatMoney(data.balance)}`)
    rechargeVisible.value = false
    await loadAccount()
    query.page = 1
    loadTransactions()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    recharging.value = false
  }
}

onMounted(() => {
  loadAccount()
  loadTransactions()
})
</script>

<style scoped>
.balance-card {
  background: linear-gradient(120deg, #eef1ff 0%, #ffffff 60%);
}

.balance-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.balance {
  font-size: 36px;
  font-weight: 700;
  color: var(--brand);
  margin: 6px 0 8px;
}

.balance-hint {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.8;
}

.income {
  color: #16a34a;
  font-weight: 600;
}

.expense {
  color: #dc2626;
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
