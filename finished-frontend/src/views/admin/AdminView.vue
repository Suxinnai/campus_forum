<script setup>
import { ref, reactive, onMounted } from 'vue'
import { get, post, put, del } from '@/net/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Users, FileText, MessageSquare, TrendingUp, Shield, Trash2, Search,
  Pin, PinOff, Bell, Calendar, Package, AlertTriangle, Plus, RotateCw, Edit2, Check, X
} from 'lucide-vue-next'

// ===================== 状态 =====================
const activeTab = ref('overview')
const stats = reactive({ users: 0, topics: 0, comments: 0, todayPosts: 0, sensitiveWords: 0 })

// 用户管理
const users = ref([])
const userPage = ref(1)
const userTotal = ref(0)
const userSearch = ref('')

// 帖子管理
const topics = ref([])
const topicPage = ref(1)
const topicTotal = ref(0)

// 公告管理
const notices = ref([])
const showNoticeDialog = ref(false)
const noticeForm = reactive({ title: '', content: '', publisher: '校方', isTop: 0 })

// 校历/活动管理
const activities = ref([])
const showActivityDialog = ref(false)
const editMode = ref(false)
const activityForm = reactive({
  id: null, title: '', content: '', location: '', organizer: '',
  startTime: '', endTime: '', maxPeople: 0
})

// 资源管理
const resources = ref([])
const resourcePage = ref(1)
const resourceTotal = ref(0)

// 敏感词管理
const sensitiveWords = ref([])
const newWord = ref('')

// ===================== 数据加载 =====================
function loadStats() {
  get('/api/admin/stats', data => {
    Object.assign(stats, data)
  })
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
  get('/api/admin/activities', data => { activities.value = data })
}

