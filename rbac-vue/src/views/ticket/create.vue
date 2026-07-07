<template>
  <div class="ticket-create-page">
    <a-button type="link" class="back-link" @click="router.push(returnPath)">
      <template #icon><arrow-left-outlined /></template>
      返回我的工单
    </a-button>

    <a-card :bordered="false" class="section-card ai-card">
      <div class="ai-card__head">
        <div class="ai-card__copy">
          <h2>AI 辅助整理</h2>
          <p>先粘贴客户原话，系统会通过后端 AI/Agent 预留接口整理标题、问题类型和描述骨架。</p>
        </div>
        <a-space wrap>
          <a-button type="primary" :loading="aiLoading" @click="draftFromText">按内容整理</a-button>
          <a-button @click="aiText = ''">清空</a-button>
        </a-space>
      </div>
      <a-textarea
        v-model:value="aiText"
        :rows="5"
        placeholder="例如：今天 15 点之后 ERP 系统无法登录，销售部多人提示网关超时，切换网络和重试后仍然无效。"
      />
      <div class="ai-approval-strip">
        <div>
          <strong>{{ approvalPreview.title }}</strong>
          <span>{{ approvalPreview.desc }}</span>
        </div>
        <a-tag :color="approvalPreview.color">{{ approvalPreview.tag }}</a-tag>
      </div>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="16">
        <a-card :bordered="false" class="section-card">
          <template #title>工单内容</template>
          <a-form layout="vertical" :model="form">
            <a-row :gutter="16">
              <a-col :xs="24" :md="12">
                <a-form-item label="所属系统" required>
                  <a-select v-model:value="form.systemCode" placeholder="请选择问题所属系统">
                    <a-select-option v-for="item in systemOptions" :key="item.code" :value="item.code">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="优先级">
                  <a-select v-model:value="form.priority" placeholder="请选择优先级">
                    <a-select-option v-for="item in priorityOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="问题类型">
              <a-select
                v-model:value="form.issueTypes"
                mode="multiple"
                allow-clear
                :max-tag-count="3"
                placeholder="可多选，例如：系统问题、网络问题、服务器问题"
                :options="issueTypeOptions"
              />
              <div class="field-tip">问题类型用于统计、知识沉淀与后续 Agent 分类，不确定时可以多选。</div>
            </a-form-item>

            <a-form-item label="工单标题" required>
              <a-input
                v-model:value="form.title"
                allow-clear
                placeholder="一句话概括核心问题，例如：ERP 登录网关超时"
              />
            </a-form-item>

            <a-row :gutter="16">
              <a-col :xs="24" :md="12">
                <a-form-item label="影响范围">
                  <a-input
                    v-model:value="impactScope"
                    allow-clear
                    placeholder="例如：销售部 8 人无法登录，影响开单"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="已尝试操作">
                  <a-input
                    v-model:value="triedActions"
                    allow-clear
                    placeholder="例如：切换网络、重启浏览器后仍无效"
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="问题描述" required>
              <a-textarea
                v-model:value="form.description"
                :rows="9"
                placeholder="建议按 1. 问题现象 2. 影响范围 3. 已尝试操作 4. 期望结果 分点描述。"
              />
            </a-form-item>
          </a-form>
        </a-card>

        <a-card :bordered="false" class="section-card">
          <template #title>联系信息</template>
          <a-form layout="vertical" :model="form">
            <a-row :gutter="16">
              <a-col :xs="24" :md="12">
                <a-form-item label="联系电话">
                  <a-input v-model:value="form.contactPhone" allow-clear placeholder="便于运维回访" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="联系邮箱">
                  <a-input v-model:value="form.contactEmail" allow-clear placeholder="可接收处理反馈" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="section-card upload-card">
          <template #title>附件与素材</template>
          <div class="upload-toolbar">
            <a-upload
              multiple
              :showUploadList="false"
              :customRequest="uploadCreateFile"
              accept=".jpg,.jpeg,.png,.gif,.webp,.svg,.bmp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.log,.json,.xml,.csv,.zip,.rar,.mp4,.webm,.mov,.avi"
            >
              <a-button>上传附件与素材</a-button>
            </a-upload>
            <span class="card-tip">支持图片、Markdown、Office 文档、压缩包和视频。</span>
          </div>

          <div class="attachment-groups">
            <section v-for="group in attachmentGroups" :key="group.key">
              <div class="group-title">
                <h3>{{ group.title }}</h3>
                <span>{{ group.files.length }}</span>
              </div>

              <a-empty v-if="!group.files.length" :description="`暂无${group.title}`" />

              <div v-else-if="group.key === 'image'" class="preview-grid">
                <article v-for="file in group.files" :key="file.id || file.fileName" class="preview-card">
                  <img
                    v-if="inlinePreviewUrl(file)"
                    :src="inlinePreviewUrl(file)"
                    :alt="file.fileName"
                    loading="lazy"
                    @click="openPreview(file)"
                  />
                  <div v-else class="preview-fallback">图片加载中</div>
                  <div class="preview-meta">
                    <strong :title="file.fileName">{{ file.fileName }}</strong>
                    <small>{{ formatFileSize(file.fileSize) }}</small>
                    <a class="danger-link" @click="removeFile(file)">移除</a>
                  </div>
                </article>
              </div>

              <div v-else-if="group.key === 'video'" class="preview-grid">
                <article v-for="file in group.files" :key="file.id || file.fileName" class="preview-card">
                  <video v-if="inlinePreviewUrl(file)" class="video-preview" :src="inlinePreviewUrl(file)" controls preload="metadata" />
                  <div v-else class="preview-fallback">视频加载中</div>
                  <div class="preview-meta">
                    <strong :title="file.fileName">{{ file.fileName }}</strong>
                    <small>{{ formatFileSize(file.fileSize) }}</small>
                    <a class="danger-link" @click="removeFile(file)">移除</a>
                  </div>
                </article>
              </div>

              <div v-else class="file-stack">
                <article v-for="file in group.files" :key="file.id || file.fileName" class="file-item">
                  <div class="file-item__main">
                    <strong>{{ file.fileName }}</strong>
                    <small>{{ formatFileSize(file.fileSize) }}</small>
                  </div>
                  <a-space>
                    <a @click="openPreview(file)">预览</a>
                    <a class="danger-link" @click="removeFile(file)">移除</a>
                  </a-space>
                </article>
              </div>
            </section>
          </div>
        </a-card>

        <a-card :bordered="false" class="section-card tips-card">
          <template #title>填写建议</template>
          <ul class="tips-list">
            <li>先选系统，再选问题类型，避免运维接单后还要反推到底是哪个系统。</li>
            <li>标题只写一个核心问题，详细排查背景放到问题描述里。</li>
            <li>影响范围和已尝试操作尽量补齐，后续转派时更容易留痕。</li>
            <li>图片适合放截图，文档适合放日志或说明，视频适合录屏复现。</li>
          </ul>
        </a-card>
      </a-col>
    </a-row>

    <div class="footer-actions">
      <a-space>
        <a-button class="footer-button" size="large" @click="router.push(returnPath)">取消</a-button>
        <a-button class="footer-button" size="large" :loading="draftSubmitting" @click="saveDraft">保存草稿</a-button>
        <a-button class="footer-button" type="primary" size="large" :loading="submitting" @click="submitTicket">提交工单</a-button>
      </a-space>
    </div>

    <file-preview v-model:open="previewVisible" :record="previewRecord" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import FilePreview from '@/components/file-preview/index.vue'
