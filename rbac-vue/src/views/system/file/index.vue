<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="文件名称">
          <a-input v-model:value="queryParams.fileName" placeholder="请输入文件名" allow-clear />
        </a-form-item>
        <a-form-item label="存储位置">
          <a-select v-model:value="queryParams.storageType" placeholder="存储位置" allow-clear style="width: 120px">
            <a-select-option value="LOCAL">本地存储</a-select-option>
            <a-select-option value="ALIYUN">阿里云 OSS</a-select-option>
            <a-select-option value="MINIO">私有云 MinIO</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="文件类型">
          <a-select
            v-model:value="queryParams.fileSuffixes"
            mode="multiple"
            :options="suffixOptions"
            placeholder="按后缀筛选"
            allow-clear
            :max-tag-count="1"
            style="width: 220px"
          />
        </a-form-item>
        <a-form-item label="上传时间">
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
            <a-button type="primary" @click="fetchData">
              <template #icon><search-outlined /></template>查询
            </a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card slide-in-2" :bordered="false">
      <template #extra>
        <a-space>
          <a-button type="primary" class="report-button" @click="openReportDrawer">
            <template #icon><bar-chart-outlined /></template>统计报表
          </a-button>
          
          <a-button v-hasPerm="['sys:file:export']" type="primary" ghost @click="handleExport">
            <template #icon><export-outlined /></template>导出
          </a-button>
          
          <a-upload
            name="file"
            accept="image/*"
            :showUploadList="false"
            :customRequest="customUpload"
          >
            <a-button v-hasPerm="['sys:file:upload']" type="primary" ghost>
              <template #icon><file-image-outlined /></template>上传图片
            </a-button>
          </a-upload>
          <a-upload
            name="file"
            :showUploadList="false"
            :customRequest="customUpload"
          >
            <a-button v-hasPerm="['sys:file:upload']" type="primary">
              <template #icon><upload-outlined /></template>上传文件
            </a-button>
          </a-upload>
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
          <template v-if="column.key === 'fileName'">
            <span class="file-name-cell">
              <component :is="getFileIcon(record)" :style="{ color: getFileIconColor(record) }" />
              <span class="file-name-text">{{ record.fileName }}</span>
            </span>
          </template>
          <template v-if="column.key === 'storageType'">
            <a-tag :color="record.storageType === 'ALIYUN' ? 'blue' : (record.storageType === 'MINIO' ? 'purple' : 'green')">
              {{ record.storageType === 'ALIYUN' ? '阿里云' : (record.storageType === 'MINIO' ? 'MinIO' : '本地') }}
            </a-tag>
          </template>
          <template v-if="column.key === 'url'">
            <a v-if="isImage(record.fileSuffix)" class="url-link" @click="openPreview(record)">
              <img :src="imagePreviewMap[record.id] || ''" class="thumbnail" />
            </a>
            <a v-else class="url-link" @click="handleDownload(record)">
              {{ record.fileName || resolveFileLink(record) }}
            </a>
          </template>
          <template v-if="column.key === 'fileSize'">
            {{ formatSize(record.fileSize) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a class="table-action-link" @click="openPreview(record)">预览</a>
              <a class="table-action-link" @click="handleDownload(record)">下载</a>
              <a-popconfirm title="确定删除该文件吗？" @confirm="handleDelete(record.id)">
                <a v-hasPerm="['sys:file:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <file-preview
      v-model:open="previewVisible"
      :record="previewRecord"
      @download="handleDownload"
    />

    <!-- 统计报表抽屉 -->
    <a-drawer
      title="文件资产统计报表"
      :width="800"
      :open="reportVisible"
      @close="reportVisible = false"
    >
      <template #extra>
        <a-button @click="fetchReportData" type="primary" size="small">
          <template #icon><reload-outlined /></template>刷新报表
        </a-button>
      </template>
      <div class="report-container">
        <a-row :gutter="[16, 16]">
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="全站文件总数" :value="statsData.totalCount" suffix="个">
                <template #prefix><folder-open-outlined :style="{ color: STATUS_COLORS.info }" /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="全站存储容量" :value="formatSizeValue(statsData.totalSize)" :suffix="formatSizeUnit(statsData.totalSize)">
                <template #prefix><hdd-outlined :style="{ color: STATUS_COLORS.accent }" /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card today">
              <a-statistic title="今日新增文件" :value="statsData.todayCount" suffix="个">
                <template #prefix><plus-circle-outlined :style="{ color: STATUS_COLORS.success }" /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card today">
              <a-statistic title="今日新增容量" :value="formatSizeValue(statsData.todaySize)" :suffix="formatSizeUnit(statsData.todaySize)">
                <template #prefix><cloud-upload-outlined :style="{ color: STATUS_COLORS.warning }" /></template>
              </a-statistic>
            </a-card>
          </a-col>
        </a-row>

        <a-row :gutter="[16, 16]" style="margin-top: 16px;">
          <a-col :span="24">
            <a-card class="chart-card" title="近30天上传趋势" :bordered="false">
              <div ref="uploadTrendRef" style="height: 250px;"></div>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card class="chart-card" title="存储方式分布" :bordered="false">
              <div ref="storageTypePieRef" style="height: 250px;"></div>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card class="chart-card" title="文件类型 Top 10" :bordered="false">
              <div ref="fileTypeTopRef" style="height: 250px;"></div>
            </a-card>
          </a-col>
        </a-row>
        
        <a-card class="info-card" style="margin-top: 16px;">
          <template #title>
            <span><info-circle-outlined style="color: #64748b; margin-right: 8px;" />系统健康指引</span>
          </template>
          <p style="color: #64748b; font-size: 13px; line-height: 1.6; margin: 0;">
            定期关注文件资产增长速率，可提前为您是否需要扩容 OSS 或私有云磁盘提供决策依据。当前报表统计为准实时计算。
          </p>
        </a-card>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import {
  SearchOutlined, UploadOutlined, FileImageOutlined, BarChartOutlined,
  FolderOpenOutlined, HddOutlined, PlusCircleOutlined, CloudUploadOutlined, InfoCircleOutlined, ExportOutlined,
  ReloadOutlined, FilePdfOutlined, FileWordOutlined, FileExcelOutlined, FilePptOutlined, FileZipOutlined,
  FileTextOutlined, FileUnknownOutlined
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import { buildFileDownloadUrl, buildFilePreviewUrl } from '@/utils/file'
import { useEcharts } from '@/utils/useEcharts'
import { STATUS_COLORS } from '@/theme/palette'
import { downloadByRequest } from '@/utils/download'
import { getAuthedPreviewUrl, revokeAuthedPreviewUrl } from '@/utils/file-preview'
import FilePreview from '@/components/file-preview/index.vue'

const { initChart } = useEcharts()

const loading = ref(false)
const dataSource = ref([])
const imagePreviewMap = reactive({})
const suffixOptions = ref([])
const dateRange = ref([])
const queryParams = reactive({ fileName: '', storageType: undefined, fileSuffixes: [], beginTime: '', endTime: '', pageNum: 1, pageSize: 10 })

const reportVisible = ref(false)
const previewVisible = ref(false)
const previewRecord = ref(null)
const statsData = ref({ totalCount: 0, totalSize: 0, todayCount: 0, todaySize: 0 })

const uploadTrendRef = ref(null)
const storageTypePieRef = ref(null)
const fileTypeTopRef = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const columns = [
  { title: '编号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '文件名称', dataIndex: 'fileName', key: 'fileName' },
  { title: '文件后缀', dataIndex: 'fileSuffix', key: 'fileSuffix', width: 100 },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 120 },
  { title: '访问入口', dataIndex: 'url', key: 'url', ellipsis: true },
  { title: '存储位置', dataIndex: 'storageType', key: 'storageType', width: 100 },
  { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]

const isImage = (suffix) => {
  if (!suffix) return false
  const imgExts = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg']
  return imgExts.includes(suffix.toLowerCase())
}

const normalizeSuffix = (suffix) => (suffix || '').toLowerCase()

const getFileIcon = (record) => {
  const suffix = normalizeSuffix(record.fileSuffix)
  if (isImage(suffix)) return FileImageOutlined
  if (suffix === '.pdf') return FilePdfOutlined
  if (['.doc', '.docx'].includes(suffix)) return FileWordOutlined
  if (['.xls', '.xlsx'].includes(suffix)) return FileExcelOutlined
  if (['.ppt', '.pptx'].includes(suffix)) return FilePptOutlined
  if (['.zip', '.rar', '.7z', '.tar', '.gz'].includes(suffix)) return FileZipOutlined
  if (['.txt', '.md', '.json', '.xml', '.csv', '.log'].includes(suffix)) return FileTextOutlined
  return FileUnknownOutlined
}

const getFileIconColor = (record) => {
  const suffix = normalizeSuffix(record.fileSuffix)
  if (isImage(suffix)) return STATUS_COLORS.success
  if (suffix === '.pdf') return STATUS_COLORS.danger
  if (['.doc', '.docx'].includes(suffix)) return STATUS_COLORS.info
  if (['.xls', '.xlsx'].includes(suffix)) return STATUS_COLORS.success
  if (['.ppt', '.pptx'].includes(suffix)) return STATUS_COLORS.warning
  if (['.zip', '.rar', '.7z', '.tar', '.gz'].includes(suffix)) return STATUS_COLORS.accent
  return 'var(--text-sub)'
}

const buildFileSuffixParam = () => {
  return queryParams.fileSuffixes?.length ? queryParams.fileSuffixes.join(',') : undefined
}

const buildListParams = () => ({
  fileName: queryParams.fileName,
  storageType: queryParams.storageType,
  fileSuffixes: buildFileSuffixParam(),
  beginTime: queryParams.beginTime,
  endTime: queryParams.endTime,
  pageNum: queryParams.pageNum,
  pageSize: queryParams.pageSize
})

const buildReportParams = () => ({
  fileName: queryParams.fileName,
  storageType: queryParams.storageType,
  fileSuffixes: buildFileSuffixParam(),
  startDate: queryParams.beginTime,
  endDate: queryParams.endTime
})

const cleanParams = (params) => {
  const result = { ...params }
  Object.keys(result).forEach(key => {
    if (result[key] === undefined || result[key] === null || result[key] === '') {
      delete result[key]
    }
  })
  return result
}

const resolveFileLink = (record) => {
  if (isImage(record.fileSuffix) && imagePreviewMap[record.id]) {
    return imagePreviewMap[record.id]
  }
  return buildFileDownloadUrl(record.id)
}

const clearImagePreviewMap = () => {
  Object.keys(imagePreviewMap).forEach((key) => {
    revokeAuthedPreviewUrl(key)
    delete imagePreviewMap[key]
  })
}

const syncImagePreviewMap = async (records) => {
  clearImagePreviewMap()
  const imageRecords = records.filter(item => isImage(item.fileSuffix))
  await Promise.all(imageRecords.map(async (item) => {
    try {
      imagePreviewMap[item.id] = await getAuthedPreviewUrl(item.id)
    } catch {
      imagePreviewMap[item.id] = ''
    }
  }))
}

const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatSizeValue = (bytes) => {
  if (!bytes || bytes === 0) return 0
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2))
}

const formatSizeUnit = (bytes) => {
  if (!bytes || bytes === 0) return 'B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return sizes[i]
}

const onDateChange = (val) => {
  if (val && val.length === 2) {
    queryParams.beginTime = val[0]
    queryParams.endTime = val[1]
  } else {
    queryParams.beginTime = ''
    queryParams.endTime = ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/file/list', { params: cleanParams(buildListParams()) })
    dataSource.value = res.data.records
    pagination.total = res.data.total
    await syncImagePreviewMap(dataSource.value)
  } finally {
    loading.value = false
  }
}

const fetchSuffixOptions = async () => {
  const res = await request.get('/system/file/suffix-options')
  suffixOptions.value = (res.data || []).map(item => ({
    label: `${item.label} (${item.count})`,
    value: item.value
  }))
}

const initLineChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.map(item => item.date) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: data.map(item => item.value), areaStyle: { opacity: 0.1 }, itemStyle: { color: '#0ea5e9' } }]
  })
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

