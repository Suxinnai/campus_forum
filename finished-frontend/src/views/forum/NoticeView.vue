<script setup>
import {ref} from 'vue'
import {get} from '@/net/api.js'
import LightCard from '@/components/LightCard.vue'
import { Document, Top } from '@element-plus/icons-vue'

const list = ref([])
const detail = ref(null)
const showDetail = ref(false)

get('/api/notice/list', data => list.value = data)

function viewDetail(id) {
  get(`/api/notice/detail?id=${id}`, data => {
    detail.value = data
    showDetail.value = true
  })
}
</script>

<template>
  <div class="notice-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">📢</span>
        <h2 style="margin:0">教务通知</h2>
      </div>
    </div>

    <div class="notice-timeline">
      <div v-for="item in list" :key="item.id" class="timeline-row">
        <div class="date-col">
          <div class="day">{{new Date(item.createTime).getDate().toString().padStart(2, '0')}}</div>
          <div class="month-year">{{new Date(item.createTime).getFullYear()}}.{{(new Date(item.createTime).getMonth() + 1).toString().padStart(2, '0')}}</div>
        </div>
        <div class="node-col">
          <div :class="['node-dot', { 'is-top': item.isTop }]"></div>
          <div class="node-line"></div>
        </div>
        <div class="card-col">
          <div class="notice-card" @click="viewDetail(item.id)">
            <div class="card-content">
              <div class="title-wrap">
                <div v-if="item.isTop" class="top-badge"><el-icon><Top/></el-icon> 置顶</div>
                <h3 class="title">{{item.title}}</h3>
              </div>
              <p class="publisher">
                <el-icon><Document/></el-icon>
                发文机关: {{item.publisher}}
              </p>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!list.length" description="近期暂无教务通知"/>
    </div>

    <el-dialog v-model="showDetail" :title="detail?.title" width="600px" center custom-class="notice-dialog">
      <div v-if="detail" class="dialog-body">
        <div class="meta-info">
          <span>{{detail.publisher}}</span>
          <span class="dot">·</span>
          <span>{{new Date(detail.createTime).toLocaleString()}}</span>
        </div>
        <div class="content-text">{{detail.content}}</div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="less" scoped>
.notice-view {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 15px;
}

.page-header {
  margin-bottom: 32px;
  .title-area {
    display: flex;
    align-items: center;
    gap: 8px;
    .emoji { font-size: 28px; }
  }
}

.notice-timeline {
  display: flex;
  flex-direction: column;
}

.timeline-row {
  display: flex;
  min-height: 100px;

  &:last-child .node-line {
    display: none;
  }
}

.date-col {
  width: 80px;
  text-align: right;
  padding-right: 20px;
  padding-top: 10px;
  color: var(--el-text-color-primary);

  .day {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }
  .month-year {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
}

.node-col {
  width: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;

  .node-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: var(--el-color-primary-light-5);
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
    margin-top: 18px;
    z-index: 2;

    &.is-top {
      background-color: var(--el-color-danger);
      box-shadow: 0 0 0 2px var(--el-color-danger-light-8);
    }
  }

  .node-line {
    position: absolute;
    top: 30px;
    bottom: -18px;
    width: 2px;
    background-color: var(--el-border-color-lighter);
    z-index: 1;
  }
}

.card-col {
  flex: 1;
  padding-left: 20px;
  padding-bottom: 24px;
}

.notice-card {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border-left: 4px solid transparent;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.06);
    border-left-color: var(--el-color-primary);
  }

  .title-wrap {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .top-badge {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      padding: 2px 8px;
      background: var(--el-color-danger-light-9);
      color: var(--el-color-danger);
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
    }

    .title {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  .publisher {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.dialog-body {
  .meta-info {
    text-align: center;
    color: var(--el-text-color-secondary);
    font-size: 13px;
    margin-bottom: 24px;
    
    .dot { margin: 0 8px; }
  }

  .content-text {
    font-size: 15px;
    line-height: 1.8;
    color: var(--el-text-color-regular);
    white-space: pre-wrap;
    background: var(--el-fill-color-light);
    padding: 20px;
    border-radius: 8px;
  }
}
</style>
