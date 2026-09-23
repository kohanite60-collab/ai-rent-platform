<template>
  <div class="page-head">
    <h2>作品广场</h2>
    <p class="sub">数据来自 <code>/ai/show/list?sortid=</code>，按浏览量 <code>view</code> 降序</p>
  </div>

  <div class="row-between" style="margin-bottom: 18px; flex-wrap: wrap">
    <div class="tabs" style="margin-bottom: 0">
      <button :class="{ active: activeCat === 0 }" @click="switchCat(0)">全部</button>
      <button
        v-for="c in CATEGORIES"
        :key="c.id"
        :class="{ active: activeCat === c.id }"
        @click="switchCat(c.id)"
      >
        {{ c.name }}
      </button>
    </div>
    <button class="btn btn-sm" :disabled="loading" @click="load">
      {{ loading ? '加载中…' : '刷新' }}
    </button>
  </div>

  <div v-if="loading" class="grid grid-3">
    <div v-for="i in 6" :key="i" class="card skeleton" style="height: 150px"></div>
  </div>

  <div v-else-if="items.length" class="grid grid-3">
    <article v-for="item in items" :key="item.id" class="card work" @click="open(item)">
      <div class="row-between">
        <h3 class="work-title">{{ item.title }}</h3>
        <span class="tag">{{ catName(item.sortid) }}</span>
      </div>
      <p class="work-preview">{{ item.data }}</p>
      <div class="row-between work-foot">
        <span class="faint">@{{ item.user || '匿名' }}</span>
        <span class="faint">{{ item.view }} 次浏览</span>
      </div>
    </article>
  </div>

  <div v-else class="empty">
    该分类下暂无作品。
    <template v-if="activeCat === 0">
      <br />「全部」会合并 {{ SORT_ID_RANGE.join('、') }} 这些 sortid 的结果，可在
      <code>src/config.js</code> 里调整。
    </template>
  </div>

  <!-- 上传作品 -->
  <section v-if="isLoggedIn" class="card" style="margin-top: 22px">
    <h3>发布我的作品到广场</h3>
    <p class="faint" style="margin: 6px 0 14px">
      对应 <code>POST /ai/upload</code>。该接口没有返回值（响应体为空），成功与否以列表是否出现为准。
    </p>
    <form class="grid grid-2" @submit.prevent="onUpload">
      <label class="field">
        <span>标题</span>
        <input v-model.trim="uploadForm.title" placeholder="作品标题" />
      </label>
      <label class="field">
        <span>分类</span>
        <select v-model.number="uploadForm.sortid">
          <option v-for="c in CATEGORIES" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </label>
      <label class="field" style="grid-column: 1 / -1">
        <span>正文</span>
        <textarea v-model.trim="uploadForm.data" rows="4" placeholder="粘贴你的作品内容"></textarea>
      </label>
      <div style="grid-column: 1 / -1">
        <button class="btn btn-primary" :disabled="uploading || !uploadForm.title || !uploadForm.data">
          {{ uploading ? '发布中…' : '发布作品' }}
        </button>
      </div>
    </form>
  </section>

  <div v-else class="notice notice-info" style="margin-top: 22px">
    登录后可以把自己的作品发布到广场。
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listProductions, uploadProduction } from '../api'
import { isLoggedIn } from '../store/user'
import { toast } from '../toast'
import { CATEGORIES, SORT_ID_RANGE } from '../config'

const router = useRouter()

const activeCat = ref(0)
const items = ref([])
const loading = ref(false)
const uploading = ref(false)

const uploadForm = reactive({ title: '', data: '', sortid: CATEGORIES[0]?.id ?? 1 })

function catName(id) {
  return CATEGORIES.find((c) => c.id === id)?.name || `分类 ${id}`
}

async function load() {
  loading.value = true
  try {
    if (activeCat.value === 0) {
      // 后端没有「查全部」的口径，逐个 sortid 拉取后合并去重
      const results = await Promise.all(
        SORT_ID_RANGE.map((id) =>
          listProductions(id)
            .then((res) => (Array.isArray(res.data) ? res.data : []))
            .catch(() => []),
        ),
      )
      const merged = new Map()
      results.flat().forEach((item) => merged.set(item.id, item))
      items.value = [...merged.values()].sort((a, b) => b.view - a.view)
    } else {
      const res = await listProductions(activeCat.value)
      items.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    items.value = []
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function switchCat(id) {
  activeCat.value = id
  load()
}

function open(item) {
  router.push(`/gallery/${item.id}`)
}

async function onUpload() {
  uploading.value = true
  try {
    await uploadProduction(uploadForm.title, uploadForm.data, uploadForm.sortid)
    toast.success('发布请求已提交')
    uploadForm.title = ''
    uploadForm.data = ''
    await load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.work {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
}

.work:hover {
  transform: translateY(-2px);
  border-color: #d9dcfb;
  box-shadow: var(--shadow);
}

.work-title {
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 11em;
}

.work-preview {
  margin: 12px 0 16px;
  color: var(--text-soft);
  font-size: 13.5px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 92px;
}

.work-foot {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
