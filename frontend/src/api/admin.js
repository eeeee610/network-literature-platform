import request from './request'

export function getDashboardSummary() {
  return request.get('/admin/dashboard/summary')
}

export function getNovelAudits(params) {
  return request.get('/admin/audit/novel', { params })
}

export function auditNovel(novelId, payload) {
  return request.post(`/admin/audit/novel/${novelId}`, payload)
}

export function getChapterAudits(params) {
  return request.get('/admin/audit/chapter', { params })
}

export function getChapterAuditDetail(chapterId) {
  return request.get(`/admin/audit/chapter/${chapterId}`)
}

export function auditChapter(chapterId, payload) {
  return request.post(`/admin/audit/chapter/${chapterId}`, payload)
}

export function getAuditLogs(params) {
  return request.get('/admin/audit/log', { params })
}

export function getAdminUsers(params) {
  return request.get('/admin/user', { params })
}

export function changeUserStatus(userId, status) {
  return request.patch(`/admin/user/${userId}/status`, { status })
}

export function getAdminTags(params) {
  return request.get('/admin/tag', { params })
}

export function createTag(payload) {
  return request.post('/admin/tag', payload)
}

export function updateTag(tagId, payload) {
  return request.put(`/admin/tag/${tagId}`, payload)
}

export function deleteTag(tagId) {
  return request.delete(`/admin/tag/${tagId}`)
}
