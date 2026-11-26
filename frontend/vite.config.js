import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    // 允许嵌入型环境（如 VSCode 集成浏览器 / webview）跨上下文访问开发服务器
    cors: { origin: '*' },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        configure(proxy) {
          // 后端未配置 CORS，Spring 会把带 Origin 的请求判定为跨域并返回 403。
          // 开发环境下由代理将 Origin 改写成与后端同源，避免被 CORS 拒绝
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('origin', 'http://127.0.0.1:8080')
          })
          // 同时在响应上补齐 CORS 头，兼容 webview 等隔离上下文
          proxy.on('proxyRes', (proxyRes) => {
            proxyRes.headers['access-control-allow-origin'] = '*'
            proxyRes.headers['access-control-allow-methods'] =
              'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS'
            proxyRes.headers['access-control-allow-headers'] =
              'Content-Type,Authorization'
          })
        },
      },
    },
  },
})
