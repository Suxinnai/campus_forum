<script setup>
import {Check, Document, Plus} from "@element-plus/icons-vue";
import {computed, reactive, ref} from "vue";
import { MdEditor } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import axios from "axios";
import {ElMessage} from "element-plus";
import {getToken, post} from "@/net/api.js";
import ColorDot from "@/components/ColorDot.vue";
import {useAppStore} from "@/stores/app-store.js";

const props = defineProps({
  tid: { default: "-1", type: String },
  show: Boolean,
  defaultTitle: { default: '', type: String },
  defaultText: { default: '', type: String },
  defaultType: { default: null, type: Number },
  submitButton: { default: '立即发布', type: String },
})

const store = useAppStore();
const emit = defineEmits(['close', 'success'])

const editor = reactive({
  type: null,
  title: "",
  text: "",
  loading: false,
  tags: [] // 新增标签字段
})

const presetTags = [
  '校园生活', '学习交流', '失物招领', '校内活动', '避雷指南', 
  '求助咨询', '表白墙', '二手交易', '社团招新', '学术讲座'
]

function toggleTag(tag) {
  if (editor.tags.includes(tag)) {
    editor.tags = editor.tags.filter(t => t !== tag)
  } else if (editor.tags.length < 3) {
    editor.tags.push(tag)
  } else {
    ElMessage.warning({message: "最多只能选择 3 个标签哦", plain: true})
  }
}

const images = ref([]);

const contentLength = computed(() => editor.text ? editor.text.length : 0)

