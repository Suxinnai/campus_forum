<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { get, post, put, del } from '@/net/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app-store.js'
import {
  Users, FileText, MessageSquare, TrendingUp, Shield, Trash2, Search,
  Pin, PinOff, Bell, Calendar, CalendarDays, Package, AlertTriangle, Plus,
  RotateCw, Edit2, Check, X, PartyPopper, Star, Clock, MapPin,
  LayoutDashboard, ArrowLeft
} from 'lucide-vue-next'
import router from '@/router/index.js'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { createLineChartOption, createPieChartOption } from './admin-chart-options.js'
import { formatFileSize, formatTime, toDateInputValue } from './admin-formatters.js'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const store = useAppStore()
const myRole = computed(() => store.user.role)
const isAdmin = computed(() => myRole.value === 'admin')
const isContentAdmin = computed(() => myRole.value === 'content_admin')
const isModerator = computed(() => myRole.value === 'moderator')
const canManageTopics = computed(() => isAdmin.value || isContentAdmin.value || isModerator.value)

// ===================== 状态 =====================
const activeTab = ref('overview')
const stats = reactive({ users: 0, topics: 0, comments: 0, todayPosts: 0, sensitiveWords: 0, todayRegistered: 0, todayActive: 0, hotTopics: [], dailyPosts: [], categoryDistribution: [] })

const lineChartOption = computed(() => createLineChartOption(stats.dailyPosts || []))
const pieChartOption = computed(() => createPieChartOption(stats.categoryDistribution || []))

// 用户管理
const users = ref([])
const userPage = ref(1)
const userTotal = ref(0)
const userSearch = ref('')
const selectedUsers = ref([])
const userMap = reactive({}) // uid -> { username, avatar }

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
  id: null, title: '', description: '', eventDate: '', endDate: '', type: 'semester'
})

// 资源管理
const resources = ref([])
const resourcePage = ref(1)
const resourceTotal = ref(0)
const resourceStatusFilter = ref('all')
const resourceSearch = ref('')
const selectedResources = ref([])

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

// ===================== 侧边栏导航 =====================
const navItems = computed(() => {
  const items = [
    { key: 'overview',    icon: LayoutDashboard, label: '数据总览',  adminOnly: true },
    { key: 'users',       icon: Users,           label: '用户管理',  adminOnly: true },
    { key: 'topics',      icon: FileText,         label: '帖子管理',  adminOnly: false },
    { key: 'notices',     icon: Bell,             label: '公告管理',  adminOnly: true },
    { key: 'calendar',    icon: CalendarDays,     label: '校历日程',  adminOnly: true },
    { key: 'activities',  icon: PartyPopper,      label: '活动管理',  adminOnly: true },
    { key: 'resources',   icon: Package,          label: '资源审计',  adminOnly: true },
    { key: 'feedback',    icon: MessageSquare,    label: '用户反馈',  adminOnly: true },
    { key: 'sensitive',   icon: AlertTriangle,    label: '敏感词库',  adminOnly: true }
  ]
  return items.filter(t => !t.adminOnly || isAdmin.value)
})

const currentNavLabel = computed(() => {
  const item = navItems.value.find(n => n.key === activeTab.value)
  return item ? item.label : ''
})

function isTabAvailable(tab) {
  return navItems.value.some(item => item.key === tab)
}

watch(myRole, role => {
  if (!role || isTabAvailable(activeTab.value)) return
  switchTab(navItems.value[0]?.key || 'topics')
})

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
    // 缓存用户信息用于帖子管理等页面的用户名显示
    for (const u of users.value) {
      userMap[u.id] = { username: u.username, avatar: u.avatar }
    }
  })
}

