<script setup>
import { get } from '@/net/api.js'
import { useAppStore } from '@/stores/app-store.js'
import { computed, ref } from 'vue'
import { logout } from '@/net/api.js'
import router from '@/router/index.js'
import { Shield, LogOut, ArrowLeft, Sun, Moon } from 'lucide-vue-next'
import { useDark, useToggle } from '@vueuse/core'

const store = useAppStore()
const loading = ref(true)

const isDark = useDark()
const toggleDark = useToggle(isDark)

get('/api/user/info', data => {
  store.user = data
  loading.value = false
})

const getAvatar = computed(() => store.getAvatar(store.user.avatar, store.user.username))

function userLogout() {
  logout(() => {
    store.user = {}
    router.push('/')
  })
}
</script>

<template>
  <div class="admin-shell" v-loading="loading" element-loading-text="正在加载管理后台...">
    <header class="admin-topbar">
      <div class="topbar-left">
        <div class="brand">
          <img src="/favicon.png" alt="Logo" class="brand-logo" />
          <div class="brand-text">
            <span class="brand-name">青研社</span>
            <span class="brand-sub">管理后台</span>
          </div>
        </div>
        <div class="topbar-divider"></div>
        <div class="admin-badge">
          <Shield :size="12" />
          <span>{{ store.user.role === 'admin' ? '超级管理员' : store.user.role === 'content_admin' ? '内容管理员' : '版主' }}</span>
        </div>
      </div>
      <div class="topbar-right">
        <button class="topbar-icon-btn" @click="toggleDark()" :title="isDark ? '切换亮色' : '切换暗色'">
          <Moon v-if="isDark" :size="16" />
          <Sun v-else :size="16" />
        </button>
        <button class="topbar-icon-btn" @click="router.push('/home')" title="返回前台">
          <ArrowLeft :size="16" />
        </button>
        <div class="topbar-divider"></div>
        <div class="user-info">
          <el-avatar :size="28" :src="getAvatar" />
          <span class="user-name">{{ store.user.username }}</span>
        </div>
        <button class="logout-btn" @click="userLogout" title="退出登录">
          <LogOut :size="15" />
        </button>
      </div>
    </header>
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--el-bg-color-page, #f4f5f9);
  transition: background 0.3s;
}

.admin-topbar {
  height: 54px;
  background: var(--el-bg-color, #fff);
  border-bottom: 1px solid var(--el-border-color-lighter, #eef0f4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 100;
  transition: background 0.3s, border-color 0.3s;
}

.topbar-left, .topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-logo {
  width: 28px;
  height: 28px;
  border-radius: 6px;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 14px;
  font-weight: 800;
  color: var(--el-text-color-primary, #1a1d2e);
  line-height: 1.2;
}

.brand-sub {
  font-size: 9px;
  font-weight: 700;
  color: var(--el-text-color-placeholder, #9ca3af);
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

.topbar-divider {
  width: 1px;
  height: 20px;
  background: var(--el-border-color-lighter, #e5e7eb);
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.topbar-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary, #6b7280);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.topbar-icon-btn:hover {
  background: var(--el-fill-color-light, #f3f4f6);
  color: var(--el-text-color-primary, #374151);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 7px;
}

.user-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular, #374151);
}

.logout-btn {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: var(--el-text-color-placeholder, #9ca3af);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.logout-btn:hover {
  background: #fef2f2;
  color: #ef4444;
}

.admin-main {
  flex: 1;
  overflow: hidden;
}
</style>
