<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { get, post, put, del } from '@/net/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app-store.js'
import {
  Users, FileText, MessageSquare, TrendingUp, Shield, Trash2, Search,
  Pin, PinOff, Bell, Calendar, CalendarDays, Package, AlertTriangle, Plus,
  RotateCw, Edit2, Check, X, PartyPopper, Star, Clock, MapPin
} from 'lucide-vue-next'

const store = useAppStore()
const myRole = computed(() => store.user.role)
const isAdmin = computed(() => myRole.value === 'admin')
const isContentAdmin = computed(() => myRole.value === 'content_admin')
const isModerator = computed(() => myRole.value === 'moderator')
const canManageTopics = computed(() => isAdmin.value || isContentAdmin.value || isModerator.value)

// ===================== 状态 =====================
const activeTab = ref('topics')
const stats = reactive({ users: 0, topics: 0, comments: 0, todayPosts: 0, sensitiveWords: 0, todayRegistered: 0, todayActive: 0, hotTopics: [], dailyPosts: [] })

// 用户管理
const users = ref([])
const userPage = ref(1)
const userTotal = ref(0)
const userSearch = ref('')
const selectedUsers = ref([])

// 帖子管理
const topics = ref([])
const topicPage = ref(1)
const topicTotal = ref(0)
const selectedTopics = ref([])

// 公告管理
const notices = ref([])
const showNoticeDialog = ref(false)
const noticeForm = reactive({ title: '', content: '', publisher: '校方', isTop: 0 })
const editNoticeMode = ref(false)
const editNoticeId = ref(null)

// 活动管理
const activities = ref([])
const showActivityDialog = ref(false)
const activityEditMode = ref(false)
const activityForm = reactive({
  id: null, title: '', content: '', location: '', organizer: '',
  startTime: '', endTime: '', maxPeople: 0
})

// 校历日程管理（独立于活动）
const schedules = ref([])
const showScheduleDialog = ref(false)
const scheduleEditMode = ref(false)
const scheduleForm = reactive({
  id: null, title: '', description: '', eventDate: '', endDate: '', type: 'event'
})

// 资源管理
const resources = ref([])
const resourcePage = ref(1)
const resourceTotal = ref(0)

// 反馈管理
const feedbacks = ref([])
const feedbackLoading = ref(false)

// 敏感词管理
const sensitiveWords = ref([])
const newWord = ref('')

// 版块类型
const topicTypes = ref([])
function loadTopicTypes() {
  get('/api/forum/types', data => { topicTypes.value = data || [] })
}

// 版主设置弹窗
const showModeratorDialog = ref(false)
const pendingModeratorUser = ref(null)
const pendingModeratorType = ref(null)

function openModeratorDialog(user) {
  pendingModeratorUser.value = user
  pendingModeratorType.value = user.moderatorType || null
  showModeratorDialog.value = true
}

function confirmModeratorAssign() {
  if (!pendingModeratorType.value) {
    ElMessage.warning('请选择负责的版块')
    return
  }
  post(`/api/admin/user/role?id=${pendingModeratorUser.value.id}&role=moderator&moderatorType=${pendingModeratorType.value}`, {}, () => {
    ElMessage.success('版主已设置')
    showModeratorDialog.value = false
    loadUsers()
  })
}

// ===================== 数据加载 =====================
function loadStats() {
  get('/api/admin/stats', data => { Object.assign(stats, data) })
}

function loadUsers() {
  let url = `/api/admin/users?page=${userPage.value - 1}`
  if (userSearch.value) url += `&keyword=${userSearch.value}`
  get(url, data => {
    users.value = data.records || data
    userTotal.value = data.total || data.length
  })
}

function loadTopics() {
  get(`/api/admin/topics?page=${topicPage.value - 1}`, data => {
    topics.value = data.records || data
    topicTotal.value = data.total || data.length
  })
}

function loadNotices() {
  get('/api/admin/notices', data => { notices.value = data })
}

function loadActivities() {
  get('/api/admin/activities', data => { activities.value = data || [] })
}

function loadSchedules() {
  get('/api/admin/schedules', data => { schedules.value = data || [] })
}

function loadResources() {
  get(`/api/admin/resources?page=${resourcePage.value - 1}`, data => {
    resources.value = data.records || data
    resourceTotal.value = data.total || data.length
  })
}

function loadFeedbacks() {
  feedbackLoading.value = true
  get('/api/admin/feedbacks', data => {
    feedbacks.value = data || []
    feedbackLoading.value = false
  }, () => {
    feedbackLoading.value = false
    ElMessage.error('加载反馈失败，请检查数据库是否已执行最新迁移脚本')
  })
}

function loadSensitiveWords() {
  get('/api/admin/sensitive-words', data => { sensitiveWords.value = data })
}

// ===================== 操作函数 =====================

// 用户管理
function deleteUser(id, username) {
  ElMessageBox.confirm(`确定删除用户「${username}」吗？该用户的所有数据将被清除。`, '高危操作确认', {
    confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    del(`/api/admin/user?id=${id}`, () => {
      ElMessage.success('用户已删除')
      loadUsers(); loadStats()
    })
  }).catch(() => {})
}

function toggleBan(user) {
  const newBanned = user.banned === 1 ? false : true
  const msg = newBanned ? `确定封禁用户「${user.username}」吗？` : `确定解封用户「${user.username}」吗？`
  ElMessageBox.confirm(msg, '操作确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    post(`/api/admin/user/ban?id=${user.id}&banned=${newBanned}`, {}, () => {
      ElMessage.success(newBanned ? '用户已封禁' : '用户已解封')
      loadUsers()
    })
  }).catch(() => {})
}

function changeRole(user, role) {
  if (role === 'moderator') { openModeratorDialog(user); return }
  post(`/api/admin/user/role?id=${user.id}&role=${role}`, {}, () => {
    ElMessage.success('角色已更新')
    loadUsers()
  })
}

