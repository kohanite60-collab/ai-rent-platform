import { reactive } from 'vue'

export const toasts = reactive([])

let seq = 0

function push(message, type = 'info', timeout = 2600) {
  const id = ++seq
  toasts.push({ id, message, type })
  setTimeout(() => {
    const index = toasts.findIndex((t) => t.id === id)
    if (index > -1) toasts.splice(index, 1)
  }, timeout)
}

export const toast = {
  info: (msg) => push(msg, 'info'),
  success: (msg) => push(msg, 'success'),
  error: (msg) => push(msg, 'error', 3600),
  warn: (msg) => push(msg, 'warn', 3200),
}

/** 把 ApiError / 未知异常统一弹成提示 */
export function toastError(e) {
  toast.error(e?.message || '操作失败')
}
