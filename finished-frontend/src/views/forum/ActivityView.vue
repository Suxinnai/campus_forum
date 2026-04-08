<script setup>
import { ref } from 'vue'
import { get, post } from '@/net/api.js'
import { ElMessage } from 'element-plus'
import { Calendar, MapPin, Users, Flag, Clock } from 'lucide-vue-next'

const list = ref([])
get('/api/activity/list', data => list.value = data)

function joinActivity(id) {
  post(`/api/activity/join?id=${id}`, {}, () => {
    ElMessage.success('报名成功！')
    get('/api/activity/list', data => list.value = data)
  })
}

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getMonth()+1}月${date.getDate()}日 ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

function isFull(item) {
  return item.maxPeople > 0 && item.currentPeople >= item.maxPeople
}
</script>

<template>
  <div class="ds-page">
    <div class="ds-page-header">
      <h2 class="page-title">
        <span class="title-accent"></span>
        校园活动
      </h2>
    </div>

    <div class="activity-list" v-if="list.length">
      <div v-for="item in list" :key="item.id" class="activity-card ds-card">
        <div class="card-left">
          <div class="activity-no">NO.{{ String(item.id).padStart(2, '0') }}</div>
          <h3 class="activity-title">{{ item.title }}</h3>
          <p class="activity-desc">{{ item.content }}</p>
          <div class="meta-list">
            <div class="meta-item">
              <Calendar :size="14" />
              <span>{{ formatDate(item.startTime) }} — {{ formatDate(item.endTime) }}</span>
            </div>
            <div class="meta-item" v-if="item.location">
              <MapPin :size="14" />
              <span>{{ item.location }}</span>
            </div>
            <div class="meta-item" v-if="item.organizer">
              <Users :size="14" />
              <span>{{ item.organizer }}</span>
            </div>
          </div>
        </div>

        <div class="card-right">
          <div class="people-counter">
            <div class="counter-num">{{ item.currentPeople }}</div>
            <div class="counter-total">/ {{ item.maxPeople || '∞' }}</div>
            <div class="counter-label">已报名</div>
          </div>
          <div class="progress-bar" v-if="item.maxPeople > 0">
            <div class="progress-fill" :style="{ width: Math.min(100, item.currentPeople / item.maxPeople * 100) + '%', background: isFull(item) ? '#DC2626' : 'var(--ds-primary)' }"></div>
          </div>
          <button
            class="join-btn"
            :class="{ full: isFull(item) }"
            :disabled="isFull(item)"
            @click="joinActivity(item.id)"
          >
            <Flag :size="14" />
            {{ isFull(item) ? '名额已满' : '立即报名' }}
          </button>
        </div>
      </div>
    </div>

    <el-empty v-else description="近期暂无校园活动" style="margin-top: 60px" />
  </div>
</template>

<style lang="less" scoped>
@import '@/assets/design-system.less';

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-card {
  display: flex;
  gap: 0;
  overflow: hidden;
  transition: var(--ds-transition);

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--ds-shadow-md);
  }
}

.card-left {
  flex: 1;
  padding: 28px 32px;

  .activity-no {
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 2px;
    color: var(--ds-primary-light);
    margin-bottom: 10px;
  }

  .activity-title {
    font-size: 20px;
    font-weight: 800;
    color: var(--ds-text-1);
    margin: 0 0 12px;
  }

  .activity-desc {
    font-size: 14px;
    color: var(--ds-muted);
    line-height: 1.7;
    margin: 0 0 20px;
  }
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ds-muted);
  svg { color: var(--ds-primary-light); flex-shrink: 0; }
}

.card-right {
  width: 200px;
  flex-shrink: 0;
  background: var(--ds-primary-lighter);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  gap: 16px;
  border-left: 1px solid var(--ds-border);
}

.people-counter {
  text-align: center;

  .counter-num {
    font-size: 42px;
    font-weight: 900;
    color: var(--ds-primary);
    line-height: 1;
  }
  .counter-total {
    font-size: 16px;
    color: var(--ds-primary-light);
    font-weight: 600;
  }
  .counter-label {
    font-size: 12px;
    color: var(--ds-muted);
    margin-top: 4px;
  }
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: var(--ds-border);
  border-radius: 2px;
  overflow: hidden;

  .progress-fill {
    height: 100%;
    border-radius: 2px;
    transition: width 0.6s ease;
  }
}

.join-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--ds-primary), var(--ds-primary-light));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border: none;
  border-radius: 50px;
  cursor: pointer;
  transition: var(--ds-transition);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.3);
  width: 100%;
  justify-content: center;

  &:hover:not(.full) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(124, 58, 237, 0.4);
  }

  &.full {
    background: #E5E7EB;
    color: var(--ds-muted);
    box-shadow: none;
    cursor: not-allowed;
  }
}
</style>