import { ticketApi } from '@/api/ticket'
import { hasAnyPermission } from '@/utils/permission'
import { getAuthedPreviewUrl, revokeAuthedPreviewUrl } from '@/utils/file-preview'
import { getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'
import { createUploadRequest, formatFileSize } from '@/utils/upload'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const draftSubmitting = ref(false)
const aiLoading = ref(false)
const aiText = ref('')
const impactScope = ref('')
const triedActions = ref('')
const createFiles = ref([])
const previewVisible = ref(false)
const previewRecord = ref(null)
const inlinePreviewUrls = ref({})
const draftId = ref(null)
const systemOptions = ref([])
const priorityOptions = ref([])
const issueTypeOptions = ref([])
const returnPath = computed(() => {
  if (hasAnyPermission(['ticket:my:list'])) {
    return '/ticket/my'
  }
  if (hasAnyPermission(['ticket:list'])) {
    return '/ticket/workbench'
  }
  return '/dashboard'
})

const form = reactive({
  title: '',
  description: '',
  category: 'system',
  issueTypes: ['system'],
  priority: 'normal',
  systemCode: undefined,
  contactPhone: '',
  contactEmail: '',
  fileIds: []
})

const approvalPreview = computed(() => {
  const issueTypes = Array.isArray(form.issueTypes) ? form.issueTypes : []
  const highRisk = form.priority === 'urgent'
    || form.priority === 'high'
    || issueTypes.some(item => ['account', 'data', 'server'].includes(item))
  if (highRisk) {
    return {
      title: '审批预判：需要重点审核',
      desc: '建议补齐影响范围、授权依据、回滚方式和期望完成时间。',
      tag: '高关注',
      color: 'red'
    }
  }
  if (!form.systemCode || !issueTypes.length) {
    return {
      title: '审批预判：信息还不完整',
      desc: '先选所属系统和问题类型，审批人更容易判断受理路径。',
      tag: '待补齐',
      color: 'gold'
    }
  }
  return {
    title: '审批预判：可走普通受理',
    desc: '当前信息适合进入标准待审批流程。',
    tag: '普通',
    color: 'blue'
  }
})

const getSuffix = file => {
  const suffix = file?.fileSuffix || ''
  if (suffix) {
    return suffix.toLowerCase()
  }
  const name = file?.fileName || ''
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index).toLowerCase() : ''
}

