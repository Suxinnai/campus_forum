<script setup>
import {get, post, del} from "@/net/api.js";
import {useRoute} from "vue-router";
import {reactive, ref, onMounted, nextTick, computed} from "vue";
import { ArrowLeft, EditPen, Female, Male, Delete, Promotion } from "@element-plus/icons-vue";
import { ThumbsUp, Star, Share2, MessageCircle, Send, CornerDownRight } from "lucide-vue-next";
import router from "@/router/index.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {useAppStore} from "@/stores/app-store.js";
import TopicEditor from "@/components/TopicEditor.vue";
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

const route = useRoute();
const store = useAppStore();
const loading = ref(true);

const topic = reactive({ data: null, like: false, collect: false, comments: [], page: 1 });
const tid = parseInt(route.params.tid); // 转化为数字，确保 @Min(1) 验证通过

const commentText = ref("");
const showReplyId = ref(null);
const replyText = ref("");
const edit = ref(false);

function init() {
  loading.value = true;
  refreshTopicState(1, true);
}

function refreshTopicState(page = topic.page || 1, stopLoading = false) {
  get(`/api/forum/topic?tid=${tid}`, data => {
    topic.data = data;
    topic.like = data.interact?.like || false;
    topic.collect = data.interact?.collect || false;
    loadComments(page, stopLoading);
  }, () => {
    ElMessage.error("网络异常，无法获取详情");
    loading.value = false;
  });
}

onMounted(() => init());

