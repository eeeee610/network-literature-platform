import request from './request'

// ---------------------------------------------------------------------------
// 读者端：评论盖楼 / 评分 / 榜单
// ---------------------------------------------------------------------------

export function getComments(params) {
  return request.get('/interaction/comment', { params })
}

export function createComment(payload) {
  return request.post('/interaction/comment', payload)
}

export function deleteComment(commentId) {
  return request.delete(`/interaction/comment/${commentId}`)
}

export function likeComment(commentId) {
  return request.post(`/interaction/comment/${commentId}/like`)
}

export function unlikeComment(commentId) {
  return request.delete(`/interaction/comment/${commentId}/like`)
}

export function getRatingSummary(novelId) {
  return request.get(`/interaction/rating/summary/${novelId}`)
}

export function saveRating(payload) {
  return request.post('/interaction/rating', payload)
}

export function getPublicReviews(params) {
  return request.get('/interaction/rating/review', { params })
}

export function getRankings() {
  return request.get('/interaction/ranking')
}

export function getRankingDetail(rankingCode, params) {
  return request.get(`/interaction/ranking/${rankingCode}`, { params })
}

export function getRankingPeriods(rankingCode, params) {
  return request.get(`/interaction/ranking/${rankingCode}/period`, { params })
}

// ---------------------------------------------------------------------------
// 后台端：评论审核 / 短评审核 / 榜单配置与生成
// ---------------------------------------------------------------------------

export function getAdminComments(params) {
  return request.get('/admin/comment', { params })
}

export function auditComment(commentId, payload) {
  return request.post(`/admin/comment/${commentId}/audit`, payload)
}

export function changeCommentStatus(commentId, status) {
  return request.post(`/admin/comment/${commentId}/status`, { status })
}

export function getAdminReviews(params) {
  return request.get('/admin/rating/review', { params })
}

export function auditReview(ratingId, payload) {
  return request.post(`/admin/rating/${ratingId}/audit`, payload)
}

export function getAdminRankings() {
  return request.get('/admin/ranking')
}

export function createRanking(payload) {
  return request.post('/admin/ranking', payload)
}

export function updateRanking(rankingId, payload) {
  return request.put(`/admin/ranking/${rankingId}`, payload)
}

export function generateRanking(rankingId, payload) {
  return request.post(`/admin/ranking/${rankingId}/generate`, payload)
}
