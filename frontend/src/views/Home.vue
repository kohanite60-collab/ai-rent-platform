<template>
  <section class="hero">
    <div class="hero-text">
      <h1>按需租赁 AI 算力</h1>
      <p>
        用算力跑通 DeepSeek 大模型：一站式托管，<strong>50 算力</strong> 生成一首诗，
        作品可上架广场获取曝光。
      </p>
      <div class="btn-row">
        <router-link v-if="isLoggedIn" to="/ai" class="btn btn-primary">开始 AI 创作</router-link>
        <router-link v-else to="/login" class="btn btn-primary">登录后开始</router-link>
        <router-link to="/gallery" class="btn">逛逛作品广场</router-link>
      </div>
    </div>

    <aside v-if="isLoggedIn" class="hero-side">
      <div class="side-label">当前算力余额</div>
      <div class="side-value">
        {{ power }}
        <span class="unit">算力</span>
      </div>
      <div class="side-actions">
        <button class="btn btn-sm" :disabled="signing" @click="onSign">
          {{ signing ? '签到中…' : '每日签到 +100' }}
        </button>
        <router-link to="/profile" class="btn btn-sm">个人中心</router-link>
      </div>
    </aside>
    <aside v-else class="hero-side">
      <div class="side-label">还没有账号？</div>
      <div class="side-value small">注册后每日签到即可领算力</div>
      <div class="side-actions">
        <router-link to="/login" class="btn btn-sm btn-primary">登录 / 注册</router-link>
      </div>
    </aside>
  </section>

  <div class="page-head">
    <h2>算力套餐</h2>
    <p class="sub">
      套餐列表来自公开接口 <code>/spu/list</code>，后端已做过滤，只返回已上架（status = 1）的套餐
    </p>
  </div>

  <div v-if="!isLoggedIn" class="notice notice-info">
    套餐可以直接浏览，<strong>下单前需要先登录</strong>。登录后即可用支付宝购买算力。
  </div>

  <div v-if="loading" class="grid grid-3">
    <div v-for="i in 3" :key="i" class="card skeleton" style="height: 186px"></div>
  </div>

  <div v-else-if="spus.length" class="grid grid-3">
    <article v-for="spu in spus" :key="spu.id" class="card spu">
      <div class="spu-name">{{ spu.name }}</div>
      <div class="spu-money">
        {{ spu.money.toLocaleString('zh-CN') }}
        <span class="unit">算力</span>
      </div>
      <div class="spu-price">
        <span class="rmb">¥{{ spu.rmb }}</span>
        <span class="faint">/ {{ spu.money }} 算力</span>
      </div>
      <button
        class="btn btn-block"
        :class="isLoggedIn ? 'btn-primary' : ''"
        :disabled="paying === spu.id"
        @click="buy(spu)"
      >
        {{ paying === spu.id ? '正在创建订单…' : isLoggedIn ? '立即购买' : '登录后购买' }}
      </button>
      <p class="faint spu-tip">约可生成 {{ Math.floor(spu.money / POEM_COST) }} 次 AI 写诗</p>
    </article>
  </div>

  <div v-else class="empty">
    当前没有已上架的套餐。管理员可在「管理后台 → 套餐管理」里新增并上架。
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listSpus, payOrder, signIn } from '../api'
import { isLoggedIn, money, refresh } from '../store/user'
import { toast } from '../toast'
import { POEM_COST } from '../config'

const router = useRouter()
const spus = ref([])
const loading = ref(false)
const signing = ref(false)
const paying = ref(0)

const power = computed(() => money.value.toLocaleString('zh-CN'))

// /spu/list 是公开接口，未登录也能拉；下架套餐已由后端过滤掉
async function loadSpus() {
  loading.value = true
  try {
    const res = await listSpus()
    spus.value = res.data || []
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function onSign() {
  signing.value = true
  try {
    const res = await signIn()
    toast.success(res.msg || '签到成功')
    await refresh()
  } catch (e) {
    toast.error(e.message)
  } finally {
    signing.value = false
  }
}

async function buy(spu) {
  // 浏览放开、交易不放宽：未登录不允许下单，引导去登录
  if (!isLoggedIn.value) {
    toast.warn('请先登录后再购买')
    router.push({ path: '/login', query: { redirect: '/' } })
    return
  }

  // 先同步打开窗口，避免被浏览器当作弹窗拦截
  const win = window.open('', '_blank')
  if (win) {
    win.document.write(
      '<p style="font-family:sans-serif;padding:24px;color:#333">正在跳转支付宝…</p>',
    )
  }

  paying.value = spu.id
  try {
    const html = await payOrder(spu.id)
    const looksLikeJson = html.trim().startsWith('{')
    if (!html || looksLikeJson) {
      win?.close()
      let message = '创建支付订单失败'
      try {
        message = JSON.parse(html).msg || message
      } catch (e) {
        /* ignore */
      }
      toast.error(message)
      return
    }
    win.document.open()
    win.document.write(html)
    win.document.close()
  } catch (e) {
    win?.close()
    toast.error(e.message)
  } finally {
    paying.value = 0
  }
}

onMounted(loadSpus)
</script>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  align-items: stretch;
  margin-bottom: 30px;
}

.hero-text {
  padding: 30px 32px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #eef0ff 0%, #f6f2ff 55%, #ffffff 100%);
  border: 1px solid #e2e4fb;
}

.hero-text h1 {
  font-size: 27px;
  letter-spacing: -0.3px;
}

.hero-text p {
  margin: 12px 0 20px;
  color: var(--text-soft);
  max-width: 46em;
}

.hero-text strong {
  color: var(--primary);
}

.hero-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px;
  border-radius: var(--radius);
  background: var(--panel);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.side-label {
  font-size: 13px;
  color: var(--text-soft);
}

.side-value {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -1px;
  margin: 6px 0 16px;
  color: var(--text);
}

.side-value.small {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-soft);
  letter-spacing: 0;
}

.unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-faint);
  margin-left: 2px;
}

.side-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spu {
  display: flex;
  flex-direction: column;
  padding: 22px;
}

.spu-name {
  font-size: 15px;
  font-weight: 600;
}

.spu-money {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.8px;
  margin: 10px 0 2px;
  color: var(--primary);
}

.spu-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 18px;
}

.rmb {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}

.spu-tip {
  margin-top: 10px;
  text-align: center;
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}

@media (max-width: 820px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
