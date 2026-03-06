<script setup>
import {ref} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import {Calendar, Location, User} from '@element-plus/icons-vue'

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
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <h2 style="margin-bottom:15px">🎉 校园活动</h2>
    <div style="display:flex;flex-direction:column;gap:15px">
      <light-card v-for="item in list" :key="item.id" style="padding:20px">
        <div style="display:flex;justify-content:space-between;align-items:flex-start">
          <div>
            <h3 style="margin:0 0 8px 0">{{item.title}}</h3>
            <div style="color:#666;font-size:14px;margin-bottom:5px">
              <el-icon><Calendar/></el-icon> {{formatDate(item.startTime)}} ~ {{formatDate(item.endTime)}}
            </div>
            <div style="color:#666;font-size:14px;margin-bottom:5px" v-if="item.location">
              <el-icon><Location/></el-icon> {{item.location}}
            </div>
            <div style="color:#666;font-size:14px" v-if="item.organizer">
              <el-icon><User/></el-icon> 主办: {{item.organizer}}
            </div>
          </div>
          <div style="text-align:center;min-width:100px">
            <div style="font-size:24px;font-weight:bold;color:#409eff">{{item.currentPeople}}</div>
            <div style="font-size:12px;color:grey">/ {{item.maxPeople || '不限'}} 人</div>
            <el-button type="primary" size="small" style="margin-top:8px"
                       :disabled="item.maxPeople > 0 && item.currentPeople >= item.maxPeople"
                       @click="joinActivity(item.id)">
              {{item.maxPeople > 0 && item.currentPeople >= item.maxPeople ? '已满' : '立即报名'}}
            </el-button>
          </div>
        </div>
        <el-divider style="margin:12px 0"/>
        <div style="font-size:14px;color:#555">{{item.content}}</div>
      </light-card>
      <el-empty v-if="!list.length" description="暂无校园活动"/>
    </div>
  </div>
</template>
