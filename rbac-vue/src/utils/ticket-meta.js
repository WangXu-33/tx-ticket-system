import { ticketApi } from '@/api/ticket'

const EMPTY_META = {
  statusOptions: [],
  priorityOptions: [],
  categoryOptions: [],
  actionOptions: [],
  systemOptions: []
}

let cachedMeta = null
let inflightPromise = null

/**
 * 修改时间：2026-07-02
 * 功能说明：加载并缓存工单元数据，避免每个页面重复请求字典接口。
 * 入参：force，可选，是否强制刷新缓存。
 * 出参：标准化后的工单元数据对象。
 * 异常场景：接口失败时返回空元数据，并把异常继续抛给页面决定是否提示。
 */
export const loadTicketMeta = async (force = false) => {
  if (!force && cachedMeta) {
    return cachedMeta
  }
  if (!force && inflightPromise) {
    return inflightPromise
  }
  inflightPromise = ticketApi.meta()
    .then(res => {
      cachedMeta = { ...EMPTY_META, ...(res.data || {}) }
      inflightPromise = null
      return cachedMeta
    })
    .catch(error => {
      inflightPromise = null
      throw error
    })
  return inflightPromise
}

export const clearTicketMetaCache = () => {
  cachedMeta = null
  inflightPromise = null
}

export const getOptionByValue = (options = [], value) => options.find(item => item?.value === value)

export const getOptionLabel = (options = [], value, fallback = '-') => {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  return getOptionByValue(options, value)?.label || value
}

export const getOptionColor = (options = [], value, fallback = 'default') => {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  return getOptionByValue(options, value)?.color || fallback
}
