<template>
  <div class="page-container ticket-detail-page">
    <a-button type="link" class="back-link" @click="router.back()">
      <template #icon><arrow-left-outlined /></template>
      返回
    </a-button>

    <a-card v-if="ticket" :bordered="false" class="detail-card">
      <div class="detail-header">
        <div>
          <h2>{{ ticket.ticketNo }} - {{ ticket.title }}</h2>
          <p>{{ categoryText(ticket.category) }} / {{ priorityText(ticket.priority) }}</p>
        </div>
        <a-space wrap>
          <a-button v-if="canProcessTicket" type="primary" @click="router.push(`/ticket/process/${ticket.id}`)">进入处理页</a-button>
          <a-button v-if="canEditKnowledge && ticket.solution" @click="draftKnowledge">沉淀知识库</a-button>
          <a-button v-if="canWarnSla" @click="openAction('sla')">SLA 提醒</a-button>
          <a-button v-if="canCancelTicket" danger @click="openAction('cancel')">撤销工单</a-button>
          <a-button v-if="canReopenTicket" @click="openAction('reopen')">重开工单</a-button>
          <a-button v-if="canCloseTicket" @click="closeTicket">关闭工单</a-button>
          <a-button v-if="canEvaluateTicket" type="primary" @click="openAction('evaluate')">评价</a-button>
        </a-space>
      </div>

      <a-alert
        v-if="ticket.mergeParentId"
        class="merge-alert"
        type="info"
        show-icon
        :message="`该工单已合并至主工单 #${ticket.mergeParentId}`"
        :description="ticket.mergeReason || '重复问题已合并处理。'"
      >
        <template #action>
          <a-button size="small" type="link" @click="router.push(`/ticket/detail/${ticket.mergeParentId}`)">查看主工单</a-button>
        </template>
      </a-alert>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :xl="8">
          <div class="info-card">
            <h3>基本信息</h3>
            <dl>
              <div>
                <dt>状态</dt>
                <dd><a-tag :color="statusColor(ticket.status)">{{ statusText(ticket.status) }}</a-tag></dd>
              </div>
              <div>
                <dt>优先级</dt>
                <dd><a-tag :color="priorityColor(ticket.priority)">{{ priorityText(ticket.priority) }}</a-tag></dd>
              </div>
              <div>
                <dt>客户</dt>
                <dd>{{ ticket.creatorName || '-' }}</dd>
              </div>
              <div>
                <dt>联系电话</dt>
                <dd>{{ ticket.creatorPhone || '-' }}</dd>
              </div>
              <div>
                <dt>联系邮箱</dt>
                <dd>{{ ticket.creatorEmail || '-' }}</dd>
              </div>
              <div>
                <dt>当前处理人</dt>
                <dd>{{ ticket.handlerName || '未接单' }}</dd>
              </div>
              <div>
                <dt>重开次数</dt>
                <dd>{{ ticket.reopenCount || 0 }}</dd>
              </div>
              <div>
                <dt>SLA 提醒时间</dt>
                <dd>{{ formatDateTime(ticket.slaWarnTime) || '-' }}</dd>
              </div>
              <div>
                <dt>客户评价</dt>
                <dd>{{ ticket.evaluationScore ? `${ticket.evaluationScore} 分` : '未评价' }}</dd>
              </div>
            </dl>
          </div>
        </a-col>

        <a-col :xs="24" :xl="16">
          <div class="problem-card">
            <h3>问题描述</h3>
            <div class="problem-box">{{ ticket.description }}</div>
            <div v-if="ticket.solution" class="solution-box">
              <strong>最终解决方案</strong>
              <p>{{ ticket.solution }}</p>
            </div>
            <div class="attachment-panel">
              <div class="attachment-panel__head">
                <strong>工单附件</strong>
                <span>{{ files.length }}</span>
              </div>
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
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-card :bordered="false" class="detail-card">
      <template #title>流程留痕</template>
      <ticket-flow-stream :flows="flows" :action-text="actionText" />
    </a-card>

    <a-modal
      v-model:open="actionVisible"
      :title="actionTitle"
      :confirm-loading="actionLoading"
      @ok="submitAction"
    >
      <a-form layout="vertical" :model="actionForm">
        <a-form-item v-if="actionMode === 'evaluate'" label="评分" required>
          <a-rate v-model:value="actionForm.rating" />
        </a-form-item>
        <a-form-item v-if="actionMode === 'sla'" label="提醒类型">
          <a-select v-model:value="actionForm.slaType">
            <a-select-option value="response">响应超时</a-select-option>
            <a-select-option value="resolve">解决超时</a-select-option>
            <a-select-option value="manual">人工关注</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="actionContentLabel" :required="actionMode !== 'sla'">
          <a-textarea v-model:value="actionForm.content" :rows="4" :placeholder="actionPlaceholder" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { knowledgeApi, ticketApi } from '@/api/ticket'
