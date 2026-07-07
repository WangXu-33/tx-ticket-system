<template>
  <div class="dashboard-container">
    <!-- 顶部概览卡片 -->
    <a-row :gutter="[16, 16]" class="overview-row slide-in-1">
      <a-col :xs="12" :sm="12" :md="8" :lg="6" :xl="4" v-for="(item, index) in overviewCards" :key="index">
        <a-card class="stat-card" :bordered="false">
          <div class="stat-content">
            <div class="stat-icon-wrapper" :style="{ backgroundColor: item.bgColor, color: item.color }">
              <component :is="item.icon" />
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">
                <span class="num">{{ item.value }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 中间趋势图 -->
    <a-row :gutter="[16, 16]" class="trend-row slide-in-2" style="margin-top: 16px;">
      <a-col :span="8">
        <a-card title="近30天登录趋势" :bordered="false" class="chart-card">
          <div ref="loginTrendRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="近30天操作趋势" :bordered="false" class="chart-card">
          <div ref="operTrendRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="近30天文件上传趋势" :bordered="false" class="chart-card">
          <div ref="fileUploadTrendRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 底部四个分布/排行图 -->
    <a-row :gutter="[16, 16]" class="distribution-row slide-in-3" style="margin-top: 16px;">
      <a-col :span="6">
        <a-card title="登录状态分布" :bordered="false" class="chart-card">
          <div ref="loginStatusPieRef" style="height: 280px;"></div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="文件存储分布" :bordered="false" class="chart-card">
          <div ref="fileStoragePieRef" style="height: 280px;"></div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="部门用户排行" :bordered="false" class="chart-card">
          <div ref="deptUserTopRef" style="height: 280px;"></div>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="角色用户排行" :bordered="false" class="chart-card">
          <div ref="roleUserTopRef" style="height: 280px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 最底部快捷入口及服务器监控 -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px;">
      <a-col :span="16">
        <a-card class="monitor-card slide-in-3" :bordered="false">
          <template #title>
            <div class="monitor-title">
              <span>服务器运行探针 (实时)</span>
              <a-tag :color="sseStatus.color">{{ sseStatus.text }}</a-tag>
            </div>
          </template>
          <a-row :gutter="24">
            <a-col :span="6" style="text-align: center;">
              <a-progress type="dashboard" :percent="serverStats.cpuUsage" :size="100" :stroke-color="{'0%': '#108ee9', '100%': '#87d068'}" />
              <div class="monitor-label">CPU负载</div>
            </a-col>
            <a-col :span="6" style="text-align: center;">
              <a-progress type="dashboard" :percent="serverStats.memoryUsage" :size="100" :stroke-color="{ '0%': 'var(--color-warning)', '100%': 'var(--color-danger)' }" />
              <div class="monitor-label">内存使用率</div>
            </a-col>
            <a-col :span="6" style="text-align: center;">
              <a-progress type="dashboard" :percent="serverStats.diskUsage" :size="100" :stroke-color="{ '0%': 'var(--color-accent)', '100%': 'var(--color-danger)' }" />
              <div class="monitor-label">磁盘使用率</div>
            </a-col>
            <a-col :span="6" style="text-align: center;">
               <div style="margin-top: 20px;">
                <div>JVM已用: {{ serverStats.jvmUsed }} MB</div>
                <div style="margin-top: 10px;">JVM最大: {{ serverStats.jvmMax }} MB</div>
               </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="快捷入口" class="tool-card slide-in-3" :bordered="false">
          <a-row :gutter="16">
            <a-col :span="8" v-for="(menu, idx) in shortcuts" :key="idx" style="text-align: center; margin-bottom: 16px;">
               <a-button type="default" style="width: 100%; height: 60px; padding: 0" @click="$router.push(menu.path)">
                 <div style="display: flex; flex-direction: column; align-items: center; justify-content: center;">
                   <component :is="menu.icon" style="font-size: 20px; margin-bottom: 4px;" />
                   <span style="font-size: 12px;">{{ menu.name }}</span>
                 </div>
               </a-button>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  UserOutlined, VerifiedOutlined, ApartmentOutlined, SolutionOutlined,
  MenuOutlined, SafetyCertificateOutlined, CloudOutlined, LoginOutlined,
  ProfileOutlined, RadarChartOutlined, BookOutlined
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { useEcharts } from '@/utils/useEcharts'
import { createSseClient } from '@/utils/sseClient'
import { LIGHT_THEME_PALETTE, STATUS_COLORS } from '@/theme/palette'

const { initChart } = useEcharts()

// 概览卡片配置
const overviewCards = ref([
  { title: '用户总数', value: 0, icon: UserOutlined, color: STATUS_COLORS.info, bgColor: LIGHT_THEME_PALETTE.infoSoft, key: 'userTotal' },
  { title: '正常用户', value: 0, icon: VerifiedOutlined, color: STATUS_COLORS.success, bgColor: LIGHT_THEME_PALETTE.successSoft, key: 'enabledUserTotal' },
  { title: '部门数量', value: 0, icon: ApartmentOutlined, color: STATUS_COLORS.warning, bgColor: LIGHT_THEME_PALETTE.warningSoft, key: 'deptTotal' },
  { title: '角色数量', value: 0, icon: SolutionOutlined, color: STATUS_COLORS.accent, bgColor: LIGHT_THEME_PALETTE.accentSoft, key: 'roleTotal' },
  { title: '菜单数量', value: 0, icon: MenuOutlined, color: LIGHT_THEME_PALETTE.link, bgColor: LIGHT_THEME_PALETTE.infoSoft, key: 'menuTotal' },
  { title: '权限数量', value: 0, icon: SafetyCertificateOutlined, color: '#ec4899', bgColor: '#fdf2f8', key: 'permissionTotal' },
  { title: '文件总数', value: 0, icon: CloudOutlined, color: '#0ea5e9', bgColor: '#e0f2fe', key: 'fileTotal' },
  { title: '在线用户', value: 0, icon: RadarChartOutlined, color: '#14b8a6', bgColor: '#ecfdf5', key: 'onlineUserTotal' },
  { title: '今日登录', value: 0, icon: LoginOutlined, color: '#f43f5e', bgColor: '#fff1f2', key: 'todayLoginTotal' },
  { title: '今日操作', value: 0, icon: ProfileOutlined, color: '#84cc16', bgColor: '#f0fdf4', key: 'todayOperTotal' }
])

const serverStats = ref({ cpuUsage: 0, memoryUsage: 0, diskUsage: 0, jvmUsed: 0, jvmMax: 0 })
const realtimeStatus = ref('closed')
let realtimeClient = null

const sseStatus = computed(() => {
  const statusMap = {
    connecting: { text: '连接中', color: 'processing' },
    connected: { text: '已连接', color: 'success' },
    reconnecting: { text: '重连中', color: 'warning' },
    disconnected: { text: '已断开', color: 'error' },
    closed: { text: '未连接', color: 'default' }
  }
  return statusMap[realtimeStatus.value] || statusMap.closed
})
const shortcuts = [
  { name: '用户管理', path: '/system/user', icon: UserOutlined },
  { name: '角色管理', path: '/system/role', icon: SolutionOutlined },
  { name: '部门管理', path: '/system/dept', icon: ApartmentOutlined },
  { name: '文件管理', path: '/system/file', icon: CloudOutlined },
  { name: '登录日志', path: '/log/loginlog', icon: LoginOutlined },
  { name: '字典管理', path: '/system/dict', icon: BookOutlined }
]

// ECharts 实例
const loginTrendRef = ref(null)
const operTrendRef = ref(null)
const fileUploadTrendRef = ref(null)
const loginStatusPieRef = ref(null)
const fileStoragePieRef = ref(null)
const deptUserTopRef = ref(null)
const roleUserTopRef = ref(null)

const initLineChart = (el, title, data, color) => {
  initChart(el, {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.map(item => item.date) },
    yAxis: { type: 'value' },
    series: [{
      name: title, type: 'line', smooth: true,
      areaStyle: { opacity: 0.1 },
      itemStyle: { color: color },
      data: data.map(item => item.value)
    }]
  })
}

