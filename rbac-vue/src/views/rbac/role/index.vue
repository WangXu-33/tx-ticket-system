<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <TreeSearchToolbar
        v-model="roleKeyword"
        placeholder="请输入角色名称或权限标识"
        @search="handleRoleSearch"
        @reset="resetRoleSearch"
      />
    </a-card>

    <a-card class="table-card slide-in-2" :bordered="false">
      <template #title>
        <span class="card-title">角色列表</span>
      </template>
      <template #extra>
        <a-button v-hasPerm="['sys:role:add']" type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>新增角色
        </a-button>
      </template>

      <a-table :dataSource="filteredRoleList" :columns="columns" rowKey="id" :loading="loading" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dataScope'">
            <a-tag :color="record.dataScope === 1 ? 'red' : 'blue'">
              {{ getDataScopeName(record.dataScope) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-hasPerm="['sys:role:edit']" class="edit-link" @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a v-hasPerm="['sys:role:edit']" class="auth-link" @click="handleAuth(record)">配置权限</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除吗？" @confirm="handleDelete(record.id)">
                <a v-hasPerm="['sys:role:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 角色表单弹窗 -->
    <a-modal
      v-model:open="roleVisible"
      :title="form.id ? '编辑角色' : '新增角色'"
      @ok="submitRole"
      :confirmLoading="submitting"
      destroyOnClose
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="角色名称" :rules="[{ required: true }]">
          <a-input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="权限标识" :rules="[{ required: true }]">
          <a-input v-model:value="form.roleKey" placeholder="如: admin" :disabled="!!form.id" />
        </a-form-item>
        <a-form-item label="数据范围" :rules="[{ required: true }]">
          <a-select v-model:value="form.dataScope" placeholder="请选择数据范围">
            <a-select-option :value="1">全部数据权限</a-select-option>
            <a-select-option :value="2">自定义数据权限</a-select-option>
            <a-select-option :value="3">本部门数据权限</a-select-option>
            <a-select-option :value="4">本部门及以下数据权限</a-select-option>
            <a-select-option :value="5">仅本人数据权限</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.dataScope === 2" label="自定义可见部门">
          <a-tree-select
            v-model:value="checkedDeptIds"
            :tree-data="deptTree"
            :fieldNames="{ children: 'children', label: 'deptName', value: 'id' }"
            tree-checkable
            tree-default-expand-all
            show-search
            tree-node-filter-prop="deptName"
            style="width: 100%"
            placeholder="请选择角色可见的部门范围"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 角色权限弹窗 -->
    <a-modal v-model:open="authVisible" title="配置角色权限" @ok="submitAuth" :width="980" destroyOnClose>
      <a-alert
        v-if="currentRole"
        class="auth-tip"
        type="info"
        show-icon
        :message="`${currentRole.roleName} 将拥有下方勾选的菜单和按钮权限。给用户分配角色请到「用户管理」编辑用户资料。`"
      />
      <a-tabs v-model:activeKey="authTab">
        <a-tab-pane key="menus" tab="菜单权限">
          <div class="auth-tree-container">
            <TreeSearchToolbar
              v-model="menuKeyword"
              placeholder="请输入菜单名称或路由路径"
              input-width="280px"
              @search="handleMenuSearch"
              @reset="resetMenuSearch"
            />
            <a-tree
              v-if="filteredMenuTree.length"
              v-model:checkedKeys="checkedMenuIds"
              checkable
              :treeData="filteredMenuTree"
              :fieldNames="{ title: 'title', key: 'id' }"
              defaultExpandAll
            />
            <a-empty v-else description="暂无菜单数据" />
          </div>
        </a-tab-pane>
        <a-tab-pane key="perms" tab="按钮权限">
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
                  <a-select-option v-for="item in menuData" :key="item.id" :value="item.id">
                    {{ item.title }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" @click="fetchPermissionPage">查询</a-button>
                  <a-button @click="resetPermissionQuery">重置</a-button>
                </a-space>
              </a-form-item>
            </a-form>

            <a-table
              rowKey="id"
              :dataSource="permissionRows"
              :columns="permissionColumns"
              :loading="permissionLoading"
              :pagination="permissionPagination"
              :rowSelection="rowSelection"
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
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import TreeSearchToolbar from '@/components/tree-search-toolbar/index.vue'
import { filterTreeWithLinkage } from '@/utils/tree'

const loading = ref(false)
const submitting = ref(false)
const roleList = ref([])
const menuData = ref([])
const menuTreeData = ref([])
const deptTree = ref([])
const roleKeyword = ref('')
const activeRoleKeyword = ref('')
const menuKeyword = ref('')
const activeMenuKeyword = ref('')

const roleVisible = ref(false)
const form = reactive({ id: null, roleName: '', roleKey: '', dataScope: 1, status: 1 })
const checkedDeptIds = ref([])

const authVisible = ref(false)
const currentRole = ref(null)
const checkedMenuIds = ref([])
const checkedPermIds = ref([])
const authTab = ref('menus')
const permissionLoading = ref(false)
const permissionRows = ref([])
const permQuery = reactive({ keyword: '', menuId: undefined, pageNum: 1, pageSize: 10 })
const permissionPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const filteredRoleList = computed(() => {
  const keyword = activeRoleKeyword.value
  if (!keyword) {
    return roleList.value
  }
  const normalizedKeyword = keyword.toLowerCase()
  return roleList.value.filter(item =>
    String(item.roleName || '').toLowerCase().includes(normalizedKeyword) ||
    String(item.roleKey || '').toLowerCase().includes(normalizedKeyword)
  )
})

const columns = [
  { title: '角色ID', dataIndex: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'roleName' },
  { title: '权限标识', dataIndex: 'roleKey' },
  { title: '数据范围', key: 'dataScope' },
  { title: '操作', key: 'action', width: 200 }
]

const permissionColumns = [
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '权限标识', dataIndex: 'permKey', key: 'permKey', width: 220 },
  { title: '挂载菜单', dataIndex: 'menuTitles', key: 'menuTitles' }
]

const rowSelection = computed(() => ({
  selectedRowKeys: checkedPermIds.value,
  preserveSelectedRowKeys: true,
  onChange: selectedRowKeys => {
    checkedPermIds.value = selectedRowKeys
  }
}))

const filteredMenuTree = computed(() => {
  return filterTreeWithLinkage(menuTreeData.value, activeMenuKeyword.value, ['title', 'path'])
})

const getDataScopeName = (scope) => {
  const map = {
    1: '全部数据权限',
    2: '自定义数据权限',
    3: '本部门数据权限',
    4: '本部门及以下',
    5: '仅本人数据权限'
  }
  return map[scope] || '未知'
}

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/role/list')
    roleList.value = res.data
  } finally {
    loading.value = false
  }
}

