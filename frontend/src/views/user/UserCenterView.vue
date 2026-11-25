<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Money, Reading, SwitchButton, User, Wallet } from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import {
  addToBookshelf,
  cancelSubscription,
  getAccount,
  getBookshelf,
  getProfile,
  getSubscriptions,
  getTips,
  getTransactions,
  removeFromBookshelf,
  subscribeChapter,
  tipNovel,
  updateProfile,
} from '@/api/user'

const router = useRouter()
const activeTab = ref('overview')
const profile = ref(null)
const account = ref(null)
const initialLoading = ref(true)
const initialError = ref('')
const savingProfile = ref(false)
const submitting = reactive({ shelf: false, subscribe: false, tip: false })
const loading = reactive({ shelf: false, subscriptions: false, tips: false, transactions: false })
const profileForm = reactive({ nickname: '', avatarUrl: '', email: '', phone: '' })
const shelfForm = reactive({ novelId: '' })
const subscribeForm = reactive({ chapterId: '', requestNo: '' })
const tipForm = reactive({ novelId: '', chapterId: '', amount: 1, message: '', requestNo: '' })
const bookshelf = ref({ list: [], total: 0, page: 1, pageSize: 6 })
const subscriptions = ref({ list: [], total: 0, page: 1, pageSize: 10 })
const tips = ref({ list: [], total: 0, page: 1, pageSize: 10 })
const transactions = ref({ list: [], total: 0, page: 1, pageSize: 10 })
const tipDirection = ref('OUT')

const tabs = [
  { key: 'overview', label: '资料与交易', icon: User },
  { key: 'bookshelf', label: '我的书架', icon: Collection },
  { key: 'subscriptions', label: '章节订阅', icon: Reading },
  { key: 'tips', label: '打赏记录', icon: Money },
  { key: 'transactions', label: '账户流水', icon: Wallet },
]
const activeTitle = computed(() => tabs.find((item) => item.key === activeTab.value)?.label || '')
const roleLabel = computed(() => ({ READER: '读者', AUTHOR: '作者', ADMIN: '管理员' }[profile.value?.role] || '读者'))
const avatarText = computed(() => (profile.value?.nickname || profile.value?.username || '读').slice(0, 1))

function money(value) {
  return Number(value || 0).toFixed(2)
}

