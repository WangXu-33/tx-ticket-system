<template>
  <section class="ticket-page-header">
    <div class="ticket-page-header__main">
      <button v-if="showBack" class="ticket-page-header__back" type="button" @click="$emit('back')">
        <arrow-left-outlined />
        <span>{{ backText }}</span>
      </button>
      <div class="ticket-page-header__copy">
        <h1>{{ title }}</h1>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.actions" class="ticket-page-header__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup>
import { ArrowLeftOutlined } from '@ant-design/icons-vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: false
  },
  backText: {
    type: String,
    default: '返回'
  }
})

defineEmits(['back'])
</script>

<style scoped>
.ticket-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.ticket-page-header__main {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.ticket-page-header__copy {
  display: grid;
  gap: 4px;
}

.ticket-page-header__copy h1 {
  margin: 0;
  color: #172033;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.ticket-page-header__copy p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.ticket-page-header__back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.ticket-page-header__back:hover {
  color: #1677ff;
}

.ticket-page-header__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 960px) {
  .ticket-page-header {
    flex-direction: column;
  }

  .ticket-page-header__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
