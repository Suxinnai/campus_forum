<script setup>
import {ref, reactive} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'

const list = ref([])
const showPublish = ref(false)
const form = reactive({content: '', anonymous: 1})

function loadList() {
  get('/api/confession/list?page=1', data => list.value = data)
}
loadList()

function publish() {
  if (!form.content) return ElMessage.warning('请输入内容')
  post('/api/confession/publish', form, () => {
    ElMessage.success('发布成功')
    showPublish.value = false
    form.content = ''
    loadList()
  })
}

function likeIt(id) {
  post(`/api/confession/like?id=${id}`, {}, () => loadList())
}
</script>

<template>
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px">
      <h2 style="margin:0">💕 表白墙</h2>
      <el-button type="primary" @click="showPublish=true">写一条</el-button>
    </div>
    <div style="display:grid;grid-template-columns:repeat(2, 1fr);gap:12px">
      <light-card v-for="item in list" :key="item.id" style="padding:15px">
        <div style="font-size:14px;line-height:1.6;min-height:60px">{{item.content}}</div>
        <el-divider style="margin:8px 0"/>
        <div style="display:flex;justify-content:space-between;align-items:center;font-size:12px;color:grey">
          <span>{{item.username}} · {{new Date(item.createTime).toLocaleDateString()}}</span>
          <el-button text size="small" @click="likeIt(item.id)">❤️ {{item.likes}}</el-button>
        </div>
      </light-card>
    </div>
    <el-empty v-if="!list.length" description="还没有人表白，快来第一个吧~"/>

    <el-dialog v-model="showPublish" title="发表心情" width="450">
      <el-input type="textarea" v-model="form.content" :rows="5" placeholder="说出你想说的话..."/>
      <div style="margin-top:10px">
        <el-checkbox v-model="form.anonymous" :true-value="1" :false-value="0">匿名发布</el-checkbox>
      </div>
      <template #footer>
        <el-button @click="showPublish=false">取消</el-button>
        <el-button type="primary" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>
