<template>
  <div class="page-container my-ticket-page">
    <ticket-filter-card v-model:collapsed="queryCollapsed">
      <a-form layout="inline" :model="query" class="compact-filter-form">
        <a-form-item label="工单编号">
          <a-input v-model:value="query.ticketNo" allow-clear placeholder="工单编号" style="width: 180px" />
        </a-form-item>
        <a-form-item label="工单标题">
          <a-input v-model:value="query.title" allow-clear placeholder="工单标题" style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="状态" style="width: 160px">
            <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="query.priority" allow-clear placeholder="优先级" style="width: 140px">
            <a-select-option v-for="item in priorityOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="query-actions">
          <a-space>
            <a-button type="primary" @click="fetchData">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </ticket-filter-card>

    <a-card :bordered="false" class="table-card">
      <template #title>
        <a-tabs v-model:activeKey="activeTab" class="view-tabs" @change="handleTabChange">
          <a-tab-pane key="all" tab="全部工单" />
          <a-tab-pane key="draft" tab="草稿箱" />
          <a-tab-pane key="submitted" tab="已提交" />
          <a-tab-pane key="closed" tab="已关闭" />
        </a-tabs>
      </template>
      <template #extra>
        <a-button v-if="canCreateTicket" type="primary" @click="router.push('/ticket/create')">
          <template #icon><plus-outlined /></template>
          提交工单
        </a-button>
      </template>
      <a-table
        row-key="id"
        size="small"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1120 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ticketNo'">
            <a class="link-text" @click="openRecord(record)">
              {{ record.ticketNo || `#${record.id}` }}
            </a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="priorityColor(record.priority)">{{ priorityText(record.priority) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a
                v-if="record.status === 'draft'"
                class="table-action-link table-action-link--primary"
                @click="continueDraft(record)"
              >
                继续编辑
              </a>
              <a
                v-else
                class="table-action-link table-action-link--primary"
                @click="goDetail(record.id)"
              >
                查看详情
              </a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { ticketApi } from '@/api/ticket'
import { formatDateTime } from '@/utils/datetime'
import { hasAnyPermission } from '@/utils/permission'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'
import TicketFilterCard from '../components/TicketFilterCard.vue'

const router = useRouter()
const loading = ref(false)
const queryCollapsed = ref(false)
const activeTab = ref('all')
const dataSource = ref([])
const statusOptions = ref([])
const priorityOptions = ref([])
const canCreateTicket = ref(false)

const query = reactive({
  owner: 'customer',
  ticketNo: '',
  title: '',
  status: undefined,
  priority: undefined,
  statusList: '',
  pageNum: 1,
  pageSize: 10
})

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 180, ellipsis: true },
  { title: '工单标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 110 },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    customRender: ({ text }) => formatDateTime(text)
  },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  statusOptions.value = meta.statusOptions || []
  priorityOptions.value = meta.priorityOptions || []
}

const applyTabQuery = () => {
  if (activeTab.value === 'draft') {
    query.statusList = 'draft'
  } else if (activeTab.value === 'submitted') {
    query.statusList = 'pending_approval,pending,processing,waiting_customer,transferred,resolved'
  } else if (activeTab.value === 'closed') {
    query.statusList = 'closed,rejected,cancelled'
  } else {
    query.statusList = ''
  }
}

const fetchData = async () => {
  applyTabQuery()
  loading.value = true
  try {
    const res = await ticketApi.list(query)
    dataSource.value = res.data.records || []
    pagination.total = res.data.total || 0
    pagination.current = res.data.current || query.pageNum
    pagination.pageSize = res.data.size || query.pageSize
  } finally {
    loading.value = false
  }
}

const handleTableChange = page => {
  query.pageNum = page.current
  query.pageSize = page.pageSize
  fetchData()
}

const handleTabChange = () => {
  query.pageNum = 1
  fetchData()
}

const resetQuery = () => {
  query.ticketNo = ''
  query.title = ''
  query.status = undefined
  query.priority = undefined
  query.pageNum = 1
  fetchData()
}

const goDetail = id => router.push(`/ticket/detail/${id}`)
const continueDraft = record => router.push(`/ticket/create?id=${record.id}`)
const openRecord = record => (record.status === 'draft' ? continueDraft(record) : goDetail(record.id))

const statusText = value => getOptionLabel(statusOptions.value, value, value || '-')
const statusColor = value => getOptionColor(statusOptions.value, value, 'default')
const priorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const priorityColor = value => getOptionColor(priorityOptions.value, value, 'default')

onMounted(async () => {
  canCreateTicket.value = hasAnyPermission(['ticket:add', 'ticket:my:add'])
  await fetchMeta()
  fetchData()
})
</script>

<style scoped>
.my-ticket-page {
  display: flex;
  flex-direction: column;
}

.table-card {
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.view-tabs {
  margin-bottom: -12px;
}

.query-actions {
  align-self: flex-end;
}

.link-text {
  font-weight: 600;
}

.table-action-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.18s ease;
}

.table-action-link:hover {
  border-color: #dbe3ee;
  background: #f1f5f9;
  color: #0f172a;
}

.table-action-link--primary {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.table-action-link--primary:hover {
  border-color: #93c5fd;
  background: #dbeafe;
  color: #1e3a8a;
}

@media (max-width: 1200px) {
  .query-actions {
    width: 100%;
  }
}
</style>
