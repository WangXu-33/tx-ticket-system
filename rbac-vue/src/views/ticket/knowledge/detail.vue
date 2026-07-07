<template>
  <div class="page-container knowledge-detail-page">
    <a-button type="link" class="back-link" @click="router.back()">
      <template #icon><arrow-left-outlined /></template>
      返回
    </a-button>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="16">
        <a-card :bordered="false" class="detail-card">
          <template #title>
            <a-space>
              <span>知识内容</span>
              <a-tag :color="knowledgeStatusColor(form.status)">{{ knowledgeStatusText(form.status) }}</a-tag>
            </a-space>
          </template>

          <a-form layout="vertical" :model="form">
            <a-form-item label="标准问题标题" required>
              <a-input v-model:value="form.title" placeholder="例如：ERP 登录后提示账号无权限" />
            </a-form-item>

            <a-row :gutter="12">
              <a-col :xs="24" :md="10">
                <a-form-item label="问题类型" extra="用于统计和筛选，来自工单字典。">
                  <a-select v-model:value="form.category" placeholder="请选择问题类型">
                    <a-select-option v-for="item in categoryOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="14">
                <a-form-item label="检索标签" extra="用于后续 Agent 检索，建议填系统名、报错词、模块名。">
                  <a-select
                    v-model:value="tagValues"
                    mode="tags"
                    :token-separators="[',', '，']"
                    placeholder="输入后回车，例如 VPN、登录失败、库存同步"
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="问题现象">
              <a-textarea v-model:value="form.phenomenon" :rows="4" placeholder="客户能看到的现象、报错信息、触发步骤。" />
            </a-form-item>
            <a-form-item label="原因分析">
              <a-textarea v-model:value="form.causeAnalysis" :rows="4" placeholder="运维排查后的根因，不确定时写排除过程。" />
            </a-form-item>
            <a-form-item label="解决步骤" required>
              <a-textarea v-model:value="form.solutionSteps" :rows="8" placeholder="按 1、2、3 写清操作步骤，方便后续直接复用。" />
            </a-form-item>
            <a-form-item label="适用范围">
              <a-textarea v-model:value="form.applicableScope" :rows="3" placeholder="适用系统、版本、客户环境、限制条件。" />
            </a-form-item>

            <div class="section-panel">
              <div class="section-panel__head">
                <strong>附件材料</strong>
                <a-upload :showUploadList="false" :customRequest="uploadKnowledgeFile">
                  <a-button>上传附件</a-button>
                </a-upload>
              </div>
              <a-empty v-if="!files.length" description="暂无附件" />
              <a-list v-else size="small" :data-source="files" class="file-list">
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
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="detail-card relation-card">
          <template #title>来源与合并</template>

          <div class="workflow-strip">
            <span>发现重复问题</span>
            <i />
            <span>合并到主工单</span>
            <i />
            <span>沉淀知识库</span>
          </div>

          <a-form layout="vertical" :model="form" class="relation-form">
            <a-form-item label="首个来源工单" extra="生成这篇知识的第一张工单，用于追溯原始排查过程。">
              <a-input-number v-model:value="form.sourceTicketId" style="width: 100%" placeholder="填写工单 ID" />
            </a-form-item>
            <ticket-mini-card v-if="sourceTicket" :ticket="sourceTicket" @open="goTicket" />
            <a-empty v-else class="compact-empty" description="暂无来源工单" />

            <a-form-item label="相同问题工单" extra="同类问题只做关联；需要关闭重复单时使用下面的合并动作。">
              <a-input v-model:value="linkedTicketText" placeholder="多个工单 ID 用英文逗号分隔，例如 12,18,21" />
            </a-form-item>
          </a-form>

          <div class="linked-list">
            <div class="linked-list__title">
              <strong>已关联工单</strong>
              <span>{{ linkTickets.length }}</span>
            </div>
            <a-empty v-if="!linkTickets.length" class="compact-empty" description="暂无关联工单" />
            <template v-else>
              <ticket-mini-card
                v-for="item in linkTickets"
                :key="`${item.id}-${item.linkType}`"
                :ticket="item"
                :link-type-text="linkTypeText(item.linkType)"
                @open="goTicket"
              />
            </template>
          </div>

          <div class="merge-panel">
            <div class="merge-panel__title">
              <strong>合并重复工单</strong>
              <span>重复单会关闭并写入流程留痕</span>
            </div>
            <a-form layout="vertical" :model="mergeForm">
              <a-form-item label="主工单 ID">
                <a-input-number v-model:value="mergeForm.mainTicketId" style="width: 100%" placeholder="保留继续处理的工单" />
              </a-form-item>
              <a-form-item label="重复工单 ID">
                <a-input v-model:value="mergeDuplicateText" placeholder="多个 ID 用英文逗号分隔" />
              </a-form-item>
              <a-form-item label="合并说明">
                <a-textarea v-model:value="mergeForm.reason" :rows="3" placeholder="说明为什么判断为同一个问题。" />
              </a-form-item>
              <a-button
                block
                type="primary"
                :disabled="!canMergeTicket"
                :loading="mergeLoading"
                @click="mergeTickets"
              >
                合并重复工单
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <div class="footer-actions">
      <a-space>
        <a-button class="footer-button" size="large" @click="router.back()">取消</a-button>
        <a-button v-if="canSubmitReview" class="footer-button" size="large" :loading="reviewLoading" @click="submitReview">提交审核</a-button>
        <a-button v-if="canRejectReview" class="footer-button" size="large" danger :loading="reviewLoading" @click="rejectReviewVisible = true">审核驳回</a-button>
        <a-button v-if="canPublishKnowledge" class="footer-button" size="large" :loading="reviewLoading" @click="publishKnowledge">审核通过并发布</a-button>
        <a-button v-if="canSaveKnowledge" class="footer-button" type="primary" size="large" @click="save">保存</a-button>
      </a-space>
    </div>

    <a-modal
      v-model:open="rejectReviewVisible"
      title="审核驳回"
      :confirm-loading="reviewLoading"
      @ok="rejectReview"
    >
      <a-form layout="vertical" :model="reviewForm">
        <a-form-item label="驳回原因" required>
          <a-textarea v-model:value="reviewForm.content" :rows="4" placeholder="写清需要补充或修正的内容" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { knowledgeApi, ticketApi } from '@/api/ticket'