import { buildFileDownloadUrl, buildFilePreviewUrl } from '@/utils/file'
import { formatDateTime } from '@/utils/datetime'
import { getCurrentUserId, hasAnyRole } from '@/utils/auth'
import { hasAnyPermission } from '@/utils/permission'
import { formatFileSize } from '@/utils/upload'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'
import TicketFlowStream from '../components/TicketFlowStream.vue'

const route = useRoute()
const router = useRouter()
const ticket = ref(null)
const flows = ref([])
const files = ref([])
const statusOptions = ref([])
const priorityOptions = ref([])
const categoryOptions = ref([])
const actionOptions = ref([])
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionMode = ref('cancel')

const currentUserId = computed(() => getCurrentUserId())
const managerMode = computed(() => hasAnyRole(['admin', 'system_admin', 'supervisor']))
const terminalStatuses = ['closed', 'rejected', 'cancelled']

const actionForm = reactive({
  content: '',
  rating: 5,
  slaType: 'manual'
})

const canEditKnowledge = computed(() => hasAnyPermission(['knowledge:edit']))
const canProcessTicket = computed(() => {
  if (!ticket.value || ['pending_approval', 'pending', ...terminalStatuses].includes(ticket.value.status)) {
    return false
  }
  return (managerMode.value || ticket.value.handlerId === currentUserId.value) && hasAnyPermission(['ticket:reply', 'ticket:resolve', 'ticket:transfer'])
})
const canCloseTicket = computed(() => {
  if (!ticket.value || ticket.value.status !== 'resolved') {
    return false
  }
  const relatedUser = ticket.value.creatorId === currentUserId.value || ticket.value.handlerId === currentUserId.value
  return hasAnyPermission(['ticket:close', 'ticket:my:close']) && (managerMode.value || relatedUser)
})
const canCancelTicket = computed(() => {
  if (!ticket.value || !['draft', 'pending_approval', 'pending', 'waiting_customer'].includes(ticket.value.status)) {
    return false
  }
  return hasAnyPermission(['ticket:my:close', 'ticket:assign']) && (managerMode.value || ticket.value.creatorId === currentUserId.value)
})
const canReopenTicket = computed(() => {
  if (!ticket.value || !['resolved', 'closed'].includes(ticket.value.status)) {
    return false
  }
  return hasAnyPermission(['ticket:my:reply', 'ticket:assign']) && (managerMode.value || ticket.value.creatorId === currentUserId.value)
})
const canEvaluateTicket = computed(() => {
  return ticket.value?.status === 'closed'
    && !ticket.value?.evaluationScore
    && ticket.value?.creatorId === currentUserId.value
    && hasAnyPermission(['ticket:my:close'])
})
const canWarnSla = computed(() => {
  return ticket.value
    && managerMode.value
    && hasAnyPermission(['ticket:assign'])
    && !terminalStatuses.includes(ticket.value.status)
})

