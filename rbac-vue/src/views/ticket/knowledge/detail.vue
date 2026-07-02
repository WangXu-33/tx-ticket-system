<template>
  <div class="page-container">
    <a-card :bordered="false" title="知识库维护">
      <template #extra>
        <a-space>
          <a-button @click="router.back()">返回</a-button>
          <a-button type="primary" @click="save">保存</a-button>
        </a-space>
      </template>

      <a-form layout="vertical" :model="form">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="标准问题标题" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="分类">
              <a-input v-model:value="form.category" placeholder="例如 network/system/account" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标签">
              <a-input v-model:value="form.tags" placeholder="逗号分隔，便于后续 Agent 检索" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="问题现象">
          <a-textarea v-model:value="form.phenomenon" :rows="4" />
        </a-form-item>
        <a-form-item label="原因分析">
          <a-textarea v-model:value="form.causeAnalysis" :rows="4" />
        </a-form-item>
        <a-form-item label="解决步骤" required>
          <a-textarea v-model:value="form.solutionSteps" :rows="7" />
        </a-form-item>
        <a-form-item label="适用范围">
          <a-textarea v-model:value="form.applicableScope" :rows="3" placeholder="适用系统、版本、客户环境、限制条件" />
        </a-form-item>
        <a-form-item label="来源工单 ID">
          <a-input-number v-model:value="form.sourceTicketId" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const form = reactive({
  id: null,
  title: '',
  category: 'general',
  tags: '',
  phenomenon: '',
  causeAnalysis: '',
  solutionSteps: '',
  applicableScope: '',
  sourceTicketId: null,
  linkedTicketIds: [],
  fileIds: []
})

const fetchDetail = async () => {
  if (!route.params.id) return
  const res = await request.get(`/knowledge/detail/${route.params.id}`)
  Object.assign(form, res.data.article)
}

const save = async () => {
  await request.post('/knowledge/save', form)
  message.success('知识库已保存')
  router.push('/ticket/knowledge')
}

onMounted(fetchDetail)
</script>
