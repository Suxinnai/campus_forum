<script setup>
import {get, logout} from "@/net/api.js";
import {useCounterStore} from "@/stores/counter.js"
import {computed, reactive, ref} from "vue";
import {
  Back,
  Bell,
  ChatDotSquare, Check, Collection, DataLine,
  Document, Files,
  Location, Lock, Message, Moon, Notification, Operation,
  Position,
  School, Search, Sunny,
  Umbrella, User
} from "@element-plus/icons-vue";
import router from "@/router/index.js";
import axios from "axios";
import LightCard from "@/components/LightCard.vue";
import {ElMessage} from "element-plus";
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
  get(`/api/notification/delete?id=${id}`, () => {
    loadNotification()
    window.open(url);
  })
}

function deleteAllNotification() {
  get("/api/notification/delete-all", () => {
    loadNotification()
  })
}

const getAvatar = computed(() => {
  return store.user.avatar ? `${axios.defaults.baseURL}/images${store.user.avatar}` : "https://www.vexipui.com/qmhc.jpg";
});

const dark = ref(useDark())

const sidebarCollapse = ref(false)
</script>

<template>
  <div class="main-content" v-loading="loading" element-loading-text="正在加载信息, 请稍后...">
    <el-container style="height: 100%">
      <el-header class="main-content-header">
        <div class="logo-area" @click="router.push('/index')">
          <el-icon class="logo-icon" :size="28" color="#0d4a75"><School /></el-icon>
          <span class="logo-text">青研社</span>
        </div>
        <div class="header-search">
          <el-input v-model="searchInput.text" class="search-input" placeholder="搜索论坛相关内容..." clearable>
            <template #prefix>
              <el-icon><Search/></el-icon>
            </template>
            <template #append>
              <el-select style="width: 120px" v-model="searchInput.type">
                <el-option value="1" label="帖子广场"/>
                <el-option value="2" label="校园活动"/>
                <el-option value="3" label="表白墙"/>
                <el-option value="4" label="教务通知"/>
              </el-select>
            </template>
          </el-input>
        </div>
        <div class="user-info">
          <el-switch
              class="theme-switch"
              v-model="dark" :active-action-icon="Moon" :inactive-action-icon="Sunny"/>
          <el-popover placement="bottom" :width="350" trigger="click">
            <template #reference>
              <el-badge style="margin-right: 15px" is-dot :hidden="!notification.length">
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
                <el-drawer style="margin: 7px 0 3px 0"/>
                <div style="font-size: 13px;color: grey">
                  {{item.content}}
                </div>
              </light-card>
            </el-scrollbar>
            <div style="margin-top: 10px">
              <el-button size="small" type="info" :icon="Check" @click="deleteAllNotification" style="width: 100%" plain>清除全部未读消息</el-button>
            </div>
          </el-popover>
          <div class="profile">
            <div>{{ store.user.username }}</div>
            <div>{{ store.user.email }}</div>
          </div>
          <el-dropdown>
            <el-avatar :src="getAvatar" :size="36"/>
            <template #dropdown>
              <el-dropdown-item @click="router.push('/index/user-setting')">
                <el-icon><Operation/></el-icon>
                个人设置
              </el-dropdown-item>
              <el-dropdown-item>
                <el-icon><Message/></el-icon>
                消息列表
              </el-dropdown-item>
              <el-dropdown-item @click="logout" divided>
                <el-icon><Back/></el-icon>
                退出登录
              </el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside :width="sidebarCollapse ? '64px' : '220px'" class="sidebar">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <el-menu
                router
                :collapse="sidebarCollapse"
                :default-active="$route.path"
                :default-openeds="['1', '2', '3']"
                class="sidebar-menu"
            >
              <el-sub-menu index="1">
                <template #title>
                  <el-icon><Location/></el-icon>
                  <span>校园社区</span>
                </template>
                <el-menu-item index="/index">
                  <template #title>
                    <el-icon><ChatDotSquare/></el-icon>
                    帖子广场
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/lost-found">
                  <template #title>
                    <el-icon><Search/></el-icon>
                    失物招领
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/activity">
                  <template #title>
                    <el-icon><Notification/></el-icon>
                    校园活动
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/confession">
                  <template #title>
                    <el-icon><Umbrella/></el-icon>
                    表白墙
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-sub-menu index="2">
                <template #title>
                  <el-icon><Position/></el-icon>
                  <span>学习工具</span>
                </template>
                <el-menu-item index="/index/resource">
                  <template #title>
                    <el-icon><Files/></el-icon>
                    资源共享
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/stat">
                  <template #title>
                    <el-icon><DataLine/></el-icon>
                    数据可视化
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/grade">
                  <template #title>
                    <el-icon><Document/></el-icon>
                    成绩查询
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/notice">
                  <template #title>
                    <el-icon><Monitor/></el-icon>
                    教务通知
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/library">
                  <template #title>
                    <el-icon><Collection/></el-icon>
                    在线图书馆
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-sub-menu index="3">
                <template #title>
                  <el-icon><Operation/></el-icon>
                  <span>个人设置</span>
                </template>
                <el-menu-item index="/index/user-setting">
                  <template #title>
                    <el-icon><User/></el-icon>
                    个人信息设置
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/privacy-setting">
                  <template #title>
                    <el-icon><Lock/></el-icon>
                    账号安全设置
                  </template>
                </el-menu-item>
              </el-sub-menu>
            </el-menu>
          </el-scrollbar>
          <div class="sidebar-toggle" @click="sidebarCollapse = !sidebarCollapse">
            <el-icon :size="14">
              <Back v-if="!sidebarCollapse"/>
              <Position v-else/>
            </el-icon>
          </div>
        </el-aside>
        <el-main class="main-content-page">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <router-view v-slot="{ Component }">
              <transition name="el-fade-in-linear" mode="out-in">
                <component :is="Component" style="height: 100%" />
              </transition>
            </router-view>
          </el-scrollbar>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="less" scoped>
