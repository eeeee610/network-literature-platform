import request from './request'

export function getChapterPurchaseInfo(chapterId) {
  return request.get(`/user/subscription/chapter/${chapterId}`)
}

export function subscribeChapter(data) {
  return request.post('/user/subscription', data)
}

export function listSubscriptions(params) {
  return request.get('/user/subscription', { params })
}

export function checkSubscription(chapterId) {
  return request.get('/user/subscription/check', { params: { chapterId } })
}
