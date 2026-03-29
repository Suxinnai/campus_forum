<script setup>
import {ref, reactive, onMounted} from 'vue'
import {get, post, getToken} from "@/net/api.js"
import {ElMessage} from "element-plus"
import {Upload, Download, Folder, Document, DocumentCopy, Ticket, More} from "@element-plus/icons-vue"
import axios from "axios"

const defaultUrl = "http://localhost:8080"

const resourceList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const categoryFilter = ref('')

const uploadForm = reactive({
  title: '',
  category: '',
  description: ''
})

const uploadDialogVisible = ref(false)
const fileList = ref([])

const categoryOptions = [
  {label: '全部', value: ''},
  {label: '课件', value: '课件'},
  {label: '论文', value: '论文'},
  {label: '笔记', value: '笔记'},
  {label: '其他', value: '其他'},
]

function loadResources() {
  loading.value = true
  let url = `/api/resource/list?page=${currentPage.value}`
  if (categoryFilter.value) {
    url += `&category=${categoryFilter.value}`
  }
  get(url, (data) => {
    resourceList.value = data
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

function handleCategoryChange() {
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

function handleUpload(param) {
  const formData = new FormData()
  formData.append('file', param.file)
  formData.append('title', uploadForm.title)
  formData.append('category', uploadForm.category || '其他')
  formData.append('description', uploadForm.description || '')

  axios.post(defaultUrl + '/api/resource/upload', formData, {
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    if (res.data.code === 200) {
      ElMessage.success('资源上传成功！')
      uploadDialogVisible.value = false
      loadResources()
    } else {
      ElMessage.error(res.data.message)
    }
  }).catch(err => {
    ElMessage.error('上传失败，请重试')
  })
}

function downloadResource(id, fileName) {
  const link = document.createElement('a')
  link.href = `${defaultUrl}/api/resource/download/${id}`
  link.setAttribute('download', fileName)
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function getCategoryIcon(cat) {
  switch (cat) {
    case '课件': return Document
    case '论文': return DocumentCopy
    case '笔记': return Ticket
    default: return More
  }
}
</script>

<template>
  <div class="resource-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">📚</span>
        <h2 style="margin:0; color: #6dbcf0">资源共享中心</h2>
      </div>
      <el-button color="var(--ice-bg-gradient)" round class="action-btn" @click="openUploadDialog">
        <el-icon style="margin-right: 5px"><Upload/></el-icon>
        上传资源
      </el-button>
    </div>

    <div class="filter-controls">
      <el-radio-group v-model="categoryFilter" @change="handleCategoryChange">
        <el-radio-button v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">
           {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="resource-table-container" v-loading="loading">
      <el-table :data="resourceList" style="width: 100%" class="modern-table">
        <el-table-column label="资源标题" min-width="250">
          <template #default="{row}">
            <div class="title-cell">
              <div class="icon-box" :class="row.category === '其他' ? 'other' : 'cat-' + row.category">
                <el-icon><component :is="getCategoryIcon(row.category)"/></el-icon>
              </div>
              <div class="info">
                <span class="title">{{ row.title }}</span>
                <span class="desc" v-if="row.description">{{ row.description }}</span>
                <span class="filename" v-else>{{ row.fileName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="存储分类" width="120">
          <template #default="{row}">
            <el-tag size="small" type="info" effect="dark" round>{{ row.category || '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="120">
          <template #default="{row}">
            <span class="filesize">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传者" width="120" />
        <el-table-column label="下载度" width="120" align="center">
          <template #default="{row}">
            <div class="download-count">
              <span>{{ row.downloadCount }}</span> 次
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{row}">
            <el-button 
              type="primary" 
              circle 
              plain
              :icon="Download" 
              @click="downloadResource(row.id, row.fileName)"
              title="下载文件"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !resourceList.length" description="该分类下暂无资源分享" />
    </div>

    <div class="pagination-wrapper">
      <el-pagination 
         background 
         layout="prev, pager, next" 
         :current-page="currentPage"
         @current-change="handlePageChange" 
         :page-size="10" 
         :total="100" 
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="📦 贡献新资源" width="550px" center>
      <el-form :model="uploadForm" label-position="top">
        <el-form-item label="资源标题" required>
          <el-input v-model="uploadForm.title" placeholder="最好能一目了然说明内容..." />
        </el-form-item>
        <el-form-item label="资源类别">
          <el-select v-model="uploadForm.category" placeholder="选择最匹配的分类" style="width: 100%">
            <el-option label="👨‍🏫 课件资料" value="课件" />
            <el-option label="📝 学术论文" value="论文" />
            <el-option label="✍ 学霸笔记" value="笔记" />
            <el-option label="🗃 其他附件" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容说明">
          <el-input type="textarea" v-model="uploadForm.description" placeholder="（选填）简要描述此资源解决什么问题..." :rows="3" resize="none"/>
        </el-form-item>
        <el-form-item label="附件实体" required>
          <el-upload :http-request="handleUpload" :auto-upload="false"
                     :file-list="fileList" :limit="1" ref="uploadRef" drag action="#" class="upload-drag-area">
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">单文件体积限制: 50MB。支持主流文档/压缩包格式。</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="uploadDialogVisible = false">考虑一下</el-button>
        <el-button round type="primary" @click="submitUpload">确认提交云端</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default {
  methods: {
    submitUpload() {
      this.$refs.uploadRef.submit()
    }
  }
}
</script>

<style lang="less" scoped>
.resource-view {
  max-width: 1100px;
  margin: 20px auto;
  padding: 0 15px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .title-area {
    display: flex;
    align-items: center;
    gap: 8px;
    .emoji { font-size: 28px; }
  }

  .action-btn {
    border: none;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(79, 172, 254, 0.4);
    color: white !important;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(79, 172, 254, 0.5);
    }
  }
}

.filter-controls {
  margin-bottom: 24px;
  
  :deep(.el-radio-button__inner) {
    border-radius: 20px !important;
    border: none !important;
    background: var(--el-fill-color-light);
    margin-right: 10px;
    padding: 8px 20px;
    transition: all 0.3s;
  }
  
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background-color: var(--el-color-primary);
    box-shadow: 0 4px 12px var(--el-color-primary-light-5) !important;
  }
}

.resource-table-container {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 20px;
  overflow: hidden;

  .modern-table {
    :deep(.el-table__header th) {
      background-color: var(--el-fill-color-light) !important;
      color: var(--el-text-color-secondary);
      font-weight: 500;
      border-bottom: none;
    }
    :deep(.el-table__row) {
      transition: background-color 0.3s;
      td { border-bottom: 1px dashed var(--el-border-color-lighter); }
      &:hover td { background-color: var(--el-fill-color-extra-light) !important; }
    }
    :deep(.el-table__row:last-child td) {
      border-bottom: none;
    }
  }
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .icon-box {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;

    &.cat-课件 { background: #e0f2fe; color: #0284c7; }
    &.cat-论文 { background: #fce7f3; color: #be185d; }
    &.cat-笔记 { background: #fef3c7; color: #d97706; }
    &.other { background: #f3f4f6; color: #4b5563; }
  }

  .info {
    display: flex;
    flex-direction: column;
    justify-content: center;

    .title {
      font-size: 15px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      margin-bottom: 4px;
    }

    .desc {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.4;
    }

    .filename {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
      font-family: monospace;
    }
  }
}

.filesize {
  font-family: monospace;
  color: var(--el-text-color-regular);
}

.download-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  span {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-regular);
  }
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.upload-drag-area {
  :deep(.el-upload-dragger) {
    border-radius: 8px;
    background-color: var(--el-fill-color-extra-light);
    border-color: var(--el-border-color-light);
    transition: all 0.3s;
    
    &:hover {
      border-color: var(--el-color-primary);
      background-color: var(--el-color-primary-light-9);
    }
  }
}
</style>
