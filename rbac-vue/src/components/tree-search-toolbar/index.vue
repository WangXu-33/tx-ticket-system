<template>
  <div class="tree-search-toolbar">
    <div v-if="label" class="toolbar-label">{{ label }}</div>
    <div class="toolbar-controls">
      <a-input
        :value="modelValue"
        :placeholder="placeholder"
        allow-clear
        :style="{ width: inputWidth }"
        @update:value="value => emit('update:modelValue', value)"
        @pressEnter="emit('search')"
      />
      <a-button type="primary" @click="emit('search')">
        <template #icon><SearchOutlined /></template>查询
      </a-button>
      <a-button @click="emit('reset')">重置</a-button>
      <slot />
    </div>
  </div>
</template>

<script setup>
import { SearchOutlined } from '@ant-design/icons-vue'

defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入关键词'
  },
  label: {
    type: String,
    default: ''
  },
  inputWidth: {
    type: String,
    default: '260px'
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'reset'])
</script>

<style scoped>
.tree-search-toolbar {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
}

.toolbar-label {
  color: var(--text-main);
  font-size: 14px;
  white-space: nowrap;
}

.toolbar-label::after {
  content: ':';
  margin-left: 2px;
}

.toolbar-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
