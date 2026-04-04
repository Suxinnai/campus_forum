<script setup>
import LightCard from "@/components/LightCard.vue";
import {
  SquarePen, MoreHorizontal, Clock, ThumbsUp, Star, MessageSquare, Share2,
  FolderOpen, ChevronRight, Flag, CalendarDays, Link, TrendingUp, Tags
} from "lucide-vue-next";
import {computed, reactive, ref, watch} from "vue";
import {get} from "@/net/api.js";
import TopicEditor from "@/components/TopicEditor.vue";
import {useCounterStore} from "@/stores/counter.js";
import axios from "axios";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";

const store = useCounterStore();
const editor = ref();
const calendarValue = ref(new Date());

const topics = reactive({
  list: [],
  type: 0,
  page: 0,
  end: false,
  top: []
})

function updateList() {
  if (topics.end) return
  get(`/api/forum/list-topic?page=${topics.page}&type=${topics.type}`, data => {
    if (data) {
      data.forEach(item => topics.list.push(item))
      topics.page++;
    }
    if (!data || data.length < 5) topics.end = true;
  })
}

get("/api/forum/top-topic", data => topics.top = data)

function onTopicCreate() {
  editor.value = false;
  resetList()
}

function resetList(){
  topics.page = 0
  topics.list = []
  topics.end = false;
  updateList()
}

const collects = ref(false);

watch(() => topics.type, () => {
  resetList()
}, { immediate: true })

</script>

<template>
  <div class="page-container">
    <div class="main-layout">
      
      <!-- 主内容区 (左侧/中间) -->
      <div class="main-column">
        
        <!-- 顶部轮播图横幅 -->
        <light-card class="carousel-card">
          <el-carousel height="240px" :interval="5000" arrow="hover" indicator-position="outside">
            <el-carousel-item>
              <div class="carousel-slide slide-1">
                <div class="slide-overlay">
                  <h2>连接 · 探索 · 成长</h2>
                  <p>寻找更多共鸣，就在青研社</p>
                </div>
              </div>
            </el-carousel-item>
            <el-carousel-item>
              <div class="carousel-slide slide-2">
                <div class="slide-overlay">
                  <h2>校园热点直达</h2>
                  <p>不错过任何一条对你重要的信息</p>
                </div>
              </div>
            </el-carousel-item>
            <el-carousel-item>
              <div class="carousel-slide slide-3">
                <div class="slide-overlay">
                  <h2>记录专属时刻</h2>
                  <p>与全校同学分享你的大学生活</p>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </light-card>

        <div style="display: flex; justify-content: space-between; align-items: center; margin: 10px 0;">
            <div style="font-size: 16px; font-weight: 700; color: var(--el-text-color-primary);">🔥 广场动态</div>
        </div>

        <!-- 帖子列表 (瀑布流 / 卡片堆叠) -->
        <transition name="el-fade-in" mode="out-in">
          <div v-if="topics.list.length" class="masonry-grid" v-infinite-scroll="updateList">
            
            <div v-for="item in topics.list" :key="item.id" class="masonry-item">
              <light-card class="modern-topic-card" @click="router.push('/index/topic-detail/'+item.id)">
                
                <!-- 头部 -->
                <div class="card-header">
                  <el-avatar :size="38" :src="store.getAvatar(item.avatar)" class="user-avatar" />
                  <div class="user-info">
                    <div class="user-name">{{item.username}}</div>
                    <div class="post-time">{{new Date(item.time).toLocaleString()}}</div>
                  </div>
                  <el-icon class="more-options"><MoreHorizontal/></el-icon>
                </div>
                
                <!-- 内容 -->
                <div class="card-body">
                  <div class="title-row">
                    <TopicTag :type="item.type"/>
                    <h3 class="post-title">{{item.title}}</h3>
                  </div>
                  <p class="post-text">{{item.text}}</p>
                  
                  <div class="post-images" v-if="item.images && item.images.length">
                    <el-image v-for="img in item.images.slice(0, 3)" :src="img" fit="cover" class="preview-img"/>
                  </div>
                </div>
                
                <!-- 底部交互 -->
                <div class="card-footer">
                  <div class="post-actions">
                     <div class="action-btn"><el-icon><ThumbsUp/></el-icon> {{item.like || 0}}</div>
                     <div class="action-btn"><el-icon><Star/></el-icon> {{item.collect || 0}}</div>
                     <div class="action-btn"><el-icon><MessageSquare/></el-icon> {{item.commentCount || '评论'}}</div>
                  </div>
                </div>
              </light-card>
            </div>
            
          </div>
          <el-empty v-else description="暂无内容" />
        </transition>
      </div>
      
      <!-- 右侧小组件栏 -->
      <div class="side-column">
        <div class="sticky-side">
          
          <!-- 1. 创建帖子 -->
          <light-card>
            <div class="create-topic faux-input" @click="editor = true">
              <el-icon><SquarePen/></el-icon> 发个贴，分享新鲜事
            </div>
            <el-divider style="margin: 14px 0" />
            <div class="collect-list-button" @click="collects = true">
              <span><el-icon><FolderOpen/></el-icon> 查看我的收藏夹</span>
              <el-icon style="transform: translateY(3px)"><ChevronRight/></el-icon>
            </div>
          </light-card>

          <!-- 2. 热门帖子 (Top5) -->
          <light-card v-if="topics.top && topics.top.length > 0" class="top-topics-card">
            <div class="widget-title">
                <el-icon><TrendingUp/></el-icon> 热门前五
            </div>
            <div style="margin-top: 14px;">
                <div v-for="(item, index) in topics.top.slice(0, 5)" :key="item.id" class="hot-topic-item" @click="router.push(`/index/topic-detail/${item.id}`)">
                    <span class="hot-rank" :class="`rank-${index+1}`">{{index + 1}}</span>
                    <span class="hot-title">{{item.title}}</span>
                </div>
            </div>
          </light-card>

          <!-- 3. 帖子标签 -->
          <light-card class="tags-card">
            <div class="widget-title">
                <el-icon><Tags/></el-icon> 话题标签
            </div>
            <div class="tags-cloud">
               <div :class="`tag-item ${topics.type === item.id ? 'active' : ''}`" v-for="item in store.forum.types" @click="topics.type = item.id;">
                  {{item.name}}
               </div>
            </div>
          </light-card>

          <!-- 4. 校园日历 -->
          <light-card class="calendar-card">
            <div class="elegant-calendar">
                <div class="cal-top">
                    <div class="cal-month">{{ new Date().toLocaleString('en-US', { month: 'long', year: 'numeric' }) }}</div>
                </div>
                <div class="cal-center">
                    <div class="cal-date">{{ new Date().getDate() }}</div>
                    <div class="cal-day">{{ new Date().toLocaleString('zh-CN', { weekday: 'long' }) }}</div>
                </div>
            </div>
          </light-card>

          <!-- 5. 每日一言 -->
          <light-card class="quote-card">
            <div class="quote-header">❝ 每日一言</div>
            <div class="quote-text-en">Life is never easier, we just get stronger.</div>
            <div class="quote-text-cn">生活从未变得容易，只是我们变得更加坚强</div>
          </light-card>

        </div>
      </div>
      
    </div>
    <TopicEditor :show="editor" @success="onTopicCreate" @close="editor = false"/>
    <TopicCollectList :show="collects" @close="collects = false"/>
  </div>
