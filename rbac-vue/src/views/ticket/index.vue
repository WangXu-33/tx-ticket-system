<template>
  <div class="ticket-home-page">
    <a-card :bordered="false" class="hero-card">
      <div class="hero-card__main">
        <div class="hero-copy">
          <span class="hero-badge">头绪工单系统</span>
          <h1>工单首页</h1>
          <p>客户提单、运维接单、流程留痕和知识沉淀统一在一个工作台里完成。</p>
        </div>

        <div class="hero-metrics">
          <article v-for="item in metrics" :key="item.key" class="metric-panel">
            <div class="metric-panel__icon" :class="item.key">
              <component :is="item.icon" />
            </div>
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </article>
        </div>
      </div>
    </a-card>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="15">
        <a-card :bordered="false" class="section-card">
          <template #title>
            <div class="section-title">
              <span>最近工单</span>
              <a-button type="link" @click="goTicketList">查看全部</a-button>
            </div>
          </template>
          <a-table
            row-key="id"
            size="small"
            :columns="ticketColumns"
            :data-source="recentTickets"
            :loading="loadingTickets"
            :pagination="false"
            :scroll="{ x: 860 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'ticketNo'">
                <a @click="router.push(`/ticket/detail/${record.id}`)">{{ record.ticketNo || `#${record.id}` }}</a>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="ticketStatusColor(record.status)">{{ ticketStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'priority'">
                <a-tag :color="ticketPriorityColor(record.priority)">{{ ticketPriorityText(record.priority) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'category'">
                <a-tag>{{ ticketCategoryText(record.category) }}</a-tag>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!loadingTickets && recentTickets.length === 0" description="暂无工单，先提交一个问题吧。" />
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="9">
        <a-card :bordered="false" class="section-card">
          <template #title>快捷入口</template>
          <div class="action-list">
            <button v-for="item in quickActions" :key="item.path" type="button" @click="router.push(item.path)">
              <span :class="['action-icon', item.tone]">
                <component :is="item.icon" />
              </span>
              <span class="action-copy">
                <strong>{{ item.title }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <right-outlined class="action-arrow" />
            </button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="section-card">
          <template #title>
            <div class="section-title">
              <span>知识库沉淀</span>
              <a-button v-if="canViewKnowledge" type="link" @click="router.push('/ticket/knowledge')">进入知识库</a-button>
            </div>
          </template>
          <a-list v-if="canViewKnowledge" :data-source="knowledgeItems" :loading="loadingKnowledge" item-layout="horizontal">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a @click="router.push(`/ticket/knowledge/detail/${item.id}`)">{{ item.title }}</a>
                  </template>
                  <template #description>
                    {{ ticketCategoryText(item.category) || '通用方案' }} · {{ item.status || 'draft' }}
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="当前账号暂无知识库权限" />
          <a-empty v-if="canViewKnowledge && !loadingKnowledge && knowledgeItems.length === 0" description="暂无知识库内容" />
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="section-card">
          <template #title>当前建议</template>
          <div class="suggestion-list">
            <article class="suggestion-item">
              <strong>客户提单</strong>
              <p>提单页面要先明确系统、问题类型和影响范围，减少运维回访确认成本。</p>
            </article>
            <article class="suggestion-item">
              <strong>运维接单</strong>
              <p>优先在工作台里按结构化条件筛选，不再依赖一个关键字模糊查所有字段。</p>
            </article>
            <article class="suggestion-item">
              <strong>知识沉淀</strong>
              <p>处理完成后的最终方案建议沉淀为知识库条目，为后续 Agent 检索做准备。</p>
            </article>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ControlOutlined,
  InboxOutlined,
  PlusOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'
import { knowledgeApi, ticketApi } from '@/api/ticket'
import { formatDateTime } from '@/utils/datetime'
import { hasAnyPermission } from '@/utils/permission'
import { getOptionColor, getOptionLabel, loadTicketMeta } from '@/utils/ticket-meta'

const router = useRouter()
const loadingTickets = ref(false)
const loadingKnowledge = ref(false)
const recentTickets = ref([])
const knowledgeItems = ref([])
const statistics = ref({})
const statusOptions = ref([])
const priorityOptions = ref([])
const categoryOptions = ref([])

const canUseWorkbench = computed(() => hasAnyPermission(['ticket:list']))
const canCreateTicket = computed(() => hasAnyPermission(['ticket:add', 'ticket:my:add']))
const canViewKnowledge = computed(() => hasAnyPermission(['knowledge:list']))
const canManageAttachmentSecurity = computed(() => hasAnyPermission(['sys:config:list']))
const canManageTicketSystem = computed(() => hasAnyPermission(['ticket:list', 'sys:dict:list']))

const ticketColumns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 150 },
  { title: '工单标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '问题类型', dataIndex: 'category', key: 'category', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 170,
    customRender: ({ text }) => formatDateTime(text)
  }
]

const fetchMeta = async () => {
  const meta = await loadTicketMeta()
  statusOptions.value = meta.statusOptions || []
  priorityOptions.value = meta.priorityOptions || []
  categoryOptions.value = meta.categoryOptions || []
}

const statusCounter = computed(() => {
  return recentTickets.value.reduce((counter, item) => {
    counter[item.status] = (counter[item.status] || 0) + 1
    return counter
  }, {})
})

const statValue = (key, fallback = 0) => Number(statistics.value?.[key] ?? fallback)

const metrics = computed(() => [
  { key: 'pending', label: '待审批/待接', value: statValue('pendingApproval', statusCounter.value.pending_approval || 0) + statValue('pending', statusCounter.value.pending || 0), icon: ClockCircleOutlined },
  { key: 'processing', label: '处理中', value: statValue('processing', statusCounter.value.processing || 0) + statValue('waitingCustomer', statusCounter.value.waiting_customer || 0) + statValue('transferred', statusCounter.value.transferred || 0), icon: ToolOutlined },
  { key: 'resolved', label: '已解决', value: statValue('resolved', statusCounter.value.resolved || 0), icon: CheckCircleOutlined },
  { key: 'knowledge', label: '知识条目', value: statValue('knowledge', knowledgeItems.value.length), icon: BookOutlined }
])

const quickActions = computed(() => {
  const actions = []
  if (canCreateTicket.value) {
    actions.push({
      title: '提交工单',
      desc: '进入独立提单页，补充系统、描述和附件。',
      path: '/ticket/create',
      icon: PlusOutlined,
      tone: 'green'
    })
    actions.push({
      title: '我的工单',
      desc: '查看我提交的问题、草稿与处理进度。',
      path: '/ticket/my',
      icon: ToolOutlined,
      tone: 'blue'
    })
  }
  if (canUseWorkbench.value) {
    actions.push({
      title: '待接工单',
      desc: '进入运维接单大厅，待接任务和处理工作台彻底分开。',
      path: '/ticket/pending',
      icon: InboxOutlined,
      tone: 'amber'
    })
    actions.push({
      title: '工单工作台',
      desc: '只看我处理中、我已完成和全部处理记录。',
      path: '/ticket/workbench',
      icon: ToolOutlined,
      tone: 'blue'
    })
  }
  if (canManageTicketSystem.value) {
    actions.push({
      title: '系统配置',
      desc: '维护客户提单时可选择的业务系统。',
      path: '/ticket/systems',
      icon: ControlOutlined,
      tone: 'amber'
    })
  }
  if (canViewKnowledge.value) {
    actions.push({
      title: '知识库',
      desc: '沉淀解决方案，为后续 Agent 做准备。',
      path: '/ticket/knowledge',
      icon: BookOutlined,
      tone: 'amber'
    })
  }
  if (canManageAttachmentSecurity.value) {
    actions.push({
      title: '附件安全配置',
      desc: '维护上传大小、类型和安全策略。',
      path: '/system/config',
      icon: SafetyCertificateOutlined,
      tone: 'red'
    })
  }
  return actions
})

const fetchTickets = async () => {
  if (!canUseWorkbench.value && !canCreateTicket.value) {
    return
  }
  loadingTickets.value = true
  try {
    const params = { pageNum: 1, pageSize: 8 }
    if (!canUseWorkbench.value && canCreateTicket.value) {
      params.owner = 'customer'
    }
    const res = await ticketApi.list(params)
    recentTickets.value = res.data?.records || []
  } finally {
    loadingTickets.value = false
  }
}

const fetchStatistics = async () => {
  if (!canUseWorkbench.value && !canCreateTicket.value) {
    return
  }
  try {
    const res = await ticketApi.statistics()
    statistics.value = res.data || {}
  } catch {
    statistics.value = {}
  }
}

const fetchKnowledge = async () => {
  if (!canViewKnowledge.value) {
    return
  }
  loadingKnowledge.value = true
  try {
    const res = await knowledgeApi.list({ pageNum: 1, pageSize: 5 })
    knowledgeItems.value = res.data?.records || []
  } finally {
    loadingKnowledge.value = false
  }
}

const goTicketList = () => {
  router.push(canUseWorkbench.value ? '/ticket/workbench' : '/ticket/my')
}

const ticketStatusText = value => getOptionLabel(statusOptions.value, value, value || '-')
const ticketStatusColor = value => getOptionColor(statusOptions.value, value, 'default')
const ticketPriorityText = value => getOptionLabel(priorityOptions.value, value, value || '-')
const ticketPriorityColor = value => getOptionColor(priorityOptions.value, value, 'default')
const ticketCategoryText = value => getOptionLabel(categoryOptions.value, value, value || '-')

onMounted(async () => {
  await fetchMeta()
  fetchStatistics()
  fetchTickets()
  fetchKnowledge()
})
</script>

<style scoped>
.ticket-home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card {
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #143a7b 55%, #1677ff 100%);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.14);
}

.hero-card :deep(.ant-card-body) {
  padding: 22px 24px;
}

.hero-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.9fr);
  gap: 18px;
  align-items: stretch;
}

.hero-copy {
  display: grid;
  align-content: center;
  gap: 10px;
  min-height: 150px;
  color: #f8fafc;
}

.hero-badge {
  display: inline-flex;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 600;
}

.hero-copy h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
}