function extractPlain(md) {
  return md.replace(/[\#\*\_\[\]\(\)\!\\\`\>\-]/g, '').substring(0, 300);
}

const submitTopic = () => {
  if (contentLength.value > 20000) {
    ElMessage.warning({message:"字数超出限制", plain:true})
    return
  }
  if (!editor.title) {
    ElMessage.warning({message:"请填写标题!", plain:true})
    return
  }
  
  // 默认选择第一个分类以满足后端要求
  const defaultType = editor.type || store.forum.types.find(t => t.id > 0);
  if (!defaultType) {
    ElMessage.warning({message:"分类获取失败，请重试", plain:true})
    return;
  }
  
  const dummyOps = [{ insert: extractPlain(editor.text) }];
  images.value.forEach(img => {
      dummyOps.push({ insert: { image: img.url } });
  });

  const payloadStr = JSON.stringify({
    ops: dummyOps,
    md: editor.text,
    images: images.value.map(i => i.url),
    tags: editor.tags
  });

  if (props.tid === "-1") {
    post("/api/forum/create-topic", {
      type: defaultType.id,
      title: editor.title,
      content: payloadStr
    }, () => {
      ElMessage.success({message:"文章发表成功", plain:true})
      emit('success')
    })
  } else {
    post("/api/forum/update-topic", {
      id: parseInt(props.tid),
      type: defaultType.id,
      title: editor.title,
      content: payloadStr
    }, () => {
      ElMessage.success({message:"文章更新成功", plain:true})
      emit('success')
    })
  }
}

function initEditor() {
  images.value = [];
  editor.tags = [];
  if (props.defaultText) {
    try {
      const parsed = JSON.parse(props.defaultText);
      if (parsed.md) {
        editor.text = parsed.md;
        editor.tags = parsed.tags || []; // 回显标签
        if (parsed.images) {
          images.value = parsed.images.map((url, i) => ({ name: `img${i}`, url }));
        }
      } else {
        editor.text = "旧版富文本无法在 Markdown 编辑器中直接展示，请直接清空重写。";
      }
    } catch(e) {
      editor.text = props.defaultText;
    }
  } else {
    editor.text = "";
  }
  editor.title = props.defaultTitle
  editor.type = store.forum.types.find(t => t.id === props.defaultType);
}

const onUploadSuccess = (response, uploadFile) => {
  if(response.code === 200) {
    images.value.push({ name: uploadFile.name, url: axios.defaults.baseURL + '/images' + response.data })
  } else {
    ElMessage.error("图片上传失败")
  }
}

const authHeaders = { Authorization: `Bearer ${getToken()}` };

const onMdUploadImg = async (files, callback) => {
  const resUrls = [];
  for (let file of files) {
      const formData = new FormData();
      formData.append('file', file);
      try {
        const res = await axios.post(axios.defaults.baseURL + '/api/image/cache', formData, { headers: authHeaders });
        if(res.data.code === 200) {
           resUrls.push(axios.defaults.baseURL + '/images' + res.data.data);
        }
      } catch (e) {}
  }
  callback(resUrls);
}
</script>

<template>
  <div>
    <el-dialog 
      :model-value="show" 
      @open="initEditor" 
      @close="emit('close')"
      :close-on-click-modal="false"
      width="820px"
      top="5vh"
      class="topic-editor-dialog"
      destroy-on-close
      align-center
    >
      <template #header>
        <div class="editor-header">
          <div class="editor-header-title">
            <div class="header-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
            </div>
            <div>
              <div class="header-main">{{ tid === '-1' ? '发表新帖子' : '编辑帖子' }}</div>
              <div class="header-sub">支持 Markdown 和 Emoji · 点选话题标签更吸引人</div>
            </div>
          </div>
        </div>
      </template>

      <!-- 标题栏：占满整行 -->
      <div class="editor-title-row">
        <el-input 
          placeholder="请输入文章标题 (最多38字)" 
          v-model="editor.title" 
          maxlength="38" 
          size="large"
          class="title-input-premium"
        >
          <template #prefix>
             <el-icon><Document /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- Markdown 编辑器 -->
      <div class="editor-body">
        <MdEditor 
          v-model="editor.text" 
          :onUploadImg="onMdUploadImg" 
          placeholder="今天想分享点什么呢？支持 Markdown 语法..." 
          style="height: 100%; border: none;" 
          :preview="false"
          :toolbars="['bold', 'italic', 'title', '-', 'unorderedList', 'orderedList', '-', 'link', 'image', 'quote', '-', 'revoke', 'next', '=', 'preview']"
        />
      </div>

      <!-- 底部附加区：图片 + 分类 + 标签 -->
      <div class="editor-attachment-section">
        <div class="editor-upload">
          <div class="section-label">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
            附加图片素材 (最多 9 张)
          </div>
          <el-upload
            :action="axios.defaults.baseURL + '/api/image/cache'"
            :headers="authHeaders"
            list-type="picture-card"
            :limit="9"
            :file-list="images"
            :on-success="onUploadSuccess"
            :on-remove="(file, fileList) => images = fileList"
            accept="image/png, image/jpeg, image/jpg, image/gif"
            class="compact-upload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </div>

        <div class="editor-meta-footer">
          <div class="meta-item tag-picker-item">
            <div class="section-label">添加话题 (最多 3 个)</div>
            <div class="tag-pool">
              <span 
                v-for="tag in presetTags" 
                :key="tag" 
                class="preset-tag-chip"
                :class="{ active: editor.tags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                #{{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底栏 -->
      <template #footer>
        <div class="editor-footer">
          <div class="word-count" :class="{ warn: contentLength > 18000 }">
            <span>{{ contentLength.toLocaleString() }}</span> / 20,000 字
          </div>
          <div class="footer-actions">
            <el-button @click="emit('close')" size="large" round>取消</el-button>
            <el-button type="primary" :icon="Check" @click="submitTopic" size="large" round>
              {{ submitButton }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="less">
/* Dialog 自定样式 */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

:deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  margin: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.el-dialog__body) {
  padding: 0;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.editor-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #7C3AED, #4F46E5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-main {
  font-size: 17px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

.header-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* 标题栏 */
.editor-title-row {
  padding: 20px 24px 12px;
  .title-input-premium {
    :deep(.el-input__wrapper) {
      box-shadow: none !important;
      background: transparent;
      padding-left: 0;
      .el-input__inner {
        font-size: 20px;
        font-weight: 900;
        letter-spacing: -0.5px;
        &::placeholder { font-weight: 500; opacity: 0.5; }
      }
    }
  }
}

/* 编辑器 */
.editor-body {
  height: 380px;
  overflow: hidden;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

:deep(.md-editor) {
  --md-bk-color: transparent;
  border: none !important;
  box-shadow: none !important;
}

/* 底部附加区 */
.editor-attachment-section {
  background: #fcfcfd;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.editor-upload {
  padding: 16px 24px 12px;
}

.compact-upload {
  :deep(.el-upload--picture-card) {
    width: 64px; height: 64px; border-radius: 10px;
  }
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 64px; height: 64px; border-radius: 10px;
  }
}

.editor-meta-footer {
  padding: 12px 24px 20px;
  display: flex;
  gap: 32px;
  border-top: 1px solid rgba(0,0,0,0.03);
}

.tag-picker-item {
  flex: 1;
}

.tag-pool {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-tag-chip {
  padding: 5px 12px;
  border-radius: 100px;
  background: #f1f5f9;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;

  &:hover {
    background: #e2e8f0;
    color: var(--el-color-primary);
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-7);
    box-shadow: 0 4px 10px rgba(124, 58, 237, 0.1);
  }
}

/* 底栏 */
.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.word-count {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  font-weight: 500;

  span { font-weight: 700; color: var(--el-text-color-regular); }
  &.warn span { color: #f59e0b; }
}

.footer-actions {
  display: flex;
  gap: 8px;
}
</style>

