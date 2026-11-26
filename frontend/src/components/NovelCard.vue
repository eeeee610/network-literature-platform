<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  novel: {
    type: Object,
    required: true,
  },
})

const coverFailed = ref(false)
watch(
  () => props.novel.coverUrl,
  () => {
    coverFailed.value = false
  },
)

const cover = computed(() =>
  !coverFailed.value && props.novel.coverUrl ? props.novel.coverUrl : '',
)
const initial = computed(() => (props.novel.title || '未').trim().slice(0, 1))
const statusText = computed(() => (props.novel.status === 1 ? '已完结' : '连载中'))
const wordCount = computed(() => {
  const value = Number(props.novel.totalWords || 0)
  return value >= 10000 ? `${(value / 10000).toFixed(1)} 万字` : `${value} 字`
})
const updatedAt = computed(() => formatTime(props.novel.updatedAt))

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <RouterLink :to="`/novel/${novel.novelId}`" class="novel-card">
    <div class="cover-wrap">
      <img
        v-if="cover"
        :src="cover"
        :alt="novel.title"
        class="cover-image"
        loading="lazy"
        @error="coverFailed = true"
      />
      <div v-else class="cover-placeholder" aria-hidden="true">
        <span>{{ initial }}</span>
        <small>NO COVER</small>
      </div>
      <span class="status-chip">{{ statusText }}</span>
    </div>

    <div class="card-body">
      <h3 class="novel-title">{{ novel.title }}</h3>
      <p class="novel-author">作者 · {{ novel.authorName || '佚名' }}</p>

      <div v-if="novel.tags && novel.tags.length" class="tag-row">
        <el-tag
          v-for="tag in novel.tags.slice(0, 3)"
          :key="tag.tagId"
          size="small"
          effect="plain"
        >
          {{ tag.tagName }}
        </el-tag>
      </div>
      <div v-else class="tag-row tag-row--empty">暂无标签</div>

      <p class="latest-chapter">
        {{ novel.latestChapterTitle || '暂无已发布章节' }}
      </p>

      <div class="card-meta">
        <span>{{ wordCount }}</span>
        <span>{{ updatedAt }}</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.novel-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: inherit;
  text-decoration: none;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(23, 35, 60, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.novel-card:hover {
  transform: translateY(-3px);
  border-color: #c9d7e8;
  box-shadow: 0 14px 30px rgba(23, 35, 60, 0.1);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #e9eef5;
}

.cover-image {
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
  gap: 8px;
  color: rgba(255, 255, 255, 0.92);
  background:
    radial-gradient(circle at 24% 20%, rgba(255, 255, 255, 0.24), transparent 46%),
    linear-gradient(140deg, #2c4a75 0%, #3568a8 52%, #4b83c0 100%);
}

.cover-placeholder span {
  font: 600 42px/1 "Songti SC", "SimSun", serif;
}

.cover-placeholder small {
  font-size: 9px;
  letter-spacing: 0.28em;
  opacity: 0.7;
}

.status-chip {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 9px;
  color: #fff;
  font-size: 11px;
  background: rgba(23, 35, 60, 0.72);
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 14px 16px 16px;
}

.novel-title {
  margin: 0 0 6px;
  overflow: hidden;
  color: var(--ink);
  font: 600 16px/1.35 "Songti SC", "SimSun", serif;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.novel-author {
  margin: 0 0 10px;
  color: var(--muted);
  font-size: 12px;
}

.tag-row {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-row--empty {
  color: #a4afbe;
  font-size: 11px;
  align-items: center;
}

.latest-chapter {
  margin: 0 0 12px;
  overflow: hidden;
  color: #4a5a70;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 10px;
  color: #8793a5;
  font-size: 11px;
  border-top: 1px dashed var(--line);
}
</style>
