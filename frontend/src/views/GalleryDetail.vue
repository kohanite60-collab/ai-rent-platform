<template>
  <div class="page-head">
    <div class="row-between" style="flex-wrap: wrap">
      <div>
        <h2>{{ item ? item.title : '作品详情' }}</h2>
        <p class="sub" v-if="item">
          @{{ item.user || '匿名' }} · {{ catName(item.sortid) }} ·
          {{ item.view }} 次浏览 · 作品 ID {{ item.id }}
        </p>
      </div>
      <div class="btn-row">
        <button class="btn btn-sm" @click="router.back()">返回</button>
        <button class="btn btn-sm" :disabled="loading" @click="load">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="loading && !item" class="card skeleton" style="height: 240px"></div>

  <article v-else-if="item" class="card content">
    <pre>{{ item.data }}</pre>
  </article>

  <div v-else class="empty">没有找到这篇作品</div>

  <div class="notice notice-info" style="margin-top: 16px">
    每次打开详情，后端都会把 <code>view</code> 加一并写库（<code>/ai/show/{id}</code>），
    所以刷新一次浏览量就会 +1。
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduction } from '../api'
import { toast } from '../toast'
import { CATEGORIES } from '../config'

const route = useRoute()
const router = useRouter()

const item = ref(null)
const loading = ref(false)

function catName(id) {
  return CATEGORIES.find((c) => c.id === id)?.name || `分类 ${id}`
}

async function load() {
  loading.value = true
  try {
    const res = await getProduction(route.params.id)
    item.value = res.data && res.data.id ? res.data : null
  } catch (e) {
    item.value = null
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.content {
  padding: 30px 34px;
}

.content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 15.5px;
  line-height: 2;
  color: var(--text);
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
