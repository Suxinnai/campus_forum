<script setup>
import {ref, computed} from 'vue'
import {get} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import {Search, Trophy, Medal, DataAnalysis} from '@element-plus/icons-vue'

const studentId = ref('')
const grades = ref([])
const searched = ref(false)
const loading = ref(false)

function queryGrades() {
  if (!studentId.value) return ElMessage.warning('请输入学号进行查询')
  loading.value = true
  get(`/api/grade/query?studentId=${studentId.value}`, data => {
    grades.value = data
    searched.value = true
    setTimeout(() => { loading.value = false }, 300)
  }, (msg) => {
    grades.value = []
    searched.value = true
    loading.value = false
    ElMessage.warning(msg)
  })
}

const avgScore = computed(() => {
  if (!grades.value.length) return 0
  const sum = grades.value.reduce((s, g) => s + parseFloat(g.score), 0)
  return (sum / grades.value.length).toFixed(1)
})

const gpa = computed(() => {
  if (!grades.value.length) return 0
  const total = grades.value.reduce((s, g) => {
    const score = parseFloat(g.score)
    let point = 0
    if (score >= 90) point = 4.0
    else if (score >= 85) point = 3.7
    else if (score >= 82) point = 3.3
    else if (score >= 78) point = 3.0
    else if (score >= 75) point = 2.7
    else if (score >= 72) point = 2.3
    else if (score >= 68) point = 2.0
    else if (score >= 64) point = 1.5
    else if (score >= 60) point = 1.0
    return s + point
  }, 0)
  return (total / grades.value.length).toFixed(2)
})
</script>

<template>
  <div class="grade-view">
    <div class="search-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h2 class="hero-title">教务成绩查询系统</h2>
        <p class="hero-subtitle">请输入您的学号以获取各科成绩及学分绩点统计</p>
        
        <div class="search-box">
          <el-input 
            v-model="studentId" 
            placeholder="示例学号: 2022001" 
            class="grade-search-input"
            clearable
            @keyup.enter="queryGrades"
          >
            <template #prefix>
              <el-icon class="search-icon"><Search/></el-icon>
            </template>
            <template #append>
              <el-button @click="queryGrades" :loading="loading" class="query-btn">
                立即查询
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <transition name="el-fade-in-linear">
      <div v-if="searched && !loading && grades.length" class="result-container">
        <div class="stats-cards">
          <div class="stat-card blue">
            <div class="stat-icon"><el-icon><DataAnalysis/></el-icon></div>
            <div class="stat-info">
              <div class="label">已修课程</div>
              <div class="value">{{grades.length}}<span class="unit">门</span></div>
            </div>
          </div>
          
          <div class="stat-card green">
            <div class="stat-icon"><el-icon><Medal/></el-icon></div>
            <div class="stat-info">
              <div class="label">平均成绩</div>
              <div class="value">{{avgScore}}<span class="unit">分</span></div>
            </div>
          </div>
          
          <div class="stat-card orange">
            <div class="stat-icon"><el-icon><Trophy/></el-icon></div>
            <div class="stat-info">
              <div class="label">平均学分绩点 (GPA)</div>
              <div class="value">{{gpa}}</div>
            </div>
          </div>
        </div>

        <light-card class="table-card">
          <div class="table-header">成绩明细表</div>
          <el-table :data="grades" stripe style="width:100%" class="modern-table">
            <el-table-column prop="courseName" label="课程名称" min-width="180">
              <template #default="{row}">
                <span style="font-weight: 600; color: var(--el-text-color-primary)">{{row.courseName}}</span>
              </template>
            </el-table-column>
            <el-table-column prop="semester" label="修读学期" width="150" align="center">
              <template #default="{row}">
                <el-tag size="small" type="info" effect="plain" round>{{row.semester}}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="最终得分" width="120" align="center">
              <template #default="{row}">
                <div :class="['score-pill', row.score >= 90 ? 'excellent' : (row.score >= 60 ? 'pass' : 'fail')]">
                  {{row.score}}
                </div>
              </template>
            </el-table-column>
          </el-table>
        </light-card>
      </div>
    </transition>
    
    <div v-if="searched && !loading && !grades.length" class="empty-state">
      <el-empty description="未查询到该学号的成绩信息，请核对学号后重试">
        <template #image>
          <div style="font-size: 64px; opacity: 0.5;">📂</div>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<style lang="less" scoped>
