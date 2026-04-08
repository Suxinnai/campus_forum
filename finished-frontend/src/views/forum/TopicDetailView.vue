<script setup>
import {get, post, del} from "@/net/api.js";
import {useRoute} from "vue-router";
import {reactive, ref, onMounted, nextTick} from "vue";
import {
  ArrowLeft, EditPen, Female, Male, Pointer, Star,
  Plus, ChatSquare, Delete, Share, Promotion, User
} from "@element-plus/icons-vue";
import router from "@/router/index.js";
import TopicTag from "@/components/TopicTag.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {useAppStore} from "@/stores/app-store.js";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicCommentEditor from "@/components/TopicCommentEditor.vue";
import DOMPurify from "dompurify";

const route = useRoute();
const store = useAppStore();
const loading = ref(true);

const topic = reactive({
  data: null,
  like: false,
  collect: false,
  comments: null,
  page: 1
})

const tid = route.params.tid;

function init() {
  loading.value = true;
  get(`/api/forum/topic?tid=${tid}`, data => {
    topic.data = data;
    topic.like = data.interact?.like || false;
    topic.collect = data.interact?.collect || false;
    loadComments(1);
    loading.value = false;
  }, () => {
    ElMessage.error({message: "网络异常，无法获取详情", plain: true});
    loading.value = false;
  })
}

onMounted(() => init());

import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

// 鲁棒内容渲染器：处理 JSON 泄露的关键
const renderContent = (content) => {
  if (!content) return "";
  let html = "";
  try {
    const parsed = typeof content === 'string' ? JSON.parse(content) : content;

    if (parsed.md) return "";

    if (parsed.ops) {
      let result = "";
      parsed.ops.forEach(op => {
        if (typeof op.insert === 'string') {
          // 先转义基础 HTML 实体
          let text = op.insert.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
          if (op.attributes) {
             if (op.attributes.bold) text = `<b>${text}</b>`;
             if (op.attributes.link) {
               const safeHref = DOMPurify.sanitize(op.attributes.link);
               text = `<a href="${safeHref}" style="color:#409eff" target="_blank" rel="noopener noreferrer">${text}</a>`;
             }
          }
          result += text;
        }
      });
      html = result.replace(/\n/g, '<br/>');
    }
  } catch(e) {
    // 解析失败，转义后返回
    html = content.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }
  return DOMPurify.sanitize(html);
}

const isMarkdown = (content) => {
  try {
    const p = typeof content === 'string' ? JSON.parse(content) : content;
    return !!p.md;
  } catch(e) { return false; }
}

const extractMd = (content) => {
  try {
    const p = typeof content === 'string' ? JSON.parse(content) : content;
    return p.md || "";
  } catch(e) { return ""; }
}

const extractImages = (content) => {
  try {
    const p = typeof content === 'string' ? JSON.parse(content) : content;
    return p.images || [];
  } catch(e) { return []; }
}

const edit = ref(false)
const commentEditorRef = ref(null)
const comment = reactive({
  quote: null
})

function interact(type, message) {
  post(`/api/forum/interact?tid=${tid}&type=${type}&state=${!topic[type]}`, null, () => {
    topic[type] = !topic[type];
    if (topic[type]) {
       topic.data[type]++;
    } else {
       topic.data[type]--;
    }
    ElMessage.success({message: `${message}成功`, plain: true});
  })
}

