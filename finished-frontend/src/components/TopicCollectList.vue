<script setup>
import {get, post} from "@/net/api.js";
import {ref} from "vue";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import {ElMessage} from "element-plus";
import {Trash2, ExternalLink} from 'lucide-vue-next';

defineProps({
  show: Boolean
})

const emit = defineEmits(['close'])

const list = ref([])
const loading = ref(true)

function init() {
  loading.value = true;
  get("/api/forum/collects", data => {
    list.value = data || []
    loading.value = false;
  })
}

function deleteCollect(index, tid) {
  post(`/api/forum/interact?tid=${tid}&type=collect&state=false`, { tid: tid, type: 'collect', state: false }, () => {
    ElMessage.success({message: "已取消收藏", plain: true})
    list.value.splice(index, 1);
  });
}
</script>

<template>
  <el-drawer :model-value="show" direction="rtl" :size="400" @close="emit('close')" @open="init" :with-header="false">
    <div class="collect-drawer-container">
      <div class="drawer-header">
        <h3 class="drawer-title">我的收藏</h3>
        <span class="drawer-count">{{ list.length }} 篇帖子</span>
      </div>

      <div class="collect-list" v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="暂无收藏内容" />
        
        <div v-for="(item, index) in list" :key="item.id" class="collect-card" @click="emit('close'); router.push(`/home/topic/${item.id}`)">
          <div class="card-left">
            <topic-tag :type="item.type" :tags="item.tags" class="c-tag" />
          </div>
          <div class="card-main">
            <div class="c-title">{{ item.title }}</div>
            <div class="c-meta" v-if="item.time">{{ new Date(item.time).toLocaleDateString() }}</div>
          </div>
          <div class="card-actions">
            <button class="action-btn delete-btn" @click.stop="deleteCollect(index, item.id)" title="移除收藏">
              <Trash2 :size="15" />
            </button>
            <button class="action-btn view-btn" title="查看帖子">
              <ExternalLink :size="15" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.collect-drawer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.drawer-header {
  padding: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.drawer-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  padding: 2px 8px;
  border-radius: 12px;
}

.collect-list {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collect-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    background: var(--el-fill-color-light);
    border-color: var(--el-border-color-lighter);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);

    .card-actions { opacity: 1; }
  }
}

.card-left {
  padding-top: 2px;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.c-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.c-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);

  &:hover {
    color: var(--el-color-primary);
  }

  &.delete-btn:hover {
    color: #ef4444;
    background: #fef2f2;
    html.dark & { background: rgba(239, 68, 68, 0.1); }
  }
}
</style>
