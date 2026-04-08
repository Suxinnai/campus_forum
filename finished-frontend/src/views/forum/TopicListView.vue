<script setup>
import {
  SquarePen, ThumbsUp, MessageSquare, Share2,
  TrendingUp, Flame, CalendarDays, Megaphone,
  Bookmark, Trash2, Quote, MoreHorizontal, RefreshCcw
} from "lucide-vue-next";
import { computed, reactive, ref, watch, onMounted } from "vue";
import { get, post, del } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import { ElMessage, ElMessageBox } from "element-plus";

const store = useAppStore();

function deletePost(id) {
  ElMessageBox.confirm('确定要删除这条帖子吗？此操作不可恢复。', '删除确认', {
    confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    del(`/api/forum/delete-topic?id=${id}`, () => {
      topics.list = topics.list.filter(t => t.id !== id);
      ElMessage.success('帖子已删除');
    });
  }).catch(() => {});
}

function collectPost(tid) {
  post(`/api/forum/interact?tid=${tid}&type=collect&state=true`, null, () => {
    ElMessage.success({ message: '收藏成功', plain: true });
  });
}

function sharePost(id) {
  const url = `${window.location.origin}/home/topic/${id}`;
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success({ message: '链接已复制到剪贴板', plain: true });
  });
}

// 快速发帖路由联动
function handleQuickPublish() {
  const btn = document.querySelector('.publish-btn');
  if (btn) btn.click();
}

const yiyanList = [
  { zh: "人生如逆旅，我亦是行人。", en: "Life is a journey, and I am a traveler too." },
  { zh: "学而不思则罔，思而不学则殆。", en: "Learning without thinking is useless." },
  { zh: "博学之，审问之，慎思之，明辨之，笃行之。", en: "Study extensively, inquire carefully, think deeply." },
  { zh: "千里之行，始于足下。", en: "A journey of a thousand miles begins with a single step." },
  { zh: "知之者不如好之者，好之者不如乐之者。", en: "Knowing is not as good as loving, loving is not as good as enjoying." },
];
const yiyan = ref(yiyanList[0]);
const fetchYiyan = () => {
  yiyan.value = yiyanList[Math.floor(Math.random() * yiyanList.length)];
}

onMounted(() => {
  fetchYiyan();
});

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

function resetList() {
  topics.page = 0
  topics.list = []
  topics.end = false;
  updateList()
}


watch(() => topics.type, () => {
  resetList()
}, { immediate: true })