// ======= 内容解析 =======
function decodeStructuredContent(content) {
  let decoded = content;
  for (let i = 0; i < 4; i++) {
    if (typeof decoded !== 'string') break;
    const normalized = decoded.trim();
    try {
      decoded = JSON.parse(normalized);
      continue;
    } catch (e) {
      if (/[\r\n\t]/.test(normalized)) {
        decoded = normalized
          .replace(/\r/g, '\\r')
          .replace(/\n/g, '\\n')
          .replace(/\t/g, '\\t');
        continue;
      }
      if (normalized.startsWith('"') && normalized.endsWith('"')) {
        decoded = normalized.slice(1, -1);
        continue;
      }
      if (normalized.includes('\\"')) {
        decoded = normalized
          .replace(/\\"/g, '"')
          .replace(/\r/g, '\\r')
          .replace(/\n/g, '\\n')
          .replace(/\t/g, '\\t')
          .replace(/\\\\/g, '\\');
        continue;
      }
      break;
    }
  }
  return decoded;
}

function parseContent(content) {
  if (!content) return { type: 'empty', md: '', ops: [], images: [], tags: [] };
  const decoded = decodeStructuredContent(content);
  if (decoded && typeof decoded === 'object') {
    if (typeof decoded.md === 'string') {
      return { type: 'md', md: decoded.md, images: decoded.images || [], tags: decoded.tags || [] };
    }
    if (Array.isArray(decoded.ops)) {
      return { type: 'ops', ops: decoded.ops, images: decoded.images || [], tags: decoded.tags || [] };
    }
  }
  return { type: 'plain', text: typeof decoded === 'string' ? decoded : String(content) };
}

function renderOps(ops) {
  if (!ops || !ops.length) return '';
  let result = '';
  for (const op of ops) {
    if (typeof op.insert === 'string') {
      let text = op.insert
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      if (op.attributes) {
        if (op.attributes.bold)   text = `<strong>${text}</strong>`;
        if (op.attributes.italic) text = `<em>${text}</em>`;
        if (op.attributes.underline) text = `<u>${text}</u>`;
        if (op.attributes.link) {
          const href = op.attributes.link.replace(/"/g, '');
          text = `<a href="${href}" target="_blank" rel="noopener noreferrer" style="color:#409eff">${text}</a>`;
        }
      }
      result += text;
    }
  }
  return result.replace(/\n/g, '<br/>');
}

// ======= 互动 =======
function interact(type, message) {
  post(`/api/forum/interact?tid=${tid}&type=${type}&state=${!topic[type]}`, null, () => {
    ElMessage.success(`${message}成功`);
    refreshTopicState(topic.page);
  });
}

function deleteTopic() {
  ElMessageBox.confirm('确定要永久删除这篇帖子吗？', '删除提醒', { type: 'warning' }).then(() => {
    del(`/api/forum/delete-topic?id=${tid}`, () => {
      ElMessage.success('帖子已删除');
      router.push('/home');
    });
  }).catch(() => {});
}

async function copyUrl() {
  await navigator.clipboard.writeText(window.location.href);
  ElMessage.success("链接已复制");
}

// ======= 评论 =======
function loadComments(page, stopLoading = false) {
  topic.page = page;
  get(`/api/forum/comments?tid=${tid}&page=${page - 1}`, data => {
    topic.comments = data || [];
    if (stopLoading) {
      loading.value = false;
    }
  }, () => {
    if (stopLoading) {
      loading.value = false;
    }
  });
}

function textToContent(text) {
  // 生成标准 Quill Delta 格式
  return JSON.stringify({ ops: [{ insert: text }, { insert: '\n' }] });
}

function submitMainComment() {
  const trimmed = commentText.value.trim();
  if (!trimmed) { ElMessage.warning("评论不能为空"); return; }
  if (trimmed.length > 500) { ElMessage.warning("评论不能超过500字"); return; }
  post("/api/forum/add-comment", {
    tid,
    quote: -1,
    content: textToContent(trimmed)
  }, () => {
    ElMessage.success("评论发布成功！");
    commentText.value = "";
    refreshTopicState(topic.page);
  }, err => {
    ElMessage.error(err || "发布失败，请重试");
  });
}

function toggleReply(item) {
  if (showReplyId.value === item.id) {
    showReplyId.value = null;
    replyText.value = "";
  } else {
    showReplyId.value = item.id;
    replyText.value = "";
    nextTick(() => document.getElementById(`reply-input-${item.id}`)?.focus());
  }
}

function submitReply(item) {
  const trimmed = replyText.value.trim();
  if (!trimmed) { ElMessage.warning("回复不能为空"); return; }
  post("/api/forum/add-comment", {
    tid,
    quote: item.id,
    content: textToContent(trimmed)
  }, () => {
    ElMessage.success("回复发布成功！");
    replyText.value = "";
    showReplyId.value = null;
    refreshTopicState(topic.page);
  }, err => {
    ElMessage.error(err || "回复失败，请重试");
  });
}

function deleteComment(id) {
  ElMessageBox.confirm('确定删除这条评论吗？', { type: 'warning' }).then(() => {
    del(`/api/forum/delete-comment?id=${id}`, () => {
      ElMessage.success("已删除");
      refreshTopicState(topic.page);
    });
  }).catch(() => {});
}

function relativeTime(dateStr) {
  const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
  if (diff < 60)    return '刚刚';
  if (diff < 3600)  return `${Math.floor(diff / 60)} 分钟前`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`;
  return `${Math.floor(diff / 86400)} 天前`;
}

function getCommentText(content) {
  try {
    const p = typeof content === 'string' ? JSON.parse(content) : content;
    if (p && p.ops) {
      return p.ops.map(op => (typeof op.insert === 'string' ? op.insert : '')).join('').replace(/\n$/, '');
    }
  } catch(e) {}
  return content || '';
}

const likeCount    = computed(() => topic.data?.like     ?? 0);
const collectCount = computed(() => topic.data?.collect  ?? 0);
const commentCount = computed(() => topic.data?.comments ?? 0);
const parsed       = computed(() => topic.data ? parseContent(topic.data.content) : null);
const displayTags  = computed(() => topic.data?.tags?.length ? topic.data.tags : (parsed.value?.tags || []));
</script>

<template>
  <div class="tdv" v-if="topic.data">
    <!-- 顶部导航 -->
    <div class="tdv-nav">
      <button class="back-btn" @click="router.push('/home')">
        <ArrowLeft style="width:15px; margin-right:4px"/> 返回社区
      </button>
      <div class="nav-sep"></div>
      <div class="tags-row" v-if="displayTags.length">
        <span class="tag-main">{{ displayTags[0] }}</span>
        <span v-for="t in displayTags.slice(1)" :key="t" class="tag-sub">#{{ t }}</span>
      </div>
      <span v-else class="tag-main gray">未分类</span>
      <h1 class="nav-title">{{ topic.data.title }}</h1>
    </div>

    <div class="tdv-layout">
      <!-- 主内容区 -->
      <div class="tdv-main">
        <!-- 文章卡片 -->
        <div class="article-card">
          <div class="author-bar">
            <el-avatar :src="store.getAvatar(topic.data.user.avatar, topic.data.user.username)" :size="44"/>
            <div class="ab-meta">
              <div class="ab-name">
                {{ topic.data.user.username }}
                <el-icon v-if="topic.data.user.gender === 1" color="#f43f5e"><Female/></el-icon>
                <el-icon v-if="topic.data.user.gender === 0" color="#3b82f6"><Male/></el-icon>
              </div>
              <div class="ab-time">发布于 {{ new Date(topic.data.time).toLocaleString() }}</div>
            </div>
            <div style="flex:1"></div>
            <div v-if="store.user.id === topic.data.user.id || store.user.role === 'admin' || store.user.role === 'moderator'" class="ab-ops">
              <el-button v-if="store.user.id === topic.data.user.id" :icon="EditPen" circle size="small" @click="edit=true"/>
              <el-button :icon="Delete" circle size="small" type="danger" plain @click="deleteTopic"/>
            </div>
          </div>

          <el-divider style="margin: 16px 0"/>

          <!-- 正文 - 根据内容类型渲染 -->
          <div class="post-body">
            <!-- 新帖：Markdown 渲染 -->
            <MdPreview v-if="parsed && parsed.type === 'md'"
                       :modelValue="parsed.md"
                       class="md-preview-area"/>
            <!-- 旧帖：Quill Delta 渲染 -->
            <div v-else-if="parsed && parsed.type === 'ops'"
                 class="html-content"
                 v-html="renderOps(parsed.ops)">
            </div>
            <!-- 纯文本兜底 -->
            <div v-else class="html-content">{{ parsed?.text || '' }}</div>

            <!-- 图片画廊 -->
            <div class="img-gallery" v-if="parsed && parsed.images && parsed.images.length">
              <el-image
                v-for="img in parsed.images"
                :key="img"
                :src="img"
                :preview-src-list="parsed.images"
                fit="cover"
                class="post-img"
              />
            </div>
          </div>

          <!-- 互动栏 - 优化版 -->
          <div class="interact-bar">
            <div class="interact-left">
              <!-- 点赞按钮 -->
              <button class="ib-btn like-btn" :class="{active: topic.like}" @click="interact('like','点赞')">
                <ThumbsUp :size="15" :fill="topic.like ? 'currentColor' : 'none'"/>
                <span>点赞</span>
                <b class="ib-count">{{ likeCount }}</b>
              </button>
              <!-- 收藏按钮 -->
              <button class="ib-btn collect-btn" :class="{active: topic.collect}" @click="interact('collect','收藏')">
                <Star :size="15" :fill="topic.collect ? 'currentColor' : 'none'"/>
                <span>{{ topic.collect ? '已收藏' : '收藏' }}</span>
                <b class="ib-count">{{ collectCount }}</b>
              </button>
              <!-- 分享按钮 -->
              <button class="ib-btn share-btn" @click="copyUrl">
                <Share2 :size="15"/>
                <span>分享</span>
              </button>
            </div>
            <div class="interact-right">
              <MessageCircle :size="14" style="color:#aaa"/>
              <span class="comment-stat">{{ commentCount }} 条评论</span>
            </div>
          </div>
        </div>

        <!-- B站风格评论区 -->
        <div class="comment-section">
          <div class="cs-header">
            <span class="cs-title">评论</span>
            <span class="cs-count">{{ commentCount }}</span>
          </div>

          <!-- 主评论输入框 -->
          <div class="cs-input-wrap">
            <el-avatar :src="store.getAvatar(store.user.avatar, store.user.username)" :size="38"/>
            <div class="cs-input-box" :class="{focused: commentText.length > 0}">
              <textarea
                v-model="commentText"
                class="cs-textarea"
                placeholder="发一条友善的评论~"
                maxlength="500"
                rows="1"
                @input="(e) => { e.target.style.height='auto'; e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px'; }"
              ></textarea>
              <div class="cs-footer">
                <span class="cs-count-text">{{ commentText.length }}/500</span>
                <button class="cs-publish-btn" :disabled="!commentText.trim()" @click="submitMainComment">发布</button>
              </div>
            </div>
          </div>

          <!-- 评论列表 -->
          <div class="cs-list">
            <el-empty v-if="!topic.comments.length" description="暂无评论，快来抢沙发~" :image-size="60" style="padding: 30px 0"/>

            <div v-for="item in topic.comments" :key="item.id" class="cs-item">
              <el-avatar :src="store.getAvatar(item.user.avatar, item.user.username)" :size="36" class="cs-avatar"/>
              <div class="cs-body">
                <div class="cs-row1">
                  <span class="cs-username">{{ item.user.username }}</span>
                  <span class="cs-time">{{ relativeTime(item.time) }}</span>
                </div>
                <!-- 引用内容 -->
                <div v-if="item.quote" class="cs-quote">
                  <CornerDownRight :size="11"/>
                  <span>{{ item.quote }}</span>
                </div>
                <!-- 评论正文 -->
                <p class="cs-text">{{ getCommentText(item.content) }}</p>
                <!-- 操作 -->
                <div class="cs-actions">
                  <button class="cs-act-btn" @click="toggleReply(item)">
                    <MessageCircle :size="12"/> {{ showReplyId === item.id ? '取消' : '回复' }}
                  </button>
                  <button
                    v-if="item.user.id === store.user.id || store.user.role === 'admin' || store.user.role === 'moderator'"
                    class="cs-act-btn danger"
                    @click="deleteComment(item.id)"
                  >删除</button>
                </div>
                <!-- 内联回复框 -->
                <div v-if="showReplyId === item.id" class="cs-reply-box">
                  <input
                    :id="`reply-input-${item.id}`"
                    v-model="replyText"
                    type="text"
                    :placeholder="`回复 @${item.user.username}：`"
                    class="cs-reply-input"
                    maxlength="200"
                    @keyup.enter="submitReply(item)"
                  />
                  <button class="cs-reply-send" @click="submitReply(item)">
                    <Send :size="12"/> 发布
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="cs-pagination" v-if="commentCount > 10">
            <el-pagination
              background layout="prev, pager, next"
              v-model:current-page="topic.page"
              @current-change="loadComments"
              :total="commentCount"
              :page-size="10"
            />
          </div>
        </div>
      </div>

      <!-- 右侧作者卡片 -->
      <div class="tdv-sidebar">
        <div class="author-card">
          <el-avatar :src="store.getAvatar(topic.data.user.avatar, topic.data.user.username)" :size="64"/>
          <div class="ac-name">{{ topic.data.user.username }}</div>
          <div class="ac-desc">{{ topic.data.user.desc || '这个同学没有留下简介' }}</div>
          <el-divider style="margin: 14px 0"/>
          <div class="ac-row" v-if="topic.data.user.email">
            <el-icon><Promotion/></el-icon>
            <span>{{ topic.data.user.email }}</span>
          </div>
        </div>
      </div>
    </div>

    <topic-editor
      :show="edit"
      :default-text="topic.data.content"
      :default-title="topic.data.title"
      :default-type="topic.data.type"
      @close="edit = false; init()"
      :tid="String(tid)"
    />
  </div>

  <div v-else-if="loading" style="padding: 60px 20px;">
    <el-skeleton :rows="10" animated/>
  </div>
</template>

<style lang="less" scoped>
.tdv {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px 16px 80px;
}

/* ===== 顶部导航 ===== */
.tdv-nav {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 12px 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.back-btn {
  display: inline-flex; align-items: center;
  background: none; border: none; color: #606266;
  cursor: pointer; font-size: 13px; font-weight: 600;
  padding: 4px 10px; border-radius: 6px; transition: all 0.15s; white-space: nowrap;
  &:hover { background: var(--el-fill-color-light); color: var(--el-color-primary); }
}
.nav-sep { width: 1px; height: 16px; background: var(--el-border-color); flex-shrink: 0; }
.tags-row { display: flex; align-items: center; gap: 6px; }
.tag-main {
  font-size: 12px; font-weight: 800; color: #f43f5e;
  background: #fff1f2; padding: 3px 12px; border-radius: 100px;
  border: 1px solid #ffe4e6; white-space: nowrap;
  &.gray { color: #64748b; background: #f1f5f9; border-color: #e2e8f0; }
}
.tag-sub { font-size: 11px; font-weight: 600; color: #64748b; background: #f8fafc; padding: 2px 8px; border-radius: 6px; }
.nav-title {
  margin: 0; font-size: 17px; font-weight: 800;
  color: var(--el-text-color-primary); line-height: 1.4; word-break: break-word;
}

/* ===== 主布局 ===== */
.tdv-layout {
  display: grid; grid-template-columns: 1fr 270px;
  gap: 20px; align-items: flex-start;
}

/* ===== 文章卡片 ===== */
.article-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px; padding: 24px;
}
.author-bar {
  display: flex; align-items: center; gap: 12px;
  .ab-meta { flex: 1; }
  .ab-name { font-weight: 800; font-size: 15px; display: flex; align-items: center; gap: 4px; color: var(--el-text-color-primary); }
  .ab-time { font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 2px; }
  .ab-ops { display: flex; gap: 6px; }
}
.post-body {
  font-size: 16px; line-height: 1.8; color: var(--el-text-color-primary);
  .html-content { word-break: break-word; }
  .md-preview-area { 
    background: transparent !important; border: none !important; padding: 0 !important;
    /* 创建独立的层叠上下文，防止内部元素（如代码块 toolbar）的 z-index 
       突破到弹窗遮罩层之上，导致 UI 穿透 bug */
    isolation: isolate;
    /* Limit markdown image height */
    :deep(.md-editor-preview img) {
      max-height: 480px;
      width: auto;
      max-width: 100%;
      border-radius: 8px;
    }
    /* 重置 md-editor-v3 代码块 toolbar 的层级，防止其 sticky z-index 穿透弹窗 */
    :deep(.md-editor-code-head) {
      z-index: 1 !important;
      position: sticky;
    }
    :deep(.md-editor-copy-button) {
      z-index: 1 !important;
    }
  }
}
.img-gallery {
  display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px;
  .post-img { max-width: 320px; height: 200px; border-radius: 8px; border: 1px solid var(--el-border-color-lighter); }
}

/* ===== 互动栏 优化版 ===== */
.interact-bar {
  margin-top: 28px; padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex; align-items: center; justify-content: space-between;
}
.interact-left { display: flex; align-items: center; gap: 8px; }
.interact-right {
  display: flex; align-items: center; gap: 6px;
  .comment-stat { font-size: 13px; color: var(--el-text-color-placeholder); }
}

.ib-btn {
  display: inline-flex; align-items: center; gap: 6px;
  min-width: 96px;
  justify-content: center;
  padding: 8px 16px; border-radius: 20px;
  font-size: 13px; font-weight: 600;
  border: 1.5px solid var(--el-border-color);
  background: var(--el-bg-color); color: var(--el-text-color-regular);
  cursor: pointer; transition: all 0.2s;
  user-select: none;

  &:hover { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }

  &.like-btn.active {
    background: #eff6ff; color: #2563eb; border-color: #93c5fd;
    font-weight: 700;
  }
  &.collect-btn.active {
    background: #fffbeb; color: #d97706; border-color: #fde68a;
    font-weight: 700;
  }
  &.share-btn:hover { border-color: #a3e635; background: #f7fee7; color: #4d7c0f; }
}

.ib-count {
  font-size: 13px;
  font-weight: 800;
}

/* ===== B站风格评论区 ===== */
.comment-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px; padding: 24px; margin-top: 20px;
}
.cs-header {
  display: flex; align-items: baseline; gap: 8px; margin-bottom: 22px;
  .cs-title { font-size: 18px; font-weight: 800; color: var(--el-text-color-primary); }
  .cs-count { font-size: 14px; color: var(--el-text-color-placeholder); }
}

/* 主输入框 */
.cs-input-wrap { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 28px; }
.cs-input-box {
  flex: 1; border: 1.5px solid var(--el-border-color);
  border-radius: 10px; overflow: hidden; transition: border-color 0.2s;
  &.focused, &:focus-within { border-color: var(--el-color-primary); }
}
.cs-textarea {
  width: 100%; min-height: 42px; max-height: 120px;
  padding: 10px 12px; font-size: 14px; color: var(--el-text-color-primary);
  background: transparent; border: none; outline: none; resize: none;
  line-height: 1.6; font-family: inherit; box-sizing: border-box;
  &::placeholder { color: var(--el-text-color-placeholder); }
}
.cs-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
  .cs-count-text { font-size: 12px; color: var(--el-text-color-placeholder); }
}
.cs-publish-btn {
  padding: 5px 18px; background: #00aeec; color: #fff;
  border: none; border-radius: 6px; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: background 0.2s;
  &:hover:not(:disabled) { background: #0093c7; }
  &:disabled { background: #b9dff5; cursor: not-allowed; }
}

/* 评论列表 */
.cs-list { display: flex; flex-direction: column; }
.cs-item {
  display: flex; gap: 13px; padding: 18px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  &:last-child { border-bottom: none; }
}
.cs-avatar { flex-shrink: 0; }
.cs-body { flex: 1; min-width: 0; }
.cs-row1 {
  display: flex; align-items: center; gap: 10px; margin-bottom: 6px;
  .cs-username { font-size: 14px; font-weight: 700; color: var(--el-text-color-primary); }
  .cs-time { font-size: 12px; color: var(--el-text-color-placeholder); }
}
.cs-quote {
  display: flex; align-items: flex-start; gap: 4px;
  background: var(--el-fill-color-light); border-left: 3px solid var(--el-border-color);
  padding: 5px 10px; border-radius: 0 6px 6px 0;
  margin-bottom: 8px; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5;
}
.cs-text {
  font-size: 14px; line-height: 1.7; color: var(--el-text-color-primary);
  white-space: pre-wrap; word-break: break-word; margin: 0;
}
.cs-actions {
  display: flex; align-items: center; gap: 14px; margin-top: 9px;
}
.cs-act-btn {
  display: inline-flex; align-items: center; gap: 4px;
  background: none; border: none; font-size: 12px;
  color: var(--el-text-color-placeholder); cursor: pointer;
  padding: 2px 0; transition: color 0.15s;
  &:hover { color: var(--el-color-primary); }
  &.danger:hover { color: var(--el-color-danger); }
}

/* 内联回复框 */
.cs-reply-box {
  display: flex; align-items: center; gap: 8px;
  margin-top: 12px; padding: 10px 12px;
  background: var(--el-fill-color-lighter); border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}
.cs-reply-input {
  flex: 1; height: 34px; padding: 0 12px;
  border: 1px solid var(--el-border-color); border-radius: 17px;
  font-size: 13px; outline: none; background: var(--el-bg-color);
  color: var(--el-text-color-primary); transition: border-color 0.2s;
  &:focus { border-color: var(--el-color-primary); }
  &::placeholder { color: var(--el-text-color-placeholder); }
}
.cs-reply-send {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; background: #00aeec; color: #fff;
  border: none; border-radius: 14px; font-size: 12px; font-weight: 700;
  cursor: pointer; white-space: nowrap; transition: background 0.2s;
  flex-shrink: 0;
  &:hover { background: #0093c7; }
}
.cs-pagination { margin-top: 20px; display: flex; justify-content: center; }

/* ===== 右侧作者卡片 ===== */
.author-card {
  background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px; padding: 24px; text-align: center;
  position: sticky; top: 80px;
  .ac-name { font-weight: 800; font-size: 16px; margin-top: 12px; color: var(--el-text-color-primary); }
  .ac-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 6px; line-height: 1.5; }
  .ac-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--el-text-color-regular); text-align: left; }
}

@media (max-width: 860px) {
  .tdv-layout { grid-template-columns: 1fr; }
  .tdv-sidebar { display: none; }
}
</style>
