<script setup>
import { useAppStore } from "@/stores/app-store.js";
import { computed, reactive, ref } from "vue";
import { get, getToken, post } from "@/net/api.js";
import { ElMessage } from "element-plus";
import axios from "axios";
import { Camera, ImagePlus, Mail, Save, Shield, User } from "lucide-vue-next";

const store = useAppStore();
const baseFormRef = ref();
const emailFormRef = ref();
const securityFormRef = ref();
const activeTab = ref("profile");
const loading = reactive({ form: true, base: false, security: false, cover: false });

const baseForm = reactive({ username: "", gender: 1, phone: "", qq: "", desc: "" });
const emailForm = reactive({ email: "", code: "" });
const securityForm = reactive({ password: "", newPassword: "", newPasswordRepeat: "" });
const privacy = reactive({ phone: false, qq: false, email: false, gender: false });
const coverPath = ref("");

const rules = {
  username: [
    {
      validator: (rule, value, callback) => {
        if (value === "") callback(new Error("用户名不能为空"));
        else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) callback(new Error("用户名不能包含特殊字符"));
        else callback();
      },
      trigger: ["blur", "change"]
    },
    { min: 2, max: 30, message: "用户名长度2-30字符", trigger: ["blur", "change"] }
  ],
  email: [
    { required: true, message: "邮件地址不能为空", trigger: ["blur", "change"] },
    { type: "email", message: "请输入合法邮箱", trigger: ["blur", "change"] }
  ],
  code: [
    { required: true, message: "验证码不能为空", trigger: ["blur", "change"] },
    { min: 6, max: 6, message: "验证码6位", trigger: ["blur", "change"] }
  ]
};

const securityRules = {
  password: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [{ required: true, message: "请输入新密码", trigger: "blur" }, { min: 6, message: "长度不少于6位", trigger: "blur" }],
  newPasswordRepeat: [{
    validator: (rule, value, callback) => {
      if (value !== securityForm.newPassword) callback(new Error("两次密码不一致"));
      else callback();
    },
    trigger: ["blur", "change"]
  }]
};

function loadFormData() {
  loading.form = true;
  get("/api/user/details", data => {
    baseForm.username = store.user.username;
    baseForm.gender = data.gender;
    baseForm.phone = data.phone;
    baseForm.qq = data.qq;
    baseForm.desc = data.desc;
    coverPath.value = data.cover || "";
    emailForm.email = store.user.email;
    loading.form = false;
  });

  get("/api/user/privacy", data => {
    privacy.phone = data.phone;
    privacy.qq = data.qq;
    privacy.email = data.email;
    privacy.gender = data.gender;
  });
}

loadFormData();

function saveDetails() {
  baseFormRef.value.validate(valid => {
    if (!valid) return;
    loading.base = true;
    post("/api/user/save-details", baseForm, () => {
      ElMessage.success({ message: "用户信息更新成功", plain: true });
      store.user.username = baseForm.username;
      loading.base = false;
    }, message => {
      ElMessage.warning({ message, plain: true });
      loading.base = false;
    });
  });
}

const codeTime = ref(0);

function sendEmailCode() {
  if (/^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/.test(emailForm.email)) {
    codeTime.value = 60;
    get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
      ElMessage.success({ message: `验证码已发送至${emailForm.email}`, plain: true });
      const handle = setInterval(() => {
        codeTime.value--;
        if (codeTime.value === 0) clearInterval(handle);
      }, 1000);
    }, message => {
      codeTime.value = 0;
      ElMessage.warning({ message, plain: true });
    });
  } else {
    ElMessage.warning({ message: "电子邮件格式不正确", plain: true });
  }
}

function modifyEmail() {
  emailFormRef.value.validate(valid => {
    if (!valid) return;
    post("/api/user/modify-email", emailForm, () => {
      ElMessage.success({ message: "邮件修改成功", plain: true });
      store.user.email = emailForm.email;
    });
  });
}

function savePrivacy(type, status) {
  post("/api/user/save-privacy", { type, status }, () => {
    ElMessage.success({ message: "隐私设置已更新", plain: true });
  });
}

function resetPassword() {
  securityFormRef.value.validate(valid => {
    if (!valid) return;
    loading.security = true;
    post("/api/user/change-password", {
      password: securityForm.password,
      newPassword: securityForm.newPasswordRepeat
    }, () => {
      ElMessage.success({ message: "密码修改成功", plain: true });
      securityFormRef.value.resetFields();
      loading.security = false;
    }, err => {
      loading.security = false;
      ElMessage.error(err);
    });
  });
}

