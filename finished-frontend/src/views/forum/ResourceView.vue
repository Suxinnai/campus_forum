<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {get, post, getToken, baseURL} from "@/net/api.js"
import {ElMessage as _msg} from 'element-plus'
import {useAppStore} from "@/stores/app-store.js"
import axios from 'axios'
import {ElMessage} from 'element-plus'
import {
  Upload, Download, FileText, BookOpen, StickyNote,
  FolderOpen, Search, Clock, User, HardDrive, TrendingUp, Star, Trash2
} from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'

const store = useAppStore()

const resourceList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const total = ref(0)
const categoryFilter = ref('')
const searchKeyword = ref('')

const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadRef = ref(null)
const uploadForm = reactive({
  title: '',
  category: '',
  description: ''
})
const fileList = ref([])

const categoryOptions = [
  {label: '全部资源', value: '', icon: FolderOpen, color: '#7C3AED'},
  {label: '课件资料', value: '课件', icon: BookOpen, color: '#3b82f6'},
  {label: '学术论文', value: '论文', icon: FileText, color: '#ec4899'},
  {label: '学霸笔记', value: '笔记', icon: StickyNote, color: '#f59e0b'},
  {label: '其他资源', value: '其他', icon: HardDrive, color: '#6b7280'},
]

const filteredList = computed(() => {
  if (!searchKeyword.value) return resourceList.value
  const kw = searchKeyword.value.toLowerCase()
  return resourceList.value.filter(r =>
    r.title.toLowerCase().includes(kw) ||
    (r.description && r.description.toLowerCase().includes(kw)) ||
    (r.fileName && r.fileName.toLowerCase().includes(kw))
  )
})

function loadResources() {
  loading.value = true
  let url = `/api/resource/list?page=${currentPage.value}`
  if (categoryFilter.value) {
    url += `&category=${categoryFilter.value}`
  }
  get(url, (data) => {
    resourceList.value = data.list || []
    total.value = data.total || 0
    loading.value = false
  })
}

onMounted(() => {
  loadResources()
})

function handlePageChange(page) {
  currentPage.value = page
  loadResources()
}

function handleCategoryChange(val) {
  categoryFilter.value = val
  currentPage.value = 1
  loadResources()
}

function openUploadDialog() {
  uploadDialogVisible.value = true
  uploadForm.title = ''
  uploadForm.category = ''
  uploadForm.description = ''
  fileList.value = []
}

function submitUpload() {
  if (!uploadForm.title.trim()) {
    ElMessage.warning('请填写资源标题')
    return
  }
  if (!fileList.value.length) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('file', fileList.value[0].raw)
  formData.append('title', uploadForm.title)
  formData.append('category', uploadForm.category || '其他')
  formData.append('description', uploadForm.description || '')

  axios.post(baseURL + '/api/resource/upload', formData, {
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    if (res.data.code === 200) {
      ElMessage.success(store.user.role === 'admin' ? '资源上传成功！' : '资源已提交审核')
      uploadDialogVisible.value = false
      currentPage.value = 1
      loadResources()
    } else {
      ElMessage.error(res.data.message || '上传失败')
    }
  }).catch(() => {
    ElMessage.error('上传失败，请重试')
  }).finally(() => {
    uploading.value = false
  })
}

function handleFileChange(file) {
  if (file.size > 1024 * 1024 * 50) {
    ElMessage.warning('文件大小不能超过 50MB')
    fileList.value = []
    return false
  }
  fileList.value = [file]
  if (!uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^/.]+$/, '')
  }
}

function handleFileRemove() {
  fileList.value = []
}

function downloadResource(id, fileName) {
  axios.get(`${baseURL}/api/resource/download/${id}`, {
    responseType: 'blob',
    headers: {
      'Authorization': `Bearer ${getToken()}`
    }
  }).then(res => {
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  }).catch(() => {
    ElMessage.error('下载失败，请重试')
  })
}

