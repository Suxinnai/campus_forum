<script setup>
import {ref, computed} from 'vue'
import {get} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'

const studentId = ref('')
const grades = ref([])
const searched = ref(false)

function queryGrades() {
  if (!studentId.value) return ElMessage.warning('请输入学号')
  get(`/api/grade/query?studentId=${studentId.value}`, data => {
    grades.value = data
    searched.value = true
  }, (msg) => {
    grades.value = []
    searched.value = true
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
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <h2>📋 成绩查询</h2>
    <light-card style="padding:20px;margin-bottom:15px">
      <el-input v-model="studentId" placeholder="请输入学号（如 2022001）" style="max-width:300px" @keyup.enter="queryGrades">
        <template #append>
          <el-button @click="queryGrades">查询</el-button>
        </template>
      </el-input>
      <div style="margin-top:8px;font-size:12px;color:#999">提示: 演示学号 2022001 / 2022002</div>
    </light-card>

    <div v-if="searched && grades.length">
      <div style="display:flex;gap:15px;margin-bottom:15px">
        <light-card style="flex:1;text-align:center;padding:15px">
          <div style="font-size:14px;color:grey">课程数</div>
          <div style="font-size:28px;font-weight:bold;color:#409eff">{{grades.length}}</div>
        </light-card>
        <light-card style="flex:1;text-align:center;padding:15px">
          <div style="font-size:14px;color:grey">平均分</div>
          <div style="font-size:28px;font-weight:bold;color:#67c23a">{{avgScore}}</div>
        </light-card>
        <light-card style="flex:1;text-align:center;padding:15px">
          <div style="font-size:14px;color:grey">GPA</div>
          <div style="font-size:28px;font-weight:bold;color:#e6a23c">{{gpa}}</div>
        </light-card>
      </div>
      <light-card>
        <el-table :data="grades" stripe style="width:100%">
          <el-table-column prop="courseName" label="课程名称"/>
          <el-table-column prop="score" label="成绩" width="100" align="center">
            <template #default="{row}">
              <el-tag :type="row.score >= 90 ? 'success' : row.score >= 60 ? '' : 'danger'" size="small">{{row.score}}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="semester" label="学期" width="150" align="center"/>
        </el-table>
      </light-card>
    </div>
    <el-empty v-if="searched && !grades.length" description="未查询到该学号的成绩"/>
  </div>
</template>
