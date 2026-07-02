<template>
  <div class="page-container">
    <a-card :bordered="false" class="knowledge-hero">
      <div>
        <p>Knowledge Base</p>
        <h2>知识库</h2>
      </div>
      <a-button type="primary" @click="router.push('/ticket/knowledge/detail')">新增知识</a-button>
    </a-card>

    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="query">
        <a-form-item label="关键词">
          <a-input v-model:value="query.keyword" allow-clear placeholder="标题/标签/方案" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear style="width: 140px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="published">已发布</a-select-option>
            <a-select-option value="withdrawn">已下架</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="fetchData">查询</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table row-key="id" :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="router.push(`/ticket/knowledge/detail/${record.id}`)">{{ record.title }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'published' ? 'green' : record.status === 'draft' ? 'orange' : 'default'">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="router.push(`/ticket/knowledge/detail/${record.id}`)">编辑</a>
              <a v-if="record.status !== 'published'" @click="publish(record.id)">发布</a>
              <a v-if="record.status === 'published'" @click="withdraw(record.id)">下架</a>
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

const router = useRouter()
const loading = ref(false)
const dataSource = ref([])
const query = reactive({ keyword: '', status: undefined, pageNum: 1, pageSize: 10 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '分类', dataIndex: 'category', key: 'category', width: 130 },
  { title: '标签', dataIndex: 'tags', key: 'tags', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 180 },
  { title: '操作', key: 'action', width: 160 }
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

const publish = async (id) => {
  await knowledgeApi.publish(id)
  message.success('已发布')
  fetchData()
}

const withdraw = async (id) => {
  await knowledgeApi.withdraw(id)
  message.success('已下架')
  fetchData()
}

const statusText = (value) => ({ draft: '草稿', published: '已发布', withdrawn: '已下架' }[value] || value)

onMounted(fetchData)
</script>

<style scoped>
.knowledge-hero {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #1e293b, #0369a1);
  color: #fff;
}

.knowledge-hero :deep(.ant-card-body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.knowledge-hero p {
  margin: 0 0 6px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #bae6fd;
  font-weight: 800;
}

.knowledge-hero h2 {
  color: #fff;
  margin: 0;
  font-size: 30px;
}

.search-card {
  margin-bottom: 16px;
}
</style>
