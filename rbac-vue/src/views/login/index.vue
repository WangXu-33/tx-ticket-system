<template>
  <div class="login-page">
    <div class="orb orb-a"></div>
    <div class="orb orb-b"></div>

    <section class="brand-panel">
      <p class="eyebrow">TX Ticket System</p>
      <h1>头绪工单系统</h1>
      <p class="subtitle">客户提单、运维流转、知识沉淀的一体化服务台。</p>
      <div class="flow-line">
        <span>提交</span>
        <i></i>
        <span>受理</span>
        <i></i>
        <span>解决</span>
        <i></i>
        <span>沉淀</span>
      </div>
    </section>

    <section class="auth-card">
      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="账号登录">
          <a-form :model="loginForm" layout="vertical" @finish="handleLogin">
            <a-form-item name="username" label="用户名" :rules="[{ required: true, message: '请输入用户名' }]">
              <a-input v-model:value="loginForm.username" size="large" placeholder="请输入用户名">
                <template #prefix><user-outlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item name="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model:value="loginForm.password" size="large" placeholder="请输入密码">
                <template #prefix><lock-outlined /></template>
              </a-input-password>
            </a-form-item>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">登录</a-button>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="客户注册">
          <a-form :model="registerForm" layout="vertical" @finish="handleRegister">
            <a-form-item name="username" label="用户名" :rules="[{ required: true, message: '请输入用户名' }]">
              <a-input v-model:value="registerForm.username" size="large" placeholder="用于登录的账号" />
            </a-form-item>
            <a-form-item name="nickname" label="客户名称">
              <a-input v-model:value="registerForm.nickname" size="large" placeholder="企业或联系人名称" />
            </a-form-item>
            <a-form-item name="phone" label="联系电话">
              <a-input v-model:value="registerForm.phone" size="large" placeholder="便于运维联系" />
            </a-form-item>
            <a-form-item name="email" label="邮箱">
              <a-input v-model:value="registerForm.email" size="large" placeholder="可选" />
            </a-form-item>
            <a-form-item name="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model:value="registerForm.password" size="large" placeholder="请设置密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">注册客户账号</a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import request from '@/api/request'
import { refreshPermissionCache } from '@/utils/permission'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: 'admin', password: '' })
const registerForm = reactive({ username: '', nickname: '', phone: '', email: '', password: '' })

const handleLogin = async (values) => {
  loading.value = true
  try {
    const res = await request.post('/auth/login', values)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    const info = await request.get('/auth/info')
    localStorage.setItem('permissions', JSON.stringify(info.data.permissions))
    refreshPermissionCache()
    message.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    await request.post('/auth/register', registerForm)
    message.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    registerForm.username = ''
    registerForm.nickname = ''
    registerForm.phone = ''
    registerForm.email = ''
    registerForm.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 44px;
  align-items: center;
  padding: 56px clamp(24px, 8vw, 120px);
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.9), rgba(15, 118, 110, 0.82)),
    radial-gradient(circle at 10% 20%, rgba(20, 184, 166, 0.42), transparent 26%),
    #0f172a;
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(16px);
  opacity: 0.52;
}

.orb-a {
  width: 320px;
  height: 320px;
  right: 8%;
  top: 8%;
  background: #22c55e;
}

.orb-b {
  width: 240px;
  height: 240px;
  left: 8%;
  bottom: 12%;
  background: #38bdf8;
}

.brand-panel,
.auth-card {
  position: relative;
  z-index: 1;
}

.brand-panel {
  color: #f8fafc;
}

.eyebrow {
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #99f6e4;
  font-weight: 800;
  margin-bottom: 18px;
}

h1 {
  font-size: clamp(42px, 7vw, 82px);
  line-height: 0.98;
  margin: 0 0 24px;
  font-weight: 900;
}

.subtitle {
  font-size: 18px;
  max-width: 560px;
  color: #d1fae5;
}

.flow-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 38px;
  color: #ecfeff;
  font-weight: 700;
}

.flow-line i {
  width: 42px;
  height: 2px;
  background: rgba(204, 251, 241, 0.52);
}

.auth-card {
  padding: 34px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 30px 80px rgba(2, 6, 23, 0.32);
  backdrop-filter: blur(18px);
}

.auth-card :deep(.ant-tabs-tab) {
  font-size: 16px;
  font-weight: 800;
}

.auth-card :deep(.ant-input),
.auth-card :deep(.ant-input-affix-wrapper) {
  border-radius: 14px;
}

.auth-card :deep(.ant-btn) {
  height: 46px;
  border-radius: 14px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 36px 18px;
  }

  .brand-panel {
    text-align: center;
  }

  .subtitle {
    margin-inline: auto;
  }

  .flow-line {
    justify-content: center;
    flex-wrap: wrap;
  }

  .auth-card {
    width: min(100%, 460px);
    margin: 0 auto;
  }
}
</style>