function loadResources() {
  get(`/api/admin/resources?page=${resourcePage.value - 1}`, data => {
    resources.value = data.records || data
    resourceTotal.value = data.total || data.length
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

// 公告管理
function submitNotice() {
  if (!noticeForm.title || !noticeForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  post('/api/admin/notice/add', noticeForm, () => {
    ElMessage.success('公告发布成功')
    showNoticeDialog.value = false
    Object.assign(noticeForm, { title: '', content: '', publisher: '校方', isTop: 0 })
    loadNotices()
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

// 校历管理
function openAddActivity() {
  editMode.value = false
  Object.assign(activityForm, { id: null, title: '', content: '', location: '', organizer: '', startTime: '', endTime: '', maxPeople: 0 })
  showActivityDialog.value = true
}

function openEditActivity(act) {
  editMode.value = true
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
  if (editMode.value) {
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
    users: () => { loadUsers() },
    topics: () => { loadTopics() },
    notices: loadNotices,
    calendar: loadActivities,
    resources: loadResources,
    sensitive: loadSensitiveWords
  }
  if (loaders[tab]) loaders[tab]()
}

onMounted(() => {
  loadStats()
  loadUsers()
  loadTopics()
  loadNotices()
  loadActivities()
  loadResources()
  loadSensitiveWords()
})
</script>

<template>
  <div class="admin-view ds-page">
    <!-- Page Header -->
    <div class="admin-header">
      <div class="header-left">
        <div class="admin-badge">
          <Shield :size="18" />
          <span>管理后台</span>
        </div>
        <h1 class="admin-title">全站控制中心</h1>
      </div>
      <el-button type="danger" plain size="small" round :icon="RotateCw" @click="loadStats">刷新数据</el-button>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card users" @click="activeTab = 'users'">
        <div class="stat-icon"><Users :size="22" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.users }}</div>
          <div class="stat-label">注册用户总数</div>
        </div>
      </div>
      <div class="stat-card topics" @click="activeTab = 'topics'">
        <div class="stat-icon"><FileText :size="22" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.topics }}</div>
          <div class="stat-label">帖子总量</div>
        </div>
      </div>
      <div class="stat-card comments">
        <div class="stat-icon"><MessageSquare :size="22" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.comments }}</div>
          <div class="stat-label">评论总量</div>
        </div>
      </div>
      <div class="stat-card today">
        <div class="stat-icon"><TrendingUp :size="22" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.todayPosts }}</div>
          <div class="stat-label">今日新帖</div>
        </div>
      </div>
      <div class="stat-card sensitive" @click="activeTab = 'sensitive'">
        <div class="stat-icon"><AlertTriangle :size="22" /></div>
        <div class="stat-body">
          <div class="stat-num">{{ stats.sensitiveWords }}</div>
          <div class="stat-label">敏感词数量</div>
        </div>
      </div>
    </div>

    <!-- Nav Tabs -->
    <div class="admin-tabs">
      <button v-for="tab in [
        { key: 'users', icon: Users, label: '用户管理' },
        { key: 'topics', icon: FileText, label: '帖子管理' },
        { key: 'notices', icon: Bell, label: '公告管理' },
        { key: 'calendar', icon: Calendar, label: '校历管理' },
        { key: 'resources', icon: Package, label: '资源审计' },
        { key: 'sensitive', icon: AlertTriangle, label: '敏感词库' }
      ]" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key; onTabChange(tab.key)">
        <component :is="tab.icon" :size="15" />
        {{ tab.label }}
      </button>
    </div>

    <!-- ============ 用户管理 ============ -->
    <div v-if="activeTab === 'users'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">用户列表</div>
        <div class="search-box">
          <Search :size="14" class="s-icon" />
          <input v-model="userSearch" placeholder="搜索用户名或邮箱..." @input="loadUsers" />
        </div>
      </div>
      <el-table :data="users" style="width:100%" class="admin-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar ? `http://localhost:8080/images${row.avatar}` : 'https://www.vexipui.com/qmhc.jpg'" />
              <span class="name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="200" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <span class="role-tag" :class="row.role">{{ row.role === 'admin' ? '管理员' : '普通用户' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatTime(row.registerTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <button class="del-btn" @click="deleteUser(row.id, row.username)" v-if="row.role !== 'admin'">
              <Trash2 :size="14" />
            </button>
            <span v-else class="protected">受保护</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="userPage" @current-change="loadUsers" :total="userTotal" :page-size="10" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 帖子管理 ============ -->
    <div v-if="activeTab === 'topics'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">帖子列表</div>
        <div class="panel-hint">置顶帖子将显示在社区首页最顶部</div>
      </div>
      <el-table :data="topics" style="width:100%" class="admin-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="帖子标题" prop="title" min-width="260" show-overflow-tooltip />
        <el-table-column label="发帖人ID" prop="uid" width="90" />
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.time) }}</template>
        </el-table-column>
        <el-table-column label="置顶" width="100" align="center">
          <template #default="{ row }">
            <button class="pin-btn" :class="{ pinned: row.top === 1 }" @click="toggleTopTopic(row)" :title="row.top === 1 ? '取消置顶' : '置顶'">
              <Pin v-if="row.top !== 1" :size="15" />
              <PinOff v-else :size="15" />
              {{ row.top === 1 ? '已置顶' : '置顶' }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="删除" width="90" align="center">
          <template #default="{ row }">
            <button class="del-btn" @click="deleteTopic(row.id)"><Trash2 :size="14" /></button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="topicPage" @current-change="loadTopics" :total="topicTotal" :page-size="10" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 公告管理 ============ -->
    <div v-if="activeTab === 'notices'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">全站公告</div>
        <button class="add-btn" @click="showNoticeDialog = true"><Plus :size="15" /> 发布新公告</button>
      </div>
      <div class="notice-list">
        <div v-for="n in notices" :key="n.id" class="notice-item" :class="{ top: n.isTop === 1 }">
          <div class="notice-top-tag" v-if="n.isTop === 1">顶置</div>
          <div class="notice-main">
            <div class="notice-title">{{ n.title }}</div>
            <div class="notice-content">{{ n.content }}</div>
            <div class="notice-meta">
              <span>{{ n.publisher }}</span>
              <span>{{ formatTime(n.createTime) }}</span>
            </div>
          </div>
          <button class="del-btn" @click="deleteNotice(n.id)"><Trash2 :size="14" /></button>
        </div>
        <el-empty v-if="!notices.length" description="暂无公告" />
      </div>
    </div>

    <!-- ============ 校历管理 ============ -->
    <div v-if="activeTab === 'calendar'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">校历与活动</div>
        <button class="add-btn" @click="openAddActivity"><Plus :size="15" /> 新增活动</button>
      </div>
      <el-table :data="activities" style="width:100%" class="admin-table">
        <el-table-column label="活动标题" prop="title" min-width="200" />
        <el-table-column label="地点" prop="location" width="120" show-overflow-tooltip />
        <el-table-column label="主办方" prop="organizer" width="120" />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="人数" width="90" align="center">
          <template #default="{ row }">{{ row.currentPeople }}/{{ row.maxPeople || '∞' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <button class="edit-btn" @click="openEditActivity(row)"><Edit2 :size="14" /></button>
            <button class="del-btn" style="margin-left:8px" @click="deleteActivity(row.id)"><Trash2 :size="14" /></button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ============ 资源审计 ============ -->
    <div v-if="activeTab === 'resources'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">资源文件审计</div>
        <div class="panel-hint">管理员可删除违规或失效的共享文件</div>
      </div>
      <el-table :data="resources" style="width:100%" class="admin-table">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="资源标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" prop="category" width="100" />
        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="下载次数" prop="downloadCount" width="100" align="center" />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <button class="del-btn" @click="deleteResource(row.id)"><Trash2 :size="14" /></button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" v-model:current-page="resourcePage" @current-change="loadResources" :total="resourceTotal" :page-size="15" hide-on-single-page />
      </div>
    </div>

    <!-- ============ 敏感词库 ============ -->
    <div v-if="activeTab === 'sensitive'" class="panel ds-card">
      <div class="panel-header">
        <div class="panel-title">敏感词库管理</div>
        <div class="panel-hint">精确匹配，发帖/评论时自动拦截含有这些词的内容</div>
      </div>
      <div class="word-add-box">
        <input v-model="newWord" placeholder="输入要添加的敏感词..." @keyup.enter="addWord" class="word-input" maxlength="50" />
        <button class="add-word-btn" @click="addWord"><Plus :size="16" /> 添加</button>
      </div>
      <div class="word-cloud" v-if="sensitiveWords.length">
        <div v-for="w in sensitiveWords" :key="w.id" class="word-tag">
          <span>{{ w.word }}</span>
          <button class="word-del" @click="deleteWord(w.id, w.word)"><X :size="12" /></button>
        </div>
      </div>
      <el-empty v-else description="词库为空，添加敏感词以开启内容过滤" style="margin-top:20px" />
    </div>

    <!-- ============ 公告发布抽屉 ============ -->
    <el-drawer v-model="showNoticeDialog" title="发布校园公告" size="480px" direction="rtl">
      <el-form label-position="top" style="padding: 0 4px">
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
        <el-button type="primary" @click="submitNotice">发布公告</el-button>
      </template>
    </el-drawer>

    <!-- ============ 活动编辑抽屉 ============ -->
    <el-drawer v-model="showActivityDialog" :title="editMode ? '编辑活动' : '新增活动'" size="520px" direction="rtl">
      <el-form label-position="top" style="padding: 0 4px">
        <el-form-item label="活动标题">
          <el-input v-model="activityForm.title" maxlength="100" />
        </el-form-item>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:16px">
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
        <el-form-item label="最大报名人数">
          <el-input-number v-model="activityForm.maxPeople" :min="0" :max="9999" placeholder="0=不限" style="width:100%" />
        </el-form-item>
        <el-form-item label="活动详情">
          <el-input type="textarea" v-model="activityForm.content" :rows="5" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showActivityDialog = false">取消</el-button>
        <el-button type="primary" @click="submitActivity">{{ editMode ? '保存修改' : '创建活动' }}</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style lang="less" scoped>
@import '@/assets/design-system.less';

.admin-view {
  max-width: 1200px;
  margin: 24px auto;
}

.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;

  .header-left { display: flex; flex-direction: column; gap: 8px; }
  .admin-title { font-size: 26px; font-weight: 900; color: var(--ds-text-1); margin: 0; }
}

