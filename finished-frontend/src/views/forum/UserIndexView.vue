<script setup>
import { computed, onActivated, onMounted, reactive, ref } from "vue";
import { get } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";
import axios from "axios";
import { MessageSquare, Heart, Bookmark, Calendar, MapPin, Edit3, Award, Star, Zap, Shield } from "lucide-vue-next";
import TopicTag from "@/components/TopicTag.vue";
import router from "@/router/index.js";
import { ElMessage } from "element-plus";

const store = useAppStore();
const activeTab = ref("posts");
const bannerError = ref(false);

const center = reactive({
  loading: true,
  account: null,
  details: null,
  stats: { posts: 0, comments: 0, likes: 0, bookmarks: 0 },
  posts: [],
  bookmarks: [],
});

const geo = store.geo;

function loadCenter() {
  center.loading = true;
  get("/api/user/center", data => {
    center.account = data.account;
    center.details = data.details;
    center.stats = data.stats;
    center.posts = data.posts || [];
    center.bookmarks = data.bookmarks || [];
    center.loading = false;
  }, message => {
    center.loading = false;
    ElMessage.error(message || "个人中心数据加载失败");
  });
}


onMounted(() => {
  loadCenter();
  store.loadGeo();
  if (!store.forum._typesLoaded) {
    store.forum._typesLoaded = true;
    get("/api/forum/types", data => {
      store.forum.types = [
        { name: "全部", id: 0, color: "linear-gradient(45deg, white, red, orange, gold, green, blue)" },
        ...data
      ];
    });
  }
});

onActivated(() => {
  loadCenter();
});

const avatarUrl = computed(() =>
  store.getAvatar(center.account?.avatar, center.account?.username || store.user.username)
);

const bannerUrl = computed(() =>
  center.details?.cover ? `${axios.defaults.baseURL}/images${center.details.cover}` : ""
);

const joinedLabel = computed(() => {
  if (!center.account?.registerTime) return "加入时间待补充";
  const date = new Date(center.account.registerTime);
  return `${date.getFullYear()}年${date.getMonth() + 1}月加入`;
});

const userBio = computed(() =>
  center.details?.desc || "行到水穷处，坐看云起时。这个同学有点神秘，还没写简介呢。"
);

const medals = computed(() => {
  const result = [];
  if (center.account?.role === "admin") {
    result.push({ icon: Shield, label: "系统管理员", color: "#6366f1" });
  }
  if (center.stats.posts > 0) {
    result.push({ icon: Zap, label: "发帖达人", color: "#f59e0b" });
  }
  if (center.stats.likes > 0) {
    result.push({ icon: Star, label: "被看见了", color: "#10b981" });
  }
  if (result.length === 0) {
    result.push({ icon: Award, label: "社区新星", color: "#6366f1" });
  }
  return result.slice(0, 3);
});

const personalTags = computed(() => {
  const tags = [];
  if (center.account?.role === "admin") {
    tags.push({ text: "管理员", bg: "#eef2ff", color: "#4f46e5" });
  }
  if (geo.city || geo.region) {
    tags.push({ text: geo.city || geo.region, bg: "#eff6ff", color: "#0284c7" });
  }
  const tagCounter = new Map();
  center.posts.forEach(item => (item.tags || []).forEach(tag => tagCounter.set(tag, (tagCounter.get(tag) || 0) + 1)));
  [...tagCounter.entries()].slice(0, 3).forEach(([tag], index) => {
    const palette = [
      { bg: "#ecfdf3", color: "#16a34a" },
      { bg: "#fff4e5", color: "#d97706" },
      { bg: "#fff1f2", color: "#e11d48" },
    ];
    tags.push({ text: tag, ...palette[index % palette.length] });
  });
  if (!tags.length) {
    tags.push({ text: "校园新朋友", bg: "#f8fafc", color: "#475569" });
  }
  return tags.slice(0, 5);
});
</script>

