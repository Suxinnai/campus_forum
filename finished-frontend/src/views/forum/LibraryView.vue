<script setup>
import {ref} from 'vue'
import {get} from '@/net/api.js'
import LightCard from '@/components/LightCard.vue'
import {Search} from '@element-plus/icons-vue'

const list = ref([])
const keyword = ref('')
const category = ref('')
const categories = ref([])
const detail = ref(null)
const showDetail = ref(false)

function loadBooks() {
  get(`/api/book/list?page=1&keyword=${keyword.value}&category=${category.value}`, data => list.value = data)
}
loadBooks()

get('/api/book/categories', data => categories.value = data)

function viewDetail(item) {
  detail.value = item
  showDetail.value = true
}
</script>

<template>
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <h2>📚 在线图书馆</h2>
    <light-card style="padding:15px;margin-bottom:15px;display:flex;gap:10px;align-items:center">
      <el-input v-model="keyword" placeholder="搜索书名或作者..." :prefix-icon="Search" style="flex:1" @keyup.enter="loadBooks"/>
      <el-select v-model="category" placeholder="全部分类" clearable style="width:150px" @change="loadBooks">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c"/>
      </el-select>
      <el-button type="primary" @click="loadBooks">搜索</el-button>
    </light-card>

    <div style="display:grid;grid-template-columns:repeat(3, 1fr);gap:12px">
      <light-card v-for="book in list" :key="book.id" style="padding:15px;cursor:pointer;transition:.3s"
                  @click="viewDetail(book)">
        <div style="font-size:15px;font-weight:bold;margin-bottom:5px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
          {{book.title}}
        </div>
        <div style="font-size:13px;color:#666;margin-bottom:3px">{{book.author}}</div>
        <div style="font-size:12px;color:#999;margin-bottom:8px">{{book.category}}</div>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <el-tag :type="book.available ? 'success' : 'info'" size="small">{{book.available ? '可借' : '已借出'}}</el-tag>
          <span style="font-size:12px;color:#999">{{book.location}}</span>
        </div>
      </light-card>
    </div>
    <el-empty v-if="!list.length" description="暂无图书信息"/>

    <el-dialog v-model="showDetail" :title="detail?.title" width="500">
      <div v-if="detail">
        <div style="margin-bottom:10px">
          <el-tag :type="detail.available ? 'success' : 'info'">{{detail.available ? '可借阅' : '已借出'}}</el-tag>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="作者">{{detail.author}}</el-descriptions-item>
          <el-descriptions-item label="ISBN">{{detail.isbn || '-'}}</el-descriptions-item>
          <el-descriptions-item label="分类">{{detail.category}}</el-descriptions-item>
          <el-descriptions-item label="馆藏位置">{{detail.location || '-'}}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:15px;font-size:14px;color:#555;line-height:1.6" v-if="detail.description">
          <strong>简介：</strong>{{detail.description}}
        </div>
      </div>
    </el-dialog>
  </div>
</template>
