<script setup>
import {ref, reactive, onMounted} from 'vue'
import {get, post, getToken} from "@/net/api.js"
import {ElMessage} from "element-plus"
import {Upload, Download, Search} from "@element-plus/icons-vue"
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
</script>

<template>
  <div style="padding: 20px; max-width: 1200px; margin: 0 auto;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2 style="margin: 0;">📚 资源共享中心</h2>
      <el-button type="primary" :icon="Upload" @click="openUploadDialog">上传资源</el-button>
    </div>

    <div style="margin-bottom: 15px;">
      <el-radio-group v-model="categoryFilter" @change="handleCategoryChange">
        <el-radio-button v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="resourceList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{row}">
          <span style="font-weight: bold;">{{ row.title }}</span>
          <div style="font-size: 12px; color: #999; margin-top: 4px;" v-if="row.description">
            {{ row.description }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{row}">
          <el-tag size="small">{{ row.category || '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" width="180" show-overflow-tooltip />
      <el-table-column label="大小" width="100">
        <template #default="{row}">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传者" width="120" />
      <el-table-column prop="downloadCount" label="下载次数" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{row}">
          <el-button type="primary" :icon="Download" size="small"
                     @click="downloadResource(row.id, row.fileName)">
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 15px; text-align: center;">
      <el-pagination layout="prev, pager, next" :current-page="currentPage"
                     @current-change="handlePageChange" :page-size="10" :total="100" />
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传资源" width="500px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="uploadForm.title" placeholder="请输入资源标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" placeholder="选择分类">
            <el-option label="课件" value="课件" />
            <el-option label="论文" value="论文" />
            <el-option label="笔记" value="笔记" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="uploadForm.description" placeholder="简要描述此资源" />
        </el-form-item>
        <el-form-item label="文件" required>
          <el-upload :http-request="handleUpload" :auto-upload="false"
                     :file-list="fileList" :limit="1" ref="uploadRef">
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="color: #999; font-size: 12px;">单个文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload">确认上传</el-button>
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