function deleteResource(item) {
  ElMessageBox.confirm(`确定要删除资源「${item.title}」吗？此操作不可恢复。`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    post(`/api/resource/delete?id=${item.id}`, null, () => {
      ElMessage.success('资源已删除')
      loadResources()
    })
  }).catch(() => {})
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTime(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + ' 天前'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getCategoryMeta(cat) {
  const found = categoryOptions.find(c => c.value === cat)
  return found || categoryOptions[4]
}

function getFileTypeIcon(fileName) {
  if (!fileName) return '📄'
  const ext = fileName.split('.').pop().toLowerCase()
  const map = {
    pdf: '📕', doc: '📘', docx: '📘', ppt: '📙', pptx: '📙',
    xls: '📗', xlsx: '📗', zip: '📦', rar: '📦', '7z': '📦',
    txt: '📄', md: '📝', jpg: '🖼️', png: '🖼️', mp4: '🎬',
    py: '🐍', java: '☕', js: '📜', c: '⚙️', cpp: '⚙️'
  }
  return map[ext] || '📄'
}

// 资源收藏
const collectMap = reactive({})

function checkCollected(rid) {
  get(`/api/resource/is-collected?rid=${rid}`, data => {
    collectMap[rid] = !!data
  })
}

function toggleCollect(item) {
  const newState = !collectMap[item.id]
  post(`/api/resource/collect?rid=${item.id}&state=${newState}`, null, () => {
    collectMap[item.id] = newState
    ElMessage.success(newState ? '已收藏' : '已取消收藏')
  })
}

// 加载资源后检查收藏状态
const originalLoadResources = loadResources
loadResources = function() {
  loading.value = true
  let url = `/api/resource/list?page=${currentPage.value}`
  if (categoryFilter.value) {
    url += `&category=${categoryFilter.value}`
  }
  get(url, (data) => {
    resourceList.value = data.list || []
    total.value = data.total || 0
    loading.value = false
    resourceList.value.forEach(r => checkCollected(r.id))
  })
}
</script>

<template>
  <div class="resource-page ds-page">
    <!-- Header -->
    <div class="ds-page-header">
      <h2 class="page-title">
        <span class="title-accent"></span>
        资源共享中心
      </h2>
      <button class="ds-btn-primary upload-trigger" @click="openUploadDialog">
        <Upload :size="16" />
        分享资源
      </button>
    </div>

    <!-- Stats Bar -->
    <div class="stats-bar">
      <div class="stat-chip">
        <FolderOpen :size="15" />
        <span>共 <b>{{ total }}</b> 份资源</span>
      </div>
      <div class="stat-chip">
        <TrendingUp :size="15" />
        <span>持续更新中</span>
      </div>
    </div>

    <!-- Filter + Search -->
    <div class="toolbar">
      <div class="category-tabs">
        <button
          v-for="opt in categoryOptions"
          :key="opt.value"
          class="cat-tab"
          :class="{ active: categoryFilter === opt.value }"
          @click="handleCategoryChange(opt.value)"
        >
          <component :is="opt.icon" :size="14" />
          {{ opt.label }}
        </button>
      </div>
      <div class="search-box">
        <Search :size="15" class="search-icon" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索资源..."
          class="search-input"
        />
      </div>
    </div>

    <!-- Resource Grid -->
    <div class="resource-grid" v-loading="loading" element-loading-text="加载中...">
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="resource-card ds-card"
      >
        <div class="card-top">
          <div class="file-icon-wrap" :style="{ background: getCategoryMeta(item.category).color + '15', color: getCategoryMeta(item.category).color }">
            <span class="file-emoji">{{ getFileTypeIcon(item.fileName) }}</span>
          </div>
          <div class="card-meta-right">
            <el-tag size="small" round effect="plain" :color="getCategoryMeta(item.category).color + '15'" :style="{ color: getCategoryMeta(item.category).color, borderColor: getCategoryMeta(item.category).color + '40' }">
              {{ item.category || '其他' }}
            </el-tag>
          </div>
        </div>

        <h3 class="card-title">{{ item.title }}</h3>
        <p class="card-desc" v-if="item.description">{{ item.description }}</p>
        <p class="card-filename" v-else>{{ item.fileName }}</p>

        <div class="card-info-row">
          <div class="info-item">
            <HardDrive :size="12" />
            <span>{{ formatFileSize(item.fileSize) }}</span>
          </div>
          <div class="info-item">
            <Download :size="12" />
            <span>{{ item.downloadCount || 0 }} 次下载</span>
          </div>
        </div>

        <div class="card-bottom">
          <div class="uploader">
            <el-avatar :size="22" :src="store.getAvatar(item.uploaderAvatar, item.uploaderName)" />
            <span class="uploader-name">{{ item.uploaderName }}</span>
            <span class="upload-time">
              <Clock :size="11" />
              {{ formatTime(item.createTime) }}
            </span>
          </div>
          <div class="action-btns">
            <button class="collect-btn" :class="{ collected: collectMap[item.id] }" @click="toggleCollect(item)" :title="collectMap[item.id] ? '取消收藏' : '收藏'">
              <Star :size="15" :fill="collectMap[item.id] ? 'currentColor' : 'none'" />
            </button>
            <button class="download-btn" @click="downloadResource(item.id, item.fileName)" title="下载资源">
              <Download :size="15" />
            </button>
            <button v-if="store.user.role === 'admin' || item.uploaderName === store.user.username"
                    class="delete-btn" @click="deleteResource(item)" title="删除资源">
              <Trash2 :size="15" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && !filteredList.length" class="empty-state ds-card">
      <div class="empty-icon">📂</div>
      <h3>暂无相关资源</h3>
      <p>该分类下还没有人分享资源，成为第一个贡献者吧！</p>
      <button class="ds-btn-primary" @click="openUploadDialog">
        <Upload :size="16" />
        上传第一份资源
      </button>
    </div>

    <!-- Pagination -->
    <div class="pagination-wrap" v-if="total > 10">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="currentPage"
        @current-change="handlePageChange"
        :page-size="10"
        :total="total"
      />
    </div>

    <!-- Upload Dialog -->
    <el-dialog
      v-model="uploadDialogVisible"
      width="580px"
      :close-on-click-modal="false"
      class="upload-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <div class="dialog-icon"><Upload :size="22" /></div>
          <div>
            <h3 class="dialog-title">分享学习资源</h3>
            <p class="dialog-subtitle">与校友共享知识，让学习不再孤单</p>
          </div>
        </div>
      </template>

      <el-form :model="uploadForm" label-position="top" class="upload-form">
        <el-form-item label="资源标题" required>
          <el-input
            v-model="uploadForm.title"
            placeholder="例：数据结构期末复习笔记"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="资源分类">
          <div class="category-selector">
            <label
              v-for="opt in categoryOptions.slice(1)"
              :key="opt.value"
              class="cat-option"
              :class="{ selected: uploadForm.category === opt.value }"
              @click="uploadForm.category = opt.value"
            >
              <component :is="opt.icon" :size="16" :style="{ color: opt.color }" />
              <span>{{ opt.label }}</span>
            </label>
          </div>
        </el-form-item>

        <el-form-item label="资源描述">
          <el-input
            type="textarea"
            v-model="uploadForm.description"
            placeholder="简要描述这份资源的内容、适用场景..."
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="none"
          />
        </el-form-item>

        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            drag
            action="#"
            class="upload-area"
          >
            <div class="upload-inner">
              <Upload :size="32" class="upload-icon" />
              <p class="upload-main-text">拖拽文件到此处，或 <em>点击选择</em></p>
              <p class="upload-hint">支持文档、压缩包等格式，单文件不超过 50MB</p>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button round @click="uploadDialogVisible = false">取消</el-button>
          <el-button round type="primary" @click="submitUpload" :loading="uploading">
            确认上传
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
@import '@/assets/design-system.less';

.resource-page {
  max-width: 1100px;
  margin: 28px auto;
  padding: 0 24px 48px;
}

// Stats
.stats-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--ds-primary-lighter);
  color: var(--ds-primary);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;

  b { font-weight: 800; }
}

