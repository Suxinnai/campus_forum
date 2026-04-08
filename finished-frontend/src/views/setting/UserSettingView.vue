<script setup>
import { Message, Refresh, Select, User } from "@element-plus/icons-vue";
import { useAppStore } from "@/stores/app-store.js";
import { computed, reactive, ref } from "vue";
import { get, getToken, post } from "@/net/api.js";
import { ElMessage } from "element-plus";
import axios from "axios";
import { Camera, Mail, Save, Shield } from 'lucide-vue-next'

const store = useAppStore()
const baseFormRef = ref();
const emailFormRef = ref();
const securityFormRef = ref();
const activeTab = ref('profile')
const desc = ref("")
const loading = reactive({ form: true, base: false, security: false })

const baseForm = reactive({ username: "", gender: 1, phone: "", qq: "", desc: "" })
const emailForm = reactive({ email: "", code: "" })
const securityForm = reactive({ password: "", newPassword: "", newPasswordRepeat: "" })
const privacy = reactive({ phone: false, qq: false, email: false, gender: false })

const validateUsername = (rule, value, callback) => {
  if (value === '') callback(new Error("用户名不能为空"))
  else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) callback(new Error("用户名不能包含特殊字符"))
  else callback()
}

const rules = {
  username: [{ validator: validateUsername, trigger: ["blur", "change"] }, { min: 2, max: 30, message: "用户名长度2-30字符", trigger: ["blur", "change"] }],
  email: [{ required: true, message: "邮件地址不能为空", trigger: ["blur", "change"] }, { type: "email", message: "请输入合法邮箱", trigger: ["blur", "change"] }],
  code: [{ required: true, message: "验证码不能为空", trigger: ["blur", "change"] }, { min: 6, max: 6, message: "验证码6位", trigger: ["blur", "change"] }]
}

const saveDetails = () => {
  baseFormRef.value.validate(isValid => {
    if (isValid) {
      loading.base = true;
      post("/api/user/save-details", baseForm, () => {
        ElMessage.success({ message: "用户信息更新成功", plain: true })
        store.user.username = baseForm.username
        desc.value = baseForm.desc
        loading.base = false;
      }, (message) => {
        ElMessage.warning({ message, plain: true })
        loading.base = false;
      })
    }
  })
}

get("/api/user/details", data => {
  baseForm.username = store.user.username;
  baseForm.gender = data.gender;
  baseForm.phone = data.phone;
  baseForm.qq = data.qq;
  baseForm.desc = desc.value = data.desc;
  emailForm.email = store.user.email
  loading.form = false;
})

const codeTime = ref(0);

function sendEmailCode() {
  if (/^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/.test(emailForm.email)) {
    codeTime.value = 60;
    get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
      ElMessage.success({ message: `验证码已发送至${emailForm.email}`, plain: true })
      const handle = setInterval(() => { codeTime.value--; if (codeTime.value === 0) clearInterval(handle); }, 1000)
    }, (message) => { codeTime.value = 0; ElMessage.warning({ message, plain: true }) })
  } else {
    ElMessage.warning({ message: "电子邮件格式不正确", plain: true })
  }
}

const modifyEmail = () => {
  emailFormRef.value.validate(value => {
    if (value) {
      post("/api/user/modify-email", emailForm, () => {
        ElMessage.success({ message: "邮件修改成功", plain: true })
        store.user.email = emailForm.email;
      })
    }
  })
}

// Privacy & Password
get("/api/user/privacy", (data) => {
  privacy.phone = data.phone;
  privacy.qq = data.qq;
  privacy.email = data.email;
  privacy.gender = data.gender;
})

function savePrivacy(type, status) {
  post("/api/user/save-privacy", { type, status }, () => {
    ElMessage.success({ message: "隐私设置已更新", plain: true })
  })
}

function resetPassword() {
  securityFormRef.value.validate(valid => {
    if (valid) {
      loading.security = true;
      post("/api/user/change-password", {
        password: securityForm.password,
        newPassword: securityForm.newPasswordRepeat
      }, () => {
        ElMessage.success({ message: "密码修改成功", plain: true })
        securityFormRef.value.resetFields();
        loading.security = false;
      }, (err) => { loading.security = false; ElMessage.error(err); })
    }
  })
}

