const BIZ_TYPE_LABELS = {
  RECHARGE: '充值',
  SUBSCRIBE: '订阅章节',
  TIP_OUT: '发出打赏',
  TIP_IN: '收到打赏',
  REFUND: '退款',
  WITHDRAW: '提现',
  ADJUST_IN: '后台入账',
  ADJUST_OUT: '后台扣减'
}

export const BIZ_TYPE_OPTIONS = Object.entries(BIZ_TYPE_LABELS).map(([value, label]) => ({ value, label }))

export function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('+08:00', '').slice(0, 19)
}

export function formatMoney(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

export function bizTypeLabel(value) {
  return BIZ_TYPE_LABELS[value] || value || '-'
}

export function requestNo() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}
