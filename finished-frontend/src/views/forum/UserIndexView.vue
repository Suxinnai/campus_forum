<script setup>
import { useAppStore } from "@/stores/app-store.js";
import { computed, reactive, ref, onMounted } from "vue";
import { get } from "@/net/api.js";
import axios from "axios";
import { MessageSquare, Heart, Bookmark, Calendar, MapPin, Edit3, Award, Star, Zap } from 'lucide-vue-next';
import TopicTag from "@/components/TopicTag.vue";
import router from "@/router/index.js";

const store = useAppStore();
const activeTab = ref('posts');
const topics = reactive({ list: [], loading: true });
const stats = reactive({ posts: 0, comments: 12, likes: 89, bookmarks: 18 });

const getAvatar = computed(() =>
  store.user.avatar
    ? `${axios.defaults.baseURL}/images${store.user.avatar}`
    : "https://www.vexipui.com/qmhc.jpg"
);

const bannerError = ref(false);

function fetchUserTopics() {
  topics.loading = true;
  get("/api/forum/list-topic?page=0&type=0", data => {
    topics.list = data.filter(i => i.uid === store.user.id);
    stats.posts = topics.list.length;
    topics.loading = false;
  });
}

onMounted(fetchUserTopics);

const medals = [
  { icon: Award, label: '社区先锋', color: '#6366f1' },
  { icon: Zap, label: '发帖达人', color: '#f59e0b' },
  { icon: Star, label: '优质回答', color: '#10b981' },
];

const personalTags = [
  { text: '学习猿', bg: '#eef0ff', color: '#4b4fd6' },
  { text: '考公党', bg: '#ecfdf3', color: '#16a34a' },
  { text: '广东仔', bg: '#fff4e5', color: '#d97706' },
];
</script>