const passMatch = (rule, value, callback) => {
  if (value !== securityForm.newPassword) callback(new Error("两次密码不一致"))
  else callback()
}

const securityRules = {
  password: [{ required: true, message: "请输入原密码", trigger: 'blur' }],
  newPassword: [{ required: true, message: "请输入新密码", trigger: 'blur' }, { min: 6, message: "长度不少于6位", trigger: "blur" }],
  newPasswordRepeat: [{ validator: passMatch, trigger: ["blur", "change"] }]
}

function beforeAvatarUpload(rawFile) {
  if (rawFile.type !== "image/jpeg" && rawFile.type !== 'image/png') {
    ElMessage.warning({ message: "头像图片只能是JPG/PNG格式", plain: true })
    return false;
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.warning({ message: "头像图片大小不能大于2MB", plain: true })
    return false;
  }
  return true;
}

function uploadSuccess(response) {
  ElMessage.success({ message: response.message, plain: true })
  store.user.avatar = response.data;
}

const getAvatar = computed(() =>
  store.user.avatar?.length > 0
    ? `${axios.defaults.baseURL}/images${store.user.avatar}`
    : "https://www.vexipui.com/qmhc.jpg"
);
</script>

<template>
  <div class="setting-page ds-page">
    <div class="setting-layout">
      <!-- Left Sidebar: Profile Summary & Tabs -->
      <aside class="setting-sidebar">
        <div class="profile-card ds-card">
          <div class="avatar-section">
            <div class="avatar-wrap">
              <el-avatar :size="80" :src="getAvatar" class="profile-avatar" />
              <el-upload
                :action="axios.defaults.baseURL + '/api/image/avatar'"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :on-success="uploadSuccess"
                :headers="{'Authorization': `Bearer ${getToken()}`}"
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

      <!-- Right Content: Forms -->
      <div class="setting-content">
        <!-- Tab: Profile -->
        <div v-if="activeTab === 'profile'" class="tab-pane animate-fade">
          <div class="form-section ds-card" v-loading="loading.form">
            <h3 class="section-title">个人信息</h3>
            <p class="section-desc">管理您的公开昵称和自我介绍，让其他校友更好地认识你。</p>
            <el-form :model="baseForm" :rules="rules" label-position="top" ref="baseFormRef">
              <div class="form-group">
                <el-form-item label="公开昵称" prop="username">
                  <el-input v-model="baseForm.username" maxlength="30" />
                </el-form-item>
                <el-form-item label="性别" style="width: 200px">
                  <el-radio-group v-model="baseForm.gender" class="ds-radio-group">
                    <el-radio-button :value="0">男</el-radio-button>
                    <el-radio-button :value="1">女</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </div>
              <el-form-item label="自我介绍" prop="desc">
                <el-input type="textarea" v-model="baseForm.desc" :rows="4" maxlength="200" show-word-limit />
              </el-form-item>
              <div class="form-actions"><el-button type="primary" @click="saveDetails" class="ds-action-btn">保存更改</el-button></div>
            </el-form>
          </div>

          <div class="form-section ds-card" style="margin-top:24px">
            <h3 class="section-title">联系方式</h3>
            <p class="section-desc">填写您的校内联系渠道，方便志同道合的人联系你。</p>
            <el-form :model="baseForm" label-position="top">
              <div class="form-group">
                <el-form-item label="手机号码" style="flex:1"><el-input v-model="baseForm.phone" maxlength="11" /></el-form-item>
                <el-form-item label="QQ / 微信" style="flex:1"><el-input v-model="baseForm.qq" maxlength="20" /></el-form-item>
              </div>
              <div class="form-actions"><el-button type="primary" plain @click="saveDetails" class="ds-action-btn">更新联系方式</el-button></div>
            </el-form>
          </div>
        </div>

        <!-- Tab: Security -->
        <div v-if="activeTab === 'security'" class="tab-pane animate-fade">
          <div class="form-section ds-card">
            <h3 class="section-title">修改密码</h3>
            <p class="section-desc">定期更换密码有助于保障您的账号安全。</p>
            <el-form :model="securityForm" :rules="securityRules" label-position="top" ref="securityFormRef">
              <el-form-item label="当前密码" prop="password"><el-input type="password" v-model="securityForm.password" show-password /></el-form-item>
              <div class="form-group">
                <el-form-item label="新密码" prop="newPassword" style="flex:1"><el-input type="password" v-model="securityForm.newPassword" show-password /></el-form-item>
                <el-form-item label="确认新密码" prop="newPasswordRepeat" style="flex:1"><el-input type="password" v-model="securityForm.newPasswordRepeat" show-password /></el-form-item>
              </div>
              <div class="form-actions"><el-button type="success" @click="resetPassword" :loading="loading.security" class="ds-action-btn">立即重置密码</el-button></div>
            </el-form>
          </div>

          <div class="form-section ds-card" style="margin-top:24px">
            <h3 class="section-title">隐私展示设置</h3>
            <p class="section-desc">控制您的哪些信息在个人主页对其他人可见。</p>
            <div class="privacy-list">
              <div class="privacy-item">
                <span>公开展示性别</span>
                <el-switch v-model="privacy.gender" @change="savePrivacy('gender', privacy.gender)" />
              </div>
              <div class="privacy-item">
                <span>公开展示邮箱</span>
                <el-switch v-model="privacy.email" @change="savePrivacy('email', privacy.email)" />
              </div>
              <div class="privacy-item">
                <span>公开展示手机号</span>
                <el-switch v-model="privacy.phone" @change="savePrivacy('phone', privacy.phone)" />
              </div>
              <div class="privacy-item">
                <span>公开展示 QQ/微信</span>
                <el-switch v-model="privacy.qq" @change="savePrivacy('qq', privacy.qq)" />
              </div>
            </div>
          </div>
        </div>

        <!-- Tab: Notif -->
        <div v-if="activeTab === 'notif'" class="tab-pane animate-fade">
          <div class="form-section ds-card">
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
              <div class="form-actions"><el-button type="primary" @click="modifyEmail" class="ds-action-btn">验证并绑定</el-button></div>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.setting-page {
  max-width: 1000px;
  margin: 32px auto;
  padding: 0 4px;
}

