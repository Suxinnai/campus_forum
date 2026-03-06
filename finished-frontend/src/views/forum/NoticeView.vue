<script setup>
import {ref} from 'vue'
import {get} from '@/net/api.js'
import LightCard from '@/components/LightCard.vue'

const list = ref([])
const detail = ref(null)
const showDetail = ref(false)

get('/api/notice/list', data => list.value = data)

function viewDetail(id) {
  get(`/api/notice/detail?id=${id}`, data => {
    detail.value = data
    showDetail.value = true
  })
}
</script>

<template>
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <h2>📢 教务通知</h2>
    <el-timeline style="padding-left:5px">
      <el-timeline-item v-for="item in list" :key="item.id"
                        :timestamp="new Date(item.createTime).toLocaleDateString()"
                        :type="item.isTop ? 'danger' : 'primary'"
                        placement="top">
        <light-card style="cursor:pointer;padding:12px" @click="viewDetail(item.id)">
          <div style="display:flex;align-items:center;gap:8px">
            <el-tag v-if="item.isTop" type="danger" size="small">置顶</el-tag>
            <span style="font-weight:bold">{{item.title}}</span>
          </div>
          <div style="margin-top:6px;font-size:13px;color:#999">发布者: {{item.publisher}}</div>
        </light-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!list.length" description="暂无教务通知"/>

    <el-dialog v-model="showDetail" :title="detail?.title" width="600">
      <div v-if="detail">
        <div style="color:#999;font-size:13px;margin-bottom:15px">
          {{detail.publisher}} · {{new Date(detail.createTime).toLocaleString()}}
        </div>
        <div style="font-size:14px;line-height:1.8;white-space:pre-wrap">{{detail.content}}</div>
      </div>
    </el-dialog>
  </div>
</template>