// Toolbar
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cat-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--ds-border);
  border-radius: 50px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-muted);
  background: var(--ds-surface);
  cursor: pointer;
  transition: var(--ds-transition);

  &:hover {
    border-color: var(--ds-primary-light);
    color: var(--ds-primary);
    background: var(--ds-primary-lighter);
  }

  &.active {
    background: var(--ds-primary);
    color: #fff;
    border-color: var(--ds-primary);
    box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
  }
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  min-width: 200px;
  transition: var(--ds-transition);

  &:focus-within {
    border-color: var(--ds-primary-light);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.08);
  }

  .search-icon { color: var(--ds-muted); flex-shrink: 0; }

  .search-input {
    border: none;
    outline: none;
    background: transparent;
    font-size: 13px;
    color: var(--ds-text-1);
    width: 100%;
    &::placeholder { color: var(--ds-muted); }
  }
}

// Resource Grid
.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.resource-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  transition: var(--ds-transition);
  cursor: default;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--ds-shadow-md);
  }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.file-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  .file-emoji {
    font-size: 24px;
    line-height: 1;
  }
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ds-text-1);
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  font-size: 13px;
  color: var(--ds-muted);
  line-height: 1.6;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-filename {
  font-size: 12px;
  color: var(--ds-muted);
  font-family: 'Courier New', monospace;
  margin: 0 0 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.card-info-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--ds-border);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ds-muted);
  svg { color: var(--ds-primary-light); }
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.uploader {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  .uploader-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--ds-text-1);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }

  .upload-time {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    color: var(--ds-muted);
    white-space: nowrap;
  }
}

