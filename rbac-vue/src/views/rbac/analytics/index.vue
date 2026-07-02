<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false" title="权限分析概览">
      <template #extra>
        <a-button v-hasPerm="['sys:analytics:export']" type="primary" ghost @click="handleExport">
          <template #icon><export-outlined /></template>导出分析报告 (XLSX)
        </a-button>
      </template>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="12" :md="8" :lg="6" :xl="4" v-for="(item, index) in overviewCards" :key="index">
          <a-card class="stat-card" :bordered="false" style="margin-bottom: 16px;">
            <a-statistic :title="item.title" :value="overviewData[item.key] || 0">
              <template #prefix>
                <component :is="item.icon" :style="{ color: item.color, marginRight: '8px' }" />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="[16, 16]" style="margin-top: 16px;" class="slide-in-2">
      <a-col :span="12">
        <a-card class="chart-card" title="角色分布情况" :bordered="false">
          <div ref="rolePieRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card class="chart-card" title="用户拥有的权限数 Top 10" :bordered="false">
          <div ref="userTopRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="table-card slide-in-3" title="系统权限复用情况" :bordered="false" style="margin-top: 16px;">
      <a-table 
        :dataSource="permissionUsageList" 
        :columns="columns" 
        rowKey="permKey" 
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permKey'">
            <a-tag color="blue">{{ record.permKey }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { 
  ExportOutlined, UserOutlined, SolutionOutlined, ApartmentOutlined, 
  MenuOutlined, SafetyCertificateOutlined, UserSwitchOutlined 
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { useEcharts } from '@/utils/useEcharts'
import { LIGHT_THEME_PALETTE, STATUS_COLORS } from '@/theme/palette'
import { downloadByRequest } from '@/utils/download'

const { initChart } = useEcharts()

const overviewData = ref({})
const rolePieRef = ref(null)
const userTopRef = ref(null)
const permissionUsageList = ref([])
const loading = ref(false)

const overviewCards = [
  { title: '系统用户数', key: 'userTotal', icon: UserOutlined, color: STATUS_COLORS.info },
  { title: '业务角色数', key: 'roleTotal', icon: SolutionOutlined, color: STATUS_COLORS.accent },
  { title: '组织部门数', key: 'deptTotal', icon: ApartmentOutlined, color: STATUS_COLORS.warning },
  { title: '功能菜单数', key: 'menuTotal', icon: MenuOutlined, color: LIGHT_THEME_PALETTE.link },
  { title: '按钮权限数', key: 'permissionTotal', icon: SafetyCertificateOutlined, color: STATUS_COLORS.success },
  { title: '直授权限用户', key: 'directGrantUserTotal', icon: UserSwitchOutlined, color: STATUS_COLORS.danger }
]

const columns = [
  { title: '权限标识', dataIndex: 'permKey', key: 'permKey', width: 250 },
  { title: '权限名称', dataIndex: 'permName', key: 'permName' },
  { title: '绑定菜单数', dataIndex: 'menuBindCount', key: 'menuBindCount', width: 120, align: 'center' },
  { title: '绑定角色数', dataIndex: 'roleBindCount', key: 'roleBindCount', width: 120, align: 'center' },
  { title: '拥有用户数', dataIndex: 'userBindCount', key: 'userBindCount', width: 120, align: 'center' }
]

const initPieChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'item' },
    legend: { bottom: '0', left: 'center' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: data
    }]
  })
}

const initBarChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(item => item.nickname), inverse: true },
    series: [{ 
      name: '拥有权限数', type: 'bar', 
      data: data.map(item => item.permissionCount), 
      itemStyle: { color: STATUS_COLORS.accent, borderRadius: [0, 4, 4, 0] }
    }]
  })
}

const fetchData = async () => {
  loading.value = true
  try {
    const overviewRes = await request.get('/rbac/stats/overview')
    overviewData.value = overviewRes.data || {}

    const roleRes = await request.get('/rbac/stats/role-distribution')
    nextTick(() => { initPieChart(rolePieRef.value, roleRes.data || []) })

    const userTopRes = await request.get('/rbac/stats/user-auth-top')
    nextTick(() => { initBarChart(userTopRef.value, userTopRes.data || []) })

    const usageRes = await request.get('/rbac/stats/permission-usage')
    permissionUsageList.value = usageRes.data || []
  } catch (e) {
    console.error("Analytics Error", e)
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  await downloadByRequest({
    url: '/rbac/stats/export',
    params: { format: 'xlsx' },
    fileName: 'tx-ticket-analytics.xlsx'
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.stat-card { border-radius: 8px; background: var(--bg-card); border: 1px solid #e2e8f0; }
.chart-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); background: var(--bg-card); }

.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }
.slide-in-3 { animation: slide-up 0.6s both 0.2s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

:deep(.ant-table) { background: transparent !important; }
:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
.dark-mode .stat-card { border-color: #334155; }
</style>
