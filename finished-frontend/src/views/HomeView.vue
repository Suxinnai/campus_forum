<script setup>
import { get, del, logout } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";
import { computed, ref } from "vue";
import { Bell, Check, Tent, BookOpen, BarChart2, Settings, LogOut, Sun, Moon, Home, ShieldCheck, GraduationCap, Users, Bookmark, PlusSquare, Plus, Link, MessageSquare, User, Search } from "lucide-vue-next";
import router from "@/router/index.js";
import axios from "axios";
import LightCard from "@/components/LightCard.vue";
import { useDark } from "@vueuse/core";
import TopicEditor from "@/components/TopicEditor.vue"; // Added

const store = useAppStore();
const loading = ref(true);

get("/api/user/info", (data) => {
  store.user = data;
  loading.value = false;
});

const notification = ref([]);
const loadNotification = () => {
  get("/api/notification/list", data => { notification.value = data; });
};
loadNotification();

function userLogout() {
  logout(() => {
    store.user = {};
    router.push("/");
    ElMessage.success("已成功退出登录");
  });
}

function confirmNotification(id, url) {
  del(`/api/notification/delete?id=${id}`, () => {
    loadNotification();
    if (url) router.push(url);
  });
}
function deleteAllNotification() {
  del("/api/notification/delete-all", () => { loadNotification(); });
}

const getAvatar = computed(() =>
  store.user.avatar
    ? `${axios.defaults.baseURL}/images${store.user.avatar}`
    : "https://www.vexipui.com/qmhc.jpg"
);

const dark = ref(useDark());
const editor = ref(false); // Make editor available here

const navGroups = [
  {
    title: "社区互动",
    items: [
      { label: "动态首页", path: "/home", icon: Home },
      { label: "校园生活", path: "/home/activity", icon: Users },
    ]
  },
  {
    title: "资源工具",
    items: [
      { label: "学习资源", path: "/home/resource", icon: GraduationCap },
      { label: "快速访问", path: "/home/quick-access", icon: Link },
    ]
  },
  {
    title: "个人服务",
    items: [
      { label: "我的收藏", path: "/home/collections", icon: Bookmark },
      { label: "建议反馈", path: "/home/feedback", icon: MessageSquare },
    ]
  }
];

const adminNav = {
  title: "管理后台",
  items: [
    { label: "全站概览", path: "/home/admin", icon: ShieldCheck },
  ]
};

const currentPath = computed(() => router.currentRoute.value.path);
const isAdmin = computed(() => store.user.role === 'admin');

function handleGlobalSearch() {
  ElMessage.info("搜索功能已准备就绪，目前在开发模式下，关键词过滤即将上线...");
}

</script>

