<template>
  <component
    :is="iconMeta.component"
    v-if="iconMeta?.type === 'component' && iconMeta.component"
    :style="componentStyle"
    class="menu-icon-renderer"
  />
  <img
    v-else-if="iconMeta?.type === 'image' && iconMeta.src"
    :src="iconMeta.src"
    :alt="iconMeta.label || iconKey"
    :style="imageStyle"
    class="menu-icon-renderer menu-icon-image"
  />
  <span v-else class="menu-icon-fallback">{{ fallbackText }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { resolveMenuIconMeta } from '@/config/menu-icons'

const props = defineProps({
  iconKey: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 18
  },
  fallbackText: {
    type: String,
    default: ''
  }
})

const iconMeta = computed(() => resolveMenuIconMeta(props.iconKey))

const componentStyle = computed(() => ({
  fontSize: `${props.size}px`,
  lineHeight: 1
}))

const imageStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))
</script>

<style scoped>
.menu-icon-renderer {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.menu-icon-image {
  object-fit: contain;
}

.menu-icon-fallback {
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
