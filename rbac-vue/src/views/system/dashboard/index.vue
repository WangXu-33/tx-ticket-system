<template>
  <div class="ticket-dashboard">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">TX Ticket System</p>
        <h1>头绪工单系统</h1>
        <p class="hero-copy">把客户问题、运维处理、附件证据和知识沉淀放到同一个闭环里。</p>
        <a-space wrap>
          <a-button v-if="canCreateTicket" type="primary" size="large" @click="router.push('/ticket/my')">
            <template #icon><plus-outlined /></template>
            提交工单
          </a-button>
          <a-button v-if="canUseWorkbench" size="large" ghost @click="router.push('/ticket/workbench')">
            进入工作台
          </a-button>
        </a-space>
      </div>
      <div class="hero-card">
        <span class="hero-card-label">今日关注</span>
        <strong>{{ focusCount }}</strong>
        <small>待受理 / 处理中 / 待客户补充</small>
      </div>
    </section>

    <a-row :gutter="[18, 18]" class="metric-row">
      <a-col v-for="item in metrics" :key="item.key" :xs="24" :sm="12" :lg="6">
        <a-card :bordered="false" class="metric-card">
          <div class="metric-icon" :class="item.key">
            <component :is="item.icon" />
          </div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[18, 18]">
      <a-col :xs="24" :xl="15">
        <a-card :bordered="false" class="work-card">
          <template #title>
            <div class="section-title">
              <span>最近工单</span>
              <a-button type="link" @click="goTicketList">查看全部</a-button>
            </div>
          </template>
          <a-table
            row-key="id"
            :columns="ticketColumns"
            :data-source="recentTickets"
            :loading="loadingTickets"
            :pagination="false"
            size="middle"
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
            </template>
          </a-table>
          <a-empty v-if="!loadingTickets && recentTickets.length === 0" description="暂无工单，先提交一个问题吧" />
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="9">
        <a-card :bordered="false" class="action-card">
          <template #title>快捷入口</template>
          <div class="action-list">
            <button v-for="item in quickActions" :key="item.path" type="button" @click="router.push(item.path)">
              <span :class="['action-icon', item.tone]"><component :is="item.icon" /></span>
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <right-outlined />
            </button>
          </div>
        </a-card>

        <a-card :bordered="false" class="knowledge-card">
          <template #title>
            <div class="section-title">
              <span>知识库沉淀</span>
              <a-button v-if="canViewKnowledge" type="link" @click="router.push('/ticket/knowledge')">进入</a-button>
            </div>
          </template>
          <a-list v-if="canViewKnowledge" :data-source="knowledgeItems" :loading="loadingKnowledge" item-layout="horizontal">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a @click="router.push(`/ticket/knowledge/detail/${item.id}`)">{{ item.title }}</a>
                  </template>
                  <template #description>{{ item.category || '通用方案' }} · {{ item.status || 'draft' }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="当前账号暂无知识库权限" />
          <a-empty v-if="canViewKnowledge && !loadingKnowledge && knowledgeItems.length === 0" description="暂无知识库内容" />
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
  FileTextOutlined,
  PlusOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'
import { knowledgeApi, ticketApi } from '@/api/ticket'
import {
  TICKET_STATUS,
  ticketPriorityColor,
  ticketPriorityText,
  ticketStatusColor,
  ticketStatusText
} from '@/constants/ticket'
import { hasAnyPermission } from '@/utils/permission'

const router = useRouter()
const loadingTickets = ref(false)
const loadingKnowledge = ref(false)
const recentTickets = ref([])
const knowledgeItems = ref([])

const canUseWorkbench = computed(() => hasAnyPermission(['ticket:list']))
const canCreateTicket = computed(() => hasAnyPermission(['ticket:my:list']))
const canViewKnowledge = computed(() => hasAnyPermission(['knowledge:list']))
const canManageAttachmentSecurity = computed(() => hasAnyPermission(['sys:config:list']))

const ticketColumns = [
  { title: '工单编号', dataIndex: 'ticketNo', key: 'ticketNo', width: 150 },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]

const statusCounter = computed(() => {
  return recentTickets.value.reduce((counter, item) => {
    counter[item.status] = (counter[item.status] || 0) + 1
    return counter
  }, {})
})

const focusCount = computed(() => {
  return [
    TICKET_STATUS.PENDING,
    TICKET_STATUS.PROCESSING,
    TICKET_STATUS.WAITING_CUSTOMER,
    TICKET_STATUS.TRANSFERRED
  ].reduce((total, status) => total + (statusCounter.value[status] || 0), 0)
})

const metrics = computed(() => [
  { key: 'pending', label: '待受理', value: statusCounter.value[TICKET_STATUS.PENDING] || 0, icon: ClockCircleOutlined },
  { key: 'processing', label: '处理中', value: statusCounter.value[TICKET_STATUS.PROCESSING] || 0, icon: ToolOutlined },
  { key: 'resolved', label: '已解决', value: statusCounter.value[TICKET_STATUS.RESOLVED] || 0, icon: CheckCircleOutlined },
  { key: 'knowledge', label: '知识条目', value: knowledgeItems.value.length, icon: BookOutlined }
])

