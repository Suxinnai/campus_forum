export function createLineChartOption(dailyPosts = []) {
  const dates = dailyPosts.map(item => item.date.slice(5))
  const counts = dailyPosts.map(item => item.count)

  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    grid: { left: '2%', right: '4%', bottom: '2%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
    series: [
      {
        name: '发帖量',
        type: 'line',
        smooth: true,
        data: counts,
        itemStyle: { color: '#0ea5e9' },
        lineStyle: { width: 3, shadowColor: 'rgba(14,165,233,0.3)', shadowBlur: 8, shadowOffsetY: 4 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(14,165,233,0.2)' }, { offset: 1, color: 'rgba(14,165,233,0)' }]
          }
        }
      }
    ]
  }
}

export function createPieChartOption(categoryDistribution = []) {
  const data = categoryDistribution.length
    ? categoryDistribution
    : [{ name: '暂无数据', value: 1 }]

  return {
    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', textStyle: { color: '#334155' }, formatter: '{b}: {c}篇 ({d}%)' },
    legend: { bottom: '0%', left: 'center', itemWidth: 8, itemHeight: 8, icon: 'circle', textStyle: { color: '#64748b', fontSize: 12 } },
    series: [
      {
        name: '内容分类占比',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#0ea5e9' } },
        labelLine: { show: false },
        data,
        color: ['#0ea5e9', '#14b8a6', '#38bdf8', '#f59e0b', '#2dd4bf', '#7dd3fc', '#a78bfa']
      }
    ]
  }
}
