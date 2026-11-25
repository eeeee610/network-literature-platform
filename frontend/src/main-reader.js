import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@/styles/main.css'
import ReaderApp from './ReaderApp.vue'
import router from './router/reader'

createApp(ReaderApp).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