function deleteTopic() {
  ElMessageBox.confirm('确定要永久删除这篇帖子吗？', '删除提醒', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(() => {
    del(`/api/forum/delete-topic?id=${tid}`, () => {
      ElMessage.success({message: '帖子已删除', plain: true})
      router.push('/home')
    })
  })
}

async function copyUrl() {
  await navigator.clipboard.writeText(window.location.href);
  ElMessage.success({message: "链接已复制", plain: true})
}

function loadComments(page) {
  topic.page = page
  get(`/api/forum/comments?tid=${tid}&page=${page - 1}`, data => {
    topic.comments = data;
  })
}

function onCommentAdd() {
    comment.quote = null;
    init(); // 刷新数据
}

function openCommentEditor(reply = null) {
  comment.quote = reply
  nextTick(() => {
    commentEditorRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

function deleteComment(id) {
  del(`/api/forum/delete-comment?id=${id}`, () => {
    ElMessage.success({message: "已删除", plain: true})
    loadComments(topic.page)
  })
}

function relativeTime(dateStr) {
  const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
  if (diff < 60) return '刚刚';
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`;
  return `${Math.floor(diff / 86400)} 天前`;
}
</script>

<template>
  <div class="clean-topic-view" v-if="topic.data">
    <!-- 极简顶部导航 -->
    <div class="top-nav">
      <div class="nav-content">
        <el-button :icon="ArrowLeft" link @click="router.push('/home')">返回社区</el-button>
        <el-divider direction="vertical" />
        
        <!-- 动态话题标签 (详情页顶部) -->
        <div class="detail-tags-area" v-if="topic.data.tags && topic.data.tags.length">
            <span class="main-det-tag">{{ topic.data.tags[0] }}</span>
            <span v-for="tag in topic.data.tags.slice(1)" :key="tag" class="sub-det-tag">#{{ tag }}</span>
        </div>
        <span v-else class="main-det-tag" style="background: #f1f5f9; color: #64748b; border-color: #e2e8f0">未分类</span>
        
        <h1 class="main-title">{{ topic.data.title }}</h1>
      </div>
    </div>

    <div class="main-layout">
      <!-- 左侧：稳健排版的文章 -->
      <div class="article-section">
        <div class="article-card">
          <!-- 作者条 -->
          <div class="author-strip">
            <el-avatar :src="store.getAvatar(topic.data.user.avatar)" :size="44" />
            <div class="author-meta">
              <div class="a-name">
                {{ topic.data.user.username }}
                <el-icon v-if="topic.data.user.gender === 1" color="#f43f5e"><Female /></el-icon>
                <el-icon v-if="topic.data.user.gender === 0" color="#3b82f6"><Male /></el-icon>
              </div>
              <div class="a-time">发布于 {{ new Date(topic.data.time).toLocaleString() }}</div>
            </div>
            <div style="flex: 1"></div>
            <div class="top-interact" v-if="store.user.id === topic.data.user.id || store.user.role === 'admin'">
              <el-button v-if="store.user.id === topic.data.user.id" :icon="EditPen" circle size="small" @click="edit = true"></el-button>
              <el-button :icon="Delete" circle size="small" type="danger" plain @click="deleteTopic"></el-button>
            </div>
          </div>

          <el-divider style="margin: 20px 0" />

          <!-- 正文区 -->
          <div class="post-body">
            <template v-if="isMarkdown(topic.data.content)">
              <MdPreview :modelValue="extractMd(topic.data.content)" />
            </template>
            <div v-else class="html-content" v-html="renderContent(topic.data.content)"></div>
            
            <div class="image-gallery" v-if="extractImages(topic.data.content).length">
               <el-image v-for="img in extractImages(topic.data.content)" :src="img" :preview-src-list="extractImages(topic.data.content)" fit="cover" class="post-img" />
            </div>
          </div>

          <!-- 底部超稳交互条 -->
          <div class="footer-actions">
            <div class="btn-group">
              <button class="action-btn" :class="{active: topic.like}" @click="interact('like', '点赞')">
                <Pointer style="width:16px" /> <span>{{ topic.data.like || 0 }}</span>
              </button>
              <button class="action-btn" :class="{active: topic.collect}" @click="interact('collect', '收藏')">
                <Star style="width:16px" /> <span>{{ topic.collect ? '已收藏' : '收藏' }}</span>
              </button>
              <button class="action-btn" @click="copyUrl">
                <Share style="width:16px" /> <span>分享</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 稳健评论区 -->
        <div class="comment-card">
          <div class="c-header">
            <div class="h-label">讨论区 ({{ topic.data.comments }})</div>
            <el-button type="primary" size="small" :icon="Plus" @click="openCommentEditor()">发表评论</el-button>
          </div>

          <div ref="commentEditorRef" class="comment-editor-wrap">
            <topic-comment-editor
              @comment="onCommentAdd"
              @close="comment.quote = null"
              :tid="tid"
              :quote="comment.quote"
            />
          </div>

          <div class="c-list">
            <div v-for="item in topic.comments" :key="item.id" class="c-item">
              <el-avatar :src="store.getAvatar(item.user.avatar)" :size="40" />
              <div class="c-content">
                <div class="c-user-info">
                  <span class="c-un">{{ item.user.username }}</span>
                  <span class="c-tm">{{ relativeTime(item.time) }}</span>
                </div>
                <div v-if="item.quote" class="c-quote">
                  「 {{ item.quote }} 」
                </div>
                <div class="c-text">
                  <template v-if="isMarkdown(item.content)">
                    <MdPreview :modelValue="extractMd(item.content)" style="font-size:14px" />
                  </template>
                  <div v-else v-html="renderContent(item.content)"></div>
                </div>
                <div class="c-actions">
                  <el-button link size="small" @click="openCommentEditor(item)">回复</el-button>
                  <el-button v-if="item.user.id === store.user.id" link type="danger" size="small" @click="deleteComment(item.id)">删除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="!topic.comments?.length" description="暂无评论" :image-size="60" />
          </div>
          
          <div class="pagination">
             <el-pagination background layout="prev, pager, next" v-model:current-page="topic.page" @current-change="loadComments" :total="topic.data.comments" :page-size="10" hide-on-single-page />
          </div>
        </div>
      </div>

      <!-- 右侧：作者卡片（稳健版） -->
      <div class="sidebar-section">
        <div class="user-card-stable">
           <div class="u-avatar-box">
             <el-avatar :src="store.getAvatar(topic.data.user.avatar)" :size="64" />
           </div>
           <div class="u-info">
              <div class="u-name">{{ topic.data.user.username }}</div>
              <div class="u-desc">{{ topic.data.user.desc || '这个同学没有留下简介' }}</div>
           </div>
           <el-divider style="margin: 15px 0" />
           <div class="u-details">
             <div class="u-row"><el-icon><Promotion /></el-icon> <span>{{ topic.data.user.email || '保密' }}</span></div>
           </div>
        </div>
      </div>
    </div>

    <!-- 弹出式编辑 -->
    <topic-editor
        :show="edit"
        :default-text="topic.data.content"
        :default-title="topic.data.title"
        :default-type="topic.data.type"
        @close="edit = false;init()"
        :tid="tid" />
  </div>
</template>

<style lang="less" scoped>
.clean-topic-view {
  max-width: 1140px;
  margin: 0 auto;
  padding: 20px 15px 100px;
}

.top-nav {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 12px 20px;
  margin-bottom: 20px;
  .nav-content {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .main-title {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
    color: #303133;
    word-break: break-word;
    line-height: 1.4;
  }
}

.main-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
}

.article-card, .comment-card, .user-card-stable {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 24px;
}

.author-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  .a-name { font-weight: 800; font-size: 15px; display: flex; align-items: center; gap: 4px; }
  .a-time { font-size: 12px; color: #909399; margin-top: 2px; }
}

.post-body {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  .html-content { word-break: break-word; }
}

.detail-tags-area {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 12px;
}

.main-det-tag {
  font-size: 13px;
  font-weight: 900;
  color: #f43f5e;
  background: #fff1f2;
  padding: 4px 14px;
  border-radius: 100px;
  border: 1px solid #ffe4e6;
  white-space: nowrap;
}

.sub-det-tag {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  background: #f8fafc;
  padding: 3px 10px;
  border-radius: 8px;
  opacity: 0.8;
  white-space: nowrap;
}

.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
  .post-img { max-width: 100%; height: auto; border-radius: 8px; border: 1px solid #f2f2f2; }
}

.footer-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f2f2f2;
}

.btn-group { display: flex; gap: 10px; }

.action-btn {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  
  &:hover { background: #ecf5ff; color: #409eff; border-color: #c6e2ff; }
  &.active { background: #409eff; color: #fff; border-color: #409eff; }
}

.comment-card { margin-top: 20px; }

.comment-editor-wrap {
  margin-bottom: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid #f2f2f2;
}

.c-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #f2f2f2;
  margin-bottom: 20px;
  .h-label { font-weight: 800; font-size: 16px; }
}

.c-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #fafafa;
  &:last-child { border-bottom: none; }
}

.c-content { flex: 1; }

.c-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  .c-un { font-weight: 700; font-size: 14px; color: #303133; }
  .c-tm { font-size: 12px; color: #909399; }
}

.c-quote {
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  border-left: 3px solid #dcdfe6;
}

.c-text { font-size: 14px; line-height: 1.6; color: #606266; }

.c-actions { margin-top: 8px; }

.pagination { margin-top: 20px; display: flex; justify-content: center; }

.user-card-stable {
  text-align: center;
  .u-info { margin-top: 15px; }
  .u-name { font-weight: 800; font-size: 16px; }
  .u-desc { font-size: 12px; color: #909399; margin-top: 6px; line-height: 1.4; }
  .u-details { text-align: left; font-size: 13px; color: #606266; .u-row { display: flex; align-items: center; gap: 8px; } }
}

@media (max-width: 800px) {
  .main-layout { grid-template-columns: 1fr; }
  .sidebar-section { display: none; }
}
</style>