<template>
  <div class="user-index" v-loading="center.loading">
    <div class="profile-header">
      <div class="banner-wrap">
        <img
          v-if="bannerUrl && !bannerError"
          :src="bannerUrl"
          class="banner-img"
          @error="bannerError = true"
        />
        <div v-else class="banner-fallback"></div>
      </div>

      <div class="header-body">
        <div class="avatar-ring">
          <el-avatar :size="96" :src="avatarUrl" class="main-avatar" />
        </div>
        <div class="user-meta">
          <div class="name-row">
            <h1 class="user-name">{{ center.account?.username || store.user.username }}</h1>
            <button class="edit-btn" @click="router.push('/home/user-setting')">
              <Edit3 :size="14" />
              编辑资料
            </button>
          </div>
          <p class="user-bio">{{ userBio }}</p>
          <div class="meta-row">
            <span class="meta-item"><Calendar :size="13" /> {{ joinedLabel }}</span>
            <span class="meta-item"><MapPin :size="13" /> {{ geo.label }}</span>
          </div>
        </div>
      </div>

      <div class="stats-bar">
        <div class="stat-tab" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">
          <span class="stat-num">{{ center.stats.posts }}</span>
          <span class="stat-lbl">动态</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'comments' }" @click="activeTab = 'comments'">
          <span class="stat-num">{{ center.stats.comments }}</span>
          <span class="stat-lbl">评论</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'likes' }" @click="activeTab = 'likes'">
          <span class="stat-num">{{ center.stats.likes }}</span>
          <span class="stat-lbl">获赞</span>
        </div>
        <div class="stat-tab" :class="{ active: activeTab === 'bookmarks' }" @click="activeTab = 'bookmarks'">
          <span class="stat-num">{{ center.stats.bookmarks }}</span>
          <span class="stat-lbl">收藏</span>
        </div>
      </div>
    </div>

    <div class="index-content">
      <div class="content-main">
        <div v-if="activeTab === 'posts'" class="post-feed">
          <el-empty v-if="center.posts.length === 0" description="空空如也，去发个帖子吧" />
          <div
            v-for="item in center.posts"
            :key="item.id"
            class="post-card"
            @click="router.push('/home/topic/' + item.id)"
          >
            <div class="card-top">
              <TopicTag :type="item.type" :tags="item.tags" />
              <span class="card-time">{{ new Date(item.time).toLocaleDateString() }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-text">{{ item.text }}</p>
            <div class="card-footer">
              <span class="card-stat"><MessageSquare :size="13" /> {{ item.comments || 0 }}</span>
              <span class="card-stat"><Heart :size="13" /> {{ item.like || 0 }}</span>
              <span class="card-stat"><Bookmark :size="13" /> {{ item.collect || 0 }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'bookmarks'" class="post-feed">
          <el-empty v-if="center.bookmarks.length === 0" description="暂无收藏内容" />
          <div
            v-for="item in center.bookmarks"
            :key="item.id"
            class="post-card"
            @click="router.push('/home/topic/' + item.id)"
          >
            <div class="card-top">
              <TopicTag :type="item.type" :tags="item.tags" />
              <span class="card-time">{{ new Date(item.time).toLocaleDateString() }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-text">{{ item.text }}</p>
          </div>
        </div>

        <div v-else class="placeholder-section">
          <el-empty description="该版块正在建设中..." />
        </div>
      </div>

      <div class="content-side">
        <div class="side-card">
          <h4 class="side-title">资料卡</h4>
          <div class="profile-brief">
            <div class="brief-item"><span>邮箱</span><strong>{{ center.account?.email || "未绑定" }}</strong></div>
            <div class="brief-item"><span>手机号</span><strong>{{ center.details?.phone || "未填写" }}</strong></div>
            <div class="brief-item"><span>QQ / 微信</span><strong>{{ center.details?.qq || "未填写" }}</strong></div>
            <div class="brief-item"><span>IP 定位</span><strong>{{ geo.label }}</strong></div>
          </div>
          <div v-if="geo.ip" class="brief-foot">当前识别 IP：{{ geo.ip }}</div>
        </div>

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

        <div class="side-card">
          <h4 class="side-title">成就勋章</h4>
          <div class="medals-grid">
            <div v-for="m in medals" :key="m.label" class="medal-badge">
              <div class="medal-icon" :style="{ background: m.color + '18', color: m.color }">
                <component :is="m.icon" :size="18" />
              </div>
              <span class="medal-label">{{ m.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.user-index { max-width: 1060px; margin: 28px auto; padding: 0 4px; }
.profile-header { background: var(--el-bg-color); border-radius: 20px; border: 1px solid var(--el-border-color-lighter); box-shadow: 0 2px 12px rgba(0,0,0,.04); overflow: hidden; margin-bottom: 24px; }
.banner-wrap { height: 210px; overflow: hidden; background: linear-gradient(135deg, #c7d2fe 0%, #a5b4fc 50%, #818cf8 100%); }
.banner-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.banner-fallback { width: 100%; height: 100%; background: linear-gradient(135deg, #c7d2fe 0%, #a5b4fc 50%, #818cf8 100%); }
.header-body { display: flex; padding: 0 36px 24px; position: relative; margin-top: -44px; gap: 20px; }
.avatar-ring { flex-shrink: 0; :deep(.el-avatar) { border: 4px solid var(--el-bg-color); box-shadow: 0 4px 16px rgba(0,0,0,.12); } }
.user-meta { flex: 1; padding-top: 52px; min-width: 0; }
.name-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.user-name { font-size: 28px; font-weight: 800; color: var(--el-text-color-primary); margin: 0; line-height: 1.2; }
.edit-btn { display: inline-flex; align-items: center; gap: 6px; height: 36px; padding: 0 16px; border-radius: 10px; border: 1.5px solid var(--el-border-color); background: transparent; color: var(--el-text-color-regular); font-size: 13px; font-weight: 600; cursor: pointer; transition: all .2s; white-space: nowrap; }
.edit-btn:hover { border-color: var(--el-color-primary-light-5); color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.user-bio { font-size: 15px; color: var(--el-text-color-regular); line-height: 1.65; margin: 0 0 12px; }
.meta-row { display: flex; gap: 20px; flex-wrap: wrap; }
.meta-item { display: flex; align-items: center; gap: 5px; font-size: 13px; color: var(--el-text-color-secondary); }
.stats-bar { display: flex; border-top: 1px solid var(--el-border-color-lighter); padding: 0 36px; }
.stat-tab { display: flex; flex-direction: column; align-items: center; padding: 16px 28px; cursor: pointer; position: relative; transition: background .2s; border-radius: 0; }
.stat-num { font-size: 20px; font-weight: 800; color: var(--el-text-color-primary); line-height: 1; }
.stat-lbl { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.stat-tab:hover { background: var(--el-fill-color-lighter); }
.stat-tab.active .stat-num, .stat-tab.active .stat-lbl { color: var(--el-color-primary); }
.stat-tab.active::after { content: ''; position: absolute; bottom: 0; left: 20px; right: 20px; height: 3px; background: var(--el-color-primary); border-radius: 3px 3px 0 0; }
.index-content { display: grid; grid-template-columns: 1fr 288px; gap: 24px; }
.post-feed { display: flex; flex-direction: column; gap: 14px; }
.post-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 16px; padding: 22px 24px; cursor: pointer; transition: all .25s cubic-bezier(.4,0,.2,1); box-shadow: 0 1px 4px rgba(0,0,0,.03); }
.post-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,.08); border-color: var(--el-color-primary-light-7); }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.card-time { font-size: 12px; color: var(--el-text-color-placeholder); }
.card-title { font-size: 17px; font-weight: 700; color: var(--el-text-color-primary); margin: 0 0 10px; line-height: 1.4; }
.card-text { font-size: 14px; color: var(--el-text-color-secondary); line-height: 1.65; margin: 0 0 14px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-footer { display: flex; gap: 16px; }
.card-stat { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--el-text-color-placeholder); }
.placeholder-section { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 16px; padding: 40px; }
.content-side { display: flex; flex-direction: column; gap: 16px; }
.side-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 16px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,.03); }
.side-title { font-size: 14px; font-weight: 700; color: var(--el-text-color-primary); margin: 0 0 14px; }
.profile-brief { display: flex; flex-direction: column; gap: 10px; }
.brief-item { display: flex; flex-direction: column; gap: 4px; padding-bottom: 10px; border-bottom: 1px solid var(--el-border-color-extra-light); }
.brief-item:last-child { border-bottom: none; padding-bottom: 0; }
.brief-item span { font-size: 12px; color: var(--el-text-color-placeholder); }
.brief-item strong { font-size: 13px; color: var(--el-text-color-primary); word-break: break-word; }
.brief-foot { margin-top: 12px; font-size: 12px; color: var(--el-text-color-secondary); }
.tags-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-pill { display: inline-flex; align-items: center; height: 26px; padding: 0 10px; border-radius: 999px; font-size: 12px; font-weight: 600; line-height: 1; }
.medals-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.medal-badge { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 12px 8px; border-radius: 10px; background: var(--el-fill-color-lighter); }
.medal-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.medal-label { font-size: 11px; color: var(--el-text-color-secondary); font-weight: 600; text-align: center; }
@media (max-width: 900px) { .index-content { grid-template-columns: 1fr; } .content-side { order: -1; } .header-body { padding: 0 20px 20px; } .stats-bar { padding: 0 20px; } .stat-tab { padding: 14px 16px; } }
</style>
