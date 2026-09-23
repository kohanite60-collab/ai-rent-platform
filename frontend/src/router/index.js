import { createRouter, createWebHistory } from 'vue-router'
import { state, refresh, isLoggedIn, isAdmin } from '../store/user'
import { toast } from '../toast'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { guestOnly: true } },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/ai',
    name: 'ai',
    component: () => import('../views/AiCreate.vue'),
    meta: { requiresAuth: true },
  },
  { path: '/gallery', name: 'gallery', component: () => import('../views/Gallery.vue') },
  { path: '/gallery/:id', name: 'gallery-detail', component: () => import('../views/GalleryDetail.vue') },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  { path: '/pay/success', name: 'pay-success', component: () => import('../views/PaySuccess.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  // history 模式：后端 application.yml 里支付宝 return-url 是 /pay/success，
  // vite dev server 自带 SPA fallback，刷新不会 404
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  if (!state.ready) await refresh()

  if (to.meta.requiresAuth && !isLoggedIn.value) {
    toast.warn('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAuth && to.meta.requiresAdmin && !isAdmin.value) {
    toast.error('需要管理员权限')
    return { path: '/' }
  }

  if (to.meta.guestOnly && isLoggedIn.value) {
    return { path: '/' }
  }

  return true
})

export default router
