import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import 'element-plus/es/components/message/style/css';
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/index.css'

import axios from "axios";

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
