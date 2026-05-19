import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import 'element-plus/es/components/notification/style/css';
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/index.css'
import '@/assets/design-system.less'

import axios from "axios";

const apiBaseURL = import.meta.env.VITE_API_BASE_URL
axios.defaults.baseURL = apiBaseURL === undefined ? "http://localhost:8080" : apiBaseURL

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
