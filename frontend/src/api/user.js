import request from './request'

export function getBookshelf(params) {
  return request.get('/user/bookshelf', { params })
}

export function addBook(novelId) {
  return request.post('/user/bookshelf', { novelId })
}

export function updateBook(novelId, payload) {
  return request.patch(`/user/bookshelf/${novelId}`, payload)
}

export function removeBook(novelId) {
  return request.delete(`/user/bookshelf/${novelId}`)
}

export function getSubscriptions(params) {
  return request.get('/user/subscriptions', { params })
}

export function subscribeChapter(payload) {
  return request.post('/user/subscriptions', payload)
}

export function getTips(params) {
  return request.get('/user/tips', { params })
}

export function createTip(payload) {
  return request.post('/user/tips', payload)
}

export function getBalance() {
  return request.get('/user/balance')
}

export function getTransactions(params) {
  return request.get('/user/transactions', { params })
}