function beforeAvatarUpload(rawFile) {
  if (!["image/jpeg", "image/png", "image/webp"].includes(rawFile.type)) {
    ElMessage.warning({ message: "头像图片只能是 JPG / PNG / WEBP 格式", plain: true });
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.warning({ message: "头像图片大小不能大于2MB", plain: true });
    return false;
  }
  return true;
}

function beforeCoverUpload(rawFile) {
  if (!["image/jpeg", "image/png", "image/webp"].includes(rawFile.type)) {
    ElMessage.warning({ message: "封面图片只能是 JPG / PNG / WEBP 格式", plain: true });
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.warning({ message: "封面图片大小不能大于5MB", plain: true });
    return false;
  }
  loading.cover = true;
  return true;
}

function uploadAvatarSuccess(response) {
  ElMessage.success({ message: response.message, plain: true });
  store.user.avatar = response.data;
}

function uploadCoverSuccess(response) {
  coverPath.value = response.data;
  loading.cover = false;
  ElMessage.success({ message: response.message, plain: true });
}

function uploadCoverError() {
  loading.cover = false;
}

const getAvatar = computed(() =>
  store.user.avatar?.length > 0
    ? `${axios.defaults.baseURL}/images${store.user.avatar}`
    : store.getAvatar("", store.user.username)
);

const getCover = computed(() =>
  coverPath.value ? `${axios.defaults.baseURL}/images${coverPath.value}` : ""
);
</script>