// Relative time helper
function relativeTime(dateStr) {
  const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
  if (diff < 60) return '刚刚';
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`;
  return `${Math.floor(diff / 86400)}天前`;
}

// Calendar
const today = new Date();
const calYear = ref(today.getFullYear());
const calMonth = ref(today.getMonth());

const calDays = computed(() => {
  const firstDay = new Date(calYear.value, calMonth.value, 1).getDay();
  const daysInMonth = new Date(calYear.value, calMonth.value + 1, 0).getDate();
  const cells = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);
  return cells;
});

const calMonthNum = computed(() => calMonth.value + 1);

const isToday = (d) =>
  d === today.getDate() &&
  calYear.value === today.getFullYear() &&
  calMonth.value === today.getMonth();
</script>

<template>
  <div class="page-container">
    <div class="main-layout">
      
      <!-- 主内容区 -->
      <div class="main-column">

        <!-- Hero Banner 轮播 -->
        <div class="hero-wrap">
          <el-carousel :interval="5000" arrow="hover" height="160px">
            <el-carousel-item v-for="(img, idx) in ['/banner1.jpg', '/banner2.jpg', '/banner3.jpg']" :key="idx">
              <div class="hero-slide slide-image" :style="`background-image: url('${img}'); background-size: cover; background-position: center;`">
                <div class="hero-overlay"></div>
                <div class="hero-content">
                  <h1>连接校园，点亮青春</h1>
                  <p>分享知识，发现活动，连接彼此。</p>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <!-- 快速发帖器 -->
        <div class="quick-publish-bar" @click="handleQuickPublish">
          <el-avatar :size="36" :src="store.getAvatar(store.user.avatar)" />
          <div class="qp-input">有什么新鲜事想和大家分享？</div>
          <button class="qp-btn"><SquarePen :size="16" /> 发布</button>
        </div>

        <!-- Tab 筛选栏 -->
        <div class="topic-tabs">
          <div class="tab-item" :class="{ active: topics.type === 0 }" @click="topics.type = 0">全部</div>
          <div class="tab-item" :class="{ active: topics.type === -1 }" @click="topics.type = -1">🔥 热门</div>
          <div class="tab-item" :class="{ active: topics.type === -2 }" @click="topics.type = -2">🕐 最新</div>
        </div>

        <!-- 帖子列表 -->
        <transition name="el-fade-in" mode="out-in">
          <div v-if="topics.list.length" v-infinite-scroll="updateList" class="post-list">
            <article
              v-for="item in topics.list"
              :key="item.id"
              class="post-card"
              @click="router.push('/home/topic/' + item.id)"
            >
              <!-- 瀑布流缩略图 -->
              <div class="waterfall-img-wrap" v-if="item.images && item.images.length">
                <el-image
                  :src="item.images[0]"
                  fit="cover"
                  class="waterfall-img"
                  lazy
                />
              </div>

              <div class="post-body">
                <div class="post-content" :style="{ borderLeft: `3px solid ${store.findTypeById(item.type)?.color || 'transparent'}`, paddingLeft: '12px' }">
                    <div class="post-meta-top">
                      <!-- 动态话题标签 (主+副) -->
                      <div class="card-tags-area" v-if="item.tags && item.tags.length">
                        <span class="main-card-tag">{{ item.tags[0] }}</span>
                        <span v-for="tag in item.tags.slice(1)" :key="tag" class="sub-card-tag">#{{ tag }}</span>
                      </div>
                      <span v-else class="main-card-tag" style="background: #f1f5f9; color: #64748b; opacity: 0.6">未分类</span>
                      
                      <span class="post-time">{{ relativeTime(item.time) }}</span>
                    </div>
                    <h3 class="post-title">{{ item.title }}</h3>
                    <p class="post-excerpt">{{ item.text }}</p>
                  </div>
              </div>

              <div class="post-footer">
                <div class="footer-left">
                  <el-avatar :size="20" :src="store.getAvatar(item.avatar)" class="compact-avatar" />
                  <span class="meta-name">{{ item.username }}</span>
                </div>
                <div class="footer-actions">
                  <div class="action-item" title="点赞"><ThumbsUp :size="12" /> {{ item.like || 0 }}</div>
                  <div class="action-item" title="评论"><MessageSquare :size="12" /> {{ item.comments || 0 }}</div>
                  <el-dropdown trigger="click" placement="bottom-end">
                    <div class="action-more" @click.stop><MoreHorizontal :size="14" /></div>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click.stop="collectPost(item.id)">
                          <Bookmark :size="14" style="margin-right:8px" /> 收藏
                        </el-dropdown-item>
                        <el-dropdown-item @click.stop="sharePost(item.id)">
                          <Share2 :size="14" style="margin-right:8px" /> 分享
                        </el-dropdown-item>
                        <el-dropdown-item v-if="store.user.id === item.uid || store.user.role === 'admin'"
                                          divided style="color:var(--el-color-danger)" @click.stop="deletePost(item.id)">
                          <Trash2 :size="14" style="margin-right:8px" /> 删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </article>

            <div v-if="topics.end" class="end-hint">— 已经到底了 —</div>
          </div>
          <el-empty v-else description="暂无内容" style="margin-top: 60px;" />
        </transition>
      </div>

      <!-- 右侧边栏 -->
      <aside class="side-column">
        <div class="sticky-side">

          <!-- 热门话题榜 -->
          <div class="widget-card" v-if="topics.top && topics.top.length">
            <div class="widget-header">
              <TrendingUp :size="16" class="widget-icon" />
              热门话题榜
            </div>
            <ul class="hot-list">
              <li
                v-for="(item, index) in topics.top.slice(0, 5)"
                :key="item.id"
                class="hot-item"
                @click="router.push(`/home/topic/${item.id}`)"
              >
                <span class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
                <span class="hot-title">{{ item.title }}</span>
                <Flame :size="13" class="hot-flame" v-if="index < 3" />
              </li>
            </ul>
          </div>

          <!-- 校园日历 -->
          <div class="widget-card">
            <div class="widget-header">
              <CalendarDays :size="16" class="widget-icon" />
              校园日程
              <span class="month-badge">{{ calMonthNum }}月</span>
            </div>
            <div class="cal-weekdays">
              <span v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</span>
            </div>
            <div class="cal-grid">
              <span
                v-for="(d, i) in calDays"
                :key="i"
                class="cal-cell"
                :class="{ today: d && isToday(d), empty: !d }"
              >{{ d || '' }}</span>
            </div>
            <div class="today-schedule">
              <div class="schedule-title">
                <span>今日安排</span>
                <i>{{ calMonthNum }}月{{ today.getDate() }}日</i>
              </div>
              <ul class="schedule-list">
                <li class="schedule-item">
                  <div class="s-time">14:00</div>
                  <div class="s-event">计算机学院学术讲座</div>
                </li>
                <li class="schedule-item">
                  <div class="s-time">19:30</div>
                  <div class="s-event">青研社技术分享会</div>
                </li>
              </ul>
            </div>
          </div>

          <!-- 网站公告 -->
          <div class="widget-card bulletin-card">
            <div class="widget-header">
              <Megaphone :size="16" class="widget-icon" />
              网站公告
            </div>
            <div class="bulletin-list">
              <div class="bulletin-item">
                <p class="b-title" style="margin-bottom:0">欢迎使用青研社校园论坛</p>
              </div>
              <div class="bulletin-item">
                <p class="b-title" style="margin-bottom:0">如有问题请联系管理员</p>
              </div>
            </div>
          </div>

          <!-- 每日一言 (重构无边框图片版) -->
          <div class="yiyan-card">
            <div class="yiyan-header">
              <span class="y-title"><Quote :size="16" /> 每日一言</span>
              <RefreshCcw :size="14" class="y-refresh" @click="fetchYiyan" />
            </div>
            <div class="yiyan-content">
              <p class="yiyan-en">{{ yiyan.en || 'If you hold tight, how can a free hand to hug now?' }}</p>
              <div class="yiyan-divider"></div>
              <p class="yiyan-zh">{{ yiyan.zh || '你若将过去抱的太紧，怎么能腾出手来拥抱现在？' }}</p>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style lang="less" scoped>
/* ===== Layout ===== */
.page-container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 20px 40px;
}

.main-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.main-column {
  flex: 1;
  min-width: 0;
}

.side-column {
  width: 300px;
  flex-shrink: 0;
}

.sticky-side {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== Hero Banner ===== */
.hero-wrap {
  margin-bottom: 24px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.hero-slide {
  width: 100%;
  height: 280px;
  position: relative;
  display: flex;
  align-items: center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(15, 23, 42, 0.85) 0%, rgba(15, 23, 42, 0.4) 100%);
  z-index: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 0 48px;

  h1 {
    margin: 0 0 12px;
    font-size: 32px;
    font-weight: 900;
    color: #fff;
    letter-spacing: 0.5px;
    text-shadow: 0 2px 8px rgba(0,0,0,0.3);
  }

  p {
    margin: 0;
    font-size: 15px;
    color: rgba(255,255,255,0.9);
    max-width: 500px;
    line-height: 1.6;
    text-shadow: 0 1px 4px rgba(0,0,0,0.3);
  }
}

.quick-publish-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 24px;
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--ds-primary-light);
    background: var(--el-bg-color-page);
  }

  .qp-input {
    flex: 1;
    font-size: 14px;
    color: var(--el-text-color-placeholder);
    background: var(--el-fill-color-lighter);
    padding: 10px 16px;
    border-radius: 20px;
    transition: all 0.2s;
  }

  &:hover .qp-input {
    background: var(--el-fill-color-light);
  }

  .qp-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 700;
    color: var(--el-color-white);
    background: var(--ds-primary);
    border: none;
    border-radius: 20px;
    padding: 8px 18px;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2);

    &:hover {
      background: var(--ds-primary-dark);
      transform: translateY(-1px);
    }
  }
}

/* ===== Tab 栏 ===== */
.topic-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 20px;
  overflow-x: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.tab-item {
  padding: 12px 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 5px;

  &:hover { color: var(--el-color-primary); }

  &.active {
    color: var(--el-color-primary);
    border-bottom-color: var(--el-color-primary);
    font-weight: 800;
  }

  &.type-tab {
    font-size: 13px;
    .type-dot {
      width: 6px; height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }
  }
}

.tab-divider {
  width: 1px;
  height: 20px;
  background: var(--el-border-color-lighter);
  align-self: center;
  margin: 0 4px;
}

/* ===== 帖子卡片 meta ===== */
.post-meta-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-tags-area {
  display: flex;
  align-items: center;
  gap: 6px;
}

.main-card-tag {
  font-size: 11px;
  font-weight: 900;
  color: #f43f5e;
  background: #fff1f2;
  padding: 3px 10px;
  border-radius: 100px;
  border: 1px solid #ffe4e6;
}

.sub-card-tag {
  font-size: 10px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
  background: #f8fafc;
  padding: 2px 7px;
  border-radius: 6px;
  opacity: 0.8;
}

.post-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-weight: 500;
}

/* ===== 帖子列表 (瀑布流布局) ===== */
.post-list {
  columns: 2;
  column-gap: 20px;
}

.post-card {
  break-inside: avoid;
  margin-bottom: 20px;
  background: var(--el-bg-color);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--el-border-color-lighter);
  transition: border-color 0.2s, background-color 0.2s, transform 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    background: var(--el-bg-color);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.06);
    .post-title { color: var(--el-color-primary); }
  }
}

.waterfall-img-wrap {
  width: 100%;
}

.waterfall-img {
  width: 100%;
  display: block;
  max-height: 280px;
  object-fit: cover;
}

.post-body {
  padding: 12px 16px 8px;
}

.post-content {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0 0 6px;
  line-height: 1.5;
  transition: color 0.2s;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.post-excerpt {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 14px;
  border: none;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  
  .meta-name {
    font-size: 12px;
    color: var(--el-text-color-regular);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);

  .action-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    font-weight: 600;
  }

  .action-more {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    cursor: pointer;
    transition: background 0.2s;
    
    &:hover { background: var(--el-fill-color-light); }
  }
}

.end-hint {
  text-align: center;
  padding: 24px 0 8px;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

/* ===== Widget 通用 ===== */
.widget-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.03);
  margin-bottom: 24px;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;

  .widget-icon { color: var(--el-color-primary); flex-shrink: 0; }
}

.month-badge {
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 2px 10px;
  border-radius: 10px;
}

/* ===== 热门榜 ===== */
.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
    .hot-title { color: var(--el-color-primary); }
  }
}

.hot-rank {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 800;
  flex-shrink: 0;
  background: var(--el-fill-color-dark);
  color: #fff;

  &.rank-1 { background: #ef4444; }
  &.rank-2 { background: #f97316; }
  &.rank-3 { background: #f59e0b; }
}

.hot-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}

.hot-flame {
  color: #f97316;
  flex-shrink: 0;
}

/* ===== 日历 ===== */
.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 6px;

  span {
    font-size: 11px;
    font-weight: 700;
    color: var(--el-text-color-placeholder);
    padding: 4px 0;
  }
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  text-align: center;
}

.cal-cell {
  padding: 6px 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover:not(.empty):not(.today) { background: var(--el-fill-color-light); }
  &.empty { cursor: default; }

  &.today {
    background: #f59e0b;
    color: #fff;
    font-weight: 800;
    box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
  }
}

.today-schedule {
  margin-top: 14px;
  border-top: 1px dashed var(--el-border-color-lighter);
  padding-top: 12px;

  .schedule-title {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 8px;
    span { font-size: 13px; font-weight: 700; color: var(--el-color-primary); }
    i { font-size: 11px; color: var(--ds-muted); font-style: normal; }
  }

  .schedule-list {
    list-style: none; padding: 0; margin: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .schedule-item {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: 6px 0;
  }
  .s-time {
    font-size: 14px;
    font-weight: 700;
    color: #f5a623;
    width: 44px;
    padding-top: 0;
  }
  .s-event {
    flex: 1;
    font-size: 12px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    line-height: 1.5;
    background: var(--el-fill-color-lighter);
    padding: 6px 10px;
    border-radius: 8px;
  }
}

/* ===== 公告 ===== */

.bulletin-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.bulletin-item {
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
    border-color: var(--el-border-color-lighter);
  }

  &.accent {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-7);
    .b-title { color: var(--el-color-primary); }
    &:hover { background: var(--el-color-primary-light-8); }
  }
}

.b-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 3px;
}

.b-sub {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin: 0;
}

.bulletin-more {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 700;
  color: var(--el-color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-left: auto;
  width: 100%;
  justify-content: flex-end;
  transition: opacity 0.15s;

  &:hover { opacity: 0.7; }
}

/* ===== 每日一言 (现代无干扰版) ===== */
.yiyan-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.yiyan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--el-color-primary);
  margin-bottom: 16px;
  
  .y-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 800;
  }
  .y-refresh {
    cursor: pointer;
    transition: transform 0.3s;
    color: var(--el-text-color-secondary);
    &:hover { transform: rotate(180deg); color: var(--el-color-primary); }
  }
}

.yiyan-content {
  border-left: 3px solid var(--el-color-primary-light-3);
  padding-left: 14px;

  .yiyan-en {
    font-size: 16px;
    font-style: italic;
    color: var(--ds-primary);
    line-height: 1.5;
    margin: 0;
  }
  .yiyan-divider {
    height: 1px;
    background: var(--el-border-color-lighter);
    margin: 12px 0;
  }
  .yiyan-zh {
    font-size: 14px;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    margin: 0;
  }
}

/* ===== IP定位 (极简现代版) ===== */
.ip-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.ip-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 24px;
}

.ip-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.loc-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-primary);
}

.loc-text-box {
  display: flex;
  flex-direction: column;
}

.loc-city {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.loc-ip {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

/* ===== 响应式 ===== */
@media (max-width: 960px) {
  .main-layout { flex-direction: column; }
  .side-column { width: 100%; display: none; }
  .sticky-side { position: static; }
}

@media (max-width: 768px) {
  .hero-wrap { height: 120px; margin-bottom: 16px; border-radius: 12px; }
  :deep(.el-carousel) { height: 120px !important; }
  .post-card { padding: 16px; }
  .post-thumb { width: 80px; height: 60px; }
  .topic-tabs { gap: 16px; }
  .side-column { display: none; }
}

@media (max-width: 600px) {
  .page-container { padding: 0 12px 40px; }
  .hero-overlay { padding: 20px; h1 { font-size: 20px; } }
}
</style>
