<script setup>
import {Delta, QuillEditor} from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css"
import {computed, ref} from "vue";
import {post} from "@/net/api.js";
import {ElMessage} from "element-plus";
import {useAppStore} from "@/stores/app-store.js";

const props = defineProps({
  tid: String,
  quote: Object
})

const store = useAppStore()
const content = ref(new Delta())

const emit = defineEmits(['close', "comment"])

function submitComment() {
  if (deltaToText(content.value).length > 2000) {
    ElMessage.warning({message:"评论字数已超出最大限制, 请缩减内容！", plain: true})
    return
  }
  post("/api/forum/add-comment", {
    tid: props.tid,
    quote: props.quote ? props.quote.id : -1,
    content: JSON.stringify(content.value)
  }, () => {
    ElMessage.success({message: "发表评论成功！", plain: true})
    content.value = new Delta()
    emit("comment")
  })
}

function cancelReply() {
  emit("close")
}

function deltaToSimpleText(delta) {
    let str = deltaToText(JSON.parse(delta));
    if (str.length > 35) str = str.substring(0,35) + "..."
    return str;
}

function deltaToText(delta) {
  if (!delta?.ops) return "";
  let str = "";
  for (let op of delta.ops) {
    str += op.insert
  }
  return str.replace(/\s/g, "");
}

const inputTitle = computed(() =>
  props.quote ? `回复 ${props.quote.user?.username || "这条评论"}` : "发布评论"
)

const inputPlaceholder = computed(() =>
  props.quote
    ? "写下你的回复，像在 B 站评论区那样直接参与讨论..."
    : "发一条友善、具体、有信息量的评论..."
)
</script>

<template>
    <div class="inline-comment-editor">
        <div class="editor-shell">
            <el-avatar :src="store.getAvatar(store.user.avatar)" :size="40" class="editor-avatar" />
            <div class="editor-main">
                <div class="editor-head">
                    <div class="editor-title">{{ inputTitle }}</div>
                    <div class="editor-tip">评论会直接出现在当前帖子下方</div>
                </div>

                <div v-if="quote" class="reply-banner">
                    <div class="reply-label">
                        正在回复：
                        <b>{{ quote.user?.username || "评论作者" }}</b>
                    </div>
                    <div class="reply-content">“{{ deltaToSimpleText(quote.content) }}”</div>
                    <el-button link size="small" @click="cancelReply">取消回复</el-button>
                </div>

                <quill-editor
                  class="editor-input"
                  v-model:content="content"
                  :placeholder="inputPlaceholder"
                />

                <div class="editor-footer">
                  <div class="editor-count">
                      字数统计：{{ deltaToText(content).length }} / 2000
                  </div>
                  <div class="editor-actions">
                      <el-button v-if="quote" plain @click="cancelReply">取消</el-button>
                      <el-button type="primary" @click="submitComment">
                        {{ quote ? "回复评论" : "发表评论" }}
                      </el-button>
                  </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style lang="less" scoped>
.inline-comment-editor {
  margin-top: 18px;
}

.editor-shell {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.editor-main {
  flex: 1;
  min-width: 0;
}

.editor-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.editor-title {
  font-size: 16px;
  font-weight: 800;
  color: #303133;
}

.editor-tip {
  font-size: 12px;
  color: #909399;
}

.reply-banner {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.reply-label {
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

.reply-content {
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
  margin-bottom: 4px;
}

:deep(.ql-toolbar) {
  border-radius: 12px 12px 0 0;
  border-color: var(--el-border-color) !important;
  background: #fff;
}

:deep(.ql-container) {
  border-radius: 0 0 12px 12px;
  border-color: var(--el-border-color) !important;
  background: #fff;
}

:deep(.ql-editor) {
  font-size: 14px;
  min-height: 110px;
}

:deep(.ql-editor.ql-blank::before) {
  color: var(--el-text-color-placeholder) !important;
  font-style: normal !important;
}

.editor-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.editor-count {
  font-size: 13px;
  color: #909399;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .editor-shell {
    gap: 10px;
  }

  .editor-avatar {
    display: none;
  }

  .editor-head,
  .editor-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .editor-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
