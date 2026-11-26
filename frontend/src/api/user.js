import request from './request'
import { updateStoredUser } from './auth'

export function getProfile() {
  return request.get('/user/profile')
}

export async function updateProfile(data) {
  const profile = await request.put('/user/profile', data)
  updateStoredUser(profile)
  return profile
}

export function getAccount() {
  return request.get('/user/account')
}

export function getTransactions(params) {
  return request.get('/user/account/transaction', { params })
}

export function getBookshelf(params) {
  return request.get('/user/bookshelf', { params })
}

export function addToBookshelf(novelId) {
  return request.post('/user/bookshelf', { novelId })
}

export function removeFromBookshelf(novelId) {
  return request.delete(`/user/bookshelf/${novelId}`)
}

export function getSubscriptions(params) {
  return request.get('/user/subscription', { params })
}

export function subscribeChapter(data) {
  return request.post('/user/subscription', data)
}

export function cancelSubscription(chapterId) {
  return request.delete(`/user/subscription/${chapterId}`)
}

export function getTips(params) {
  return request.get('/user/tip', { params })
}

export function tipNovel(data) {
  return request.post('/user/tip', data)
}