const initPieChart = (el, title, data) => {
  initChart(el, {
    tooltip: { trigger: 'item' },
    legend: { bottom: '0', left: 'center' },
    series: [{
      name: title, type: 'pie', radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
      labelLine: { show: false },
      data: data
    }]
  })
}

const initBarChart = (el, title, data, color) => {
  initChart(el, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(item => item.name), inverse: true },
    series: [{
      name: title, type: 'bar',
      itemStyle: { color: color, borderRadius: [0, 4, 4, 0] },
      data: data.map(item => item.value)
    }]
  })
}

const fetchDashboardData = async () => {
  try {
    // 1. Overview
    const overviewRes = await request.get('/system/dashboard/overview')
    overviewCards.value.forEach(card => {
      if (overviewRes.data[card.key] !== undefined) {
        card.value = overviewRes.data[card.key]
      }
    })

    // 2. Trend (3 charts)
    const trendRes = await request.get('/system/dashboard/trend?days=30')
    nextTick(() => {
      initLineChart(loginTrendRef.value, '登录次数', trendRes.data.loginTrend || [], STATUS_COLORS.info)
      initLineChart(operTrendRef.value, '操作次数', trendRes.data.operTrend || [], STATUS_COLORS.success)
      initLineChart(fileUploadTrendRef.value, '文件上传', trendRes.data.fileUploadTrend || [], STATUS_COLORS.warning)
    })

    // 3. Distribution (4 charts)
    const distRes = await request.get('/system/dashboard/distribution')
    nextTick(() => {
      initPieChart(loginStatusPieRef.value, '登录状态', distRes.data.loginStatusPie || [])
      initPieChart(fileStoragePieRef.value, '存储分布', distRes.data.fileStoragePie || [])
      initBarChart(deptUserTopRef.value, '部门用户', distRes.data.deptUserTop || [], STATUS_COLORS.accent)
      initBarChart(roleUserTopRef.value, '角色用户', distRes.data.roleUserTop || [], '#ec4899')
    })

    // 4. Server
    const serverRes = await request.get('/system/dashboard/server')
    serverStats.value = serverRes.data || { cpuUsage: 0, memoryUsage: 0, diskUsage: 0, jvmUsed: 0, jvmMax: 0 }

  } catch (e) {
    console.error("Dashboard data error", e)
  }
}

