<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createChapter,
  createNovel,
  getAuthorChapter,
  getAuthorChapters,
  getAuthorNovels,
  getErrorMessage,
  getNovelDetail,
  getTagList,
  offlineChapter,
  publishChapter,
  updateChapter,
  updateNovel,
} from '@/api/novel'
import { isLoggedIn } from '@/api/auth'

const router = useRouter()
const loggedIn = ref(isLoggedIn())

const novels = ref([])
const novelsLoading = ref(false)
const novelsError = ref('')
const novelsTotal = ref(0)
const novelPage = ref(1)
const novelPageSize = ref(10)
const selectedNovelId = ref('')
const selectedNovel = computed(
  () => novels.value.find((item) => item.novelId === selectedNovelId.value) || null,
)

const chapters = ref([])
const chaptersLoading = ref(false)
const chaptersError = ref('')
const chaptersTotal = ref(0)
const chapterPage = ref(1)
const chapterPageSize = ref(10)
const workingChapterId = ref('')

const tags = ref([])
const tagsError = ref('')
const tagsLoading = ref(false)

const novelDialogVisible = ref(false)
const novelMode = ref('create')
const novelFormRef = ref(null)
const novelSubmitting = ref(false)
const novelHint = ref('')
const novelForm = reactive({
  novelId: '',
  title: '',
  coverUrl: '',
  description: '',
  tagIds: [],
})

const chapterDialogVisible = ref(false)
const chapterMode = ref('create')
const chapterFormRef = ref(null)
const chapterSubmitting = ref(false)
const chapterFetchingId = ref('')
const chapterHint = ref('')
const chapterForm = reactive({
  chapterId: '',
  title: '',
  content: '',
  isVip: 0,
  vipPrice: 0,
})

