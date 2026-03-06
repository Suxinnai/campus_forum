<script setup>

import LightCard from "@/components/LightCard.vue";
import {
  ArrowRight, ArrowRightBold,
  Avatar,
  Calendar,
  Clock,
  CollectionTag,
  Compass,
  Document,
  Edit,
  EditPen, FolderOpened,
  Guide,
  Picture, Pointer, Star
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, reactive, ref, watch} from "vue";
import {get} from "@/net/api.js";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useCounterStore} from "@/stores/counter.js";
import axios from "axios";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";

const store = useCounterStore();

const today = computed(() => {
  const date = new Date();
  return `${date.getFullYear()} 年 ${date.getMonth()+1} 月 ${date.getDate()} 日`
})

const editor = ref();

const topics = reactive({
  list: [],
  type: 0,
  page: 0,
  end: false,
  top: []
})

function updateList() {
  if (topics.end) return
  get(`/api/forum/list-topic?page=${topics.page}&type=${topics.type}`, data => {
    if (data) {
      data.forEach(item => topics.list.push(item))
      topics.page++;
    }
    if (!data || data.length < 5) topics.end = true;
  })
}

get("/api/forum/top-topic", data => topics.top = data)

function onTopicCreate() {
  editor.value = false;
  resetList()
}

function resetList(){
  topics.page = 0
  topics.list = []
  topics.end = false;
  updateList()
}

const collects = ref(false);

watch(() => topics.type, () => {
  resetList()
}, {
  immediate: true
})

const weather = reactive({
  location: {},
  now: {},
  hourly: {},
  success: false
})

// 直接使用重庆坐标获取天气，无需浏览器地理位置授权
get("/api/forum/weather?longitude=106.55&latitude=29.56", (data) => {
  Object.assign(weather, data)
  weather.success = true;
}, () => {
  // 后端天气接口失败时，显示静态兜底数据
  weather.location = { name: '重庆', adm2: '重庆', adm1: '重庆市', country: '中国' }
  weather.now = { text: '多云', temp: '18', feelsLike: '17', windDir: '东北风', windScale: '2', humidity: '65', icon: '104' }
  weather.hourly = []
  weather.success = true;
})
</script>

