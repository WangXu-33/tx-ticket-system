<template>
  <div class="page-container ticket-system-page">
    <ticket-filter-card v-model:collapsed="queryCollapsed">
      <a-form layout="inline" :model="query" class="compact-filter-form">
        <a-form-item label="系统名称">
          <a-input v-model:value="query.keyword" allow-clear placeholder="系统名称" style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="状态" style="width: 140px">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
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
      <template #title>系统配置</template>
      <template #extra>
        <a-button v-if="canManageSystem" type="primary" @click="openCreate">新增系统</a-button>
      </template>
      <a-table
        row-key="id"
        size="small"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1120 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'defaultPriority'">
            <a-tag :color="priorityColor(record.defaultPriority)">
              {{ priorityText(record.defaultPriority) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space v-if="canManageSystem" size="small" wrap>
              <a class="table-action-link table-action-link--primary" @click="openEdit(record)">编辑</a>
              <a class="table-action-link" @click="toggleStatus(record)">
                {{ record.status === 1 ? '停用' : '启用' }}
              </a>
            </a-space>
            <span v-else class="readonly-text">只读</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="editorVisible"
      :title="form.id ? '编辑系统' : '新增系统'"
      :confirm-loading="saving"
      @ok="saveSystem"
      @cancel="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="系统名称" required>
          <a-input v-model:value="form.name" allow-clear placeholder="例如：ERP 系统" />
        </a-form-item>
        <a-form-item label="系统编码" required>
          <a-input v-model:value="form.code" allow-clear placeholder="例如：erp" />
        </a-form-item>
        <a-form-item label="负责组 / 处理组">
          <a-input v-model:value="form.ownerGroup" allow-clear placeholder="例如：应用运维一组" />
        </a-form-item>
        <a-form-item label="默认优先级">
          <a-select v-model:value="form.defaultPriority" placeholder="请选择默认优先级">
            <a-select-option v-for="item in priorityOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="说明">
          <a-textarea
            v-model:value="form.remark"
            :rows="4"
            placeholder="说明该系统受理范围、常见问题或值班要求"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ticketApi } from '@/api/ticket'
import TicketFilterCard from './components/TicketFilterCard.vue'
import { hasAnyRole } from '@/utils/auth'
import { hasAnyPermission } from '@/utils/permission'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'

const loading = ref(false)
const saving = ref(false)
const queryCollapsed = ref(false)
const editorVisible = ref(false)
const dataSource = ref([])
const priorityOptions = ref([])
const canManageSystem = computed(() =>
  hasAnyRole(['admin', 'system_admin', 'supervisor'])
  && hasAnyPermission(['ticket:assign', 'sys:dict:add', 'sys:dict:edit'])
)

const query = reactive({
  keyword: '',
  status: undefined
})

const form = reactive({
  id: null,
  code: '',
  name: '',
  ownerGroup: '',
  defaultPriority: 'normal',
  remark: '',
  status: 1
})

const columns = [
  { title: '系统名称', dataIndex: 'name', key: 'name', width: 220, ellipsis: true },
  { title: '系统编码', dataIndex: 'code', key: 'code', width: 140 },
  { title: '负责组', dataIndex: 'ownerGroup', key: 'ownerGroup', width: 180, ellipsis: true },
  { title: '默认优先级', dataIndex: 'defaultPriority', key: 'defaultPriority', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '说明', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  priorityOptions.value = meta.priorityOptions || []
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await ticketApi.systemList(query)
    dataSource.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.keyword = ''
  query.status = undefined
  fetchData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    code: '',
    name: '',
    ownerGroup: '',
    defaultPriority: 'normal',
    remark: '',
    status: 1
  })
}

const openCreate = () => {
  resetForm()
  editorVisible.value = true
}

const openEdit = record => {
  Object.assign(form, {
    id: record.id,
    code: record.code,
    name: record.name,
    ownerGroup: record.ownerGroup,
    defaultPriority: record.defaultPriority || 'normal',
    remark: record.remark || '',
    status: record.status ?? 1
  })
  editorVisible.value = true
}

const saveSystem = async () => {
  if (!form.name.trim() || !form.code.trim()) {
    message.warning('请填写系统名称和系统编码')
    return
  }
  saving.value = true
  try {
    await ticketApi.saveSystem({ ...form, code: form.code.trim(), name: form.name.trim() })
    message.success(form.id ? '系统配置已更新' : '系统配置已新增')
    editorVisible.value = false
    resetForm()
    fetchData()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async record => {
  await ticketApi.toggleSystem(record.id)
  message.success(record.status === 1 ? '系统已停用' : '系统已启用')
  fetchData()
}

const priorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const priorityColor = value => getOptionColor(priorityOptions.value, value, 'default')

onMounted(async () => {
  await fetchMeta()
  fetchData()
})
</script>

<style scoped>
.ticket-system-page {
  display: flex;
  flex-direction: column;
}

.table-card {
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
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

.readonly-text {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .query-actions {
    width: 100%;
  }
}
</style>
