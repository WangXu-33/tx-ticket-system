<template>
  <div class="external-frame-page">
    <a-card :bordered="false" class="frame-card">
      <div class="frame-toolbar">
        <div class="frame-meta">
          <div class="frame-title">{{ pageTitle }}</div>
          <div class="frame-url">{{ targetUrl }}</div>
        </div>
        <a-space>
          <a-button @click="reloadFrame">
            刷新
          </a-button>
          <a-button type="primary" ghost @click="openInNewTab">
            新窗口打开
          </a-button>
        </a-space>
      </div>

      <a-alert
        v-if="loadFailed"
        type="warning"
        show-icon
        class="frame-alert"
        message="当前外部系统可能禁止被 iframe 嵌入"
        description="如果页面一直空白或被浏览器拦截，请使用“新窗口打开”。这通常是目标系统的安全策略限制，不是本系统故障。"
      />

      <div class="frame-shell">
        <div v-if="loading" class="frame-loading">
          <a-spin size="large" />
          <span>正在加载外部系统...</span>
        </div>
        <iframe
          ref="frameRef"
          class="external-frame"
          :src="frameSrc"
          frameborder="0"
          @load="handleFrameLoad"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'

const route = useRoute()
const frameRef = ref(null)
const loading = ref(true)
const loadFailed = ref(false)
const refreshSeed = ref(Date.now())

const targetUrl = computed(() => {
  const value = typeof route.query.url === 'string' ? route.query.url.trim() : ''
  return /^(https?:)?\/\//i.test(value) ? value : ''
})

const pageTitle = computed(() => {
  return typeof route.query.title === 'string' && route.query.title.trim()
    ? route.query.title.trim()
    : '外部系统'
})

const frameSrc = computed(() => {
  if (!targetUrl.value) return ''
  const separator = targetUrl.value.includes('?') ? '&' : '?'
  return `${targetUrl.value}${separator}_iframe_ts=${refreshSeed.value}`
})

const reloadFrame = () => {
  if (!targetUrl.value) {
    message.warning('未找到可加载的外链地址')
    return
  }
  loading.value = true
  loadFailed.value = false
  refreshSeed.value = Date.now()
}

const openInNewTab = () => {
  if (!targetUrl.value) {
    message.warning('未找到可打开的外链地址')
    return
  }
  window.open(targetUrl.value, '_blank', 'noopener,noreferrer')
}

const handleFrameLoad = () => {
  loading.value = false
  try {
    // 若跨域可访问这里通常会报错；报错本身不代表失败，所以只用于探测“空白未结束”的场景。
    void frameRef.value?.contentWindow?.location?.href
  } catch (error) {
    // 跨域 iframe 正常会进入这里，不视为失败。
  }
}

watch(targetUrl, value => {
  loading.value = true
  loadFailed.value = !value
  refreshSeed.value = Date.now()
}, { immediate: true })

// 目标系统如果被禁止嵌入，浏览器通常不会抛标准错误给 Vue。
// 这里给一个超时兜底，避免用户长时间看到空白。
watch(frameSrc, value => {
  if (!value) return
  const currentSeed = refreshSeed.value
  window.setTimeout(() => {
    if (loading.value && currentSeed === refreshSeed.value) {
      loadFailed.value = true
    }
  }, 6000)
}, { immediate: true })
</script>

<style scoped>
.external-frame-page {
  min-height: calc(100vh - 112px);
}

.frame-card {
  border-radius: 14px;
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.06);
}

.frame-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.frame-meta {
  min-width: 0;
}

.frame-title {
  color: var(--text-main);
  font-size: 18px;
  font-weight: 700;
}

.frame-url {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.frame-alert {
  margin-bottom: 16px;
}

.frame-shell {
  position: relative;
  min-height: 72vh;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(255, 255, 255, 0.96) 100%);
}

.frame-loading {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--text-secondary);
  backdrop-filter: blur(3px);
}

.external-frame {
  width: 100%;
  min-height: 72vh;
  background: #fff;
}

.dark-mode .frame-card {
  box-shadow: 0 14px 36px rgba(2, 6, 23, 0.24);
}

.dark-mode .frame-shell {
  border-color: rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.96) 0%, rgba(2, 6, 23, 0.96) 100%);
}

.dark-mode .frame-loading {
  background: rgba(2, 6, 23, 0.72);
}

@media (max-width: 768px) {
  .frame-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
