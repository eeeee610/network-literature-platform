<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Promotion,
  RefreshRight,
  Star,
} from '@element-plus/icons-vue'
import { currentUser, isLoggedIn } from '@/api/auth'
import {
  createComment,
  deleteComment,
  getComments,
  getPublicReviews,
  getRatingSummary,
  likeComment,
  saveRating,
  unlikeComment,
} from '@/api/interaction'

const route = useRoute()
const router = useRouter()
const novelId = computed(() => String(route.params.novelId || ''))

const summary = ref(null)
const summaryLoading = ref(true)
const summaryError = ref('')

const activeTab = ref('comment')

const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 10
const commentsLoading = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)

const replyTarget = ref(null)
const replyContent = ref('')
const submittingReply = ref(false)

const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewPageSize = 5
const reviewsLoading = ref(false)

const ratingForm = reactive({ score: 0, reviewContent: '' })
const ratingSaving = ref(false)

const user = computed(() => currentUser())
const loggedIn = computed(() => isLoggedIn())
const averageScore = computed(() => Number(summary.value?.avgScore || 0))
const ratingCount = computed(() => summary.value?.ratingCount || 0)

async function loadSummary() {
  summaryLoading.value = true
  summaryError.value = ''
  try {
    summary.value = await getRatingSummary(novelId.value)
    ratingForm.score = summary.value.myScore || 0
    ratingForm.reviewContent = summary.value.myReviewContent || ''
  } catch {
    summaryError.value = '作品不存在或互动数据暂时无法加载，请确认作品 ID 与后端服务。'
  } finally {
    summaryLoading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    const data = await getComments({
      novelId: novelId.value,
      page: commentPage.value,
      pageSize: commentPageSize,
    })
    comments.value = data.list
    commentTotal.value = data.total
  } catch {
    comments.value = []
    commentTotal.value = 0
  } finally {
    commentsLoading.value = false
  }
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const data = await getPublicReviews({
      novelId: novelId.value,
      page: reviewPage.value,
      pageSize: reviewPageSize,
    })
    reviews.value = data.list
    reviewTotal.value = data.total
  } catch {
    reviews.value = []
    reviewTotal.value = 0
  } finally {
    reviewsLoading.value = false
  }
}

