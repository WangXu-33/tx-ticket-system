<template>
  <div class="page-container ticket-pending-page">
    <ticket-filter-card v-model:collapsed="queryCollapsed">
      <a-form layout="inline" :model="query" class="compact-filter-form">
        <a-form-item label="工单编号">
          <a-input v-model:value="query.ticketNo" allow-clear placeholder="工单编号" style="width: 180px" />
        </a-form-item>
        <a-form-item label="工单标题">
          <a-input v-model:value="query.title" allow-clear placeholder="工单标题" style="width: 220px" />
        </a-form-item>
        <a-form-item label="客户">
          <a-input v-model:value="query.creatorName" allow-clear placeholder="客户名称" style="width: 180px" />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="query.priority" allow-clear placeholder="优先级" style="width: 140px">
            <a-select-option v-for="item in priorityOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="问题类型">
          <a-select v-model:value="query.category" allow-clear placeholder="问题类型" style="width: 160px">
            <a-select-option v-for="item in categoryOptions" :key="item.value" :value="item.value">
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
      <a-table
        row-key="id"
        size="small"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1240 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ticketNo'">
            <a class="link-text" @click="goDetail(record.id)">{{ record.ticketNo }}</a>
          </template>
          <template v-else-if="column.key === 'category'">
            <a-tag>{{ categoryText(record.category) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="priorityColor(record.priority)">{{ priorityText(record.priority) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" wrap>
              <a class="table-action-link" @click="goDetail(record.id)">详情</a>
              <a
                v-if="canApprove(record)"
                class="table-action-link table-action-link--primary"
                @click="approve(record)"
              >
                通过
              </a>
              <a
                v-if="canReturn(record)"
                class="table-action-link table-action-link--warning"
                @click="openReviewAction(record, 'return')"
              >
                退回
              </a>
              <a
                v-if="canReject(record)"
                class="table-action-link table-action-link--danger"
                @click="openReviewAction(record, 'reject')"
              >
                驳回
              </a>
              <a
                v-if="canReceive(record)"
                class="table-action-link table-action-link--success"
                @click="receive(record.id)"
              >
                接单
              </a>
              <a
                v-if="canAssign(record)"
                class="table-action-link"
                @click="openAssign(record)"
              >
                分派
              </a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="assignVisible"
      title="分派工单"
      :confirm-loading="assignLoading"
      @ok="submitAssign"
    >
      <a-form layout="vertical" :model="assignForm">
        <a-form-item label="处理人" required>
          <a-select
            v-model:value="assignForm.handlerId"
            show-search
            allow-clear
            placeholder="请选择处理人"
            :filter-option="filterHandlerOption"
            @search="fetchHandlerOptions"
          >
            <a-select-option v-for="item in handlerOptions" :key="item.id" :value="item.id">
              {{ item.nickname || item.username }}（{{ item.roleName || item.roleKey || '未配置角色' }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="说明">
          <a-textarea v-model:value="assignForm.reason" :rows="4" placeholder="写清分派原因或处理要求" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="reviewActionVisible"
      :title="reviewActionTitle"
      :confirm-loading="reviewActionLoading"
      @ok="submitReviewAction"
    >
      <a-form layout="vertical" :model="reviewActionForm">
        <a-form-item label="处理说明" required>
          <a-textarea
            v-model:value="reviewActionForm.content"
            :rows="4"
            :placeholder="reviewActionMode === 'return' ? '写清需要客户补充的材料或信息' : '写清驳回原因，客户侧可见'"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ticketApi } from '@/api/ticket'
import { formatDateTime } from '@/utils/datetime'
import { hasAnyRole } from '@/utils/auth'
import { hasAnyPermission } from '@/utils/permission'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'
import TicketFilterCard from '../components/TicketFilterCard.vue'

const router = useRouter()
const loading = ref(false)
const assignLoading = ref(false)
const reviewActionLoading = ref(false)
const queryCollapsed = ref(false)
const dataSource = ref([])
const handlerOptions = ref([])
const statusOptions = ref([])
const priorityOptions = ref([])
const categoryOptions = ref([])
const assignVisible = ref(false)
const reviewActionVisible = ref(false)
const reviewActionMode = ref('return')
const currentTicket = ref(null)

const managerMode = computed(() => hasAnyRole(['admin', 'system_admin', 'supervisor']))

const query = reactive({
  ticketNo: '',
  title: '',
  creatorName: '',
  statusList: 'pending',
  priority: undefined,
  category: undefined,
  pageNum: 1,
  pageSize: 10
})

const assignForm = reactive({
  handlerId: null,
  reason: ''
})

const reviewActionForm = reactive({
  content: ''
})

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 180, ellipsis: true },
  { title: '工单标题', dataIndex: 'title', key: 'title', width: 320, ellipsis: true },
  { title: '问题类型', dataIndex: 'category', key: 'category', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 110 },
  { title: '客户', dataIndex: 'creatorName', key: 'creatorName', width: 150, ellipsis: true },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    customRender: ({ text }) => formatDateTime(text)
  },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
]

/**
 * 2026-07-03 独立待接工单大厅，避免在工作台内误触接单动作。
 */
const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  statusOptions.value = meta.statusOptions || []
  priorityOptions.value = meta.priorityOptions || []
  categoryOptions.value = meta.categoryOptions || []
}

