<template>
  <div class="user-profile-container">
    <!-- 返回导航区 -->
    <div class="page-header slide-in-1">
      <a-button type="link" @click="router.back()">
        <template #icon><arrow-left-outlined /></template> 返回用户列表
      </a-button>
      <div class="header-actions">
        <a-button v-hasPerm="saveUserPerms" type="primary" :loading="submitting" @click="submitForm">
          <template #icon><save-outlined /></template> {{ isEdit ? '保存资料' : '创建用户' }}
        </a-button>
      </div>
    </div>

    <a-row :gutter="[24, 24]">
      <!-- 左侧：身份名片卡 (Identity Card) -->
      <a-col :xs="24" :md="8" :lg="6">
        <a-card class="identity-card slide-in-2" :bordered="false">
          <div class="avatar-section">
            <a-upload
              name="file"
              list-type="picture-card"
              class="avatar-uploader"
              :show-upload-list="false"
              action="/api/system/upload"
              :headers="uploadHeaders"
              @change="handleAvatarChange"
            >
              <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="avatar" class="user-avatar-img" />
              <div v-else class="user-avatar-placeholder">
                {{ form.nickname ? form.nickname.charAt(0) : 'U' }}
              </div>
            </a-upload>
            <div class="user-name">{{ form.nickname || '新用户' }}</div>
            <div class="user-role">系统用户</div>
            <div class="status-badge" :class="form.status === 1 ? 'status-active' : 'status-inactive'">
              {{ form.status === 1 ? '● 账号正常' : '● 账号停用' }}
            </div>
          </div>
          <a-divider style="margin: 16px 0;" />
          <div class="info-list">
            <div class="info-item">
              <span class="label"><mail-outlined /> 邮箱</span>
              <span class="value">{{ form.email || '未填写' }}</span>
            </div>
            <div class="info-item">
              <span class="label"><phone-outlined /> 电话</span>
              <span class="value">{{ form.phone || '未填写' }}</span>
            </div>
            <div class="info-item">
              <span class="label"><environment-outlined /> 地址</span>
              <span class="value">{{ form.address || '未填写' }}</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧：详细内容区 (Tabs) -->
      <a-col :xs="24" :md="16" :lg="18">
        <a-card class="detail-card slide-in-3" :bordered="false" :bodyStyle="{ padding: '0 24px 24px' }">
          <a-tabs v-model:activeKey="activeTab" class="profile-tabs">
            <!-- Tab 1: 基础资料编辑 -->
            <a-tab-pane key="basic" tab="基础资料">
              <div class="tab-content">
                <a-form :model="form" layout="vertical" ref="formRef">
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="登录账号" name="username" :rules="[{ required: true, message: '账号必填' }]">
                        <a-input v-model:value="form.username" placeholder="建议使用拼音或工号" :disabled="isEdit" size="large" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12" v-if="!isEdit">
                      <a-form-item label="初始密码" name="password" :rules="[{ required: true, message: '密码必填' }]">
                        <a-input-password v-model:value="form.password" placeholder="请输入密码" size="large" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="真实姓名 / 昵称" name="nickname">
                        <a-input v-model:value="form.nickname" placeholder="如：张三" size="large" />
                      </a-form-item>
                    </a-col>
                    
                    <!-- 新增：所属部门 -->
                    <a-col :span="12">
                      <a-form-item label="所属部门" name="deptId">
                        <a-tree-select
                          v-model:value="form.deptId"
                          style="width: 100%"
                          size="large"
                          :tree-data="deptTree"
                          :fieldNames="{ children: 'children', label: 'deptName', value: 'id' }"
                          tree-default-expand-all
                          placeholder="请选择归属部门"
                          allow-clear
                        />
                      </a-form-item>
                    </a-col>

                    <!-- 新增：角色分配 -->
                    <a-col :span="24">
                      <a-form-item label="分配角色">
                        <a-select
                          v-model:value="assignedRoles"
                          mode="multiple"
                          size="large"
                          style="width: 100%"
                          placeholder="请选择一个或多个角色"
                          allow-clear
                          show-search
                          option-filter-prop="searchText"
                          :filter-option="filterRoleOption"
                          :options="roleOptions"
                          :max-tag-count="4"
                        />
                      </a-form-item>
                    </a-col>

                    <a-col :span="12">
                      <a-form-item label="联系电话" name="phone">
                        <a-input v-model:value="form.phone" placeholder="11位手机号" size="large" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="电子邮箱" name="email">
                        <a-input v-model:value="form.email" placeholder="example@company.com" size="large" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="性别" name="sex">
                        <a-select v-model:value="form.sex" size="large">
                          <a-select-option :value="0">未知</a-select-option>
                          <a-select-option :value="1">男</a-select-option>
                          <a-select-option :value="2">女</a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="24">
                      <a-form-item label="联系地址" name="address">
                        <a-input v-model:value="form.address" placeholder="详细住址或工作地" size="large" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="24">
                      <a-form-item label="个人简介 (Remark)" name="remark">
                        <a-textarea v-model:value="form.remark" :rows="4" placeholder="一句话介绍自己..." />
                      </a-form-item>
                    </a-col>
                    <a-col :span="24">
                      <a-form-item label="账号状态" name="status">
                        <a-switch v-model:checked="form.status" :checkedValue="1" :unCheckedValue="0" checked-children="启用" un-checked-children="停用" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </div>
            </a-tab-pane>

            <a-tab-pane key="permissions" tab="用户直授权限" v-if="isEdit">
              <a-card :bordered="false" class="perm-card">
                <a-form layout="inline" :model="permQuery">
                  <a-form-item label="关键词">
                    <a-input v-model:value="permQuery.keyword" placeholder="名称或权限标识" allow-clear />
                  </a-form-item>
                  <a-form-item label="所属菜单">
                    <a-select
                      v-model:value="permQuery.menuId"
                      placeholder="全部菜单"
                      allow-clear
                      style="width: 220px"
                    >
                      <a-select-option v-for="item in permissionMenuOptions" :key="item.id" :value="item.id">
                        {{ item.title }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item>
                    <a-space>
                      <a-button type="primary" @click="fetchPermissionPage">查询</a-button>
                      <a-button @click="resetPermissionQuery">重置</a-button>
                      <a-button
                        type="primary"
                        ghost
                        @click="saveUserPermissions"
                        v-hasPerm="['sys:user:assignPerm']"
                      >
                        保存直授权限
                      </a-button>
                    </a-space>
                  </a-form-item>
                </a-form>

                <a-table
                  rowKey="id"
                  :dataSource="permissionRows"
                  :columns="permissionColumns"
                  :loading="permissionLoading"
                  :pagination="permissionPagination"
                  :rowSelection="permissionRowSelection"
                  @change="handlePermissionTableChange"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'permKey'">
                      <a-tag color="purple">{{ record.permKey }}</a-tag>
                    </template>
                    <template v-if="column.key === 'menuTitles'">
                      <span>{{ record.menuTitles || '未挂载菜单' }}</span>
                    </template>
                  </template>
                </a-table>
              </a-card>
            </a-tab-pane>

            <a-tab-pane key="security" tab="安全设置" v-if="isEdit">
              <div class="security-panel">
                <div class="sec-item">
                  <div class="sec-info">
                    <h4>重置密码</h4>
                    <p>如果用户忘记密码，管理员可以在此强制重置。</p>
                  </div>
                  <a-button
                    type="default"
                    danger
                    @click="resetPwdVisible = true"
                    v-hasPerm="['sys:user:resetPwd']"
                  >
                    强制重置
                  </a-button>
                </div>
                <a-divider />
                <div class="sec-item">
                  <div class="sec-info">
                    <h4>强制下线</h4>
                    <p>踢出当前正在登录该账号的所有设备。</p>
                  </div>
                  <a-button
                    type="default"
                    @click="handleKickout"
                    v-hasPerm="['sys:user:kickout']"
                  >
                    踢出设备
                  </a-button>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="resetPwdVisible"
      title="重置用户密码"
      @ok="submitResetPassword"
      :confirmLoading="resetPwdLoading"
      destroyOnClose
    >
      <a-form :model="resetPwdForm" layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model:value="resetPwdForm.newPassword" placeholder="请输入6-32位新密码" />
        </a-form-item>
        <a-form-item label="确认密码">
          <a-input-password v-model:value="resetPwdForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  ArrowLeftOutlined, SaveOutlined, MailOutlined, 
  PhoneOutlined, EnvironmentOutlined 
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import { extractFileId } from '@/utils/file'
import { getAuthedPreviewUrl, revokeAuthedPreviewUrl } from '@/utils/file-preview'
import { refreshPermissionCache } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const submitting = ref(false)
const activeTab = ref('basic')
const currentLoginUserId = Number(JSON.parse(localStorage.getItem('user') || '{}').id || 0)

const formRef = ref(null)
const form = reactive({
  id: null, username: '', password: '', nickname: '', 
  avatarFileId: '', email: '', phone: '', sex: 0, address: '', remark: '', status: 1, deptId: null
})

const deptData = ref([])
const allRoles = ref([])
const menuData = ref([])
const assignedRoles = ref([])
const avatarPreviewUrl = ref('')
const checkedUserPermIds = ref([])
const permissionLoading = ref(false)
const permissionRows = ref([])
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)

