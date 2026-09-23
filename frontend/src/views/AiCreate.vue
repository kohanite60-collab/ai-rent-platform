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

        <!-- 生成结果：消费者已把诗文写入 aitask.content，轮询时会一并带回 -->
        <div v-if="current.content" class="poem">
          <div class="poem-head">
            <span class="poem-label">生成结果</span>
            <button class="btn btn-sm" @click="copyPoem(current.content)">复制</button>
          </div>
          <pre class="poem-body">{{ current.content }}</pre>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="row-between">
        <h3>我的任务</h3>
        <button class="btn btn-sm" :disabled="loadingTasks" @click="loadTasks">
          {{ loadingTasks ? '加载中…' : '刷新' }}
        </button>
      </div>

      <p class="faint" style="margin: 6px 0 12px">
        数据来自 <code>GET /user/aitask</code>，后端按 Session 用户名过滤，只会返回你自己的任务。
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
          <div class="task-foot">
            <span class="faint">#{{ task.id }} · {{ task.taskname }} · {{ shortNo(task.taskNo) }}</span>
            <button v-if="task.content" class="btn btn-sm" @click="toggle(task.id)">
              {{ expandedId === task.id ? '收起' : '查看诗文' }}
            </button>
          </div>
          <pre v-if="expandedId === task.id" class="poem-body">{{ task.content }}</pre>
        </div>
      </div>

      <div v-else class="empty">还没有创作记录</div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { createPoem, getTaskStatus, listMyTasks } from '../api'
import { money, refresh } from '../store/user'
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
// 「我的任务」里正在展开查看诗文的条目 id
const expandedId = ref(null)

let timer = null
let startedAt = 0

// 后端 /user/aitask 已按 Session 用户过滤并按 id 倒序，这里直接使用
const myTasks = computed(() => tasks.value)

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
  try {
    // 身份由后端 Session 决定，前端不传用户名
    const res = await listMyTasks()
    tasks.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    tasks.value = []
    toast.error(e.message)
  } finally {
    loadingTasks.value = false
  }
}

function toggle(id) {
  expandedId.value = expandedId.value === id ? null : id
}

async function copyPoem(text) {
  try {
    await navigator.clipboard.writeText(text)
    toast.success('已复制到剪贴板')
  } catch (e) {
    // 非安全上下文（http）或浏览器拒绝剪贴板权限时会走到这里
    toast.warn('复制失败，请手动选中文本复制')
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

.task-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
}

.poem {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--panel-soft);
}

.poem-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.poem-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-soft);
}

.poem-body {
  margin: 8px 0 0;
  padding: 0;
  background: transparent;
  font-family: var(--font-serif, Georgia, serif);
  font-size: 14.5px;
  line-height: 1.9;
  color: var(--text);
  white-space: pre-wrap;
  word-break: break-word;
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