const actionTitle = computed(() => {
  const map = {
    cancel: '撤销工单',
    reopen: '重开工单',
    evaluate: '评价工单',
    sla: 'SLA 提醒'
  }
  return map[actionMode.value] || '处理工单'
})
const actionContentLabel = computed(() => {
  const map = {
    cancel: '撤销原因',
    reopen: '重开原因',
    evaluate: '评价内容',
    sla: '提醒说明'
  }
  return map[actionMode.value] || '说明'
})
const actionPlaceholder = computed(() => {
  const map = {
    cancel: '说明为什么撤销该工单',
    reopen: '写清未解决的问题或新的异常现象',
    evaluate: '可以补充服务体验、响应速度或解决质量',
    sla: '说明需要关注的时限、风险或升级对象'
  }
  return map[actionMode.value] || '请填写说明'
})

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  statusOptions.value = meta.statusOptions || []
  priorityOptions.value = meta.priorityOptions || []
  categoryOptions.value = meta.categoryOptions || []
  actionOptions.value = meta.actionOptions || []
}

const fetchDetail = async () => {
  const res = await ticketApi.detail(route.params.id)
  ticket.value = res.data.ticket
  flows.value = res.data.flows || []
  files.value = res.data.files || []
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

const openAction = mode => {
  actionMode.value = mode
  actionForm.content = ''
  actionForm.rating = 5
  actionForm.slaType = 'manual'
  actionVisible.value = true
}

const submitAction = async () => {
  if (actionMode.value !== 'sla' && !actionForm.content.trim()) {
    message.warning(`请填写${actionContentLabel.value}`)
    return
  }
  actionLoading.value = true
  try {
    const payload = {
      ticketId: ticket.value.id,
      content: actionForm.content,
      rating: actionForm.rating,
      slaType: actionForm.slaType
    }
    if (actionMode.value === 'cancel') {
      await ticketApi.cancel(payload)
      message.success('工单已撤销')
    } else if (actionMode.value === 'reopen') {
      await ticketApi.reopen(payload)
      message.success('工单已重开')
    } else if (actionMode.value === 'evaluate') {
      await ticketApi.evaluate(payload)
      message.success('评价已提交')
    } else if (actionMode.value === 'sla') {
      await ticketApi.warnSla(payload)
      message.success('SLA 提醒已记录')
    }
    actionVisible.value = false
    fetchDetail()
  } finally {
    actionLoading.value = false
  }
}

const statusText = value => getOptionLabel(statusOptions.value, value, value || '-')
const statusColor = value => getOptionColor(statusOptions.value, value, 'default')
const priorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const priorityColor = value => getOptionColor(priorityOptions.value, value, 'default')
const categoryText = value => getOptionLabel(categoryOptions.value, value, value || '-')
const actionText = value => getOptionLabel(actionOptions.value, value, value)

onMounted(async () => {
  await fetchMeta()
  fetchDetail()
})
</script>

<style scoped>
.ticket-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.back-link {
  width: fit-content;
  padding-inline: 0;
  color: #475569;
  font-weight: 600;
}

.back-link:hover {
  color: #0f766e;
}

.detail-card {
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.detail-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.detail-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.merge-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

.info-card,
.problem-card {
  height: 100%;
  padding: 16px 18px;
  border: 1px solid #e5edf5;
  border-radius: 16px;
  background: #ffffff;
}

.info-card h3,
.problem-card h3 {
  margin: 0 0 14px;
  color: #0f172a;
  font-size: 15px;
}

.info-card dl {
  display: grid;
  gap: 12px;
  margin: 0;
}

.info-card dl div {
  display: grid;
  gap: 4px;
}

.info-card dt {
  color: #64748b;
  font-size: 12px;
}

.info-card dd {
  margin: 0;
  color: #1e293b;
  line-height: 1.6;
}

.problem-box,
.solution-box {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  color: #172033;
  line-height: 1.8;
  white-space: pre-wrap;
}

.solution-box {
  margin-top: 12px;
  background: #f0fdf4;
}

.solution-box strong {
  display: block;
  margin-bottom: 6px;
  color: #166534;
}

.solution-box p {
  margin: 0;
}

.attachment-panel {
  margin-top: 14px;
}

.attachment-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #475569;
}

.muted {
  color: #64748b;
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
  }
}
</style>