const permQuery = reactive({ keyword: '', menuId: undefined, pageNum: 1, pageSize: 10 })
const permissionPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})
const resetPwdForm = reactive({ newPassword: '', confirmPassword: '' })

const deptTree = computed(() => {
  const map = {}
  deptData.value.forEach(item => map[item.id] = { ...item, children: [] })
  const tree = []
  deptData.value.forEach(item => {
    if (item.parentId !== 0 && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else {
      tree.push(map[item.id])
    }
  })
  return tree
})

const permissionMenuOptions = computed(() => menuData.value)
const saveUserPerms = computed(() => isEdit.value ? ['sys:user:edit'] : ['sys:user:add'])
const roleOptions = computed(() => allRoles.value.map(role => {
  const roleName = role.roleName || ''
  const roleKey = role.roleKey || ''
  return {
    value: role.id,
    label: roleKey ? `${roleName}（${roleKey}）` : roleName,
    searchText: `${roleName} ${roleKey}`.toLowerCase()
  }
}))

const permissionColumns = [
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '权限标识', dataIndex: 'permKey', key: 'permKey', width: 220 },
  { title: '挂载菜单', dataIndex: 'menuTitles', key: 'menuTitles' }
]

const permissionRowSelection = computed(() => ({
  selectedRowKeys: checkedUserPermIds.value,
  preserveSelectedRowKeys: true,
  onChange: selectedRowKeys => {
    checkedUserPermIds.value = selectedRowKeys
  }
}))

