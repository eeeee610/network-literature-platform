<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Money, Star } from '@element-plus/icons-vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  getErrorMessage,
  getNovelChapters,
  getNovelDetail,
} from '@/api/novel'
import {
  addToBookshelf,
  removeFromBookshelf,
  getBookshelf,
  tipNovel,
  getAccount,
} from '@/api/user'
import { isLoggedIn } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const novel = ref(null)

const chaptersLoading = ref(false)
const chaptersError = ref('')
const chapters = ref([])

const coverFailed = ref(false)

// 书架 & 打赏
const shelfLoading = ref(false)
const inShelf = ref(false)
const tipDialogVisible = ref(false)
const tipLoading = ref(false)
const tipAmount = ref(1)
const tipMessage = ref('')
const account = ref(null)

const novelId = computed(() => route.params.novelId)
const loggedIn = isLoggedIn()

const statusText = computed(() => (novel.value?.status === 1 ? '已完结' : '连载中'))
const wordCount = computed(() => {
  const value = Number(novel.value?.totalWords || 0)
  return value >= 10000 ? `${(value / 10000).toFixed(1)} 万字` : `${value} 字`
})
const initial = computed(() => (novel.value?.title || '未').trim().slice(0, 1))
const firstChapter = computed(() => chapters.value[0] || null)
const cover = computed(() =>
  novel.value?.coverUrl && !coverFailed.value ? novel.value.coverUrl : '',
)
const tipRequestNo = computed(
  () => `TIP-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
)

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  coverFailed.value = false
  try {
    novel.value = await getNovelDetail(novelId.value)
    if (loggedIn) await refreshShelfStatus()
  } catch (err) {
    error.value = getErrorMessage(err)
    novel.value = null
  } finally {
    loading.value = false
  }
}

async function loadChapters() {
  chaptersLoading.value = true
  chaptersError.value = ''
  try {
    const data = await getNovelChapters(novelId.value)
    chapters.value = data?.list || []
  } catch (err) {
    chaptersError.value = getErrorMessage(err)
    chapters.value = []
  } finally {
    chaptersLoading.value = false
  }
}

function reloadAll() {
  loadDetail()
  loadChapters()
}

async function refreshShelfStatus() {
  if (!loggedIn || !novelId.value) return
  try {
    const data = await getBookshelf({ page: 1, pageSize: 999 })
    const ids = (data?.list || []).map((b) => String(b.novelId))
    inShelf.value = ids.includes(String(novelId.value))
  } catch {
    // 状态刷新失败不影响主流程
  }
}

async function toggleShelf() {
  if (!loggedIn) {
    goLogin()
    return
  }
  shelfLoading.value = true
  try {
    if (inShelf.value) {
      await ElMessageBox.confirm(
        `将《${novel.value.title}》从书架移出？`,
        '移出书架',
        { type: 'warning' },
      )
      await removeFromBookshelf(novelId.value)
      inShelf.value = false
      ElMessage.success('已移出书架')
    } else {
      await addToBookshelf(novelId.value)
      inShelf.value = true
      ElMessage.success('已加入书架')
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(getErrorMessage(err, '书架操作失败'))
    }
  } finally {
    shelfLoading.value = false
  }
}

async function openTipDialog() {
  if (!loggedIn) {
    goLogin()
    return
  }
  try {
    account.value = await getAccount()
  } catch {
    account.value = null
  }
  tipAmount.value = 1
  tipMessage.value = ''
  tipDialogVisible.value = true
}

async function submitTip() {
  if (tipLoading.value) return
  const amount = Number(tipAmount.value)
  if (!(amount > 0)) {
    ElMessage.warning('请输入有效打赏金额')
    return
  }
  tipLoading.value = true
  try {
    const result = await tipNovel({
      novelId: String(novelId.value),
      amount: amount.toFixed(2),
      message: tipMessage.value.trim(),
      requestNo: tipRequestNo.value,
    })
    const paid = Number(result.amount || amount).toFixed(2)
    ElMessage.success(
      result.duplicate
        ? '该请求已处理，未重复扣款'
        : `打赏成功，已支付 ¥${paid}`,
    )
    tipDialogVisible.value = false
  } catch (err) {
    const msg = getErrorMessage(err, '打赏失败')
    if (/余额不足/.test(msg)) {
      ElMessage.warning(`${msg} — 请先前往「我的书院」充值`)
    } else {
      ElMessage.error(msg)
    }
  } finally {
    tipLoading.value = false
  }
}

watch(
  novelId,
  (value) => {
    if (value) reloadAll()
  },
  { immediate: true },
)
</script>

<template>
  <div class="detail-page">
    <RouterLink to="/bookstore" class="back-link">← 返回书城</RouterLink>

    <div v-if="loading" class="skeleton-hero">
      <el-skeleton animated>
        <template #template>
          <div class="skeleton-layout">
            <el-skeleton-item variant="image" class="skeleton-cover" />
            <div class="skeleton-info">
              <el-skeleton-item variant="h1" style="width: 55%" />
              <el-skeleton-item variant="text" style="width: 34%" />
              <el-skeleton-item variant="text" style="width: 88%" />
              <el-skeleton-item variant="text" style="width: 76%" />
              <el-skeleton-item variant="text" style="width: 42%" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="error" class="state-panel">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <p class="state-note">作品可能不存在、已下架或尚未通过审核。</p>
      <div class="state-actions">
        <el-button type="primary" @click="reloadAll">重试</el-button>
        <el-button @click="$router.push('/bookstore')">返回书城</el-button>
      </div>
    </div>

    <template v-else-if="novel">
      <section class="hero">
        <div class="hero-cover">
          <img
            v-if="cover"
            :src="cover"
            :alt="novel.title"
            @error="coverFailed = true"
          />
          <div v-else class="cover-placeholder" aria-hidden="true">
            <span>{{ initial }}</span>
            <small>NO COVER</small>
          </div>
        </div>

        <div class="hero-info">
          <div class="title-row">
            <h1>{{ novel.title }}</h1>
            <el-tag :type="novel.status === 1 ? 'success' : 'primary'" effect="plain">
              {{ statusText }}
            </el-tag>
          </div>

          <p class="author-line">
            作者 · <strong>{{ novel.authorName || '佚名' }}</strong>
          </p>

          <div class="tag-row">
            <el-tag
              v-for="tag in novel.tags"
              :key="tag.tagId"
              size="small"
              effect="plain"
            >
              {{ tag.tagName }}
            </el-tag>
            <span v-if="!novel.tags || !novel.tags.length" class="muted">暂无标签</span>
          </div>

          <dl class="meta-grid">
            <div>
              <dt>总字数</dt>
              <dd>{{ wordCount }}</dd>
            </div>
            <div>
              <dt>已发布章节</dt>
              <dd>{{ novel.chapterCount ?? 0 }} 章</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatTime(novel.updatedAt) }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatTime(novel.createdAt) }}</dd>
            </div>
          </dl>

          <div class="latest-line">
            <span class="latest-label">最新章节</span>
            <RouterLink
              v-if="novel.latestChapter"
              :to="`/read/${novel.latestChapter.chapterId}`"
              class="latest-link"
            >
              {{ novel.latestChapter.title }}
            </RouterLink>
            <span v-else class="muted">暂无已发布章节</span>
          </div>

          <div class="hero-actions">
            <el-button
              type="primary"
              size="large"
              :disabled="!firstChapter"
              @click="$router.push(`/read/${firstChapter.chapterId}`)"
            >
              开始阅读
            </el-button>
            <el-button
              size="large"
              :type="inShelf ? 'success' : 'default'"
              :icon="inShelf ? Check : Star"
              :loading="shelfLoading"
              @click="toggleShelf"
            >
              {{ inShelf ? '已在书架' : '加入书架' }}
            </el-button>
            <el-button
              type="warning"
              size="large"
              :icon="Money"
              @click="openTipDialog"
            >
              打赏作者
            </el-button>
            <el-button v-if="firstChapter" size="large" @click="loadChapters">
              刷新目录
            </el-button>
          </div>
        </div>
      </section>

      <section class="panel">
        <h2 class="panel-title">作品简介</h2>
        <p class="description">{{ novel.description || '作者尚未填写简介。' }}</p>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2 class="panel-title">章节目录</h2>
          <span class="chapter-total">共 {{ chapters.length }} 章</span>
        </div>

        <div v-if="chaptersLoading" class="chapter-skeleton">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else-if="chaptersError" class="chapter-state">
          <el-alert type="error" :title="chaptersError" show-icon :closable="false" />
          <el-button size="small" type="primary" @click="loadChapters">重新加载目录</el-button>
        </div>

        <el-empty v-else-if="!chapters.length" description="暂无已发布章节" />

        <ul v-else class="chapter-list">
          <li v-for="chapter in chapters" :key="chapter.chapterId">
            <RouterLink :to="`/read/${chapter.chapterId}`" class="chapter-row">
              <span class="chapter-no">{{ chapter.chapterNo }}</span>
              <span class="chapter-name">{{ chapter.title }}</span>
              <span class="chapter-meta">
                <el-tag v-if="chapter.isVip === 1" size="small" type="warning" effect="plain">
                  VIP ¥{{ formatPrice(chapter.vipPrice) }}
                </el-tag>
                <el-tag v-else size="small" type="info" effect="plain">免费</el-tag>
                <span class="chapter-words">{{ chapter.wordCount }} 字</span>
                <span class="chapter-time">{{ formatTime(chapter.publishTime) }}</span>
              </span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </template>

    <el-dialog
      v-model="tipDialogVisible"
      title="打赏作者"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="tip-dialog-body">
        <p class="tip-title">
          为《<strong>{{ novel?.title }}</strong>》打赏
        </p>
        <p v-if="account" class="tip-balance">
          当前余额：<strong>¥{{ Number(account.balance).toFixed(2) }}</strong>
        </p>
        <el-form label-position="top">
          <el-form-item label="打赏金额（元）">
            <el-radio-group v-model="tipAmount" size="default">
              <el-radio-button :value="1">¥1</el-radio-button>
              <el-radio-button :value="5">¥5</el-radio-button>
              <el-radio-button :value="10">¥10</el-radio-button>
              <el-radio-button :value="20">¥20</el-radio-button>
              <el-radio-button :value="50">¥50</el-radio-button>
            </el-radio-group>
            <el-input-number
              v-model="tipAmount"
              :min="0.01"
              :max="99999999.99"
              :precision="2"
              :step="1"
              size="small"
              style="margin-left: 10px"
            />
          </el-form-item>
          <el-form-item label="留言（可选）">
            <el-input
              v-model="tipMessage"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="给作者留个言"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="tipDialogVisible = false">取消</el-button>
        <el-button
          type="warning"
          :loading="tipLoading"
          @click="submitTip"
        >
          确认打赏 ¥{{ Number(tipAmount).toFixed(2) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 16px;
  color: var(--muted);
  font-size: 13px;
  text-decoration: none;
}

.back-link:hover {
  color: var(--accent);
}

.hero {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 28px;
  padding: 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-top: 3px solid var(--accent);
  border-radius: 8px;
  box-shadow: 0 10px 26px rgba(23, 35, 60, 0.05);
}

.hero-cover {
  overflow: hidden;
  aspect-ratio: 3 / 4;
  background: #e9eef5;
  border-radius: 6px;
}

.hero-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.92);
  background:
    radial-gradient(circle at 26% 22%, rgba(255, 255, 255, 0.24), transparent 46%),
    linear-gradient(140deg, #2c4a75 0%, #3568a8 52%, #4b83c0 100%);
}

.cover-placeholder span {
  font: 600 56px/1 "Songti SC", "SimSun", serif;
}

.cover-placeholder small {
  font-size: 9px;
  letter-spacing: 0.28em;
  opacity: 0.7;
}

.title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.title-row h1 {
  margin: 0;
  color: var(--ink);
  font: 600 28px/1.3 "Songti SC", "SimSun", serif;
}

.author-line {
  margin-bottom: 12px;
  color: var(--muted);
  font-size: 13px;
}

.author-line strong {
  color: var(--ink-soft);
  font-weight: 600;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 18px;
}

.muted {
  color: #9aa6b6;
  font-size: 12px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0 0 18px;
}

.meta-grid div {
  padding: 10px 12px;
  background: #f7f9fb;
  border-radius: 5px;
}

.meta-grid dt {
  margin-bottom: 5px;
  color: #8793a5;
  font-size: 11px;
}

.meta-grid dd {
  margin: 0;
  color: #344054;
  font-size: 13px;
}

.latest-line {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 20px;
  font-size: 13px;
  background: var(--accent-light);
  border-radius: 5px;
}

.latest-label {
  color: var(--accent);
  font-weight: 600;
}

.latest-link {
  color: var(--ink-soft);
  text-decoration: none;
}

.latest-link:hover {
  color: var(--accent);
  text-decoration: underline;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.panel {
  padding: 22px 24px;
  margin-top: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-title {
  margin: 0 0 12px;
  color: var(--ink);
  font: 600 18px/1.4 "Songti SC", "SimSun", serif;
}

.panel-head .panel-title {
  margin-bottom: 0;
}

.chapter-total {
  color: var(--muted);
  font-size: 12px;
}

.description {
  margin: 0;
  color: #445068;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.chapter-skeleton {
  padding: 6px 2px;
}

.chapter-state {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.chapter-state :deep(.el-alert) {
  flex: 1;
}

.chapter-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.chapter-list li + li {
  border-top: 1px dashed var(--line);
}

.chapter-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 10px;
  color: inherit;
  text-decoration: none;
  border-radius: 5px;
  transition: background 0.2s ease;
}

.chapter-row:hover {
  background: #f7f9fb;
}

.chapter-no {
  flex: 0 0 34px;
  color: #9aa6b6;
  font: 600 13px/1 Georgia, serif;
  text-align: right;
}

.chapter-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #8793a5;
  font-size: 12px;
}

.chapter-words,
.chapter-time {
  white-space: nowrap;
}

.state-panel {
  padding: 46px 24px;
  text-align: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.state-panel :deep(.el-alert) {
  max-width: 620px;
  margin: 0 auto 14px;
  text-align: left;
}

.state-note {
  margin: 0 0 14px;
  color: var(--muted);
  font-size: 13px;
}

.state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.skeleton-hero {
  padding: 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.skeleton-layout {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 28px;
}

.skeleton-cover {
  width: 100%;
  height: 280px;
}

.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 760px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .hero-cover {
    max-width: 190px;
  }

  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chapter-time {
    display: none;
  }

  .skeleton-layout {
    grid-template-columns: 1fr;
  }

  .skeleton-cover {
    max-width: 190px;
    height: 240px;
  }
}

@media (max-width: 520px) {
  .chapter-row {
    flex-wrap: wrap;
    gap: 8px;
  }

  .chapter-name {
    flex-basis: calc(100% - 48px);
    white-space: normal;
  }

  .chapter-meta {
    padding-left: 48px;
  }
}

.tip-dialog-body {
  padding: 6px 0 4px;
}

.tip-title {
  margin: 0 0 4px;
  color: #445068;
  font-size: 13px;
}

.tip-title strong {
  color: var(--ink);
  font-weight: 600;
}

.tip-balance {
  margin: 0 0 14px;
  color: #8793a5;
  font-size: 12px;
}

.tip-balance strong {
  color: #b06a32;
  font-weight: 600;
}
</style>
