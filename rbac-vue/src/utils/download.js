import axios from 'axios'
import { message } from 'ant-design-vue'

const fileClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  responseType: 'blob'
})

fileClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

const decodeFileName = (disposition, fallbackName) => {
  if (!disposition) {
    return fallbackName
  }
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = disposition.match(/filename="?([^\";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return fallbackName
}

const saveBlob = (blob, fileName) => {
  const blobUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(blobUrl)
}

const normalizeErrorMessage = async (blob, fallbackMessage) => {
  try {
    const text = await blob.text()
    const parsed = JSON.parse(text)
    return parsed.msg || fallbackMessage
  } catch {
    return fallbackMessage
  }
}

export const downloadByRequest = async ({ url, params, fileName = 'download.xlsx', method = 'get' }) => {
  try {
    const response = await fileClient.request({
      url,
      method,
      params
    })

    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/json')) {
      const msg = await normalizeErrorMessage(response.data, '下载失败')
      message.error(msg)
      return false
    }

    const disposition = response.headers['content-disposition']
    const resolvedName = decodeFileName(disposition, fileName)
    saveBlob(response.data, resolvedName)
    return true
  } catch (error) {
    message.error(error.message || '下载失败')
    return false
  }
}