import { buildFileDownloadUrl, buildFilePreviewUrl } from '@/utils/file'
import { hasAnyPermission } from '@/utils/permission'
import { loadTicketMeta } from '@/utils/ticket-meta'
import { createUploadRequest, formatFileSize } from '@/utils/upload'

const TicketMiniCard = defineComponent({
  name: 'TicketMiniCard',
  props: {
    ticket: {
      type: Object,
      required: true
    },
    linkTypeText: {
      type: String,
      default: ''
    }
  },
  emits: ['open'],
  setup(props, { emit }) {
    return () => h('button', {
      type: 'button',
      class: 'ticket-mini-card',
      onClick: () => emit('open', props.ticket.id)
    }, [
      h('span', { class: 'ticket-mini-card__no' }, props.ticket.ticketNo || `#${props.ticket.id}`),
      h('strong', props.ticket.title || '-'),
      h('span', { class: 'ticket-mini-card__meta' }, [
        props.ticket.status || '-',
        props.ticket.creatorName ? ` / ${props.ticket.creatorName}` : '',
        props.linkTypeText ? ` / ${props.linkTypeText}` : ''
      ].join(''))
    ])
  }
})

const route = useRoute()
const router = useRouter()
const files = ref([])
const linkedTicketText = ref('')
const mergeDuplicateText = ref('')
const tagValues = ref([])
const sourceTicket = ref(null)
const linkTickets = ref([])
const categoryOptions = ref([])
const mergeLoading = ref(false)
const reviewLoading = ref(false)
const rejectReviewVisible = ref(false)

const form = reactive({
  id: null,
  title: '',
  category: 'general',
  tags: '',
  phenomenon: '',
  causeAnalysis: '',
  solutionSteps: '',
  applicableScope: '',
  status: 'draft',
  sourceTicketId: null,
  linkedTicketIds: [],
  fileIds: []
})

const mergeForm = reactive({
  mainTicketId: null,
  reason: ''
})

const reviewForm = reactive({
  content: ''
})

const canMergeTicket = computed(() => hasAnyPermission(['ticket:assign']) && !!form.id)
const canSaveKnowledge = computed(() => hasAnyPermission(['knowledge:edit']) && (!form.id || ['draft', 'rejected'].includes(form.status)))
const canSubmitReview = computed(() => !!form.id && ['draft', 'rejected'].includes(form.status) && hasAnyPermission(['knowledge:edit']))
const canPublishKnowledge = computed(() => !!form.id && form.status === 'reviewing' && hasAnyPermission(['knowledge:publish']))
const canRejectReview = computed(() => !!form.id && form.status === 'reviewing' && hasAnyPermission(['knowledge:publish']))

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  categoryOptions.value = meta.categoryOptions || []
}

const fetchDetail = async () => {
  if (!route.params.id) return
  const res = await knowledgeApi.detail(route.params.id)
  Object.assign(form, res.data.article)
  files.value = res.data.files || []
  sourceTicket.value = res.data.sourceTicket || null
  linkTickets.value = res.data.linkTickets || []
  linkedTicketText.value = (res.data.links || []).map(item => item.ticketId).join(',')
  tagValues.value = parseTextList(form.tags)
  if (!mergeForm.mainTicketId) {
    mergeForm.mainTicketId = form.sourceTicketId || sourceTicket.value?.id || null
  }
}