// 加载全部用户缓存（用于帖子发帖人显示）
function loadAllUserCache() {
  get('/api/admin/users?page=0', data => {
    const list = data.records || data
    for (const u of list) {
      userMap[u.id] = { username: u.username, avatar: u.avatar }
    }
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
  let url = `/api/admin/resources?page=${resourcePage.value - 1}`
  if (resourceStatusFilter.value && resourceStatusFilter.value !== 'all') url += `&status=${resourceStatusFilter.value}`
  if (resourceSearch.value.trim()) url += `&keyword=${encodeURIComponent(resourceSearch.value.trim())}`
  get(url, data => {
    resources.value = data.records || data
    resourceTotal.value = data.total || data.length
    selectedResources.value = []
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

const auditStatusText = (status) => {
  if (status === 'pending') return '待审核'
  if (status === 'rejected') return '已驳回'
  return '已通过'
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
  const confirmMsg = noticeForm.isTop === 1
    ? '该公告将置顶展示在学生端公告栏，请确认是否发布？'
    : '发布后学生端将立即可见，请确认内容无误。'
  ElMessageBox.confirm(confirmMsg, '确认发布公告？', {
    confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'info'
  }).then(() => {
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
  }).catch(() => {})
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
  Object.assign(scheduleForm, { id: null, title: '', description: '', eventDate: '', endDate: '', type: 'semester' })
  showScheduleDialog.value = true
}

function openEditSchedule(s) {
  scheduleEditMode.value = true
  Object.assign(scheduleForm, {
    id: s.id,
    title: s.title,
    description: s.description || '',
    eventDate: toDateInputValue(s.eventDate),
    endDate: toDateInputValue(s.endDate),
    type: s.type || 'event'
  })
  showScheduleDialog.value = true
}

function submitSchedule() {
  if (!scheduleForm.title || !scheduleForm.eventDate) {
    ElMessage.warning('请填写日程标题和日期')
    return
  }
  if (scheduleForm.endDate && scheduleForm.endDate < scheduleForm.eventDate) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  const payload = {
    ...scheduleForm,
    eventDate: scheduleForm.eventDate,
    endDate: scheduleForm.endDate || null
  }
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

const scheduleTypeMap = { semester: { label: '学期', color: '#059669', bg: '#D1FAE5' }, exam: { label: '考试', color: '#DC2626', bg: '#FEE2E2' }, holiday: { label: '假期', color: '#D97706', bg: '#FEF3C7' }, event: { label: '其他', color: '#0ea5e9', bg: '#E0F2FE' } }
const getScheduleType = (type) => scheduleTypeMap[type] || scheduleTypeMap.event

// 资源管理
function auditResource(resource, status) {
  if (status === 'rejected') {
    ElMessageBox.prompt('填写驳回原因，便于后续追踪', '驳回资源', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：资源内容不完整或不符合共享规范',
      inputValue: resource.rejectReason || ''
    }).then(({ value }) => {
      submitResourceAudit(resource, status, value || '')
    }).catch(() => {})
    return
  }
  submitResourceAudit(resource, status)
}

function submitResourceAudit(resource, status, reason = '') {
  let url = `/api/admin/resource/status?id=${resource.id}&status=${status}`
  if (reason.trim()) url += `&reason=${encodeURIComponent(reason.trim())}`
  post(url, {}, () => {
    resource.status = status
    resource.rejectReason = status === 'rejected' ? (reason || '内容不符合资源共享规范') : null
    ElMessage.success(status === 'approved' ? '资源已通过审核' : status === 'pending' ? '资源已设为待审核' : '资源已驳回')
    loadResources()
  })
}

function selectedResourceIds() {
  return selectedResources.value.map(r => r.id).filter(Boolean)
}

function isResourceSelected(resource) {
  return selectedResources.value.some(item => item.id === resource.id)
}

function toggleResourceSelection(resource, checked) {
  if (checked) {
    if (!isResourceSelected(resource)) selectedResources.value = [...selectedResources.value, resource]
    return
  }
  selectedResources.value = selectedResources.value.filter(item => item.id !== resource.id)
}

function allVisibleResourcesSelected() {
  return resources.value.length > 0 && resources.value.every(resource => isResourceSelected(resource))
}

function toggleAllVisibleResources(checked) {
  selectedResources.value = checked ? [...resources.value] : []
}

function batchAuditResources(status) {
  const ids = selectedResourceIds()
  if (!ids.length) {
    ElMessage.warning('请先选择资源')
    return
  }
  if (status === 'rejected') {
    ElMessageBox.prompt(`将 ${ids.length} 个资源驳回，请填写原因`, '批量驳回资源', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：资源内容不完整或不符合共享规范'
    }).then(({ value }) => {
      submitBatchResourceAudit(status, value || '')
    }).catch(() => {})
    return
  }
  submitBatchResourceAudit(status)
}

function submitBatchResourceAudit(status, reason = '') {
  const ids = selectedResourceIds()
  if (!ids.length) {
    ElMessage.warning('请先选择资源')
    return
  }
  let url = `/api/admin/resource/batch-status?status=${status}`
  if (reason.trim()) url += `&reason=${encodeURIComponent(reason.trim())}`
  post(url, ids, () => {
    ElMessage.success(status === 'approved' ? '已批量通过资源' : status === 'pending' ? '已批量设为待审核' : '已批量驳回资源')
    selectedResources.value = []
    loadResources()
  })
}

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

function batchDeleteResources() {
  const ids = selectedResourceIds()
  if (!ids.length) {
    ElMessage.warning('请先选择资源')
    return
  }
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个资源文件吗？`, '批量删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    post('/api/admin/resource/batch-delete', ids, () => {
      ElMessage.success('已批量删除资源')
      selectedResources.value = []
      loadResources()
    })
  }).catch(() => {})
}

function resourceFileMeta(row) {
  if (row.fileName && row.description) return `${row.fileName} · ${row.description}`
  return row.fileName || row.description || '无文件描述'
}

function resourceBatchLabel() {
  return `已选 ${selectedResources.value.length} 项`
}

// 反馈管理
function resolveFeedback(id) {
  post(`/api/admin/feedback/resolve?id=${id}`, {}, () => {
    ElMessage.success('已标记为已处理')
    loadFeedbacks()
  })
}

function deleteFeedback(id) {
  ElMessageBox.confirm('删除后该反馈将无法恢复，确定继续吗？', '删除反馈', {
    confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error'
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

function switchTab(tab) {
  activeTab.value = tab
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
  loadAllUserCache()
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
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-title">
          <Shield :size="16" />
          <span>控制台</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <a
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeTab === item.key }"
          @click="switchTab(item.key)"
        >
          <component :is="item.icon" :size="16" class="nav-icon" />
          <span class="nav-label">{{ item.label }}</span>
        </a>
      </nav>
      <div class="sidebar-footer">
        <button type="button" class="nav-item back-link" @click="router.push('/home')">
          <ArrowLeft :size="16" class="nav-icon" />
          <span class="nav-label">返回前台</span>
        </button>
      </div>
    </aside>

    <!-- Content -->
    <div class="content-area">
      <el-scrollbar>
        <div class="content-inner">
          <!-- Page Header -->
          <div class="page-header">
            <div class="page-header-left">
              <h1 class="page-title">{{ currentNavLabel }}</h1>
              <span class="page-subtitle" v-if="activeTab === 'overview'">站点运行状态与数据概览</span>
              <span class="page-subtitle" v-else-if="activeTab === 'users'">管理注册用户、角色与权限</span>
              <span class="page-subtitle" v-else-if="activeTab === 'topics'">审核、置顶、精华帖子管理</span>
              <span class="page-subtitle" v-else-if="activeTab === 'notices'">发布与管理全站公告</span>
              <span class="page-subtitle" v-else-if="activeTab === 'calendar'">管理学期、考试、假期等校历事件</span>
              <span class="page-subtitle" v-else-if="activeTab === 'activities'">管理校园线下活动与赛事信息</span>
              <span class="page-subtitle" v-else-if="activeTab === 'resources'">审计与管理共享资源文件</span>
              <span class="page-subtitle" v-else-if="activeTab === 'feedback'">查看与处理用户反馈</span>
              <span class="page-subtitle" v-else-if="activeTab === 'sensitive'">管理内容过滤敏感词库</span>
            </div>
            <button class="header-action" @click="loadStats">
              <RotateCw :size="13" />
              <span>刷新</span>
            </button>
          </div>

          <!-- Stats Cards（仅超级管理员，数据总览） -->
          <div v-if="isAdmin && activeTab === 'overview'" class="stats-row">
            <div class="stat-card" @click="switchTab('users')">
              <div class="stat-icon purple"><Users :size="18" /></div>
              <div class="stat-body">
                <div class="stat-num">{{ stats.users }}</div>
                <div class="stat-label">注册用户</div>
              </div>
            </div>
            <div class="stat-card" @click="switchTab('topics')">
              <div class="stat-icon blue"><FileText :size="18" /></div>
              <div class="stat-body">
                <div class="stat-num">{{ stats.topics }}</div>
                <div class="stat-label">帖子总量</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon green"><MessageSquare :size="18" /></div>
              <div class="stat-body">
                <div class="stat-num">{{ stats.comments }}</div>
                <div class="stat-label">评论总量</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon orange"><TrendingUp :size="18" /></div>
              <div class="stat-body">
                <div class="stat-num">{{ stats.todayPosts }}</div>
                <div class="stat-label">今日新帖</div>
              </div>
            </div>
            <div class="stat-card" @click="switchTab('sensitive')">
              <div class="stat-icon red"><AlertTriangle :size="18" /></div>
              <div class="stat-body">
                <div class="stat-num">{{ stats.sensitiveWords }}</div>
                <div class="stat-label">敏感词</div>
              </div>
            </div>
          </div>

          <!-- 今日数据概览 -->
          <div v-if="isAdmin && activeTab === 'overview'" class="overview-grid">
            <div class="overview-card mini-stats">
              <div class="mini-stat">
                <span class="ms-num">{{ stats.todayRegistered }}</span>
                <span class="ms-label">今日注册</span>
              </div>
              <div class="mini-stat">
                <span class="ms-num">{{ stats.todayActive }}</span>
                <span class="ms-label">今日活跃</span>
              </div>
              <div class="mini-stat">
                <span class="ms-num">{{ stats.todayPosts }}</span>
                <span class="ms-label">今日新帖</span>
              </div>
            </div>

            <div class="charts-row">
              <div class="overview-card line-chart-card">
                <div class="oc-title">近 7 日发帖趋势</div>
                <v-chart class="echart-box" :option="lineChartOption" autoresize />
              </div>
              <div class="overview-card pie-chart-card">
                <div class="oc-title">发帖内容分类</div>
                <v-chart class="echart-box" :option="pieChartOption" autoresize />
              </div>
            </div>

            <div class="overview-card hot-card">
              <div class="oc-title">热门帖子 Top 5</div>
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

          <!-- ============ 用户管理 ============ -->
          <div v-if="isAdmin && activeTab === 'users'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <div class="search-box">
                  <Search :size="14" class="s-icon" />
                  <input v-model="userSearch" placeholder="搜索用户名或邮箱..." @input="loadUsers" />
                </div>
              </div>
              <div class="toolbar-right">
                <template v-if="selectedUsers.length">
                  <button class="tbtn danger" @click="batchDeleteUsers">
                    <Trash2 :size="13" /> 删除 ({{ selectedUsers.length }})
                  </button>
                  <button class="tbtn warn" @click="batchBanUsers(true)">封禁</button>
                  <button class="tbtn ok" @click="batchBanUsers(false)">解封</button>
                </template>
              </div>
            </div>
            <el-table :data="users" style="width:100%" class="admin-table" @selection-change="(val) => selectedUsers = val">
              <el-table-column type="selection" width="45" :selectable="(row) => row.role !== 'admin'" />
              <el-table-column label="ID" prop="id" width="60" />
              <el-table-column label="用户" min-width="180">
                <template #default="{ row }">
                  <div class="user-cell">
                    <el-avatar :size="30" :src="store.getAvatar(row.avatar, row.username)" />
                    <div class="user-info-cell">
                      <span class="name">{{ row.username }}</span>
                      <span v-if="row.banned === 1" class="banned-tag">封禁</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="邮箱" prop="email" min-width="180" />
              <el-table-column label="角色" width="220">
                <template #default="{ row }">
                  <span v-if="row.role === 'admin'" class="role-badge admin">超级管理员</span>
                  <div v-else style="display:flex;align-items:center;gap:6px">
                    <el-select :model-value="row.role" size="small" @change="(val) => changeRole(row, val)" style="width:120px">
                      <el-option label="普通用户" value="user" />
                      <el-option label="内容管理员" value="content_admin" />
                      <el-option label="版主" value="moderator" />
                    </el-select>
                    <span v-if="row.role === 'moderator' && row.moderatorType" class="role-badge moderator" :title="'负责版块ID: ' + row.moderatorType">
                      {{ topicTypes.find(t => t.id === row.moderatorType)?.name || '版块' + row.moderatorType }}
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="注册时间" width="150">
                <template #default="{ row }">{{ formatTime(row.registerTime || row.create_time) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <div v-if="row.role !== 'admin'" class="cell-actions">
                    <button class="tbtn sm" :class="row.banned === 1 ? 'ok' : 'warn'" @click="toggleBan(row)">
                      {{ row.banned === 1 ? '解封' : '封禁' }}
                    </button>
                    <button class="tbtn-icon danger" @click="deleteUser(row.id, row.username)"><Trash2 :size="13" /></button>
                  </div>
                  <span v-else class="muted">受保护</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="table-pagination">
              <el-pagination background layout="prev, pager, next" v-model:current-page="userPage" @current-change="loadUsers" :total="userTotal" :page-size="10" hide-on-single-page />
            </div>
          </div>

          <!-- ============ 帖子管理 ============ -->
          <div v-if="activeTab === 'topics'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">待审核和驳回帖子不会在社区前台展示</span>
              </div>
              <div class="toolbar-right">
                <button v-if="selectedTopics.length" class="tbtn danger" @click="batchDeleteTopics">
                  <Trash2 :size="13" /> 删除 ({{ selectedTopics.length }})
                </button>
              </div>
            </div>
            <el-table :data="topics" style="width:100%" class="admin-table topic-table" @selection-change="(val) => selectedTopics = val" row-class-name="topic-row">
              <el-table-column type="selection" width="45" />
              <el-table-column label="ID" width="55" align="center">
                <template #default="{ row }">
                  <span class="cell-id">{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column label="帖子标题" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="cell-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="发帖人" width="130">
                <template #default="{ row }">
                  <div class="cell-author">
                    <el-avatar :size="24" :src="store.getAvatar(userMap[row.uid]?.avatar, userMap[row.uid]?.username || ('U' + row.uid))" />
                    <span class="cell-author-name">{{ userMap[row.uid]?.username || ('用户' + row.uid) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="发布时间" width="145">
                <template #default="{ row }">
                  <span class="cell-time">{{ formatTime(row.time) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="审核" width="90" align="center">
                <template #default="{ row }">
                  <span class="audit-badge" :class="row.status || 'approved'">
                    {{ auditStatusText(row.status) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="审核操作" width="142" align="center">
                <template #default="{ row }">
                  <div class="audit-actions">
                    <button v-if="row.status !== 'approved'" class="tbtn-icon ok" @click="auditTopic(row, 'approved')" title="通过">
                      <Check :size="13" />
                    </button>
                    <button v-if="row.status !== 'pending'" class="tbtn-icon warn" @click="auditTopic(row, 'pending')" title="设为待审核">
                      <RotateCw :size="13" />
                    </button>
                    <button v-if="row.status !== 'rejected'" class="tbtn-icon danger" @click="auditTopic(row, 'rejected')" title="驳回">
                      <X :size="13" />
                    </button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="精华" width="60" align="center">
                <template #default="{ row }">
                  <button class="icon-toggle" :class="{ active: row.featured === 1 }" @click="toggleFeatured(row)" title="精华">
                    <Star :size="15" />
                  </button>
                </template>
              </el-table-column>
              <el-table-column v-if="isAdmin" label="置顶" width="60" align="center">
                <template #default="{ row }">
                  <button class="icon-toggle" :class="{ active: row.top === 1 }" @click="toggleTopTopic(row)" title="置顶">
                    <Pin :size="15" />
                  </button>
                </template>
              </el-table-column>
              <el-table-column label="删除" width="86" align="center">
                <template #default="{ row }">
                  <button class="tbtn sm danger row-delete-btn" @click="deleteTopic(row.id)" title="删除帖子">
                    <Trash2 :size="12" />
                    删除
                  </button>
                </template>
              </el-table-column>
            </el-table>
            <div class="table-pagination">
              <el-pagination background layout="prev, pager, next" v-model:current-page="topicPage" @current-change="loadTopics" :total="topicTotal" :page-size="10" hide-on-single-page />
            </div>
          </div>

          <!-- ============ 公告管理 ============ -->
          <div v-if="isAdmin && activeTab === 'notices'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">全站公告会在用户首页展示</span>
              </div>
              <div class="toolbar-right">
                <button class="tbtn primary" @click="openAddNotice"><Plus :size="13" /> 发布公告</button>
              </div>
            </div>
            <div class="notice-list">
              <div v-for="n in notices" :key="n.id" class="notice-item" :class="{ pinned: n.isTop === 1 }">
                <div class="notice-left">
                  <div class="notice-title-row">
                    <div class="notice-title">{{ n.title }}</div>
                    <div v-if="n.isTop === 1" class="pin-badge">
                      <Pin :size="11" />
                      <span>已置顶</span>
                    </div>
                  </div>
                  <div class="notice-content">{{ n.content }}</div>
                  <div class="notice-meta">
                    <span class="meta-item"><Shield :size="12" /> {{ n.publisher }}</span>
                    <span class="meta-item"><Clock :size="12" /> {{ formatTime(n.createTime) }}</span>
                  </div>
                </div>
                <div class="notice-actions">
                  <button class="notice-action-btn" :class="{ pinned: n.isTop === 1 }" @click="toggleNoticeTop(n)" :title="n.isTop === 1 ? '取消置顶' : '置顶'">
                    <Pin :size="15" />
                  </button>
                  <button class="notice-action-btn" @click="openEditNotice(n)" title="编辑">
                    <Edit2 :size="15" />
                  </button>
                  <button class="notice-action-btn del" @click="deleteNotice(n.id)" title="删除">
                    <Trash2 :size="15" />
                  </button>
                </div>
              </div>
              <el-empty v-if="!notices.length" description="暂无公告" style="padding:40px 0" />
            </div>
          </div>

          <!-- ============ 校历日程管理 ============ -->
          <div v-if="isAdmin && activeTab === 'calendar'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">管理学期、考试、假期等校历事件</span>
              </div>
              <div class="toolbar-right">
                <button class="tbtn primary" @click="openAddSchedule"><Plus :size="13" /> 新增日程</button>
              </div>
            </div>
            <div class="schedule-list">
              <div v-for="s in schedules" :key="s.id" class="schedule-item">
                <div class="schedule-bar" :style="{ background: getScheduleType(s.type).color }"></div>
                <div class="schedule-body">
                  <div class="schedule-top">
                    <span class="schedule-badge" :style="{ background: getScheduleType(s.type).bg, color: getScheduleType(s.type).color }">
                      {{ getScheduleType(s.type).label }}
                    </span>
                    <span class="schedule-title">{{ s.title }}</span>
                  </div>
                  <div class="schedule-date">
                    <Calendar :size="12" />
                    {{ toDateInputValue(s.eventDate) }}
                    <template v-if="s.endDate"> → {{ toDateInputValue(s.endDate) }}</template>
                  </div>
                  <div v-if="s.description" class="schedule-desc">{{ s.description }}</div>
                </div>
                <div class="schedule-actions">
                  <button class="tbtn-icon edit" @click="openEditSchedule(s)"><Edit2 :size="13" /></button>
                  <button class="tbtn-icon danger" @click="deleteSchedule(s.id)"><Trash2 :size="13" /></button>
                </div>
              </div>
              <el-empty v-if="!schedules.length" description="暂无校历日程" style="padding:40px 0" />
            </div>
          </div>

          <!-- ============ 活动管理 ============ -->
          <div v-if="isAdmin && activeTab === 'activities'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">管理校园线下活动与赛事信息</span>
              </div>
              <div class="toolbar-right">
                <button class="tbtn primary" @click="openAddActivity"><Plus :size="13" /> 新增活动</button>
              </div>
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
              <el-table-column label="开始时间" width="155">
                <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
              </el-table-column>
              <el-table-column label="结束时间" width="155">
                <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
              </el-table-column>
              <el-table-column label="报名" width="85" align="center">
                <template #default="{ row }">
                  <span class="quota-badge">{{ row.currentPeople || 0 }}/{{ row.maxPeople || '∞' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <div class="cell-actions" style="justify-content:center">
                    <button class="tbtn-icon edit" @click="openEditActivity(row)"><Edit2 :size="13" /></button>
                    <button class="tbtn-icon danger" @click="deleteActivity(row.id)"><Trash2 :size="13" /></button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!activities.length" description="暂无校园活动" style="padding:40px 0" />
          </div>

          <!-- ============ 资源审计 ============ -->
          <div v-if="isAdmin && activeTab === 'resources'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <div class="search-box">
                  <Search :size="14" class="s-icon" />
                  <input v-model="resourceSearch" placeholder="搜索标题、文件名或描述..." @keyup.enter="resourcePage = 1; loadResources()" />
                </div>
                <el-select v-model="resourceStatusFilter" size="small" style="width:128px" placeholder="全部状态" @change="resourcePage = 1; loadResources()">
                  <el-option label="全部状态" value="all" />
                  <el-option label="待审核" value="pending" />
                  <el-option label="已通过" value="approved" />
                  <el-option label="已驳回" value="rejected" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <div v-if="selectedResources.length" class="batch-actions">
                  <span class="selection-count">{{ resourceBatchLabel() }}</span>
                  <button class="tbtn sm ok" @click="batchAuditResources('approved')">
                    <Check :size="12" /> 通过
                  </button>
                  <button class="tbtn sm warn" @click="batchAuditResources('pending')">
                    <RotateCw :size="12" /> 待审
                  </button>
                  <button class="tbtn sm danger" @click="batchAuditResources('rejected')">
                    <X :size="12" /> 驳回
                  </button>
                  <button class="tbtn sm danger" @click="batchDeleteResources">
                    <Trash2 :size="12" /> 删除
                  </button>
                </div>
                <button class="tbtn sm" @click="resourcePage = 1; loadResources()">
                  <RotateCw :size="12" /> 刷新
                </button>
              </div>
            </div>
            <div v-if="resources.length" class="resource-audit-list">
              <div class="resource-list-head">
                <label class="resource-select-all">
                  <input type="checkbox" :checked="allVisibleResourcesSelected()" @change="toggleAllVisibleResources($event.target.checked)" />
                  <span>本页全选</span>
                </label>
                <span>共 {{ resourceTotal }} 个资源</span>
              </div>
              <div v-for="row in resources" :key="row.id" class="resource-audit-item" :class="{ selected: isResourceSelected(row) }">
                <label class="resource-check">
                  <input type="checkbox" :checked="isResourceSelected(row)" @change="toggleResourceSelection(row, $event.target.checked)" />
                </label>
                <div class="resource-card-main">
                  <div class="resource-card-top">
                    <div class="resource-title-group">
                      <span class="resource-id">#{{ row.id }}</span>
                      <h3>{{ row.title }}</h3>
                    </div>
                  </div>
                  <div class="resource-file-line">{{ resourceFileMeta(row) }}</div>
                  <div class="resource-meta">
                    <span>上传者：{{ row.uploaderName || '-' }}</span>
                    <span>{{ row.category || '未分类' }}</span>
                    <span>{{ formatFileSize(row.fileSize) }}</span>
                    <span>{{ row.downloadCount || 0 }} 次下载</span>
                    <span>{{ formatTime(row.createTime) }}</span>
                  </div>
                  <div v-if="row.rejectReason" class="resource-reject-note">{{ row.rejectReason }}</div>
                </div>
                <div class="resource-card-side">
                  <span class="audit-badge" :class="row.status || 'approved'">{{ auditStatusText(row.status) }}</span>
                  <div class="resource-actions">
                    <button v-if="row.status !== 'approved'" class="tbtn sm ok audit-text-btn" @click="auditResource(row, 'approved')" title="通过">
                      <Check :size="12" />
                      通过
                    </button>
                    <button v-if="row.status !== 'pending'" class="tbtn sm warn audit-text-btn" @click="auditResource(row, 'pending')" title="设为待审核">
                      <RotateCw :size="12" />
                      待审
                    </button>
                    <button v-if="row.status !== 'rejected'" class="tbtn sm danger audit-text-btn" @click="auditResource(row, 'rejected')" title="驳回">
                      <X :size="12" />
                      驳回
                    </button>
                    <button class="tbtn sm danger audit-text-btn" @click="deleteResource(row.id)" title="删除资源">
                      <Trash2 :size="12" />
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无资源" style="padding:40px 0" />
            <div class="table-pagination">
              <el-pagination background layout="prev, pager, next" v-model:current-page="resourcePage" @current-change="loadResources" :total="resourceTotal" :page-size="15" hide-on-single-page />
            </div>
          </div>

          <!-- ============ 用户反馈 ============ -->
          <div v-if="isAdmin && activeTab === 'feedback'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">来自用户的建议、Bug 反馈和问题报告</span>
              </div>
            </div>
            <div v-if="feedbackLoading" style="padding:60px;text-align:center;color:var(--el-text-color-secondary)">
              加载中...
            </div>
            <div class="feedback-list" v-else-if="feedbacks.length">
              <div v-for="fb in feedbacks" :key="fb.id" class="feedback-item" :class="{ resolved: fb.status === 'resolved' }">
                <div class="fb-header">
                  <span class="fb-type" :class="fb.type">
                    {{ fb.type === 'bug' ? 'Bug' : fb.type === 'suggestion' ? '建议' : '其它' }}
                  </span>
                  <span class="fb-status" :class="fb.status">
                    {{ fb.status === 'resolved' ? '已处理' : '待处理' }}
                  </span>
                  <span class="fb-user">{{ fb.username }}</span>
                  <span class="fb-time">{{ formatTime(fb.createTime) }}</span>
                </div>
                <div class="fb-title">{{ fb.title }}</div>
                <div class="fb-content">{{ fb.content }}</div>
                <div v-if="fb.contact" class="fb-contact">联系方式：{{ fb.contact }}</div>
                <div class="fb-actions">
                  <button v-if="fb.status !== 'resolved'" class="tbtn sm ok" @click="resolveFeedback(fb.id)">
                    <Check :size="12" /> 标记已处理
                  </button>
                  <button class="tbtn sm danger feedback-delete-btn" @click="deleteFeedback(fb.id)" title="删除反馈">
                    <Trash2 :size="12" /> 删除
                  </button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无用户反馈" style="padding:40px 0" />
          </div>

          <!-- ============ 敏感词库 ============ -->
          <div v-if="isAdmin && activeTab === 'sensitive'" class="panel">
            <div class="panel-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-hint">维护内容过滤词条</span>
              </div>
            </div>
            <div class="word-input-row">
              <input v-model="newWord" placeholder="输入要添加的敏感词..." @keyup.enter="addWord" maxlength="50" class="word-input" />
              <button class="tbtn primary" @click="addWord"><Plus :size="13" /> 添加</button>
            </div>
            <div class="word-list" v-if="sensitiveWords.length">
              <div v-for="w in sensitiveWords" :key="w.id" class="word-chip">
                <span>{{ w.word }}</span>
                <button class="word-x" @click="deleteWord(w.id, w.word)"><X :size="10" /></button>
              </div>
            </div>
            <el-empty v-else description="词库为空，添加敏感词以开启内容过滤" style="margin:20px 0" />
          </div>

        </div>
      </el-scrollbar>
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

    <!-- ============ 公告发布/编辑弹窗 ============ -->
    <el-dialog v-model="showNoticeDialog" width="880px" class="admin-modal" :show-close="true" :close-on-click-modal="false" destroy-on-close align-center>
      <template #header>
        <div class="modal-header">
          <h2>{{ editNoticeMode ? '编辑公告' : '发布校园公告' }}</h2>
          <p>请认真填写公告细节，确保信息准确。公告将展示在学生端首页及公告栏。</p>
        </div>
      </template>
      <div class="modal-body">
        <el-form label-position="top" class="modal-form">
          <el-form-item label="公告标题 *">
            <el-input v-model="noticeForm.title" maxlength="100" placeholder="请输入公告标题，如：关于图书馆开放时间调整的通知" />
          </el-form-item>
          <el-form-item label="发布单位">
            <el-select v-model="noticeForm.publisher" style="width:100%">
              <el-option label="校方" value="校方" />
              <el-option label="教务处" value="教务处" />
              <el-option label="学生处" value="学生处" />
              <el-option label="后勤处" value="后勤处" />
              <el-option label="信息中心" value="信息中心" />
              <el-option label="学生会" value="学生会" />
              <el-option label="团委" value="团委" />
            </el-select>
          </el-form-item>
          <el-form-item label="公告级别">
            <div class="mode-options">
              <button type="button" class="mode-card" :class="{ active: noticeForm.isTop === 0 }" @click="noticeForm.isTop = 0">
                <Clock :size="15" />
                <span><strong>普通发布</strong><small>按发布时间自然排序</small></span>
              </button>
              <button type="button" class="mode-card" :class="{ active: noticeForm.isTop === 1 }" @click="noticeForm.isTop = 1">
                <Pin :size="15" />
                <span><strong>首页置顶</strong><small>始终显示在列表最上方</small></span>
              </button>
            </div>
          </el-form-item>
          <el-form-item label="正文内容 *">
            <div class="textarea-wrap">
              <el-input type="textarea" v-model="noticeForm.content" maxlength="2000" placeholder="请输入公告正文，建议包含时间、地点、对象及注意事项" :rows="4" />
              <div class="textarea-footer">
                <span>建议分段书写，让公告更容易阅读</span>
                <span class="word-count">{{ (noticeForm.content || '').length }}/2000</span>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="footer-actions">
          <button class="btn secondary" @click="showNoticeDialog = false">取消</button>
          <button class="btn primary" :disabled="!noticeForm.title || !noticeForm.content" @click="submitNotice">{{ editNoticeMode ? '保存修改' : '立即发布' }}</button>
        </div>
      </template>
    </el-dialog>

    <!-- ============ 校历日程编辑弹窗 ============ -->
    <el-dialog v-model="showScheduleDialog" width="680px" class="admin-modal" :show-close="true" :close-on-click-modal="false" destroy-on-close align-center>
      <template #header>
        <div class="modal-header">
          <h2>{{ scheduleEditMode ? '编辑日程' : '新增校历日程' }}</h2>
          <p>校历日程将展示在学生端日历模块，请确保日期准确、标题清晰。</p>
        </div>
      </template>
      <div class="modal-body">
        <el-form label-position="top" class="modal-form">
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
          <el-form-item label="类型预览">
            <div class="type-preview" :style="{ '--preview-color': getScheduleType(scheduleForm.type).color, '--preview-bg': getScheduleType(scheduleForm.type).bg }">
              <span></span>
              {{ getScheduleType(scheduleForm.type).label }}
            </div>
          </el-form-item>
          <div class="modal-grid two-cols">
          <el-form-item label="开始日期">
            <el-input type="date" v-model="scheduleForm.eventDate" />
          </el-form-item>
          <el-form-item label="结束日期（可选）">
            <el-input type="date" v-model="scheduleForm.endDate" />
          </el-form-item>
          </div>
          <el-form-item label="描述（可选）">
            <el-input type="textarea" v-model="scheduleForm.description" :rows="4" maxlength="500" placeholder="补充日程说明，例如涉及院系、地点或注意事项。" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="footer-actions">
          <button class="btn secondary" @click="showScheduleDialog = false">取消</button>
          <button class="btn primary" :disabled="!scheduleForm.title || !scheduleForm.eventDate" @click="submitSchedule">{{ scheduleEditMode ? '保存修改' : '添加日程' }}</button>
        </div>
      </template>
    </el-dialog>

    <!-- ============ 活动编辑弹窗 ============ -->
    <el-dialog v-model="showActivityDialog" width="720px" class="admin-modal" :show-close="true" :close-on-click-modal="false" destroy-on-close align-center>
      <template #header>
        <div class="modal-header">
          <h2>{{ activityEditMode ? '编辑活动' : '新增校园活动' }}</h2>
          <p>活动信息将展示在活动中心，请完善地点、时间、主办方和报名限制。</p>
        </div>
      </template>
      <div class="modal-body">
        <el-form label-position="top" class="modal-form">
          <el-form-item label="活动标题">
            <el-input v-model="activityForm.title" maxlength="100" placeholder="例如：AI 创新工作坊 / 校园摄影大赛" />
          </el-form-item>
          <div class="modal-grid two-cols">
          <el-form-item label="活动地点">
            <el-input v-model="activityForm.location" maxlength="100" placeholder="如：图书馆报告厅" />
          </el-form-item>
          <el-form-item label="主办单位">
            <el-input v-model="activityForm.organizer" maxlength="100" placeholder="如：学生会 / 计算机学院" />
          </el-form-item>
          </div>
          <div class="modal-grid two-cols">
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
            <el-input type="textarea" v-model="activityForm.content" :rows="5" maxlength="1000" placeholder="描述活动亮点、报名要求、流程安排等。" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="footer-actions">
          <button class="btn secondary" @click="showActivityDialog = false">取消</button>
          <button class="btn primary" @click="submitActivity">{{ activityEditMode ? '保存修改' : '创建活动' }}</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.admin-layout {
  display: flex;
  height: 100%;
  background: #f8fafc;
  font-family: "Microsoft YaHei UI", "PingFang SC", "HarmonyOS Sans SC", -apple-system, BlinkMacSystemFont, sans-serif;

  html.dark & { background: #0f111a; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.content-inner > * {
  animation: fadeInUp 0.5s ease-out forwards;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 4px 0 24px rgba(0,0,0,0.02);
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;

  html.dark & {
    background: rgba(20, 20, 25, 0.8);
    border-right: 1px solid rgba(255, 255, 255, 0.05);
  }
}

.sidebar-header {
  padding: 28px 24px 20px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  background: linear-gradient(135deg, #0ea5e9, #14b8a6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sidebar-nav {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none;
  position: relative;
  overflow: hidden;

  .nav-icon { flex-shrink: 0; transition: color 0.3s; }

  &:hover {
    background: rgba(14, 165, 233, 0.05);
    color: #0ea5e9;
    transform: translateX(4px);
  }

  &.active {
    background: linear-gradient(135deg, #0ea5e9, #38bdf8);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);

    html.dark & {
      box-shadow: 0 4px 12px rgba(14, 165, 233, 0.2);
    }

    .nav-icon { color: #ffffff; }
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(0,0,0,0.04);
  html.dark & { border-top-color: rgba(255,255,255,0.05); }
}

.back-link {
  border: 1px solid transparent !important;
  color: var(--el-text-color-secondary) !important;
  &:hover { 
    border-color: #22c55e !important;
    color: var(--el-text-color-primary) !important; 
    background: rgba(0,0,0,0.03);
    html.dark & { background: rgba(255,255,255,0.05); }
  }
}

.content-area {
  flex: 1;
  min-width: 0;
  height: 100%;
}

.content-inner {
  padding: 40px 48px 60px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.header-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  background: white;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  html.dark & { background: #1e1e24; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }

  &:hover { 
    color: #0ea5e9; 
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    transform: translateY(-2px);
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  html.dark & {
    background: rgba(30, 30, 36, 0.7);
    border-color: rgba(255, 255, 255, 0.05);
  }

  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: 0 12px 30px rgba(0,0,0,0.08);
    border-color: rgba(14, 165, 233, 0.2);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;

  &.purple { background: linear-gradient(135deg, #e0f2fe, #bae6fd); color: #0ea5e9; html.dark & { background: rgba(14,165,233,0.2); } }
  &.blue   { background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0d9488; html.dark & { background: rgba(13,148,136,0.2); } }
  &.green  { background: linear-gradient(135deg, #dcfce7, #bbf7d0); color: #16a34a; html.dark & { background: rgba(22,163,74,0.2); } }
  &.orange { background: linear-gradient(135deg, #ffedd5, #fed7aa); color: #ea580c; html.dark & { background: rgba(234,88,12,0.2); } }
  &.red    { background: linear-gradient(135deg, #fee2e2, #fecaca); color: #dc2626; html.dark & { background: rgba(220,38,38,0.2); } }
}

.stat-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  line-height: 1;
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

// ─── Overview Grid ───────────────────────────────────────
.overview-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.overview-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
  border: 1px solid rgba(0,0,0,0.04);
  transition: all 0.3s;

  html.dark & { background: #1e1e24; border-color: rgba(255,255,255,0.06); }

  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
}

.mini-stats {
  display: flex;
  gap: 0;
  background: white;
  border: 1px solid rgba(0,0,0,0.04);
  box-shadow: 0 1px 6px rgba(0,0,0,0.03);

  html.dark & { background: #1e1e24; border-color: rgba(255,255,255,0.06); }

  .mini-stat {
    flex: 1;
    text-align: center;
    padding: 28px 0;
    position: relative;

    & + .mini-stat::before {
      content: '';
      position: absolute;
      left: 0;
      top: 25%;
      height: 50%;
      width: 1px;
      background: rgba(0,0,0,0.06);
      html.dark & { background: rgba(255,255,255,0.1); }
    }
  }

  .ms-num {
    display: block;
    font-size: 36px;
    font-weight: 800;
    color: #0ea5e9;
    line-height: 1;
    margin-bottom: 6px;
  }

  .ms-label {
    display: block;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    font-weight: 600;
  }
}

.oc-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin-bottom: 20px;
}

// ECharts container
.echart-box {
  width: 100%;
  height: 220px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: var(--el-bg-color-page);
  border-radius: 12px;
  transition: all 0.3s;
  border: 1px solid transparent;

  &:hover { 
    background: white;
    border-color: rgba(14, 165, 233, 0.2);
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    transform: translateX(4px);
    
    html.dark & { background: #2a2a32; }
  }
}

.hot-rank {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  background: var(--el-fill-color-darker);
  color: var(--el-text-color-regular);
  flex-shrink: 0;

  &.top3 { 
    background: linear-gradient(135deg, #0ea5e9, #14b8a6); 
    color: #fff;
    box-shadow: 0 2px 8px rgba(14,165,233,0.3);
  }
}

.hot-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-likes {
  font-size: 13px;
  font-weight: 800;
  color: #0ea5e9;
  background: rgba(14,165,233,0.1);
  padding: 4px 10px;
  border-radius: 20px;
}

.panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.03);
  border: 1px solid rgba(0,0,0,0.02);
  overflow: hidden;
  transition: all 0.3s;
  animation: fadeInUp 0.5s ease-out forwards;

  html.dark & { background: #1e1e24; border-color: rgba(255,255,255,0.05); }
}

.panel-toolbar {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  html.dark & { border-bottom-color: rgba(255,255,255,0.05); }
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.tbtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  &.primary { 
    background: linear-gradient(135deg, #0ea5e9, #0284c7);
    color: #fff; 
    box-shadow: 0 4px 12px rgba(14,165,233,0.3);
    &:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(14,165,233,0.4); } 
  }
  &.danger  { 
    background: #fef2f2; color: #dc2626; 
    &:hover { background: #dc2626; color: #fff; transform: translateY(-2px); }
    html.dark & { background: rgba(220,38,38,0.15); &:hover { background: #dc2626; } }
  }
  &.warn    { 
    background: #fffbeb; color: #d97706; 
    &:hover { background: #d97706; color: #fff; transform: translateY(-2px); }
    html.dark & { background: rgba(217,119,6,0.15); color: #fbbf24; }
  }
  &.ok      { 
    background: #f0fdf4; color: #166534; 
    &:hover { background: #059669; color: #fff; transform: translateY(-2px); }
    html.dark & { background: rgba(5,150,105,0.15); color: #34d399; }
  }
  &.sm      { padding: 6px 12px; font-size: 12px; border-radius: 8px; }
}

.tbtn-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.3s;
  background: white;

  html.dark & { background: #2a2a32; }

  &.danger { 
    border-color: #fecaca; color: #dc2626; 
    &:hover { background: #dc2626; color: #fff; border-color: #dc2626; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(220,38,38,0.2); }
    html.dark & { border-color: rgba(220,38,38,0.4); background: rgba(220,38,38,0.1); }
  }
  &.ok {
    border-color: #bbf7d0; color: #15803d;
    &:hover { background: #059669; color: #fff; border-color: #059669; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(5,150,105,0.18); }
    html.dark & { border-color: rgba(5,150,105,0.4); background: rgba(5,150,105,0.1); }
  }
  &.warn {
    border-color: #fde68a; color: #b45309;
    &:hover { background: #d97706; color: #fff; border-color: #d97706; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(217,119,6,0.18); }
    html.dark & { border-color: rgba(217,119,6,0.4); background: rgba(217,119,6,0.1); }
  }
  &.edit { 
    border-color: var(--el-border-color); color: var(--el-text-color-secondary); 
    &:hover { border-color: #0ea5e9; color: #0ea5e9; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(14,165,233,0.1); } 
  }
}

.chip-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;

  &:hover { border-color: #0ea5e9; color: #0ea5e9; }
  &.on { 
    background: rgba(14,165,233,0.1); 
    border-color: #0ea5e9; 
    color: #0284c7;
    html.dark & { background: rgba(14,165,233,0.2); color: #38bdf8; }
  }
  &.sm { height: 26px; padding: 0 10px; font-size: 11px; }
}

.cell-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.batch-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.selection-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
  padding: 5px 8px;
  border-radius: 8px;
  background: #f8fafc;

  html.dark & { background: #2a2a32; }
}

.row-delete-btn {
  padding: 6px 10px !important;
}

.resource-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: nowrap;
  flex-shrink: 0;
  width: 190px;
}

.audit-text-btn {
  min-width: 54px;
  height: 30px !important;
  padding: 0 8px !important;
  border-radius: 8px !important;
}

.muted { font-size: 13px; color: var(--el-text-color-placeholder); font-style: italic; }

.admin-table {
  --el-table-border-color: rgba(0,0,0,0.04);
  --el-table-header-bg-color: #f8fafc;
  
  html.dark & {
    --el-table-border-color: rgba(255,255,255,0.05);
    --el-table-header-bg-color: #2a2a32;
  }

  :deep(th.el-table__cell) { 
    font-weight: 800; 
    font-size: 13px; 
    color: var(--el-text-color-primary); 
    padding: 14px 0;
  }
  
  :deep(td.el-table__cell) { 
    font-size: 14px; 
    padding: 14px 0;
    vertical-align: middle; 
  }

  // Row hover for topic table
  :deep(.topic-row:hover > td.el-table__cell) {
    background: #f8fafc !important;
    html.dark & { background: #252530 !important; }
  }
}

// ─── Topic Table Cells ──────────────────────────────────
.cell-id {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}

.cell-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  html.dark & { color: #e2e8f0; }
}

.resource-audit-list {
  padding: 14px 18px 18px;
}

.resource-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 6px 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
}

.resource-select-all,
.resource-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;

  input {
    width: 15px;
    height: 15px;
    margin: 0;
    cursor: pointer;
    accent-color: #0ea5e9;
  }
}

.resource-audit-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 310px;
  align-items: center;
  column-gap: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 12px;
  background: var(--el-bg-color);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;

  & + .resource-audit-item {
    margin-top: 10px;
  }

  &:hover {
    border-color: rgba(14, 165, 233, 0.32);
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  }

  &.selected {
    border-color: rgba(14, 165, 233, 0.45);
    background: rgba(14, 165, 233, 0.04);
  }
}

.resource-card-main {
  flex: 1;
  min-width: 0;
}

.resource-card-top {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.resource-card-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  min-width: 0;

  .audit-badge {
    min-width: 74px;
    text-align: center;
  }
}

.resource-title-group {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;

  h3 {
    margin: 0;
    font-size: 15px;
    line-height: 1.35;
    font-weight: 800;
    color: var(--el-text-color-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.resource-id {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 800;
  color: #94a3b8;
}

.resource-file-line {
  max-width: 680px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 14px;
  min-width: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.resource-reject-note {
  margin-top: 9px;
  padding: 7px 10px;
  border-radius: 8px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .resource-audit-item {
    grid-template-columns: 24px minmax(0, 1fr);
    align-items: flex-start;
  }

  .resource-card-side {
    grid-column: 2;
    justify-content: space-between;
    margin-top: 12px;
  }
}

.cell-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cell-author-name {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.cell-time {
  font-size: 13px;
  font-weight: 400;
  color: #94a3b8;
}

.audit-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;

  &.approved { background: #f0fdf4; color: #15803d; html.dark & { background: rgba(22,163,74,0.15); color: #4ade80; } }
  &.pending  { background: #fffbeb; color: #b45309; html.dark & { background: rgba(217,119,6,0.15); color: #fbbf24; } }
  &.rejected { background: #fef2f2; color: #dc2626; html.dark & { background: rgba(220,38,38,0.15); color: #f87171; } }
}

.icon-toggle {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #cbd5e1;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { color: #64748b; background: #f1f5f9; html.dark & { background: #2a2a32; } }

  &.active {
    color: #0ea5e9;
    background: #f0f9ff;
    html.dark & { background: rgba(14,165,233,0.15); }
  }

  &.delete {
    color: #cbd5e1;
    &:hover { color: #ef4444; background: #fef2f2; html.dark & { background: rgba(239,68,68,0.15); } }
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .name { font-weight: 700; color: var(--el-text-color-primary); }
}

.banned-tag {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 6px;
  color: #dc2626;
  background: #fef2f2;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 800;

  &.admin     { background: #fef3c7; color: #b45309; }
  &.moderator { background: #e0f2fe; color: #0284c7; }
}

.quota-badge {
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
  background: #f3f4f6;
  color: #4b5563;
  html.dark & { background: #374151; color: #d1d5db; }
}

.table-pagination {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgba(0,0,0,0.04);
  html.dark & { border-top-color: rgba(255,255,255,0.05); }
}

.search-box {
  position: relative;
  width: 260px;

  .s-icon {
    position: absolute;
    left: 14px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--el-text-color-placeholder);
  }

  input {
    width: 100%;
    height: 40px;
    padding: 0 16px 0 38px;
    border: 1px solid var(--el-border-color);
    border-radius: 10px;
    background: var(--el-bg-color);
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    outline: none;
    transition: all 0.3s;

    &:focus { 
      border-color: #0ea5e9; 
      box-shadow: 0 0 0 4px rgba(14,165,233,0.1); 
    }
    &::placeholder { color: var(--el-text-color-placeholder); }
  }
}

.notice-list { padding: 16px 24px; display: flex; flex-direction: column; gap: 16px; }

.notice-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,0.04);
  background: white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  transition: all 0.3s;

  html.dark & { background: #1e1e24; border-color: rgba(255,255,255,0.06); }

  &:hover {
    box-shadow: 0 6px 20px rgba(0,0,0,0.06);
    .notice-action-btn { opacity: 1; }
  }

  &.pinned { 
    background: rgba(14,165,233,0.03);
    border-color: rgba(14,165,233,0.15);
    html.dark & { background: rgba(14,165,233,0.05); }
  }
}

.notice-left { flex: 1; min-width: 0; }

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  html.dark & { color: #e2e8f0; }
}

.pin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  color: #0ea5e9;
  background: #e0f2fe;
  flex-shrink: 0;
  html.dark & { background: rgba(14,165,233,0.2); }
}

.notice-content {
  font-size: 14px;
  color: #64748b;
  line-height: 1.65;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  html.dark & { color: #94a3b8; }
}

.notice-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.notice-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.notice-action-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
  opacity: 0.5;

  &:hover { color: #475569; background: #f1f5f9; html.dark & { background: #2a2a32; color: #e2e8f0; } }
  &.pinned { color: #0ea5e9; opacity: 1; &:hover { background: #f0f9ff; } }
  &.del:hover { color: #ef4444; background: #fef2f2; html.dark & { background: rgba(239,68,68,0.15); } }
}

.schedule-list { padding: 16px 24px; display: flex; flex-direction: column; gap: 12px; }

.schedule-item {
  display: flex;
  align-items: stretch;
  border: 1px solid rgba(0,0,0,0.04);
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-bg-color);
  transition: all 0.3s;

  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.04); transform: translateX(4px); }
}
.schedule-bar { width: 6px; flex-shrink: 0; }
.schedule-body { flex: 1; padding: 16px; }
.schedule-top { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.schedule-badge { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 800; }
.schedule-title { font-size: 15px; font-weight: 800; color: var(--el-text-color-primary); }
.schedule-date { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--el-text-color-secondary); }
.schedule-desc { font-size: 13px; color: var(--el-text-color-regular); margin-top: 6px; line-height: 1.5; }
.schedule-actions { display: flex; align-items: center; gap: 8px; padding: 0 16px; }

// Feedback
.feedback-list { padding: 16px 24px; display: flex; flex-direction: column; gap: 16px; }
.feedback-item { padding: 20px; border-radius: 16px; border: 1px solid rgba(0,0,0,0.04); background: var(--el-bg-color); transition: all 0.3s; &:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.04); } &.resolved { opacity: 0.6; filter: grayscale(50%); } }
.fb-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.fb-type, .fb-status { padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 800; }
.fb-type.bug { background: #fef2f2; color: #dc2626; } .fb-type.suggestion { background: #eff6ff; color: #2563eb; } .fb-type.other { background: #f3f4f6; color: #4b5563; }
.fb-status.pending { background: #fffbeb; color: #b45309; } .fb-status.resolved { background: #ecfdf5; color: #059669; }
.fb-user { font-size: 13px; font-weight: 700; color: var(--el-text-color-primary); }
.fb-time { font-size: 13px; color: var(--el-text-color-placeholder); margin-left: auto; font-weight: 500; }
.fb-title { font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); margin-bottom: 8px; }
.fb-content { font-size: 14px; color: var(--el-text-color-regular); line-height: 1.7; margin-bottom: 10px; }
.fb-contact { font-size: 13px; font-weight: 500; color: var(--el-text-color-secondary); background: rgba(0,0,0,0.02); padding: 8px 12px; border-radius: 8px; }
.fb-actions { display: flex; align-items: center; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.feedback-delete-btn { border: 1px solid #fecaca; box-shadow: none; }

// Sensitive Words
.word-input-row { display: flex; align-items: center; gap: 12px; padding: 20px 24px; border-bottom: 1px solid rgba(0,0,0,0.04); }
.word-input { flex: 1; height: 40px; padding: 0 16px; border: 1px solid var(--el-border-color); border-radius: 10px; font-size: 14px; font-weight: 500; outline: none; transition: all 0.3s; &:focus { border-color: #0ea5e9; box-shadow: 0 0 0 4px rgba(14,165,233,0.1); } }
.word-list { display: flex; flex-wrap: wrap; gap: 10px; padding: 24px; }
.word-chip { display: flex; align-items: center; gap: 8px; padding: 7px 10px 7px 14px; border-radius: 20px; font-size: 14px; font-weight: 700; background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; box-shadow: 0 2px 8px rgba(220,38,38,0.08); }
.word-x { display: flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; background: #fecaca; border: none; color: #dc2626; cursor: pointer; transition: all 0.2s; &:hover { background: #dc2626; color: #fff; transform: scale(1.1); } }

// ─── Admin Modals — Professional, Clean, Functional ─────
.admin-modal {
  :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
    background: #ffffff;
    border: 1px solid #e4e7ed;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
  }

  :deep(.el-dialog__header) { padding: 0; margin: 0; }
  :deep(.el-dialog__body) { padding: 0; }
  :deep(.el-dialog__footer) {
    padding: 20px 48px 26px;
    background: #ffffff;
    border-top: 1px solid #f0f2f5;
  }

  :deep(.el-dialog__headerbtn) {
    top: 22px;
    right: 22px;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    transition: background 0.15s ease;
    &:hover { background: #f5f7fa; }
  }

  :deep(.el-form-item) { margin-bottom: 22px; }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
    color: #374151;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.4;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner),
  :deep(.el-input-number .el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 40px;
    border-radius: 10px;
    border: 1px solid #e0e3e8;
    background: #ffffff;
    box-shadow: none;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-textarea__inner:hover),
  :deep(.el-select__wrapper:hover) { border-color: #c8ccd4; }

  :deep(.el-input__wrapper.is-focus),
  :deep(.el-textarea__inner:focus),
  :deep(.el-select__wrapper.is-focused) {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
  }

  html.dark & {
    :deep(.el-dialog) {
      background: #1e1e24;
      border-color: rgba(255,255,255,0.08);
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
    }
    :deep(.el-dialog__footer) {
      background: #1e1e24;
      border-top-color: rgba(255,255,255,0.06);
    }
    :deep(.el-dialog__headerbtn:hover) { background: rgba(255,255,255,0.08); }
    :deep(.el-form-item__label) { color: #e2e8f0; }
    :deep(.el-input__wrapper),
    :deep(.el-textarea__inner),
    :deep(.el-input-number .el-input__wrapper),
    :deep(.el-select__wrapper) {
      background: rgba(15, 17, 26, 0.7);
      border-color: rgba(255,255,255,0.1);
    }
  }
}

.modal-header {
  padding: 24px 68px 16px 48px;
  border-bottom: 1px solid #f0f2f5;

  h2 {
    margin: 0 0 6px;
    color: #1e293b;
    font-size: 20px;
    font-weight: 800;
    letter-spacing: -0.02em;
    line-height: 1.3;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 13px;
    line-height: 1.55;
  }

  html.dark & {
    border-bottom-color: rgba(255,255,255,0.06);
    h2 { color: #f1f5f9; }
    p { color: #9ca3af; }
  }
}

.modal-body {
  padding: 26px 48px 4px;
}

.modal-grid {
  display: grid;
  gap: 18px;
  &.two-cols { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
}

// ─── Compressed level cards (72px target height) ─────
.mode-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mode-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  min-height: 56px;
  border-radius: 10px;
  border: 1.5px solid #e4e7ed;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;

  svg { flex-shrink: 0; color: #9ca3af; }

  strong { display: block; color: #374151; font-size: 13px; font-weight: 700; line-height: 1.3; }
  small { display: block; color: #9ca3af; font-size: 12px; line-height: 1.35; font-weight: 500; margin-top: 2px; }

  &:hover { border-color: #b0b7c3; }

  &.active {
    border-color: #3b82f6;
    background: #f8faff;
    svg { color: #3b82f6; }
    strong { color: #1d4ed8; }
  }

  html.dark & {
    background: rgba(255,255,255,0.04);
    border-color: rgba(255,255,255,0.12);
    strong { color: #e2e8f0; }
    svg { color: #6b7280; }
    small { color: #6b7280; }
    &.active {
      background: rgba(59,130,246,0.1);
      border-color: #3b82f6;
      svg { color: #60a5fa; }
      strong { color: #60a5fa; }
    }
  }
}

// ─── Full-width textarea ─────
.textarea-wrap {
  width: 100%;
  border-radius: 10px;
  border: 1px solid #e0e3e8;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.12); }

  :deep(.el-textarea) {
    width: 100%;
  }

  :deep(.el-textarea__inner) {
    width: 100%;
    border: none;
    border-radius: 10px 10px 0 0;
    box-shadow: none;
    resize: vertical;
    min-height: 220px;
  }
}

.textarea-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px 9px;
  border-top: 1px solid #f0f2f5;
  color: #9ca3af;
  font-size: 12px;
  font-weight: 500;

  .word-count {
    font-variant-numeric: tabular-nums;
    color: #c4cad4;
  }
}

.type-preview {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  color: var(--preview-color);
  background: var(--preview-bg);
  font-size: 13px;
  font-weight: 700;
  span { width: 8px; height: 8px; border-radius: 50%; background: var(--preview-color); }
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 20px;
  border-radius: 10px;
  border: none;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;

  &.secondary {
    color: #374151;
    background: #ffffff;
    border: 1px solid transparent;
    &:hover { background: #f9fafb; border-color: #22c55e; }
  }

  &.primary {
    color: #ffffff;
    background: #3b82f6;
    &:hover:not(:disabled) { background: #2563eb; box-shadow: 0 4px 12px rgba(59,130,246,0.25); }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  html.dark &.secondary {
    color: #e2e8f0;
    background: rgba(255,255,255,0.06);
    border-color: rgba(255,255,255,0.12);
    &:hover { background: rgba(255,255,255,0.1); }
  }
  html.dark &.primary {
    &:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
  }
}

// ─── Responsive ──────────────────────────────────────────
@media (max-width: 900px) {
  .charts-row { grid-template-columns: 1fr; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .overview-grid { display: flex; flex-direction: column; }
  .sidebar { width: 64px; }
  .nav-label { display: none; }
  .sidebar-title span { display: none; }
  .content-inner { padding: 24px 20px 40px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .admin-modal :deep(.el-dialog) { width: calc(100vw - 28px) !important; }
  .modal-header { padding: 20px 52px 14px 24px; }
  .modal-body { padding: 22px 24px 4px; }
  .mode-options { grid-template-columns: 1fr; }
  .modal-grid.two-cols { grid-template-columns: 1fr; gap: 0; }
  .admin-modal :deep(.el-dialog__footer) { padding: 16px 24px 22px; }
}
</style>