const initBarChart = (el, data) => {
  initChart(el, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(item => item.name), inverse: true },
    series: [{ type: 'bar', data: data.map(item => item.value), itemStyle: { color: STATUS_COLORS.warning, borderRadius: [0, 4, 4, 0] } }]
  })
}

const fetchReportData = async () => {
  try {
    const params = cleanParams(buildReportParams())
    const statsRes = await request.get('/system/file/stats', { params })
    statsData.value = statsRes.data || { totalCount: 0, totalSize: 0, todayCount: 0, todaySize: 0 }

    const trendRes = await request.get('/system/file/stats/trend', { params: { ...params, days: 30 } })
    nextTick(() => { initLineChart(uploadTrendRef.value, trendRes.data || []) })

    const typeRes = await request.get('/system/file/stats/type', { params })
    nextTick(() => {
      initPieChart(storageTypePieRef.value, typeRes.data?.storageTypePie || [])
      initBarChart(fileTypeTopRef.value, typeRes.data?.suffixTop || [])
    })
  } catch(e) {
    console.error("Report data error", e)
  }
}

const openReportDrawer = () => {
  reportVisible.value = true
  nextTick(() => {
    fetchReportData()
  })
}

const resetQuery = () => {
  queryParams.fileName = ''
  queryParams.storageType = undefined
  queryParams.fileSuffixes = []
  dateRange.value = []
  queryParams.beginTime = ''
  queryParams.endTime = ''
  queryParams.pageNum = 1
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchData()
}

