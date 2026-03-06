<script setup>
import {ref, computed} from 'vue'
import LightCard from '@/components/LightCard.vue'

const examDate = new Date('2026-12-20')
const now = ref(new Date())
setInterval(() => now.value = new Date(), 1000)

const countdown = computed(() => {
  const diff = examDate - now.value
  if (diff <= 0) return {days: 0, hours: 0, minutes: 0, seconds: 0}
  return {
    days: Math.floor(diff / 86400000),
    hours: Math.floor((diff % 86400000) / 3600000),
    minutes: Math.floor((diff % 3600000) / 60000),
    seconds: Math.floor((diff % 60000) / 1000)
  }
})

const partners = [
  {name: '海文考研', desc: '全国连锁考研辅导机构，提供全科目考研培训服务', tag: '战略合作'},
  {name: '中公教育', desc: '综合性教育培训集团，覆盖考研、公考等多领域', tag: '合作机构'},
  {name: '新东方在线', desc: '在线教育平台，提供考研英语、政治等课程', tag: '合作机构'}
]

const tips = [
  '📖 制定合理的复习计划，保持每日学习节奏',
  '🎯 明确目标院校和专业，做好信息收集',
  '💪 坚持锻炼身体，保持良好的身心状态',
  '📝 多做真题，归纳总结高频考点',
  '🤝 找到志同道合的研友，互相鼓励监督'
]
</script>

<template>
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <h2>🎓 海文考研</h2>

    <light-card style="text-align:center;padding:30px;margin-bottom:15px">
      <div style="font-size:16px;color:#666;margin-bottom:10px">距离 2026 年全国硕士研究生入学考试还有</div>
      <div style="display:flex;justify-content:center;gap:15px">
        <div class="countdown-item">
          <div class="countdown-num">{{countdown.days}}</div><div class="countdown-label">天</div>
        </div>
        <div class="countdown-item">
          <div class="countdown-num">{{countdown.hours}}</div><div class="countdown-label">时</div>
        </div>
        <div class="countdown-item">
          <div class="countdown-num">{{countdown.minutes}}</div><div class="countdown-label">分</div>
        </div>
        <div class="countdown-item">
          <div class="countdown-num">{{countdown.seconds}}</div><div class="countdown-label">秒</div>
        </div>
      </div>
    </light-card>

    <h3>🏢 合作机构</h3>
    <div style="display:flex;flex-direction:column;gap:10px;margin-bottom:20px">
      <light-card v-for="p in partners" :key="p.name" style="padding:15px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <span style="font-weight:bold;font-size:16px">{{p.name}}</span>
            <el-tag size="small" type="warning" style="margin-left:8px">{{p.tag}}</el-tag>
          </div>
        </div>
        <div style="margin-top:6px;color:#666;font-size:14px">{{p.desc}}</div>
      </light-card>
    </div>

    <h3>💡 考研小贴士</h3>
    <light-card style="padding:15px">
      <ul style="margin:0;padding-left:20px">
        <li v-for="tip in tips" :key="tip" style="margin-bottom:8px;font-size:14px;color:#555">{{tip}}</li>
      </ul>
    </light-card>
  </div>
</template>

<style scoped>
.countdown-item { text-align: center; }
.countdown-num {
  font-size: 42px; font-weight: bold; color: #409eff;
  background: #ecf5ff; border-radius: 8px; padding: 5px 15px; min-width: 50px;
}
.countdown-label { font-size: 14px; color: grey; margin-top: 4px; }
</style>
