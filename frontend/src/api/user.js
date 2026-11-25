import userRequest from './userRequest'

export function getBookshelf(params) {
  return userRequest.get('/user/bookshelf', { params })
}

export function addBook(novelId) {
  return userRequest.post('/user/bookshelf', { novelId })
}

export function updateBook(novelId, payload) {
  return userRequest.patch(`/user/bookshelf/${novelId}`, payload)
}

export function removeBook(novelId) {
  return userRequest.delete(`/user/bookshelf/${novelId}`)
}

export function getSubscriptions(params) {
  return userRequest.get('/user/subscriptions', { params })
}

export function subscribeChapter(payload) {
  return userRequest.post('/user/subscriptions', payload)
}

export function getTips(params) {
  return userRequest.get('/user/tips', { params })
}

export function createTip(payload) {
  return userRequest.post('/user/tips', payload)
}

export function getBalance() {
  return userRequest.get('/user/balance')
}

export function getTransactions(params) {
  return userRequest.get('/user/transactions', { params })
}