const getFileKind = file => {
  const suffix = getSuffix(file)
  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'].includes(suffix)) {
    return 'image'
  }
  if (['.mp4', '.webm', '.mov', '.avi'].includes(suffix)) {
    return 'video'
  }
  return 'document'
}

const attachmentGroups = computed(() => [
  { key: 'image', title: '图片素材', files: createFiles.value.filter(file => getFileKind(file) === 'image') },
  { key: 'document', title: '文档附件', files: createFiles.value.filter(file => getFileKind(file) === 'document') },
  { key: 'video', title: '视频附件', files: createFiles.value.filter(file => getFileKind(file) === 'video') }
])

const inlinePreviewUrl = file => inlinePreviewUrls.value[file.id] || ''

const findSystemName = code => systemOptions.value.find(item => item.code === code)?.name || code || '未选择'
const findIssueLabel = code => getOptionLabel(issueTypeOptions.value, code, code)

const parseIssueCodesByLabel = raw => {
  if (!raw) {
    return []
  }
  return raw
    .split(/[、,，]/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(label => issueTypeOptions.value.find(option => option.label === label || option.value === label)?.value)
    .filter(Boolean)
}

const buildDescription = () => {
  const issueLabels = (form.issueTypes || []).map(findIssueLabel).join('、') || '其他问题'
  return [
    `【所属系统】${findSystemName(form.systemCode)}`,
    `【问题类型】${issueLabels}`,
    `【影响范围】${impactScope.value || '未填写'}`,
    `【已尝试操作】${triedActions.value || '未填写'}`,
    '',
    form.description.trim()
  ].join('\n')
}

const hydrateDescription = rawDescription => {
  const lines = String(rawDescription || '').split('\n')
  const meta = {
    systemName: '',
    issueLabels: '',
    impact: '',
    tried: ''
  }
  const contentLines = []

  lines.forEach(line => {
    if (line.startsWith('【所属系统】')) {
      meta.systemName = line.replace('【所属系统】', '').trim()
      return
    }
    if (line.startsWith('【问题类型】')) {
      meta.issueLabels = line.replace('【问题类型】', '').trim()
      return
    }
    if (line.startsWith('【影响范围】')) {
      meta.impact = line.replace('【影响范围】', '').trim()
      return
    }
    if (line.startsWith('【已尝试操作】')) {
      meta.tried = line.replace('【已尝试操作】', '').trim()
      return
    }
    contentLines.push(line)
  })

  const normalizedSystem = systemOptions.value.find(item => item.name === meta.systemName || item.code === meta.systemName)
  form.systemCode = normalizedSystem?.code
  form.issueTypes = parseIssueCodesByLabel(meta.issueLabels)
  if (!form.issueTypes.length) {
    form.issueTypes = [form.category || 'system']
  }
  impactScope.value = meta.impact && meta.impact !== '未填写' ? meta.impact : ''
  triedActions.value = meta.tried && meta.tried !== '未填写' ? meta.tried : ''
  form.description = contentLines.join('\n').trim()
}

const normalizeSuggestedTypes = types => {
  const availableValues = new Set(issueTypeOptions.value.map(item => item.value))
  return (types || []).filter(item => availableValues.has(item))
}

const draftFromTextLocal = text => {
  const detectedTypes = new Set(Array.isArray(form.issueTypes) ? form.issueTypes : [])
  if (/(网络|vpn|网关|超时|ping|延迟)/i.test(text)) detectedTypes.add('network')
  if (/(服务器|服务不可用|cpu|内存|磁盘|宕机)/i.test(text)) detectedTypes.add('server')
  if (/(权限|账号|角色|登录)/i.test(text)) detectedTypes.add('account')
  if (/(数据|同步|缺失|报表|统计)/i.test(text)) detectedTypes.add('data')
  if (/(页面|流程|系统|菜单|报错)/i.test(text)) detectedTypes.add('system')
  if (!detectedTypes.size) detectedTypes.add('other')

  form.issueTypes = Array.from(detectedTypes)
  form.category = form.issueTypes[0] || 'other'

  if (!form.title) {
    form.title = text.length > 30 ? `${text.slice(0, 30)}...` : text
  }

  form.description = [
    `1. 问题现象：${text}`,
    `2. 影响范围：${impactScope.value || '待补充'}`,
    `3. 已尝试操作：${triedActions.value || '待补充'}`,
    '4. 期望结果：请运维协助定位并恢复业务'
  ].join('\n')
}

const applyAiSuggestion = (suggestion, text) => {
  const issueTypes = normalizeSuggestedTypes(suggestion.issueTypes)
  if (issueTypes.length) {
    form.issueTypes = issueTypes
    form.category = issueTypes[0]
  } else if (suggestion.category) {
    form.category = suggestion.category
  }
  if (!form.title && suggestion.title) {
    form.title = suggestion.title
  }
  if (form.priority === 'normal' && suggestion.priority) {
    form.priority = suggestion.priority
  }
  if (!impactScope.value && suggestion.impactScope) {
    impactScope.value = suggestion.impactScope
  }
  if (!triedActions.value && suggestion.triedActions) {
    triedActions.value = suggestion.triedActions
  }
  form.description = suggestion.description || form.description || [
    `1. 问题现象：${text}`,
    `2. 影响范围：${impactScope.value || '待补充'}`,
    `3. 已尝试操作：${triedActions.value || '待补充'}`,
    '4. 期望结果：请运维协助定位并恢复业务'
  ].join('\n')
}

/**
 * 修改时间：2026-07-05
 * 功能说明：根据客户原话调用后端 AI/Agent 预留接口整理工单；接口不可用时使用本地规则兜底。
 * 入参：无，读取 aiText、表单分类、系统和优先级。
 * 出参：无，直接回填当前表单字段。
 * 异常场景：输入为空时仅提示；AI 接口异常时降级为本地整理。
 */
const draftFromText = async () => {
  const text = aiText.value.trim()
  if (!text) {
    message.warning('请先粘贴客户原始描述')
    return
  }

  aiLoading.value = true
  try {
    const res = await ticketApi.aiPrecheck({
      rawText: text,
      title: form.title,
      category: form.category,
      priority: form.priority,
      systemCode: form.systemCode,
      issueTypes: form.issueTypes
    })
    applyAiSuggestion(res.data || {}, text)
    message.success('整理建议已回填')
  } catch {
    draftFromTextLocal(text)
    message.info('已使用本地规则整理')
  } finally {
    aiLoading.value = false
  }
}

const cacheInlinePreview = async file => {
  if (!file?.id || !['image', 'video'].includes(getFileKind(file))) {
    return
  }
  try {
    const url = await getAuthedPreviewUrl(file.id)
    inlinePreviewUrls.value = {
      ...inlinePreviewUrls.value,
      [file.id]: url
    }
  } catch {
    inlinePreviewUrls.value = {
      ...inlinePreviewUrls.value,
      [file.id]: ''
    }
  }
}

const loadExistingFiles = async files => {
  createFiles.value = files || []
  form.fileIds = createFiles.value.map(item => item.id).filter(Boolean)
  await Promise.all(createFiles.value.map(cacheInlinePreview))
}

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  systemOptions.value = (meta.systemOptions || []).map(item => ({
    code: item.code || item.value,
    name: item.name || item.label
  }))
  priorityOptions.value = meta.priorityOptions || []
  issueTypeOptions.value = (meta.categoryOptions || []).filter(item => item.value !== 'general')
}

