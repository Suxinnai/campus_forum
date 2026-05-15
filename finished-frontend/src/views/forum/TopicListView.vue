<script setup>
import {
  SquarePen, ThumbsUp, MessageSquare, Share2,
  TrendingUp, Flame, CalendarDays, Megaphone,
  Bookmark, Trash2, Quote, MoreHorizontal, RefreshCcw,
  ChevronDown, ChevronUp, Pin, Star, Eye
} from "lucide-vue-next";
import { computed, reactive, ref, watch, onMounted, onActivated } from "vue";
import { get, post, del } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import { ElMessage, ElMessageBox } from "element-plus";

const store = useAppStore();
const showAllHot = ref(false);

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

function defaultVisualCapacity() {
  return typeof window !== 'undefined' && window.innerWidth <= 768 ? 6 : 10
}

const topics = reactive({
  list: [],
  type: 0,
  page: 0,
  visualCapacity: defaultVisualCapacity(),
  total: 0,
  pages: 0,
  end: false,
  top: [],
  loading: false
})

const topicColumns = computed(() => {
  const total = topics.list.length
  const even = total % 2 === 0
  return [
    topics.list.filter((_, i) => even ? i % 2 === 0 : i < total - 1 && i % 2 === 0),
    topics.list.filter((_, i) => even ? i % 2 === 1 : i % 2 === 1 || i === total - 1)
  ]
})

function updateList() {
  if (topics.loading || topics.end) return
  topics.loading = true
  if (topics.type === -3) {
    // 推荐接口一次性返回，不分页
    get('/api/recommend/topics', data => {
      topics.list = data || []
      topics.total = topics.list.length
      topics.pages = topics.list.length ? 1 : 0
      topics.end = true
      topics.loading = false
    }, () => {
      topics.list = []
      topics.total = 0
      topics.pages = 0
      topics.end = true
      topics.loading = false
    })
    return
  }
  get(`/api/forum/page-topic?page=${topics.page}&type=${topics.type}&size=${topics.visualCapacity}`, data => {
    const records = data?.records || []
    const existingIds = new Set(topics.list.map(item => item.id))
    topics.list.push(...records.filter(item => !existingIds.has(item.id)))
    topics.total = data?.total || 0
    topics.pages = data?.pages || 0
    topics.end = !records.length || topics.pages === 0 || topics.page >= topics.pages - 1
    if (!topics.end) topics.page += 1
    topics.loading = false
  }, () => {
    topics.end = true
    topics.loading = false
  })
}

get("/api/forum/top-topic", data => topics.top = data)

function loadTopTopics() {
  get("/api/forum/top-topic", data => topics.top = data)
}

function resetList() {
  topics.page = 0
  topics.list = []
  topics.end = false;
  topics.visualCapacity = defaultVisualCapacity()
  updateList()
}

function hotMetric(item) {
  const value = Number(item.hotScore || 0)
  return value >= 10 ? Math.round(value) : value.toFixed(1)
}


watch(() => topics.type, () => {
  resetList()
}, { immediate: true })

onActivated(() => {
  if (topics.list.length > 0) {
    resetList()
    loadTopTopics()
  }
})

