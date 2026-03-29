<script setup>
import {ref} from 'vue'
import {get} from '@/net/api.js'
import LightCard from '@/components/LightCard.vue'
import {Search, Collection} from '@element-plus/icons-vue'

const list = ref([])
const keyword = ref('')
const category = ref('')
const categories = ref([])
const detail = ref(null)
const showDetail = ref(false)

function loadBooks() {
  get(`/api/book/list?page=1&keyword=${keyword.value}&category=${category.value}`, data => list.value = data)
}
loadBooks()

get('/api/book/categories', data => categories.value = data)

function viewDetail(item) {
  detail.value = item
  showDetail.value = true
}
</script>

<template>
  <div class="library-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">📚</span>
        <h2 style="margin:0">在线图书馆</h2>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-inner">
        <el-input 
           v-model="keyword" 
           placeholder="搜索书名或作者..." 
           :prefix-icon="Search" 
           class="main-search"
           clearable
           @keyup.enter="loadBooks"
        />
        <div class="divider"></div>
        <el-select 
           v-model="category" 
           placeholder="全部分类" 
           clearable 
           class="category-select"
           @change="loadBooks"
        >
          <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
        </el-select>
        <el-button type="primary" round class="search-btn" @click="loadBooks">开始查询</el-button>
      </div>
    </div>

    <div class="book-grid">
      <div 
        v-for="book in list" 
        :key="book.id" 
        class="book-card"
        @click="viewDetail(book)"
      >
        <div class="book-cover">
          <div class="cover-placeholder">
            <el-icon :size="32"><Collection/></el-icon>
          </div>
          <div :class="['status-chip', book.available ? 'available' : 'borrowed']">
            {{book.available ? '可借阅' : '已借出'}}
          </div>
        </div>
        
        <div class="book-info">
          <div class="category">{{book.category}}</div>
          <h3 class="title" :title="book.title">{{book.title}}</h3>
          <div class="author">{{book.author}}</div>
          
          <div class="location-footer">
            <span class="icon">📍</span>
            <span>{{book.location}}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!list.length" description="未搜索到相关图书信息"/>

    <el-dialog v-model="showDetail" title="图书详情" width="550px" center custom-class="book-dialog">
      <div v-if="detail" class="detail-container">
        <div class="detail-header">
          <div class="big-cover placeholder">
             <el-icon :size="48"><Collection/></el-icon>
          </div>
          <div class="prime-info">
            <h2>{{detail.title}}</h2>
            <p class="author">{{detail.author}}</p>
            <div :class="['status-badge', detail.available ? 'available' : 'borrowed']">
              {{detail.available ? '🟢 当前馆内可借' : '🔴 暂无馆藏副本'}}
            </div>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="label">国际标准书号 (ISBN)</span>
            <span class="value">{{detail.isbn || '暂无数据'}}</span>
          </div>
          <div class="info-item">
            <span class="label">图书分类</span>
            <span class="value">{{detail.category}}</span>
          </div>
          <div class="info-item">
            <span class="label">馆藏位置</span>
            <span class="value">{{detail.location || '未知'}}</span>
          </div>
        </div>

        <div class="desc-box" v-if="detail.description">
          <div class="box-title">内容简介</div>
          <div class="box-content">{{detail.description}}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.library-view {
  max-width: 1100px;
  margin: 20px auto;
  padding: 0 15px;
}

.page-header {
  margin-bottom: 24px;
  .title-area {
    display: flex;
    align-items: center;
    gap: 8px;
    .emoji { font-size: 28px; }
  }
}

.search-bar {
  background: var(--el-bg-color);
  padding: 8px;
  border-radius: 100px;
  box-shadow: var(--card-shadow);
  margin-bottom: 30px;
  
  .search-inner {
    display: flex;
    align-items: center;
    
    :deep(.el-input__wrapper) {
      box-shadow: none !important;
      background: transparent;
    }

    .main-search {
      flex: 1;
      :deep(.el-input__inner) { font-size: 15px; }
    }

    .divider {
      width: 1px;
      height: 24px;
      background-color: var(--el-border-color);
      margin: 0 10px;
    }

    .category-select {
      width: 140px;
    }

    .search-btn {
      margin-left: 10px;
      padding: 0 24px;
      height: 40px;
    }
  }
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.book-card {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.08);

    .book-cover {
      background-color: var(--el-color-primary-light-8);
      color: var(--el-color-primary);
    }
  }

  .book-cover {
    height: 140px;
    background-color: var(--el-fill-color-light);
    color: var(--el-text-color-placeholder);
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;

    .status-chip {
      position: absolute;
      top: 12px;
      right: 12px;
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
      backdrop-filter: blur(4px);

      &.available { background: rgba(103, 194, 58, 0.9); color: #fff; }
      &.borrowed { background: rgba(144, 147, 153, 0.9); color: #fff; }
    }
  }

  .book-info {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;

    .category {
      font-size: 11px;
      font-weight: 600;
      color: var(--el-color-primary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 6px;
    }

    .title {
      margin: 0 0 4px 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .author {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-bottom: 16px;
    }

    .location-footer {
      margin-top: auto;
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      padding-top: 12px;
      border-top: 1px dashed var(--el-border-color-lighter);
    }
  }
}

.detail-container {
  .detail-header {
    display: flex;
    gap: 20px;
    margin-bottom: 24px;

    .big-cover {
      width: 100px;
      height: 140px;
      background: var(--el-fill-color-light);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--el-text-color-placeholder);
    }

    .prime-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;

      h2 { margin: 0 0 8px 0; font-size: 20px; }
      .author { margin: 0 0 16px 0; color: var(--el-text-color-secondary); font-size: 14px; }
      
      .status-badge {
        display: inline-block;
        padding: 6px 12px;
        border-radius: 6px;
        font-size: 13px;
        font-weight: 600;
        width: fit-content;

        &.available { background: var(--el-color-success-light-9); color: var(--el-color-success); }
        &.borrowed { background: var(--el-color-info-light-9); color: var(--el-text-color-secondary); }
      }
    }
  }

  .info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 24px;
    background: var(--el-fill-color-extra-light);
    padding: 16px;
    border-radius: 8px;

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .label { font-size: 12px; color: var(--el-text-color-secondary); }
      .value { font-size: 14px; color: var(--el-text-color-primary); font-weight: 500; }
    }
  }

  .desc-box {
    .box-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 8px;
    }
    .box-content {
      font-size: 14px;
      line-height: 1.6;
      color: var(--el-text-color-regular);
      background: var(--el-fill-color-light);
      padding: 16px;
      border-radius: 8px;
    }
  }
}
</style>