<template>
  <div class="app-shell" v-loading="loading" element-loading-text="正在加载，请稍后...">

    <!-- Top Nav -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="topbar-center">
          <div class="search-input-wrap">
            <Search :size="16" class="search-icon" />
            <input 
              type="text" 
              placeholder="搜索感兴趣的话题、资源或校友..." 
              class="search-inner"
              @keyup.enter="handleGlobalSearch"
            />
            <div class="search-kbd">
              <span class="cmd">⌘</span>
              <span>K</span>
            </div>
          </div>
        </div>

        <div class="topbar-right">
          <button class="icon-btn" @click="dark = !dark" :title="dark ? '切换亮色' : '切换暗色'">
            <Moon v-if="dark" :size="18" />
            <Sun v-else :size="18" />
          </button>

          <el-popover placement="bottom-end" :width="360" trigger="click">
            <template #reference>
              <el-badge is-dot :hidden="!notification.length">
                <button class="icon-btn">
                  <Bell :size="18" />
                </button>
              </el-badge>
            </template>
            <el-empty :image-size="70" description="没有未读消息" v-if="!notification.length" />
            <el-scrollbar :max-height="460" v-else>
              <LightCard
                v-for="item in notification"
                :key="item.id"
                class="notif-item"
                @click="confirmNotification(item.id, item.url)"
              >
                <div><el-tag :type="item.type" size="small">消息</el-tag>&nbsp;<b>{{ item.title }}</b></div>
                <el-divider style="margin: 6px 0 2px" />
                <div style="font-size:13px;color:var(--ds-muted)">{{ item.content }}</div>
              </LightCard>
            </el-scrollbar>
            <div style="margin-top:10px">
              <el-button size="small" type="info" :icon="Check" @click="deleteAllNotification" style="width:100%" plain>
                清除全部未读消息
              </el-button>
            </div>
          </el-popover>

          <el-dropdown trigger="click" placement="bottom-end" popper-class="user-dropdown-popper">
            <div class="user-trigger">
              <el-avatar :size="32" :src="getAvatar" class="user-avatar" />
              <span class="user-name-bubble">{{ store.user.username }}</span>
            </div>
            <template #dropdown>
              <div class="user-dropdown-card">
                <div class="udc-header">
                  <el-avatar :size="40" :src="getAvatar" class="udc-avatar" />
                  <div class="udc-info">
                    <div class="udc-name">{{ store.user.username }}</div>
                    <div class="udc-role">{{ store.user.role === 'admin' ? '系统管理员' : '认证校友' }} · 珠海</div>
                  </div>
                </div>
                <div class="udc-divider"></div>
                <div class="udc-menu">
                  <div class="udc-item" @click="router.push('/home/user')">
                    <div class="udc-icon-wrap"><User :size="16" /></div>
                    <span>个人主页</span>
                  </div>
                  <div class="udc-item" @click="router.push('/home/user-setting')">
                    <div class="udc-icon-wrap"><Settings :size="16" /></div>
                    <span>账号设置</span>
                  </div>
                  <div class="udc-item" @click="router.push('/home/collections')">
                    <div class="udc-icon-wrap"><Bookmark :size="16" /></div>
                    <span>我的收藏</span>
                  </div>
                  <div v-if="isAdmin" class="udc-item" @click="router.push('/home/admin')">
                    <div class="udc-icon-wrap"><ShieldCheck :size="16" /></div>
                    <span>管理后台</span>
                  </div>
                </div>
                <div class="udc-divider"></div>
                <div class="udc-menu">
                  <div class="udc-item udc-logout" @click="userLogout">
                    <div class="udc-icon-wrap"><LogOut :size="16" /></div>
                    <span>退出登录</span>
                  </div>
                </div>
              </div>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <img src="/favicon.png" alt="Logo" class="sb-logo" />
        <div class="sb-text">
          <span class="sb-title">青研社</span>
          <span class="sb-sub">CAMPUS FORUM</span>
        </div>
      </div>
      <div class="sidebar-action" style="padding: 0 16px 12px">
        <button class="publish-btn" @click="editor = true">
          <Plus :size="18" />
          <span>发布新帖</span>
        </button>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-group" v-for="(group, gIdx) in navGroups" :key="gIdx">
          <div class="nav-group-title">{{ group.title }}</div>
          <a
            v-for="item in group.items"
            :key="item.label"
            class="nav-item"
            :class="{ active: item.path && (currentPath === item.path || (item.path !== '/home' && currentPath.startsWith(item.path))) }"
            @click="item.action ? item.action() : router.push(item.path)"
          >
            <component :is="item.icon" :size="17" class="nav-icon" />
            <span>{{ item.label }}</span>
          </a>
        </div>
        <a v-if="isAdmin" class="nav-item nav-admin" @click="router.push('/home/admin')">
          <ShieldCheck :size="17" class="nav-icon" />
          <span>管理后台</span>
        </a>
      </nav>
    </aside>

    <!-- Main -->
    <main class="main-area">
      <el-scrollbar style="height: calc(100vh - 60px)">
        <div class="main-content-wrap">
          <router-view v-slot="{ Component }">
            <transition name="el-fade-in-linear" mode="out-in">
              <component :is="Component" style="min-height: calc(100vh - 120px)" />
            </transition>
          </router-view>
        </div>
        <footer class="site-footer">
          <p>© {{ new Date().getFullYear() }} 青研社 · Campus Forum</p>
          <p>连接 · 发现 · 成长</p>
        </footer>
      </el-scrollbar>
    </main>

    <TopicEditor :show="editor" @success="() => { editor = false; router.push('/home'); }" @close="editor = false" />
  </div>
</template>

<style lang="less" scoped>
@sidebar-width: 240px;
@topbar-height: 56px;

.app-shell {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: #f4f5f9;
  html.dark & { background: var(--ds-bg); }
}

// Top Nav
.topbar {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: @topbar-height;
  background: #f4f5f9;
  z-index: 100;
  html.dark & { background: var(--ds-bg); }
}

.topbar-inner {
  height: 100%;
  padding: 0 40px 0 (@sidebar-width + 30px);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.search-input-wrap {
  width: 420px;
  max-width: 100%;
  height: 38px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: text;

  &:hover { background: var(--el-fill-color-light); border-color: var(--el-border-color-light); }
  &:focus-within {
    background: #fff;
    width: 480px;
    border-color: var(--el-color-primary-light-3);
    box-shadow: 0 4px 20px rgba(0,0,0,0.06);
    .search-icon { color: var(--el-color-primary); }
    .search-kbd { opacity: 0; transform: translateX(10px); }
  }
}

.search-icon {
  color: var(--el-text-color-placeholder);
  transition: color 0.3s;
}

.search-inner {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--el-text-color-primary);
  &::placeholder { color: var(--el-text-color-placeholder); font-size: 13px; font-weight: 500; }
}