.setting-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 28px;
  align-items: flex-start;
}

.setting-sidebar {
  position: sticky;
  top: 80px;
}

.profile-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  padding: 28px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.avatar-wrap {
  position: relative;
  .profile-avatar {
    border: 3px solid var(--el-bg-color-page);
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }

  .avatar-upload-btn {
    position: absolute;
    bottom: 2px; right: 2px;
    width: 28px; height: 28px;
    background: var(--el-color-primary);
    color: #fff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    transition: all 0.2s;
    &:hover { transform: scale(1.1); background: var(--el-color-primary-dark-2); }
  }
}

.profile-info {
  text-align: center;
  .profile-name { font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); }
  .profile-email { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
}

.setting-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);

  .nav-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 11px 14px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition: all 0.2s;

    &:hover { background: var(--el-fill-color-light); color: var(--el-text-color-primary); }
    &.active {
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }
  }
}

.setting-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  padding: 28px 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
}

.section-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin: 0 0 6px;
}

.section-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 22px;
  line-height: 1.6;
}

.form-group {
  display: flex;
  gap: 20px;
  > * { flex: 1; }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.ds-action-btn {
  font-weight: 700;
  height: 40px;
  padding: 0 24px;
  border-radius: 10px;
}

.privacy-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.privacy-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
  transition: background 0.2s;

  &:hover { background: var(--el-fill-color-light); }

  span { font-size: 14px; font-weight: 600; color: var(--el-text-color-regular); }
}

.animate-fade {
  animation: fadeIn 0.25s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

// El-form label & input tweaks
:deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 10px;
}

:deep(.el-textarea__inner) {
  min-height: 120px !important;
  border-radius: 10px;
  font-family: inherit;
}
</style>
