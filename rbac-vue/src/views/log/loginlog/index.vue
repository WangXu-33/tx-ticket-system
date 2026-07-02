<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="登录用户">
          <a-input v-model:value="queryParams.username" placeholder="请输入用户名" allow-clear />
        </a-form-item>
        <a-form-item label="登录状态">
          <a-select v-model:value="queryParams.status" placeholder="登录状态" allow-clear style="width: 120px">
            <a-select-option value="0">成功</a-select-option>
            <a-select-option value="1">失败</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="登录时间">
          <a-range-picker 
            v-model:value="dateRange" 
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="onDateChange"
            allow-clear 
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery">
              <template #icon><search-outlined /></template>查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 统计区域 -->
    <div class="stats-container slide-in-2" style="margin-top: 16px;">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-row :gutter="[16, 16]">
            <a-col :span="12">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="登录总数" :value="statsSummary.totalCount" />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="今日登录" :value="statsSummary.todayCount" />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="成功次数" :value="statsSummary.successCount" :value-style="{ color: 'var(--color-success)' }" />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="失败次数" :value="statsSummary.failCount" :value-style="{ color: 'var(--color-danger)' }" />
              </a-card>
            </a-col>
          </a-row>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-chart-card" title="状态分布" :bordered="false">
            <div ref="statusPieRef" style="height: 180px;"></div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-chart-card" title="近30天趋势" :bordered="false">
            <div ref="trendChartRef" style="height: 180px;"></div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <a-card class="table-card slide-in-3" :bordered="false" style="margin-top: 16px;">
      <template #extra>
        <a-space>
          <a-button v-hasPerm="['sys:loginlog:export']" type="primary" ghost @click="handleExport">
            <template #icon><export-outlined /></template>导出 (XLSX)
          </a-button>
          <a-popconfirm title="确定清空所有登录日志吗？此操作不可恢复" @confirm="handleClean">
            <a-button v-hasPerm="['sys:loginlog:delete']" danger>
              <template #icon><delete-outlined /></template>清空
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>

      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        rowKey="id" 
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge :status="record.status === 0 ? 'success' : 'error'" :text="record.status === 0 ? '成功' : '失败'" />
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { SearchOutlined, DeleteOutlined, ExportOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import { useEcharts } from '@/utils/useEcharts'
import { STATUS_COLORS } from '@/theme/palette'
import { downloadByRequest } from '@/utils/download'

const { initChart } = useEcharts()

const loading = ref(false)
const dataSource = ref([])
const dateRange = ref([])
const queryParams = reactive({ username: '', status: undefined, startDate: '', endDate: '', pageNum: 1, pageSize: 10 })

const statsSummary = ref({ totalCount: 0, successCount: 0, failCount: 0, todayCount: 0 })
const statusPieRef = ref(null)
const trendChartRef = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
  { title: '编号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户账号', dataIndex: 'username', key: 'username' },
  { title: '登录IP', dataIndex: 'ipaddr', key: 'ipaddr' },
  { title: '登录地点', dataIndex: 'loginLocation', key: 'loginLocation' },
  { title: '浏览器', dataIndex: 'browser', key: 'browser' },
  { title: '操作系统', dataIndex: 'os', key: 'os' },
  { title: '登录状态', dataIndex: 'status', key: 'status' },
  { title: '操作信息', dataIndex: 'msg', key: 'msg' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime' }
]

const onDateChange = (val) => {
  if (val && val.length === 2) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

const initPieChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      label: { show: false },
      data: data
    }]
  })
}

const initLineChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.map(item => item.date) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: data.map(item => item.value), itemStyle: { color: STATUS_COLORS.info } }]
  })
}

const fetchStats = async () => {
  try {
    const summaryRes = await request.get('/system/loginlog/stats/summary', { params: queryParams })
    statsSummary.value = summaryRes.data || { totalCount: 0, successCount: 0, failCount: 0, todayCount: 0 }
    nextTick(() => {
      initPieChart(statusPieRef.value, summaryRes.data?.statusPie || [])
    })

    const trendRes = await request.get('/system/loginlog/stats/trend', { params: { ...queryParams, days: 30 } })
    nextTick(() => {
      initLineChart(trendChartRef.value, trendRes.data || [])
    })
  } catch (e) {
    console.error("Stats Error", e)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams, beginTime: queryParams.startDate, endTime: queryParams.endDate }
    const res = await request.get('/system/loginlog/list', { params })
    dataSource.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  pagination.current = 1
  fetchData()
  fetchStats()
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.status = undefined
  dateRange.value = []
  queryParams.startDate = ''
  queryParams.endDate = ''
  handleQuery()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchData()
}

const handleExport = async () => {
  await downloadByRequest({
    url: '/system/loginlog/export',
    params: { ...queryParams, format: 'xlsx' },
    fileName: 'login-report.xlsx'
  })
}

const handleClean = async () => {
  await request.delete(`/system/loginlog/clean`)
  message.success('清空成功')
  handleQuery()
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.stat-card { border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); background: var(--bg-card); }
.stat-chart-card { border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); background: var(--bg-card); height: 236px; }
.stat-chart-card :deep(.ant-card-body) { padding: 12px 24px; }

.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }
.slide-in-3 { animation: slide-up 0.6s both 0.2s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
:deep(.ant-table) { background: transparent !important; }
:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
</style>
