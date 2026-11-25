import request from './request'

export function listBookshelf(params) {
  return request.get('/user/bookshelf', { params })
}

export function addToShelf(novelId) {
  return request.post('/user/bookshelf', { novelId })
}

export function getShelfStatus(novelId) {
  return request.get(`/user/bookshelf/${novelId}`)
}

export function updateShelfFlags(novelId, data) {
  return request.put(`/user/bookshelf/${novelId}`, data)
}

export function updateReadingProgress(novelId, chapterId) {
  return request.put(`/user/bookshelf/${novelId}/reading`, { chapterId })
}

export function removeFromShelf(novelId) {
  return request.delete(`/user/bookshelf/${novelId}`)
}