const handleExport = async () => {
  const exportParams = cleanParams({
    ...buildReportParams(),
    format: 'xlsx'
  })

  await downloadByRequest({
    url: '/system/file/export',
    params: exportParams,
    fileName: 'file-report.xlsx'
  })
}

const handleDownload = async (record) => {
  await downloadByRequest({
    url: `/system/file/download/${record.id}`,
    fileName: record.fileName || 'download'
  })
}

const openPreview = (record) => {
  previewRecord.value = record
  previewVisible.value = true
}

const customUpload = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await request.post('/system/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    message.success('上传成功')
    onSuccess(res, file)
    fetchSuffixOptions()
    fetchData()
  } catch (err) {
    onError(err)
  }
}

const handleDelete = async (id) => {
  await request.delete(`/system/file/delete/${id}`)
  message.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchSuffixOptions()
  fetchData()
})

onBeforeUnmount(() => {
  clearImagePreviewMap()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 20px; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.slide-in-1 { animation: slide-up 0.6s both 0.1s; }
.slide-in-2 { animation: slide-up 0.6s both 0.2s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.delete-link { color: var(--color-danger); }
.url-link { color: var(--color-link); text-decoration: underline; display: flex; align-items: center; }
.report-button,
.report-button:hover,
.report-button:focus {
  background-color: var(--color-success) !important;
  border-color: var(--color-success) !important;
  color: #ffffff !important;
}
.report-button :deep(span),
.report-button :deep(.anticon) {
  color: #ffffff !important;
}
.thumbnail { height: 30px; border-radius: 4px; object-fit: cover; border: 1px solid #e2e8f0; }
.file-name-cell { display: inline-flex; align-items: center; gap: 8px; min-width: 0; }
.file-name-cell :deep(.anticon) { font-size: 17px; flex: 0 0 auto; }
.file-name-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.report-container { padding: 10px; }
.stat-card {
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
}
.stat-card.today {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border-color: #d1fae5;
}
.chart-card {
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.info-card {
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

:deep(.ant-table) { background: transparent !important; }
:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
.dark-mode .thumbnail { border-color: rgba(255,255,255,0.1); }
.dark-mode .stat-card, .dark-mode .chart-card { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); border-color: #334155; }
.dark-mode .stat-card.today { background: linear-gradient(135deg, #064e3b 0%, #022c22 100%); border-color: #065f46; }
.dark-mode .info-card { background: #0f172a; border-color: #334155; }
</style>