const fetchData = async () => {
  query.statusList = 'pending_approval,pending'
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

const fetchHandlerOptions = async (keyword = '') => {
  const res = await ticketApi.handlerOptions({ keyword })
  handlerOptions.value = res.data || []
}

const filterHandlerOption = (input, option) => {
  const record = handlerOptions.value.find(item => item.id === option.value)
  const label = `${record?.username || ''}${record?.nickname || ''}${record?.roleName || ''}${record?.roleKey || ''}`
  return label.toLowerCase().includes(input.toLowerCase())
}

const handleTableChange = page => {
  query.pageNum = page.current
  query.pageSize = page.pageSize
  fetchData()
}

const resetQuery = () => {
  query.ticketNo = ''
  query.title = ''
  query.creatorName = ''
  query.priority = undefined
  query.category = undefined
  query.pageNum = 1
  fetchData()
}

const reviewActionTitle = computed(() => (reviewActionMode.value === 'return' ? '退回客户补充' : '驳回工单'))
const canApprove = record => hasAnyPermission(['ticket:assign']) && managerMode.value && record.status === 'pending_approval'
const canReturn = record => hasAnyPermission(['ticket:assign']) && managerMode.value && ['pending_approval', 'pending'].includes(record.status)
const canReject = record => hasAnyPermission(['ticket:reject']) && managerMode.value && ['pending_approval', 'pending'].includes(record.status)
const canReceive = record => hasAnyPermission(['ticket:receive']) && record.status === 'pending'
const canAssign = record => hasAnyPermission(['ticket:assign']) && managerMode.value && record.status === 'pending'

const approve = async record => {
  await ticketApi.approve({ ticketId: record.id, content: '审批通过，进入待受理大厅' })
  message.success('审批已通过')
  fetchData()
}

const receive = async id => {
  await ticketApi.receive(id)
  message.success('接单成功')
  fetchData()
}

const openAssign = record => {
  currentTicket.value = record
  assignForm.handlerId = record.handlerId || null
  assignForm.reason = ''
  assignVisible.value = true
}

const openReviewAction = (record, mode) => {
  currentTicket.value = record
  reviewActionMode.value = mode
  reviewActionForm.content = ''
  reviewActionVisible.value = true
}

const submitReviewAction = async () => {
  if (!reviewActionForm.content.trim()) {
    message.warning(reviewActionMode.value === 'return' ? '请填写退回补充原因' : '请填写驳回原因')
    return
  }
  reviewActionLoading.value = true
  try {
    const payload = { ticketId: currentTicket.value.id, content: reviewActionForm.content }
    if (reviewActionMode.value === 'return') {
      await ticketApi.returnForSupplement(payload)
      message.success('已退回客户补充')
    } else {
      await ticketApi.reject(payload)
      message.success('工单已驳回')
    }
    reviewActionVisible.value = false
    fetchData()
  } finally {
    reviewActionLoading.value = false
  }
}

const submitAssign = async () => {
  if (!assignForm.handlerId) {
    message.warning('请选择处理人')
    return
  }
  assignLoading.value = true
  try {
    await ticketApi.assign({ ticketId: currentTicket.value.id, handlerId: assignForm.handlerId, reason: assignForm.reason })
    message.success('分派成功')
    assignVisible.value = false
    fetchData()
  } finally {
    assignLoading.value = false
  }
}

const goDetail = id => router.push(`/ticket/detail/${id}`)

const statusText = value => getOptionLabel(statusOptions.value, value, value || '-')
const statusColor = value => getOptionColor(statusOptions.value, value, 'default')
const priorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const priorityColor = value => getOptionColor(priorityOptions.value, value, 'default')
const categoryText = value => getOptionLabel(categoryOptions.value, value, value || '-')

onMounted(async () => {
  await fetchMeta()
  await fetchHandlerOptions()
  fetchData()
})
</script>

<style scoped>
.ticket-pending-page {
  display: flex;
  flex-direction: column;
}

.table-card {
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.link-text {
  font-weight: 600;
}

.query-actions {
  align-self: flex-end;
}

.table-action-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
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

.table-action-link--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.table-action-link--success:hover {
  border-color: #86efac;
  background: #dcfce7;
  color: #166534;
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

.table-action-link--warning {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.table-action-link--warning:hover {
  border-color: #fcd34d;
  background: #fef3c7;
  color: #92400e;
}

.table-action-link--danger {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.table-action-link--danger:hover {
  border-color: #fca5a5;
  background: #fee2e2;
  color: #991b1b;
}

@media (max-width: 1200px) {
  .query-actions {
    width: 100%;
  }
}
</style>
