<script setup>
import { ref, onMounted } from 'vue'
import { get } from "@/net/api.js"
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { TrendingUp, PieChart as PieIcon, Hash } from 'lucide-vue-next'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const lineOption = ref(null)
const pieOption = ref(null)
const wordCloudData = ref([])

function loadHotTrend() {
  get('/api/stat/hot-trend', (data) => {
    lineOption.value = {
      tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#DDD6FE', textStyle: { color: '#1E1B4B' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(item => item.date),
        axisLine: { lineStyle: { color: '#DDD6FE' } },
        axisLabel: { color: '#6B7280', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#F5F3FF', type: 'dashed' } },
        axisLabel: { color: '#6B7280', fontSize: 12 }
      },
      series: [{
        data: data.map(item => item.count),
        type: 'line',
        smooth: true,
        showSymbol: false,
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(124,58,237,0.25)' }, { offset: 1, color: 'rgba(124,58,237,0.02)' }]
          }
        },
        lineStyle: { color: '#7C3AED', width: 3 },
        itemStyle: { color: '#7C3AED' }
      }]
    }
  })
}

function loadCategoryPie() {
  get('/api/stat/category-pie', (data) => {
    pieOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: '0', icon: 'circle', textStyle: { color: '#6B7280' } },
      color: ['#7C3AED', '#A78BFA', '#16A34A', '#F59E0B', '#EF4444', '#3B82F6'],
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        data: data.map(item => ({ name: item.name, value: item.value }))
      }]
    }
  })
}

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
  <div class="ds-page">
    <div class="ds-page-header">
      <h2 class="page-title">
        <span class="title-accent"></span>
        数据统计
      </h2>
    </div>

    <div class="chart-grid">
      <div class="chart-card ds-card">
        <div class="chart-card-header">
          <TrendingUp :size="18" class="chart-icon" />
          <span>近7天发帖热度趋势</span>
        </div>
        <v-chart v-if="lineOption" :option="lineOption" style="height: 320px; width: 100%;" autoresize />
        <el-empty v-else description="暂无数据" :image-size="80" />
      </div>

      <div class="chart-card ds-card">
        <div class="chart-card-header">
          <PieIcon :size="18" class="chart-icon" />
          <span>板块活跃度分布</span>
        </div>
        <v-chart v-if="pieOption" :option="pieOption" style="height: 320px; width: 100%;" autoresize />
        <el-empty v-else description="暂无数据" :image-size="80" />
      </div>
    </div>

    <div class="keyword-card ds-card">
      <div class="chart-card-header">
        <Hash :size="18" class="chart-icon" />
        <span>热词榜</span>
      </div>
      <div v-if="wordCloudData.length" class="keyword-cloud">
        <span
          v-for="(item, index) in wordCloudData"
          :key="index"
          class="keyword-tag"
          :style="{
            fontSize: Math.min(12 + item.value * 1.8, 26) + 'px',
            opacity: Math.max(0.5, Math.min(1, item.value / 10 + 0.4))
          }"
        >
          {{ item.name }}
          <sup class="kw-count">{{ item.value }}</sup>
        </span>
      </div>
      <el-empty v-else description="暂无关键词数据" :image-size="80" />
    </div>
  </div>
</template>

<style lang="less" scoped>
@import '@/assets/design-system.less';

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(440px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card, .keyword-card {
  padding: 24px;
  transition: var(--ds-transition);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--ds-shadow-md);
  }
}

.chart-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--ds-text-1);
  margin-bottom: 20px;

  .chart-icon { color: var(--ds-primary); }
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
  padding: 16px 0;
  min-height: 160px;

  .keyword-tag {
    font-weight: 700;
    color: var(--ds-primary);
    cursor: pointer;
    transition: var(--ds-transition);
    display: inline-flex;
    align-items: baseline;
    gap: 2px;

    &:nth-child(3n+1) { color: var(--ds-primary); }
    &:nth-child(3n+2) { color: var(--ds-accent); }
    &:nth-child(3n) { color: #F59E0B; }

    &:hover { transform: scale(1.12); opacity: 1 !important; }

    .kw-count {
      font-size: 10px;
      font-weight: 500;
      opacity: 0.6;
    }
  }
}
</style>
