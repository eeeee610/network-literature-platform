import request from './request'

export function getAccount() {
  return request.get('/user/account')
}

export function recharge(amount) {
  return request.post('/user/account/recharge', {
    amount,
    requestNo: `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  })
}

export function listTransactions(params) {
  return request.get('/user/account/transactions', { params })
}
