<script setup>
import {ref} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import {Calendar, Location, User, Flag} from '@element-plus/icons-vue'

const list = ref([])
get('/api/activity/list', data => list.value = data)

function joinActivity(id) {
  post(`/api/activity/join?id=${id}`, {}, () => {
    ElMessage.success('报名成功！')
    get('/api/activity/list', data => list.value = data)
  })
}

function formatDate(d) { return d ? new Date(d).toLocaleString() : '' }
</script>

<template>
  <div class="activity-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">🎉</span>
        <h2 style="margin:0">校园活动</h2>
      </div>
    </div>
    
    <div class="activity-list">
      <light-card v-for="item in list" :key="item.id" class="activity-card">
        <div class="card-inner">
          <div class="info-section">
            <div class="header-row">
              <h3 class="title">{{item.title}}</h3>
              <div class="status-badge" v-if="item.maxPeople > 0 && item.currentPeople >= item.maxPeople">
                已满员
              </div>
            </div>
            
            <div class="meta-grid">
              <div class="meta-item">
                <div class="icon-wrap"><el-icon><Calendar/></el-icon></div>
                <span>{{formatDate(item.startTime)}} ~ {{formatDate(item.endTime)}}</span>
              </div>
              <div class="meta-item" v-if="item.location">
                <div class="icon-wrap"><el-icon><Location/></el-icon></div>
                <span>{{item.location}}</span>
              </div>
              <div class="meta-item" v-if="item.organizer">
                <div class="icon-wrap"><el-icon><User/></el-icon></div>
                <span>主办方: {{item.organizer}}</span>
              </div>
            </div>

            <div class="desc-content">{{item.content}}</div>
          </div>
          
          <div class="action-section">
            <div class="progress-ring">
              <div class="count-display">
                <span class="current">{{item.currentPeople}}</span>
                <span class="divider">/</span>
                <span class="max">{{item.maxPeople || '∞'}}</span>
              </div>
              <div class="label">已报名人数</div>
            </div>
            
            <el-button 
               class="join-btn"
               :type="(item.maxPeople > 0 && item.currentPeople >= item.maxPeople) ? 'info' : 'primary'" 
               round
               :disabled="item.maxPeople > 0 && item.currentPeople >= item.maxPeople"
               @click="joinActivity(item.id)">
               <el-icon style="margin-right:4px"><Flag/></el-icon>
              {{item.maxPeople > 0 && item.currentPeople >= item.maxPeople ? '名额已满' : '立即报名'}}
            </el-button>
          </div>
        </div>
      </light-card>
      <el-empty v-if="!list.length" description="近期暂无校园活动哦~"/>
    </div>
  </div>
</template>

<style lang="less" scoped>
.activity-view {
  max-width: 1000px;
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

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-card {
  padding: 0;
  overflow: hidden;
  
  .card-inner {
    display: flex;
    min-height: 200px;

    @media (max-width: 768px) {
      flex-direction: column;
    }
  }

  .info-section {
    flex: 1;
    padding: 24px 30px;
    display: flex;
    flex-direction: column;

    .header-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .title {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: var(--el-text-color-primary);
      }

      .status-badge {
        padding: 4px 10px;
        background-color: var(--el-color-info-light-9);
        color: var(--el-color-info);
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .meta-grid {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 20px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 8px;
        color: var(--el-text-color-regular);
        font-size: 14px;

        .icon-wrap {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 24px;
          height: 24px;
          border-radius: 6px;
          background: var(--el-fill-color-light);
          color: var(--el-color-primary);
        }
      }
    }

    .desc-content {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      line-height: 1.6;
      margin-top: auto;
    }
  }

  .action-section {
    width: 220px;
    background: var(--el-fill-color-extra-light);
    border-left: 1px dashed var(--el-border-color-light);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px 20px;
    gap: 20px;

    @media (max-width: 768px) {
      width: 100%;
      border-left: none;
      border-top: 1px dashed var(--el-border-color-light);
      flex-direction: row;
      justify-content: space-around;
      padding: 20px;
    }

    .progress-ring {
      text-align: center;

      .count-display {
        color: var(--el-color-primary);
        font-family: monospace;
        
        .current { font-size: 32px; font-weight: 700; }
        .divider { font-size: 18px; margin: 0 4px; color: var(--el-border-color-darker); }
        .max { font-size: 16px; color: var(--el-text-color-secondary); font-weight: 600; }
      }

      .label {
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .join-btn {
      width: 100%;
      max-width: 160px;
      height: 40px;
      font-weight: 600;
      font-size: 14px;

      &:not(.is-disabled) {
        box-shadow: 0 4px 12px var(--el-color-primary-light-5);
      }
    }
  }
}
</style>