.grade-view {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 15px;
}

.search-hero {
  position: relative;
  border-radius: var(--card-radius);
  overflow: hidden;
  padding: 40px 20px;
  text-align: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(138, 216, 255, 0.2); /* 修改阴影适配新主题 */

  .hero-bg {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: var(--ice-bg-gradient); /* 重点：使用冰璃微光渐变 */
    z-index: 1;
    opacity: 0.3; /* 根据情况可调整透明度 */
  }

  .hero-content {
    position: relative;
    z-index: 2;
  }

  .hero-title {
    margin: 0 0 10px 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }

  .hero-subtitle {
    margin: 0 0 30px 0;
    font-size: 15px;
    color: var(--el-text-color-secondary);
  }
}

.search-box {
  max-width: 480px;
  margin: 0 auto;

  .grade-search-input {
    :deep(.el-input-group__append) {
      background-color: transparent;
      border: none;
      padding: 0;
    }
    
    :deep(.el-input__wrapper) {
      padding: 4px 15px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.05) !important;
      border-radius: 8px 0 0 8px !important;
    }

    .query-btn {
      height: 100%;
      border-radius: 0 8px 8px 0 !important;
      color: white !important;
      padding: 0 24px;
      font-weight: 600;
      background: var(--ice-bg-gradient) !important;
      border: none;
    }

    .search-icon {
      font-size: 18px;
      color: var(--el-text-color-secondary);
    }
  }
}

.stats-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;

  @media (max-width: 600px) {
    flex-direction: column;
  }

  .stat-card {
    flex: 1;
    background: var(--el-bg-color);
    border-radius: var(--card-radius);
    padding: 24px;
    box-shadow: var(--card-shadow);
    display: flex;
    align-items: center;
    gap: 16px;
    transition: transform 0.3s;

    &:hover { transform: translateY(-3px); }

    .stat-icon {
      width: 54px;
      height: 54px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
    }

    .stat-info {
      .label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        margin-bottom: 4px;
      }
      .value {
        font-size: 26px;
        font-weight: 700;
        line-height: 1;

        .unit {
          font-size: 14px;
          margin-left: 4px;
          font-weight: 500;
        }
      }
    }

    &.blue {
      .stat-icon { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
      .value { color: var(--el-color-primary); }
    }
    &.green {
      .stat-icon { background: var(--el-color-success-light-9); color: var(--el-color-success); }
      .value { color: var(--el-color-success); }
    }
    &.orange {
      .stat-icon { background: var(--el-color-warning-light-9); color: var(--el-color-warning); }
      .value { color: var(--el-color-warning); }
    }
  }
}

.table-card {
  padding: 0;
  overflow: hidden;

  .table-header {
    padding: 20px 24px;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background-color: var(--el-fill-color-extra-light);
  }
  
  .modern-table {
    :deep(.el-table__header th) {
      background-color: var(--el-fill-color-extra-light) !important;
      color: var(--el-text-color-secondary);
      font-weight: 500;
    }
    :deep(.el-table__row) {
      transition: background-color 0.3s;
    }
  }
}

.score-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 16px;
  font-weight: 700;
  font-size: 14px;
  font-family: monospace;

  &.excellent { background: var(--el-color-success-light-9); color: var(--el-color-success); }
  &.pass { background: var(--el-fill-color-light); color: var(--el-text-color-regular); }
  &.fail { background: var(--el-color-danger-light-9); color: var(--el-color-danger); }
}

.empty-state {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 40px;
}
</style>
