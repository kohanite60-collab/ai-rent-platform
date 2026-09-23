import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 端口保持 5173：后端 application.yml 里支付宝 return-url 写的是
// http://localhost:5173/pay/success，正好对应前端 /pay/success 路由
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    // 关键：用代理转发，浏览器视角下前后端同源，
    // 所以 Cookie + Session 能正常携带，不必改后端加 CORS 配置
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 前端统一带 /api 前缀，转发时抹掉，避免和前端路由 /ai、/admin 冲突
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
