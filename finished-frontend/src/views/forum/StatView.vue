<script setup>
import {ref, onMounted, markRaw} from 'vue'
import {get} from "@/net/api.js"
import {use} from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {LineChart, PieChart} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const lineOption = ref(null)
const pieOption = ref(null)
const wordCloudData = ref([])

// 加载发帖趋势数据
function loadHotTrend() {
  get('/api/stat/hot-trend', (data) => {
    lineOption.value = {
      title: {
        text: '近7天发帖热度趋势', 
        left: 'center',
        textStyle: { fontWeight: 600, fontSize: 16 }
      },
      tooltip: {trigger: 'axis'},
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(item => item.date),
        axisLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#666' }
      },
      yAxis: {
        type: 'value', 
        splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
        axisLabel: { color: '#666' }
      },
      series: [{
        data: data.map(item => item.count),
        type: 'line',
        smooth: true,
        showSymbol: false,
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              {offset: 0, color: 'rgba(138, 216, 255, 0.6)'},
              {offset: 1, color: 'rgba(138, 216, 255, 0.05)'}
            ]
          }
        },
        lineStyle: {color: '#8ad8ff', width: 3},
        itemStyle: {color: '#8ad8ff'}
      }]
    }
  })
}

// 加载分类占比数据
function loadCategoryPie() {
  get('/api/stat/category-pie', (data) => {
    pieOption.value = {
      title: {
        text: '板块活跃度分布', 
        left: 'center',
        textStyle: { fontWeight: 600, fontSize: 16 }
      },
      tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
      legend: {bottom: '0', icon: 'circle'},
      color: ['#8ad8ff', '#addffe', '#6ebce6', '#bce6fd', '#d8f1fe', '#4ba3e3'],
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {borderRadius: 8, borderColor: '#fff', borderWidth: 2},
        label: {show: false},
        data: data.map(item => ({name: item.name, value: item.value}))
      }]
    }
  })
}

// 加载关键词词频数据
function loadKeywordCloud() {
  get('/api/stat/keyword-cloud', (data) => {
    wordCloudData.value = data || []
  })
}

onMounted(() => {
  loadHotTrend()
  loadCategoryPie()
  loadKeywordCloud()
})
</script>

<template>
  <div class="stat-view">
    <div class="page-header">
      <div class="title-area">
        <span class="emoji">📊</span>
        <h2 style="margin:0; color: #6dbcf0">数据可视化</h2>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <v-chart v-if="lineOption" :option="lineOption" style="height: 380px; width: 100%;" autoresize />
        <el-empty v-else description="暂无热度数据" />
      </div>

      <div class="chart-card">
        <v-chart v-if="pieOption" :option="pieOption" style="height: 380px; width: 100%;" autoresize />
        <el-empty v-else description="暂无分布数据" />
      </div>
    </div>

    <div class="chart-card keyword-card" style="margin-top: 24px;">
      <h3 class="card-title" style="color: #0d4a75">❄️ 冰璃实时热词榜</h3>
      <div v-if="wordCloudData.length" class="keyword-cloud">
        <div 
          v-for="(item, index) in wordCloudData" 
          :key="index"
          class="keyword-item"
          :style="{
            fontSize: Math.min(13 + item.value * 1.5, 28) + 'px',
            color: ['#8ad8ff', '#5ebdf0', '#9ce6ff', '#addffe', '#6ebce6'][index % 5],
            opacity: Math.max(0.6, Math.min(1, item.value / 10 + 0.4))
          }"
        >
          {{ item.name }}
          <span class="count">{{ item.value }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无关键词数据" />
    </div>
  </div>
</template>

<style lang="less" scoped>
.stat-view {
  max-width: 1200px;
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 24px;
}

.chart-card {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 24px;
  box-shadow: var(--card-shadow);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 30px rgba(0,0,0,0.06);
  }

  .card-title {
    margin: 0 0 20px 0;
    font-size: 16px;
    font-weight: 600;
  }
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 16px 24px;
  padding: 20px;
  min-height: 200px;

  .keyword-item {
    font-weight: 600;
    transition: all 0.3s;
    cursor: pointer;
    display: flex;
    align-items: baseline;
    gap: 4px;

    .count {
      font-size: 12px;
      font-weight: 500;
      opacity: 0.6;
    }

    &:hover {
      transform: scale(1.1);
      opacity: 1 !important;
    }
  }
}
</style>