// Relative time helper
function relativeTime(dateStr) {
  const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
  if (diff < 60) return '刚刚';
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`;
  return `${Math.floor(diff / 86400)}天前`;
}

function stripMarkdown(text) {
  if (!text) return '';
  return text
    .replace(/!\[.*?\]\([^)]+\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/(\*\*|__)(.*?)\1/g, '$2')
    .replace(/#{1,6}\s?/g, '')
    .replace(/\s+/g, ' ')
    .replace(/http[s]?:\/\/[^\s]+/g, '')
    .replace(/localhost:\d+\/\S+/g, '')
    .trim();
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

const pad2 = value => String(value).padStart(2, '0')
const todayKey = dateKey(today)
const selectedDateKey = ref(todayKey);

function dateKey(value) {
  if (!value) return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) return value.slice(0, 10)
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function monthDayLabel(key) {
  if (!key) return ''
  const [, month, day] = key.split('-')
  return `${Number(month)}/${Number(day)}`
}

function scheduleRangeLabel(item) {
  const start = dateKey(item.eventDate)
  const end = dateKey(item.endDate || item.eventDate)
  return start === end ? monthDayLabel(start) : `${monthDayLabel(start)}-${monthDayLabel(end)}`
}

function compareDateKey(a, b) {
  return a.localeCompare(b)
}

// 公告数据
const notices = ref([])
get('/api/notice/list', data => { notices.value = (data || []).slice(0, 5) })

// 活动/校历数据
const activities = ref([])
const schedules = ref([])
get('/api/activity/list', data => {
  const now = new Date()
  activities.value = (data || [])
    .filter(a => new Date(a.endTime) >= now)
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
})
get('/api/schedule/list', data => {
  schedules.value = data || []
})

const calendarMarks = computed(() => {
  const marks = new Set()
  const monthPrefix = `${calYear.value}-${pad2(calMonth.value + 1)}-`
  activities.value.forEach(item => {
    const key = dateKey(item.startTime)
    if (key.startsWith(monthPrefix)) marks.add(key)
  })
  schedules.value.forEach(item => {
    const start = dateKey(item.eventDate)
    const end = dateKey(item.endDate || item.eventDate)
    if (!start) return
    for (let day = 1; day <= 31; day++) {
      const key = `${monthPrefix}${pad2(day)}`
      if (compareDateKey(key, start) >= 0 && compareDateKey(key, end) <= 0) marks.add(key)
    }
  })
  return marks
})

const selectedDateLabel = computed(() => {
  const [, month, day] = selectedDateKey.value.split('-')
  return `${Number(month)}月${Number(day)}日`
})

const selectedCalendarItems = computed(() => {
  const selected = selectedDateKey.value
  const scheduleItems = schedules.value
    .filter(item => {
      const start = dateKey(item.eventDate)
      const end = dateKey(item.endDate || item.eventDate)
      return start && compareDateKey(start, selected) <= 0 && compareDateKey(end, selected) >= 0
    })
    .map(item => ({
      id: `schedule-${item.id}`,
      title: item.title,
      kind: '校历',
      when: dateKey(item.eventDate) === dateKey(item.endDate || item.eventDate) ? '全天' : scheduleRangeLabel(item),
      date: dateKey(item.eventDate),
      route: null
    }))
  const activityItems = activities.value
    .filter(item => compareDateKey(dateKey(item.startTime), selected) <= 0 && compareDateKey(dateKey(item.endTime || item.startTime), selected) >= 0)
    .map(item => ({
      id: `activity-${item.id}`,
      title: item.title,
      kind: '活动',
      when: formatActivityTime(item.startTime),
      date: dateKey(item.startTime),
      route: '/home/activity'
    }))
  return [...scheduleItems, ...activityItems]
    .sort((a, b) => compareDateKey(a.date, b.date))
    .slice(0, 3)
})

function hasCalendarItem(day) {
  if (!day) return false
  return calendarMarks.value.has(`${calYear.value}-${pad2(calMonth.value + 1)}-${pad2(day)}`)
}

function calendarCellKey(day) {
  return `${calYear.value}-${pad2(calMonth.value + 1)}-${pad2(day)}`
}

function selectCalendarDay(day) {
  if (!day) return
  selectedDateKey.value = calendarCellKey(day)
}

function formatActivityTime(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}
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
          <el-avatar :size="36" :src="store.getAvatar(store.user.avatar, store.user.username)" />
          <div class="qp-input">有什么新鲜事想和大家分享？</div>
          <button class="qp-btn"><SquarePen :size="16" /> 发布</button>
        </div>

        <!-- Tab 筛选栏 -->
        <div class="topic-tabs">
          <div class="tab-item" :class="{ active: topics.type === 0 }" @click="topics.type = 0">全部</div>
          <div class="tab-item" :class="{ active: topics.type === -3 }" @click="topics.type = -3">✨ 推荐</div>
          <div class="tab-item" :class="{ active: topics.type === -1 }" @click="topics.type = -1">🔥 热门</div>
          <div class="tab-item" :class="{ active: topics.type === -2 }" @click="topics.type = -2">🕐 最新</div>
        </div>

        <!-- 帖子列表 -->
        <transition name="el-fade-in" mode="out-in">
          <div
            v-if="topics.list.length"
            v-infinite-scroll="updateList"
            :infinite-scroll-disabled="topics.loading || topics.end"
            infinite-scroll-distance="240"
            class="post-stream"
          >
            <div class="post-list">
              <div v-for="(column, columnIndex) in topicColumns" :key="columnIndex" class="post-column">
                <article
                  v-for="item in column"
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
                <div class="post-badges" v-if="item.top === 1 || item.featured === 1">
                  <div v-if="item.top === 1" class="pinned-badge"><Pin :size="12" /> 置顶</div>
                  <div v-if="item.featured === 1" class="featured-badge"><Star :size="12" /> 精华</div>
                </div>
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
                    <p class="post-excerpt">{{ stripMarkdown(item.text) }}</p>
                  </div>
                </div>

                <div class="post-footer">
                  <div class="footer-left">
                    <el-avatar :size="20" :src="store.getAvatar(item.avatar, item.username)" class="compact-avatar" />
                    <span class="meta-name">{{ item.username }}</span>
                  </div>
                  <div class="footer-actions">
                    <div class="action-item" title="浏览"><Eye :size="12" /> {{ item.views || 0 }}</div>
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
              </div>
            </div>

            <div v-if="topics.loading" class="stream-status">正在加载更多...</div>
            <div v-else-if="topics.end" class="end-hint">— 已经到底了 —</div>
          </div>
          <el-empty v-else v-loading="topics.loading" description="暂无内容" style="margin-top: 60px;" />
        </transition>
      </div>

      <!-- 右侧边栏 -->
      <aside class="side-column">
        <div class="sticky-side">

          <!-- 今日热榜 -->
          <div class="widget-card" v-if="topics.top && topics.top.length">
            <div class="widget-header">
              <TrendingUp :size="16" class="widget-icon" />
              今日热榜
            </div>
            <ul class="hot-list">
              <li
                v-for="(item, index) in topics.top.slice(0, showAllHot ? 5 : 3)"
                :key="item.id"
                class="hot-item"
                @click="router.push(`/home/topic/${item.id}`)"
              >
                <span class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
                <span class="hot-title">{{ item.title }}</span>
                <span class="hot-score">{{ hotMetric(item) }}</span>
                <Flame :size="13" class="hot-flame" />
              </li>
            </ul>
            <div
              v-if="topics.top.length > 3"
              class="hot-expand-btn"
              @click="showAllHot = !showAllHot"
            >
              <span>{{ showAllHot ? '收起' : '展开全部' }}</span>
              <ChevronUp v-if="showAllHot" :size="14" />
              <ChevronDown v-else :size="14" />
            </div>
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
                :class="{
                  today: d && isToday(d),
                  empty: !d,
                  marked: d && hasCalendarItem(d),
                  selected: d && calendarCellKey(d) === selectedDateKey
                }"
                @click="selectCalendarDay(d)"
              >{{ d || '' }}</span>
            </div>
            <div class="today-schedule">
              <div class="schedule-title">
                <span>{{ selectedDateKey === todayKey ? '今日安排' : '当日安排' }}</span>
                <i>{{ selectedDateLabel }}</i>
              </div>
              <ul class="schedule-list">
                <li
                  v-for="item in selectedCalendarItems"
                  :key="item.id"
                  class="schedule-item"
                  :class="{ clickable: item.route }"
                  @click="item.route && router.push(item.route)"
                >
                  <div class="s-event">
                    <span class="s-kind" :class="item.kind === '活动' ? 'activity' : 'calendar'">{{ item.kind }}</span>
                    <span class="s-name">{{ item.title }}</span>
                    <span class="s-time">{{ item.when }}</span>
                  </div>
                </li>
                <li v-if="!selectedCalendarItems.length" class="schedule-item">
                  <div class="s-event is-empty">{{ selectedDateKey === todayKey ? '暂无今日安排' : '暂无当日安排' }}</div>
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
              <div v-for="n in notices" :key="n.id" class="bulletin-item">
                <span v-if="n.isTop === 1" class="b-top-tag">置顶</span>
                <p class="b-title" style="margin-bottom:0">{{ n.title }}</p>
              </div>
              <div v-if="!notices.length" class="bulletin-item">
                <p class="b-title" style="margin-bottom:0;color:var(--el-text-color-placeholder)">暂无公告</p>
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

/* ===== 帖子列表 (双列错落布局) ===== */
.post-stream {
  display: flex;
  flex-direction: column;
}

.post-list {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.post-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
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

.post-badges {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.pinned-badge, .featured-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 6px;
}

.pinned-badge {
  color: #ef4444;
  background: #fef2f2;
}

.featured-badge {
  color: #d97706;
  background: #fffbeb;
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
  flex: none;
  width: 100%;
  margin-top: 48px;
  text-align: center;
  padding: 24px 0 8px;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

.stream-status {
  flex: none;
  width: 100%;
  margin-top: 36px;
  text-align: center;
  padding: 18px 0 4px;
  font-size: 13px;
  font-weight: 700;
  color: var(--el-color-primary);
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
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}

.hot-score {
  flex-shrink: 0;
  min-width: 30px;
  text-align: right;
  font-size: 11px;
  font-weight: 800;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 999px;
  padding: 2px 6px;
}

.hot-flame {
  color: #f97316;
  flex-shrink: 0;
}

.hot-expand-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 6px;
  padding: 6px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s;

  &:hover {
    background: var(--el-color-primary-light-9);
  }
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
  position: relative;
  padding: 6px 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover:not(.empty):not(.today):not(.selected) { background: var(--el-fill-color-light); }
  &.empty { cursor: default; }

  &.selected:not(.today) {
    background: var(--el-color-primary);
    color: #fff;
    font-weight: 800;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.28);
  }

  &.today {
    background: #f59e0b;
    color: #fff;
    font-weight: 800;
    box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
  }

  &.marked::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 2px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--el-color-primary);
    transform: translateX(-50%);
  }

  &.today.marked::after {
    background: #fff;
  }

  &.selected.marked::after {
    background: #fff;
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
    align-items: center;
    padding: 0;

    &.clickable {
      cursor: pointer;

      .s-event:hover {
        background: var(--el-color-primary-light-9);
      }
    }
  }
  .s-event {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
    min-height: 34px;
    font-size: 12px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    line-height: 1.5;
    background: var(--el-fill-color-lighter);
    padding: 7px 10px;
    border-radius: 8px;
    transition: background 0.15s;

    &.is-empty {
      color: var(--el-text-color-placeholder);
    }
  }
  .s-kind {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
    padding: 1px 5px;
    border-radius: 999px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-size: 10px;
    font-weight: 700;

    &.activity {
      background: #fff7ed;
      color: #ea580c;
    }
  }
  .s-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .s-time {
    flex-shrink: 0;
    font-size: 11px;
    font-weight: 700;
    color: var(--el-text-color-placeholder);
    white-space: nowrap;
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

.b-top-tag {
  display: inline-block;
  font-size: 10px;
  font-weight: 800;
  color: #7C3AED;
  background: #EDE9FE;
  padding: 1px 6px;
  border-radius: 4px;
  margin-right: 6px;
  vertical-align: middle;
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
  .post-list { flex-direction: column; flex-wrap: nowrap; gap: 16px; }
  .post-column { width: 100%; gap: 16px; }
  .end-hint { flex-basis: auto; }
}

@media (max-width: 600px) {
  .page-container { padding: 0 12px 40px; }
  .hero-overlay { padding: 20px; h1 { font-size: 20px; } }
}
</style>