const parseTextList = value => {
  if (!value) {
    return []
  }
  return String(value)
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

const parseIds = value => {
  return parseTextList(value)
    .map(item => Number(item))
    .filter(item => Number.isFinite(item) && item > 0)
}

const parseLinkedTicketIds = () => {
  const ids = parseIds(linkedTicketText.value)
  if (form.sourceTicketId && !ids.includes(form.sourceTicketId)) {
    ids.push(form.sourceTicketId)
  }
  return ids
}

const save = async () => {
  const payload = {
    ...form,
    tags: tagValues.value.join(','),
    linkedTicketIds: parseLinkedTicketIds()
  }
  await knowledgeApi.save(payload)
  message.success('知识库已保存')
  router.push('/ticket/knowledge')
}

const submitReview = async () => {
  reviewLoading.value = true
  try {
    await knowledgeApi.submitReview(form.id)
    message.success('已提交审核')
    await fetchDetail()
  } finally {
    reviewLoading.value = false
  }
}

const publishKnowledge = async () => {
  reviewLoading.value = true
  try {
    await knowledgeApi.publish(form.id)
    message.success('知识已发布')
    await fetchDetail()
  } finally {
    reviewLoading.value = false
  }
}

const rejectReview = async () => {
  if (!reviewForm.content.trim()) {
    message.warning('请填写审核驳回原因')
    return
  }
  reviewLoading.value = true
  try {
    await knowledgeApi.rejectReview(form.id, { content: reviewForm.content })
    message.success('审核已驳回')
    rejectReviewVisible.value = false
    reviewForm.content = ''
    await fetchDetail()
  } finally {
    reviewLoading.value = false
  }
}

const mergeTickets = async () => {
  const duplicateTicketIds = parseIds(mergeDuplicateText.value)
  if (!mergeForm.mainTicketId) {
    message.warning('请填写主工单 ID')
    return
  }
  if (!duplicateTicketIds.length) {
    message.warning('请填写重复工单 ID')
    return
  }
  mergeLoading.value = true
  try {
    await ticketApi.merge({
      mainTicketId: mergeForm.mainTicketId,
      duplicateTicketIds,
      reason: mergeForm.reason,
      articleId: form.id
    })
    message.success('重复工单已合并')
    mergeDuplicateText.value = ''
    mergeForm.reason = ''
    await fetchDetail()
  } finally {
    mergeLoading.value = false
  }
}

const uploadKnowledgeFile = createUploadRequest(file => {
  form.fileIds.push(file.id)
  files.value.push(file)
  message.success('附件上传成功')
})

const goTicket = id => router.push(`/ticket/detail/${id}`)
const linkTypeText = value => {
  const map = {
    source: '来源',
    manual: '同类问题',
    merged: '已合并',
    recommend: '推荐命中'
  }
  return map[value] || value || '关联'
}

const knowledgeStatusText = value => ({
  draft: '草稿',
  reviewing: '待审核',
  rejected: '审核驳回',
  published: '已发布',
  withdrawn: '已下架'
}[value] || value || '草稿')
const knowledgeStatusColor = value => ({
  draft: 'orange',
  reviewing: 'gold',
  rejected: 'red',
  published: 'green',
  withdrawn: 'default'
}[value] || 'default')

watch(() => form.sourceTicketId, value => {
  if (!mergeForm.mainTicketId && value) {
    mergeForm.mainTicketId = value
  }
})

onMounted(async () => {
  await fetchMeta()
  fetchDetail()
})
</script>

<style scoped>
.knowledge-detail-page {
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
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.section-panel {
  padding: 14px;
  border: 1px solid #e5edf5;
  border-radius: 8px;
  background: #fbfdff;
}

.section-panel__head,
.linked-list__title,
.merge-panel__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-panel__head strong,
.linked-list__title strong,
.merge-panel__title strong {
  color: #0f172a;
  font-size: 14px;
}

.linked-list__title span,
.merge-panel__title span {
  color: #64748b;
  font-size: 12px;
}

.relation-card :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workflow-strip {
  display: grid;
  grid-template-columns: 1fr 18px 1fr 18px 1fr;
  align-items: center;
  padding: 12px;
  border: 1px solid #e5edf5;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  text-align: center;
}

.workflow-strip i {
  height: 1px;
  background: #cbd5e1;
}

.relation-form {
  margin-bottom: 0;
}

.linked-list,
.merge-panel {
  padding-top: 14px;
  border-top: 1px solid #edf2f7;
}

:deep(.ticket-mini-card) {
  display: grid;
  width: 100%;
  gap: 4px;
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #e5edf5;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

:deep(.ticket-mini-card:hover) {
  border-color: #9cc7ff;
  background: #f8fbff;
}

:deep(.ticket-mini-card__no) {
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

:deep(.ticket-mini-card strong) {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ticket-mini-card__meta) {
  color: #64748b;
  font-size: 12px;
}

.compact-empty {
  margin: 4px 0 8px;
}

.file-list {
  margin-top: 10px;
}

.footer-actions {
  position: sticky;
  right: 0;
  bottom: 0;
  z-index: 5;
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
  background: linear-gradient(180deg, rgba(246, 248, 251, 0), #f6f8fb 36%);
}

.footer-button {
  min-width: 120px;
}

.muted {
  color: #64748b;
}

@media (max-width: 768px) {
  .workflow-strip {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .workflow-strip i {
    width: 1px;
    height: 12px;
    margin: 0 auto;
  }
}
</style>
