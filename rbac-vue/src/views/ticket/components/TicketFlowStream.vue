<template>
  <div class="flow-stream">
    <a-empty v-if="!flows.length" description="暂无流程记录" />
    <div v-else class="flow-list">
      <article v-for="flow in flows" :key="flow.id" class="flow-card">
        <div class="flow-card__line"></div>
        <div class="flow-card__body">
          <div class="flow-card__head">
            <strong>{{ actionText(flow.action) }}</strong>
            <span>{{ formatDateTime(flow.createTime) }}</span>
          </div>
          <div class="flow-card__meta">
            <span>处理人：{{ flow.operatorName || '-' }}</span>
            <span v-if="flow.visibleScope === 'internal'">范围：内部留痕</span>
            <span v-else>范围：客户可见</span>
          </div>
          <p class="flow-card__content">{{ flow.content || '未填写处理说明' }}</p>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { formatDateTime } from '@/utils/datetime'

defineProps({
  flows: {
    type: Array,
    default: () => []
  },
  actionText: {
    type: Function,
    required: true
  }
})
</script>

<style scoped>
.flow-list {
  display: grid;
  gap: 14px;
}

.flow-card {
  position: relative;
  padding-left: 20px;
}

.flow-card:last-child .flow-card__line {
  bottom: 40px;
}

.flow-card__line {
  position: absolute;
  left: 5px;
  top: 10px;
  bottom: -18px;
  width: 2px;
  background: #dbe7f3;
}

.flow-card__body {
  position: relative;
  padding: 16px 18px;
  border: 1px solid #e5edf5;
  border-radius: 16px;
  background: #ffffff;
}

.flow-card__body::before {
  content: '';
  position: absolute;
  left: -18px;
  top: 18px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #0f766e;
  box-shadow: 0 0 0 5px #ecfeff;
}

.flow-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.flow-card__head strong {
  color: #0f172a;
  font-size: 15px;
}

.flow-card__head span,
.flow-card__meta {
  color: #64748b;
  font-size: 12px;
}

.flow-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.flow-card__content {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.75;
  white-space: pre-wrap;
}
</style>
