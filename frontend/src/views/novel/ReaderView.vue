<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  getChapter,
  getErrorMessage,
  getNovelChapters,
} from '@/api/novel'

const route = useRoute()
const router = useRouter()

const chapter = ref(null)
const loading = ref(false)
const error = ref('')
const errorText = ref('')

const chapters = ref([])
const chaptersNovelId = ref('')
const chaptersLoading = ref(false)
const chaptersError = ref('')
const drawerVisible = ref(false)

const chapterId = computed(() => String(route.params.chapterId || ''))
const paragraphs = computed(() => {
  const content = chapter.value?.content || ''
  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
})
const isVip = computed(() => chapter.value?.isVip === 1)
const vipBlocked = computed(() =>
  /VIP|订阅|付费|购买|余额/.test(errorText.value),
)
const detailLink = computed(() =>
  chapter.value?.novelId ? `/novel/${chapter.value.novelId}` : '/bookstore',
)

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

async function loadChapter() {
  loading.value = true
  error.value = ''
  errorText.value = ''
  chapter.value = null
  try {
    const data = await getChapter(chapterId.value)
    chapter.value = data
    window.scrollTo({ top: 0 })
    if (data?.novelId) loadChapters(data.novelId)
  } catch (err) {
    errorText.value = getErrorMessage(err)
    error.value = errorText.value
  } finally {
    loading.value = false
  }
}

async function loadChapters(novelId, force = false) {
  if (!novelId) return
  const key = String(novelId)
  if (!force && chaptersNovelId.value === key && chapters.value.length) return
  chaptersLoading.value = true
  chaptersError.value = ''
  try {
    const data = await getNovelChapters(key)
    chapters.value = data?.list || []
    chaptersNovelId.value = key
  } catch (err) {
    chaptersError.value = getErrorMessage(err)
    chapters.value = []
  } finally {
    chaptersLoading.value = false
  }
}

function openDirectory() {
  drawerVisible.value = true
  if (chapter.value?.novelId) {
    loadChapters(chapter.value.novelId, !chapters.value.length)
  }
}

function goTo(id) {
  if (!id) return
  drawerVisible.value = false
  router.push(`/read/${id}`)
}

watch(
  chapterId,
  (value) => {
    if (value) loadChapter()
  },
  { immediate: true },
)
</script>

<template>
  <div class="reader-page">
    <div class="reader-toolbar">
      <RouterLink :to="detailLink" class="back-link">← 返回详情</RouterLink>
      <div class="toolbar-actions">
        <el-button size="small" @click="openDirectory">目录</el-button>
        <el-button
          size="small"
          :disabled="!chapter?.prevChapterId"
          @click="goTo(chapter.prevChapterId)"
        >
          上一章
        </el-button>
        <el-button
          size="small"
          :disabled="!chapter?.nextChapterId"
          @click="goTo(chapter.nextChapterId)"
        >
          下一章
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="reading-panel">
      <el-skeleton animated :rows="10" />
    </div>

    <div v-else-if="error" class="reading-panel state-panel">
      <template v-if="vipBlocked">
        <el-empty description="该章节为 VIP 章节，暂不可阅读" />
        <p class="state-note">
          订阅、余额与扣费由 A 模块负责，本页不发起支付或扣费。请先在 A 模块完成订阅后再回来阅读。
        </p>
      </template>
      <template v-else>
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <p class="state-note">章节可能不存在、未发布、未通过审核或已下架。</p>
      </template>
      <div class="state-actions">
        <el-button type="primary" @click="loadChapter">重试</el-button>
        <el-button @click="router.push(detailLink)">返回作品详情</el-button>
        <el-button text @click="router.push('/bookstore')">去书城</el-button>
      </div>
    </div>

    <article v-else-if="chapter" class="reading-panel">
      <header class="chapter-head">
        <p class="novel-name">{{ chapter.novelTitle }}</p>
        <h1>{{ chapter.title }}</h1>
        <div class="chapter-sub">
          <span>第 {{ chapter.chapterNo }} 章</span>
          <span>{{ chapter.wordCount }} 字</span>
          <span>{{ formatTime(chapter.publishTime) }}</span>
          <el-tag v-if="isVip" size="small" type="warning" effect="plain">
            VIP ¥{{ formatPrice(chapter.vipPrice) }}
          </el-tag>
          <el-tag v-else size="small" type="info" effect="plain">免费</el-tag>
        </div>
      </header>

      <el-alert
        v-if="isVip"
        class="vip-notice"
        type="warning"
        :closable="false"
        show-icon
        title="本章为 VIP 章节"
      >
        <template #default>
          订阅状态与扣费由 A 模块负责（价格 ¥{{ formatPrice(chapter.vipPrice) }}），本页仅展示正文，不发起扣费。
        </template>
      </el-alert>

      <div v-if="paragraphs.length" class="chapter-content">
        <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
      </div>
      <el-empty v-else description="本章暂无正文内容" />

      <footer class="chapter-foot">
        <el-button :disabled="!chapter.prevChapterId" @click="goTo(chapter.prevChapterId)">
          上一章
          <span v-if="chapter.prevChapterTitle" class="foot-title">· {{ chapter.prevChapterTitle }}</span>
        </el-button>
        <el-button
          type="primary"
          :disabled="!chapter.nextChapterId"
          @click="goTo(chapter.nextChapterId)"
        >
          下一章
          <span v-if="chapter.nextChapterTitle" class="foot-title">· {{ chapter.nextChapterTitle }}</span>
        </el-button>
      </footer>
    </article>

    <el-drawer
      v-model="drawerVisible"
      title="章节目录"
      direction="rtl"
      size="360px"
      append-to-body
    >
      <div v-if="chaptersLoading" class="drawer-skeleton">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="chaptersError" class="drawer-state">
        <el-alert type="error" :title="chaptersError" show-icon :closable="false" />
        <el-button
          size="small"
          type="primary"
          @click="loadChapters(chapter.novelId, true)"
        >
          重新加载
        </el-button>
      </div>

      <el-empty v-else-if="!chapters.length" description="暂无已发布章节" />

      <ul v-else class="drawer-list">
        <li v-for="item in chapters" :key="item.chapterId">
          <button
            type="button"
            class="drawer-item"
            :class="{ 'is-active': String(item.chapterId) === chapterId }"
            @click="goTo(item.chapterId)"
          >
            <span class="drawer-no">{{ item.chapterNo }}</span>
            <span class="drawer-name">{{ item.title }}</span>
            <el-tag v-if="item.isVip === 1" size="small" type="warning" effect="plain">
              VIP
            </el-tag>
          </button>
        </li>
      </ul>
    </el-drawer>
  </div>