function batchDeleteUsers() {
  if (!selectedUsers.value.length) { ElMessage.warning('请先选择用户'); return }
  ElMessageBox.confirm(`确定批量删除 ${selectedUsers.value.length} 个用户吗？`, '高危操作', {
    confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    post('/api/admin/user/batch-delete', selectedUsers.value.map(u => u.id), () => {
      ElMessage.success('批量删除成功')
      selectedUsers.value = []
      loadUsers(); loadStats()
    })
  }).catch(() => {})
}

function batchBanUsers(banned) {
  if (!selectedUsers.value.length) { ElMessage.warning('请先选择用户'); return }
  post(`/api/admin/user/batch-ban?banned=${banned}`, selectedUsers.value.map(u => u.id), () => {
    ElMessage.success(banned ? '批量封禁成功' : '批量解封成功')
    selectedUsers.value = []
    loadUsers()
  })
}

// 帖子管理
function deleteTopic(id) {
  ElMessageBox.confirm('确定删除该帖子吗？此操作不可恢复。', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/topic?id=${id}`, () => {
      ElMessage.success('帖子已删除')
      loadTopics(); loadStats()
    })
  }).catch(() => {})
}

function toggleTopTopic(topic) {
  const newTop = topic.top === 1 ? 0 : 1
  post(`/api/admin/topic/top?id=${topic.id}&top=${newTop}`, {}, () => {
    topic.top = newTop
    ElMessage.success(newTop === 1 ? '置顶成功' : '取消置顶成功')
  })
}

function auditTopic(topic, status) {
  post(`/api/admin/topic/status?id=${topic.id}&status=${status}`, {}, () => {
    topic.status = status
    ElMessage.success(status === 'approved' ? '已通过审核' : status === 'pending' ? '已设为待审核' : '已驳回')
  })
}

function toggleFeatured(topic) {
  const newVal = topic.featured === 1 ? 0 : 1
  post(`/api/admin/topic/featured?id=${topic.id}&featured=${newVal}`, {}, () => {
    topic.featured = newVal
    ElMessage.success(newVal === 1 ? '已设为精华' : '已取消精华')
  })
}

function batchDeleteTopics() {
  if (!selectedTopics.value.length) { ElMessage.warning('请先选择帖子'); return }
  ElMessageBox.confirm(`确定批量删除 ${selectedTopics.value.length} 篇帖子吗？`, '高危操作', {
    confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    post('/api/admin/topic/batch-delete', selectedTopics.value.map(t => t.id), () => {
      ElMessage.success('批量删除成功')
      selectedTopics.value = []
      loadTopics(); loadStats()
    })
  }).catch(() => {})
}

// 公告管理
function openAddNotice() {
  editNoticeMode.value = false
  editNoticeId.value = null
  Object.assign(noticeForm, { title: '', content: '', publisher: '校方', isTop: 0 })
  showNoticeDialog.value = true
}

function openEditNotice(n) {
  editNoticeMode.value = true
  editNoticeId.value = n.id
  Object.assign(noticeForm, { title: n.title, content: n.content, publisher: n.publisher, isTop: n.isTop })
  showNoticeDialog.value = true
}

function submitNotice() {
  if (!noticeForm.title || !noticeForm.content) { ElMessage.warning('请填写标题和内容'); return }
  if (editNoticeMode.value && editNoticeId.value) {
    put('/api/admin/notice/update', { id: editNoticeId.value, ...noticeForm }, () => {
      ElMessage.success('公告已更新')
      showNoticeDialog.value = false
      loadNotices()
    })
  } else {
    post('/api/admin/notice/add', noticeForm, () => {
      ElMessage.success('公告发布成功')
      showNoticeDialog.value = false
      loadNotices()
    })
  }
}

function toggleNoticeTop(n) {
  const newTop = n.isTop === 1 ? 0 : 1
  post(`/api/admin/notice/top?id=${n.id}&isTop=${newTop}`, {}, () => {
    n.isTop = newTop
    ElMessage.success(newTop === 1 ? '已置顶' : '已取消置顶')
  })
}

function deleteNotice(id) {
  ElMessageBox.confirm('确定删除此公告？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/notice/delete?id=${id}`, () => {
      ElMessage.success('公告已删除')
      loadNotices()
    })
  }).catch(() => {})
}

// ===== 活动管理（独立于校历）=====
function openAddActivity() {
  activityEditMode.value = false
  Object.assign(activityForm, { id: null, title: '', content: '', location: '', organizer: '', startTime: '', endTime: '', maxPeople: 0 })
  showActivityDialog.value = true
}

function openEditActivity(act) {
  activityEditMode.value = true
  Object.assign(activityForm, {
    id: act.id,
    title: act.title,
    content: act.content,
    location: act.location,
    organizer: act.organizer,
    startTime: act.startTime ? new Date(act.startTime).toISOString().slice(0, 16) : '',
    endTime: act.endTime ? new Date(act.endTime).toISOString().slice(0, 16) : '',
    maxPeople: act.maxPeople || 0
  })
  showActivityDialog.value = true
}

function submitActivity() {
  if (!activityForm.title || !activityForm.startTime || !activityForm.endTime) {
    ElMessage.warning('请填写活动标题和时间')
    return
  }
  const payload = { ...activityForm }
  if (activityEditMode.value) {
    put('/api/admin/activity/update', payload, () => {
      ElMessage.success('活动已更新')
      showActivityDialog.value = false
      loadActivities()
    })
  } else {
    post('/api/admin/activity/add', payload, () => {
      ElMessage.success('活动添加成功')
      showActivityDialog.value = false
      loadActivities()
    })
  }
}

function deleteActivity(id) {
  ElMessageBox.confirm('确定删除此活动？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/activity/delete?id=${id}`, () => {
      ElMessage.success('活动已删除')
      loadActivities()
    })
  }).catch(() => {})
}

// ===== 校历日程管理（独立于活动）=====
function openAddSchedule() {
  scheduleEditMode.value = false
  Object.assign(scheduleForm, { id: null, title: '', description: '', eventDate: '', endDate: '', type: 'event' })
  showScheduleDialog.value = true
}