function time(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function newRequestNo(prefix) {
  const id = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `${prefix}-${id.replaceAll('-', '')}`
}

async function loadOverview() {
  initialLoading.value = true
  initialError.value = ''
  try {
    const [profileData, accountData] = await Promise.all([getProfile(), getAccount()])
    profile.value = profileData
    account.value = accountData
    Object.assign(profileForm, {
      nickname: profileData.nickname || '',
      avatarUrl: profileData.avatarUrl || '',
      email: profileData.email || '',
      phone: profileData.phone || '',
    })
  } catch (error) {
    initialError.value = error?.response?.data?.message || '个人资料加载失败，请重试'
  } finally {
    initialLoading.value = false
  }
}

async function refreshAccount() {
  account.value = await getAccount()
}

async function loadBookshelf() {
  loading.shelf = true
  try {
    bookshelf.value = await getBookshelf({ page: bookshelf.value.page, pageSize: bookshelf.value.pageSize })
  } finally {
    loading.shelf = false
  }
}

async function loadSubscriptions() {
  loading.subscriptions = true
  try {
    subscriptions.value = await getSubscriptions({
      page: subscriptions.value.page,
      pageSize: subscriptions.value.pageSize,
    })
  } finally {
    loading.subscriptions = false
  }
}

async function loadTips() {
  loading.tips = true
  try {
    tips.value = await getTips({
      direction: tipDirection.value,
      page: tips.value.page,
      pageSize: tips.value.pageSize,
    })
  } finally {
    loading.tips = false
  }
}

async function loadTransactions() {
  loading.transactions = true
  try {
    transactions.value = await getTransactions({
      page: transactions.value.page,
      pageSize: transactions.value.pageSize,
    })
  } finally {
    loading.transactions = false
  }
}

function loadTab(key) {
  if (key === 'bookshelf') loadBookshelf()
  if (key === 'subscriptions') loadSubscriptions()
  if (key === 'tips') loadTips()
  if (key === 'transactions') loadTransactions()
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  savingProfile.value = true
  try {
    profile.value = await updateProfile(profileForm)
    ElMessage.success('个人资料已保存')
  } finally {
    savingProfile.value = false
  }
}

async function addShelf() {
  if (!/^\d+$/.test(shelfForm.novelId.trim())) {
    ElMessage.warning('请输入有效的数字作品 ID')
    return
  }
  submitting.shelf = true
  try {
    const result = await addToBookshelf(shelfForm.novelId.trim())
    ElMessage.success(result.added ? '已加入书架' : '这部作品已在书架中，未重复添加')
    shelfForm.novelId = ''
    bookshelf.value.page = 1
    await loadBookshelf()
  } finally {
    submitting.shelf = false
  }
}

async function removeShelf(row) {
  await ElMessageBox.confirm(`确定将《${row.title}》移出书架吗？`, '移出书架', { type: 'warning' })
  await removeFromBookshelf(row.novelId)
  ElMessage.success('已移出书架')
  await loadBookshelf()
}

async function subscribe() {
  if (!/^\d+$/.test(subscribeForm.chapterId.trim())) {
    ElMessage.warning('请输入有效的数字章节 ID')
    return
  }
  submitting.subscribe = true
  try {
    const result = await subscribeChapter({
      chapterId: subscribeForm.chapterId.trim(),
      requestNo: subscribeForm.requestNo,
    })
    ElMessage.success(result.duplicate ? '该章节已订阅，本次未重复扣费' : `订阅成功，已扣 ¥${money(result.paidAmount)}`)
    subscribeForm.chapterId = ''
    subscribeForm.requestNo = newRequestNo('SUB')
    subscriptions.value.page = 1
    await Promise.all([refreshAccount(), loadSubscriptions()])
  } finally {
    submitting.subscribe = false
  }
}

async function refund(row) {
  await ElMessageBox.confirm(
    `将退回 ¥${money(row.paidAmount)}。取消后，当前规则不允许重新订阅该章节，是否继续？`,
    '取消订阅',
    { type: 'warning', confirmButtonText: '确认退款' },
  )
  await cancelSubscription(row.chapterId)
  ElMessage.success('订阅已取消，退款已计入账户流水')
  await Promise.all([refreshAccount(), loadSubscriptions()])
}

async function submitTip() {
  if (!/^\d+$/.test(tipForm.novelId.trim())) {
    ElMessage.warning('请输入有效的数字作品 ID')
    return
  }
  if (tipForm.chapterId && !/^\d+$/.test(tipForm.chapterId.trim())) {
    ElMessage.warning('章节 ID 须为数字，或留空表示整本书打赏')
    return
  }
  submitting.tip = true
  try {
    const result = await tipNovel({
      novelId: tipForm.novelId.trim(),
      chapterId: tipForm.chapterId.trim() || null,
      amount: Number(tipForm.amount),
      message: tipForm.message.trim(),
      requestNo: tipForm.requestNo,
    })
    ElMessage.success(result.duplicate ? '该请求已处理，未重复扣款' : `打赏成功，已支付 ¥${money(result.amount)}`)
    tipForm.message = ''
    tipForm.requestNo = newRequestNo('TIP')
    tips.value.page = 1
    tipDirection.value = 'OUT'
    await Promise.all([refreshAccount(), loadTips()])
  } finally {
    submitting.tip = false
  }
}

async function signOut() {
  try {
    await logout()
  } finally {
    router.replace('/login')
  }
}

watch(activeTab, loadTab)
watch(tipDirection, () => {
  tips.value.page = 1
  loadTips()
})
onMounted(async () => {
  subscribeForm.requestNo = newRequestNo('SUB')
  tipForm.requestNo = newRequestNo('TIP')
  await loadOverview()
})
</script>

<template>
  <div class="center-shell">
    <header class="center-header">
      <RouterLink to="/account" class="center-brand">
        <span class="brand-seal">文</span>
        <span><strong>我的书院</strong><small>个人资料与阅读账目</small></span>
      </RouterLink>
      <nav class="header-links" aria-label="平台导航">
        <RouterLink to="/rankings">榜单广场</RouterLink>
        <RouterLink to="/bookstore">书城</RouterLink>
        <el-button text :icon="SwitchButton" @click="signOut">退出登录</el-button>
      </nav>
    </header>

    <main class="center-main">
      <aside class="account-rail">
        <section class="identity-card">
          <el-avatar :size="68" :src="profile?.avatarUrl || undefined">{{ avatarText }}</el-avatar>
          <h1>{{ profile?.nickname || '读者' }}</h1>
          <p>@{{ profile?.username || '—' }}</p>
          <div class="identity-tags">
            <el-tag size="small" effect="plain">{{ roleLabel }}</el-tag>
            <el-tag size="small" :type="profile?.status === 'ACTIVE' ? 'success' : 'danger'" effect="plain">
              {{ profile?.status === 'ACTIVE' ? '正常' : '不可用' }}
            </el-tag>
          </div>
        </section>

        <section class="balance-ticket">
          <span>可用余额</span>
          <strong><small>¥</small>{{ money(account?.balance) }}</strong>
          <p>订阅、退款与打赏均记入流水</p>
        </section>

        <nav class="center-nav" aria-label="个人中心导航">
          <button
            v-for="item in tabs"
            :key="item.key"
            type="button"
            :class="{ active: activeTab === item.key }"
            @click="activeTab = item.key"
          >
            <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
          </button>
        </nav>
      </aside>

      <section class="center-content">
        <header class="content-heading">
          <div><p>PERSONAL DESK</p><h2>{{ activeTitle }}</h2></div>
          <el-button v-if="activeTab !== 'overview'" @click="loadTab(activeTab)">刷新</el-button>
        </header>

        <div v-if="initialLoading" class="panel"><el-skeleton :rows="7" animated /></div>
        <div v-else-if="initialError" class="panel error-panel">
          <el-alert :title="initialError" type="error" show-icon :closable="false" />
          <el-button type="primary" @click="loadOverview">重新加载</el-button>
        </div>

        <template v-else>
          <div v-if="activeTab === 'overview'" class="overview-grid">
            <section class="panel">
              <div class="panel-title"><h3>基本资料</h3><span>头像使用图片 URL，不上传本地文件</span></div>
              <el-form :model="profileForm" label-position="top" @submit.prevent="saveProfile">
                <el-form-item label="昵称" required><el-input v-model="profileForm.nickname" maxlength="50" /></el-form-item>
                <el-form-item label="头像 URL"><el-input v-model="profileForm.avatarUrl" maxlength="500" placeholder="https://…" /></el-form-item>
                <div class="two-columns">
                  <el-form-item label="邮箱"><el-input v-model="profileForm.email" type="email" /></el-form-item>
                  <el-form-item label="手机号"><el-input v-model="profileForm.phone" maxlength="20" /></el-form-item>
                </div>
                <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
              </el-form>
            </section>

            <div class="trade-stack">
              <section class="panel trade-panel">
                <div class="panel-title"><h3>订阅 VIP 章节</h3><span>价格由章节信息决定</span></div>
                <el-form label-position="top" @submit.prevent="subscribe">
                  <el-form-item label="章节 ID" required>
                    <el-input v-model="subscribeForm.chapterId" placeholder="从作品目录获取章节 ID" />
                  </el-form-item>
                  <el-form-item label="请求号（防止重复扣款）">
                    <el-input v-model="subscribeForm.requestNo" readonly>
                      <template #append><el-button @click="subscribeForm.requestNo = newRequestNo('SUB')">更换</el-button></template>
                    </el-input>
                  </el-form-item>
                  <el-button type="primary" :loading="submitting.subscribe" @click="subscribe">确认订阅并扣费</el-button>
                </el-form>
              </section>

              <section class="panel trade-panel">
                <div class="panel-title"><h3>打赏作品</h3><span>收款人由作品作者自动确定</span></div>
                <el-form label-position="top" @submit.prevent="submitTip">
                  <div class="two-columns">
                    <el-form-item label="作品 ID" required><el-input v-model="tipForm.novelId" /></el-form-item>
                    <el-form-item label="章节 ID（选填）"><el-input v-model="tipForm.chapterId" placeholder="留空打赏整本书" /></el-form-item>
                  </div>
                  <el-form-item label="打赏金额（元）" required>
                    <el-input-number v-model="tipForm.amount" :min="0.01" :max="9999999999.99" :precision="2" :step="1" />
                  </el-form-item>
                  <el-form-item label="留言"><el-input v-model="tipForm.message" maxlength="200" show-word-limit /></el-form-item>
                  <el-form-item label="请求号（防止重复打赏）"><el-input v-model="tipForm.requestNo" readonly /></el-form-item>
                  <el-button type="warning" :loading="submitting.tip" @click="submitTip">确认打赏</el-button>
                </el-form>
              </section>
            </div>
          </div>

          <section v-else-if="activeTab === 'bookshelf'" class="panel">
            <form class="inline-action" @submit.prevent="addShelf">
              <el-input v-model="shelfForm.novelId" placeholder="输入作品 ID 加入书架" clearable />
              <el-button type="primary" native-type="submit" :loading="submitting.shelf">加入书架</el-button>
            </form>
            <div v-loading="loading.shelf" class="section-body">
              <el-empty v-if="!bookshelf.list.length" description="书架还是空的，输入作品 ID 收藏第一部作品" />
              <div v-else class="book-grid">
                <article v-for="book in bookshelf.list" :key="book.novelId" class="book-entry">
                  <div class="book-cover"><img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" /><span v-else>{{ book.title?.slice(0, 1) }}</span></div>
                  <div class="book-copy">
                    <h3>{{ book.title }}</h3><p>{{ book.authorName }} · {{ book.novelStatus === 1 ? '已完结' : '连载中' }}</p>
                    <small>加入于 {{ time(book.createdAt) }}</small>
                    <div class="book-actions">
                      <el-button size="small" @click="router.push(`/novel/${book.novelId}`)">查看作品</el-button>
                      <el-button size="small" type="danger" plain @click="removeShelf(book)">移出</el-button>
                    </div>
                  </div>
                </article>
              </div>
              <el-pagination v-if="bookshelf.total > bookshelf.pageSize" v-model:current-page="bookshelf.page" :page-size="bookshelf.pageSize" :total="bookshelf.total" layout="prev, pager, next" @current-change="loadBookshelf" />
            </div>
          </section>

          <section v-else-if="activeTab === 'subscriptions'" class="panel">
            <el-alert title="重复订阅不会重复扣费；取消订阅会退回原支付金额，且当前规则不允许重新订阅该章节。" type="info" :closable="false" show-icon />
            <el-table v-loading="loading.subscriptions" :data="subscriptions.list" class="data-table">
              <el-table-column label="作品 / 章节" min-width="240"><template #default="{ row }"><strong>{{ row.novelTitle }}</strong><small class="cell-sub">第 {{ row.chapterNo }} 章 · {{ row.chapterTitle }}</small></template></el-table-column>
              <el-table-column label="实付" width="100"><template #default="{ row }">¥{{ money(row.paidAmount) }}</template></el-table-column>
              <el-table-column prop="subscribedAt" label="订阅时间" width="160" :formatter="(row) => time(row.subscribedAt)" />
              <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '已订阅' : '已退款' }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'ACTIVE'" type="danger" text @click="refund(row)">取消订阅</el-button><span v-else class="muted">{{ time(row.refundedAt) }}</span></template></el-table-column>
              <template #empty><el-empty description="暂无订阅记录，可在“资料与交易”中订阅 VIP 章节" /></template>
            </el-table>
            <el-pagination v-if="subscriptions.total > subscriptions.pageSize" v-model:current-page="subscriptions.page" :page-size="subscriptions.pageSize" :total="subscriptions.total" layout="prev, pager, next" @current-change="loadSubscriptions" />
          </section>

          <section v-else-if="activeTab === 'tips'" class="panel">
            <div class="record-toolbar"><el-radio-group v-model="tipDirection"><el-radio-button value="OUT">我的打赏</el-radio-button><el-radio-button value="IN">收到的打赏</el-radio-button></el-radio-group></div>
            <el-table v-loading="loading.tips" :data="tips.list" class="data-table">
              <el-table-column label="作品 / 章节" min-width="220"><template #default="{ row }"><strong>{{ row.novelTitle }}</strong><small class="cell-sub">{{ row.chapterTitle || '整本书打赏' }}</small></template></el-table-column>
              <el-table-column prop="counterpartyName" :label="tipDirection === 'OUT' ? '作者' : '打赏读者'" width="130" />
              <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
              <el-table-column prop="message" label="留言" min-width="140" show-overflow-tooltip />
              <el-table-column label="时间" width="160"><template #default="{ row }">{{ time(row.createdAt) }}</template></el-table-column>
              <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'info'">{{ row.status === 'SUCCESS' ? '成功' : '已退款' }}</el-tag></template></el-table-column>
              <template #empty><el-empty description="暂无打赏记录" /></template>
            </el-table>
            <el-pagination v-if="tips.total > tips.pageSize" v-model:current-page="tips.page" :page-size="tips.pageSize" :total="tips.total" layout="prev, pager, next" @current-change="loadTips" />
          </section>

          <section v-else class="panel">
            <div class="ledger-summary"><span>当前余额</span><strong>¥{{ money(account?.balance) }}</strong><small>每笔流水同时记录变动前后余额</small></div>
            <el-table v-loading="loading.transactions" :data="transactions.list" class="data-table">
              <el-table-column label="业务" width="120"><template #default="{ row }"><el-tag size="small" :type="row.direction === 'IN' ? 'success' : 'warning'" effect="plain">{{ ({ RECHARGE: '充值', SUBSCRIBE: '章节订阅', TIP_OUT: '打赏支出', TIP_IN: '打赏收入', REFUND: '订阅退款', WITHDRAW: '提现', ADJUST_IN: '调账收入', ADJUST_OUT: '调账支出' })[row.bizType] || row.bizType }}</el-tag></template></el-table-column>
              <el-table-column label="发生额" width="120"><template #default="{ row }"><span :class="row.direction === 'IN' ? 'amount-in' : 'amount-out'">{{ row.direction === 'IN' ? '+' : '-' }}¥{{ money(row.amount) }}</span></template></el-table-column>
              <el-table-column label="变动前 → 变动后" width="190"><template #default="{ row }">¥{{ money(row.balanceBefore) }} → ¥{{ money(row.balanceAfter) }}</template></el-table-column>
              <el-table-column prop="remark" label="摘要" min-width="190" show-overflow-tooltip />
              <el-table-column label="时间" width="160"><template #default="{ row }">{{ time(row.createdAt) }}</template></el-table-column>
              <el-table-column prop="txnNo" label="流水号" min-width="220" show-overflow-tooltip />
              <template #empty><el-empty description="暂无账户流水" /></template>
            </el-table>
            <el-pagination v-if="transactions.total > transactions.pageSize" v-model:current-page="transactions.page" :page-size="transactions.pageSize" :total="transactions.total" layout="prev, pager, next" @current-change="loadTransactions" />
          </section>
        </template>
      </section>
    </main>
  </div>
