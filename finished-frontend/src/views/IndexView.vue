<script setup>
import {get, del, logout} from "@/net/api.js";
import {useCounterStore} from "@/stores/counter.js"
import {computed, reactive, ref} from "vue";
import {
  Bell, Check, MapPin, Tent, SearchCode, Heart, BookOpen, FileText,
  LineChart, BellRing, BookMarked, Settings, Mail, LogOut, PanelLeftClose,
  Sun, Moon, Hash, User, Search
} from "lucide-vue-next";
import router from "@/router/index.js";
import axios from "axios";
import LightCard from "@/components/LightCard.vue";
import {useDark} from "@vueuse/core";

const store = useCounterStore()
const loading = ref(true);

const searchInput = reactive({
  type: "1",
  text: ""
})

get("/api/user/info", (data) => {
  store.user = data;
  loading.value = false;
})

const notification = ref([])

const loadNotification = () => {
  get("/api/notification/list", data => {
    notification.value = data;
  })
}

loadNotification()

function confirmNotification(id, url) {
  del(`/api/notification/delete?id=${id}`, () => {
    loadNotification()
    window.open(url);
  })
}

function deleteAllNotification() {
  del("/api/notification/delete-all", () => {
    loadNotification()
  })
}

const getAvatar = computed(() => {
  return store.user.avatar ? `${axios.defaults.baseURL}/images${store.user.avatar}` : "https://www.vexipui.com/qmhc.jpg";
});

const dark = ref(useDark())

const sidebarCollapse = ref(false);
</script>

<template>
  <div class="main-content" v-loading="loading" element-loading-text="正在加载信息, 请稍后...">
    <el-container style="height: 100vh">
      <el-header class="main-content-header glass-effect">
        <div class="header-left">
          <div class="logo-area" @click="router.push('/index')">
            <img src="/favicon.png" alt="Logo" class="logo-image" />
            <span class="logo-text">青研社</span>
          </div>
        </div>
        
        <div class="header-center">
          <el-input v-model="searchInput.text" class="global-search-input" placeholder=" 全局搜索... (帖子/用户/活动)" clearable>
            <template #prefix>
              <el-icon><Search/></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="header-right">
          <div class="theme-switch-btn" @click="dark = !dark">
            <el-icon><Moon v-if="dark"/><Sun v-else/></el-icon>
          </div>
          
          <el-popover placement="bottom" :width="350" trigger="click">
            <template #reference>
              <el-badge is-dot :hidden="!notification.length">
                <div class="notification">
                  <el-icon><Bell/></el-icon>
                  <div style="font-size: 10px">消息</div>
                </div>
              </el-badge>
            </template>
            <el-empty :image-size="80" description="没有未读消息" v-if="!notification.length"/>
            <el-scrollbar :max-height="500" v-else>
              <light-card v-for="item in notification" class="notification-item" @click="confirmNotification(item.id, item.url)">
                <div>
                  <el-tag :type="item.type">消息</el-tag>&nbsp;
                  <span style="font-weight: bold">{{item.title}}</span>
                </div>
                <el-divider style="margin: 7px 0 3px 0"/>
                <div style="font-size: 13px;color: grey">
                  {{item.content}}
                </div>
              </light-card>
            </el-scrollbar>
            <div style="margin-top: 10px">
              <el-button size="small" type="info" :icon="Check" @click="deleteAllNotification" style="width: 100%" plain>清除全部未读消息</el-button>
            </div>
          </el-popover>
          
          <el-dropdown>
            <el-avatar :src="getAvatar"/>
            <template #dropdown>
              <el-dropdown-item @click="router.push('/index/user-setting')">
                <el-icon><Settings/></el-icon>个人设置
              </el-dropdown-item>
              <el-dropdown-item>
                <el-icon><Mail/></el-icon>消息列表
              </el-dropdown-item>
              <el-dropdown-item @click="logout" divided>
                <el-icon><LogOut/></el-icon>退出登录
              </el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-container>
        <el-aside :width="sidebarCollapse ? '72px' : '230px'" class="sidebar">
          <el-scrollbar style="height: calc(100vh - 60px)">
            <el-menu
                router
                :collapse="sidebarCollapse"
                :default-active="router.currentRoute.value.path"
                :default-openeds="['1', '2', '3']"
                class="sidebar-menu"
            >
              <el-menu-item index="/index">
                <template #title>
                  <el-icon><Hash/></el-icon>
                  <span>帖子广场</span>
                </template>
              </el-menu-item>

              <el-sub-menu index="1">
                <template #title>
                  <el-icon><MapPin/></el-icon>
                  <span>校园探索</span>
                </template>
                <el-menu-item index="/index/activity">
                  <template #title>
                    <el-icon><Tent/></el-icon>校园活动
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/lost-found">
                  <template #title>
                    <el-icon><SearchCode/></el-icon>失物招领
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/confession">
                  <template #title>
                    <el-icon><Heart/></el-icon>表白墙
                  </template>
                </el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="2">
                <template #title>
                  <el-icon><BookOpen/></el-icon>
                  <span>学习资源</span>
                </template>
                <el-menu-item index="/index/resource">
                  <template #title>
                    <el-icon><FileText/></el-icon>学习资料
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/grade">
                  <template #title>
                    <el-icon><LineChart/></el-icon>成绩查询
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/notice">
                  <template #title>
                    <el-icon><BellRing/></el-icon>教务通知
                  </template>
                </el-menu-item>

              </el-sub-menu>

              <el-sub-menu index="3">
                <template #title>
                  <el-icon><User/></el-icon>
                  <span>个人中心</span>
                </template>
                <el-menu-item index="/index/user-setting">
                  <template #title>
                    <el-icon><Settings/></el-icon>个人设置
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/stat">
                  <template #title>
                    <el-icon><LineChart/></el-icon>数据统计
                  </template>
                </el-menu-item>
              </el-sub-menu>
            </el-menu>
          </el-scrollbar>
          <div class="collapse-btn" @click="sidebarCollapse = !sidebarCollapse">
            <el-icon><PanelLeftClose/></el-icon>
          </div>
        </el-aside>
        
        <el-main class="main-content-page">
          <el-scrollbar style="height: calc(100vh - 60px)">
            <router-view v-slot="{ Component }">
              <transition name="el-fade-in-linear" mode="out-in">
                <component :is="Component" style="min-height: calc(100vh - 120px)" />
              </transition>
            </router-view>
            <div class="global-footer">
              <p>© {{ new Date().getFullYear() }} 青研社 (Qingyan Society). 版权所有.</p>
              <p>连接·发现·成长 — 你的校园数字新生活</p>
            </div>
          </el-scrollbar>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="less" scoped>
