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

        <iframe
          v-else-if="previewType === 'pdf'"
          :src="previewUrl"
          class="office-preview"
          title="PDF 文件预览"
        />

        <video
          v-else-if="previewType === 'video'"
          :src="previewUrl"
          class="video-preview"
          controls
        />

        <pre v-else-if="previewType === 'text'" class="text-preview">{{ textContent }}</pre>

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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { FileUnknownOutlined } from '@ant-design/icons-vue'
import { getPreviewBlob } from '@/utils/file-preview'

const props = defineProps({
  open: { type: Boolean, default: false },
  record: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'download'])

const loading = ref(false)
const previewUrl = ref('')
const textContent = ref('')
const errorMessage = ref('')

const suffix = computed(() => {
  const explicitSuffix = props.record?.fileSuffix || ''
  if (explicitSuffix) {
    return explicitSuffix.toLowerCase()
  }
  const fileName = props.record?.fileName || ''
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index).toLowerCase() : ''
})

const previewTitle = computed(() => props.record?.fileName || '文件预览')

const previewType = computed(() => {
  const ext = suffix.value
  if (['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'].includes(ext)) return 'image'
  if (ext === '.pdf') return 'pdf'
  if (['.mp4', '.webm', '.mov', '.avi'].includes(ext)) return 'video'
  if (['.md', '.txt', '.log', '.json', '.xml', '.csv'].includes(ext)) return 'text'
  return 'unsupported'
})

const clearPreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  textContent.value = ''
  errorMessage.value = ''
}

/**
 * 修改时间：2026-07-02
 * 功能说明：按文件类型加载在线预览内容，支持图片、PDF、视频、Markdown 和常见文本。
 * 入参：无，读取弹窗打开状态和文件记录。
 * 出参：无，写入预览 URL 或文本内容。
 * 异常场景：文件不存在、下载失败或类型不支持时展示可恢复提示。
 */
const loadPreview = async () => {
  clearPreview()
  if (!props.open || !props.record?.id || previewType.value === 'unsupported') {
    return
  }

  loading.value = true
  try {
    const blob = await getPreviewBlob(props.record.id)
    if (!blob) {
      errorMessage.value = '文件预览数据为空'
      return
    }
    if (previewType.value === 'text') {
      textContent.value = await blob.text()
      return
    }
    previewUrl.value = URL.createObjectURL(blob)
  } catch {
    errorMessage.value = '文件加载失败，请下载后查看'
  } finally {
    loading.value = false
  }
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

.image-preview,
.video-preview {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
  object-fit: contain;
  border-radius: 10px;
  background: #0f172a;
}

.office-preview {
  width: 100%;
  height: 72vh;
  border: 0;
  border-radius: 10px;
  background: #ffffff;
}

.text-preview {
  min-height: 58vh;
  max-height: 72vh;
  overflow: auto;
  padding: 18px;
  border-radius: 12px;
  background: #0f172a;
  color: #e5edf5;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
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
