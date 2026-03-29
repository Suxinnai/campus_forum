<script setup>

import {Lock, User} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import router from "@/router/index.js";
import {ElMessage} from "element-plus";
import {login} from "@/net/api.js"

const form = reactive({
  username: "",
  password: "",
  remember: false
})

const formRef = ref()

const rules = reactive({
  username: [{required: true, message: "用户名/邮箱不能为空", trigger: 'blur'}],
  password: [{required: true, message: "密码不能为空", trigger: "blur"}]
})

const loginValid = (formRef) => {
  if (!formRef) return;
  formRef.validate((valid) => {
    if (valid) {
      login(form.username, form.password, form.remember)
    } else {
      ElMessage.warning("err")
    }
  })
}
</script>

<template>
  <div class="login-page">
    <div class="header">
      <div class="logo-text">青研社</div>
      <h2 class="title">登录青研社</h2>
      <div class="subtitle">请输入账号和密码以继续</div>
    </div>
    
    <div class="form-container">
      <el-form :rules="rules" :model="form" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名 / 邮箱" maxlength="30" :prefix-icon="User"/>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock"/>
        </el-form-item>
      </el-form>
      
      <div class="options">
        <el-checkbox v-model="form.remember">记住我</el-checkbox>
        <el-link @click="router.push('/reset')">忘记密码?</el-link>
      </div>

      <div class="actions">
        <el-button type="primary" class="login-btn" @click="loginValid(formRef)">登录</el-button>
        <div class="footer-links">
          <span>还没有账号?</span>
          <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  background-color: #f9fbff;
  box-shadow: 0 0 0 1px #e4e7ed inset !important;
  border-radius: 12px;
  padding: 0 16px;
  height: 48px;
  transition: all 0.3s;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  box-shadow: 0 0 0 1px #0d4a75 inset !important;
}

.login-page {
  width: 100%;
  animation: slide-up 0.6s ease-out;
}

@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.header {
  margin-bottom: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.logo-text {
  font-size: 13px;
  font-weight: 500;
  color: #8c98a9;
  letter-spacing: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.logo-text::before,
.logo-text::after {
  content: "";
  display: block;
  width: 32px;
  height: 1px;
  background-color: #e2e8f0;
}

.title {
  font-size: 32px;
  font-weight: 900;
  color: #1a1a1a;
  margin: 0 0 6px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 14px;
  color: #8c98a9;
  margin: 0;
  line-height: 1.5;
}

.form-container {
  margin-bottom: 24px;
}

.options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -10px 0 24px 0;
  font-size: 14px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #0d4a75 0%, #1a5a8a 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(13, 74, 117, 0.2);
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(13, 74, 117, 0.3);
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #4a5568;
}
</style>