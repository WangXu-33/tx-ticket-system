<template>
  <div class="page-container">
    <!-- 顶部搜索栏 (阶梯进入 1) -->
    <a-card class="search-card slide-in-1" :bordered="false">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="用户名">
          <a-input v-model:value="queryParams.username" placeholder="请输入用户名" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="fetchData">
              <template #icon><search-outlined /></template>查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 数据表格栏 (阶梯进入 2) -->
    <a-card class="table-card slide-in-2" :bordered="false">
      <template #extra>
        <a-button v-hasPerm="['sys:user:add']" type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>新增用户
        </a-button>
      </template>

      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        rowKey="id" 
        :loading="loading"
        :pagination="{ pageSize: 10, showTotal: total => `共 ${total} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '禁用'" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-hasPerm="['sys:user:edit']" class="edit-link" @click="handleEdit(record)">编辑/分配角色</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(record.id)">
                <a v-hasPerm="['sys:user:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()
const loading = ref(false)
const dataSource = ref([])
const queryParams = reactive({ username: '' })

const columns = [
  { title: 'UID', dataIndex: 'id', key: 'id', width: 100 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', customRender: ({ text }) => formatDateTime(text) },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/user/list', { params: queryParams })
    dataSource.value = res.data
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.username = ''
  fetchData()
}

const handleAdd = () => {
  router.push('/system/user/detail')
}

const handleEdit = (record) => {
  router.push(`/system/user/detail/${record.id}`)
}

const handleDelete = async (id) => {
  await request.delete(`/system/user/delete/${id}`)
  message.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.02);
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.03);
}

/* 动效 */
.slide-in-1 { animation: slide-up 0.6s both 0.1s; }
.slide-in-2 { animation: slide-up 0.6s both 0.2s; }

@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.edit-link { color: var(--color-primary); font-weight: 600; }
.dark-mode .edit-link { color: #818cf8; }
.delete-link { color: var(--color-danger); }

:deep(.ant-table) { background: transparent !important; }
:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
</style>