const novelRules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) callback(new Error('作品标题不能为空'))
        else if (text.length > 120) callback(new Error('作品标题不能超过 120 字'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  coverUrl: [{ max: 255, message: '封面地址不能超过 255 个字符', trigger: 'blur' }],
  description: [{ max: 1000, message: '简介不能超过 1000 字', trigger: 'blur' }],
}

const chapterRules = {
  title: [
    { required: true, message: '请输入章节标题', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const text = String(value || '').trim()
        if (!text) callback(new Error('章节标题不能为空'))
        else if (text.length > 120) callback(new Error('章节标题不能超过 120 字'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  content: [
    {
      validator: (_rule, value, callback) => {
        if (!String(value || '').trim()) callback(new Error('章节正文不能为空'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  vipPrice: [
    {
      validator: (_rule, value, callback) => {
        if (chapterForm.isVip !== 1) {
          callback()
          return
        }
        if (!(Number(value) > 0)) callback(new Error('VIP 章节价格必须大于 0'))
        else callback()
      },
      trigger: 'change',
    },
  ],
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function formatWords(value) {
  const count = Number(value || 0)
  return count >= 10000 ? `${(count / 10000).toFixed(1)} 万字` : `${count} 字`
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function novelStatusTag(novel) {
  if (novel.status === 2) return { label: '已下架', type: 'info' }
  if (novel.status === 1) return { label: '已完结', type: 'success' }
  return { label: '连载中', type: 'primary' }
}

function novelAuditTag(novel) {
  if (novel.auditStatus === 1) return { label: '审核通过', type: 'success' }
  if (novel.auditStatus === 2) return { label: '审核驳回', type: 'danger' }
  return { label: '待审核', type: 'warning' }
}

function chapterStatusTag(row) {
  if (row.status === 2) return { label: '已下架', type: 'info' }
  if (row.status === 1) return { label: '已发布', type: 'success' }
  if (row.auditStatus === 0) return { label: '待审核', type: 'warning' }
  if (row.auditStatus === 2) return { label: '审核驳回', type: 'danger' }
  if (row.auditStatus === 1) return { label: '待发布', type: 'primary' }
  return { label: '草稿', type: 'info' }
}

function coverInitial(title) {
  return String(title || '未').trim().slice(0, 1)
}

async function loadTags() {
  tagsLoading.value = true
  tagsError.value = ''
  try {
    tags.value = (await getTagList()) || []
  } catch (err) {
    tagsError.value = getErrorMessage(err)
  } finally {
    tagsLoading.value = false
  }
}

async function loadNovels() {
  if (!loggedIn.value) return
  novelsLoading.value = true
  novelsError.value = ''
  try {
    const data = await getAuthorNovels({
      page: novelPage.value,
      pageSize: novelPageSize.value,
    })
    novels.value = data?.list || []
    novelsTotal.value = Number(data?.total || 0)
    if (data?.page) novelPage.value = Number(data.page)
    if (!novels.value.some((item) => item.novelId === selectedNovelId.value)) {
      selectedNovelId.value = novels.value[0]?.novelId || ''
    }
  } catch (err) {
    novelsError.value = getErrorMessage(err)
    novels.value = []
    novelsTotal.value = 0
    selectedNovelId.value = ''
  } finally {
    novelsLoading.value = false
  }
}

async function loadChapters() {
  if (!selectedNovel.value) return
  chaptersLoading.value = true
  chaptersError.value = ''
  try {
    const data = await getAuthorChapters(selectedNovel.value.novelId, {
      page: chapterPage.value,
      pageSize: chapterPageSize.value,
    })
    chapters.value = data?.list || []
    chaptersTotal.value = Number(data?.total || 0)
  } catch (err) {
    chaptersError.value = getErrorMessage(err)
    chapters.value = []
    chaptersTotal.value = 0
  } finally {
    chaptersLoading.value = false
  }
}

function selectNovel(novel) {
  if (selectedNovelId.value === novel.novelId) return
  selectedNovelId.value = novel.novelId
}

function handleNovelPageChange(value) {
  novelPage.value = value
  loadNovels()
}

function handleNovelSizeChange(value) {
  novelPageSize.value = value
  novelPage.value = 1
  loadNovels()
}

function handleChapterPageChange(value) {
  chapterPage.value = value
  loadChapters()
}

function handleChapterSizeChange(value) {
  chapterPageSize.value = value
  chapterPage.value = 1
  loadChapters()
}

function openCreateNovel() {
  novelMode.value = 'create'
  Object.assign(novelForm, {
    novelId: '',
    title: '',
    coverUrl: '',
    description: '',
    tagIds: [],
  })
  novelHint.value = '新建作品提交后为「待审核」状态，审核通过前不会出现在书城。'
  novelDialogVisible.value = true
  nextTick(() => novelFormRef.value?.clearValidate())
}

async function openEditNovel(novel) {
  novelMode.value = 'edit'
  Object.assign(novelForm, {
    novelId: novel.novelId,
    title: novel.title || '',
    coverUrl: novel.coverUrl || '',
    description: '',
    tagIds: (novel.tags || []).map((tag) => tag.tagId),
  })
  novelHint.value = '正在读取作品详情…'
  novelDialogVisible.value = true
  nextTick(() => novelFormRef.value?.clearValidate())

  if (novel.auditStatus === 1 && novel.status !== 2) {
    try {
      const data = await getNovelDetail(novel.novelId)
      novelForm.description = data?.description || ''
      novelForm.coverUrl = data?.coverUrl || ''
      novelForm.tagIds = (data?.tags || []).map((tag) => tag.tagId)
      novelHint.value = ''
    } catch (err) {
      novelHint.value = '读取作品详情失败，请重新填写简介后再保存。'
    }
  } else {
    novelHint.value =
      '该作品当前不在公开可见状态，接口不返回简介；保存会整体覆盖，请重新填写完整简介，否则简介会被清空。'
  }
}

async function submitNovel() {
  if (novelSubmitting.value) return
  try {
    await novelFormRef.value?.validate()
  } catch {
    return
  }
  novelSubmitting.value = true
  try {
    const payload = {
      title: novelForm.title.trim(),
      coverUrl: novelForm.coverUrl.trim() || null,
      description: novelForm.description.trim() || null,
      tagIds: [...novelForm.tagIds],
    }
    if (novelMode.value === 'create') {
      const data = await createNovel(payload)
      ElMessage.success('作品已创建，等待审核')
      novelPage.value = 1
      await loadNovels()
      if (data?.novelId) selectedNovelId.value = data.novelId
    } else {
      await updateNovel(novelForm.novelId, payload)
      ElMessage.success('作品已保存，已重新进入待审核')
      await loadNovels()
    }
    novelDialogVisible.value = false
  } catch (err) {
    // 错误提示由请求拦截器统一展示，保留弹窗便于修正后重试
  } finally {
    novelSubmitting.value = false
  }
}

function openCreateChapter() {
  if (!selectedNovel.value) return
  chapterMode.value = 'create'
  Object.assign(chapterForm, {
    chapterId: '',
    title: '',
    content: '',
    isVip: 0,
    vipPrice: 0,
  })
  chapterHint.value = '新建章节保存后为草稿，需审核通过后才能发布。'
  chapterDialogVisible.value = true
  nextTick(() => chapterFormRef.value?.clearValidate())
}

async function openEditChapter(row) {
  if (chapterFetchingId.value) return
  chapterFetchingId.value = row.chapterId
  let data = null
  try {
    data = await getAuthorChapter(row.chapterId)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '读取章节正文失败，请手动填写正文'))
  } finally {
    chapterFetchingId.value = ''
  }

  chapterMode.value = 'edit'
  Object.assign(chapterForm, {
    chapterId: row.chapterId,
    title: data?.title ?? row.title ?? '',
    content: data?.content || '',
    isVip: Number(data?.isVip ?? row.isVip) === 1 ? 1 : 0,
    vipPrice: Number(data?.vipPrice ?? row.vipPrice ?? 0) || 0,
  })
  chapterHint.value = data
    ? ''
    : '未能读取章节正文，请在下方手动填写完整正文后再保存；直接保存会覆盖原正文。'
  chapterDialogVisible.value = true
  nextTick(() => chapterFormRef.value?.clearValidate())
}

function handleVipChange(value) {
  if (value !== 1) {
    chapterForm.vipPrice = 0
  }
  nextTick(() => chapterFormRef.value?.validateField('vipPrice').catch(() => {}))
}

async function submitChapter() {
  if (chapterSubmitting.value) return
  try {
    await chapterFormRef.value?.validate()
  } catch {
    return
  }
  if (chapterMode.value === 'create' && !selectedNovel.value) {
    ElMessage.warning('请先选择作品')
    return
  }
  const isVip = chapterForm.isVip === 1 ? 1 : 0
  const vipPrice = isVip === 1 ? Number(Number(chapterForm.vipPrice).toFixed(2)) : 0
  if (isVip === 1 && !(vipPrice > 0)) {
    ElMessage.warning('VIP 章节价格必须大于 0')
    return
  }
  chapterSubmitting.value = true
  try {
    const payload = {
      title: chapterForm.title.trim(),
      content: chapterForm.content,
      isVip,
      vipPrice,
    }
    if (chapterMode.value === 'create') {
      await createChapter({ ...payload, novelId: selectedNovel.value.novelId })
      ElMessage.success('章节草稿已保存')
    } else {
      await updateChapter(chapterForm.chapterId, payload)
      ElMessage.success('章节已保存')
    }
    chapterDialogVisible.value = false
    await loadChapters()
    await loadNovels()
  } catch (err) {
    // 错误提示由请求拦截器统一展示
  } finally {
    chapterSubmitting.value = false
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确认发布《${row.title}》？`, '发布章节', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  workingChapterId.value = row.chapterId
  try {
    await publishChapter(row.chapterId)
    ElMessage.success('章节已发布')
    await loadChapters()
    await loadNovels()
  } catch (err) {
    // 错误提示由请求拦截器统一展示
  } finally {
    workingChapterId.value = ''
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(
      `确认下架《${row.title}》？下架后读者将无法阅读。`,
      '下架章节',
      {
        type: 'warning',
        confirmButtonText: '下架',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  workingChapterId.value = row.chapterId
  try {
    await offlineChapter(row.chapterId)
    ElMessage.success('章节已下架')
    await loadChapters()
    await loadNovels()
  } catch (err) {
    // 错误提示由请求拦截器统一展示
  } finally {
    workingChapterId.value = ''
  }
}

watch(selectedNovelId, (value) => {
  chapterPage.value = 1
  chapters.value = []
  chaptersError.value = ''
  chaptersTotal.value = 0
  if (value) loadChapters()
})

onMounted(() => {
  loadTags()
  if (loggedIn.value) loadNovels()
})
</script>

<template>
  <div class="workbench">
    <header class="page-heading">
      <div>
        <p class="page-eyebrow">AUTHOR / 创作台</p>
        <h1>我的创作台</h1>
        <p class="page-description">
          管理本人作品与章节草稿、发布与下架。作者身份由登录会话解析，页面不提交 authorId。
        </p>
      </div>
      <div v-if="loggedIn" class="heading-actions">
        <el-button :icon="Refresh" :loading="novelsLoading" @click="loadNovels">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateNovel">新建作品</el-button>
      </div>
    </header>

    <section v-if="!loggedIn" class="token-guide">
      <el-empty description="请先登录后再使用创作台">
        <p class="guide-text">
          创作台接口需要登录会话，登录用户的角色须为 AUTHOR 或 ADMIN。未登录前不会发起任何写请求。
        </p>
        <div class="guide-actions">
          <el-button type="primary" @click="router.push({ path: '/login', query: { redirect: '/author' } })">去登录</el-button>
          <el-button @click="router.push('/bookstore')">先去书城看看</el-button>
        </div>
      </el-empty>
    </section>

    <template v-else>
      <el-alert
        v-if="tagsError"
        class="inline-alert"
        type="warning"
        :closable="false"
        show-icon
        title="标签加载失败，新建或编辑作品时可能无法选择标签"
      >
        <template #default>
          <span class="alert-detail">{{ tagsError }}</span>
          <el-button size="small" text type="primary" @click="loadTags">重试</el-button>
        </template>
      </el-alert>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h2 class="panel-title">我的作品</h2>
            <span class="panel-sub">共 {{ novelsTotal }} 本，含待审核、驳回与已下架</span>
          </div>
        </div>

        <div v-if="novelsLoading" class="novel-skeleton">
          <div v-for="n in 3" :key="n" class="skeleton-item">
            <el-skeleton animated>
              <template #template>
                <div class="skeleton-row">
                  <el-skeleton-item variant="image" class="skeleton-thumb" />
                  <div class="skeleton-lines">
                    <el-skeleton-item variant="h3" style="width: 50%" />
                    <el-skeleton-item variant="text" style="width: 80%" />
                    <el-skeleton-item variant="text" style="width: 60%" />
                  </div>
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <div v-else-if="novelsError" class="state-inline">
          <el-alert type="error" :title="novelsError" show-icon :closable="false" />
          <div class="state-actions">
            <el-button type="primary" size="small" @click="loadNovels">重试</el-button>
            <el-button size="small" @click="router.push('/account')">我的书院</el-button>
          </div>
        </div>

        <el-empty v-else-if="!novels.length" description="还没有作品，先创建第一本吧">
          <el-button type="primary" @click="openCreateNovel">新建作品</el-button>
        </el-empty>

        <template v-else>
          <div class="novel-grid">
            <article
              v-for="novel in novels"
              :key="novel.novelId"
              class="novel-item"
              :class="{ 'is-selected': novel.novelId === selectedNovelId }"
            >
              <div class="novel-thumb">
                <img
                  v-if="novel.coverUrl"
                  :src="novel.coverUrl"
                  :alt="novel.title"
                  loading="lazy"
                />
                <div v-else class="thumb-placeholder" aria-hidden="true">
                  {{ coverInitial(novel.title) }}
                </div>
              </div>

              <div class="novel-info">
                <h3>{{ novel.title }}</h3>
                <div class="tag-row">
                  <el-tag size="small" :type="novelStatusTag(novel).type" effect="plain">
                    {{ novelStatusTag(novel).label }}
                  </el-tag>
                  <el-tag size="small" :type="novelAuditTag(novel).type" effect="plain">
                    {{ novelAuditTag(novel).label }}
                  </el-tag>
                </div>
                <p class="meta-line">
                  {{ formatWords(novel.totalWords) }} · {{ novel.chapterCount ?? 0 }} 章
                </p>
                <p class="meta-line muted">更新于 {{ formatTime(novel.updatedAt) }}</p>
              </div>

              <div class="novel-actions">
                <el-button
                  size="small"
                  :type="novel.novelId === selectedNovelId ? 'primary' : 'default'"
                  @click="selectNovel(novel)"
                >
                  管理章节
                </el-button>
                <el-button size="small" @click="openEditNovel(novel)">编辑</el-button>
                <el-button
                  v-if="novel.auditStatus === 1 && novel.status !== 2"
                  size="small"
                  text
                  @click="router.push(`/novel/${novel.novelId}`)"
                >
                  公开页
                </el-button>
              </div>
            </article>
          </div>

          <div class="pagination-wrap">
            <el-pagination
              :current-page="novelPage"
              :page-size="novelPageSize"
              :page-sizes="[10, 20, 50]"
              :total="novelsTotal"
              background
              layout="total, sizes, prev, pager, next"
              @current-change="handleNovelPageChange"
              @size-change="handleNovelSizeChange"
            />
          </div>
        </template>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h2 class="panel-title">
              章节管理
              <span v-if="selectedNovel" class="panel-title-extra">《{{ selectedNovel.title }}》</span>
            </h2>
            <span class="panel-sub">草稿、待审、已发布与已下架章节均在此列出</span>
          </div>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            :disabled="!selectedNovel"
            @click="openCreateChapter"
          >
            新建章节
          </el-button>
        </div>

        <el-empty v-if="!selectedNovel" description="请先在上方选择一本作品" />

        <div v-else-if="chaptersLoading" class="chapter-skeleton">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="chaptersError" class="state-inline">
          <el-alert type="error" :title="chaptersError" show-icon :closable="false" />
          <div class="state-actions">
            <el-button type="primary" size="small" @click="loadChapters">重试</el-button>
          </div>
        </div>

        <el-empty v-else-if="!chapters.length" description="该作品还没有章节">
          <el-button type="primary" @click="openCreateChapter">新建章节</el-button>
        </el-empty>

        <template v-else>
          <div class="table-scroll">
            <el-table :data="chapters" row-key="chapterId" style="width: 100%">
              <el-table-column label="章节" min-width="220">
                <template #default="{ row }">
                  <span class="chapter-no">{{ row.chapterNo }}</span>
                  <span class="chapter-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="chapterStatusTag(row).type" effect="plain">
                    {{ chapterStatusTag(row).label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="定价" width="110">
                <template #default="{ row }">
                  <span v-if="row.isVip === 1" class="vip-price">VIP ¥{{ formatPrice(row.vipPrice) }}</span>
                  <span v-else class="muted">免费</span>
                </template>
              </el-table-column>
              <el-table-column label="字数" width="90">
                <template #default="{ row }">{{ row.wordCount }} 字</template>
              </el-table-column>
              <el-table-column label="更新时间" width="150">
                <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" min-width="190">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status !== 2"
                    size="small"
                    text
                    type="primary"
                    :loading="chapterFetchingId === row.chapterId"
                    @click="openEditChapter(row)"
                  >
                    改稿
                  </el-button>
                  <el-button
                    v-if="row.status === 0"
                    size="small"
                    text
                    type="success"
                    :disabled="row.auditStatus !== 1"
                    :loading="workingChapterId === row.chapterId"
                    :title="row.auditStatus === 1 ? '发布章节' : '需审核通过后才能发布'"
                    @click="handlePublish(row)"
                  >
                    发布
                  </el-button>
                  <el-button
                    v-if="row.status === 1"
                    size="small"
                    text
                    type="danger"
                    :loading="workingChapterId === row.chapterId"
                    @click="handleOffline(row)"
                  >
                    下架
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-wrap">
            <el-pagination
              :current-page="chapterPage"
              :page-size="chapterPageSize"
              :page-sizes="[10, 20, 50]"
              :total="chaptersTotal"
              background
              layout="total, sizes, prev, pager, next"
              @current-change="handleChapterPageChange"
              @size-change="handleChapterSizeChange"
            />
          </div>
        </template>
      </section>
    </template>

    <el-dialog
      v-model="novelDialogVisible"
      :title="novelMode === 'create' ? '新建作品' : '编辑作品'"
      width="540px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="novelHint"
        class="form-hint"
        type="info"
        :closable="false"
        show-icon
        :title="novelHint"
      />
      <el-form
        ref="novelFormRef"
        :model="novelForm"
        :rules="novelRules"
        label-width="88px"
        @submit.prevent
      >
        <el-form-item label="作品标题" prop="title">
          <el-input
            v-model="novelForm.title"
            maxlength="120"
            show-word-limit
            placeholder="请输入书名，最多 120 字"
          />
        </el-form-item>
        <el-form-item label="封面地址" prop="coverUrl">
          <el-input
            v-model="novelForm.coverUrl"
            maxlength="255"
            placeholder="https://... 可留空使用占位封面"
          />
        </el-form-item>
        <el-form-item label="作品简介" prop="description">
          <el-input
            v-model="novelForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="最多 1000 字"
          />
        </el-form-item>
        <el-form-item label="作品标签" prop="tagIds">
          <el-select
            v-model="novelForm.tagIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="tagsLoading"
            placeholder="可多选，仅启用标签可选"
            style="width: 100%"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.tagId"
              :label="tag.tagName"
              :value="tag.tagId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="novelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="novelSubmitting" @click="submitNovel">
          {{ novelMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="chapterDialogVisible"
      :title="chapterMode === 'create' ? '新建章节草稿' : '修改章节'"
      width="660px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="chapterHint"
        class="form-hint"
        type="info"
        :closable="false"
        show-icon
        :title="chapterHint"
      />
      <el-form
        ref="chapterFormRef"
        :model="chapterForm"
        :rules="chapterRules"
        label-width="88px"
        @submit.prevent
      >
        <el-form-item label="章节标题" prop="title">
          <el-input
            v-model="chapterForm.title"
            maxlength="120"
            show-word-limit
            placeholder="例如：第二十二章 归途"
          />
        </el-form-item>
        <el-form-item label="VIP 设置">
          <el-switch
            v-model="chapterForm.isVip"
            :active-value="1"
            :inactive-value="0"
            active-text="VIP"
            inactive-text="免费"
            @change="handleVipChange"
          />
          <span class="vip-tip">非 VIP 章节价格固定为 0.00</span>
        </el-form-item>
        <el-form-item label="VIP 价格" prop="vipPrice">
          <el-input-number
            v-model="chapterForm.vipPrice"
            :min="0"
            :precision="2"
            :step="1"
            :disabled="chapterForm.isVip === 0"
            controls-position="right"
            style="width: 180px"
          />
          <span class="vip-tip">单位：元</span>
        </el-form-item>
        <el-form-item label="章节正文" prop="content">
          <div class="content-field">
            <el-input
              v-model="chapterForm.content"
              type="textarea"
              :rows="12"
              resize="vertical"
              placeholder="请输入章节正文，保存后字数由后端统计"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="chapterSubmitting" @click="submitChapter">
          {{ chapterMode === 'create' ? '保存草稿' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.page-heading h1 {
  margin-bottom: 10px;
  color: var(--ink);
  font: 600 28px/1.3 "Songti SC", "SimSun", serif;
}

.heading-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.token-guide {
  padding: 40px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-top: 3px solid var(--gold);
  border-radius: 8px;
}

.guide-text {
  max-width: 560px;
  margin: 0 auto 18px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.9;
}

.guide-text code {
  padding: 1px 5px;
  color: var(--ink-soft);
  background: #f2f5f9;
  border-radius: 3px;
}

.guide-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.inline-alert {
  margin-bottom: 16px;
}

.alert-detail {
  margin-right: 8px;
  font-size: 12px;
}

.panel {
  padding: 22px 24px;
  margin-bottom: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-title {
  margin: 0 0 5px;
  color: var(--ink);
  font: 600 18px/1.4 "Songti SC", "SimSun", serif;
}

.panel-title-extra {
  color: var(--accent);
  font-weight: 500;
}

.panel-sub {
  color: var(--muted);
  font-size: 12px;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.novel-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
  background: #fbfcfe;
  border: 1px solid var(--line);
  border-radius: 7px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.novel-item.is-selected {
  background: #f6faff;
  border-color: #b9cde6;
  box-shadow: inset 3px 0 0 var(--accent);
}

.novel-thumb {
  flex: 0 0 62px;
  width: 62px;
  height: 82px;
  overflow: hidden;
  background: #e9eef5;
  border-radius: 4px;
}

.novel-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  display: grid;
  height: 100%;
  place-items: center;
  color: #fff;
  font: 600 24px/1 "Songti SC", "SimSun", serif;
  background: linear-gradient(140deg, #2c4a75, #4b83c0);
}

.novel-info {
  flex: 1;
  min-width: 0;
}

.novel-info h3 {
  margin: 0 0 7px;
  overflow: hidden;
  color: var(--ink);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.meta-line {
  margin: 0 0 4px;
  color: #4a5a70;
  font-size: 12px;
}

.meta-line.muted,
.muted {
  color: #93a0b1;
}

.novel-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  flex-shrink: 0;
}

.novel-actions .el-button {
  margin-left: 0;
}

.novel-skeleton {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.skeleton-item {
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 7px;
}

.skeleton-row {
  display: flex;
  gap: 14px;
}

.skeleton-thumb {
  flex: 0 0 62px;
  width: 62px;
  height: 82px;
}

.skeleton-lines {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.state-inline {
  padding: 8px 0;
}

.state-inline :deep(.el-alert) {
  margin-bottom: 12px;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-scroll {
  overflow-x: auto;
}

.chapter-no {
  display: inline-block;
  min-width: 30px;
  margin-right: 10px;
  color: #9aa6b6;
  font: 600 12px/1 Georgia, serif;
  text-align: right;
}

.chapter-title {
  color: var(--ink-soft);
  font-size: 13px;
}

.vip-price {
  color: #a9762b;
  font-size: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

.form-hint {
  margin-bottom: 16px;
}

.vip-tip {
  margin-left: 10px;
  color: #93a0b1;
  font-size: 12px;
}

.content-field {
  width: 100%;
}

@media (max-width: 860px) {
  .novel-grid,
  .novel-skeleton {
    grid-template-columns: 1fr;
  }

  .page-heading {
    flex-direction: column;
  }
}

@media (max-width: 600px) {
  .panel {
    padding: 18px 16px;
  }

  .novel-item {
    flex-wrap: wrap;
  }

  .novel-actions {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .heading-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
