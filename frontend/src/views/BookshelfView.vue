<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的书架</h2>
        <p class="page-desc">收藏作品、记录阅读进度，并设置置顶与更新提醒</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">添加作品</el-button>
    </div>

    <el-card class="section-card" shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="作品" min-width="220">
          <template #default="{ row }">
            <div class="novel-cell">
              <div class="cover">
                <img v-if="row.coverUrl" :src="row.coverUrl" :alt="row.title" />
                <span v-else>封面</span>
              </div>
              <div>
                <div class="novel-title">{{ row.title }}</div>
                <div class="muted">{{ row.totalWords || 0 }} 字</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近阅读" min-width="180">
          <template #default="{ row }">
            <div>{{ row.lastReadTitle || '尚未开始阅读' }}</div>
            <div class="muted">{{ formatTime(row.lastReadAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="最新章节" min-width="180">
          <template #default="{ row }">
            <div>{{ row.latestChapterTitle || '-' }}</div>
            <div class="muted">更新于 {{ formatTime(row.updatedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isTop" @change="(value) => onFlagChange(row, { isTop: value })" />
          </template>
        </el-table-column>
        <el-table-column label="更新提醒" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isNotify" @change="(value) => onFlagChange(row, { isNotify: value })" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="continueReading(row)">继续阅读</el-button>
            <el-button link type="danger" @click="onRemove(row)">移除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">书架还是空的，先添加一部作品吧</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加作品到书架" width="420px">
      <el-form label-position="top">
        <el-form-item label="作品 ID">
          <el-input v-model="novelIdInput" placeholder="请输入作品 ID" clearable />
        </el-form-item>
      </el-form>
      <p class="muted">作品 ID 由作品详情页跳转时携带，也可填写联调数据中的作品 ID（如 1001）。</p>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="onAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addToShelf, listBookshelf, removeFromShelf, updateShelfFlags } from '../api/bookshelf'
import { formatTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const adding = ref(false)
const novelIdInput = ref('')
const query = reactive({ page: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await listBookshelf(query)
    rows.value = data.list
    total.value = data.total
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function onSizeChange() {
  query.page = 1
  load()
}

async function onFlagChange(row, payload) {
  try {
    await updateShelfFlags(row.novelId, payload)
    ElMessage.success('设置已保存')
  } catch (e) {
    if ('isTop' in payload) row.isTop = !payload.isTop
    if ('isNotify' in payload) row.isNotify = !payload.isNotify
  }
}

function continueReading(row) {
  const chapterId = row.lastReadChapterId || row.latestChapterId
  router.push({
    path: '/subscription',
    query: { novelId: row.novelId, ...(chapterId ? { chapterId } : {}) }
  })
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`确定将《${row.title}》移出书架吗？`, '移除确认', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await removeFromShelf(row.novelId)
    ElMessage.success('已移出书架')
    if (rows.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

async function onAdd() {
  const novelId = novelIdInput.value.trim()
  if (!novelId) {
    ElMessage.warning('请输入作品 ID')
    return
  }
  adding.value = true
  try {
    await addToShelf(novelId)
    ElMessage.success('已加入书架')
    dialogVisible.value = false
    novelIdInput.value = ''
    query.page = 1
    load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    adding.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.novel-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.novel-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
