<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Bell, Delete, Plus, Refresh, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addBook, getBookshelf, removeBook, updateBook } from '@/api/user'

const loading = ref(false)
const adding = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(8)
const addForm = reactive({ novelId: '' })

function formatTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    const data = await getBookshelf({ page: page.value, pageSize: pageSize.value })
    items.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function add() {
  const novelId = addForm.novelId.trim()
  if (!novelId) {
    ElMessage.warning('请输入小说 ID')
    return
  }
  adding.value = true
  try {
    await addBook(novelId)
    ElMessage.success('已加入书架')
    addForm.novelId = ''
    page.value = 1
    await load()
  } finally {
    adding.value = false
  }
}

async function toggle(row, field, value) {
  const payload = field === 'isTop' ? { isTop: value } : { isNotify: value }
  try {
    await updateBook(row.novelId, payload)
    ElMessage.success(field === 'isTop' ? (value ? '已置顶' : '已取消置顶') : (value ? '已开启更新提醒' : '已关闭更新提醒'))
  } catch {
    await load()
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定将《${row.title}》移出书架吗？`, '移出书架', {
      type: 'warning',
      confirmButtonText: '移出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await removeBook(row.novelId)
  ElMessage.success('已移出书架')
  if (items.value.length === 1 && page.value > 1) page.value -= 1
  await load()
}

function changePage(next) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="shelf-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">COLLECTION / 收藏</p>
        <h1>我的书架</h1>
        <p class="page-description">管理收藏作品、阅读进度和更新提醒。书架与收藏共用同一份数据。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="add-panel">
      <div>
        <p class="panel-kicker">ADD TO SHELF</p>
        <h2>加入一本小说</h2>
        <p>输入 B 模块的小说 ID。作品详情页开发完成后，这里会由详情页直接调用。</p>
      </div>
      <div class="add-form">
        <el-input v-model="addForm.novelId" placeholder="例如 10001" clearable @keyup.enter="add">
          <template #prefix><el-icon><Plus /></el-icon></template>
        </el-input>
        <el-button type="primary" :loading="adding" @click="add">加入书架</el-button>
      </div>
    </section>

    <section class="shelf-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">MY COLLECTION</p>
          <h2>收藏作品 <span>{{ total }}</span></h2>
        </div>
      </div>

      <el-table v-loading="loading" :data="items" row-key="novelId" class="shelf-table">
        <el-table-column label="作品" min-width="300">
          <template #default="scope">
            <div class="book-cell">
              <el-image v-if="scope.row.coverUrl" :src="scope.row.coverUrl" fit="cover" class="book-cover">
                <template #error><div class="book-cover fallback">文</div></template>
              </el-image>
              <div v-else class="book-cover fallback">文</div>
              <div class="book-copy">
                <strong>{{ scope.row.title }}</strong>
                <span>作者 {{ scope.row.authorName }} · {{ scope.row.chapterCount || 0 }} 章</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近阅读" min-width="190">
          <template #default="scope">
            <div class="read-cell">
              <span>{{ scope.row.lastReadChapterTitle || '尚未开始阅读' }}</span>
              <small>{{ formatTime(scope.row.lastReadAt) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="86" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isTop" :active-value="true" :inactive-value="false"
              @change="toggle(scope.row, 'isTop', $event)" />
          </template>
        </el-table-column>
        <el-table-column label="更新提醒" width="104" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isNotify" :active-value="true" :inactive-value="false"
              @change="toggle(scope.row, 'isNotify', $event)" />
          </template>
        </el-table-column>
        <el-table-column label="加入时间" width="164">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="84" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="danger" :icon="Delete" @click="remove(scope.row)">移出</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="书架还是空的，先加入一本小说吧" />
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

    <div class="tip-bar">
      <el-icon><Star /></el-icon>
      <span>置顶和更新提醒会写入 user_bookshelf 的 is_top / is_notify 字段。</span>
      <el-icon><Bell /></el-icon>
    </div>
  </div>
</template>

<style scoped>
.shelf-page {
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
  max-width: 680px;
  margin-bottom: 0;
  color: #6e7b8f;
  font-size: 13px;
  line-height: 1.8;
}

.add-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  padding: 22px 24px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-left: 3px solid #c3903f;
  border-radius: 8px;
}

.add-panel h2,
.panel-heading h2 {
  margin: 0 0 7px;
  color: #17233c;
  font-size: 17px;
}

.add-panel p:last-child {
  margin-bottom: 0;
  color: #8793a5;
  font-size: 12px;
}

.add-form {
  display: flex;
  width: min(100%, 390px);
  gap: 10px;
}

.add-form .el-input {
  flex: 1;
}

.shelf-panel {
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e7ed;
  border-radius: 8px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-heading h2 span {
  color: #3568a8;
  font: 600 14px/1 Georgia, serif;
}

.book-cell {
  display: flex;
  align-items: center;
  gap: 13px;
}

.book-cover {
  width: 42px;
  height: 56px;
  flex: 0 0 42px;
  overflow: hidden;
  border-radius: 3px 3px 7px 3px;
}

.book-cover.fallback {
  display: grid;
  place-items: center;
  color: #3568a8;
  font: 600 19px/1 "Songti SC", "SimSun", serif;
  background: #eaf1f9;
}

.book-copy,
.read-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.book-copy strong {
  overflow: hidden;
  color: #1d2939;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-copy span,
.read-cell small {
  color: #8793a5;
  font-size: 11px;
}

.read-cell span {
  overflow: hidden;
  color: #475467;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.tip-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8793a5;
  font-size: 11px;
}

.tip-bar .el-icon:last-child {
  margin-left: auto;
  color: #c3903f;
}

@media (max-width: 760px) {
  .add-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .add-form {
    width: 100%;
  }

  .shelf-panel {
    padding: 18px 14px;
  }
}
</style>