</template>

<style scoped>
.reader-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.back-link {
  color: var(--muted);
  font-size: 13px;
  text-decoration: none;
}

.back-link:hover {
  color: var(--accent);
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.reading-panel {
  max-width: 820px;
  min-height: 420px;
  margin: 0 auto;
  padding: 40px 52px 34px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-top: 3px solid var(--accent);
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(23, 35, 60, 0.05);
}

.chapter-head {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.novel-name {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.chapter-head h1 {
  margin-bottom: 12px;
  color: var(--ink);
  font: 600 26px/1.4 "Songti SC", "SimSun", serif;
}

.chapter-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #8793a5;
  font-size: 12px;
}

.vip-notice {
  margin-top: 20px;
}

.chapter-content {
  padding: 10px 0 4px;
  color: #2b3547;
  font-size: 17px;
  line-height: 2.05;
}

.chapter-content p {
  margin: 0 0 1.1em;
  text-indent: 2em;
  word-break: break-word;
}

.chapter-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 22px;
  margin-top: 12px;
  border-top: 1px dashed var(--line);
}

.chapter-foot .el-button {
  max-width: 48%;
}

.foot-title {
  font-size: 12px;
  opacity: 0.8;
}

.state-panel {
  padding: 46px 32px;
  text-align: center;
}

.state-panel :deep(.el-alert) {
  max-width: 580px;
  margin: 0 auto 16px;
  text-align: left;
}

.state-note {
  max-width: 560px;
  margin: 4px auto 18px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.drawer-skeleton {
  padding: 4px;
}

.drawer-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.drawer-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 10px;
  font: inherit;
  text-align: left;
  color: #344054;
  background: none;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.drawer-item:hover {
  background: #f7f9fb;
}

.drawer-item.is-active {
  color: var(--accent);
  background: var(--accent-light);
}

.drawer-no {
  flex: 0 0 30px;
  color: #9aa6b6;
  font: 600 12px/1 Georgia, serif;
  text-align: right;
}

.drawer-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .reading-panel {
    padding: 26px 20px 24px;
  }

  .chapter-head h1 {
    font-size: 21px;
  }

  .chapter-content {
    font-size: 16px;
    line-height: 1.95;
  }

  .chapter-foot {
    flex-direction: column;
  }

  .chapter-foot .el-button {
    max-width: none;
    margin-left: 0;
  }
}
</style>
