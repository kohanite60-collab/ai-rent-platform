<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/" class="brand">
        <span class="brand-mark">AI</span>
        <span class="brand-text">
          <strong>算力租赁平台</strong>
          <em>AI Rent Platform</em>
        </span>
      </router-link>

      <nav class="links">
        <router-link to="/">算力套餐</router-link>
        <router-link to="/ai">AI 创作</router-link>
        <router-link to="/gallery">作品广场</router-link>
        <router-link v-if="isLoggedIn" to="/profile">个人中心</router-link>
        <router-link v-if="isAdmin" to="/admin">管理后台</router-link>
      </nav>

      <div class="account">
        <template v-if="isLoggedIn">
          <router-link to="/profile" class="power" title="当前算力余额">
            <span class="power-dot"></span>
            {{ power }} 算力
          </router-link>
          <span class="whoami">
            {{ state.profile.username }}
            <span v-if="isAdmin" class="tag tag-primary">管理员</span>
          </span>
          <button class="btn btn-sm" @click="onLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-sm btn-primary">登录 / 注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { state, isLoggedIn, isAdmin, doLogout, money } from '../store/user'
import { toast } from '../toast'

const router = useRouter()
const power = computed(() => money.value.toLocaleString('zh-CN'))

async function onLogout() {
  await doLogout()
  toast.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 20px;
  height: 62px;
  display: flex;
  align-items: center;
  gap: 26px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text);
  flex-shrink: 0;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #6d6df0, #9b6ef3);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 14.5px;
}

.brand-text em {
  font-style: normal;
  font-size: 10.5px;
  color: var(--text-faint);
  letter-spacing: 0.4px;
}

.links {
  display: flex;
  gap: 4px;
  flex: 1;
}

.links a {
  padding: 6px 12px;
  border-radius: 8px;
  color: var(--text-soft);
  font-weight: 500;
  transition: all 0.15s;
}

.links a:hover {
  background: var(--panel-soft);
  color: var(--text);
}

.links a.router-link-exact-active {
  background: var(--primary-soft);
  color: var(--primary);
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.power {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--success-soft);
  border: 1px solid #c6ecd5;
  color: var(--success);
  font-size: 13px;
  font-weight: 600;
}

.power-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success);
}

.whoami {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  color: var(--text-soft);
}

@media (max-width: 860px) {
  .brand-text,
  .whoami {
    display: none;
  }
  .links a {
    padding: 6px 8px;
    font-size: 13.5px;
  }
  .nav-inner {
    gap: 12px;
  }
}
</style>
