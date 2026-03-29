<script setup>

import {Key, Lock, Message, User} from "@element-plus/icons-vue";
import router from "@/router/index.js";
import {reactive, ref, watch} from "vue";
import {ElMessage} from "element-plus";
import {askCodeForType, doPost} from "@/net/api.js";

const form = reactive({
  email: "",
  username: "",
  password: "",
  repeat_password: "",
  code: ""
})

const formRef = ref()

const codeTime = ref(0)

let codeTimeDown;

const validatePass = (rule, value, callback) => {
  if (value === "") {
    callback(new Error("重复密码不能为空"))
  } else if (value !== form.password) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (value === "") {
    callback(new Error("邮箱不能为空"))
  } else if (!/^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/.test(value)) {
    callback(new Error("邮箱格式不正确"))
  } else {
    callback()
  }
}

const validateUsername = (rule, value, callback) => {
  if (value === '') {
    callback(new Error("用户名不能为空"))
  } else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) {
    callback(new Error("用户名不能包含特殊字符, 只能是中文/英文"))
  } else {
    callback()
  }
}

const rules = reactive({
  email: [{required: true, validator: validateEmail, trigger: 'blur'}],
  username: [{required: true, validator: validateUsername, trigger: 'blur'}],
  password: [{required: true, message: "密码不能为空", trigger: 'blur'}, {min : 6, message: "密码长度不能小于6"}],
  repeat_password: [{required: true, validator: validatePass, trigger: 'blur'}],
  code: [{required: true, message: "验证码不能为空", trigger: 'blur'}]
})

const askCode = () => {
  if (/^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/.test(form.email)) {
    askCodeForType(form.email, "register", (data) => {
      ElMessage.success({message: "验证码发送成功, 请及时查看", plain: true})
      codeTime.value = 60;
      codeTimeDown = setInterval(() => {codeTime.value--}, 1000)
    })
  } else {
    ElMessage.warning("电子邮件格式不正确")
  }
}

watch(codeTime, () => {
  if (codeTime.value === 0) {
    clearInterval(codeTimeDown)
  }
})

const register = (formRef) => {
  if (!formRef) return;
  formRef.validate(valid => {
    if (valid) {
      doPost("/api/auth/register", {
        email: form.email,
        username: form.username,
        password: form.repeat_password,
        code: form.code
      }, {"Content-Type" : "application/x-www-form-urlencoded"}, () => {
        ElMessage.success({message: "注册成功!", plain: true})
        router.push("/")
      });
    }
  })
}

</script>

<template>
  <div class="register-page">
    <div class="header">
      <div class="logo-text">青研社</div>
      <h2 class="title">注册新账号</h2>
      <div class="subtitle">填写以下信息加入我们的社区</div>
    </div>

    <div class="form-container">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="电子邮箱地址" :prefix-icon="Message"/>
        </el-form-item>
        
        <el-row :gutter="10">
          <el-col :span="16">
            <el-form-item prop="code">
              <el-input v-model="form.code" maxlength="6" placeholder="邮箱验证码" :prefix-icon="Key"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item>
              <el-button 
                type="primary" 
                class="code-btn"
                @click="askCode" 
                :disabled="codeTime !== 0 || !form.email">
                {{ codeTime > 0 ? `${codeTime}s` : "获取验证码" }}
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="建议使用真实姓名" :prefix-icon="User"/>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="设置登录密码" :prefix-icon="Lock"/>
        </el-form-item>
        
        <el-form-item prop="repeat_password">
          <el-input v-model="form.repeat_password" type="password" show-password placeholder="再次输入密码" :prefix-icon="Lock"/>
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button type="primary" class="register-btn" @click="register(formRef)">立即注册</el-button>
        <div class="footer-links">
          <span>已有账号?</span>
          <el-link type="primary" @click="router.push('/')">立即登录</el-link>
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

.register-page {
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
  font-size: 30px;
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
  margin-bottom: 20px;
}

.code-btn {
  width: 100%;
  padding: 10px 0;
  height: 48px;
  border-radius: 12px;
  background-color: #0d4a75;
  border-color: #0d4a75;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.register-btn {
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

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(13, 74, 117, 0.3);
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 5px;
  font-size: 14px;
  color: #666;
}
</style>