.action-btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.collect-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #fff7ed;
  color: #d97706;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--ds-transition);

  &:hover { background: #fde68a; transform: scale(1.1); }
  &.collected { background: #fef3c7; color: #b45309; }
}

.download-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--ds-primary-lighter);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--ds-transition);

  &:hover {
    background: var(--ds-primary);
    color: #fff;
    box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35);
    transform: scale(1.1);
  }
}

.delete-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #fef2f2;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--ds-transition);

  &:hover {
    background: #ef4444;
    color: #fff;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35);
    transform: scale(1.1);
  }
}

// Empty
.empty-state {
  text-align: center;
  padding: 64px 32px;
  margin-top: 20px;

  .empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }

  h3 {
    font-size: 20px;
    font-weight: 800;
    color: var(--ds-text-1);
    margin: 0 0 8px;
  }

  p {
    color: var(--ds-muted);
    font-size: 14px;
    margin: 0 0 28px;
  }
}

// Pagination
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

// Upload Dialog
.upload-dialog {
  :deep(.el-dialog) {
    border-radius: 20px;
    overflow: hidden;
  }
  :deep(.el-dialog__header) {
    padding: 28px 32px 0;
    margin: 0;
  }
  :deep(.el-dialog__body) {
    padding: 24px 32px;
  }
  :deep(.el-dialog__footer) {
    padding: 0 32px 28px;
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;

  .dialog-icon {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    background: var(--ds-primary-lighter);
    color: var(--ds-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .dialog-title {
    font-size: 20px;
    font-weight: 800;
    color: var(--ds-text-1);
    margin: 0 0 4px;
  }

  .dialog-subtitle {
    font-size: 13px;
    color: var(--ds-muted);
    margin: 0;
  }
}

.upload-form {
  :deep(.el-form-item__label) {
    font-weight: 700;
    color: var(--ds-text-1);
    font-size: 14px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 12px;
  }
}

.category-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  width: 100%;
}

.cat-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-muted);
  background: var(--ds-surface);
  cursor: pointer;
  transition: var(--ds-transition);

  &:hover {
    border-color: var(--ds-primary-light);
    background: var(--ds-primary-lighter);
  }

  &.selected {
    border-color: var(--ds-primary);
    background: var(--ds-primary-lighter);
    color: var(--ds-primary);
    box-shadow: 0 2px 8px rgba(124, 58, 237, 0.15);
  }
}

.upload-area {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    border-radius: 14px;
    background: var(--ds-surface-2);
    border: 2px dashed var(--ds-border);
    padding: 32px 20px;
    transition: var(--ds-transition);

    &:hover {
      border-color: var(--ds-primary);
      background: var(--ds-primary-lighter);
    }
  }
}

.upload-inner {
  text-align: center;

  .upload-icon {
    color: var(--ds-primary-light);
    margin-bottom: 12px;
  }

  .upload-main-text {
    font-size: 14px;
    color: var(--ds-text-1);
    margin: 0 0 6px;
    em { color: var(--ds-primary); font-style: normal; font-weight: 700; }
  }

  .upload-hint {
    font-size: 12px;
    color: var(--ds-muted);
    margin: 0;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.upload-trigger {
  flex-shrink: 0;
}
</style>