.admin-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #7C3AED, #4F46E5);
  color: #fff;
  border-radius: 20px;
  font-size: 12px; font-weight: 700;
  width: fit-content;
}

// Stats Grid
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 20px 22px;
  display: flex; align-items: center; gap: 14px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
  
  &:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }

  .stat-icon {
    width: 48px; height: 48px;
    border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
  }

  &.users .stat-icon { background: #EDE9FE; color: #7C3AED; }
  &.topics .stat-icon { background: #D1FAE5; color: #059669; }
  &.comments .stat-icon { background: #FEE2E2; color: #DC2626; }
  &.today .stat-icon { background: #FEF3C7; color: #D97706; }
  &.sensitive .stat-icon { background: #FFEDD5; color: #EA580C; }

  .stat-num { font-size: 28px; font-weight: 900; color: var(--el-text-color-primary); line-height: 1; }
  .stat-label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
}

// Tabs
.admin-tabs {
  display: flex; gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px;
  border: 1.5px solid var(--el-border-color);
  border-radius: 50px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 13px; font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
  &.active { background: var(--el-color-primary); border-color: var(--el-color-primary); color: #fff; }
}

// Panel
.panel {
  padding: 0;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex; align-items: center; justify-content: space-between;
  
  .panel-title { font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); }
  .panel-hint { font-size: 12px; color: var(--el-text-color-secondary); }
}

.add-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: var(--el-color-primary);
  color: #fff; border: none; border-radius: 8px;
  font-size: 13px; font-weight: 700; cursor: pointer;
  transition: all 0.2s;
  &:hover { background: var(--el-color-primary-dark-2); transform: translateY(-1px); }
}

// Admin Table
.admin-table {
  :deep(th.el-table__cell) { background: var(--el-fill-color-lighter) !important; font-weight: 700; font-size: 13px; color: var(--el-text-color-secondary); }
  :deep(td.el-table__cell) { font-size: 13px; color: var(--el-text-color-primary); border-bottom-color: var(--el-border-color-lighter); }
}

.user-cell { display: flex; align-items: center; gap: 10px; .name { font-weight: 600; } }

.role-tag {
  padding: 2px 10px; border-radius: 20px; font-size: 12px; font-weight: 700;
  &.admin { background: #FEF3C7; color: #92400E; }
  &.user { background: #EDE9FE; color: #7C3AED; }
}

.del-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border: 1.5px solid #FCA5A5; border-radius: 8px;
  background: #FEF2F2; color: #DC2626;
  cursor: pointer; transition: all 0.2s; margin: 0 auto;
  &:hover { background: #DC2626; color: #fff; }
}

.edit-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border: 1.5px solid var(--el-border-color); border-radius: 8px;
  background: var(--el-bg-color); color: var(--el-text-color-regular);
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
}

.pin-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 10px;
  border: 1.5px solid var(--el-border-color); border-radius: 6px;
  background: var(--el-bg-color); color: var(--el-text-color-regular);
  font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s;
  
  &:hover { border-color: #6366f1; color: #6366f1; }
  &.pinned { background: #EDE9FE; border-color: #7C3AED; color: #7C3AED; }
}

.protected { font-size: 12px; color: var(--el-text-color-secondary); font-style: italic; }

.search-box {
  position: relative; width: 260px;
  .s-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--el-text-color-secondary); }
  input {
    width: 100%; height: 34px; padding: 0 12px 0 34px;
    border: 1.5px solid var(--el-border-color); border-radius: 50px;
    background: var(--el-bg-color); font-size: 13px; color: var(--el-text-color-primary); outline: none;
    &:focus { border-color: var(--el-color-primary); }
  }
}

.pagination { padding: 16px; display: flex; justify-content: center; border-top: 1px solid var(--el-border-color-lighter); }

// Notices
.notice-list { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }

.notice-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 16px; border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color); position: relative;
  
  &.top { border-color: #7C3AED; background: #FAF5FF; }

  .notice-top-tag {
    position: absolute; top: 8px; right: 48px;
    font-size: 11px; font-weight: 800; color: #7C3AED;
    background: #EDE9FE; padding: 1px 8px; border-radius: 4px;
  }
}

.notice-main { flex: 1; min-width: 0; }
.notice-title { font-size: 15px; font-weight: 800; color: var(--el-text-color-primary); margin-bottom: 6px; }
.notice-content { font-size: 13px; color: var(--el-text-color-regular); line-height: 1.6; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-clamp: 2; }
.notice-meta { display: flex; gap: 16px; font-size: 12px; color: var(--el-text-color-secondary); }

// Sensitive Words
.word-add-box {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px; border-bottom: 1px solid var(--el-border-color-lighter);
  
  .word-input {
    flex: 1; height: 38px; padding: 0 14px;
    border: 1.5px solid var(--el-border-color); border-radius: 8px;
    background: var(--el-bg-color); font-size: 14px; color: var(--el-text-color-primary); outline: none;
    &:focus { border-color: var(--el-color-primary); }
  }
  
  .add-word-btn {
    display: flex; align-items: center; gap: 6px;
    padding: 8px 18px; background: var(--el-color-primary); color: #fff;
    border: none; border-radius: 8px; font-size: 14px; font-weight: 700; cursor: pointer;
    transition: all 0.2s;
    &:hover { background: var(--el-color-primary-dark-2); }
  }
}

.word-cloud {
  display: flex; flex-wrap: wrap; gap: 10px;
  padding: 20px;
}

.word-tag {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 12px 6px 16px;
  background: #FEF2F2; border: 1.5px solid #FCA5A5;
  border-radius: 50px; font-size: 13px; font-weight: 700; color: #DC2626;
  
  .word-del {
    display: flex; align-items: center; justify-content: center;
    width: 18px; height: 18px; border-radius: 50%;
    background: #FECACA; border: none; color: #DC2626;
    cursor: pointer; transition: all 0.2s;
    &:hover { background: #DC2626; color: #fff; }
  }
}

@media (max-width: 900px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
