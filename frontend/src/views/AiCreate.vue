<template>
  <div class="page-head">
    <h2>AI 写诗</h2>
    <p class="sub">
      提交 Prompt 后异步走 MQ 队列交给 DeepSeek 生成，每次消耗
      <strong>{{ POEM_COST }}</strong> 算力
    </p>
  </div>

  <div class="grid grid-2">
    <section class="card">
      <h3>创作</h3>
      <form class="stack" style="margin-top: 14px" @submit.prevent="submit">
        <label class="field">
          <span>Prompt</span>
          <textarea
            v-model.trim="prompt"
            rows="5"
            placeholder="例如：以「秋夜听雨」为题，写一首七言绝句"
          ></textarea>
        </label>
        <div class="row-between">
          <span class="faint">当前算力：{{ money.toLocaleString('zh-CN') }}</span>
          <button class="btn btn-primary" :disabled="submitting || !prompt">
            {{ submitting ? '提交中…' : `提交任务（-${POEM_COST}）` }}
          </button>
        </div>
      </form>

      <!-- 任务进度 -->
      <div v-if="current" class="task">
        <div class="row-between">
          <div class="row">
            <span class="tag" :class="statusClass(current.status)">{{ current.status }}</span>
            <span class="faint">任务号 {{ shortNo(current.taskNo) }}</span>
          </div>
          <button v-if="polling" class="btn btn-sm" @click="stopPolling">停止轮询</button>
        </div>

        <div class="steps">
          <div
            v-for="(s, i) in steps"
            :key="s"
            class="step"
            :class="{ done: stepIndex >= i, active: stepIndex === i }"
          >
            <span class="dot"></span>
            {{ s }}
          </div>
        </div>

        <p class="faint">
          状态通过 <code>/ai/status?taskNo=</code> 每 2 秒轮询一次。
        </p>
      </div>

      <div class="notice notice-warn" style="margin-top: 16px">
        <strong>已知限制：</strong>后端 <code>RabbitMQConsumer</code> 只把任务状态写回
        <code>aitask</code> 表，AI 生成的诗文（<code>poem</code> 变量）没有落库，
        <code>AiTask</code> 也没有存放结果的字段。所以这里能展示任务进度，但拿不到诗句本体。
        想看到诗文，需要给 <code>AiTask</code> 加一个结果字段并在消费者里保存（属后端改动，本次未动）。
      </div>
    </section>

    <section class="card">
      <div class="row-between">
        <h3>我的任务</h3>
        <button class="btn btn-sm" :disabled="loadingTasks" @click="loadTasks">
          {{ loadingTasks ? '加载中…' : '刷新' }}
        </button>
      </div>

      <div v-if="tasksDenied" class="notice notice-info">
        <strong>你自己的创作记录暂时看不到。</strong>
        后端目前只有管理员的 <code>/admin/readai</code> 能查任务，缺少「按当前用户查任务」的接口，
        所以这里只能空着。需要后端补一个（例如 <code>GET /ai/task/list</code>）才能显示。
      </div>

      <template v-else>
        <p class="faint" style="margin: 6px 0 12px">
          后端没有「按用户查任务」的接口，这里调用 <code>/admin/readai</code> 拉全量后按
          <code>username</code> 前端过滤。
        </p>

        <div v-if="loadingTasks && !myTasks.length" class="stack">
          <div v-for="i in 3" :key="i" class="skeleton" style="height: 58px"></div>
        </div>

        <div v-else-if="myTasks.length" class="task-list">
          <div v-for="task in myTasks" :key="task.id" class="task-item">
            <div class="row-between">
              <span class="prompt-text">{{ task.prompt }}</span>
              <span class="tag" :class="statusClass(task.status)">{{ task.status }}</span>
            </div>
            <div class="faint">#{{ task.id }} · {{ task.taskname }} · {{ shortNo(task.taskNo) }}</div>
          </div>
        </div>

        <div v-else class="empty">还没有创作记录</div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { adminListTasks, createPoem, getTaskStatus } from '../api'