.main-content {
  height: 100vh;
  width: 100vw;
}

.main-content-page {
  padding: 0;
  background-color: var(--el-bg-color-page);
}

/* 高斯模糊卡片及整体设计 */
.glass-effect {
  background-color: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

html.dark .glass-effect {
  background-color: rgba(30, 30, 30, 0.7) !important;
}

/* ===== Header ===== */
.main-content-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  padding: 0 24px;
  z-index: 10;
  
  .header-left {
    width: 250px;
    display: flex;
    align-items: center;
  }
  
  .header-center {
    flex: 1;
    display: flex;
    justify-content: center;
  }
  
  .header-right {
    width: 250px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }

  .logo-area {
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: opacity 0.2s;
    
    .logo-image {
      width: 28px;
      height: 28px;
      margin-right: 12px;
      border-radius: 8px; /* 更圆润的logo */
      object-fit: cover;
    }
    
    .logo-text {
      font-size: 18px;
      font-weight: 800;
      color: var(--el-text-color-primary);
      letter-spacing: 1px;
    }
    &:hover { opacity: 0.8; }
  }

  .global-search-input {
    width: 100%;
    max-width: 600px;
    height: 40px;

    :deep(.el-input__wrapper) {
      background-color: var(--el-fill-color-light) !important;
      border-radius: 20px !important;
      box-shadow: none !important;
      padding-left: 18px;
      transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
      border: 1px solid transparent;

      &:hover { background-color: var(--el-fill-color) !important; }
      &.is-focus {
        background-color: rgba(255,255,255,0.95) !important;
        border-color: var(--el-color-primary);
        box-shadow: 0 0 0 4px var(--el-color-primary-light-8) !important;
      }
    }
    
    :deep(.el-input__inner) {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .theme-switch-btn {
    width: 36px;
    height: 36px;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
    font-size: 18px;
    color: var(--el-text-color-primary);
    background-color: transparent;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
    margin-right: 20px;

    &:hover {
      background-color: var(--el-fill-color);
      color: var(--el-color-primary);
      transform: scale(1.1);
    }
  }

  .el-avatar {
    cursor: pointer;
    border: 2px solid var(--el-border-color-lighter);
    transition: all 0.2s;
    border-radius: 50%; /* 保证头像纯圆形或顺应高斯模糊设定 */

    &:hover { 
      border-color: var(--el-color-primary);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
  }

  .profile {
    text-align: right;
    margin-right: 15px;

    :first-child {
      font-size: 14px;
      font-weight: 700;
      color: var(--el-text-color-primary);
    }

    :last-child {
      font-size: 11px;
      color: var(--el-text-color-secondary);
      margin-top: 2px;
    }
  }

  .notification {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-regular);
    transition: all 0.2s;
    cursor: pointer;
    margin-right: 20px;

    .el-icon {
      font-size: 20px;
    }

    &:hover {
      color: var(--el-color-primary);
    }
  }
}

/* ===== Sidebar ===== */
.sidebar {
  position: relative;
  transition: width 0.3s var(--transition-cubic);
  border-right: none;
  background-color: var(--el-bg-color);
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.02);
  z-index: 5;
  display: flex;
  flex-direction: column;

  .sidebar-menu {
    border-right: none;
    padding: 12px 10px;

    :deep(.el-sub-menu__title) {
      font-weight: 600;
      font-size: 13.5px;
      height: 50px;
    }

    :deep(.el-menu-item) {
      font-size: 13.5px;
      border-radius: 8px;
      margin: 4px 0;
      height: 44px;
      color: var(--el-text-color-regular);
      transition: all 0.2s ease-in-out;
      position: relative;
      
      &:hover {
        background-color: var(--el-fill-color-light);
        color: var(--el-color-primary);
      }

      &.is-active {
        background-color: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
        font-weight: 600;
        
        &::before {
          content: '';
          position: absolute;
          left: -10px;
          top: 15%;
          height: 70%;
          width: 4px;
          background-color: var(--el-color-primary);
          border-radius: 0 4px 4px 0;
        }
      }
    }
  }

  .collapse-btn {
    height: 40px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--el-text-color-regular);
    cursor: pointer;
    border-top: 1px solid var(--el-border-color-lighter);
    transition: all 0.2s;
    
    &:hover {
      background-color: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }
  }
}

.notification-item {
  transition: .3s;
  &:hover {
    cursor: pointer;
    opacity: 0.7;
    background-color: var(--el-fill-color-light);
  }
}
</style>

<style scoped>
.global-footer {
  text-align: center;
  padding: 30px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.8;
  margin-top: 20px;
  background-color: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid rgba(255, 255, 255, 0.4);
}
html.dark .global-footer {
  background-color: rgba(30, 30, 30, 0.4);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
</style>