function openEditSchedule(s) {
  scheduleEditMode.value = true
  Object.assign(scheduleForm, {
    id: s.id,
    title: s.title,
    description: s.description || '',
    eventDate: s.eventDate ? s.eventDate.slice(0, 10) : '',
    endDate: s.endDate ? s.endDate.slice(0, 10) : '',
    type: s.type || 'event'
  })
  showScheduleDialog.value = true
}

function submitSchedule() {
  if (!scheduleForm.title || !scheduleForm.eventDate) {
    ElMessage.warning('请填写日程标题和日期')
    return
  }
  const payload = { ...scheduleForm }
  if (scheduleEditMode.value) {
    put('/api/admin/schedule/update', payload, () => {
      ElMessage.success('日程已更新')
      showScheduleDialog.value = false
      loadSchedules()
    })
  } else {
    post('/api/admin/schedule/add', payload, () => {
      ElMessage.success('日程添加成功')
      showScheduleDialog.value = false
      loadSchedules()
    })
  }
}

function deleteSchedule(id) {
  ElMessageBox.confirm('确定删除此日程？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/schedule/delete?id=${id}`, () => {
      ElMessage.success('日程已删除')
      loadSchedules()
    })
  }).catch(() => {})
}

const scheduleTypeMap = { semester: { label: '学期', color: '#059669', bg: '#D1FAE5' }, exam: { label: '考试', color: '#DC2626', bg: '#FEE2E2' }, holiday: { label: '假期', color: '#D97706', bg: '#FEF3C7' }, event: { label: '其他', color: '#6366f1', bg: '#EDE9FE' } }
const getScheduleType = (type) => scheduleTypeMap[type] || scheduleTypeMap.event

// 资源管理
function deleteResource(id) {
  ElMessageBox.confirm('确定删除此资源文件吗？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/resource/delete?id=${id}`, () => {
      ElMessage.success('资源已删除')
      loadResources()
    })
  }).catch(() => {})
}

// 反馈管理
function resolveFeedback(id) {
  post(`/api/admin/feedback/resolve?id=${id}`, {}, () => {
    ElMessage.success('已标记为已处理')
    loadFeedbacks()
  })
}

function deleteFeedback(id) {
  ElMessageBox.confirm('确定删除此反馈？', '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/feedback/delete?id=${id}`, () => {
      ElMessage.success('反馈已删除')
      loadFeedbacks()
    })
  }).catch(() => {})
}

// 敏感词管理
function addWord() {
  if (!newWord.value.trim()) return
  post(`/api/admin/sensitive-word/add?word=${encodeURIComponent(newWord.value.trim())}`, {}, () => {
    ElMessage.success('添加成功')
    newWord.value = ''
    loadSensitiveWords()
    loadStats()
  })
}

function deleteWord(id, word) {
  ElMessageBox.confirm(`确定从词库中移除「${word}」？`, '确认', {
    confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    del(`/api/admin/sensitive-word/delete?id=${id}`, () => {
      ElMessage.success('已移除')
      loadSensitiveWords()
      loadStats()
    })
  }).catch(() => {})
}

// ===================== 工具函数 =====================
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function onTabChange(tab) {
  const loaders = {
    overview: loadStats,
    users: loadUsers,
    topics: loadTopics,
    notices: loadNotices,
    calendar: loadSchedules,
    activities: loadActivities,
    resources: loadResources,
    feedback: loadFeedbacks,
    sensitive: loadSensitiveWords
  }
  if (loaders[tab]) loaders[tab]()
}

onMounted(() => {
  loadTopicTypes()
  loadStats()
  loadTopics()
  if (isAdmin.value) {
    activeTab.value = 'overview'
    loadUsers()
    loadNotices()
    loadActivities()
    loadSchedules()
    loadResources()
    loadFeedbacks()
    loadSensitiveWords()
  }
})
</script>

