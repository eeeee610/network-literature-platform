<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import NovelCard from '@/components/NovelCard.vue'
import {
  getErrorMessage,
  getNovelList,
  getTagList,
  searchNovels,
} from '@/api/novel'

const loading = ref(false)
const error = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const activeKeyword = ref('')
const tagId = ref('')
const sort = ref('update')

const tags = ref([])
const tagLoading = ref(false)
const tagError = ref('')

const sortOptions = [
  { value: 'update', label: '最新更新' },
  { value: 'words', label: '按字数' },
  { value: 'new', label: '最新创建' },
]

async function loadNovels() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (tagId.value) params.tagId = tagId.value
    let data
    if (activeKeyword.value) {
      params.keyword = activeKeyword.value
      data = await searchNovels(params)
    } else {
      params.sort = sort.value
      data = await getNovelList(params)
    }
    list.value = data?.list || []
    total.value = Number(data?.total || 0)
    if (data?.page) page.value = Number(data.page)
    if (data?.pageSize) pageSize.value = Number(data.pageSize)
  } catch (err) {
    error.value = getErrorMessage(err)
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  tagLoading.value = true
  tagError.value = ''
  try {
    tags.value = (await getTagList()) || []
  } catch (err) {
    tagError.value = getErrorMessage(err)
    tags.value = []
  } finally {
    tagLoading.value = false
  }
}

function submitSearch() {
  activeKeyword.value = keyword.value.trim()
  page.value = 1
  loadNovels()
}

function handleSearchClear() {
  if (activeKeyword.value) {
    activeKeyword.value = ''
    page.value = 1
    loadNovels()
  }
}

function handleTagChange() {
  page.value = 1
  loadNovels()
}

function handleSortChange() {
  page.value = 1
  loadNovels()
}

function handlePageChange(value) {
  page.value = value
  loadNovels()
}

function handleSizeChange(value) {
  pageSize.value = value
  page.value = 1
  loadNovels()
}

function resetFilters() {
  keyword.value = ''
  activeKeyword.value = ''
  tagId.value = ''
  sort.value = 'update'
  page.value = 1
  loadNovels()
}

onMounted(() => {
  loadTags()
  loadNovels()
})
</script>

<template>
  <div class="bookstore">
    <header class="page-heading">
      <div>
        <p class="page-eyebrow">BOOKSTORE / 书城</p>
        <h1>发现下一本好书</h1>
        <p class="page-description">
          浏览已通过审核并公开连载或完结的作品，可按书名搜索、按标签筛选，并按更新时间、字数或创建时间排序。
        </p>
      </div>
    </header>

    <section class="filter-bar">
      <div class="search-group">
        <el-input
          v-model="keyword"
          placeholder="按书名搜索（前缀匹配）"
          clearable
          maxlength="50"
          :prefix-icon="Search"
          @keyup.enter="submitSearch"
          @clear="handleSearchClear"
        />
        <el-button type="primary" :loading="loading" @click="submitSearch">搜索</el-button>
      </div>

      <div class="filter-group">
        <el-select
          v-model="tagId"
          placeholder="全部标签"
          clearable
          :loading="tagLoading"
          class="filter-select"
          @change="handleTagChange"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.tagId"
            :label="tag.tagName"
            :value="tag.tagId"
          />
        </el-select>

        <el-select
          v-model="sort"
          class="filter-select"
          :disabled="Boolean(activeKeyword)"
          @change="handleSortChange"
        >
          <el-option
            v-for="item in sortOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-button text @click="resetFilters">重置</el-button>
      </div>
    </section>

    <p v-if="activeKeyword" class="search-hint">
      正在搜索「{{ activeKeyword }}」，搜索结果按书名前缀匹配，排序设置暂不生效。
    </p>

    <el-alert
      v-if="tagError"
      class="inline-alert"
      type="warning"
      :closable="false"
      show-icon
      title="标签加载失败，暂不能按标签筛选"
    >
      <template #default>
        <span class="alert-detail">{{ tagError }}</span>
        <el-button size="small" text type="primary" @click="loadTags">重试</el-button>
      </template>
    </el-alert>

    <div v-if="loading" class="card-grid">
      <div v-for="n in 6" :key="n" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-cover" />
            <div class="skeleton-body">
              <el-skeleton-item variant="h3" style="width: 62%" />
              <el-skeleton-item variant="text" style="width: 40%" />
              <el-skeleton-item variant="text" style="width: 100%" />
              <el-skeleton-item variant="text" style="width: 76%" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="error" class="state-panel">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <div class="state-actions">
        <el-button type="primary" @click="loadNovels">重新加载</el-button>
        <el-button @click="resetFilters">清空筛选</el-button>
      </div>
    </div>

    <template v-else-if="list.length">
      <div class="card-grid">
        <NovelCard v-for="novel in list" :key="novel.novelId" :novel="novel" />
      </div>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </template>

    <div v-else class="state-panel">
      <el-empty
        :description="activeKeyword ? `没有找到与「${activeKeyword}」相关的作品` : '书城暂时没有可展示的作品'"
      />
      <div class="state-actions">
        <el-button v-if="activeKeyword || tagId" @click="resetFilters">清空筛选</el-button>
        <el-button v-else type="primary" @click="loadNovels">刷新</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-heading {
  margin-bottom: 22px;
}

.page-heading h1 {
  margin-bottom: 10px;
  color: var(--ink);
  font: 600 28px/1.3 "Songti SC", "SimSun", serif;
  letter-spacing: 0.02em;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px;
  margin-bottom: 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.search-group {
  display: flex;
  flex: 1 1 320px;
  gap: 10px;
  max-width: 480px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-select {
  width: 148px;
}

.search-hint {
  margin: -4px 0 16px;
  color: var(--muted);
  font-size: 12px;
}

.inline-alert {
  margin-bottom: 16px;
}

.alert-detail {
  margin-right: 8px;
  font-size: 12px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.skeleton-card {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.skeleton-cover {
  width: 100%;
  height: 150px;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
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
  margin: 0 auto 18px;
  text-align: left;
}

.state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 30px 0 4px;
}

@media (max-width: 960px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .card-grid {
    grid-template-columns: 1fr;
  }

  .search-group {
    flex-basis: 100%;
    max-width: none;
  }

  .filter-select {
    flex: 1;
    width: auto;
  }

  .pagination-wrap :deep(.el-pagination__jump) {
    display: none;
  }
}
</style>