function requireLogin() {
  if (loggedIn.value) {
    return true
  }
  ElMessage.warning('请先登录后再参与互动')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function submitRating() {
  if (!requireLogin()) {
    return
  }
  if (!ratingForm.score) {
    ElMessage.warning('请先选择 1~5 星评分')
    return
  }
  ratingSaving.value = true
  try {
    summary.value = await saveRating({
      novelId: novelId.value,
      score: ratingForm.score,
      reviewContent: ratingForm.reviewContent,
    })
    ratingForm.score = summary.value.myScore || ratingForm.score
    ratingForm.reviewContent = summary.value.myReviewContent || ''
    ElMessage.success(
      summary.value.myReviewContent ? '评分已保存，短评待审核通过后公开展示' : '评分已保存',
    )
    await loadReviews()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    ratingSaving.value = false
  }
}

async function submitComment() {
  if (!requireLogin()) {
    return
  }
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submittingComment.value = true
  try {
    await createComment({ novelId: novelId.value, content })
    commentContent.value = ''
    ElMessage.success('评论已提交，审核通过后展示')
    await loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    submittingComment.value = false
  }
}

function startReply(comment) {
  if (!requireLogin()) {
    return
  }
  if (replyTarget.value?.commentId === comment.commentId) {
    replyTarget.value = null
    return
  }
  replyTarget.value = comment
  replyContent.value = ''
}

async function submitReply(comment) {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submittingReply.value = true
  try {
    await createComment({
      novelId: novelId.value,
      chapterId: comment.chapterId || undefined,
      parentId: comment.commentId,
      content,
    })
    replyTarget.value = null
    replyContent.value = ''
    ElMessage.success('回复已提交，审核通过后展示')
    await loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  } finally {
    submittingReply.value = false
  }
}

async function toggleLike(comment) {
  if (!requireLogin()) {
    return
  }
  try {
    if (comment.liked) {
      await unlikeComment(comment.commentId)
      comment.liked = false
      comment.likeCount = Math.max(0, comment.likeCount - 1)
    } else {
      await likeComment(comment.commentId)
      comment.liked = true
      comment.likeCount += 1
    }
  } catch {
    // 错误提示由 request.js 统一处理
  }
}

async function removeComment(comment) {
  try {
    await ElMessageBox.confirm(
      '删除后该评论将不再公开展示，确定继续吗？',
      '删除评论',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteComment(comment.commentId)
    ElMessage.success('评论已删除')
    await loadComments()
  } catch {
    // 错误提示由 request.js 统一处理
  }
}

function isMine(comment) {
  return Boolean(user.value) && String(comment.user?.userId) === String(user.value.userId)
}

function floorNumber(index) {
  return (commentPage.value - 1) * commentPageSize + index + 1
}

function formatTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function auditLabel(status) {
  return { 0: '待审核', 1: '已通过', 2: '已驳回' }[status] || ''
}

function auditTagType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function changeCommentPage(page) {
  commentPage.value = page
  loadComments()
}

function changeReviewPage(page) {
  reviewPage.value = page
  loadReviews()
}

onMounted(() => {
  loadSummary()
  loadComments()
  loadReviews()
})
</script>

<template>
  <div class="interaction-page">
    <el-alert
      v-if="summaryError"
      :title="summaryError"
      type="error"
      show-icon
      :closable="false"
      class="interaction-alert"
    >
      <template #default>
        <el-button size="small" :icon="RefreshRight" @click="loadSummary">重新加载</el-button>
        <RouterLink class="alert-link" to="/rankings">返回榜单广场</RouterLink>
      </template>
    </el-alert>

    <section v-if="summaryLoading" class="interaction-skeleton">
      <el-skeleton :rows="8" animated />
    </section>

    <template v-else>
      <section class="novel-hero">
        <div class="hero-copy">
          <p class="hero-eyebrow">NOVEL INTERACTION / 作品互动</p>
          <h1>{{ summary?.novelTitle || '作品互动' }}</h1>
          <p class="hero-meta">作品 ID {{ novelId }} · 评论盖楼 · 评分与短评</p>
          <p class="hero-desc">
            发表你的书评与回复，为作品打分并留下短评。评论与短评经审核通过后公开展示。
          </p>
        </div>
        <div class="hero-score">
          <div class="score-number">
            <strong>{{ averageScore.toFixed(1) }}</strong>
            <span>/ 5.0</span>
          </div>
          <el-rate
            :model-value="averageScore"
            disabled
            allow-half
            size="small"
            class="hero-rate"
          />
          <p>{{ ratingCount }} 人参与评分</p>
        </div>
      </section>

      <div class="interaction-grid">
        <section class="main-column">
          <div class="tab-bar" role="tablist">
            <button
              type="button"
              class="tab-button"
              :class="{ active: activeTab === 'comment' }"
              @click="activeTab = 'comment'"
            >
              <el-icon><ChatDotRound /></el-icon>
              书评盖楼
              <span class="tab-count">{{ commentTotal }}</span>
            </button>
            <button
              type="button"
              class="tab-button"
              :class="{ active: activeTab === 'review' }"
              @click="activeTab = 'review'"
            >
              <el-icon><Star /></el-icon>
              短评区
              <span class="tab-count">{{ reviewTotal }}</span>
            </button>
          </div>

          <template v-if="activeTab === 'comment'">
            <div class="composer">
              <template v-if="loggedIn">
                <div class="composer-head">
                  <span class="composer-avatar">{{ (user?.nickname || user?.username || '读').slice(0, 1) }}</span>
                  <div>
                    <strong>{{ user?.nickname || user?.username }}</strong>
                    <p>以书评开启一层新楼，审核通过后展示。</p>
                  </div>
                </div>
                <el-input
                  v-model="commentContent"
                  type="textarea"
                  :rows="3"
                  maxlength="1000"
                  show-word-limit
                  placeholder="写下你对这本书的看法……"
                />
                <div class="composer-actions">
                  <el-button type="primary" :loading="submittingComment" :icon="Promotion" @click="submitComment">
                    发表书评
                  </el-button>
                </div>
              </template>
              <template v-else>
                <div class="login-prompt">
                  <div>
                    <strong>登录后参与评论盖楼</strong>
                    <p>你可以发表书评、回复他人并为喜欢的评论点赞。</p>
                  </div>
                  <el-button type="primary" plain @click="router.push({ path: '/login', query: { redirect: route.fullPath } })">
                    去登录
                  </el-button>
                </div>
              </template>
            </div>

            <el-skeleton v-if="commentsLoading" :rows="5" animated class="comment-skeleton" />

            <template v-else>
              <article
                v-for="(comment, index) in comments"
                :key="comment.commentId"
                class="comment-card"
              >
                <div class="comment-head">
                  <span class="comment-avatar">
                    <img v-if="comment.user?.avatarUrl" :src="comment.user.avatarUrl" alt="" />
                    <template v-else>{{ (comment.user?.nickname || '读').slice(0, 1) }}</template>
                  </span>
                  <div class="comment-author">
                    <div class="author-line">
                      <strong>{{ comment.user?.nickname || '匿名读者' }}</strong>
                      <el-tag v-if="comment.top" size="small" effect="plain" type="warning">置顶</el-tag>
                    </div>
                    <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                  </div>
                  <span class="floor-tag">{{ floorNumber(index) }} 楼</span>
                </div>

                <p class="comment-content">{{ comment.content }}</p>

                <div class="comment-actions">
                  <button type="button" class="action-button" :class="{ liked: comment.liked }" @click="toggleLike(comment)">
                    <span class="like-icon">{{ comment.liked ? '♥' : '♡' }}</span>
                    {{ comment.likeCount }}
                  </button>
                  <button type="button" class="action-button" @click="startReply(comment)">
                    回复
                  </button>
                  <button
                    v-if="isMine(comment)"
                    type="button"
                    class="action-button danger"
                    @click="removeComment(comment)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </button>
                  <span class="reply-count">{{ comment.replyCount }} 条回复</span>
                </div>

                <div v-if="replyTarget?.commentId === comment.commentId" class="reply-composer">
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    :rows="2"
                    maxlength="1000"
                    placeholder="友善回复，共建讨论氛围……"
                  />
                  <div class="reply-actions">
                    <el-button size="small" @click="replyTarget = null">取消</el-button>
                    <el-button
                      size="small"
                      type="primary"
                      :loading="submittingReply"
                      @click="submitReply(comment)"
                    >
                      提交回复
                    </el-button>
                  </div>
                </div>

                <div v-if="comment.replies?.length" class="reply-list">
                  <div v-for="reply in comment.replies" :key="reply.commentId" class="reply-item">
                    <span class="reply-avatar">{{ (reply.user?.nickname || '读').slice(0, 1) }}</span>
                    <div class="reply-body">
                      <div class="reply-head">
                        <strong>{{ reply.user?.nickname || '匿名读者' }}</strong>
                        <span v-if="reply.replyToNickname" class="reply-to">
                          回复 @{{ reply.replyToNickname }}
                        </span>
                        <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                      </div>
                      <p class="reply-content">{{ reply.content }}</p>
                      <div class="comment-actions">
                        <button
                          type="button"
                          class="action-button"
                          :class="{ liked: reply.liked }"
                          @click="toggleLike(reply)"
                        >
                          <span class="like-icon">{{ reply.liked ? '♥' : '♡' }}</span>
                          {{ reply.likeCount }}
                        </button>
                        <button
                          v-if="isMine(reply)"
                          type="button"
                          class="action-button danger"
                          @click="removeComment(reply)"
                        >
                          <el-icon><Delete /></el-icon>
                          删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </article>

              <el-empty
                v-if="comments.length === 0"
                description="还没有公开书评，来写下第一层楼吧"
              />

              <el-pagination
                v-if="commentTotal > commentPageSize"
                class="comment-pagination"
                background
                layout="prev, pager, next, total"
                :total="commentTotal"
                :page-size="commentPageSize"
                :current-page="commentPage"
                @current-change="changeCommentPage"
              />
            </template>
          </template>

          <template v-else>
            <el-skeleton v-if="reviewsLoading" :rows="4" animated />
            <template v-else>
              <article v-for="review in reviews" :key="review.ratingId" class="review-card">
                <span class="comment-avatar">
                  <img v-if="review.avatarUrl" :src="review.avatarUrl" alt="" />
                  <template v-else>{{ (review.nickname || '读').slice(0, 1) }}</template>
                </span>
                <div class="review-body">
                  <div class="review-head">
                    <strong>{{ review.nickname || '匿名读者' }}</strong>
                    <el-rate :model-value="review.score" disabled size="small" />
                    <span class="comment-time">{{ formatTime(review.createdAt) }}</span>
                  </div>
                  <p class="review-content">{{ review.reviewContent }}</p>
                </div>
              </article>
              <el-empty v-if="reviews.length === 0" description="暂无公开短评，评分后写下短评吧" />
              <el-pagination
                v-if="reviewTotal > reviewPageSize"
                class="comment-pagination"
                background
                layout="prev, pager, next, total"
                :total="reviewTotal"
                :page-size="reviewPageSize"
                :current-page="reviewPage"
                @current-change="changeReviewPage"
              />
            </template>
          </template>
        </section>

        <aside class="side-column">
          <section class="rating-card">
            <div class="rating-card-head">
              <div>
                <p class="panel-eyebrow">MY RATING</p>
                <h2>我的评分</h2>
              </div>
              <el-tag
                v-if="summary?.myReviewAuditStatus !== null && summary?.myReviewAuditStatus !== undefined"
                :type="auditTagType(summary.myReviewAuditStatus)"
                size="small"
                effect="light"
              >
                {{ auditLabel(summary.myReviewAuditStatus) }}
              </el-tag>
            </div>

            <template v-if="loggedIn">
              <el-rate
                v-model="ratingForm.score"
                :allow-half="false"
                :texts="['很差', '较差', '一般', '推荐', '力荐']"
                show-text
                class="rating-input"
              />
              <el-input
                v-model="ratingForm.reviewContent"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="可选：写一句短评（审核后展示）"
                class="rating-textarea"
              />
              <el-button
                type="primary"
                class="rating-submit"
                :loading="ratingSaving"
                @click="submitRating"
              >
                {{ summary?.myScore ? '更新我的评分' : '提交评分' }}
              </el-button>
              <p class="rating-note">评分可随时修改；修改短评后会重新进入待审核状态。</p>
            </template>
            <template v-else>
              <p class="rating-guest">登录后可打分并留下短评。</p>
              <el-button
                type="primary"
                class="rating-submit"
                @click="router.push({ path: '/login', query: { redirect: route.fullPath } })"
              >
                登录后评分
              </el-button>
            </template>
          </section>

          <section class="guide-card">
            <p class="panel-eyebrow">GUIDE</p>
            <h2>互动须知</h2>
            <ul>
              <li>评论支持一级书评与直接回复，最多两级盖楼。</li>
              <li>新评论、回复与短评默认待审核，通过后公开展示。</li>
              <li>点赞可取消，点赞数由数据库触发器实时维护。</li>
              <li>评分统计以 novel_rating_stat 为唯一权威来源。</li>
            </ul>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.interaction-alert {
  margin-bottom: 20px;
}

.alert-link {
  margin-left: 12px;
  color: var(--accent);
  font-size: 12px;
  text-decoration: none;
}

.interaction-skeleton {
  padding: 30px;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-radius: 8px;
}

.novel-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 30px;
  margin-bottom: 24px;
  padding: 34px 36px;
  background:
    radial-gradient(circle at 88% 20%, rgba(195, 144, 63, 0.14), transparent 34%),
    #fffdf8;
  border: 1px solid #e6dfd2;
  border-top: 3px solid var(--ink);
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(60, 48, 30, 0.05);
}

.hero-eyebrow {
  margin-bottom: 10px;
  color: #a89c88;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.hero-copy h1 {
  margin: 0 0 9px;
  color: var(--ink);
  font: 600 34px/1.25 "Songti SC", "SimSun", serif;
  letter-spacing: 0.03em;
}

.hero-meta {
  margin-bottom: 10px;
  color: #b0894a;
  font-size: 11px;
  letter-spacing: 0.05em;
}

.hero-desc {
  max-width: 620px;
  margin: 0;
  color: #7d7466;
  font-size: 13px;
  line-height: 1.9;
}

.hero-score {
  flex: 0 0 auto;
  min-width: 176px;
  padding: 18px 20px;
  text-align: center;
  background: #f8f2e6;
  border-radius: 6px;
}

.score-number {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.score-number strong {
  color: var(--ink);
  font: 600 38px/1 Georgia, "Times New Roman", serif;
}

.score-number span {
  color: #a89c88;
  font-size: 11px;
}

.hero-rate {
  margin: 8px 0 6px;
}

.hero-score p {
  margin: 0;
  color: #8a8172;
  font-size: 11px;
}

.interaction-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 332px;
  gap: 22px;
  align-items: start;
}

.main-column {
  min-width: 0;
}

.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  padding: 4px;
  background: #ece5d8;
  border-radius: 7px;
}

