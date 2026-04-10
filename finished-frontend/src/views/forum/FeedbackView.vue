<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { post } from "@/net/api.js";
import { Send, CheckCircle, HelpCircle, Bug, Lightbulb } from 'lucide-vue-next';

const formRef = ref();
const form = reactive({
  type: 'suggestion',
  title: '',
  content: '',
  contact: ''
});

const isSubmitted = ref(false);

const rules = {
  title: [{ required: true, message: '请简述反馈标题', trigger: 'blur' }, { min: 2, max: 40, message: '标题长度控制在 2-40 字', trigger: 'blur' }],
  content: [{ required: true, message: '请填写反馈详情', trigger: 'blur' }, { min: 10, max: 500, message: '请填写至少 10 字以上的内容', trigger: 'blur' }],
};

function submitFeedback() {
  formRef.value.validate((valid) => {
    if (valid) {
      post("/api/user/feedback", form, () => {
        ElMessage.success({ message: "感谢您的反馈，我们会尽快处理！", plain: true });
        isSubmitted.value = true;
      }, (msg) => {
        ElMessage.error(msg || "提交失败，请重试");
      });
    }
  });
}

function resetForm() {
  isSubmitted.value = false;
  form.title = '';
  form.content = '';
  form.contact = '';
}
</script>

<template>
  <div class="feedback-page ds-page">
    <div class="feedback-container">
      <div v-if="!isSubmitted" class="feedback-form-card ds-card">
        <div class="form-header">
           <div class="header-icon"><HelpCircle :size="32" /></div>
           <h2 class="form-title">意见与反馈</h2>
           <p class="form-subtitle">无论是 Bug、改进建议或吐槽，您对平台成长的每一份关注都在这里化为养料。</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <el-form-item label="反馈类型">
            <div class="type-selector">
              <label class="type-btn" :class="{ active: form.type === 'bug' }">
                 <input type="radio" value="bug" v-model="form.type">
                 <Bug :size="16" /> 程序 Bug
              </label>
              <label class="type-btn" :class="{ active: form.type === 'suggestion' }">
                 <input type="radio" value="suggestion" v-model="form.type">
                 <Lightbulb :size="16" /> 改进建议
              </label>
              <label class="type-btn" :class="{ active: form.type === 'other' }">
                 <input type="radio" value="other" v-model="form.type">
                 <HelpCircle :size="16" /> 其它问题
              </label>
            </div>
          </el-form-item>

          <el-form-item label="反馈标题" prop="title">
            <el-input v-model="form.title" placeholder="请简要描述您遇到的问题或建议" maxlength="40" />
          </el-form-item>

          <el-form-item label="详细内容" prop="content">
            <el-input 
              type="textarea" 
              v-model="form.content" 
              :rows="6" 
              maxlength="500" 
              show-word-limit 
              placeholder="请尽可能详细地说明背景或重现步骤，以便我们为您解决问题..." 
            />
          </el-form-item>

          <el-form-item label="联系方式 (选填)">
            <el-input v-model="form.contact" placeholder="邮箱或手机，方便我们就此问题与您沟通" />
          </el-form-item>

          <div class="form-footer">
             <button class="ds-btn-primary submit-btn" @click.prevent="submitFeedback">
                <Send :size="16" /> 提交反馈
             </button>
          </div>
        </el-form>
      </div>

      <div v-else class="success-card ds-card">
         <div class="success-icon"><CheckCircle :size="64" /></div>
         <h2 class="success-title">提交成功</h2>
         <p class="success-desc">您的反馈已收到！我们将认真评估并在后续版本中体现您的意见。</p>
         <el-button type="primary" @click="resetForm" plain>再次反馈</el-button>
         <el-button @click="$router.push('/home')" style="margin-left: 12px">回到首页</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.feedback-page {
  padding: 40px 20px;
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.feedback-container {
  width: 100%;
  max-width: 680px;
}

.feedback-form-card {
  padding: 48px;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
  .header-icon { color: var(--el-color-primary); margin-bottom: 16px; }
  .form-title { font-size: 24px; font-weight: 800; color: var(--el-text-color-primary); margin: 0 0 12px; }
  .form-subtitle { font-size: 14px; color: var(--el-text-color-secondary); line-height: 1.6; max-width: 500px; margin: 0 auto; }
}

.type-selector {
  display: flex;
  gap: 12px;
  width: 100%;
}

.type-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: var(--el-bg-color);
  transition: all 0.2s;

  input { display: none; }

  &:hover { background: var(--el-fill-color-light); border-color: var(--el-color-primary-light-5); }
  &.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
}

.form-footer {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.submit-btn {
  width: 200px;
  height: 46px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}

.success-card {
  text-align: center;
  padding: 64px 48px;
  .success-icon { color: #10b981; margin-bottom: 24px; }
  .success-title { font-size: 28px; font-weight: 800; color: var(--el-text-color-primary); margin-bottom: 12px; }
  .success-desc { color: var(--el-text-color-secondary); margin-bottom: 32px; font-size: 15px; }
}

:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  border-radius: 10px;
}
</style>