</template>

<style lang="less" scoped>
.page-container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
}

.main-layout {
  display: flex;
  gap: 24px;
}

.main-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.side-column {
  width: 330px;
  flex-shrink: 0;
}

.sticky-side {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ================= 轮播图设计 ================= */
.carousel-card {
  padding: 0 !important;
}

.carousel-slide {
  height: 100%;
  width: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
  
  &.slide-1 { background-image: url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?q=80&w=1200&auto=format&fit=crop'); }
  &.slide-2 { background-image: url('https://images.unsplash.com/photo-1523050854058-8df90110c9f1?q=80&w=1200&auto=format&fit=crop'); }
  &.slide-3 { background-image: url('https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=1200&auto=format&fit=crop'); }

  .slide-overlay {
    position: absolute;
    bottom: 24px;
    left: 24px;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-radius: 16px;
    padding: 20px 24px;
    color: var(--el-text-color-primary);
    box-shadow: 0 8px 32px rgba(31, 38, 135, 0.08);
    max-width: 60%;

    h2 { margin: 0 0 6px 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px; }
    p { margin: 0; font-size: 14px; color: var(--el-text-color-regular); font-weight: 500;}
  }
}

html.dark .slide-overlay {
    background: rgba(15, 23, 42, 0.7) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    color: #fff !important;
    p { color: #cbd5e1 !important; }
}

:deep(.el-carousel__indicators--outside) {
  display: none;
}

/* ================= 瀑布流列表 ================= */
.masonry-grid {
  column-count: 2;
  column-gap: 20px;
  padding-bottom: 20px;
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 20px;
}

/* 现代卡片设计 (对应圆角及内边距) */
.modern-topic-card {
  padding: 24px;
  cursor: pointer;
  background-color: var(--el-bg-color);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 30px rgba(0,0,0,0.06);
  }
  
  .card-header {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    
    .user-avatar { border: 1px solid var(--el-border-color-lighter); }
    
    .user-info {
      margin-left: 12px;
      flex: 1;
      .user-name { font-size: 14px; font-weight: 700; color: var(--el-text-color-primary); }
      .post-time { font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 2px;}
    }
    
    .more-options { color: var(--el-text-color-placeholder); font-size: 18px; cursor: pointer; &:hover{ color: var(--el-color-primary); } }
  }
  
  .card-body {
    .title-row {
      display: flex;
      align-items: flex-start;
      margin-bottom: 8px;
      gap: 8px;
    }

    .post-title {
      font-size: 16px;
      font-weight: 800;
      line-height: 1.4;
      margin: 0;
      color: var(--el-text-color-primary);
    }
    
    .post-text {
      font-size: 14px;
      color: var(--el-text-color-regular);
      line-height: 1.6;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 3;
      line-clamp: 3;
      overflow: hidden;
      margin: 0 0 16px 0;
    }
    
    .post-images {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
      
      .preview-img {
        height: 110px;
        flex: 1;
        border-radius: 8px;
      }
    }
  }
  
  .card-footer {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    border-top: 1px dashed var(--el-border-color-lighter);
    padding-top: 12px;
    
    .post-actions {
      display: flex;
      gap: 20px;
      
      .action-btn {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: var(--el-text-color-secondary);
        font-weight: 500;
        transition: color 0.2s;
        
        &:hover { color: var(--el-color-primary); }
      }
    }
  }
}

/* ================= 右侧小组件 ================= */
.widget-title {
  font-weight: 800;
  font-size: 15px;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  
  .el-icon {
    color: var(--el-color-primary);
    font-size: 18px;
  }
}

/* 1. 发布帖子按钮 */
.create-topic.faux-input {
  background: linear-gradient(135deg, var(--el-color-primary-light-9), #fff);
  border-radius: 12px;
  height: 48px;
  color: var(--el-text-color-primary);
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid var(--el-color-primary-light-7);
  box-shadow: 0 4px 12px var(--el-color-primary-light-9);

  &:hover {
    cursor: pointer;
    background: var(--el-color-primary);
    color: white;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px var(--el-color-primary-light-5);
  }
  
  .el-icon { margin-right: 8px; font-size: 18px; }
}

.collect-list-button {
  font-size: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all .3s;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 600;
  color: var(--el-text-color-regular);

  &:hover {
    cursor: pointer;
    background-color: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }
  .el-icon { font-size: 16px; margin-right: 4px; }
}

/* 2. 热门前五 */
.hot-topic-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 8px;
    border-radius: 8px;
    transition: all 0.2s;
    cursor: pointer;

    &:hover {
        background-color: var(--el-fill-color-light);
        
        .hot-title { color: var(--el-color-primary); }
    }

    .hot-rank {
        width: 20px;
        height: 20px;
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: 6px;
        font-size: 12px;
        font-weight: 800;
        background-color: var(--el-fill-color-dark);
        color: white;
        flex-shrink: 0;

        &.rank-1 { background-color: #ef4444; }
        &.rank-2 { background-color: #f97316; }
        &.rank-3 { background-color: #f59e0b; }
    }

    .hot-title {
        font-size: 14px;
        font-weight: 600;
        color: var(--el-text-color-primary);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: color 0.2s;
    }
}

/* 3. 话题标签 */
.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;

  .tag-item {
    padding: 6px 16px;
    background-color: var(--el-fill-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);

    &:hover {
        background-color: var(--el-fill-color-darker);
        color: var(--el-text-color-primary);
        transform: translateY(-1px);
    }

    &.active {
        background-color: var(--el-color-primary);
        color: white;
        border-color: var(--el-color-primary);
        box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);
    }
  }
}

/* 4. 极简日历卡片 */
.elegant-calendar {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .cal-top {
    width: 100%;
    text-align: center;
    padding-bottom: 12px;
    border-bottom: 1px dashed var(--el-border-color-lighter);
    
    .cal-month {
      font-size: 14px;
      font-weight: 800;
      color: var(--el-color-primary);
      text-transform: uppercase;
      letter-spacing: 1px;
    }
  }
  
  .cal-center {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 16px;
    
    .cal-date {
      font-size: 48px;
      font-weight: 900;
      color: var(--el-text-color-primary);
      line-height: 1;
      text-shadow: 2px 2px 0px rgba(0,0,0,0.02);
    }
    
    .cal-day {
      margin-top: 8px;
      font-size: 14px;
      font-weight: 600;
      color: var(--el-text-color-secondary);
      background-color: var(--el-fill-color-light);
      padding: 4px 16px;
      border-radius: 12px;
    }
  }
}

/* 5. 每日一言 */
.quote-card {
  background: linear-gradient(135deg, var(--el-color-primary-light-8), var(--el-color-primary-light-9));
  border: 1px solid white;
  
  .quote-header {
    color: var(--el-color-primary-dark-2);
    font-weight: 800;
    font-size: 14px;
    margin-bottom: 12px;
  }
  
  .quote-text-en { font-size: 15px; font-weight: 700; font-style: italic; color: var(--el-text-color-primary); margin-bottom: 8px; line-height: 1.4; }
  .quote-text-cn { font-size: 13px; color: var(--el-text-color-regular); }
}

@media (max-width: 900px) {
  .main-layout { flex-direction: column; }
  .side-column { width: 100%; }
  .masonry-grid { column-count: 1; }
}
</style>