<template>
  <div class="admin-view">
    <!-- Page Header -->
    <div class="admin-header">
      <div class="header-left">
        <div class="admin-badge">
          <Shield :size="16" />
          <span>管理后台</span>
        </div>
        <h1 class="admin-title">
          <template v-if="isAdmin">全站控制中心</template>
          <template v-else-if="isContentAdmin">内容管理后台</template>
          <template v-else-if="isModerator">版主管理后台</template>
        </h1>
      </div>
      <button class="refresh-btn" @click="loadStats">
        <RotateCw :size="14" /> 刷新数据
      </button>
    </div>

    <!-- Stats Cards（仅超级管理员） -->
    <div v-if="isAdmin" class="stats-grid">
      <div class="stat-card" data-color="purple" @click="activeTab = 'users'; onTabChange('users')">
        <div class="stat-icon"><Users :size="20" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.users }}</div>
          <div class="stat-label">注册用户总数</div>
        </div>
      </div>
      <div class="stat-card" data-color="green" @click="activeTab = 'topics'; onTabChange('topics')">
        <div class="stat-icon"><FileText :size="20" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.topics }}</div>
          <div class="stat-label">帖子总量</div>
        </div>
      </div>
      <div class="stat-card" data-color="red">
        <div class="stat-icon"><MessageSquare :size="20" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.comments }}</div>
          <div class="stat-label">评论总量</div>
        </div>
      </div>
      <div class="stat-card" data-color="yellow">
        <div class="stat-icon"><TrendingUp :size="20" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.todayPosts }}</div>
          <div class="stat-label">今日新帖</div>
        </div>
      </div>
      <div class="stat-card" data-color="orange" @click="activeTab = 'sensitive'; onTabChange('sensitive')">
        <div class="stat-icon"><AlertTriangle :size="20" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.sensitiveWords }}</div>
          <div class="stat-label">敏感词数量</div>
        </div>
      </div>
    </div>

    <!-- 数据看板（仅超级管理员） -->
    <div v-if="isAdmin && activeTab === 'overview'" class="dashboard-section">
      <div class="dash-row">
        <div class="dash-mini-card">
          <div class="dmc-label">今日注册</div>
          <div class="dmc-num">{{ stats.todayRegistered }}</div>
        </div>
        <div class="dash-mini-card">
          <div class="dmc-label">今日活跃</div>
          <div class="dmc-num">{{ stats.todayActive }}</div>
        </div>
        <div class="dash-mini-card">
          <div class="dmc-label">今日新帖</div>
          <div class="dmc-num">{{ stats.todayPosts }}</div>
        </div>
      </div>
      <div class="dash-grid">
        <div class="dash-card panel">
          <div class="panel-header" style="border-bottom:none;padding-bottom:0">
            <div class="panel-title">近 7 日发帖趋势</div>
          </div>
          <div class="bar-chart">
            <div v-for="d in stats.dailyPosts" :key="d.date" class="bar-col">
              <div class="bar-value">{{ d.count }}</div>
              <div class="bar-fill" :style="{ height: (d.count / (Math.max(...stats.dailyPosts.map(x => x.count)) || 1)) * 100 + '%' }"></div>
              <div class="bar-label">{{ d.date.slice(5) }}</div>
            </div>
          </div>
        </div>
        <div class="dash-card panel">
          <div class="panel-header" style="border-bottom:none;padding-bottom:0">
            <div class="panel-title">热门帖子 Top 5</div>
          </div>
          <div class="hot-list">
            <div v-for="(t, idx) in stats.hotTopics" :key="t.id" class="hot-item">
              <span class="hot-rank" :class="{ top3: idx < 3 }">{{ idx + 1 }}</span>
              <span class="hot-title">{{ t.title }}</span>
              <span class="hot-likes">{{ t.likes }} 赞</span>
            </div>
            <el-empty v-if="!stats.hotTopics?.length" description="暂无数据" :image-size="50" />
          </div>
        </div>
      </div>
    </div>

    <!-- Nav Tabs -->
    <div class="admin-tabs">
      <button v-for="tab in [
        { key: 'overview',    icon: TrendingUp,    label: '数据总览',  adminOnly: true },
        { key: 'users',       icon: Users,         label: '用户管理',  adminOnly: true },
        { key: 'topics',      icon: FileText,      label: '帖子管理',  adminOnly: false },
        { key: 'notices',     icon: Bell,          label: '公告管理',  adminOnly: true },
        { key: 'calendar',    icon: CalendarDays,  label: '校历日程',  adminOnly: true },
        { key: 'activities',  icon: PartyPopper,   label: '活动管理',  adminOnly: true },
        { key: 'resources',   icon: Package,       label: '资源审计',  adminOnly: true },
        { key: 'feedback',    icon: MessageSquare, label: '用户反馈',  adminOnly: true },
        { key: 'sensitive',   icon: AlertTriangle, label: '敏感词库',  adminOnly: true }
      ].filter(t => !t.adminOnly || isAdmin)"
        :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key; onTabChange(tab.key)">
        <component :is="tab.icon" :size="14" />
        {{ tab.label }}
      </button>
    </div>

    <!-- ============ 用户管理 ============ -->
    <div v-if="isAdmin && activeTab === 'users'" class="panel">
      <div class="panel-header">
        <div class="panel-title">用户列表</div>
        <div style="display:flex;align-items:center;gap:10px">
          <template v-if="selectedUsers.length">
            <button class="action-btn danger" @click="batchDeleteUsers">批量删除 ({{ selectedUsers.length }})</button>
            <button class="action-btn warning" @click="batchBanUsers(true)">批量封禁</button>
            <button class="action-btn success" @click="batchBanUsers(false)">批量解封</button>
          </template>
          <div class="search-box">
            <Search :size="14" class="s-icon" />
            <input v-model="userSearch" placeholder="搜索用户名或邮箱..." @input="loadUsers" />
          </div>
        </div>
      </div>
      <el-table :data="users" style="width:100%" class="admin-table" @selection-change="(val) => selectedUsers = val">
        <el-table-column type="selection" width="45" :selectable="(row) => row.role !== 'admin'" />
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar ? `http://localhost:8080/images${row.avatar}` : 'https://www.vexipui.com/qmhc.jpg'" />
              <span class="name">{{ row.username }}</span>
              <span v-if="row.banned === 1" class="status-tag banned">封禁</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="180" />
        <el-table-column label="角色" width="220">
          <template #default="{ row }">
            <span v-if="row.role === 'admin'" class="role-tag admin">超级管理员</span>
            <div v-else style="display:flex;align-items:center;gap:6px">
              <el-select :model-value="row.role" size="small" @change="(val) => changeRole(row, val)" style="width:120px">
                <el-option label="普通用户" value="user" />
                <el-option label="内容管理员" value="content_admin" />
                <el-option label="版主" value="moderator" />
              </el-select>
              <span v-if="row.role === 'moderator' && row.moderatorType" class="role-tag moderator" :title="'负责版块ID: ' + row.moderatorType">
                {{ topicTypes.find(t => t.id === row.moderatorType)?.name || '版块' + row.moderatorType }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatTime(row.registerTime || row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <div v-if="row.role !== 'admin'" class="cell-actions">
              <button class="cell-btn" :class="row.banned === 1 ? 'success' : 'warning'" @click="toggleBan(row)">
                {{ row.banned === 1 ? '解封' : '封禁' }}
              </button>
              <button class="icon-btn danger" @click="deleteUser(row.id, row.username)"><Trash2 :size="13" /></button>
            </div>
            <span v-else class="muted-text">受保护</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="userPage" @current-change="loadUsers" :total="userTotal" :page-size="10" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 帖子管理 ============ -->
    <div v-if="activeTab === 'topics'" class="panel">
      <div class="panel-header">
        <div class="panel-title">帖子列表</div>
        <div style="display:flex;align-items:center;gap:10px">
          <button v-if="selectedTopics.length" class="action-btn danger" @click="batchDeleteTopics">批量删除 ({{ selectedTopics.length }})</button>
          <span class="panel-hint">置顶/精华帖将在社区首页优先展示</span>
        </div>
      </div>
      <el-table :data="topics" style="width:100%" class="admin-table" @selection-change="(val) => selectedTopics = val">
        <el-table-column type="selection" width="45" />
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="帖子标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="发帖人" prop="uid" width="70" align="center" />
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">{{ formatTime(row.time) }}</template>
        </el-table-column>
        <el-table-column label="审核" width="120" align="center">
          <template #default="{ row }">
            <el-select :model-value="row.status || 'approved'" size="small" @change="(val) => auditTopic(row, val)" style="width:105px">
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="精华" width="90" align="center">
          <template #default="{ row }">
            <button class="toggle-btn" :class="{ active: row.featured === 1 }" @click="toggleFeatured(row)">
              <Star :size="12" />
              {{ row.featured === 1 ? '精华' : '普通' }}
            </button>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="置顶" width="90" align="center">
          <template #default="{ row }">
            <button class="toggle-btn" :class="{ active: row.top === 1 }" @click="toggleTopTopic(row)">
              <Pin v-if="row.top !== 1" :size="12" />
              <PinOff v-else :size="12" />
              {{ row.top === 1 ? '置顶' : '普通' }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="删除" width="65" align="center">
          <template #default="{ row }">
            <button class="icon-btn danger" @click="deleteTopic(row.id)"><Trash2 :size="13" /></button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="topicPage" @current-change="loadTopics" :total="topicTotal" :page-size="10" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 公告管理 ============ -->
    <div v-if="isAdmin && activeTab === 'notices'" class="panel">
      <div class="panel-header">
        <div class="panel-title">全站公告</div>
        <button class="action-btn primary" @click="openAddNotice"><Plus :size="14" /> 发布新公告</button>
      </div>
      <div class="notice-list">
        <div v-for="n in notices" :key="n.id" class="notice-item" :class="{ pinned: n.isTop === 1 }">
          <div v-if="n.isTop === 1" class="pin-badge">置顶</div>
          <div class="notice-main">
            <div class="notice-title">{{ n.title }}</div>
            <div class="notice-content">{{ n.content }}</div>
            <div class="notice-meta">
              <span>{{ n.publisher }}</span>
              <span>{{ formatTime(n.createTime) }}</span>
            </div>
          </div>
          <div class="notice-actions">
            <button class="toggle-btn" :class="{ active: n.isTop === 1 }" @click="toggleNoticeTop(n)">
              <Pin :size="12" />
              {{ n.isTop === 1 ? '取消置顶' : '置顶' }}
            </button>
            <button class="icon-btn edit" @click="openEditNotice(n)"><Edit2 :size="13" /></button>
            <button class="icon-btn danger" @click="deleteNotice(n.id)"><Trash2 :size="13" /></button>
          </div>
        </div>
        <el-empty v-if="!notices.length" description="暂无公告" style="padding:40px 0" />
      </div>
    </div>

    <!-- ============ 校历日程管理 ============ -->
    <div v-if="isAdmin && activeTab === 'calendar'" class="panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">校历日程</div>
          <div class="panel-hint">管理学期、考试、假期等校历事件</div>
        </div>
        <button class="action-btn primary" @click="openAddSchedule"><Plus :size="14" /> 新增日程</button>
      </div>
      <div class="schedule-list">
        <div v-for="s in schedules" :key="s.id" class="schedule-item">
          <div class="schedule-type-bar" :style="{ background: getScheduleType(s.type).color }"></div>
          <div class="schedule-body">
            <div class="schedule-main">
              <span class="schedule-type-badge"
                :style="{ background: getScheduleType(s.type).bg, color: getScheduleType(s.type).color }">
                {{ getScheduleType(s.type).label }}
              </span>
              <span class="schedule-title">{{ s.title }}</span>
            </div>
            <div class="schedule-date">
              <Calendar :size="13" />
              {{ s.eventDate?.slice(0, 10) }}
              <template v-if="s.endDate"> → {{ s.endDate?.slice(0, 10) }}</template>
            </div>
            <div v-if="s.description" class="schedule-desc">{{ s.description }}</div>
          </div>
          <div class="schedule-actions">
            <button class="icon-btn edit" @click="openEditSchedule(s)"><Edit2 :size="13" /></button>
            <button class="icon-btn danger" @click="deleteSchedule(s.id)"><Trash2 :size="13" /></button>
          </div>
        </div>
        <el-empty v-if="!schedules.length" description="暂无校历日程" style="padding:40px 0" />
      </div>
    </div>

    <!-- ============ 活动管理 ============ -->
    <div v-if="isAdmin && activeTab === 'activities'" class="panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">校园活动</div>
          <div class="panel-hint">管理校园线下活动与赛事信息</div>
        </div>
        <button class="action-btn primary" @click="openAddActivity"><Plus :size="14" /> 新增活动</button>
      </div>
      <el-table :data="activities" style="width:100%" class="admin-table">
        <el-table-column label="活动标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="地点" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:5px;color:var(--el-text-color-secondary)">
              <MapPin :size="12" style="flex-shrink:0" />
              <span>{{ row.location || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="主办方" prop="organizer" width="110" show-overflow-tooltip />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="报名" width="90" align="center">
          <template #default="{ row }">
            <span class="quota-badge">{{ row.currentPeople || 0 }}/{{ row.maxPeople || '∞' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <div class="cell-actions" style="justify-content:center">
              <button class="icon-btn edit" @click="openEditActivity(row)"><Edit2 :size="13" /></button>
              <button class="icon-btn danger" @click="deleteActivity(row.id)"><Trash2 :size="13" /></button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!activities.length" description="暂无校园活动" style="padding:40px 0" />
    </div>

    <!-- ============ 资源审计 ============ -->
    <div v-if="isAdmin && activeTab === 'resources'" class="panel">
      <div class="panel-header">
        <div class="panel-title">资源文件审计</div>
        <div class="panel-hint">可删除违规或失效的共享文件</div>
      </div>
      <el-table :data="resources" style="width:100%" class="admin-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="资源标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" prop="category" width="100" />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="下载" prop="downloadCount" width="80" align="center" />
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <button class="icon-btn danger" @click="deleteResource(row.id)"><Trash2 :size="13" /></button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="resourcePage" @current-change="loadResources" :total="resourceTotal" :page-size="15" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 用户反馈 ============ -->
    <div v-if="isAdmin && activeTab === 'feedback'" class="panel">
      <div class="panel-header">
        <div class="panel-title">用户反馈</div>
        <div class="panel-hint">来自用户的建议、Bug 反馈和问题报告</div>
      </div>
      <div v-if="feedbackLoading" style="padding:60px;text-align:center;color:var(--el-text-color-secondary)">
        加载中...
      </div>
      <div class="feedback-list" v-else-if="feedbacks.length">
        <div v-for="fb in feedbacks" :key="fb.id" class="feedback-item" :class="{ resolved: fb.status === 'resolved' }">
          <div class="fb-top">
            <span class="type-tag" :class="fb.type">
              {{ fb.type === 'bug' ? '🐛 Bug' : fb.type === 'suggestion' ? '💡 建议' : '📌 其它' }}
            </span>
            <span class="status-badge" :class="fb.status">
              {{ fb.status === 'resolved' ? '已处理' : '待处理' }}
            </span>
            <span class="fb-user">{{ fb.username }}</span>
            <span class="fb-time">{{ formatTime(fb.createTime) }}</span>
          </div>
          <div class="fb-title">{{ fb.title }}</div>
          <div class="fb-content">{{ fb.content }}</div>
          <div v-if="fb.contact" class="fb-contact">📞 联系方式：{{ fb.contact }}</div>
          <div class="fb-actions">
            <button v-if="fb.status !== 'resolved'" class="action-btn success small" @click="resolveFeedback(fb.id)">
              <Check :size="13" /> 标记已处理
            </button>
            <button class="icon-btn danger" @click="deleteFeedback(fb.id)"><Trash2 :size="13" /></button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无用户反馈" style="padding:40px 0" />
    </div>

    <!-- ============ 敏感词库 ============ -->
    <div v-if="isAdmin && activeTab === 'sensitive'" class="panel">
      <div class="panel-header">
        <div class="panel-title">敏感词库管理</div>
        <div class="panel-hint">发帖/评论时自动拦截含有这些词的内容</div>
      </div>
      <div class="word-add-box">
        <input v-model="newWord" placeholder="输入要添加的敏感词..." @keyup.enter="addWord" maxlength="50" />
        <button class="action-btn primary" @click="addWord"><Plus :size="14" /> 添加</button>
      </div>
      <div class="word-cloud" v-if="sensitiveWords.length">
        <div v-for="w in sensitiveWords" :key="w.id" class="word-tag">
          <span>{{ w.word }}</span>
          <button class="word-del" @click="deleteWord(w.id, w.word)"><X :size="11" /></button>
        </div>
      </div>
      <el-empty v-else description="词库为空，添加敏感词以开启内容过滤" style="margin:20px 0" />
    </div>

    <!-- ============ 版主版块选择弹窗 ============ -->
    <el-dialog v-model="showModeratorDialog" title="设置版主负责版块" width="360px" :close-on-click-modal="false">
      <div style="margin-bottom:8px;color:var(--el-text-color-secondary);font-size:13px">
        为 <strong>{{ pendingModeratorUser?.username }}</strong> 指定负责的版块：
      </div>
      <el-select v-model="pendingModeratorType" placeholder="请选择版块" style="width:100%">
        <el-option v-for="t in topicTypes" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="showModeratorDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmModeratorAssign">确认设置</el-button>
      </template>
    </el-dialog>

    <!-- ============ 公告发布/编辑抽屉 ============ -->
    <el-drawer v-model="showNoticeDialog" :title="editNoticeMode ? '编辑公告' : '发布校园公告'" size="480px" direction="rtl">
      <el-form label-position="top">
        <el-form-item label="公告标题">
          <el-input v-model="noticeForm.title" maxlength="100" placeholder="请填写公告标题" />
        </el-form-item>
        <el-form-item label="发布机构">
          <el-input v-model="noticeForm.publisher" maxlength="50" placeholder="如：教务处、校方、信息中心" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input type="textarea" v-model="noticeForm.content" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="是否顶置">
          <el-switch v-model="noticeForm.isTop" :active-value="1" :inactive-value="0" active-text="顶置" inactive-text="普通" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoticeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitNotice">{{ editNoticeMode ? '保存修改' : '发布公告' }}</el-button>
      </template>
    </el-drawer>

    <!-- ============ 校历日程编辑抽屉 ============ -->
    <el-drawer v-model="showScheduleDialog" :title="scheduleEditMode ? '编辑日程' : '新增校历日程'" size="480px" direction="rtl">
      <el-form label-position="top">
        <el-form-item label="日程标题">
          <el-input v-model="scheduleForm.title" maxlength="100" placeholder="如：期末考试周 / 暑假开始" />
        </el-form-item>
        <el-form-item label="日程类型">
          <el-select v-model="scheduleForm.type" style="width:100%">
            <el-option label="学期事件" value="semester" />
            <el-option label="考试" value="exam" />
            <el-option label="假期" value="holiday" />
            <el-option label="其他" value="event" />
          </el-select>
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
          <el-form-item label="开始日期">
            <el-input type="date" v-model="scheduleForm.eventDate" />
          </el-form-item>
          <el-form-item label="结束日期（可选）">
            <el-input type="date" v-model="scheduleForm.endDate" />
          </el-form-item>
        </div>
        <el-form-item label="描述（可选）">
          <el-input type="textarea" v-model="scheduleForm.description" :rows="4" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showScheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">{{ scheduleEditMode ? '保存修改' : '添加日程' }}</el-button>
      </template>
    </el-drawer>

    <!-- ============ 活动编辑抽屉 ============ -->
    <el-drawer v-model="showActivityDialog" :title="activityEditMode ? '编辑活动' : '新增校园活动'" size="520px" direction="rtl">
      <el-form label-position="top">
        <el-form-item label="活动标题">
          <el-input v-model="activityForm.title" maxlength="100" />
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
          <el-form-item label="活动地点">
            <el-input v-model="activityForm.location" maxlength="100" />
          </el-form-item>
          <el-form-item label="主办单位">
            <el-input v-model="activityForm.organizer" maxlength="100" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input type="datetime-local" v-model="activityForm.startTime" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input type="datetime-local" v-model="activityForm.endTime" />
          </el-form-item>
        </div>
        <el-form-item label="最大报名人数（0=不限）">
          <el-input-number v-model="activityForm.maxPeople" :min="0" :max="9999" style="width:100%" />
        </el-form-item>
        <el-form-item label="活动详情">
          <el-input type="textarea" v-model="activityForm.content" :rows="5" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showActivityDialog = false">取消</el-button>
        <el-button type="primary" @click="submitActivity">{{ activityEditMode ? '保存修改' : '创建活动' }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style lang="less" scoped>
@import '@/assets/design-system.less';

.admin-view {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 4px;
}

// ─── Header ───────────────────────────────────────────────
.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
  .header-left { display: flex; flex-direction: column; gap: 8px; }
  .admin-title { font-size: 26px; font-weight: 900; color: var(--el-text-color-primary); margin: 0; }
}

.admin-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #7C3AED, #4F46E5);
  color: #fff; border-radius: 20px; font-size: 12px; font-weight: 700;
}

.refresh-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px;
  border: 1.5px solid var(--el-border-color);
  border-radius: 8px; background: var(--el-bg-color);
  color: var(--el-text-color-regular); font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all .2s;
  &:hover { border-color: #DC2626; color: #DC2626; }
}

// ─── Stats Grid ───────────────────────────────────────────
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 18px 20px;
  display: flex; align-items: center; gap: 14px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  cursor: pointer;
  transition: all .25s;
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }

  .stat-icon {
    width: 46px; height: 46px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  }

  &[data-color="purple"] .stat-icon { background: #EDE9FE; color: #7C3AED; }
  &[data-color="green"]  .stat-icon { background: #D1FAE5; color: #059669; }
  &[data-color="red"]    .stat-icon { background: #FEE2E2; color: #DC2626; }
  &[data-color="yellow"] .stat-icon { background: #FEF3C7; color: #D97706; }
  &[data-color="orange"] .stat-icon { background: #FFEDD5; color: #EA580C; }

  .stat-num   { font-size: 26px; font-weight: 900; color: var(--el-text-color-primary); line-height: 1; }
  .stat-label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
}

// ─── Tabs ──────────────────────────────────────────────────
.admin-tabs {
  display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap;
}

.tab-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  border: 1.5px solid var(--el-border-color);
  border-radius: 50px;
  background: var(--el-bg-color); color: var(--el-text-color-regular);
  font-size: 13px; font-weight: 600; cursor: pointer; transition: all .2s;
  &:hover  { border-color: var(--el-color-primary); color: var(--el-color-primary); }
  &.active { background: var(--el-color-primary); border-color: var(--el-color-primary); color: #fff; }
}

// ─── Panel ─────────────────────────────────────────────────
.panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;
  .panel-title { font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); }
  .panel-hint  { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
}

// ─── Buttons ───────────────────────────────────────────────
.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border: none; border-radius: 8px;
  font-size: 13px; font-weight: 700; cursor: pointer; transition: all .2s;
  &.primary  { background: var(--el-color-primary); color: #fff; &:hover { filter: brightness(1.1); } }
  &.danger   { background: #FEE2E2; color: #DC2626; &:hover { background: #DC2626; color: #fff; } }
  &.warning  { background: #FEF3C7; color: #92400E; &:hover { background: #D97706; color: #fff; } }
  &.success  { background: #D1FAE5; color: #065F46; &:hover { background: #059669; color: #fff; } }
  &.small    { padding: 4px 10px; font-size: 12px; border-radius: 6px; }
}

.icon-btn {
  width: 32px; height: 32px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 8px; border: 1.5px solid; cursor: pointer; transition: all .2s;
  &.danger { border-color: #FCA5A5; background: #FEF2F2; color: #DC2626; &:hover { background: #DC2626; color: #fff; border-color: #DC2626; } }
  &.edit   { border-color: var(--el-border-color); background: var(--el-bg-color); color: var(--el-text-color-regular); &:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); } }
}

// 置顶/精华切换按钮 — 统一高度，解决错位
.toggle-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 4px;
  height: 28px; padding: 0 10px;
  border: 1.5px solid var(--el-border-color); border-radius: 6px;
  background: var(--el-bg-color); color: var(--el-text-color-regular);
  font-size: 12px; font-weight: 600; cursor: pointer; transition: all .2s; white-space: nowrap;
  &:hover { border-color: #6366f1; color: #6366f1; }
  &.active { background: #EDE9FE; border-color: #7C3AED; color: #7C3AED; }
}

// 行内操作组
.cell-actions {
  display: flex; align-items: center; gap: 6px;
}

.cell-btn {
  padding: 4px 10px; border: none; border-radius: 6px;
  font-size: 12px; font-weight: 700; cursor: pointer; transition: all .2s;
  &.warning { background: #FEF3C7; color: #92400E; &:hover { background: #D97706; color: #fff; } }
  &.success { background: #D1FAE5; color: #065F46; &:hover { background: #059669; color: #fff; } }
}

// ─── Table ─────────────────────────────────────────────────
.admin-table {
  :deep(th.el-table__cell) { background: var(--el-fill-color-lighter) !important; font-weight: 700; font-size: 13px; }
  :deep(td.el-table__cell) { font-size: 13px; border-bottom-color: var(--el-border-color-lighter); vertical-align: middle; }
}

.user-cell { display: flex; align-items: center; gap: 10px; .name { font-weight: 600; } }

.status-tag {
  font-size: 11px; font-weight: 700; padding: 1px 8px; border-radius: 4px;
  &.banned { color: #DC2626; background: #FEE2E2; }
  &.normal { color: #059669; background: #D1FAE5; }
}

.role-tag {
  padding: 2px 10px; border-radius: 20px; font-size: 12px; font-weight: 700;
  &.admin     { background: #FEF3C7; color: #92400E; }
  &.moderator { background: #DBEAFE; color: #1E40AF; }
}

.quota-badge {
  padding: 2px 8px; border-radius: 6px; font-size: 12px; font-weight: 700;
  background: var(--el-fill-color-lighter); color: var(--el-text-color-secondary);
}

.muted-text { font-size: 12px; color: var(--el-text-color-secondary); font-style: italic; }

// ─── Search ────────────────────────────────────────────────
.search-box {
  position: relative; width: 240px;
  .s-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--el-text-color-placeholder); }
  input {
    width: 100%; height: 34px; padding: 0 12px 0 32px;
    border: 1.5px solid var(--el-border-color); border-radius: 50px;
    background: var(--el-bg-color); font-size: 13px; color: var(--el-text-color-primary); outline: none;
    &:focus { border-color: var(--el-color-primary); }
  }
}

.pagination { padding: 16px; display: flex; justify-content: center; border-top: 1px solid var(--el-border-color-lighter); }

// ─── Notices ───────────────────────────────────────────────
.notice-list { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }

.notice-item {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px; border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color); position: relative;
  &.pinned { border-color: #7C3AED; background: #FAF5FF; }
}

.pin-badge {
  position: absolute; top: 10px; right: 130px;
  font-size: 11px; font-weight: 800; color: #7C3AED;
  background: #EDE9FE; padding: 2px 8px; border-radius: 4px;
}

.notice-main { flex: 1; min-width: 0; }
.notice-title   { font-size: 15px; font-weight: 800; color: var(--el-text-color-primary); margin-bottom: 6px; }
.notice-content { font-size: 13px; color: var(--el-text-color-regular); line-height: 1.6; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; }
.notice-meta    { display: flex; gap: 16px; font-size: 12px; color: var(--el-text-color-secondary); }

.notice-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

// ─── Schedule ──────────────────────────────────────────────
.schedule-list { padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; }

.schedule-item {
  display: flex; align-items: stretch; gap: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px; overflow: hidden;
  background: var(--el-bg-color); transition: all .2s;
  &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
}

.schedule-type-bar { width: 5px; flex-shrink: 0; }

.schedule-body { flex: 1; padding: 14px 16px; }

.schedule-main {
  display: flex; align-items: center; gap: 10px; margin-bottom: 6px;
}

.schedule-type-badge {
  padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 800;
}

.schedule-title { font-size: 15px; font-weight: 700; color: var(--el-text-color-primary); }

.schedule-date {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 600; color: var(--el-text-color-secondary);
}

.schedule-desc { font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 4px; }

.schedule-actions {
  display: flex; align-items: center; gap: 6px; padding: 0 14px; flex-shrink: 0;
}

// ─── Feedback ──────────────────────────────────────────────
.feedback-list { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }

.feedback-item {
  padding: 16px; border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  transition: all .2s;
  &.resolved { opacity: 0.65; }
}

.fb-top { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }

.type-tag {
  padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;
  &.bug        { background: #FEE2E2; color: #DC2626; }
  &.suggestion { background: #DBEAFE; color: #2563EB; }
  &.other      { background: #F3F4F6; color: #6B7280; }
}

.status-badge {
  padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;
  &.pending  { background: #FEF3C7; color: #92400E; }
  &.resolved { background: #D1FAE5; color: #065F46; }
}

.fb-user    { font-size: 12px; font-weight: 600; color: var(--el-text-color-primary); }
.fb-time    { font-size: 12px; color: var(--el-text-color-secondary); margin-left: auto; }
.fb-title   { font-size: 15px; font-weight: 800; color: var(--el-text-color-primary); margin-bottom: 6px; }
.fb-content { font-size: 13px; color: var(--el-text-color-regular); line-height: 1.7; margin-bottom: 8px; }
.fb-contact { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 8px; }
.fb-actions { display: flex; align-items: center; gap: 8px; }

// ─── Sensitive Words ────────────────────────────────────────
.word-add-box {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px; border-bottom: 1px solid var(--el-border-color-lighter);
  input {
    flex: 1; height: 38px; padding: 0 14px;
    border: 1.5px solid var(--el-border-color); border-radius: 8px;
    background: var(--el-bg-color); font-size: 14px; color: var(--el-text-color-primary); outline: none;
    &:focus { border-color: var(--el-color-primary); }
  }
}

.word-cloud { display: flex; flex-wrap: wrap; gap: 10px; padding: 20px; }

.word-tag {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 10px 5px 14px;
  background: #FEF2F2; border: 1.5px solid #FCA5A5;
  border-radius: 50px; font-size: 13px; font-weight: 700; color: #DC2626;
}

.word-del {
  display: flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 50%;
  background: #FECACA; border: none; color: #DC2626; cursor: pointer;
  &:hover { background: #DC2626; color: #fff; }
}

// ─── Dashboard ─────────────────────────────────────────────
.dashboard-section { margin-bottom: 20px; }

.dash-row {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px;
}

.dash-mini-card {
  padding: 20px; background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter); border-radius: 14px; text-align: center;
  .dmc-label { font-size: 12px; font-weight: 700; color: var(--el-text-color-secondary); margin-bottom: 6px; }
  .dmc-num   { font-size: 32px; font-weight: 900; color: var(--el-color-primary); }
}

.dash-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

.dash-card { padding: 20px; overflow: hidden; }

.bar-chart {
  display: flex; align-items: flex-end; gap: 12px; height: 140px; padding-top: 10px;
}

.bar-col {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;
  height: 100%; justify-content: flex-end;
}

.bar-value { font-size: 11px; font-weight: 700; color: var(--el-text-color-secondary); }
.bar-fill  { width: 100%; min-height: 4px; background: linear-gradient(180deg, #7C3AED, #4F46E5); border-radius: 6px 6px 0 0; transition: height .5s; }
.bar-label { font-size: 11px; color: var(--el-text-color-placeholder); font-weight: 600; }

.hot-list { display: flex; flex-direction: column; gap: 10px; }

.hot-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 12px;
  background: var(--el-fill-color-lighter); border-radius: 8px;
}

.hot-rank {
  width: 24px; height: 24px; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800;
  background: var(--el-fill-color); color: var(--el-text-color-secondary);
  &.top3 { background: linear-gradient(135deg, #7C3AED, #4F46E5); color: #fff; }
}

.hot-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-likes { font-size: 12px; font-weight: 700; color: var(--el-color-primary); white-space: nowrap; }

@media (max-width: 900px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dash-grid  { grid-template-columns: 1fr; }
}
</style>
