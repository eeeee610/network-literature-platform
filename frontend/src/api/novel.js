import request from './request'

export function getErrorCode(error) {
  return error?.response?.data?.code ?? null
}

export function getErrorMessage(error, fallback = '请求失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

export function getNovelList(params) {
  return request.get('/novel/list', { params })
}

export function searchNovels(params) {
  return request.get('/novel/search', { params })
}

export function getNovelDetail(novelId) {
  return request.get(`/novel/${novelId}`)
}

export function getNovelChapters(novelId) {
  return request.get(`/novel/${novelId}/chapters`)
}

export function getChapter(chapterId) {
  return request.get(`/novel/chapter/${chapterId}`)
}

export function getTagList() {
  return request.get('/novel/tag/list')
}

export function createNovel(data) {
  return request.post('/novel', data)
}

export function updateNovel(novelId, data) {
  return request.put(`/novel/${novelId}`, data)
}

export function getAuthorNovels(params) {
  return request.get('/novel/author/list', { params })
}

export function getAuthorChapters(novelId, params) {
  return request.get(`/novel/author/${novelId}/chapters`, { params })
}

export function getAuthorChapter(chapterId) {
  return request.get(`/novel/author/chapter/${chapterId}`)
}

export function createChapter(data) {
  return request.post('/novel/chapter', data)
}

export function updateChapter(chapterId, data) {
  return request.put(`/novel/chapter/${chapterId}`, data)
}

export function publishChapter(chapterId) {
  return request.post(`/novel/chapter/${chapterId}/publish`)
}

export function offlineChapter(chapterId) {
  return request.post(`/novel/chapter/${chapterId}/offline`)
}