.tab-button {
  display: inline-flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px 14px;
  color: #6e6455;
  font-size: 13px;
  background: transparent;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.tab-button.active {
  color: var(--ink);
  background: #fffdf8;
  box-shadow: 0 2px 8px rgba(60, 48, 30, 0.08);
}

.tab-count {
  padding: 1px 7px;
  color: #8a8172;
  font-size: 10px;
  background: #f0e9db;
  border-radius: 999px;
}

.composer,
.comment-card,
.review-card {
  margin-bottom: 14px;
  padding: 22px 24px;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-radius: 8px;
}

.composer {
  border-top: 3px solid var(--accent);
}

.composer-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.composer-head strong {
  color: var(--ink);
  font-size: 14px;
}

.composer-head p {
  margin: 2px 0 0;
  color: #a89c88;
  font-size: 11px;
}

.composer-avatar,
.comment-avatar,
.reply-avatar {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  overflow: hidden;
  color: #fff;
  font: 600 15px/1 Georgia, serif;
  background: var(--accent);
  border-radius: 50%;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.login-prompt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.login-prompt strong {
  color: var(--ink);
  font-size: 14px;
}

.login-prompt p {
  margin: 4px 0 0;
  color: #8a8172;
  font-size: 12px;
}

.comment-skeleton {
  padding: 24px;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-radius: 8px;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-author {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.author-line {
  display: flex;
  align-items: center;
  gap: 7px;
}

.author-line strong {
  color: var(--ink);
  font-size: 14px;
}

.comment-time {
  color: #a89c88;
  font-size: 10px;
}

.floor-tag {
  color: #b0894a;
  font: 600 11px/1 Georgia, serif;
  letter-spacing: 0.04em;
}

.comment-content {
  margin: 14px 0 12px;
  color: #3d3529;
  font-size: 14px;
  line-height: 1.95;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.action-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  color: #8a8172;
  font-size: 12px;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.action-button:hover,
.action-button.liked {
  color: #c0504d;
}

.action-button.danger:hover {
  color: #b0312c;
}

.like-icon {
  font-size: 14px;
  line-height: 1;
}

.reply-count {
  margin-left: auto;
  color: #b3a894;
  font-size: 11px;
}

.reply-composer {
  margin-top: 14px;
  padding: 14px;
  background: #f8f3e9;
  border-radius: 6px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.reply-list {
  margin-top: 16px;
  padding-top: 4px;
  border-top: 1px dashed #e6dfd2;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 13px 0 0;
}

.reply-avatar {
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  font-size: 12px;
  background: #a8987b;
}

.reply-body {
  min-width: 0;
  flex: 1;
}

.reply-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.reply-head strong {
  color: var(--ink);
  font-size: 12px;
}

.reply-to {
  color: #a89c88;
  font-size: 11px;
}

.reply-content {
  margin: 7px 0 6px;
  color: #4a4134;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-pagination {
  justify-content: center;
  margin-top: 20px;
}

.review-card {
  display: flex;
  gap: 13px;
}

.review-body {
  min-width: 0;
  flex: 1;
}

.review-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
}

.review-head strong {
  color: var(--ink);
  font-size: 13px;
}

.review-content {
  margin: 9px 0 0;
  color: #4a4134;
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.side-column {
  position: sticky;
  top: 96px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rating-card,
.guide-card {
  padding: 22px 22px 20px;
  background: #fffdf8;
  border: 1px solid #e6dfd2;
  border-radius: 8px;
}

.rating-card {
  border-top: 3px solid var(--gold);
}

.rating-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.panel-eyebrow {
  margin-bottom: 6px;
  color: #a89c88;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.rating-card h2,
.guide-card h2 {
  margin: 0;
  color: var(--ink);
  font: 600 19px/1.3 "Songti SC", "SimSun", serif;
}

.rating-input {
  margin-bottom: 6px;
}

.rating-textarea {
  margin-top: 8px;
}

.rating-submit {
  width: 100%;
  margin-top: 14px;
}

.rating-note {
  margin: 12px 0 0;
  color: #a89c88;
  font-size: 10px;
  line-height: 1.7;
}

.rating-guest {
  margin: 4px 0 0;
  color: #8a8172;
  font-size: 12px;
}

.guide-card ul {
  display: grid;
  gap: 9px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.guide-card li {
  position: relative;
  padding-left: 13px;
  color: #7d7466;
  font-size: 11px;
  line-height: 1.8;
}

.guide-card li::before {
  position: absolute;
  top: 8px;
  left: 0;
  width: 4px;
  height: 4px;
  content: "";
  background: var(--gold);
  border-radius: 50%;
}

@media (max-width: 1080px) {
  .interaction-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    position: static;
  }
}

@media (max-width: 720px) {
  .novel-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 26px 22px;
  }

  .hero-copy h1 {
    font-size: 27px;
  }

  .hero-score {
    width: 100%;
  }

  .composer,
  .comment-card,
  .review-card {
    padding: 18px 16px;
  }
}
</style>
