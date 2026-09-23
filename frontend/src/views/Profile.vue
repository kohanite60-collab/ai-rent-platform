<template>
  <div class="page-head">
    <h2>个人中心</h2>
    <p class="sub">查看与修改账号信息、绑定邮箱、每日签到</p>
  </div>

  <div class="grid grid-2">
    <!-- 账号信息 -->
    <section class="card">
      <div class="row-between">
        <h3>账号信息</h3>
        <button class="btn btn-sm" :disabled="loading" @click="load">
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
      </div>

      <dl v-if="profile" class="info">
        <div>
          <dt>用户 ID</dt>
          <dd>{{ profile.id }}</dd>
        </div>
        <div>
          <dt>用户名</dt>
          <dd>{{ profile.username }}</dd>
        </div>
        <div>
          <dt>算力余额</dt>
          <dd class="power">{{ profile.money.toLocaleString('zh-CN') }}</dd>
        </div>
        <div>
          <dt>角色</dt>
          <dd>
            <span class="tag" :class="profile.role === 'admin' ? 'tag-primary' : ''">
              {{ profile.role === 'admin' ? '管理员' : '普通用户' }}
            </span>
          </dd>
        </div>
        <div>
          <dt>绑定邮箱</dt>
          <dd>{{ profile.email || '未绑定' }}</dd>
        </div>
      </dl>

      <div v-else class="empty">暂无数据</div>

      <div class="btn-row" style="margin-top: 18px">
        <button class="btn" :disabled="signing" @click="onSign">
          {{ signing ? '签到中…' : '每日签到 +100 算力' }}
        </button>
      </div>
      <p class="faint" style="margin-top: 10px">
        签到状态存在 Redis 里，key 为用户名，当天过期。后端该逻辑非原子，高并发下可能重复加算力。
      </p>
    </section>

    <div class="stack">
      <!-- 修改用户名 / 密码 -->
      <section class="card">
        <h3>修改用户名 / 密码</h3>
        <p class="faint" style="margin: 6px 0 14px">
          后端 <code>updateprofile</code> 会把用户名和密码一起写入，两项都会更新。
        </p>
        <form class="stack" @submit.prevent="onUpdate">
          <label class="field">
            <span>用户名</span>
            <input v-model.trim="editForm.username" placeholder="新的用户名" />
          </label>
          <label class="field">
            <span>密码</span>
            <input v-model="editForm.password" type="password" placeholder="新的密码" />
          </label>
          <div class="notice notice-warn">
            改用户名后 Session 里存的还是旧用户名，随后会被登录拦截器判定为「用户不存在，请重新登录」，
            这是后端的既有行为，改完请重新登录一次。
          </div>
          <button class="btn btn-primary" :disabled="saving">保存修改</button>
        </form>
      </section>

      <!-- 绑定邮箱 -->
      <section class="card">
        <h3>绑定邮箱</h3>
        <p class="faint" style="margin: 6px 0 14px">绑定后可用于邮箱验证码登录与验证码推送。</p>
        <form class="stack" @submit.prevent="onBindEmail">
          <label class="field">
            <span>邮箱地址</span>
            <input v-model.trim="emailForm.email" type="email" placeholder="you@example.com" />
          </label>
          <button class="btn" :disabled="binding">绑定邮箱</button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { bindEmail, signIn, updateProfile } from '../api'
import { state, refresh } from '../store/user'
import { toast } from '../toast'

const loading = ref(false)
const saving = ref(false)
const binding = ref(false)
const signing = ref(false)

const editForm = reactive({ username: '', password: '' })
const emailForm = reactive({ email: '' })

const profile = ref(null)

async function load() {
  loading.value = true
  try {
    const p = await refresh()
    profile.value = p
    if (p) {
      editForm.username = p.username
      emailForm.email = p.email || ''
    }
  } finally {
    loading.value = false
  }
}

async function onUpdate() {
  if (!editForm.username || !editForm.password) return toast.warn('用户名和密码都要填写')
  saving.value = true
  try {
    const res = await updateProfile(editForm.username, editForm.password)
    toast.success(res.msg || '修改成功')
    if (editForm.username !== state.profile?.username) {
      // 用户名变了，Session 已失效，直接引导重新登录
      toast.warn('用户名已变更，请重新登录')
    } else {
      await load()
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    saving.value = false
  }
}

async function onBindEmail() {
  if (!emailForm.email) return toast.warn('请填写邮箱')
  binding.value = true
  try {
    const res = await bindEmail(emailForm.email)
    toast.success(res.msg || '绑定成功')
    await load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    binding.value = false
  }
}

async function onSign() {
  signing.value = true
  try {
    const res = await signIn()
    toast.success(res.msg || '签到成功')
    await load()
  } catch (e) {
    // 签到状态是存在 Redis 里的（key = 用户名），Redis 不可用时后端会抛 500。
    // 以前这种响应会被误判成「请先登录」，所以这里单独把原因说清楚。
    if (e.serverError) {
      toast.error('签到失败：后端访问 Redis 出错（签到状态存在 Redis 里），请确认虚拟机上的 Redis 已启动')
    } else {
      toast.error(e.message)
    }
  } finally {
    signing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.info {
  margin: 16px 0 0;
  display: grid;
  gap: 0;
}

.info > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid var(--border);
}

.info > div:last-child {
  border-bottom: none;
}

.info dt {
  color: var(--text-soft);
  font-size: 13.5px;
}

.info dd {
  margin: 0;
  font-weight: 500;
}

.power {
  color: var(--success) !important;
  font-weight: 700 !important;
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
