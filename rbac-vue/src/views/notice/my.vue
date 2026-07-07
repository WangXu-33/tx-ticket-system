<template>
  <div class="my-notice-page">
    <a-card class="inbox-header slide-in-1" :bordered="false">
      <a-space wrap>
        <a-input v-model:value="query.keyword" allow-clear placeholder="搜索我的公告" style="width: 260px" @pressEnter="fetchList" />
        <a-select v-model:value="query.readFlag" allow-clear placeholder="阅读状态" style="width: 140px">
          <a-select-option :value="0">未读</a-select-option>
          <a-select-option :value="1">已读</a-select-option>
        </a-select>
        <a-button type="primary" @click="fetchList">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </a-space>
      <a-statistic title="未读公告" :value="unreadCount" class="unread-stat" />
    </a-card>

    <a-card class="inbox-card slide-in-2" :bordered="false">
      <a-list :data-source="noticeList" :loading="loading" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item class="notice-item" @click="openDetail(item)">
            <template #extra>
              <a-tag :color="item.readFlag === 1 ? 'success' : 'warning'">
                {{ item.readFlag === 1 ? '已读' : '未读' }}
              </a-tag>
            </template>
            <a-list-item-meta>
              <template #title>
                <span class="notice-title">{{ item.title }}</span>
                <a-tag style="margin-left: 8px;">{{ item.noticeType }}</a-tag>
                <a-tag :color="priorityColor(item.priority)">{{ item.priority }}</a-tag>
              </template>
              <template #description>
                接收时间：{{ formatDateTime(item.receiveTime) }}　发布时间：{{ formatDateTime(item.publishTime) }}
              </template>
            </a-list-item-meta>
            <div class="notice-preview">{{ item.content }}</div>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <a-modal v-model:open="detailModal.open" :title="detailModal.data.title" width="720" @ok="detailModal.open = false">
      <div class="detail-meta">
        <a-tag>{{ detailModal.data.noticeType }}</a-tag>
        <a-tag :color="priorityColor(detailModal.data.priority)">{{ detailModal.data.priority }}</a-tag>
        <span>发布时间：{{ formatDateTime(detailModal.data.publishTime) }}</span>
      </div>
      <div class="detail-content">{{ detailModal.data.content }}</div>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/api/request'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const noticeList = ref([])
const unreadCount = ref(0)
const query = reactive({ keyword: '', readFlag: undefined })
const detailModal = reactive({ open: false, data: {} })

const priorityColor = priority => {
  if (priority === '紧急') return 'error'
  if (priority === '重要') return 'warning'
  return 'blue'
}

const fetchUnreadCount = async () => {
  const res = await request.get('/system/notice/my/unread-count')
  unreadCount.value = res.data || 0
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/notice/my', { params: query })
    noticeList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.keyword = ''
  query.readFlag = undefined
  fetchList()
}

const openDetail = async item => {
  detailModal.data = item
  detailModal.open = true
  if (item.readFlag === 0) {
    await request.post(`/system/notice/my/read/${item.id}`)
    item.readFlag = 1
    fetchUnreadCount()
  }
}

onMounted(() => {
  fetchUnreadCount()
  fetchList()
})
</script>

<style scoped>
.my-notice-page { display: flex; flex-direction: column; gap: 16px; }
.inbox-header, .inbox-card { border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.inbox-header { display: flex; align-items: center; justify-content: space-between; }
.unread-stat { min-width: 120px; text-align: right; }
.notice-item { cursor: pointer; border-radius: 8px; padding: 16px !important; transition: background 0.2s; }
.notice-item:hover { background: rgba(5, 150, 105, 0.05); }
.notice-title { font-weight: 700; color: var(--text-main); }
.notice-preview { color: var(--text-sub); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; white-space: pre-wrap; }
.detail-meta { display: flex; align-items: center; gap: 8px; color: var(--text-sub); margin-bottom: 16px; }
.detail-content { white-space: pre-wrap; line-height: 1.8; color: var(--text-main); }
.slide-in-1 { animation: slide-up 0.5s both; }
.slide-in-2 { animation: slide-up 0.5s both 0.08s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.dark-mode .inbox-header, .dark-mode .inbox-card { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
</style>
