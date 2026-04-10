<script setup>
import { get, post } from "@/net/api.js";
import { onMounted, ref, computed } from "vue";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Trash2, Bookmark, MessageSquare, Heart, Home, TrendingUp } from 'lucide-vue-next';

const list = ref([])
const loading = ref(true)
const activeFilter = ref('all')

const filters = [
  { key: 'all', label: '全部' },
  { key: 'post', label: '帖子' },
  { key: 'resource', label: '资源' },
]

const filteredList = computed(() => list.value)

function fetchCollects() {
  loading.value = true;
  get("/api/forum/collects", data => {
    list.value = data || []
    loading.value = false;
  })
}

function deleteCollect(tid) {
  ElMessageBox.confirm('确定要取消收藏这篇文章吗？', '提示', { type: 'warning' })
    .then(() => {
      post(`/api/forum/interact?tid=${tid}&type=collect&state=false`, null, () => {
        ElMessage.success("已取消收藏");
        fetchCollects();
      });
    }).catch(() => {})
}

onMounted(fetchCollects)
</script>

<template>
  <div class="collect-page">
    <!-- Page header -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的收藏</h2>
        <span class="count-badge">{{ list.length }} 篇</span>
      </div>
      <div class="filter-tabs">
        <button
          v-for="f in filters"
          :key="f.key"
          class="filter-btn"
          :class="{ active: activeFilter === f.key }"
          @click="activeFilter = f.key"
        >{{ f.label }}</button>
      </div>
    </div>

    <!-- List -->
    <div v-loading="loading" class="collect-list">
      <!-- Empty state -->
      <div v-if="!loading && list.length === 0" class="empty-state">
        <div class="empty-icon">
          <Bookmark :size="36" />
        </div>
        <h3 class="empty-title">还没有收藏内容</h3>
        <p class="empty-desc">去首页看看热门讨论，或者先收藏一篇你感兴趣的帖子</p>
        <div class="empty-actions">
          <button class="cta-btn primary" @click="router.push('/home')">
            <Home :size="15" /> 去逛首页
          </button>
          <button class="cta-btn outline" @click="router.push('/home')">
            <TrendingUp :size="15" /> 看看热门
          </button>
        </div>
      </div>

      <!-- Cards -->
      <div v-else class="card-list">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="collect-card"
          @click="router.push(`/home/topic/${item.id}`)"
        >
          <div class="card-main">
            <div class="card-top">
              <TopicTag :type="item.type" />
              <span class="card-date">收藏于 {{ new Date(item.time).toLocaleDateString() }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-excerpt" v-if="item.text">{{ item.text }}</p>
            <div class="card-footer">
              <div class="card-stats">
                <span class="stat"><MessageSquare :size="13" /> {{ item.commentCount || 0 }}</span>
                <span class="stat"><Heart :size="13" /> {{ item.like || 0 }}</span>
              </div>
              <button
                class="del-btn"
                @click.stop="deleteCollect(item.id)"
                title="取消收藏"
              >
                <Trash2 :size="15" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.collect-page {
  max-width: 860px;
  margin: 32px auto;
  padding: 0 4px;
}

/* ── Header ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.filter-tabs {
  display: flex;
  gap: 4px;
  background: var(--el-fill-color-light);
  padding: 4px;
  border-radius: 12px;
}

.filter-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 8px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: all 0.2s;

  &:hover { color: var(--el-text-color-primary); }
  &.active {
    background: var(--el-bg-color);
    color: var(--el-color-primary);
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  }
}

/* ── Empty State ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 72px; height: 72px;
  border-radius: 20px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 28px;
  max-width: 380px;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 12px;
}

.cta-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;

  &.primary {
    background: var(--el-color-primary);
    color: #fff;
    &:hover { background: var(--el-color-primary-dark-2); transform: translateY(-1px); }
  }

  &.outline {
    background: transparent;
    border: 1.5px solid var(--el-border-color);
    color: var(--el-text-color-regular);
    &:hover { border-color: var(--el-color-primary-light-5); color: var(--el-color-primary); }
  }
}

/* ── Card List ── */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collect-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 22px 24px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
  box-shadow: 0 1px 4px rgba(0,0,0,.03);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0,0,0,.08);
    border-color: var(--el-color-primary-light-7);
  }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-date {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
  line-height: 1.4;
}

.card-excerpt {
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
  justify-content: space-between;
  align-items: center;
}

.card-stats {
  display: flex;
  gap: 14px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.del-btn {
  width: 32px; height: 32px;
  border-radius: 8px;
  border: none;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #fff0f0;
    color: #e11d48;
  }
}
</style>