<template>
  <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px">
    <div style="flex: 1">
      <light-card>
        <div class="create-topic" @click="editor = true">
          <el-icon><EditPen/></el-icon>点击发表帖子...
        </div>
        <div style="margin-top: 10px;display: flex;gap: 13px;font-size: 18px;color: grey">
          <el-icon><Edit/></el-icon>
          <el-icon><Document/></el-icon>
          <el-icon><Compass/></el-icon>
          <el-icon><Picture/></el-icon>
        </div>
      </light-card>
      <light-card style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
        <div v-for="item in topics.top" class="top-topic" @click="router.push(`/index/topic-detail/${item.id}`)">
          <el-tag type="info" size="small">置顶</el-tag>
          <div>{{item.title.length > 22 ? item.title.substring(0, 22)+'...' : item.title}}</div>
          <div>{{new Date(item.time).toLocaleString()}}</div>
        </div>
      </light-card>
      <light-card style="margin-top: 10px;display: flex;gap: 7px;flex-wrap: wrap">
        <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`" v-for="item in store.forum.types" @click="topics.type = item.id;">
          <color-dot :color="item.color" />
          <span style="margin-left: 5px">{{item.name}}</span>
        </div>
      </light-card>
      <transition name="el-fade-in" mode="out-in">
        <div v-if="topics.list.length">
          <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px" v-infinite-scroll="updateList">
            <light-card v-for="item in topics.list" class="topic-card" @click="router.push('/index/topic-detail/'+item.id)">
              <div style="display: flex">
                <div>
                  <el-avatar :size="30" :src="store.getAvatar(item.avatar)" />
                </div>
                <div style="margin-left: 7px">
                  <div style="font-size: 13px;font-weight: bold">{{item.username}}</div>
                  <div style="font-size: 12px;color: grey">
                    <el-icon><Clock/></el-icon>
                    <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">{{new Date(item.time).toLocaleString()}}</div>
                  </div>
                </div>
              </div>
              <div style="margin-top: 5px">
                <TopicTag :type="item.type"/>
                <span style="font-weight: bold;margin-left: 7px">{{item.title.length > 28 ? item.title.substring(0, 28)+'...' : item.title}}</span>
              </div>
              <div class="topic-content">{{item.text}}</div>
              <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"/>
              </div>
              <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 10px;opacity: 0.8">
                <div>
                  <el-icon style="vertical-align: middle"><Pointer/></el-icon> {{item.like}}点赞
                </div>
                <div>
                  <el-icon style="vertical-align: middle"><Star/></el-icon> {{item.collect}}收藏
                </div>
              </div>
            </light-card>
          </div>
        </div>
      </transition>
    </div>
    <div style="width: 280px">
      <div style="position: sticky;top: 20px">
        <light-card>
          <div class="collect-list-button" @click="collects = true">
            <span><el-icon><FolderOpened/></el-icon>查看我的收藏</span>
            <el-icon style="transform: translateY(3px)"><ArrowRightBold/></el-icon>
          </div>

        </light-card>
        <light-card style="margin-top: 10px">
          <div style="font-weight: bold">
            <el-icon><CollectionTag/></el-icon>
            论坛公告
          </div>
          <el-divider style="margin: 10px 0"/>
          <div style="font-size: 14px;color: grey;margin: 10px">
            各位在发帖的时候请注意设置图片大小,建议设置宽度为500px,否则页面展示会过大。
          </div>
        </light-card>
        <light-card style="margin-top: 10px">
          <div style="font-weight: bold">
            <el-icon><Calendar/></el-icon>
            天气信息
          </div>
          <el-divider style="margin: 10px 0"/>
          <Weather :data="weather"></Weather>
        </light-card>
        <light-card style="margin-top: 10px">
          <div class="info-text">
            <div>当前日期</div>
            <div>{{today}}</div>
          </div>
          <div class="info-text">
            <div>当前IP地址</div>
            <div>127.0.0.1</div>
          </div>
        </light-card>
        <div style="font-size: 14px;margin-top: 10px;color: grey">
          <el-icon><Guide/></el-icon>
          友情链接
          <el-divider style="margin: 10px 0"/>
        </div>
        <div style="display: grid;grid-template-columns: repeat(2, 1fr);grid-gap: 10px;margin-top: 10px;">
          <a class="friend-link" href="https://www.edu.cn" target="_blank">
            <div class="friend-link-text">🎓 中国教育网</div>
          </a>
          <a class="friend-link" href="https://www.cnki.net" target="_blank">
            <div class="friend-link-text">📚 中国知网</div>
          </a>
          <a class="friend-link" href="https://github.com" target="_blank">
            <div class="friend-link-text">💻 GitHub</div>
          </a>
          <a class="friend-link" href="https://element-plus.org" target="_blank">
            <div class="friend-link-text">🎨 Element Plus</div>
          </a>
        </div>
      </div>
    </div>
    <TopicEditor :show="editor" @success="onTopicCreate" @close="editor = false"/>
    <TopicCollectList :show="collects" @close="collects = false"/>
  </div>
</template>

<style lang="less" scoped>
.collect-list-button {
  font-size: 14px;
  display: flex;
  justify-content: space-between;
  transition: .3s;

  &:hover {
    cursor: pointer;
    opacity: 0.5;
  }
}
.top-topic {
  display: flex;

  div:first-of-type{
    font-size: 14px;
    margin-left: 10px;
    font-weight: bold;
    opacity: 0.8;
    transition: color .3s;

    &:hover {
      color: grey;
    }
  }

  div:nth-of-type(2) {
    flex: 1;
    color: grey;
    font-size: 13px;
    text-align: right;
  }

  &:hover {
    cursor: pointer;
  }
}

.type-select-card {
  background-color: #f5f5f5;
  padding: 2px 7px;
  font-size: 14px;
  border-radius: 3px;
  box-sizing: border-box;
  transition: background-color .3s;

  &.active {
    border: solid 1px #ead4c4;
  }

  &:hover {
    cursor: pointer;
    background-color: #dadada;
  }
}

.topic-card {
  padding: 15px;
  transition: scale .3s;

  &:hover {
    scale: 1.015;
    cursor: pointer;
  }

  .topic-content {
    font-size: 13px;
    color: grey;
    margin: 5px 0;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .topic-image {
    width: 100%;
    height: 100%;
    max-height: 110px;
    border-radius: 5px;
  }
}

.info-text {
  display: flex;
  justify-content: space-between;
  color: grey;
  font-size: 14px;
}
.friend-link {
  border-radius: 5px;
  overflow: hidden;
  background: #f0f2f5;
  text-decoration: none;
  color: inherit;
  transition: .3s;

  &:hover {
    opacity: 0.7;
    cursor: pointer;
  }
}

.friend-link-text {
  padding: 10px;
  font-size: 13px;
  text-align: center;
}
.create-topic {
  background-color: #efefef;
  border-radius: 5px;
  height: 40px;
  color: grey;
  font-size: 14px;
  line-height: 40px;
  padding: 0 10px;

  &:hover {
    cursor: pointer;
  }
}

.dark {
  .create-topic {
    background-color: #212225;
  }

  .type-select-card {
    background-color: #282828;

    &.active {
      border: solid 1px #64594b;
    }

    &:hover {
      background-color: #5e5e5e;
    }
  }
}
</style>