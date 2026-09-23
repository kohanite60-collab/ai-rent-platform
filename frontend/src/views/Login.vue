<template>
  <div class="auth-wrap">
    <div class="auth-card card">
      <div class="auth-head">
        <h2>{{ mode === 'login' ? '登录' : '注册新账号' }}</h2>
        <p class="muted">
          {{ mode === 'login' ? '登录后即可使用 AI 算力服务' : '注册即得 0 算力，签到可领取' }}
        </p>
      </div>

      <div v-if="mode === 'login'" class="tabs">
        <button :class="{ active: tab === 'pwd' }" @click="tab = 'pwd'">账号密码</button>
        <button :class="{ active: tab === 'email' }" @click="tab = 'email'">邮箱验证码</button>
      </div>

      <!-- 账号密码 -->
      <form v-if="mode === 'login' && tab === 'pwd'" class="stack" @submit.prevent="onPasswordLogin">
        <label class="field">
          <span>用户名</span>
          <input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </label>
        <button class="btn btn-primary btn-block" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <!-- 邮箱验证码 -->
      <form v-else-if="mode === 'login'" class="stack" @submit.prevent="onEmailLogin">
        <label class="field">
          <span>绑定邮箱</span>
          <input v-model.trim="form.email" type="email" placeholder="请输入已绑定的邮箱" />
        </label>
        <label class="field">
          <span>验证码</span>
          <div class="row">
            <input v-model.trim="form.code" placeholder="6 位验证码" maxlength="6" />
            <button
              type="button"
              class="btn"
              :disabled="countdown > 0 || !form.email"
              @click="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </label>
        <button class="btn btn-primary btn-block" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中…' : '登 录' }}
        </button>
        <p class="faint" style="text-align: center">
          验证码 2 分钟内有效，需使用已绑定到账号的邮箱
        </p>
      </form>

      <!-- 注册 -->
      <form v-else class="stack" @submit.prevent="onRegister">
        <label class="field">
          <span>用户名</span>
          <input v-model.trim="form.username" placeholder="设置用户名" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="设置密码" autocomplete="new-password" />
        </label>
        <label class="field">
          <span>确认密码</span>
          <input v-model="form.confirm" type="password" placeholder="再次输入密码" autocomplete="new-password" />
        </label>
        <button class="btn btn-primary btn-block" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '注册中…' : '注 册' }}
        </button>
      </form>

      <div class="auth-foot">
        <template v-if="mode === 'login'">
          还没有账号？<a href="javascript:;" @click="switchMode('register')">立即注册</a>
        </template>
        <template v-else>
          已有账号？<a href="javascript:;" @click="switchMode('login')">返回登录</a>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginByPassword, loginByEmail, sendCode, register } from '../api'
import { refresh } from '../store/user'
import { toast } from '../toast'

const route = useRoute()
const router = useRouter()

const mode = ref('login')
const tab = ref('pwd')
const loading = ref(false)
const countdown = ref(0)

const form = reactive({ username: '', password: '', confirm: '', email: '', code: '' })

function switchMode(next) {
  mode.value = next
  form.password = ''
  form.confirm = ''
}

function startCountdown() {
  countdown.value = 120
  const timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function afterLogin() {
  await refresh()
  toast.success('登录成功')
  const redirect = route.query.redirect
  router.push(typeof redirect === 'string' && redirect ? redirect : '/')
}

async function onPasswordLogin() {
  if (!form.username || !form.password) return toast.warn('请填写用户名和密码')
  loading.value = true
  try {
    await loginByPassword(form.username, form.password)
    await afterLogin()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function onSendCode() {
  if (!form.email) return toast.warn('请先填写邮箱')
  try {
    await sendCode(form.email)
    toast.success('验证码已发送，请查看邮箱')
    startCountdown()
  } catch (e) {
    toast.error(e.message)
  }
}

async function onEmailLogin() {
  if (!form.email || !form.code) return toast.warn('请填写邮箱和验证码')
  loading.value = true
  try {
    await loginByEmail(form.email, form.code)
    await afterLogin()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  if (!form.username || !form.password) return toast.warn('请填写用户名和密码')
  if (form.password !== form.confirm) return toast.warn('两次输入的密码不一致')
  loading.value = true
  try {
    await register(form.username, form.password)
    toast.success('注册成功，正在自动登录')
    await loginByPassword(form.username, form.password)
    await afterLogin()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  display: flex;
  justify-content: center;
  padding-top: 34px;
}

.auth-card {
  width: 100%;
  max-width: 430px;
  padding: 28px;
}

.auth-head {
  text-align: center;
  margin-bottom: 20px;
}

.auth-head h2 {
  font-size: 21px;
}

.auth-head .muted {
  margin-top: 6px;
  font-size: 13px;
}

.auth-head + .tabs {
  margin-left: auto;
  margin-right: auto;
}

.auth-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13.5px;
  color: var(--text-soft);
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