const connectRealtimeMetrics = () => {
  // 页面初次仍走 HTTP 拉全量仪表盘；SSE 只负责后续高频变化的服务器运行指标。
  realtimeClient = createSseClient({
    onStatusChange: status => {
      realtimeStatus.value = status
    }
  })
  realtimeClient.on('server-metrics', data => {
    if (data) {
      serverStats.value = data
    }
  })
  realtimeClient.connect()
}

onMounted(() => {
  fetchDashboardData()
  connectRealtimeMetrics()
})

onUnmounted(() => {
  if (realtimeClient) {
    realtimeClient.close()
    realtimeClient = null
  }
})
</script>

<style scoped>
.dashboard-container { padding: 0; background: transparent; }

.stat-card {
  border-radius: 8px;
  background: var(--bg-card);
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0,0,0,0.02);
  height: 90px;
  display: flex;
  align-items: center;
}
.stat-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.05); transform: translateY(-2px); }

.stat-content { display: flex; align-items: center; width: 100%; padding: 0 10px; }
.stat-icon-wrapper {
  width: 48px; height: 48px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; margin-right: 16px;
}
.stat-info { flex: 1; }
.stat-title { font-size: 13px; color: var(--text-sub); margin-bottom: 4px; }
.stat-value .num { font-size: 22px; font-weight: bold; color: var(--text-main); line-height: 1; }

.chart-card { border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.02); }
.monitor-card { border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.02); height: 100%; }
.tool-card { border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.02); height: 100%; }

.monitor-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.monitor-label { margin-top: 12px; font-weight: 600; color: var(--text-main); font-size: 13px; }

.slide-in-1 { animation: slide-up 0.4s both; }
.slide-in-2 { animation: slide-up 0.4s both 0.1s; }
.slide-in-3 { animation: slide-up 0.4s both 0.2s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

/* 暗黑模式适配 */
.dark-mode .stat-card, .dark-mode .chart-card, .dark-mode .monitor-card, .dark-mode .tool-card {
  background: #0f172a; border: 1px solid rgba(255,255,255,0.05);
}
.dark-mode .stat-icon-wrapper { filter: brightness(0.8); }
</style>
