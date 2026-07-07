<template>
  <div class="ticket-process-page">
    <a-button type="link" class="back-link" @click="router.back()">
      <template #icon><arrow-left-outlined /></template>
      返回
    </a-button>

    <a-card v-if="ticket" :bordered="false" class="section-card">
      <div class="ticket-topbar">
        <div class="ticket-topbar__title">
          <h2>{{ ticket.ticketNo }} - {{ ticket.title }}</h2>
          <p>{{ categoryText(ticket.category) }} / {{ priorityText(ticket.priority) }}</p>
        </div>
        <div class="ticket-topbar__tags">
          <a-tag :color="statusColor(ticket.status)">{{ statusText(ticket.status) }}</a-tag>
          <a-tag :color="priorityColor(ticket.priority)">{{ priorityText(ticket.priority) }}</a-tag>
        </div>
      </div>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :xl="8">
          <div class="info-card">
            <h3>客户信息</h3>
            <dl>
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
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(ticket.createTime) }}</dd>
              </div>
              <div>
                <dt>当前处理人</dt>
                <dd>{{ ticket.handlerName || '未接单' }}</dd>
              </div>
            </dl>
          </div>
        </a-col>

        <a-col :xs="24" :xl="16">
          <div class="problem-card">
            <h3>客户提交的问题</h3>
            <div class="problem-box">{{ ticket.description || '暂无问题描述' }}</div>
            <div class="attachment-panel">
              <div class="attachment-panel__head">
                <strong>原始附件</strong>
                <span>{{ files.length }}</span>
              </div>
              <a-empty v-if="!files.length" description="暂无附件" />
              <a-list v-else size="small" :data-source="files">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a :href="buildFilePreviewUrl(item.id)" target="_blank" rel="noreferrer">{{ item.fileName }}</a>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-card v-if="ticket && canReply" :bordered="false" class="section-card ai-assist-card">
      <div class="ai-assist-card__head">
        <div>
          <h3>AI 排查助手</h3>
          <p>{{ aiAssistSummary }}</p>
        </div>
        <a-space wrap>
          <a-button size="small" :loading="aiLoading" @click="fetchAiDiagnose">刷新建议</a-button>
          <a-button size="small" :disabled="aiLoading" @click="appendAiChecklist">写入排查清单</a-button>
          <a-button size="small" type="primary" :disabled="aiLoading" @click="draftAiSolution">生成解决草稿</a-button>
        </a-space>
      </div>
      <div class="ai-hint-grid">
        <div v-for="item in aiHints" :key="item.title" class="ai-hint-item">
          <strong>{{ item.title }}</strong>
          <span>{{ item.content }}</span>
        </div>
      </div>
      <div v-if="aiKnowledgeCandidates.length" class="ai-knowledge-strip">
        <strong>可参考知识</strong>
        <a-space wrap>
          <a
            v-for="item in aiKnowledgeCandidates"
            :key="item.id"
            @click="router.push(`/ticket/knowledge/detail/${item.id}`)"
          >
            {{ item.title }}
          </a>
        </a-space>
      </div>
    </a-card>

    <a-card v-if="canReply" :bordered="false" class="section-card">
      <template #title>当前处理记录</template>
      <a-form layout="vertical" :model="replyForm">
        <a-form-item label="本次排查情况" required>
          <a-textarea
            v-model:value="replyForm.content"
            :rows="8"
            placeholder="写清本次检查过程、已确认的问题、临时处理结果，以及后续还需要谁继续处理。"
          />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="记录可见范围">
              <a-radio-group v-model:value="replyForm.visibleScope">
                <a-radio value="public">客户可见</a-radio>
                <a-radio value="internal">仅内部留痕</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="处理附件">
              <a-upload :showUploadList="false" :customRequest="uploadReplyFile">
                <a-button>上传附件</a-button>
              </a-upload>
            </a-form-item>
          </a-col>
        </a-row>

        <a-list v-if="replyFiles.length" size="small" :data-source="replyFiles" class="reply-file-list">
          <template #renderItem="{ item }">
            <a-list-item>{{ item.fileName }}（{{ formatFileSize(item.fileSize) }}）</a-list-item>
          </template>
        </a-list>

        <div class="form-submit-bar">
          <a-button type="primary" size="large" @click="submitReply">保存排查记录</a-button>
        </div>
      </a-form>
    </a-card>

    <a-row v-if="canTransfer || canResolve" :gutter="[16, 16]">
      <a-col v-if="canTransfer" :xs="24" :md="12">
        <a-card :bordered="false" class="section-card action-card">
          <template #title>转派工单</template>
          <a-form layout="vertical" :model="transferForm">
            <a-form-item label="下一任处理人">
              <a-select
                v-model:value="transferForm.handlerId"
                show-search
                allow-clear
                placeholder="选择下一任运维人员"
                :filter-option="filterHandlerOption"
                @search="fetchHandlerOptions"
              >
                <a-select-option v-for="item in handlerOptions" :key="item.id" :value="item.id">
                  {{ item.nickname || item.username }}（{{ item.roleName || item.roleKey || '未配置角色' }}）
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="转派原因">
              <a-textarea v-model:value="transferForm.reason" :rows="4" placeholder="写清当前排查情况和希望下一任继续跟进的方向。" />
            </a-form-item>
            <div class="action-submit">
              <a-button @click="submitTransfer">转派工单</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>

      <a-col v-if="canResolve" :xs="24" :md="12">
        <a-card :bordered="false" class="section-card action-card">
          <template #title>解决闭环</template>
          <a-form layout="vertical" :model="resolveForm">
            <a-form-item label="最终解决方案">
              <a-textarea v-model:value="resolveForm.solution" :rows="6" placeholder="填写最终解决方案，后续可沉淀为知识库内容。" />
            </a-form-item>
            <div class="action-submit">
              <a-button type="primary" @click="submitResolve">标记为已解决</a-button>
            </div>
          </a-form>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="section-card">
      <template #title>流程留痕</template>
      <ticket-flow-stream :flows="flows" :action-text="actionText" />
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { ticketApi } from '@/api/ticket'
import { buildFilePreviewUrl } from '@/utils/file'
import { getCurrentUserId, hasAnyRole } from '@/utils/auth'
import { hasAnyPermission } from '@/utils/permission'
import { createUploadRequest, formatFileSize } from '@/utils/upload'
import { formatDateTime } from '@/utils/datetime'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'
import TicketFlowStream from './components/TicketFlowStream.vue'

