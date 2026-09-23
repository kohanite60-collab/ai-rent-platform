import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import { refresh } from './store/user'

// 启动时先探一次会话，命中则直接恢复登录态
refresh().finally(() => {
  createApp(App).use(router).mount('#app')
})
