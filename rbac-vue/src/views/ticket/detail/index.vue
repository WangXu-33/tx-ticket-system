<template>
  <div class="page-container">
    <a-card :bordered="false" class="detail-card" v-if="ticket">
      <template #title>
        <span>{{ ticket.ticketNo }} - {{ ticket.title }}</span>
      </template>
      <template #extra>
          <a-space>
            <a-button @click="router.back()">返回</a-button>
          <a-button v-if="canEditKnowledge && ticket.solution" @click="draftKnowledge">沉淀知识库</a-button>
            <a-button v-if="canResolveTicket" type="primary" @click="resolveVisible = true">标记解决</a-button>
            <a-button v-if="canCloseTicket" @click="closeTicket">关闭</a-button>
          </a-space>
      </template>

      <a-descriptions bordered :column="2">
        <a-descriptions-item label="状态">{{ statusText(ticket.status) }}</a-descriptions-item>
        <a-descriptions-item label="优先级">{{ ticket.priority }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ ticket.creatorName }}</a-descriptions-item>
        <a-descriptions-item label="处理人">{{ ticket.handlerName || '未分派' }}</a-descriptions-item>
        <a-descriptions-item label="问题描述" :span="2">{{ ticket.description }}</a-descriptions-item>
        <a-descriptions-item v-if="ticket.solution" label="解决方案" :span="2">{{ ticket.solution }}</a-descriptions-item>
      </a-descriptions>

      <a-divider />
      <h3>工单附件</h3>
      <a-empty v-if="!files.length" description="暂无附件" />
      <a-list v-else :data-source="files" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-space>
              <a :href="buildFilePreviewUrl(item.id)" target="_blank">{{ item.fileName }}</a>
              <span class="muted">{{ formatFileSize(item.fileSize) }}</span>
              <a :href="buildFileDownloadUrl(item.id)" target="_blank">下载</a>
            </a-space>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :xs="24" :lg="14">
        <a-card title="流程记录" :bordered="false">
          <a-timeline>
            <a-timeline-item v-for="flow in flows" :key="flow.id">
              <strong>{{ actionText(flow.action) }}</strong>
              <p>{{ flow.content }}</p>
              <small>{{ flow.operatorName }} · {{ flow.createTime }}</small>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="回复工单" :bordered="false">
          <a-form layout="vertical" :model="replyForm">
            <a-form-item label="回复内容">
              <a-textarea v-model:value="replyForm.content" :rows="6" />
            </a-form-item>
            <a-form-item label="可见范围">
              <a-radio-group v-model:value="replyForm.visibleScope">
                <a-radio value="public">客户可见</a-radio>
                <a-radio value="internal">内部备注</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="回复附件">
              <a-upload :showUploadList="false" :customRequest="uploadReplyFile">
                <a-button>上传附件</a-button>
              </a-upload>
              <a-list v-if="replyFiles.length" size="small" :data-source="replyFiles" class="file-list">
                <template #renderItem="{ item }">
                  <a-list-item>{{ item.fileName }}（{{ formatFileSize(item.fileSize) }}）</a-list-item>
                </template>
              </a-list>
            </a-form-item>
            <a-button type="primary" block @click="submitReply">提交回复</a-button>
          </a-form>
        </a-card>
      </a-col>
    </a-row>

    <a-modal v-model:open="resolveVisible" title="标记工单已解决" @ok="submitResolve">
      <a-textarea v-model:value="resolveForm.solution" :rows="6" placeholder="填写最终解决方案，后续可沉淀为知识库" />
      <a-divider />
      <a-upload :showUploadList="false" :customRequest="uploadResolveFile">
        <a-button>上传解决附件</a-button>
      </a-upload>
      <a-list v-if="resolveFiles.length" size="small" :data-source="resolveFiles" class="file-list">
        <template #renderItem="{ item }">
          <a-list-item>{{ item.fileName }}（{{ formatFileSize(item.fileSize) }}）</a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { knowledgeApi, ticketApi } from '@/api/ticket'
import { buildFileDownloadUrl, buildFilePreviewUrl } from '@/utils/file'
import { createUploadRequest, formatFileSize } from '@/utils/upload'
import { hasAnyPermission } from '@/utils/permission'
import {
  TICKET_STATUS,
  isTicketTerminal,
  ticketActionText,
  ticketStatusText
} from '@/constants/ticket'

const route = useRoute()
const router = useRouter()
const ticket = ref(null)
const flows = ref([])
const files = ref([])
const replyFiles = ref([])
const resolveFiles = ref([])
const resolveVisible = ref(false)
const replyForm = reactive({ ticketId: null, content: '', visibleScope: 'public', fileIds: [] })
const resolveForm = reactive({ ticketId: null, solution: '', fileIds: [] })
const canEditKnowledge = computed(() => hasAnyPermission(['knowledge:edit']))
const canResolveTicket = computed(() => {
  return hasAnyPermission(['ticket:resolve']) && ticket.value && !isTicketTerminal(ticket.value.status) && ticket.value.status !== TICKET_STATUS.RESOLVED
})
const canCloseTicket = computed(() => {
  return hasAnyPermission(['ticket:close', 'ticket:my:close']) && ticket.value?.status === TICKET_STATUS.RESOLVED
})

const fetchDetail = async () => {
  const res = await ticketApi.detail(route.params.id)
  ticket.value = res.data.ticket
  flows.value = res.data.flows || []
  files.value = res.data.files || []
  replyForm.ticketId = ticket.value.id
  resolveForm.ticketId = ticket.value.id
}

const submitReply = async () => {
  await ticketApi.reply(replyForm)
  message.success('回复已保存')
  replyForm.content = ''
  replyForm.fileIds = []
  replyFiles.value = []
  fetchDetail()
}

const submitResolve = async () => {
  await ticketApi.resolve(resolveForm)
  message.success('工单已标记解决')
  resolveVisible.value = false
  resolveForm.solution = ''
  resolveForm.fileIds = []
  resolveFiles.value = []
  fetchDetail()
}

const closeTicket = async () => {
  await ticketApi.close(ticket.value.id)
  message.success('工单已关闭')
  fetchDetail()
}

const draftKnowledge = async () => {
  const res = await knowledgeApi.draftFromTicket(ticket.value.id)
  message.success('知识库草稿已生成')
  router.push(`/ticket/knowledge/detail/${res.data}`)
}

const uploadReplyFile = createUploadRequest((file) => {
  replyForm.fileIds.push(file.id)
  replyFiles.value.push(file)
  message.success('附件上传成功')
})

const uploadResolveFile = createUploadRequest((file) => {
  resolveForm.fileIds.push(file.id)
  resolveFiles.value.push(file)
  message.success('附件上传成功')
})

const statusText = ticketStatusText
const actionText = ticketActionText

onMounted(fetchDetail)
</script>

<style scoped>
.detail-card :deep(.ant-card-head-title) {
  font-size: 18px;
  font-weight: 800;
}

.muted {
  color: #64748b;
}

.file-list {
  margin-top: 10px;
}
</style>
