<template>
  <div class="page-container">
    <a-row :gutter="24">
      <!-- 左侧：个人基本信息卡片 -->
      <a-col :span="8">
        <a-card class="profile-card slide-in-1" :bordered="false">
          <div class="user-info-head">
            <div class="avatar-wrapper">
              <a-upload
                name="file"
                :show-upload-list="false"
                action="/api/system/upload"
                :headers="headers"
                @change="handleAvatarChange"
              >
                <div class="avatar-mask">
                  <CameraOutlined />
                  <span>修改头像</span>
                </div>
                <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="user-avatar" />
                <a-avatar v-else :size="120" style="background-color: #f5f5f5; color: #ccc">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
              </a-upload>
            </div>
            <div class="user-info-text">
              <h2>{{ user.nickname || user.username }}</h2>
              <p>{{ user.remark || '这位管理员很懒，什么都没写' }}</p>
            </div>
          </div>
          <a-divider />
          <ul class="user-details">
            <li><UserOutlined /> <span>账号：</span> {{ user.username }}</li>
            <li><PhoneOutlined /> <span>手机：</span> {{ user.phone || '未绑定' }}</li>
            <li><MailOutlined /> <span>邮箱：</span> {{ user.email || '未绑定' }}</li>
            <li><CalendarOutlined /> <span>加入时间：</span> {{ formatDateTime(user.createTime) }}</li>
          </ul>
        </a-card>
      </a-col>

      <!-- 右侧：资料修改与安全设置 -->
      <a-col :span="16">
        <a-card class="profile-tabs-card slide-in-2" :bordered="false">
          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="basic" tab="基本资料">
              <a-form :model="user" layout="vertical" @finish="handleUpdateProfile">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="用户昵称">
                      <a-input v-model:value="user.nickname" placeholder="请输入昵称" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="手机号码">
                      <a-input v-model:value="user.phone" placeholder="请输入手机号" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="电子邮箱">
                      <a-input v-model:value="user.email" placeholder="请输入邮箱" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="24">
                    <a-form-item label="备注说明">
                      <a-textarea v-model:value="user.remark" :rows="4" placeholder="写点什么介绍下自己..." />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="saveLoading">保存资料</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>
            <a-tab-pane key="security" tab="安全设置">
              <a-form :model="pwdForm" layout="vertical" @finish="handleUpdatePwd">
                <a-form-item label="原密码" required>
                  <a-input-password v-model:value="pwdForm.oldPwd" placeholder="请输入原密码" />
                </a-form-item>
                <a-form-item label="新密码" required>
                  <a-input-password v-model:value="pwdForm.newPwd" placeholder="请输入新密码" />
                </a-form-item>
                <a-form-item label="确认新密码" required>
                  <a-input-password v-model:value="pwdForm.confirmPwd" placeholder="请再次输入新密码" />
                </a-form-item>
                <a-form-item>
                  <a-button type="primary" danger html-type="submit" :loading="pwdLoading">修改密码</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import { 
  UserOutlined, PhoneOutlined, MailOutlined, CalendarOutlined, CameraOutlined 
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import { extractFileId } from '@/utils/file'
import { getAuthedPreviewUrl, revokeAuthedPreviewUrl } from '@/utils/file-preview'
import { formatDateTime } from '@/utils/datetime'

const activeTab = ref('basic')
const user = ref({})
const saveLoading = ref(false)
const pwdLoading = ref(false)
const headers = { Authorization: localStorage.getItem('token') }
const avatarPreviewUrl = ref('')

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const fetchUserInfo = async () => {
  const res = await request.get('/system/user/profile')
  user.value = res.data || {}
  localStorage.setItem('user', JSON.stringify(user.value))
}

const syncAvatarPreview = async (nextId, prevId) => {
  if (prevId && String(prevId) !== String(nextId)) {
    revokeAuthedPreviewUrl(prevId)
  }
  if (!nextId) {
    avatarPreviewUrl.value = ''
    return
  }
  try {
    avatarPreviewUrl.value = await getAuthedPreviewUrl(nextId)
  } catch {
    avatarPreviewUrl.value = ''
  }
}

const handleAvatarChange = (info) => {
  if (info.file.status === 'done') {
    const res = info.file.response
    if (res.code === 200) {
      user.value.avatarFileId = extractFileId(res.data)
      handleUpdateProfile()
    }
  } else if (info.file.status === 'error') {
    message.error('头像上传失败')
  }
}

const handleUpdateProfile = async () => {
  saveLoading.value = true
  try {
    await request.post('/system/user/updateProfile', user.value)
    message.success('资料更新成功')
    localStorage.setItem('user', JSON.stringify(user.value))
  } finally {
    saveLoading.value = false
  }
}

const handleUpdatePwd = async () => {
  if (pwdForm.newPwd !== pwdForm.confirmPwd) {
    return message.error('两次输入密码不一致')
  }
  pwdLoading.value = true
  try {
    await request.post('/system/user/updatePwd', pwdForm)
    message.success('密码修改成功，请重新登录')
    localStorage.clear()
    setTimeout(() => {
      location.href = '/login'
    }, 500)
  } finally {
    pwdLoading.value = false
  }
}

onMounted(fetchUserInfo)

watch(
  () => user.value.avatarFileId,
  (nextId, prevId) => {
    syncAvatarPreview(nextId, prevId)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (user.value.avatarFileId) {
    revokeAuthedPreviewUrl(user.value.avatarFileId)
  }
})
</script>

<style scoped>
.profile-card, .profile-tabs-card { border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.user-info-head { text-align: center; padding: 20px 0; }
.avatar-wrapper { 
  position: relative; width: 120px; height: 120px; margin: 0 auto 15px; border-radius: 50%; overflow: hidden;
  cursor: pointer;
}
.user-avatar { width: 100%; height: 100%; object-fit: cover; }
.avatar-mask { 
  position: absolute; inset: 0; background: rgba(0,0,0,0.4); color: #fff;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  opacity: 0; transition: opacity 0.3s; z-index: 10;
}
.avatar-wrapper:hover .avatar-mask { opacity: 1; }
.user-info-text h2 { margin: 10px 0 5px; font-weight: 700; }
.user-info-text p { color: #94a3b8; font-size: 13px; }

.user-details { list-style: none; padding: 0; margin: 20px 0; }
.user-details li { margin-bottom: 12px; font-size: 14px; color: #475569; display: flex; align-items: center; }
.user-details li .anticon { margin-right: 10px; color: #94a3b8; }
.user-details li span { color: #94a3b8; width: 80px; }

.slide-in-1 { animation: slide-up 0.5s both; }
.slide-in-2 { animation: slide-up 0.5s both 0.1s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
</style>