.search-kbd {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color-lighter);
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 900;
  color: var(--el-text-color-placeholder);
  transition: all 0.2s;
  pointer-events: none;
  .cmd { font-size: 12px; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  width: 36px; height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--el-text-color-regular);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;

  &:hover { background: var(--el-fill-color-light); }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 10px 4px 6px;
  border-radius: 20px;
  transition: all 0.2s;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  
  &:hover { background: var(--el-fill-color-light); border-color: var(--el-color-primary-light-7); }
  
  .user-avatar {
    border: 2px solid var(--el-bg-color);
  }
}

.user-name-bubble {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// User dropdown card (rendered via popper)
.user-dropdown-card {
  width: 272px;
  padding: 8px 0;

  .udc-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;

    .udc-avatar { flex-shrink: 0; border: 2px solid var(--el-border-color-lighter); }
    .udc-info {
      min-width: 0;
      .udc-name { font-size: 15px; font-weight: 700; color: var(--el-text-color-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .udc-role { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
    }
  }

  .udc-divider {
    height: 1px;
    background: var(--el-border-color-lighter);
    margin: 4px 0;
  }

  .udc-menu {
    padding: 4px 8px;
  }

  .udc-item {
    display: flex;
    align-items: center;
    gap: 12px;
    height: 46px;
    padding: 0 10px;
    border-radius: 10px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    transition: background 0.15s;

    &:hover {
      background: var(--el-color-primary-light-9);
    }

    .udc-icon-wrap {
      width: 32px; height: 32px;
      border-radius: 8px;
      background: var(--el-fill-color-light);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      color: var(--el-text-color-regular);
    }
  }

  .udc-logout {
    color: var(--el-text-color-secondary);
    &:hover {
      background: #fff0f0;
      color: #e11d48;
      html.dark & { background: rgba(225,29,72,0.12); }
      .udc-icon-wrap { background: #ffe4e6; color: #e11d48; html.dark & { background: rgba(225,29,72,0.2); } }
    }
  }
}

// Sidebar
.sidebar {
  position: fixed;
  top: 0; left: 0;
  width: @sidebar-width;
  height: 100vh;
  background: #f4f5f9;
  html.dark & { background: var(--ds-bg); }
  display: flex;
  flex-direction: column;
  z-index: 101;
}

.sidebar-brand {
  padding: 24px 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;

  .sb-logo {
    width: 34px;
    height: 34px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }
  
  .sb-title {
    display: block;
    font-size: 18px;
    font-weight: 800;
    color: #2b3a8a;
    letter-spacing: 0.5px;
  }
  .sb-sub {
    display: block;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #8e95a5;
    margin-top: 4px;
  }
}

.sidebar-nav {
  flex: 1;
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.nav-group {
  display:flex;
  flex-direction: column;
  gap: 4px;
}

.nav-group-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
  padding: 0 10px;
  margin-bottom: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;

  .nav-icon { flex-shrink: 0; color: var(--el-text-color-secondary); }

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    
    .nav-icon { color: var(--el-color-primary); }
  }

  &.nav-admin {
    color: var(--el-color-warning);
    .nav-icon { color: var(--el-color-warning); }
    &:hover { background: var(--el-color-warning-light-9); color: var(--el-color-warning-dark-2); }
  }
}

.sidebar-footer {
  padding: 20px;
}

.publish-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 0;
  border: none;
  border-radius: 8px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px var(--el-color-primary-light-5);

  &:hover { 
    transform: translateY(-2px);
    box-shadow: 0 6px 16px var(--el-color-primary-light-3);
    background: var(--el-color-primary-dark-2); 
  }
}

// Main
.main-area {
  margin-left: @sidebar-width;
  margin-top: @topbar-height;
  flex: 1;
  min-width: 0;
  height: calc(100vh - @topbar-height);
  background: var(--el-bg-color);
  border-top-left-radius: 24px;
  box-shadow: -4px 0 20px rgba(0,0,0,0.03);
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.main-content-wrap {
  min-height: calc(100vh - 120px - 70px); // Content minus height of top and footer areas
}

.site-footer {
  text-align: center;
  padding: 30px 0;
  font-size: 12px;
  color: var(--ds-muted);
  line-height: 2;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 20px;
  html.dark & { border-color: var(--el-border-color-dark); }
}

.notif-item {
  cursor: pointer;
  transition: .2s;
  &:hover { opacity: 0.75; }
}
</style>
