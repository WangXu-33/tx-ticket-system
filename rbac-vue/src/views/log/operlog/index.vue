<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <a-form layout="inline" :model="queryParams" class="search-form-inline">
        <a-form-item label="操作模块" class="search-item search-item-input">
          <a-input v-model:value="queryParams.title" placeholder="请输入模块名" allow-clear />
        </a-form-item>
        <a-form-item label="操作人员" class="search-item search-item-input">
          <a-input v-model:value="queryParams.operName" placeholder="请输入操作人" allow-clear />
        </a-form-item>
        <a-form-item label="操作类型" class="search-item search-item-select">
          <a-select v-model:value="queryParams.businessType" placeholder="操作类型" allow-clear>
            <a-select-option :value="1">新增</a-select-option>
            <a-select-option :value="2">修改</a-select-option>
            <a-select-option :value="3">删除</a-select-option>
            <a-select-option :value="0">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="操作状态" class="search-item search-item-select">
          <a-select v-model:value="queryParams.status" placeholder="操作状态" allow-clear>
            <a-select-option :value="0">成功</a-select-option>
            <a-select-option :value="1">失败</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="操作时间" class="search-item search-item-range">
          <a-range-picker 
            v-model:value="dateRange" 
            class="full-width"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="onDateChange"
            allow-clear 
          />
        </a-form-item>
        <a-form-item class="search-item search-item-actions">
          <a-space wrap>
            <a-button type="primary" @click="handleQuery">
              <template #icon><SearchOutlined /></template>查询
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
            <a-col :span="8">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="操作总量" :value="statsSummary.totalCount" />
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="今日操作" :value="statsSummary.todayCount" />
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card class="stat-card" :bordered="false">
                <a-statistic title="平均耗时" :value="statsSummary.avgCostTime" suffix="ms" />
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
          <a-card class="stat-chart-card" title="模块 Top 10" :bordered="false">
            <div ref="topModulesRef" class="stat-chart"></div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-chart-card" title="近30天趋势" :bordered="false">
            <div ref="trendChartRef" class="stat-chart"></div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <a-card class="table-card slide-in-3" :bordered="false" style="margin-top: 16px;">
      <template #extra>
        <a-space>
          <a-button v-hasPerm="['sys:operlog:export']" type="primary" ghost @click="handleExport">
            <template #icon><ExportOutlined /></template>导出 (XLSX)
          </a-button>
          <a-popconfirm title="确定清空所有操作日志吗？此操作不可恢复" @confirm="handleClean">
            <a-button v-hasPerm="['sys:operlog:delete']" danger>
              <template #icon><DeleteOutlined /></template>清空
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
          <template v-if="column.key === 'businessType'">
            <a-tag :color="record.businessType === 1 ? 'blue' : (record.businessType === 2 ? 'green' : (record.businessType === 3 ? 'red' : 'purple'))">
              {{ record.businessType === 1 ? '新增' : (record.businessType === 2 ? '修改' : (record.businessType === 3 ? '删除' : '其他')) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge :status="record.status === 0 ? 'success' : 'error'" :text="record.status === 0 ? '成功' : '失败'" />
          </template>
          <template v-if="column.key === 'action'">
             <a class="edit-link" @click="handleDetail(record)">详细</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      title="操作日志详细"
      :width="600"
      :open="detailVisible"
      @close="detailVisible = false"
    >
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="操作模块">{{ currentRecord.title }}</a-descriptions-item>
        <a-descriptions-item label="操作人员">{{ currentRecord.operName }}</a-descriptions-item>
        <a-descriptions-item label="请求地址">{{ currentRecord.operUrl }}</a-descriptions-item>
        <a-descriptions-item label="操作方法">{{ currentRecord.method }}</a-descriptions-item>
        <a-descriptions-item label="请求参数"><pre>{{ currentRecord.operParam }}</pre></a-descriptions-item>
        <a-descriptions-item label="返回参数"><pre>{{ currentRecord.jsonResult }}</pre></a-descriptions-item>
        <a-descriptions-item label="错误消息" v-if="currentRecord.status === 1"><span style="color:red">{{ currentRecord.errorMsg }}</span></a-descriptions-item>
        <a-descriptions-item label="操作时间">{{ formatDateTime(currentRecord.operTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
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
import { formatDateTime } from '@/utils/datetime'

const { initChart } = useEcharts()

const loading = ref(false)
const dataSource = ref([])
const dateRange = ref([])
const queryParams = reactive({ title: '', operName: '', businessType: undefined, status: undefined, startDate: '', endDate: '', pageNum: 1, pageSize: 10 })

const statsSummary = ref({ totalCount: 0, successCount: 0, failCount: 0, todayCount: 0, avgCostTime: 0 })
const topModulesRef = ref(null)
const trendChartRef = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const detailVisible = ref(false)
const currentRecord = ref({})

const columns = [
  { title: '编号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模块', dataIndex: 'title', key: 'title' },
  { title: '操作类型', dataIndex: 'businessType', key: 'businessType' },
  { title: '操作人员', dataIndex: 'operName', key: 'operName' },
  { title: '主机IP', dataIndex: 'operIp', key: 'operIp' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作时间', dataIndex: 'operTime', key: 'operTime', customRender: ({ text }) => formatDateTime(text) },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
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

const initBarChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 12, bottom: 14, top: 8, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'var(--text-secondary)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(item => item.name),
      inverse: true,
      axisLabel: {
        color: 'var(--text-secondary)',
        fontSize: 11,
        width: 72,
        overflow: 'truncate'
      }
    },
    series: [{ type: 'bar', data: data.map(item => item.value), itemStyle: { color: STATUS_COLORS.accent, borderRadius: [0, 4, 4, 0] } }]
  })
}

const initLineChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis' },
    grid: { left: 6, right: 12, bottom: 18, top: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(item => item.date),
      axisLabel: { color: 'var(--text-secondary)', fontSize: 11, hideOverlap: true },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'var(--text-secondary)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.22)' } }
    },
    series: [{ type: 'line', smooth: true, data: data.map(item => item.value), itemStyle: { color: STATUS_COLORS.success } }]
  })
}