const quickActions = computed(() => {
  const actions = []
  if (canCreateTicket.value) {
    actions.push({ title: '我的工单', desc: '提交问题并跟踪处理进度', path: '/ticket/my', icon: FileTextOutlined, tone: 'green' })
  }
  if (canUseWorkbench.value) {
    actions.push({ title: '工单工作台', desc: '接单、分派、转派和闭环处理', path: '/ticket/workbench', icon: ToolOutlined, tone: 'blue' })
  }
  if (canViewKnowledge.value) {
    actions.push({ title: '知识库', desc: '沉淀解决方案，为后续 Agent 做准备', path: '/ticket/knowledge', icon: BookOutlined, tone: 'amber' })
  }
  if (canManageAttachmentSecurity.value) {
    actions.push({ title: '附件安全配置', desc: '维护上传大小、类型和安全策略', path: '/system/config', icon: SafetyCertificateOutlined, tone: 'red' })
  }
  return actions
})

/**
 * 修改时间：2026-07-02
 * 功能说明：根据当前账号权限加载可见工单，避免客户账号访问运维列表接口。
 * 入参：无。
 * 出参：无。
 * 异常场景：接口失败时由请求层统一提示，页面保留空态。
 */
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

/**
 * 修改时间：2026-07-02
 * 功能说明：加载知识库摘要，为首页展示近期沉淀内容。
 * 入参：无。
 * 出参：无。
 * 异常场景：无权限时不调用接口，避免无意义报错。
 */
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

onMounted(() => {
  fetchTickets()
  fetchKnowledge()
})
</script>

<style scoped>
.ticket-dashboard {
  --tx-ink: #10201b;
  --tx-muted: #66766f;
  --tx-green: #0f9f6e;
  --tx-deep: #0c2f28;
  --tx-card: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-panel {
  min-height: 260px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  align-items: end;
  gap: 28px;
  padding: clamp(28px, 5vw, 48px);
  border-radius: 34px;
  position: relative;
  overflow: hidden;
  color: #f8fffc;
  background:
    radial-gradient(circle at 78% 18%, rgba(134, 239, 172, 0.34), transparent 28%),
    linear-gradient(135deg, #082f28 0%, #0f766e 52%, #111827 100%);
  box-shadow: 0 26px 70px rgba(8, 47, 40, 0.24);
}

.hero-panel::after {
  content: "";
  position: absolute;
  inset: auto -8% -34% 38%;
  height: 180px;
  background: repeating-linear-gradient(135deg, rgba(255,255,255,0.14) 0 1px, transparent 1px 14px);
  transform: rotate(-6deg);
}

.hero-panel > * {
  position: relative;
  z-index: 1;
}

.eyebrow {
  margin: 0 0 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-weight: 800;
  color: #bbf7d0;
}

h1 {
  margin: 0;
  font-size: clamp(38px, 6vw, 72px);
  line-height: 0.95;
  font-weight: 900;
}

.hero-copy {
  max-width: 620px;
  margin: 18px 0 26px;
  font-size: 18px;
  color: #d7fff0;
}

.hero-card {
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(16px);
}

.hero-card-label,
.hero-card small {
  display: block;
  color: #d7fff0;
}

.hero-card strong {
  display: block;
  margin: 12px 0 4px;
  font-size: 62px;
  line-height: 1;
}

.metric-card :deep(.ant-card-body) {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 106px;
}

.metric-card,
.work-card,
.action-card,
.knowledge-card {
  border-radius: 24px;
  background: var(--tx-card);
  box-shadow: 0 12px 35px rgba(15, 23, 42, 0.06);
}

.metric-icon {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  font-size: 24px;
}

.metric-icon.pending { color: #c2410c; background: #ffedd5; }
.metric-icon.processing { color: #0369a1; background: #e0f2fe; }
.metric-icon.resolved { color: #047857; background: #d1fae5; }
.metric-icon.knowledge { color: #a16207; background: #fef3c7; }

.metric-card span {
  color: var(--tx-muted);
}

.metric-card strong {
  display: block;
  color: var(--tx-ink);
  font-size: 30px;
  line-height: 1.1;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 800;
}

.action-list {
  display: grid;
  gap: 12px;
}

.action-list button {
  width: 100%;
  display: grid;
  grid-template-columns: 46px 1fr 18px;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5eee9;
  border-radius: 18px;
  background: #fbfffd;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.action-list button:hover,
.action-list button:focus-visible {
  border-color: #9de8c8;
  box-shadow: 0 14px 28px rgba(15, 159, 110, 0.12);
  transform: translateY(-2px);
  outline: none;
}

.action-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  font-size: 20px;
}

.action-icon.green { color: #047857; background: #d1fae5; }
.action-icon.blue { color: #0369a1; background: #e0f2fe; }
.action-icon.amber { color: #a16207; background: #fef3c7; }
.action-icon.red { color: #be123c; background: #ffe4e6; }

.action-list strong,
.action-list small {
  display: block;
}

.action-list small {
  margin-top: 3px;
  color: var(--tx-muted);
}

.knowledge-card {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .hero-panel {
    grid-template-columns: 1fr;
    border-radius: 24px;
  }

  .hero-card {
    max-width: 320px;
  }
}
</style>