.notification-item {
  transition: 0.3s;

  &:hover {
    cursor: pointer;
    opacity: 0.7;
  }
}

.notification {
  font-size: 22px;
  line-height: 14px;
  text-align: center;
  transition: 0.3s;

  &:hover {
    color: grey;
    cursor: pointer;
  }
}

.main-content-page {
  padding: 0;
  background-color: #f5f6f8;
}

.dark .main-content-page {
  background-color: #1a1a1e;
}

.main-content {
  height: 100vh;
  width: 100vw;
}

/* ===== Header ===== */
.main-content-header {
  border-bottom: 1px solid var(--el-border-color);
  height: 55px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  background: var(--ice-bg-gradient);
  padding: 0 20px;

  .logo-area {
    display: flex;
    align-items: center;
    cursor: pointer;
    flex-shrink: 0;
    transition: opacity 0.2s;

    &:hover { opacity: 0.85; }

    .logo-icon {
      font-size: 26px;
      margin-right: 8px;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 700;
      color: #0d4a75;
      letter-spacing: 1px;
    }
  }

  .header-search {
    flex: 1;
    padding: 0 30px;
    display: flex;
    justify-content: center;

    .search-input {
      width: 100%;
      max-width: 480px;
    }
  }

  .theme-switch {
    --el-switch-on-color: #424242;
    margin-right: 20px;
  }

  .user-info {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    flex-shrink: 0;

    .el-avatar {
      cursor: pointer;
      border: 2px solid rgba(255, 255, 255, 0.5);
      transition: border-color 0.2s;

      &:hover { border-color: #fff; }
    }
  }

  .profile {
    text-align: right;
    margin-right: 15px;

    :first-child {
      font-size: 15px;
      font-weight: 600;
      line-height: 20px;
      color: #0d4a75;
    }

    :last-child {
      font-size: 10px;
      color: rgba(13, 74, 117, 0.7);
    }
  }

  .notification {
    color: #0d4a75;

    &:hover {
      color: #8ad8ff;
    }
  }
}

/* ===== Sidebar ===== */
.sidebar {
  position: relative;
  transition: width 0.25s ease;
  border-right: 1px solid var(--el-border-color);

  .sidebar-menu {
    border-right: none;
    min-height: calc(100vh - 55px - 36px);

    :deep(.el-sub-menu__title) {
      font-weight: 600;
      font-size: 13px;
    }

    :deep(.el-menu-item) {
      font-size: 13px;
      border-radius: 6px;
      margin: 2px 8px;
      height: 42px;

      &.is-active {
        background: linear-gradient(135deg, #667eea20, #764ba220);
        color: #667eea;
        font-weight: 600;
      }
    }
  }

  .sidebar-toggle {
    position: absolute;
    bottom: 0;
    width: 100%;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-top: 1px solid var(--el-border-color);
    color: var(--el-text-color-secondary);
    transition: background-color 0.2s, color 0.2s;

    &:hover {
      background-color: var(--el-fill-color-light);
      color: var(--el-text-color-primary);
    }
  }
}
</style>