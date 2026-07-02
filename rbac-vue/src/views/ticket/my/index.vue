<template>
  <div class="page-container">
    <a-card :bordered="false" class="page-title">
      <div>
        <h2>我的工单</h2>
        <p>提交问题、补充资料并跟踪处理进度。</p>
      </div>
      <a-button type="primary" @click="createVisible = true">提交工单</a-button>
    </a-card>

    <a-card :bordered="false">
      <a-table row-key="id" :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ticketNo'">
            <a @click="goDetail(record.id)">{{ record.ticketNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag>{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a @click="goDetail(record.id)">查看</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createVisible" title="提交工单" width="720px" @ok="submitTicket">
      <a-form layout="vertical" :model="form">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="一句话描述问题" />
        </a-form-item>
        <a-form-item label="问题描述" required>
          <a-textarea v-model:value="form.description" :rows="5" placeholder="请描述问题现象、影响范围、出现时间" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="分类">
              <a-select v-model:value="form.category">
                <a-select-option value="general">通用问题</a-select-option>
                <a-select-option value="network">网络问题</a-select-option>
                <a-select-option value="system">系统故障</a-select-option>
                <a-select-option value="account">账号权限</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优先级">
              <a-select v-model:value="form.priority">
                <a-select-option value="low">低</a-select-option>
                <a-select-option value="normal">普通</a-select-option>
                <a-select-option value="high">高</a-select-option>
                <a-select-option value="urgent">紧急</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="联系电话">
              <a-input v-model:value="form.contactPhone" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="form.contactEmail" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="附件">
          <a-upload :showUploadList="false" :customRequest="uploadCreateFile">
            <a-button>上传图片/附件</a-button>
          </a-upload>
          <a-list v-if="createFiles.length" size="small" :data-source="createFiles" class="file-list">
            <template #renderItem="{ item }">
              <a-list-item>{{ item.fileName }}（{{ formatFileSize(item.fileSize) }}）</a-list-item>
            </template>
          </a-list>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ticketApi } from '@/api/ticket'
import { createUploadRequest, formatFileSize } from '@/utils/upload'
import { ticketStatusText } from '@/constants/ticket'

const router = useRouter()
const loading = ref(false)
const createVisible = ref(false)
const dataSource = ref([])
const createFiles = ref([])
const query = reactive({ owner: 'customer', pageNum: 1, pageSize: 10 })
const form = reactive({ title: '', description: '', category: 'general', priority: 'normal', contactPhone: '', contactEmail: '', fileIds: [] })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 180 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 100 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await ticketApi.list(query)
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

const submitTicket = async () => {
  await ticketApi.create(form)
  message.success('工单已提交')
  createVisible.value = false
  form.title = ''
  form.description = ''
  form.fileIds = []
  createFiles.value = []
  fetchData()
}

const uploadCreateFile = createUploadRequest((file) => {
  form.fileIds.push(file.id)
  createFiles.value.push(file)
  message.success('附件上传成功')
})

const goDetail = (id) => router.push(`/ticket/detail/${id}`)
const statusText = ticketStatusText

onMounted(fetchData)
</script>

<style scoped>
.page-title {
  margin-bottom: 16px;
}

.page-title :deep(.ant-card-body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h2 {
  margin: 0 0 6px;
  font-size: 28px;
}

p {
  margin: 0;
  color: #64748b;
}

.file-list {
  margin-top: 10px;
}
</style>