const fetchDraftDetail = async () => {
  const id = Number(route.query.id)
  if (!Number.isFinite(id) || id <= 0) {
    return
  }
  const res = await ticketApi.detail(id)
  const ticket = res.data.ticket
  draftId.value = ticket.id
  form.title = ticket.title || ''
  form.category = ticket.category || 'system'
  form.priority = ticket.priority || 'normal'
  form.contactPhone = ticket.creatorPhone || ''
  form.contactEmail = ticket.creatorEmail || ''
  hydrateDescription(ticket.description || '')
  form.systemCode = ticket.systemCode || form.systemCode
  await loadExistingFiles(res.data.files || [])
}

const submitByMode = async draftMode => {
  const payload = {
    id: draftId.value,
    title: form.title.trim(),
    category: (form.issueTypes && form.issueTypes[0]) || form.category || 'other',
    systemCode: form.systemCode,
    priority: form.priority,
    contactPhone: form.contactPhone,
    contactEmail: form.contactEmail,
    fileIds: form.fileIds,
    draft: draftMode,
    description: buildDescription()
  }
  const res = await ticketApi.create(payload)
  draftId.value = res.data
}

const saveDraft = async () => {
  draftSubmitting.value = true
  try {
    await submitByMode(true)
    message.success('草稿已保存')
    router.push(returnPath.value)
  } finally {
    draftSubmitting.value = false
  }
}