const route = useRoute()
const router = useRouter()
const ticket = ref(null)
const flows = ref([])
const files = ref([])
const replyFiles = ref([])
const handlerOptions = ref([])
const statusOptions = ref([])
const priorityOptions = ref([])
const categoryOptions = ref([])
const actionOptions = ref([])
const aiLoading = ref(false)
const aiSuggestion = ref(null)

const currentUserId = computed(() => getCurrentUserId())
const managerMode = computed(() => hasAnyRole(['admin', 'system_admin', 'supervisor']))

const replyForm = reactive({ ticketId: null, content: '', visibleScope: 'internal', fileIds: [] })
const transferForm = reactive({ handlerId: null, reason: '' })
const resolveForm = reactive({ ticketId: null, solution: '', fileIds: [] })

const aiAssistSummary = computed(() => {
  if (aiSuggestion.value?.summary) {
    return aiSuggestion.value.summary
  }
  if (!ticket.value) {
    return ''
  }
  const category = categoryText(ticket.value.category)
  return `${category} / ${priorityText(ticket.value.priority)}，建议先确认影响范围、复现步骤和最近变更。`
})

const buildLocalHints = () => {
  const description = ticket.value?.description || ''
  const hints = [
    {
      title: '先定范围',
      content: '确认影响人数、业务系统、开始时间和是否持续复现。'
    },
    {
      title: '留好证据',
      content: '补齐截图、报错文本、日志时间点和已尝试操作。'
    }
  ]
  if (/(网络|vpn|网关|超时|ping|延迟)/i.test(description) || ticket.value?.category === 'network') {
    hints.push({ title: '网络路径', content: '检查 DNS、网关、VPN、链路延迟和同网段对比结果。' })
  }
  if (/(权限|账号|角色|登录)/i.test(description) || ticket.value?.category === 'account') {
    hints.push({ title: '权限链路', content: '核对账号状态、角色菜单、数据范围和最近授权变更。' })
  }
  if (/(服务器|CPU|内存|磁盘|宕机)/i.test(description) || ticket.value?.category === 'server') {
    hints.push({ title: '资源水位', content: '检查 CPU、内存、磁盘、服务进程和最近发布记录。' })
  }
  if (/(数据|同步|报表|统计)/i.test(description) || ticket.value?.category === 'data') {
    hints.push({ title: '数据口径', content: '确认源表、同步任务、统计时间窗和异常样本。' })
  }
  return hints.slice(0, 4)
}

