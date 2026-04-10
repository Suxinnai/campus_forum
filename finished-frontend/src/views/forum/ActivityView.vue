<script setup>
import { ref } from 'vue'
import { get } from '@/net/api.js'
import {
  Calendar, MapPin, Users, Clock, ChevronRight,
  Timer, CalendarDays
} from 'lucide-vue-next'

const list = ref([])
get('/api/activity/list', data => list.value = data)

const detailVisible = ref(false)
const current = ref(null)

function openDetail(item) {
  current.value = item
  detailVisible.value = true
}

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}年${date.getMonth()+1}月${date.getDate()}日 ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

function formatTime(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

function formatMD(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getMonth()+1}月${date.getDate()}日`
}

function getMonth(d) {
  if (!d) return ''
  return (new Date(d).getMonth() + 1) + '月'
}

function getDay(d) {
  if (!d) return ''
  return String(new Date(d).getDate())
}

function getWeekday(d) {
  if (!d) return ''
  return ['周日','周一','周二','周三','周四','周五','周六'][new Date(d).getDay()]
}

function getStatus(item) {
  const now = new Date()
  if (item.endTime && new Date(item.endTime) < now) return { label: '已结束', cls: 'ended' }
  if (item.startTime && new Date(item.startTime) > now) return { label: '即将开始', cls: 'upcoming' }
  return { label: '进行中', cls: 'active' }
}

function getCountdown(item) {
  const now = new Date()
  const start = new Date(item.startTime)
  const end = new Date(item.endTime)
  if (end < now) return null
  if (start > now) {
    const diff = start - now
    const days = Math.floor(diff / 86400000)
    if (days > 0) return `${days} 天后开始`
    const hours = Math.floor(diff / 3600000)
    if (hours > 0) return `${hours} 小时后开始`
    return '即将开始'
  }
  const diff = end - now
  const days = Math.floor(diff / 86400000)
  if (days > 0) return `还剩 ${days} 天`
  const hours = Math.floor(diff / 3600000)
  if (hours > 0) return `还剩 ${hours} 小时`
  return '即将结束'
}
</script>

<template>
  <div class="activity-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">校园活动</h2>
        <span class="count-badge">{{ list.length }} 项</span>
      </div>
    </div>

    <!-- 活动列表 -->
    <div class="activity-list" v-if="list.length">
      <div
        v-for="item in list"
        :key="item.id"
        class="activity-card"
        @click="openDetail(item)"
      >
        <!-- 左：日期 -->
        <div class="date-col">
          <span class="dc-day">{{ getDay(item.startTime) }}</span>
          <span class="dc-month">{{ getMonth(item.startTime) }}</span>
          <span class="dc-week">{{ getWeekday(item.startTime) }}</span>
        </div>

        <!-- 中：核心信息 -->
        <div class="main-col">
          <div class="main-top">
            <span class="status-pill" :class="getStatus(item).cls">{{ getStatus(item).label }}</span>
            <span class="cd-label" v-if="getCountdown(item)">
              <Timer :size="11" /> {{ getCountdown(item) }}
            </span>
          </div>

          <h3 class="main-title">{{ item.title }}</h3>

          <!-- 时间 + 地点 紧贴标题，最重要的辅助信息 -->
          <div class="key-info">
            <span class="ki-item">
              <Clock :size="13" />
              {{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}
            </span>
            <span class="ki-sep" v-if="item.location"></span>
            <span class="ki-item" v-if="item.location">
              <MapPin :size="13" />
              {{ item.location }}
            </span>
          </div>

          <p class="main-desc">{{ item.content }}</p>

          <div class="main-bottom">
            <span class="mb-tag" v-if="item.organizer">
              <Users :size="11" /> {{ item.organizer }}
            </span>
            <span class="mb-tag" v-if="item.maxPeople">限{{ item.maxPeople }}人</span>
          </div>
        </div>

        <!-- 右：箭头 -->
        <div class="go-col">
          <ChevronRight :size="18" />
        </div>
      </div>
    </div>

    <el-empty v-else description="近期暂无校园活动" style="margin-top: 60px" />

    <!-- ===== 详情弹窗 ===== -->
    <el-dialog
      v-model="detailVisible"
      width="520px"
      class="act-dialog"
      destroy-on-close
    >
      <template #header v-if="current">
        <div class="dlg-header">
          <div class="dlg-header-top">
            <span class="status-pill" :class="getStatus(current).cls">{{ getStatus(current).label }}</span>
            <span class="cd-label" v-if="getCountdown(current)">
              <Timer :size="12" />{{ getCountdown(current) }}
            </span>
          </div>
          <h2 class="dlg-title">{{ current.title }}</h2>
          <!-- 摘要信息：一行紧凑展示，不抢戏 -->
          <div class="dlg-summary">
            <span><Clock :size="12" />{{ formatMD(current.startTime) }} {{ formatTime(current.startTime) }}–{{ formatTime(current.endTime) }}</span>
            <span v-if="current.location"><MapPin :size="12" />{{ current.location }}</span>
            <span v-if="current.organizer"><Users :size="12" />{{ current.organizer }}</span>
          </div>
        </div>
      </template>

      <div v-if="current" class="dlg-body">
        <div class="intro-text">{{ current.content }}</div>

        <div class="dlg-footer-chips" v-if="current.maxPeople">
          <span class="footer-chip">
            <Users :size="12" />限 {{ current.maxPeople }} 人参与
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.activity-page {
  max-width: 860px;
  margin: 32px auto;
  padding: 0 4px;
}

/* ── Header ── */
.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
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
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

/* ── Card List ── */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-card {
  display: flex;
  align-items: stretch;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all .3s cubic-bezier(.4,0,.2,1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,.06);
    border-color: var(--el-color-primary-light-5);

    .date-col { background: var(--el-color-primary); .dc-day,.dc-month { color: #fff; } .dc-week { color: rgba(255,255,255,.7); } }
    .main-title { color: var(--el-color-primary); }
    .go-col { color: var(--el-color-primary); }
  }
}

/* 日期列 */
.date-col {
  width: 78px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 18px 0;
  background: var(--el-color-primary-light-9);
  border-right: 1px solid var(--el-border-color-lighter);
  transition: all .25s;

  .dc-day { font-size: 28px; font-weight: 900; color: var(--el-color-primary); line-height: 1; }
  .dc-month { font-size: 12px; font-weight: 700; color: var(--el-color-primary); margin-top: 4px; }
  .dc-week { font-size: 11px; font-weight: 600; color: var(--el-text-color-placeholder); margin-top: 3px; transition: color .25s; }

  border-top-left-radius: 23px;
  border-bottom-left-radius: 23px;
}

/* 主体列 */
.main-col {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.main-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.status-pill {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  &.active  { background: #dcfce7; color: #16a34a; box-shadow: 0 2px 8px rgba(22, 163, 74, 0.1); }
  &.upcoming { background: #eef2ff; color: #4f46e5; box-shadow: 0 2px 8px rgba(79, 70, 229, 0.1); }
  &.ended   { background: var(--el-fill-color-light); color: var(--el-text-color-placeholder); }
}

.cd-label {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.main-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color .2s;
}

/* 时间+地点 醒目行 */
.key-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);

  .ki-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    svg { color: var(--el-color-primary); }
  }

  .ki-sep {
    width: 1px;
    height: 12px;
    background: var(--el-border-color);
  }
}

.main-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.main-bottom {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.mb-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  svg { color: var(--el-color-primary-light-3); }
}

/* 箭头列 */
.go-col {
  width: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  transition: color .2s;
}

/* ========== Dialog（scoped 内部样式） ========== */

/* 弹窗顶部 */
.dlg-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dlg-header-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dlg-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0;
  line-height: 1.35;
}

/* 一行摘要：次要信息，字小、灰色，不抢焦点 */
.dlg-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 16px;
  padding: 10px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    svg { color: var(--el-color-primary-light-3); flex-shrink: 0; }
  }
}

/* 弹窗主体 */
.dlg-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 活动介绍正文：主角 */
.intro-text {
  font-size: 15px;
  font-weight: 400;
  color: var(--el-text-color-primary);
  line-height: 1.9;
  white-space: pre-wrap;
  padding: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
  min-height: 80px;
}

.dlg-footer-chips {
  display: flex;
  gap: 8px;
}

.footer-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  background: var(--el-fill-color-light);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  svg { color: var(--el-color-primary-light-3); }
}

</style>

<style>
/* el-dialog 圆角需在非 scoped 块中覆盖，否则 class 落在宿主元素上时 :deep 无效 */
.act-dialog.el-dialog {
  border-radius: 20px !important;
  overflow: hidden !important;
}
.act-dialog .el-dialog__header {
  padding: 28px 28px 0;
  margin: 0;
}
.act-dialog .el-dialog__body {
  padding: 20px 28px 32px;
}
.act-dialog .el-dialog__headerbtn {
  top: 20px;
  right: 20px;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}
.act-dialog .el-dialog__headerbtn:hover {
  background: var(--el-fill-color-light);
}
</style>
