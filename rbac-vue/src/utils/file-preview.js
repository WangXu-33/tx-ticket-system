import axios from 'axios'

const previewClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  responseType: 'blob'
})

previewClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

const blobUrlCache = new Map()

const normalizeRef = (value) => {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

const isExternalUrl = (value) => /^(https?:)?\/\//.test(value)
const isFileId = (value) => /^\d+$/.test(value)

export const resolvePreviewRequestUrl = (fileRef) => {
  const value = normalizeRef(fileRef)
  if (!value) {
    return ''
  }
  if (isFileId(value)) {
    return `/system/file/preview/${value}`
  }
  if (value.startsWith('/api/')) {
    return value.slice(4)
  }
  if (value.startsWith('/')) {
    return value
  }
  if (isExternalUrl(value)) {
    return value
  }
  return `/${value}`
}

export const getAuthedPreviewUrl = async (fileRef) => {
  const value = normalizeRef(fileRef)
  if (!value) {
    return ''
  }

  if (blobUrlCache.has(value)) {
    return blobUrlCache.get(value)
  }

  const requestUrl = resolvePreviewRequestUrl(value)
  if (!requestUrl) {
    return ''
  }

  if (isExternalUrl(requestUrl) && !requestUrl.startsWith(window.location.origin)) {
    return requestUrl
  }

  const response = await previewClient.get(requestUrl)
  const blobUrl = window.URL.createObjectURL(response.data)
  blobUrlCache.set(value, blobUrl)
  return blobUrl
}

export const getPreviewBlob = async (fileRef) => {
  const requestUrl = resolvePreviewRequestUrl(fileRef)
  if (!requestUrl) {
    return null
  }
  const response = await previewClient.get(requestUrl)
  return response.data
}

export const revokeAuthedPreviewUrl = (fileRef) => {
  const value = normalizeRef(fileRef)
  const blobUrl = blobUrlCache.get(value)
  if (blobUrl) {
    window.URL.revokeObjectURL(blobUrl)
    blobUrlCache.delete(value)
  }
}
