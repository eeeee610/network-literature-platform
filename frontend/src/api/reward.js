import request from './request'

export function getRewardTarget(novelId) {
  return request.get(`/user/reward/novel/${novelId}`)
}

export function sendReward(data) {
  return request.post('/user/reward', data)
}

export function listSentRewards(params) {
  return request.get('/user/reward/sent', { params })
}

export function listReceivedRewards(params) {
  return request.get('/user/reward/received', { params })
}
