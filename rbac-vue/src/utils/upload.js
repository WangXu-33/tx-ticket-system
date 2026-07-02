import request from '@/api/request'

export const uploadSystemFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}

export const createUploadRequest = (onUploaded) => async ({ file, onSuccess, onError }) => {
  try {
    const uploaded = await uploadSystemFile(file)
    onUploaded(uploaded)
    onSuccess(uploaded)
  } catch (error) {
    onError(error)
  }
}

export const formatFileSize = (size = 0) => {
  const value = Number(size) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