const fetchMenus = async () => {
  const [listRes, treeRes] = await Promise.all([
    request.get('/system/menu/list'),
    request.get('/system/menu/tree')
  ])
  menuData.value = listRes.data
  menuTreeData.value = treeRes.data
}

const fetchDepts = async () => {
  const res = await request.get('/system/dept/tree')
  deptTree.value = res.data
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

const handleRoleSearch = () => {
  activeRoleKeyword.value = roleKeyword.value.trim()
}

const resetRoleSearch = () => {
  roleKeyword.value = ''
  activeRoleKeyword.value = ''
}

const handleMenuSearch = () => {
  activeMenuKeyword.value = menuKeyword.value.trim()
}

const resetMenuSearch = () => {
  menuKeyword.value = ''
  activeMenuKeyword.value = ''
}

const handleAdd = () => {
  Object.assign(form, { id: null, roleName: '', roleKey: '', dataScope: 1, status: 1 })
  checkedDeptIds.value = []
  roleVisible.value = true
}

const handleEdit = async (record) => {
  Object.assign(form, record)
  if (record.dataScope === 2) {
    const deptRes = await request.get(`/system/role/deptIds/${record.id}`)
    checkedDeptIds.value = deptRes.data || []
  } else {
    checkedDeptIds.value = []
  }
  roleVisible.value = true
}

const handleDelete = async (id) => {
  await request.delete(`/system/role/delete/${id}`)
  message.success('删除成功')
  fetchRoles()
}

const submitRole = async () => {
  submitting.value = true
  try {
    if (form.dataScope === 2 && checkedDeptIds.value.length === 0) {
      message.error('自定义数据权限至少选择一个部门')
      return
    }
    const url = form.id ? '/system/role/edit' : '/system/role/add'
    const res = await request.post(url, form)
    const roleId = form.id || res.data
    await request.post('/system/role/assignDepts', {
      roleId,
      deptIds: form.dataScope === 2 ? checkedDeptIds.value : []
    })
    message.success('操作成功')
    roleVisible.value = false
    fetchRoles()
  } finally {
    submitting.value = false
  }
}

const handleAuth = async (record) => {
  currentRole.value = record
  const [menuRes, permRes] = await Promise.all([
    request.get(`/system/role/menuIds/${record.id}`),
    request.get(`/system/permission/role/${record.id}/ids`)
  ])
  checkedMenuIds.value = menuRes.data
  checkedPermIds.value = permRes.data
  authTab.value = 'menus'
  resetMenuSearch()
  resetPermissionQuery()
  authVisible.value = true
}

const submitAuth = async () => {
  await Promise.all([
    request.post('/system/role/assignMenus', {
      roleId: currentRole.value.id,
      menuIds: checkedMenuIds.value
    }),
    request.post('/system/permission/role/assign', {
      roleId: currentRole.value.id,
      permIds: checkedPermIds.value
    })
  ])
  message.success('角色权限已保存')
  authVisible.value = false
}

onMounted(() => {
  fetchRoles()
  fetchMenus()
  fetchDepts()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; padding: 0; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }

@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.edit-link { color: var(--color-primary); font-weight: 600; }
.auth-link { color: var(--color-link); font-weight: 600; }
.delete-link { color: var(--color-danger); }

.auth-tree-container {
  max-height: 450px;
  overflow-y: auto;
  padding: 10px;
  background: #f8fafc;
  border-radius: 12px;
}
.auth-tree-container :deep(.tree-search-toolbar) {
  margin-bottom: 12px;
}
.auth-tip {
  margin-bottom: 16px;
}
.perm-card { padding: 0; }
.dark-mode .auth-tree-container { background: #020617; }
</style>
