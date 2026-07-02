<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <TreeSearchToolbar
        v-model="searchKeyword"
        placeholder="请输入部门名称或物化路径"
        @search="handleSearch"
        @reset="resetSearch"
      />
    </a-card>

    <a-card class="table-card slide-in-2" :bordered="false">
      <template #title>
        <span class="card-title">组织架构管理</span>
      </template>
      <template #extra>
        <a-button v-hasPerm="['sys:dept:add']" type="primary" @click="handleAdd(0)">
          <template #icon><PlusOutlined /></template>新增根部门
        </a-button>
      </template>

      <a-table 
        :dataSource="filteredDeptTree"
        :columns="columns" 
        rowKey="id" 
        :loading="loading"
        :pagination="false"
        defaultExpandAllRows
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deptCode'">
            <a-tag color="blue">{{ record.deptCode }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '停用'" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-hasPerm="['sys:dept:edit']" class="edit-link" @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a v-hasPerm="['sys:dept:add']" class="table-action-link" @click="handleAdd(record.id)">添加下级</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除该部门吗？" @confirm="handleDelete(record.id)">
                <a v-hasPerm="['sys:dept:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑抽屉 (符合复杂逻辑规约) -->
    <a-drawer
      :title="form.id ? '编辑部门' : '新增部门'"
      :open="drawerVisible"
      width="480"
      @close="drawerVisible = false"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="上级部门ID">
          <a-input v-model:value="form.parentId" disabled />
        </a-form-item>
        <a-form-item label="部门名称" :rules="[{ required: true }]">
          <a-input v-model:value="form.deptName" placeholder="请输入部门名称" />
        </a-form-item>
        <a-form-item label="显示排序">
          <a-input-number v-model:value="form.orderNum" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button v-hasPerm="form.id ? ['sys:dept:edit'] : ['sys:dept:add']" type="primary" :loading="submitting" @click="submitForm">提交</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import TreeSearchToolbar from '@/components/tree-search-toolbar/index.vue'
import { filterTreeWithLinkage } from '@/utils/tree'

const loading = ref(false)
const submitting = ref(false)
const deptTree = ref([])
const drawerVisible = ref(false)
const form = ref({ id: null, parentId: 0, deptName: '', orderNum: 0, status: 1 })
const searchKeyword = ref('')
const activeKeyword = ref('')

const columns = [
  { title: '部门名称', dataIndex: 'deptName', key: 'deptName' },
  { title: '物化路径', dataIndex: 'deptCode', key: 'deptCode' },
  { title: '排序', dataIndex: 'orderNum', key: 'orderNum', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 250 }
]

const filteredDeptTree = computed(() => {
  return filterTreeWithLinkage(deptTree.value, activeKeyword.value, ['deptName', 'deptCode'])
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/dept/tree')
    deptTree.value = res.data
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  activeKeyword.value = searchKeyword.value.trim()
}

const resetSearch = () => {
  searchKeyword.value = ''
  activeKeyword.value = ''
}

const handleAdd = (parentId) => {
  form.value = { id: null, parentId, deptName: '', orderNum: 0, status: 1 }
  drawerVisible.value = true
}

const handleEdit = (record) => {
  form.value = { ...record }
  drawerVisible.value = true
}

const submitForm = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    const url = form.value.id ? '/system/dept/edit' : '/system/dept/add'
    await request.post(url, form.value)
    message.success('操作成功')
    drawerVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  await request.delete(`/system/dept/delete/${id}`)
  message.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.edit-link { color: var(--color-primary); font-weight: 600; }
.delete-link { color: var(--color-danger); }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }

@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
