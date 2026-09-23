<template>
  <div class="page-head">
    <h2>管理后台</h2>
    <p class="sub">用户、套餐、AI 任务三块管理功能，接口全部来自 <code>/admin/**</code></p>
  </div>

  <div class="notice notice-info" style="margin-bottom: 18px">
    <strong>权限说明：</strong>后端已把 <code>adminconfign</code> 的拦截路径由
    <code>/admin</code> 改成 <code>/admin/**</code>，本页所有接口现在都要求
    <code>role == "admin"</code>。非管理员被拦下时后端既不写响应体也不是 JSON，
    前端据此识别为无权限；路由守卫也已按角色隐藏入口。
  </div>

  <div class="tabs">
    <button :class="{ active: tab === 'users' }" @click="switchTab('users')">用户管理</button>
    <button :class="{ active: tab === 'spus' }" @click="switchTab('spus')">套餐管理</button>
    <button :class="{ active: tab === 'tasks' }" @click="switchTab('tasks')">AI 任务</button>
  </div>

  <!-- 用户管理 -->
  <section v-if="tab === 'users'" class="card">
    <div class="row-between">
      <h3>用户列表</h3>
      <div class="row">
        <span class="faint">共 {{ users.length }} 条</span>
        <button class="btn btn-sm" :disabled="loading" @click="loadUsers">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="loading && !users.length" class="skeleton" style="height: 160px; margin-top: 14px"></div>

    <div v-else-if="users.length" class="table-wrap" style="margin-top: 14px">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>算力</th>
            <th>角色</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.email || '-' }}</td>
            <td>
              <input v-model.number="u.money" type="number" style="width: 110px; padding: 5px 9px" />
            </td>
            <td>
              <select v-model="u.role" style="width: 108px; padding: 5px 9px">
                <option value="user">user</option>
                <option value="admin">admin</option>
              </select>
            </td>
            <td>
              <button class="btn btn-sm" :disabled="savingId === u.id" @click="saveUser(u)">
                {{ savingId === u.id ? '保存中…' : '保存' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty" style="margin-top: 14px">暂无用户</div>
  </section>

  <!-- 套餐管理 -->
  <section v-if="tab === 'spus'" class="stack">
    <div class="card">
      <h3>新增套餐</h3>
      <p class="faint" style="margin: 6px 0 14px">
        新增后默认是下架状态（status = 0），需要手动上架才会出现在前台套餐列表。
      </p>
      <form class="grid grid-3" @submit.prevent="onAddSpu">
        <label class="field">
          <span>套餐名称</span>
          <input v-model.trim="spuForm.name" placeholder="例如：入门包" />
        </label>
        <label class="field">
          <span>算力数量</span>
          <input v-model.number="spuForm.money" type="number" min="1" placeholder="1000" />
        </label>
        <label class="field">
          <span>价格（元）</span>
          <input v-model.number="spuForm.rmb" type="number" min="1" placeholder="9" />
        </label>
        <div style="grid-column: 1 / -1">
          <button class="btn btn-primary" :disabled="adding">
            {{ adding ? '提交中…' : '新增套餐' }}
          </button>
        </div>
      </form>
    </div>

    <div class="card">
      <div class="row-between">
        <h3>套餐列表</h3>
        <button class="btn btn-sm" :disabled="loading" @click="loadSpus">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>

      <div v-if="spus.length" class="table-wrap" style="margin-top: 14px">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>算力</th>
              <th>价格(元)</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="spu in spus" :key="spu.id">
              <td>{{ spu.id }}</td>
              <td>
                <input v-model.trim="spu.name" style="width: 150px; padding: 5px 9px" />
              </td>
              <td>
                <input v-model.number="spu.money" type="number" style="width: 100px; padding: 5px 9px" />
              </td>
              <td>
                <input v-model.number="spu.rmb" type="number" style="width: 90px; padding: 5px 9px" />
              </td>
              <td>
                <span class="tag" :class="spu.status === 1 ? 'tag-success' : ''">
                  {{ spu.status === 1 ? '已上架' : '已下架' }}
                </span>
              </td>
              <td>
                <div class="btn-row">
                  <button class="btn btn-sm" :disabled="savingId === `spu-${spu.id}`" @click="saveSpu(spu)">
                    {{ savingId === `spu-${spu.id}` ? '…' : '保存' }}
                  </button>
                  <button class="btn btn-sm" @click="toggleStatus(spu)">
                    {{ spu.status === 1 ? '下架' : '上架' }}
                  </button>
                  <button class="btn btn-sm btn-danger" @click="removeSpu(spu)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty" style="margin-top: 14px">暂无套餐</div>
    </div>
  </section>

  <!-- AI 任务 -->
  <section v-if="tab === 'tasks'" class="card">
    <div class="row-between">
      <h3>全部 AI 任务</h3>
      <div class="row">
        <span class="faint">共 {{ tasks.length }} 条</span>
        <button class="btn btn-sm" :disabled="loading" @click="loadTasks">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="tasks.length" class="table-wrap" style="margin-top: 14px">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>任务类型</th>
            <th>用户</th>
            <th>Prompt</th>
            <th>状态</th>
            <th>任务号</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id">
            <td>{{ t.id }}</td>
            <td>{{ t.taskname }}</td>
            <td>{{ t.username }}</td>
            <td class="cell-prompt">{{ t.prompt }}</td>
            <td>
              <span class="tag" :class="statusClass(t.status)">{{ t.status }}</span>
            </td>
            <td class="faint">{{ (t.taskNo || '').slice(0, 12) }}…</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty" style="margin-top: 14px">暂无任务</div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  adminAddSpu,
  adminDeleteSpu,
  adminListSpus,
  adminListTasks,
  adminListUsers,
  adminReviseUser,
  adminReviseSpu,
  adminSetSpuStatus,
} from '../api'
import { toast } from '../toast'

const tab = ref('users')
const loading = ref(false)
const savingId = ref('')
const adding = ref(false)

const users = ref([])
const spus = ref([])
const tasks = ref([])

const spuForm = reactive({ name: '', money: null, rmb: null })

function statusClass(status) {
  if (status === '构建完成') return 'tag-success'
  if (status === '构建失败') return 'tag-danger'
  if (status === '进行中') return 'tag-primary'
  return 'tag-warn'
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminListUsers()
    users.value = res.data || []
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadSpus() {
  loading.value = true
  try {
    const res = await adminListSpus()
    spus.value = (res.data || []).sort((a, b) => a.id - b.id)
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await adminListTasks()
    tasks.value = (res.data || []).sort((a, b) => b.id - a.id)
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function saveUser(u) {
  savingId.value = u.id
  try {
    const res = await adminReviseUser(u.id, u.role, u.money)
    toast.success(res.msg || '修改成功')
    await loadUsers()
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingId.value = ''
  }
}

async function onAddSpu() {
  if (!spuForm.name || !spuForm.money || !spuForm.rmb) return toast.warn('请填写完整的套餐信息')
  adding.value = true
  try {
    const res = await adminAddSpu(spuForm.name, spuForm.money, spuForm.rmb)
    toast.success(res.msg || '新增成功')
    spuForm.name = ''
    spuForm.money = null
    spuForm.rmb = null
    await loadSpus()
  } catch (e) {
    toast.error(e.message)
  } finally {
    adding.value = false
  }
}

async function saveSpu(spu) {
  savingId.value = `spu-${spu.id}`
  try {
    const res = await adminReviseSpu(spu.id, spu.name, spu.money, spu.rmb)
    toast.success(res.msg || '修改成功')
    await loadSpus()
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingId.value = ''
  }
}

async function toggleStatus(spu) {
  const next = spu.status === 1 ? 0 : 1
  try {
    await adminSetSpuStatus(spu.id, next)
    spu.status = next
    toast.success(next === 1 ? '已上架' : '已下架')
  } catch (e) {
    toast.error(e.message)
  }
}

async function removeSpu(spu) {
  if (!window.confirm(`确定删除套餐「${spu.name}」吗？此操作不可撤销。`)) return
  try {
    const res = await adminDeleteSpu(spu.id)
    toast.success(res.msg || '删除成功')
    await loadSpus()
  } catch (e) {
    toast.error(e.message)
  }
}

function switchTab(next) {
  tab.value = next
  if (next === 'users') loadUsers()
  if (next === 'spus') loadSpus()
  if (next === 'tasks') loadTasks()
}

onMounted(loadUsers)
</script>

<style scoped>
.cell-prompt {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
