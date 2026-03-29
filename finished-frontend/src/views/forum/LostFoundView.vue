<script setup>
import {ref, reactive} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import {useCounterStore} from '@/stores/counter.js'
import { Search, Plus } from '@element-plus/icons-vue'

const store = useCounterStore()
const filterType = ref('')
const list = ref([])
const showPublish = ref(false)
const form = reactive({title: '', content: '', type: 'lost', contact: ''})

function loadList() {
  get(`/api/lost-found/list?page=1&type=${filterType.value}`, data => list.value = data)
}
loadList()

function publish() {
  if (!form.title || !form.content) return ElMessage.warning('请填写完整信息')
  post('/api/lost-found/publish', form, () => {
    ElMessage.success('发布成功')
    showPublish.value = false
    form.title = ''; form.content = ''; form.contact = ''
    loadList()
  })
}

function closeItem(id) {
  get(`/api/lost-found/close?id=${id}`, () => { ElMessage.success('已关闭'); loadList() })
}
</script>

<template>
  <div class="lost-found-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">🔍</span>
        <h2 style="margin:0">失物招领</h2>
      </div>
      <el-button type="primary" round class="action-btn" @click="showPublish=true">
        <el-icon style="margin-right: 5px"><Plus/></el-icon>
        发布信息
      </el-button>
    </div>
    
    <div class="filter-controls">
      <el-radio-group v-model="filterType" @change="loadList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="lost">寻物启事</el-radio-button>
        <el-radio-button value="found">招领启事</el-radio-button>
      </el-radio-group>
    </div>

    <div class="item-list">
      <light-card v-for="item in list" :key="item.id" class="lf-card">
        <div class="card-header">
          <div class="title-wrap">
            <div :class="['pill-badge', item.type === 'lost' ? 'danger' : 'success']">
              {{item.type==='lost' ? '丢失' : '捡到'}}
            </div>
            <div v-if="item.status==='closed'" class="pill-badge info">已关闭</div>
            <span class="title">{{item.title}}</span>
          </div>
          <span class="time">{{new Date(item.createTime).toLocaleString()}}</span>
        </div>
        
        <div class="content">{{item.content}}</div>
        
        <div class="footer-info" v-if="item.contact">
          <div class="contact-box">
            <span class="icon">📞</span>
            <span>联系方式: {{item.contact}}</span>
          </div>
        </div>
      </light-card>
      <el-empty v-if="!list.length" description="暂无失物招领信息"/>
    </div>

    <el-dialog v-model="showPublish" title="发布失物招领" width="500px" center>
      <el-form label-position="top" class="modern-form">
        <el-form-item label="信息类型">
          <el-radio-group v-model="form.type">
            <el-radio value="lost" border>我丢了</el-radio>
            <el-radio value="found" border>我捡到</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="物品标题">
          <el-input v-model="form.title" placeholder="例如：在三教遗失黑色雨伞" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input 
            type="textarea" 
            v-model="form.content" 
            :rows="4" 
            placeholder="描述物品的特征、丢失/捡到的大致时间与地点..."
            resize="none"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" placeholder="手机号/QQ/微信" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="showPublish=false">取消</el-button>
        <el-button round type="primary" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.lost-found-view {
  max-width: 1000px;
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
    box-shadow: 0 4px 12px var(--el-color-primary-light-5);
    font-weight: 600;
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

.item-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.lf-card {
  padding: 24px;
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  
  &:hover {
    transform: translateX(4px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title-wrap {
      display: flex;
      align-items: center;
      gap: 10px;

      .title {
        font-weight: 600;
        font-size: 16px;
        color: var(--el-text-color-primary);
      }
    }

    .time {
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }
  }

  .content {
    color: var(--el-text-color-regular);
    font-size: 15px;
    line-height: 1.6;
    margin-bottom: 16px;
  }

  .footer-info {
    .contact-box {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background-color: var(--el-fill-color-light);
      border-radius: 8px;
      font-size: 14px;
      color: var(--el-text-color-regular);
      font-weight: 500;
    }
  }
}

.pill-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;

  &.danger {
    background-color: var(--el-color-danger-light-9);
    color: var(--el-color-danger);
  }
  &.success {
    background-color: var(--el-color-success-light-9);
    color: var(--el-color-success);
  }
  &.info {
    background-color: var(--el-fill-color);
    color: var(--el-text-color-secondary);
  }
}
</style>
