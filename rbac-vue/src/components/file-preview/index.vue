<template>
  <a-modal
    :open="open"
    :title="previewTitle"
    width="86vw"
    :footer="null"
    :destroyOnClose="true"
    class="file-preview-modal"
    @cancel="handleClose"
  >
    <div class="file-preview-wrap">
      <a-spin :spinning="loading">
        <div v-if="errorMessage" class="preview-empty">
          <file-unknown-outlined />
          <div>{{ errorMessage }}</div>
          <a-button v-if="record?.id" type="primary" @click="emitDownload">下载文件</a-button>
        </div>

        <img v-else-if="previewType === 'image'" :src="previewUrl" class="image-preview" />

        <vue-office-pdf
          v-else-if="previewType === 'pdf'"
          :src="previewUrl"
          class="office-preview"
          @error="handleRenderError"
        />

        <vue-office-docx
          v-else-if="previewType === 'docx'"
          :src="officeSource"
          class="office-preview"
          @error="handleRenderError"
        />

        <vue-office-excel
          v-else-if="previewType === 'excel'"
          :src="officeSource"
          class="office-preview"
          @error="handleRenderError"
        />

        <vue-office-pptx
          v-else-if="previewType === 'pptx'"
          :src="officeSource"
          class="office-preview"
          @error="handleRenderError"
        />

        <div v-else class="preview-empty">
          <file-unknown-outlined />
          <div>该文件类型暂不支持在线预览，请下载后查看。</div>
          <a-button v-if="record?.id" type="primary" @click="emitDownload">下载文件</a-button>
        </div>
      </a-spin>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch, onBeforeUnmount } from 'vue'
import { FileUnknownOutlined } from '@ant-design/icons-vue'
import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'
import { getPreviewBlob } from '@/utils/file-preview'

const VueOfficeDocx = defineAsyncComponent(() => import('@vue-office/docx'))
const VueOfficeExcel = defineAsyncComponent(() => import('@vue-office/excel'))
const VueOfficePdf = defineAsyncComponent(() => import('@vue-office/pdf'))
const VueOfficePptx = defineAsyncComponent(() => import('@vue-office/pptx'))

const props = defineProps({
  open: { type: Boolean, default: false },
  record: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'download'])

const loading = ref(false)
const previewUrl = ref('')
const officeSource = ref(null)
const errorMessage = ref('')

const suffix = computed(() => (props.record?.fileSuffix || '').toLowerCase())
const previewTitle = computed(() => props.record?.fileName || '文件预览')

const previewType = computed(() => {
  const ext = suffix.value
  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'].includes(ext)) return 'image'
  if (ext === '.pdf') return 'pdf'
  if (ext === '.docx') return 'docx'
  if (['.xlsx', '.xls'].includes(ext)) return 'excel'
  if (ext === '.pptx') return 'pptx'
  return 'unsupported'
})

const clearPreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  officeSource.value = null
  errorMessage.value = ''
}

const loadPreview = async () => {
  clearPreview()
  if (!props.open || !props.record?.id) {
    return
  }
  if (previewType.value === 'unsupported') {
    return
  }

  loading.value = true
  try {
    const blob = await getPreviewBlob(props.record.id)
    if (!blob) {
      errorMessage.value = '文件预览数据为空'
      return
    }
    if (previewType.value === 'image' || previewType.value === 'pdf') {
      previewUrl.value = URL.createObjectURL(blob)
    } else {
      officeSource.value = await blob.arrayBuffer()
    }
  } catch {
    errorMessage.value = '文件加载失败，请下载后查看'
  } finally {
    loading.value = false
  }
}

const handleRenderError = () => {
  errorMessage.value = '文件渲染失败，请下载后查看'
}

const handleClose = () => {
  emit('update:open', false)
}

const emitDownload = () => {
  emit('download', props.record)
}

watch(
  () => [props.open, props.record?.id],
  loadPreview,
  { immediate: true }
)

onBeforeUnmount(clearPreview)
</script>

<style scoped>
.file-preview-wrap {
  min-height: 62vh;
}

.image-preview {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
  object-fit: contain;
  border-radius: 10px;
  background: #f8fafc;
}

.office-preview {
  width: 100%;
  height: 72vh;
  border: 0;
  border-radius: 10px;
  background: #ffffff;
}

.preview-empty {
  min-height: 58vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-sub);
  font-size: 14px;
}

.preview-empty :deep(.anticon) {
  font-size: 42px;
  color: var(--color-info);
}
</style>