const filterRoleOption = (input, option) => {
  return String(option?.searchText || '').includes(String(input || '').toLowerCase())
}

const fetchDepts = async () => {
  const res = await request.get('/system/dept/list')
  deptData.value = res.data
}

const fetchRoles = async () => {
  const res = await request.get('/system/role/list')
  allRoles.value = res.data
}

const fetchMenus = async () => {
  const res = await request.get('/system/menu/list')
  menuData.value = res.data
}

const fetchPermissionPage = async () => {
  permissionLoading.value = true
  try {
    const res = await request.get('/system/permission/page', { params: permQuery })
    permissionRows.value = res.data.records
    permissionPagination.current = res.data.current
    permissionPagination.pageSize = res.data.size
    permissionPagination.total = res.data.total
  } finally {
    permissionLoading.value = false
  }
}

const fetchUserPermIds = async (userId) => {
  const res = await request.get(`/system/user/perms/${userId}`)
  checkedUserPermIds.value = res.data || []
}

const resetPermissionQuery = () => {
  permQuery.keyword = ''
  permQuery.menuId = undefined
  permQuery.pageNum = 1
  permQuery.pageSize = 10
  permissionPagination.current = 1
  permissionPagination.pageSize = 10
  fetchPermissionPage()
}

const handlePermissionTableChange = (pagination) => {
  permQuery.pageNum = pagination.current
  permQuery.pageSize = pagination.pageSize
  permissionPagination.current = pagination.current
  permissionPagination.pageSize = pagination.pageSize
  fetchPermissionPage()
}

onMounted(async () => {
  await fetchDepts()
  await fetchRoles()
  await fetchMenus()
  
  const id = route.params.id
  if (id) {
    isEdit.value = true
    try {
      const [detailRes, roleRes] = await Promise.all([
        request.get(`/system/user/detail/${id}`),
        request.get(`/system/user/roles/${id}`)
      ])
      Object.assign(form, detailRes.data)
      assignedRoles.value = roleRes.data
      await Promise.all([fetchUserPermIds(id), fetchPermissionPage()])
    } catch (e) {
      message.error('获取用户数据失败')
    }
  }
})

const uploadHeaders = computed(() => {
  return {
    Authorization: localStorage.getItem('token') || ''
  }
})

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
    form.avatarFileId = extractFileId(info.file.response.data)
    message.success('头像上传成功')
  } else if (info.file.status === 'error') {
    message.error('头像上传失败')
  }
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    if (!isEdit.value && (!form.password || form.password.length < 6)) {
      message.error('初始密码长度不能少于6位')
      return
    }
    submitting.value = true
    const url = isEdit.value ? '/system/user/edit' : '/system/user/add'
    
    // 1. 保存/更新基础信息
    const res = await request.post(url, form)
    const userId = isEdit.value ? form.id : res.data
    
    // 2. 分配角色
    if (assignedRoles.value.length > 0 || isEdit.value) {
      await request.post('/system/user/assignRoles', {
        userId: userId,
        roleIds: assignedRoles.value
      })
    }
    
    message.success(isEdit.value ? '资料已更新' : '用户创建成功')
    router.push('/system/user')
  } catch (error) {
    console.log('Validation failed:', error)
  } finally {
    submitting.value = false
  }
}

