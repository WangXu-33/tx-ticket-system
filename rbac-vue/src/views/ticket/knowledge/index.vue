<template>
  <div class="page-container">
    <ticket-filter-card v-model:collapsed="queryCollapsed">
      <a-form layout="inline" :model="query" class="compact-filter-form">
        <a-form-item label="标题">
          <a-input v-model:value="query.title" allow-clear placeholder="标题" style="width: 240px" />
        </a-form-item>
        <a-form-item label="标签">
          <a-input v-model:value="query.tags" allow-clear placeholder="标签关键词" style="width: 220px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="状态" style="width: 140px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="reviewing">待审核</a-select-option>
            <a-select-option value="rejected">审核驳回</a-select-option>
            <a-select-option value="published">已发布</a-select-option>
            <a-select-option value="withdrawn">已下架</a-select-option>
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
      <template #title>知识列表</template>
      <template #extra>
        <a-button v-if="canEditKnowledge" type="primary" @click="router.push('/ticket/knowledge/detail')">新增知识</a-button>
      </template>
      <a-table row-key="id" :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="router.push(`/ticket/knowledge/detail/${record.id}`)">{{ record.title }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a
                v-if="canEditKnowledge && ['draft', 'rejected'].includes(record.status)"
                class="table-action-link table-action-link--primary"
                @click="router.push(`/ticket/knowledge/detail/${record.id}`)"
              >
                编辑
              </a>
              <a
                v-else
                class="table-action-link"
                @click="router.push(`/ticket/knowledge/detail/${record.id}`)"
              >
                查看
              </a>
              <a v-if="canEditKnowledge && ['draft', 'rejected'].includes(record.status)" class="table-action-link table-action-link--review" @click="submitReview(record.id)">提交审核</a>
              <a v-if="canPublishKnowledge && record.status === 'reviewing'" class="table-action-link table-action-link--success" @click="publish(record.id)">审核通过</a>
              <a v-if="canPublishKnowledge && record.status === 'published'" class="table-action-link table-action-link--warning" @click="withdraw(record.id)">下架</a>
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
import { message } from 'ant-design-vue'
import { knowledgeApi } from '@/api/ticket'
import { formatDateTime } from '@/utils/datetime'
import { hasAnyPermission } from '@/utils/permission'
import TicketFilterCard from '../components/TicketFilterCard.vue'

const router = useRouter()
const loading = ref(false)
const dataSource = ref([])
const queryCollapsed = ref(false)
const query = reactive({ title: '', tags: '', status: undefined, pageNum: 1, pageSize: 10 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const canEditKnowledge = hasAnyPermission(['knowledge:edit'])
const canPublishKnowledge = hasAnyPermission(['knowledge:publish'])

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '分类', dataIndex: 'category', key: 'category', width: 130 },
  { title: '标签', dataIndex: 'tags', key: 'tags', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 180, customRender: ({ text }) => formatDateTime(text) },
  { title: '操作', key: 'action', width: 240 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await knowledgeApi.list(query)
    dataSource.value = res.data.records || []
    pagination.total = res.data.total || 0
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
  query.title = ''
  query.tags = ''
  query.status = undefined
  query.pageNum = 1
  fetchData()
}

const publish = async (id) => {
  await knowledgeApi.publish(id)
  message.success('已发布')
  fetchData()
}

const submitReview = async (id) => {
  await knowledgeApi.submitReview(id)
  message.success('已提交审核')
  fetchData()
}

const withdraw = async (id) => {
  await knowledgeApi.withdraw(id)
  message.success('已下架')
  fetchData()
}

const statusText = (value) => ({ draft: '草稿', reviewing: '待审核', rejected: '审核驳回', published: '已发布', withdrawn: '已下架' }[value] || value)
const statusColor = (value) => ({ draft: 'orange', reviewing: 'gold', rejected: 'red', published: 'green', withdrawn: 'default' }[value] || 'default')

onMounted(fetchData)
</script>

<style scoped>
.table-card {
  margin-bottom: 16px;
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
  min-width: 42px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
  font-weight: 600;
}

.table-action-link--primary {
  background: #e0ecff;
  color: #1d4ed8;
}

.table-action-link--success {
  background: #dcfce7;
  color: #15803d;
}

.table-action-link--review {
  background: #fef3c7;
  color: #92400e;
}

.table-action-link--warning {
  background: #fef3c7;
  color: #b45309;
}

@media (max-width: 900px) {
  .query-actions {
    width: 100%;
  }
}
</style>