</template>

<style scoped>
.center-shell { min-height: 100vh; color: #26354a; background: #f3f1eb; }
.center-header { display: flex; align-items: center; justify-content: space-between; min-height: 74px; padding: 0 5vw; background: #17233c; border-bottom: 3px solid #c3903f; }
.center-brand { display: flex; align-items: center; gap: 12px; color: #fff; text-decoration: none; }
.brand-seal { display: grid; width: 38px; height: 44px; place-items: center; font: 600 22px/1 "Songti SC", SimSun, serif; background: #3568a8; border-radius: 3px 3px 10px 3px; }
.center-brand > span:last-child { display: flex; flex-direction: column; gap: 3px; }
.center-brand strong { font: 600 19px/1.2 "Songti SC", SimSun, serif; letter-spacing: .12em; }
.center-brand small { color: #9eacc2; font-size: 10px; }
.header-links { display: flex; align-items: center; gap: 22px; }
.header-links a { color: #d5dcea; font-size: 13px; text-decoration: none; }
.header-links a:hover { color: #e3bd78; }
.header-links :deep(.el-button) { color: #d5dcea; }
.center-main { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 28px; max-width: 1400px; margin: 0 auto; padding: 34px 4vw 60px; }
.account-rail { align-self: start; }
.identity-card { padding: 28px 20px 24px; text-align: center; background: #fff; border: 1px solid #dfd9cb; border-radius: 7px 7px 0 0; }
.identity-card :deep(.el-avatar) { color: #fff; font: 600 26px/1 "Songti SC", SimSun, serif; background: #3568a8; }
.identity-card h1 { margin: 15px 0 5px; font: 600 21px/1.3 "Songti SC", SimSun, serif; }
.identity-card p { margin: 0 0 14px; color: #8a8f98; font-size: 12px; }
.identity-tags { display: flex; justify-content: center; gap: 7px; }
.balance-ticket { position: relative; padding: 23px 24px; color: #f5e7ca; background: #233c61; border-top: 1px dashed #8b94a4; }
.balance-ticket::before, .balance-ticket::after { position: absolute; top: -7px; width: 14px; height: 14px; content: ''; background: #f3f1eb; border-radius: 50%; }
.balance-ticket::before { left: -7px; }.balance-ticket::after { right: -7px; }
.balance-ticket > span { font-size: 11px; letter-spacing: .12em; }
.balance-ticket strong { display: block; margin: 8px 0; color: #fff; font: 600 30px/1.2 Georgia, serif; }
.balance-ticket strong small { margin-right: 5px; color: #d6ae6a; font-size: 16px; }
.balance-ticket p { margin: 0; color: #b8c2d1; font-size: 11px; line-height: 1.6; }
.center-nav { display: grid; padding: 10px; background: #fff; border: 1px solid #dfd9cb; border-top: 0; border-radius: 0 0 7px 7px; }
.center-nav button { display: flex; align-items: center; gap: 10px; padding: 12px 13px; color: #606b7c; font: inherit; font-size: 13px; text-align: left; background: transparent; border: 0; border-radius: 5px; cursor: pointer; }
.center-nav button:hover, .center-nav button.active { color: #274f81; background: #edf3fa; }
.center-nav button.active { font-weight: 600; box-shadow: inset 3px 0 #3568a8; }
.center-content { min-width: 0; }
.content-heading { display: flex; align-items: center; justify-content: space-between; min-height: 58px; margin-bottom: 18px; }
.content-heading p { margin: 0 0 5px; color: #ad8a50; font-size: 10px; font-weight: 600; letter-spacing: .2em; }
.content-heading h2 { margin: 0; color: #17233c; font: 600 27px/1.2 "Songti SC", SimSun, serif; }
.overview-grid { display: grid; grid-template-columns: minmax(300px, .85fr) minmax(360px, 1.15fr); gap: 18px; align-items: start; }
.trade-stack { display: grid; gap: 18px; }
.panel { min-width: 0; padding: 24px; background: #fff; border: 1px solid #dfd9cb; border-top: 3px solid #3568a8; border-radius: 7px; box-shadow: 0 8px 24px rgba(23, 35, 60, .04); }
.trade-panel { border-top-color: #c3903f; }
.panel-title { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 20px; padding-bottom: 13px; border-bottom: 1px solid #ece7dc; }
.panel-title h3 { margin: 0; color: #26354a; font: 600 17px/1.4 "Songti SC", SimSun, serif; }
.panel-title span { color: #9299a3; font-size: 11px; }
.two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.inline-action { display: flex; gap: 10px; max-width: 520px; padding-bottom: 20px; border-bottom: 1px solid #ece7dc; }
.section-body { min-height: 240px; padding-top: 20px; }
.book-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.book-entry { display: grid; grid-template-columns: 82px 1fr; gap: 15px; padding: 15px; background: #f8f7f3; border: 1px solid #e8e2d6; border-radius: 6px; }
.book-cover { display: grid; overflow: hidden; width: 82px; height: 110px; place-items: center; color: #fff; font: 600 30px/1 "Songti SC", SimSun, serif; background: #31547c; border-radius: 3px 7px 7px 3px; }
.book-cover img { width: 100%; height: 100%; object-fit: cover; }
.book-copy { min-width: 0; }
.book-copy h3 { overflow: hidden; margin: 2px 0 7px; font: 600 16px/1.4 "Songti SC", SimSun, serif; text-overflow: ellipsis; white-space: nowrap; }
.book-copy p { margin: 0 0 7px; color: #737e8e; font-size: 12px; }
.book-copy small { color: #9b9fA6; font-size: 10px; }
.book-actions { display: flex; gap: 6px; margin-top: 14px; }
.data-table { width: 100%; margin-top: 18px; }
.cell-sub { display: block; margin-top: 5px; color: #8a929e; font-size: 11px; }
.record-toolbar { display: flex; justify-content: flex-end; }
.muted { color: #9a9fa8; font-size: 11px; }
.amount-in { color: #27855b; font-weight: 600; }.amount-out { color: #b06a32; font-weight: 600; }
.ledger-summary { display: flex; align-items: baseline; gap: 16px; padding: 17px 20px; background: #f4f6f9; border-left: 3px solid #c3903f; }
.ledger-summary span { color: #778396; font-size: 12px; }.ledger-summary strong { font: 600 25px/1 Georgia, serif; }.ledger-summary small { margin-left: auto; color: #8e96a2; }
.el-pagination { justify-content: flex-end; margin-top: 20px; }
.error-panel { display: grid; gap: 16px; justify-items: start; }
@media (max-width: 1080px) { .center-main { grid-template-columns: 210px minmax(0, 1fr); gap: 18px; }.overview-grid { grid-template-columns: 1fr; }.book-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .center-header { padding: 12px 18px; }.header-links { gap: 10px; }.header-links a { display: none; }.center-main { display: block; padding: 20px 14px 40px; }.account-rail { margin-bottom: 20px; }.identity-card { padding: 20px; }.center-nav { display: flex; overflow-x: auto; }.center-nav button { flex: 0 0 auto; }.balance-ticket { text-align: center; }.panel { padding: 18px 14px; }.two-columns { grid-template-columns: 1fr; gap: 0; }.panel-title { align-items: flex-start; flex-direction: column; }.ledger-summary { flex-wrap: wrap; }.ledger-summary small { width: 100%; margin-left: 0; } }
@media (prefers-reduced-motion: no-preference) { .panel { animation: arrive .35s ease both; } @keyframes arrive { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } } }
</style>