<template>
  <div class="user-index">
    <!-- Profile Header Card -->
    <div class="profile-header">
      <!-- Banner -->
      <div class="banner-wrap">
        <img
          v-if="!bannerError"
          src="https://images.unsplash.com/photo-1541339907198-e08756ebafe3?q=80&w=2070&auto=format&fit=crop"
          class="banner-img"
          @error="bannerError = true"
        />
        <div v-else class="banner-fallback"></div>
      </div>

      <!-- User info row -->
      <div class="header-body">
        <div class="avatar-ring">
          <el-avatar :size="96" :src="getAvatar" class="main-avatar" />
        </div>
        <div class="user-meta">
          <div class="name-row">
            <h1 class="user-name">{{ store.user.username }}</h1>
            <button class="edit-btn" @click="router.push('/home/user-setting')">
              <Edit3 :size="14" />
              编辑资料
            </button>
          </div>
          <p class="user-bio">{{ store.user.desc || '行到水穷处，坐看云起时。这个同学有点神秘，还没写简介呢。' }}</p>
          <div class="meta-row">
            <span class="meta-item"><Calendar :size="13" /> 2024年3月加入</span>
            <span class="meta-item"><MapPin :size="13" /> 广东 · 珠海</span>
          </div>
        </div>
      </div>

      <!-- Stats tabs -->
      <div class="stats-bar">
        <div class="stat-tab" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">
          <span class="stat-num">{{ stats.posts }}</span>
          <span class="stat-lbl">动态</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'comments' }" @click="activeTab = 'comments'">
          <span class="stat-num">{{ stats.comments }}</span>
          <span class="stat-lbl">评论</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'likes' }" @click="activeTab = 'likes'">
          <span class="stat-num">{{ stats.likes }}</span>
          <span class="stat-lbl">获赞</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'bookmarks' }" @click="activeTab = 'bookmarks'">
          <span class="stat-num">{{ stats.bookmarks }}</span>
          <span class="stat-lbl">收藏</span>
        </div>
      </div>
    </div>

    <!-- Main content -->
    <div class="index-content">
      <!-- Left: post feed -->
      <div class="content-main">
        <div v-if="activeTab === 'posts'" class="post-feed" v-loading="topics.loading">
          <el-empty v-if="!topics.loading && topics.list.length === 0" description="空空如也，去发个帖子吧" />
          <div
            v-for="item in topics.list"
            :key="item.id"
            class="post-card"
            @click="router.push('/home/topic/' + item.id)"
          >
            <div class="card-top">
              <TopicTag :type="item.type" />
              <span class="card-time">{{ new Date(item.time).toLocaleDateString() }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-text">{{ item.text }}</p>
            <div class="card-footer">
              <span class="card-stat"><MessageSquare :size="13" /> {{ item.commentCount || 0 }}</span>
              <span class="card-stat"><Heart :size="13" /> {{ item.like || 0 }}</span>
            </div>
          </div>
        </div>
        <div v-else class="placeholder-section">
          <el-empty description="该版块正在建设中..." />
        </div>
      </div>

      <!-- Right sidebar -->
      <div class="content-side">
        <!-- Tags -->
        <div class="side-card">
          <h4 class="side-title">个人标签</h4>
          <div class="tags-cloud">
            <span
              v-for="tag in personalTags"
              :key="tag.text"
              class="tag-pill"
              :style="{ background: tag.bg, color: tag.color }"
            >{{ tag.text }}</span>
          </div>
        </div>

        <!-- Medals -->
        <div class="side-card">
          <h4 class="side-title">成就勋章</h4>
          <div class="medals-grid">
            <div v-for="m in medals" :key="m.label" class="medal-badge" :title="m.label">
              <div class="medal-icon" :style="{ background: m.color + '18', color: m.color }">
                <component :is="m.icon" :size="18" />
              </div>
              <span class="medal-label">{{ m.label }}</span>
            </div>
          </div>
        </div>

        <!-- Recent bookmarks -->
        <div class="side-card">
          <h4 class="side-title">最近收藏</h4>
          <div class="recent-empty">
            <Bookmark :size="20" style="color:var(--el-text-color-placeholder)" />
            <span>暂无收藏</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.user-index {
  max-width: 1060px;
  margin: 28px auto;
  padding: 0 4px;
}

/* ── Profile Header ── */
.profile-header {
  background: var(--el-bg-color);
  border-radius: 20px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  overflow: hidden;
  margin-bottom: 24px;
}

.banner-wrap {
  height: 210px;
  overflow: hidden;
  background: linear-gradient(135deg, #c7d2fe 0%, #a5b4fc 50%, #818cf8 100%);
}

.banner-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}

.banner-fallback {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #c7d2fe 0%, #a5b4fc 50%, #818cf8 100%);
}

.header-body {
  display: flex;
  padding: 0 36px 24px;
  position: relative;
  margin-top: -44px;
  gap: 20px;
}

.avatar-ring {
  flex-shrink: 0;
  :deep(.el-avatar) {
    border: 4px solid var(--el-bg-color);
    box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  }
}

.user-meta {
  flex: 1;
  padding-top: 52px;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.user-name {
  font-size: 28px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0;
  line-height: 1.2;
}

.edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1.5px solid var(--el-border-color);
  background: transparent;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.user-bio {
  font-size: 15px;
  color: var(--el-text-color-regular);
  line-height: 1.65;
  margin: 0 0 12px;
}

.meta-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stats-bar {
  display: flex;
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 0 36px;
}

.stat-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 28px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  border-radius: 0;

  .stat-num {
    font-size: 20px;
    font-weight: 800;
    color: var(--el-text-color-primary);
    line-height: 1;
  }

  .stat-lbl {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }

  &:hover { background: var(--el-fill-color-lighter); }

  &.active {
    .stat-num { color: var(--el-color-primary); }
    .stat-lbl { color: var(--el-color-primary); }
    &::after {
      content: '';
      position: absolute;
      bottom: 0; left: 20px; right: 20px;
      height: 3px;
      background: var(--el-color-primary);
      border-radius: 3px 3px 0 0;
    }
  }
}

/* ── Content Grid ── */
.index-content {
  display: grid;
  grid-template-columns: 1fr 288px;
  gap: 24px;
}

/* ── Post Feed ── */
.post-feed {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 22px 24px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
  box-shadow: 0 1px 4px rgba(0,0,0,.03);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0,0,0,0.08);
    border-color: var(--el-color-primary-light-7);
  }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 10px;
  line-height: 1.4;
}

.card-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.65;
  margin: 0 0 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  overflow: hidden;
}

.card-footer {
  display: flex;
  gap: 16px;
}

.card-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.placeholder-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 40px;
}

/* ── Side Cards ── */
.content-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.03);
}

.side-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 14px;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.medals-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.medal-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  transition: background 0.2s;
  cursor: default;

  &:hover { background: var(--el-fill-color-light); }
}

.medal-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.medal-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  text-align: center;
}

.recent-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  span { font-size: 13px; color: var(--el-text-color-placeholder); }
}

@media (max-width: 900px) {
  .index-content { grid-template-columns: 1fr; }
  .content-side { order: -1; }
  .header-body { padding: 0 20px 20px; }
  .stats-bar { padding: 0 20px; }
  .stat-tab { padding: 14px 16px; }
}
</style>