import { money, refresh, state } from '../store/user'
import { toast } from '../toast'
import { POEM_COST } from '../config'

const steps = ['排队中', '进行中', '构建完成']

const prompt = ref('')
const submitting = ref(false)
const current = ref(null)
const polling = ref(false)
const tasks = ref([])
const loadingTasks = ref(false)
const finished = ref(false)
// /admin/readai 已受管理员权限保护，普通用户会拿到 forbidden
const tasksDenied = ref(false)

let timer = null
let startedAt = 0

const myTasks = computed(() =>
  tasks.value
    .filter((t) => t.username === state.profile?.username)
    .sort((a, b) => b.id - a.id),
)

const stepIndex = computed(() => {
  const s = current.value?.status
  if (s === '排队中') return 0
  if (s === '进行中') return 1
  if (s === '构建完成') return 2
  return -1
})

function statusClass(status) {
  if (status === '构建完成') return 'tag-success'
  if (status === '构建失败') return 'tag-danger'
  if (status === '进行中') return 'tag-primary'
  return 'tag-warn'
}

function shortNo(no) {
  return no ? no.slice(0, 8) + '…' : '-'
}

function stopPolling() {
  polling.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function submit() {
  submitting.value = true
  finished.value = false
  try {
    const res = await createPoem(prompt.value)
    const taskNo = res.data
    toast.success('任务已提交，正在排队')
    current.value = { taskNo, status: '排队中', prompt: prompt.value }
    prompt.value = ''
    await refresh()
    loadTasks()
    startPolling(taskNo)
  } catch (e) {
    // 「余额不足」是后端正常的业务拒绝，给出下一步该去哪，而不是一句干巴巴的报错
    if (/余额不足/.test(e.message || '')) {
      toast.warn(`算力不足：每次创作需要 ${POEM_COST} 算力，可先去「个人中心」签到领取，或到首页购买套餐`)
    } else {
      toast.error(e.message)
    }
  } finally {
    submitting.value = false
  }
}

function startPolling(taskNo) {
  stopPolling()
  polling.value = true
  startedAt = Date.now()

  timer = setInterval(async () => {
    // 最长轮询 90 秒，避免队列卡死时无限请求
    if (Date.now() - startedAt > 90_000) {
      stopPolling()
      toast.warn('轮询超时，请稍后手动刷新任务列表')
      return
    }
    try {
      const res = await getTaskStatus(taskNo)
      if (res.data) {
        current.value = { ...res.data, taskNo }
        if (res.data.status === '构建完成' || res.data.status === '构建失败') {
          stopPolling()
          if (!finished.value) {
            finished.value = true
            if (res.data.status === '构建完成') toast.success('生成完成')
            else toast.error('生成失败')
            loadTasks()
          }
        }
      }
    } catch (e) {
      stopPolling()
      toast.error(e.message)
    }
  }, 2000)
}

async function loadTasks() {
  loadingTasks.value = true
  tasksDenied.value = false
  try {
    const res = await adminListTasks()
    tasks.value = res.data || []
  } catch (e) {
    // 非管理员会被 adminintercepter 拦下，给出降级提示而不是静默空列表
    tasks.value = []
    if (e.forbidden) tasksDenied.value = true
  } finally {
    loadingTasks.value = false
  }
}

onMounted(loadTasks)
onUnmounted(stopPolling)
</script>

<style scoped>
.task {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.steps {
  display: flex;
  gap: 18px;
  margin: 16px 0 12px;
  flex-wrap: wrap;
}

.step {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13.5px;
  color: var(--text-faint);
}

.step .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #dfe3ea;
}

.step.done .dot {
  background: var(--success);
}

.step.done {
  color: var(--text-soft);
}

.step.active {
  color: var(--primary);
  font-weight: 600;
}

.step.active .dot {
  background: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-soft);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 440px;
  overflow-y: auto;
}

.task-item {
  padding: 11px 13px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--panel-soft);
}

.task-item .row-between {
  gap: 10px;
}

.prompt-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 62%;
  font-size: 13.5px;
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
