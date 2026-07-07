<template>
  <a-card :bordered="false" class="ticket-filter-card">
    <template #title>
      <div class="ticket-filter-card__title">
        <span>{{ title }}</span>
        <slot name="summary" />
      </div>
    </template>
    <template #extra>
      <a-button type="link" class="ticket-filter-card__toggle" @click="toggleCollapsed">
        {{ innerCollapsed ? '展开筛选' : '收起筛选' }}
      </a-button>
    </template>
    <div v-show="!innerCollapsed" class="ticket-filter-card__body">
      <slot />
    </div>
  </a-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: '筛选条件'
  },
  collapsed: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:collapsed'])

const innerCollapsed = computed(() => props.collapsed)

const toggleCollapsed = () => {
  emit('update:collapsed', !props.collapsed)
}
</script>

<style scoped>
.ticket-filter-card {
  margin-bottom: 16px;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.ticket-filter-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #111827;
  font-weight: 600;
}

.ticket-filter-card__toggle {
  padding-inline: 0;
}

.ticket-filter-card__body {
  padding-top: 4px;
}

.ticket-filter-card :deep(.compact-filter-form) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
}

.ticket-filter-card :deep(.compact-filter-form .ant-form-item) {
  margin-bottom: 0;
}

.ticket-filter-card :deep(.compact-filter-form .ant-form-item-label) {
  padding-bottom: 6px;
}
</style>
