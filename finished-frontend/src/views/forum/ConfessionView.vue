<script setup>
import {ref, reactive} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import { EditPen } from '@element-plus/icons-vue'

const list = ref([])
const showPublish = ref(false)
const form = reactive({content: '', anonymous: 1})

function loadList() {
  get('/api/confession/list?page=1', data => list.value = data)
}
loadList()

function publish() {
  if (!form.content) return ElMessage.warning('请输入内容')
  post('/api/confession/publish', form, () => {
    ElMessage.success('发布成功')
    showPublish.value = false
    form.content = ''
    loadList()
  })
}

function likeIt(id) {
  post(`/api/confession/like?id=${id}`, {}, () => loadList())
}
</script>

<template>
  <div class="confession-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">✨</span>
        <h2 style="margin:0; color: #6dbcf0">冰璃微光</h2>
      </div>
      <el-button color="var(--ice-bg-gradient)" round class="action-btn" @click="showPublish=true">
        <el-icon style="margin-right: 5px"><EditPen/></el-icon>
        写一条
      </el-button>
    </div>
    
    <div class="waterfall-grid">
      <light-card v-for="item in list" :key="item.id" class="confession-card">
        <div class="card-content">{{item.content}}</div>
        <div class="card-footer">
          <div class="author-info">
            <span class="name">{{item.username}}</span>
            <span class="dot">·</span>
            <span class="time">{{new Date(item.createTime).toLocaleDateString()}}</span>
          </div>
          <div class="like-btn" @click.stop="likeIt(item.id)">
            <span class="heart">❤️</span>
            <span class="count">{{item.likes}}</span>
          </div>
        </div>
      </light-card>
    </div>
    <el-empty v-if="!list.length" description="还没有人表白，快来第一个吧~" />

    <el-dialog v-model="showPublish" title="发表心情" width="450px" center class="custom-dialog">
      <el-input 
        type="textarea" 
        v-model="form.content" 
        :rows="5" 
        placeholder="说出你想说的话..." 
        resize="none"
        maxlength="200"
        show-word-limit
      />
      <div style="margin-top:16px; text-align: right">
        <el-switch 
          v-model="form.anonymous" 
          :active-value="1" 
          :inactive-value="0"
          active-text="匿名发布"
        />
      </div>
      <template #footer>
        <el-button round @click="showPublish=false">取消</el-button>
        <el-button round color="#ff758c" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.confession-view {
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
    
    .emoji {
      font-size: 28px;
    }
  }

  .action-btn {
    border: none;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(255, 117, 140, 0.3);
    color: white !important;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(255, 117, 140, 0.4);
    }
  }
}

.waterfall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.confession-card {
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: linear-gradient(180deg, var(--el-bg-color) 0%, var(--el-fill-color-extra-light) 100%);
  border-top: 4px solid #8ad8ff;

  .card-content {
    font-size: 15px;
    line-height: 1.7;
    color: var(--el-text-color-regular);
    flex: 1;
    min-height: 80px;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px dashed var(--el-border-color-lighter);

    .author-info {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      display: flex;
      align-items: center;
      gap: 6px;

      .name { font-weight: 600; color: var(--el-text-color-primary); }
      .dot { color: var(--el-border-color-darker); }
    }

    .like-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: var(--el-fill-color);
      border-radius: 20px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

      .count {
        font-size: 13px;
        font-weight: 600;
        color: var(--el-text-color-regular);
      }

      &:hover {
        background: #eaf4ff;
        transform: scale(1.05);

        .count { color: #8ad8ff; }
      }
    }
  }
}
</style>
