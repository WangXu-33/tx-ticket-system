const FILE_PREVIEW_BASE = '/api/system/file/preview/'
const FILE_DOWNLOAD_BASE = '/api/system/file/download/'

const normalizeValue = (value) => {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

const isFileId = (value) => /^\d+$/.test(normalizeValue(value))

const isAbsoluteUrl = (value) => /^(https?:)?\/\//.test(value)

const isRootPath = (value) => value.startsWith('/')

export const buildFilePreviewUrl = (fileRef) => {
  const value = normalizeValue(fileRef)
  if (!value) {
    return ''
  }
  if (isFileId(value)) {
    return `${FILE_PREVIEW_BASE}${value}`
  }
  if (isAbsoluteUrl(value) || isRootPath(value)) {
    return value
  }
  return `/${value}`
}

export const buildFileDownloadUrl = (fileRef) => {
  const value = normalizeValue(fileRef)
  if (!value) {
    return ''
  }
  if (isFileId(value)) {
    return `${FILE_DOWNLOAD_BASE}${value}`
  }
  if (isAbsoluteUrl(value) || isRootPath(value)) {
    return value
  }
  return `/${value}`
}

export const extractFileId = (payload) => {
  if (!payload || payload.id === undefined || payload.id === null) {
    return ''
  }
  return String(payload.id)
}
