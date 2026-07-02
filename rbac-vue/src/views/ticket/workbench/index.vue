<template>
  <div class="page-container">
    <a-card :bordered="false" class="hero-card">
      <div>
        <p class="eyebrow">Ticket Workbench</p>
        <h2>工单工作台</h2>
        <p>集中处理待受理、处理中、转派和待客户补充的工单。</p>
      </div>
      <a-button type="primary" @click="fetchData">刷新</a-button>
    </a-card>

    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="query">
        <a-form-item label="关键词">
          <a-input v-model:value="query.keyword" allow-clear placeholder="编号/标题/描述" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear style="width: 150px">
            <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="query.priority" allow-clear style="width: 140px">
            <a-select-option value="low">低</a-select-option>
            <a-select-option value="normal">普通</a-select-option>
            <a-select-option value="high">高</a-select-option>
            <a-select-option value="urgent">紧急</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="fetchData">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ticketNo'">
            <a @click="goDetail(record.id)">{{ record.ticketNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'priority'">
            <a-tag :color="priorityColor(record.priority)">{{ priorityText(record.priority) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="goDetail(record.id)">详情</a>
              <a v-if="record.status === 'pending'" @click="receive(record.id)">接单</a>
              <a @click="openAssign(record)">分派</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="assignVisible" title="分派/转派工单" @ok="submitAssign">
      <a-form layout="vertical" :model="assignForm">
        <a-form-item label="处理人用户 ID" required>
          <a-input-number v-model:value="assignForm.handlerId" style="width: 100%" />
        </a-form-item>
        <a-form-item label="原因">
          <a-textarea v-model:value="assignForm.reason" :rows="4" placeholder="说明分派或转派原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const dataSource = ref([])
const assignVisible = ref(false)
const currentTicket = ref(null)
const query = reactive({ keyword: '', status: undefined, priority: undefined, pageNum: 1, pageSize: 10 })
const assignForm = reactive({ handlerId: null, reason: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const statusOptions = [
  { value: 'pending', label: '待受理' },
  { value: 'processing', label: '处理中' },
  { value: 'waiting_customer', label: '待客户补充' },
  { value: 'transferred', label: '已转派' },
  { value: 'resolved', label: '已解决' },
  { value: 'closed', label: '已关闭' },
  { value: 'rejected', label: '已驳回' }
]

const columns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 180 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '客户', dataIndex: 'creatorName', key: 'creatorName', width: 140 },
  { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 140 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/ticket/list', { params: query })
    dataSource.value = res.data.records || []
    pagination.total = res.data.total || 0
    pagination.current = res.data.current || query.pageNum
    pagination.pageSize = res.data.size || query.pageSize
  } finally {
    loading.value = false
  }
}

const handleTableChange = (page) => {
  query.pageNum = page.current
  query.pageSize = page.pageSize
  fetchData()
}

const resetQuery = () => {
  query.keyword = ''
  query.status = undefined
  query.priority = undefined
  query.pageNum = 1
  fetchData()
}

const receive = async (id) => {
  await request.post(`/ticket/receive/${id}`)
  message.success('接单成功')
  fetchData()
}

const openAssign = (record) => {
  currentTicket.value = record
  assignForm.handlerId = record.handlerId || null
  assignForm.reason = ''
  assignVisible.value = true
}

const submitAssign = async () => {
  await request.post('/ticket/assign', { ticketId: currentTicket.value.id, handlerId: assignForm.handlerId, reason: assignForm.reason })
  message.success('分派成功')
  assignVisible.value = false
  fetchData()
}

const goDetail = (id) => router.push(`/ticket/detail/${id}`)
const statusText = (value) => statusOptions.find(item => item.value === value)?.label || value
const statusColor = (value) => ({ pending: 'orange', processing: 'blue', waiting_customer: 'purple', transferred: 'cyan', resolved: 'green', closed: 'default', rejected: 'red' }[value] || 'default')
const priorityText = (value) => ({ low: '低', normal: '普通', high: '高', urgent: '紧急' }[value] || value)
const priorityColor = (value) => ({ low: 'default', normal: 'blue', high: 'orange', urgent: 'red' }[value] || 'default')

onMounted(fetchData)
</script>

<style scoped>
.hero-card {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #0f766e, #0f172a);
  color: #fff;
}

.hero-card :deep(.ant-card-body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0 0 6px;
  color: #99f6e4;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 800;
}

h2 {
  color: #fff;
  margin: 0 0 8px;
  font-size: 30px;
}

.search-card {
  margin-bottom: 16px;
}
</style>
