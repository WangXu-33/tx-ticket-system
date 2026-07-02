<template>
  <a-select
    v-model:value="innerValue"
    :mode="multiple ? 'multiple' : undefined"
    :options="dictOptions"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :disabled="disabled"
    :max-tag-count="maxTagCount"
    style="width: 100%"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useDict } from '@/utils/useDict'

const props = defineProps({
  value: { type: [String, Number, Array], default: undefined },
  typeCode: { type: String, required: true },
  multiple: { type: Boolean, default: false },
  placeholder: { type: String, default: '请选择' },
  allowClear: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  onlyEnabled: { type: Boolean, default: true },
  maxTagCount: { type: [Number, String], default: 'responsive' }
})

const emit = defineEmits(['update:value', 'change'])
const { getDictOptions } = useDict()
const dictOptions = ref([])

const innerValue = computed({
  get: () => props.value,
  set: (nextValue) => {
    emit('update:value', nextValue)
    emit('change', nextValue)
  }
})

const loadOptions = async () => {
  if (!props.typeCode) {
    dictOptions.value = []
    return
  }
  dictOptions.value = await getDictOptions(props.typeCode, { onlyEnabled: props.onlyEnabled })
}

watch(() => props.typeCode, loadOptions)
onMounted(loadOptions)
</script>