/**
 * 修改时间：2026-07-02
 * 功能说明：提交正式工单，并把系统、问题类型、影响范围等结构化信息写入工单正文。
 * 入参：无，读取当前表单状态。
 * 出参：无，提交成功后返回“我的工单”。
 * 异常场景：所属系统、标题或问题描述缺失时阻止提交并提示。
 */
const submitTicket = async () => {
  if (!form.systemCode || !form.title.trim() || !form.description.trim()) {
    message.warning('请至少填写所属系统、工单标题和问题描述')
    return
  }
  submitting.value = true
  try {
    await submitByMode(false)
    message.success('工单已提交')
    router.push(returnPath.value)
  } finally {
    submitting.value = false
  }
}

const uploadCreateFile = createUploadRequest(file => {
  form.fileIds.push(file.id)
  createFiles.value.push(file)
  cacheInlinePreview(file)
  message.success('附件上传成功')
})

const removeFile = file => {
  createFiles.value = createFiles.value.filter(item => item !== file)
  form.fileIds = form.fileIds.filter(id => id !== file.id)
  revokeAuthedPreviewUrl(file.id)
  const nextUrls = { ...inlinePreviewUrls.value }
  delete nextUrls[file.id]
  inlinePreviewUrls.value = nextUrls
}

const openPreview = file => {
  previewRecord.value = file
  previewVisible.value = true
}

onMounted(async () => {
  await fetchMeta()
  await fetchDraftDetail()
})

onBeforeUnmount(() => {
  Object.keys(inlinePreviewUrls.value).forEach(fileId => revokeAuthedPreviewUrl(fileId))
})
</script>

<style scoped>
.ticket-create-page {
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
  border-radius: 20px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.section-card + .section-card {
  margin-top: 16px;
}

.ai-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.ai-card__copy {
  display: grid;
  gap: 6px;
}

.ai-card__copy h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

.ai-card__copy p,
.card-tip,
.field-tip {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
}

.ai-approval-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background:
    linear-gradient(135deg, #f8fbff, #ffffff),
    repeating-linear-gradient(90deg, rgba(37, 99, 235, 0.05), rgba(37, 99, 235, 0.05) 1px, transparent 1px, transparent 8px);
}

.ai-approval-strip div {
  display: grid;
  gap: 4px;
}

.ai-approval-strip strong {
  color: #0f172a;
  font-size: 13px;
}

.ai-approval-strip span {
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.field-tip {
  margin-top: 8px;
}

.upload-toolbar {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.attachment-groups {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.group-title h3 {
  margin: 0;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.group-title span {
  color: #1677ff;
  font-size: 13px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-card {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
}

.preview-card img,
.video-preview {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  background: #e5e7eb;
}

.preview-card img {
  cursor: zoom-in;
}

.preview-fallback {
  display: grid;
  width: 100%;
  aspect-ratio: 16 / 10;
  place-items: center;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
}

.preview-meta {
  display: grid;
  gap: 4px;
  padding: 10px 12px 12px;
}

.preview-meta strong,
.file-item__main strong {
  overflow: hidden;
  color: #172033;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-meta small,
.file-item__main small {
  color: #6b7280;
}

.file-stack {
  display: grid;
  gap: 10px;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.file-item__main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.danger-link {
  color: #dc2626;
}

.tips-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.7;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  padding: 4px 2px 0;
}

.footer-button {
  min-width: 128px;
}

@media (max-width: 1200px) {
  .preview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ai-card__head,
  .ai-approval-strip,
  .footer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .preview-grid {
    grid-template-columns: 1fr;
  }

  .footer-button {
    width: 100%;
  }
}
</style>