.hero-copy p {
  max-width: 620px;
  margin: 0;
  color: rgba(248, 250, 252, 0.86);
  font-size: 15px;
  line-height: 1.8;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-content: center;
}

.metric-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
}

.metric-panel__icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  font-size: 22px;
}

.metric-panel__icon.pending { color: #fdba74; background: rgba(251, 146, 60, 0.14); }
.metric-panel__icon.processing { color: #93c5fd; background: rgba(59, 130, 246, 0.16); }
.metric-panel__icon.resolved { color: #86efac; background: rgba(34, 197, 94, 0.16); }
.metric-panel__icon.knowledge { color: #fde68a; background: rgba(245, 158, 11, 0.16); }

.metric-panel span {
  display: block;
  color: rgba(248, 250, 252, 0.74);
  font-size: 13px;
}

.metric-panel strong {
  color: #fff;
  font-size: 24px;
  line-height: 1.2;
}

.section-card {
  border-radius: 20px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.action-list {
  display: grid;
  gap: 12px;
}

.action-list button {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 16px;
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.action-list button:hover {
  border-color: #bfdbfe;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
  transform: translateY(-2px);
}

.action-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-size: 20px;
}

.action-icon.green { color: #15803d; background: #dcfce7; }
.action-icon.blue { color: #1d4ed8; background: #dbeafe; }
.action-icon.amber { color: #b45309; background: #fef3c7; }
.action-icon.red { color: #b91c1c; background: #fee2e2; }

.action-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.action-copy strong {
  color: #172033;
  font-size: 15px;
}

.action-copy small,
.action-arrow {
  color: #6b7280;
}

.suggestion-list {
  display: grid;
  gap: 14px;
}

.suggestion-item {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #f8fafc;
}

.suggestion-item strong {
  display: block;
  margin-bottom: 8px;
  color: #172033;
  font-size: 15px;
}

.suggestion-item p {
  margin: 0;
  color: #4b5563;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .hero-card__main {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    font-size: 28px;
  }
}
</style>
