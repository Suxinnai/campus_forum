<script setup>
import {ref, reactive} from 'vue'
import {get, post} from '@/net/api.js'
import {ElMessage} from 'element-plus'
import LightCard from '@/components/LightCard.vue'
import {useCounterStore} from '@/stores/counter.js'

const store = useCounterStore()
const filterType = ref('')
const list = ref([])
const showPublish = ref(false)
const form = reactive({title: '', content: '', type: 'lost', contact: ''})

function loadList() {
  get(`/api/lost-found/list?page=1&type=${filterType.value}`, data => list.value = data)
}
loadList()

function publish() {
  if (!form.title || !form.content) return ElMessage.warning('请填写完整信息')
  post('/api/lost-found/publish', form, () => {
    ElMessage.success('发布成功')
    showPublish.value = false
    form.title = ''; form.content = ''; form.contact = ''
    loadList()
  })
}

function closeItem(id) {
  get(`/api/lost-found/close?id=${id}`, () => { ElMessage.success('已关闭'); loadList() })
}
</script>

<template>
  <div style="max-width:900px;margin:20px auto;padding:0 15px">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px">
      <h2 style="margin:0">🔍 失物招领</h2>
      <el-button type="primary" @click="showPublish=true">发布信息</el-button>
    </div>
    <el-radio-group v-model="filterType" @change="loadList" style="margin-bottom:15px">
      <el-radio-button value="">全部</el-radio-button>
      <el-radio-button value="lost">我丢了</el-radio-button>
      <el-radio-button value="found">我捡到</el-radio-button>
    </el-radio-group>
    <div style="display:flex;flex-direction:column;gap:10px">
      <light-card v-for="item in list" :key="item.id">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <el-tag :type="item.type==='lost'?'danger':'success'" size="small">{{item.type==='lost'?'丢失':'捡到'}}</el-tag>
            <el-tag v-if="item.status==='closed'" type="info" size="small" style="margin-left:5px">已关闭</el-tag>
            <span style="font-weight:bold;margin-left:8px">{{item.title}}</span>
          </div>
          <span style="color:grey;font-size:12px">{{new Date(item.createTime).toLocaleString()}}</span>
        </div>
        <div style="margin-top:8px;color:#666;font-size:14px">{{item.content}}</div>
        <div style="margin-top:5px;font-size:13px;color:#999" v-if="item.contact">📞 联系方式: {{item.contact}}</div>
      </light-card>
      <el-empty v-if="!list.length" description="暂无失物招领信息"/>
    </div>

    <el-dialog v-model="showPublish" title="发布失物招领" width="500">
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="lost">我丢了</el-radio>
            <el-radio value="found">我捡到</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="简要描述物品"/></el-form-item>
        <el-form-item label="详细描述"><el-input type="textarea" v-model="form.content" :rows="4" placeholder="描述物品特征、丢失/捡到地点等"/></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contact" placeholder="手机号/QQ/微信"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish=false">取消</el-button>
        <el-button type="primary" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>
