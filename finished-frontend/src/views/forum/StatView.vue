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
      title: {text: '📈 近7天发帖热度趋势', left: 'center'},
      tooltip: {trigger: 'axis'},
      xAxis: {
        type: 'category',
        data: data.map(item => item.date),
        axisLabel: {rotate: 30}
      },
      yAxis: {type: 'value', name: '发帖数'},
      series: [{
        data: data.map(item => item.count),
        type: 'line',
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              {offset: 0, color: 'rgba(64, 158, 255, 0.5)'},
              {offset: 1, color: 'rgba(64, 158, 255, 0.05)'}
            ]
          }
        },
        lineStyle: {color: '#409EFF', width: 3},
        itemStyle: {color: '#409EFF'}
      }]
    }
  })
}

// 加载分类占比数据
function loadCategoryPie() {
  get('/api/stat/category-pie', (data) => {
    pieOption.value = {
      title: {text: '📊 帖子板块分布', left: 'center'},
      tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
      legend: {orient: 'vertical', left: 'left', top: 'middle'},
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: {borderRadius: 10, borderColor: '#fff', borderWidth: 2},
        label: {show: true, formatter: '{b}\n{d}%'},
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
  <div style="padding: 20px; max-width: 1200px; margin: 0 auto;">
    <h2 style="margin-bottom: 20px;">📊 论坛数据可视化</h2>

    <el-row :gutter="20">
      <!-- 折线图：发帖热度趋势 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <v-chart v-if="lineOption" :option="lineOption" style="height: 350px; width: 100%;" autoresize />
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>

      <!-- 饼图：板块分布 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <v-chart v-if="pieOption" :option="pieOption" style="height: 350px; width: 100%;" autoresize />
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 词云：热门关键词 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <span style="font-weight: bold;">🔥 热门关键词</span>
      </template>
      <div v-if="wordCloudData.length" style="display: flex; flex-wrap: wrap; gap: 8px; padding: 10px;">
        <el-tag
            v-for="(item, index) in wordCloudData"
            :key="index"
            :type="['', 'success', 'info', 'warning', 'danger'][index % 5]"
            :size="item.value > 5 ? 'large' : (item.value > 2 ? 'default' : 'small')"
            :style="{fontSize: Math.min(12 + item.value * 2, 28) + 'px'}"
            effect="light"
            round
        >
          {{ item.name }} ({{ item.value }})
        </el-tag>
      </div>
      <el-empty v-else description="暂无关键词数据" />
    </el-card>
  </div>
</template>