<template>
  <div class="setting-page" v-loading="loading.form">
    <div class="setting-layout">
      <aside class="setting-sidebar">
        <div class="profile-card settings-card">
          <div class="avatar-section">
            <div class="avatar-wrap">
              <el-avatar :size="80" :src="getAvatar" class="profile-avatar" />
              <el-upload
                :action="axios.defaults.baseURL + '/api/image/avatar'"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :on-success="uploadAvatarSuccess"
                :headers="{ Authorization: `Bearer ${getToken()}` }"
                class="avatar-upload"
              >
                <div class="avatar-upload-btn">
                  <Camera :size="14" />
                </div>
              </el-upload>
            </div>
            <div class="profile-info">
              <div class="profile-name">{{ store.user.username }}</div>
              <div class="profile-email">{{ store.user.email }}</div>
            </div>
          </div>
          
          <nav class="setting-nav">
            <div class="nav-item" :class="{active: activeTab === 'profile'}" @click="activeTab='profile'"><User :size="16" /> 基本资料</div>
            <div class="nav-item" :class="{active: activeTab === 'security'}" @click="activeTab='security'"><Shield :size="16" /> 安全隐私</div>
            <div class="nav-item" :class="{active: activeTab === 'notif'}" @click="activeTab='notif'"><Mail :size="16" /> 消息提示</div>
          </nav>
        </div>
      </aside>

      <div class="setting-content">
        <div v-if="activeTab === 'profile'" class="tab-pane animate-fade">
          <div class="form-section settings-card cover-section">
            <h3 class="section-title">个人封面</h3>
            <p class="section-desc">恢复成更稳的编辑样式，封面上传放在这里，不再撑乱整个页面。</p>
            <div class="cover-editor">
              <div class="cover-preview">
                <img v-if="getCover" :src="getCover" alt="封面预览" class="cover-image" />
                <div v-else class="cover-placeholder">暂无封面，上传后个人主页会展示在顶部横幅区域。</div>
              </div>
              <div class="cover-actions">
                <el-upload
                  :action="axios.defaults.baseURL + '/api/image/cover'"
                  :show-file-list="false"
                  :before-upload="beforeCoverUpload"
                  :on-success="uploadCoverSuccess"
                  :on-error="uploadCoverError"
                  :headers="{ Authorization: `Bearer ${getToken()}` }"
                >
                  <el-button type="primary" :loading="loading.cover" class="action-btn">
                    <ImagePlus :size="15" /> 上传封面
                  </el-button>
                </el-upload>
                <div class="cover-tip">建议尺寸 1600 × 400，支持 JPG / PNG / WEBP，上传后立即生效。</div>
              </div>
            </div>
          </div>

          <div class="form-section settings-card">
            <h3 class="section-title">个人信息</h3>
            <p class="section-desc">管理您的公开昵称和自我介绍，让其他校友更好地认识你。</p>
            <el-form :model="baseForm" :rules="rules" label-position="top" ref="baseFormRef">
              <div class="form-group">
                <el-form-item label="公开昵称" prop="username">
                  <el-input v-model="baseForm.username" maxlength="30" />
                </el-form-item>
                <el-form-item label="性别" style="width: 200px">
                  <el-radio-group v-model="baseForm.gender" class="custom-radio-group">
                    <el-radio-button :value="0">男</el-radio-button>
                    <el-radio-button :value="1">女</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </div>
              <el-form-item label="自我介绍" prop="desc">
                <el-input type="textarea" v-model="baseForm.desc" :rows="4" maxlength="200" show-word-limit />
              </el-form-item>
              <div class="form-actions"><el-button type="primary" @click="saveDetails" :loading="loading.base" class="action-btn"><Save :size="15" style="margin-right: 4px;" /> 保存更改</el-button></div>
            </el-form>
          </div>

          <div class="form-section settings-card" style="margin-top:24px">
            <h3 class="section-title">联系方式</h3>
            <p class="section-desc">填写您的校内联系渠道，方便志同道合的人联系你。</p>
            <el-form :model="baseForm" label-position="top">
              <div class="form-group">
                <el-form-item label="手机号码" style="flex:1"><el-input v-model="baseForm.phone" maxlength="11" /></el-form-item>
                <el-form-item label="QQ / 微信" style="flex:1"><el-input v-model="baseForm.qq" maxlength="20" /></el-form-item>
              </div>
              <div class="form-actions"><el-button type="primary" plain @click="saveDetails" :loading="loading.base" class="action-btn">更新联系方式</el-button></div>
            </el-form>
          </div>
        </div>

        <div v-if="activeTab === 'security'" class="tab-pane animate-fade">
          <div class="form-section settings-card">
            <h3 class="section-title">修改密码</h3>
            <p class="section-desc">定期更换密码有助于保障您的账号安全。</p>
            <el-form :model="securityForm" :rules="securityRules" label-position="top" ref="securityFormRef">
              <el-form-item label="当前密码" prop="password"><el-input type="password" v-model="securityForm.password" show-password /></el-form-item>
              <div class="form-group">
                <el-form-item label="新密码" prop="newPassword" style="flex:1"><el-input type="password" v-model="securityForm.newPassword" show-password /></el-form-item>
                <el-form-item label="确认新密码" prop="newPasswordRepeat" style="flex:1"><el-input type="password" v-model="securityForm.newPasswordRepeat" show-password /></el-form-item>
              </div>
              <div class="form-actions"><el-button type="success" @click="resetPassword" :loading="loading.security" class="action-btn">立即重置密码</el-button></div>
            </el-form>
          </div>

          <div class="form-section settings-card" style="margin-top:24px">
            <h3 class="section-title">隐私展示设置</h3>
            <p class="section-desc">控制您的哪些信息在个人主页对其他人可见。</p>
            <div class="privacy-list">
              <div class="privacy-item"><span>公开展示性别</span><el-switch v-model="privacy.gender" @change="savePrivacy('gender', privacy.gender)" /></div>
              <div class="privacy-item"><span>公开展示邮箱</span><el-switch v-model="privacy.email" @change="savePrivacy('email', privacy.email)" /></div>
              <div class="privacy-item"><span>公开展示手机号</span><el-switch v-model="privacy.phone" @change="savePrivacy('phone', privacy.phone)" /></div>
              <div class="privacy-item"><span>公开展示 QQ/微信</span><el-switch v-model="privacy.qq" @change="savePrivacy('qq', privacy.qq)" /></div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'notif'" class="tab-pane animate-fade">
          <div class="form-section settings-card">
            <h3 class="section-title">绑定邮箱修改</h3>
            <p class="section-desc">更改用于接收系统通知和找回密码的电子邮箱。</p>
            <el-form :rules="rules" :model="emailForm" label-position="top" ref="emailFormRef">
              <el-form-item label="新邮箱地址" prop="email"><el-input v-model="emailForm.email" /></el-form-item>
              <el-form-item label="验证码" prop="code">
                <div class="code-row">
                  <el-input v-model="emailForm.code" />
                  <el-button type="primary" @click="sendEmailCode" :disabled="codeTime > 0" plain>{{ codeTime > 0 ? `${codeTime}s 后获取` : "获取验证码" }}</el-button>
                </div>
              </el-form-item>
              <div class="form-actions"><el-button type="primary" @click="modifyEmail" class="action-btn">验证并绑定</el-button></div>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.setting-page { max-width: 1000px; margin: 32px auto; padding: 0 4px; }
