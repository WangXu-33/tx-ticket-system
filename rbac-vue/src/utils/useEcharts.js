import { onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

export function useEcharts() {
  const charts = []
  const pendingTimers = new WeakMap()

  const initChart = (el, options) => {
    if (!el) return null
    if (el.clientWidth === 0 || el.clientHeight === 0) {
      const existingTimer = pendingTimers.get(el)
      if (existingTimer) {
        clearTimeout(existingTimer)
      }
      const retryTimer = setTimeout(() => {
        pendingTimers.delete(el)
        initChart(el, options)
      }, 80)
      pendingTimers.set(el, retryTimer)
      return null
    }
    let chart = echarts.getInstanceByDom(el)
    if (!chart) {
      chart = echarts.init(el)
      charts.push(chart)
    }
    chart.setOption(options)
    return chart
  }

  const handleResize = () => {
    charts.forEach(c => c.resize())
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    charts.forEach(c => c.dispose())
    charts.length = 0
  })

  return {
    initChart,
    echarts
  }
}