const fetchStats = async () => {
  try {
    const summaryRes = await request.get('/system/operlog/stats/summary', { params: queryParams })
    statsSummary.value = summaryRes.data || { totalCount: 0, successCount: 0, failCount: 0, todayCount: 0, avgCostTime: 0 }

    const topModulesRes = await request.get('/system/operlog/stats/top-modules', { params: queryParams })
    nextTick(() => { initBarChart(topModulesRef.value, topModulesRes.data || []) })

    const trendRes = await request.get('/system/operlog/stats/trend', { params: { ...queryParams, days: 30 } })
    nextTick(() => { initLineChart(trendChartRef.value, trendRes.data || []) })
  } catch (e) {
    console.error("Stats Error", e)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams, beginTime: queryParams.startDate, endTime: queryParams.endDate }
    const res = await request.get('/system/operlog/list', { params })
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
  queryParams.title = ''
  queryParams.operName = ''
  queryParams.businessType = undefined
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

const handleDetail = (record) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleExport = async () => {
  await downloadByRequest({
    url: '/system/operlog/export',
    params: { ...queryParams, format: 'xlsx' },
    fileName: 'oper-report.xlsx'
  })
}

const handleClean = async () => {
  await request.delete(`/system/operlog/clean`)
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
.stat-chart-card {
  height: 236px;
  overflow: hidden;
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
}
.stat-chart-card :deep(.ant-card-head) {
  min-height: 48px;
  padding: 0 22px;
}
.stat-chart-card :deep(.ant-card-head-title) {
  padding: 13px 0;
  font-size: 15px;
  font-weight: 700;
}
.stat-chart-card :deep(.ant-card-body) {
  height: calc(100% - 48px);
  padding: 10px 14px 12px;
}
.stat-chart {
  width: 100%;
  height: 100%;
}
.search-form-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  align-items: center;
}
.search-form-inline :deep(.ant-form-item) {
  margin-inline-end: 0;
  margin-bottom: 0;
  display: flex;
}
.search-item-input {
  width: 280px;
}
.search-item-select {
  width: 220px;
}
.search-item-range {
  width: 380px;
}
.search-item-input :deep(.ant-form-item-control),
.search-item-select :deep(.ant-form-item-control),
.search-item-range :deep(.ant-form-item-control) {
  flex: 1 1 auto;
}
.search-item-input :deep(.ant-input),
.search-item-select :deep(.ant-select),
.search-item-select :deep(.ant-select-selector),
.search-item-range :deep(.ant-picker) {
  width: 100%;
}
.search-item-actions {
  margin-left: auto;
}
.full-width { width: 100%; }

.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }
.slide-in-3 { animation: slide-up 0.6s both 0.2s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.edit-link { color: var(--color-primary); font-weight: 600; }
.dark-mode .edit-link { color: #818cf8; }
:deep(.ant-table) { background: transparent !important; }
:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
pre { max-height: 200px; overflow-y: auto; background: var(--bg-primary); padding: 10px; border-radius: 8px; font-size: 12px; white-space: pre-wrap; word-wrap: break-word; }

@media (max-width: 1200px) {
  .search-item-actions {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .search-item-input,
  .search-item-select,
  .search-item-range,
  .search-item-actions {
    width: 100%;
    min-width: 100%;
  }
}
</style>