.setting-layout { display: grid; grid-template-columns: 260px 1fr; gap: 28px; align-items: flex-start; }
.setting-sidebar { position: sticky; top: 80px; }
.profile-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 18px; padding: 28px 20px; box-shadow: 0 2px 12px rgba(0,0,0,.04); }
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; margin-bottom: 20px; }
.avatar-wrap { position: relative; .profile-avatar { border: 3px solid var(--el-bg-color-page); box-shadow: 0 4px 12px rgba(0,0,0,0.1); } .avatar-upload-btn { position: absolute; bottom: 2px; right: 2px; width: 28px; height: 28px; background: var(--el-color-primary); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.2); } }
.profile-info { text-align: center; .profile-name { font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); } .profile-email { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; } }
.setting-nav { display: flex; flex-direction: column; gap: 2px; padding-top: 16px; border-top: 1px solid var(--el-border-color-lighter); .nav-item { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 10px; font-size: 14px; font-weight: 600; color: var(--el-text-color-regular); cursor: pointer; transition: all .2s; &:hover { background: var(--el-fill-color-light); color: var(--el-text-color-primary); } &.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); } } }
.setting-content { display: flex; flex-direction: column; gap: 20px; }
.form-section { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 18px; padding: 28px 32px; box-shadow: 0 2px 12px rgba(0,0,0,.04); }
.section-title { font-size: 17px; font-weight: 800; color: var(--el-text-color-primary); margin: 0 0 6px; }
.section-desc { font-size: 13px; color: var(--el-text-color-secondary); margin: 0 0 22px; line-height: 1.6; }
.form-group { display: flex; gap: 20px; > * { flex: 1; } }
.form-actions { display: flex; justify-content: flex-end; margin-top: 8px; padding-top: 18px; border-top: 1px solid var(--el-border-color-lighter); }
.code-row { display: flex; gap: 12px; width: 100%; }
.action-btn { font-weight: 700; height: 40px; padding: 0 24px; border-radius: 10px; }
.custom-radio-group :deep(.el-radio-button__inner) { border-radius: 8px !important; margin-right: 8px; border: 1px solid var(--el-border-color); box-shadow: none !important; }
.custom-radio-group :deep(.el-radio-button:first-child .el-radio-button__inner), .custom-radio-group :deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 8px !important; }
.custom-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) { border-color: var(--el-color-primary); }
.privacy-list { display: flex; flex-direction: column; gap: 10px; }
.privacy-item { display: flex; justify-content: space-between; align-items: center; padding: 13px 16px; background: var(--el-fill-color-lighter); border-radius: 10px; span { font-size: 14px; font-weight: 600; color: var(--el-text-color-regular); } }
.cover-section { margin-bottom: 24px; }
.cover-editor { display: flex; gap: 18px; align-items: flex-start; }
.cover-preview { width: 320px; height: 120px; border-radius: 12px; overflow: hidden; border: 1px solid var(--el-border-color-lighter); background: linear-gradient(135deg, #dbeafe 0%, #c4b5fd 48%, #818cf8 100%); flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.cover-image { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover-placeholder { padding: 16px; font-size: 12px; line-height: 1.6; color: rgba(255,255,255,.9); }
.cover-actions { display: flex; flex-direction: column; justify-content: center; gap: 10px; min-width: 0; }
.cover-tip { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.6; }
.animate-fade { animation: fadeIn 0.25s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
:deep(.el-form-item__label) { font-size: 13px; font-weight: 600; color: var(--el-text-color-regular); }
:deep(.el-input__wrapper) { min-height: 44px; border-radius: 10px; }
:deep(.el-textarea__inner) { min-height: 120px !important; border-radius: 10px; font-family: inherit; }
@media (max-width: 900px) { .setting-layout { grid-template-columns: 1fr; } .setting-sidebar { position: static; } }
@media (max-width: 720px) { .cover-editor, .form-group, .code-row { flex-direction: column; } .cover-preview { width: 100%; } }
</style>