const saveUserPermissions = async () => {
  await request.post('/system/user/assignPerms', {
    userId: form.id,
    permIds: checkedUserPermIds.value
  })
  if (Number(form.id) === currentLoginUserId) {
    const info = await request.get('/auth/info')
    localStorage.setItem('permissions', JSON.stringify(info.data.permissions || []))
    refreshPermissionCache()
  }
  message.success('直授权限已保存')
}

const submitResetPassword = async () => {
  if (!resetPwdForm.newPassword || resetPwdForm.newPassword.length < 6) {
    message.error('新密码长度不能少于6位')
    return
  }
  if (resetPwdForm.newPassword !== resetPwdForm.confirmPassword) {
    message.error('两次输入密码不一致')
    return
  }
  resetPwdLoading.value = true
  try {
    await request.post('/system/user/resetPwd', {
      userId: String(form.id),
      newPassword: resetPwdForm.newPassword
    })
    resetPwdVisible.value = false
    resetPwdForm.newPassword = ''
    resetPwdForm.confirmPassword = ''
    message.success('密码已重置，用户需要重新登录')
    if (Number(form.id) === currentLoginUserId) {
      localStorage.clear()
      router.push('/login')
    }
  } finally {
    resetPwdLoading.value = false
  }
}

const handleKickout = async () => {
  await request.post(`/system/user/kickout/${form.id}`)
  message.success('已强制用户下线')
  if (Number(form.id) === currentLoginUserId) {
    localStorage.clear()
    router.push('/login')
  }
}

watch(
  () => form.avatarFileId,
  (nextId, prevId) => {
    syncAvatarPreview(nextId, prevId)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (form.avatarFileId) {
    revokeAuthedPreviewUrl(form.avatarFileId)
  }
})
</script>

<style scoped>
.user-profile-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

/* 身份名片样式 */
.identity-card {
  border-radius: var(--border-radius, 12px);
  text-align: center;
  background: var(--bg-card);
}
.avatar-section { padding: 20px 0; }

.avatar-uploader {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
:deep(.ant-upload.ant-upload-select-picture-card) {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  background-color: var(--colorPrimary);
  border: 4px solid var(--bg-card);
  box-shadow: 0 10px 20px rgba(0,0,0,0.1);
  margin: 0;
}
.user-avatar-placeholder {
  font-size: 40px;
  line-height: 112px;
  color: white;
  font-weight: 800;
}
.user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name { font-size: 24px; font-weight: 800; color: var(--text-main); }
.user-role { color: var(--text-sub); margin-top: 4px; font-size: 14px; }

.status-badge {
  display: inline-block;
  margin-top: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.status-active { background: var(--color-primary-soft-surface); color: var(--color-primary); }
.status-inactive { background: color-mix(in srgb, var(--color-danger) 12%, transparent); color: var(--color-danger); }

.info-list { text-align: left; padding: 0 10px; }
.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 14px;
}
.info-item .label { color: var(--text-sub); display: flex; align-items: center; gap: 8px; }
.info-item .value { color: var(--text-main); font-weight: 500; }

/* 详情卡片样式 */
.detail-card {
  border-radius: var(--border-radius, 12px);
  min-height: 600px;
  background: var(--bg-card);
}

.profile-tabs :deep(.ant-tabs-nav) { margin-bottom: 24px; }
.profile-tabs :deep(.ant-tabs-tab) { font-size: 16px; padding: 16px 0; margin-right: 32px; }

.tab-content { max-width: 800px; }
.perm-card { padding: 0; }

/* 安全设置 */
.security-panel { max-width: 600px; padding: 20px 0; }
.sec-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sec-info h4 { margin: 0 0 4px; font-size: 16px; color: var(--text-main); }
.sec-info p { margin: 0; font-size: 13px; color: var(--text-sub); }

/* 动画序列 */
.slide-in-1 { animation: slide-up 0.5s both 0.1s; }
.slide-in-2 { animation: slide-up 0.5s both 0.2s; }
.slide-in-3 { animation: slide-up 0.5s both 0.3s; }
@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 覆盖输入框样式 */
:deep(.ant-input), :deep(.ant-select-selector), :deep(.ant-input-password) {
  border-radius: 8px !important;
}
</style>