const aiHints = computed(() => {
  const suggestions = aiSuggestion.value?.suggestions || []
  if (suggestions.length) {
    return suggestions.slice(0, 4).map((content, index) => {
      const parts = String(content).split('：')
      return {
        title: parts.length > 1 ? parts[0] : `建议 ${index + 1}`,
        content: parts.length > 1 ? parts.slice(1).join('：') : content
      }
    })
  }
  return buildLocalHints()
})

const aiKnowledgeCandidates = computed(() => aiSuggestion.value?.knowledgeCandidates || [])

const canReply = computed(() => {
  if (!ticket.value) {
    return false
  }
  if (['closed', 'rejected', 'cancelled'].includes(ticket.value.status)) {
    return false
  }
  if (!hasAnyPermission(['ticket:reply'])) {
    return false
  }
  return managerMode.value || ticket.value.handlerId === currentUserId.value
})

const canTransfer = computed(() => {
  if (!ticket.value || !['processing', 'waiting_customer', 'transferred'].includes(ticket.value.status)) {
    return false
  }
  if (!hasAnyPermission(['ticket:transfer'])) {
    return false
  }
  return managerMode.value || ticket.value.handlerId === currentUserId.value
})

const canResolve = computed(() => {
  if (!ticket.value || !['processing', 'waiting_customer', 'transferred'].includes(ticket.value.status)) {
    return false
  }
  if (!hasAnyPermission(['ticket:resolve'])) {
    return false
  }
  return managerMode.value || ticket.value.handlerId === currentUserId.value
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
  replyForm.ticketId = ticket.value.id
  resolveForm.ticketId = ticket.value.id
  if (canReply.value) {
    fetchAiDiagnose()
  }
}

/**
 * 修改时间：2026-07-05
 * 功能说明：调用后端 AI/Agent 预留接口生成处理建议；失败时保持本地规则建议。
 * 入参：无，读取当前工单详情。
 * 出参：无，刷新 aiSuggestion。
 * 异常场景：接口异常时清空建议并继续显示本地规则。
 */
const fetchAiDiagnose = async () => {
  if (!ticket.value) {
    return
  }
  aiLoading.value = true
  try {
    const res = await ticketApi.aiDiagnose({
      ticketId: ticket.value.id,
      title: ticket.value.title,
      description: ticket.value.description,
      category: ticket.value.category,
      priority: ticket.value.priority
    })
    aiSuggestion.value = res.data || null
  } catch {
    aiSuggestion.value = null
  } finally {
    aiLoading.value = false
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

const submitReply = async () => {
  if (!replyForm.content.trim()) {
    message.warning('请先填写本次排查情况')
    return
  }
  await ticketApi.reply(replyForm)
  message.success('排查记录已保存')
  replyForm.content = ''
  replyForm.fileIds = []
  replyFiles.value = []
  fetchDetail()
}

const submitTransfer = async () => {
  if (!replyForm.content.trim()) {
    message.warning('转派前必须先写清当前排查情况')
    return
  }
  if (!transferForm.handlerId || !transferForm.reason.trim()) {
    message.warning('请选择下一任处理人并填写转派原因')
    return
  }
  await ticketApi.reply(replyForm)
  await ticketApi.transfer({ ticketId: ticket.value.id, handlerId: transferForm.handlerId, reason: transferForm.reason })
  message.success('工单已转派')
  replyForm.content = ''
  replyForm.fileIds = []
  replyFiles.value = []
  transferForm.handlerId = null
  transferForm.reason = ''
  fetchDetail()
}

const submitResolve = async () => {
  if (!resolveForm.solution.trim()) {
    message.warning('请填写最终解决方案')
    return
  }
  await ticketApi.resolve(resolveForm)
  message.success('工单已标记为解决')
  resolveForm.solution = ''
  fetchDetail()
}

const appendAiChecklist = () => {
  const content = [
    replyForm.content.trim(),
    '【AI 排查清单】',
    ...aiHints.value.map((item, index) => `${index + 1}. ${item.title}：${item.content}`)
  ].filter(Boolean).join('\n')
  replyForm.content = content
}

const draftAiSolution = () => {
  const actions = aiSuggestion.value?.nextActions || []
  resolveForm.solution = [
    resolveForm.solution.trim(),
    '【处理结论】',
    '1. 根因：',
    '2. 处理步骤：',
    '3. 验证结果：',
    '4. 后续预防：',
    actions.length ? `【建议动作】\n${actions.map((item, index) => `${index + 1}. ${item}`).join('\n')}` : ''
  ].filter(Boolean).join('\n')
}

const uploadReplyFile = createUploadRequest(file => {
  replyForm.fileIds.push(file.id)
  replyFiles.value.push(file)
  message.success('附件上传成功')
})

const statusText = value => getOptionLabel(statusOptions.value, value, value || '-')
const statusColor = value => getOptionColor(statusOptions.value, value, 'default')
const priorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const priorityColor = value => getOptionColor(priorityOptions.value, value, 'default')
const categoryText = value => getOptionLabel(categoryOptions.value, value, value || '-')
const actionText = value => getOptionLabel(actionOptions.value, value, value)

onMounted(async () => {
  await fetchMeta()
  await fetchDetail()
  fetchHandlerOptions()
})
</script>

<style scoped>
.ticket-process-page {
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

.section-card {
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.ai-assist-card {
  border: 1px solid #dbeafe;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.96), rgba(255, 255, 255, 0.98)),
    repeating-linear-gradient(135deg, rgba(37, 99, 235, 0.05), rgba(37, 99, 235, 0.05) 1px, transparent 1px, transparent 8px);
}

.ai-assist-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.ai-assist-card__head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.ai-assist-card__head p {
  margin: 6px 0 0;
  color: #475569;
  line-height: 1.6;
}

.ai-hint-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.ai-hint-item {
  display: grid;
  gap: 4px;
  min-height: 86px;
  padding: 12px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
}

.ai-hint-item strong {
  color: #1d4ed8;
  font-size: 13px;
}

.ai-hint-item span {
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}

.ai-knowledge-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(59, 130, 246, 0.16);
}

.ai-knowledge-strip strong {
  color: #1e3a8a;
  font-size: 13px;
}

.ticket-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.ticket-topbar__title h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.ticket-topbar__title p {
  margin: 8px 0 0;
  color: #64748b;
}

.ticket-topbar__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.problem-box {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  color: #172033;
  line-height: 1.8;
  white-space: pre-wrap;
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

.reply-file-list {
  margin-top: 4px;
}

.form-submit-bar,
.action-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .ticket-topbar {
    flex-direction: column;
  }

  .ai-assist-card__head {
    flex-direction: column;
  }

  .ai-hint-grid {
    grid-template-columns: 1fr;
  }
}
</style>
