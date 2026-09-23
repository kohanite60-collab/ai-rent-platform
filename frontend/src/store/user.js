import { reactive, computed } from 'vue'
import { getProfile, logout } from '../api'

export const state = reactive({
  profile: null, // { id, username, password, money, email, role }
  ready: false, // 是否已探测过会话
})

export const isLoggedIn = computed(() => !!state.profile)
export const isAdmin = computed(() => state.profile?.role === 'admin')
export const money = computed(() => state.profile?.money ?? 0)

/** 拉取当前登录用户信息；失败则视为未登录 */
export async function refresh() {
  try {
    const res = await getProfile()
    state.profile = res.data || null
  } catch (e) {
    // 只有「确实未登录 / 没权限」才清空本地状态。
    // 后端 5xx（Redis、RabbitMQ、数据库不可用等）时保留原状态，
    // 否则服务一抖，界面就会假装「你已经退出登录了」，同样误导。
    if (e.unauthorized || e.forbidden) {
      state.profile = null
    }
  } finally {
    state.ready = true
  }
  return state.profile
}

export function setProfile(profile) {
  state.profile = profile
}

export async function doLogout() {
  try {
    await logout()
  } catch (e) {
    /* 即使后端报错也照样清掉本地状态 */
  }
  state.profile = null
}
