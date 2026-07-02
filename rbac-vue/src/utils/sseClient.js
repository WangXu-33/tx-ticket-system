export function parseSseMessages(text, previousBuffer = '') {
  const combined = previousBuffer + text
  const normalized = combined.replace(/\r\n/g, '\n')
  const parts = normalized.split('\n\n')
  const buffer = parts.pop() || ''
  const messages = parts
    .map(parseSseBlock)
    .filter(Boolean)
  return { messages, buffer }
}

export function createSseClient(options = {}) {
  const {
    url = '/api/realtime/sse',
    tokenProvider = () => localStorage.getItem('token'),
    reconnectBaseDelay = 2000,
    reconnectMaxDelay = 30000,
    fetchImpl = fetch,
    onStatusChange = () => {}
  } = options

  const listeners = new Map()
  let abortController = null
  let reconnectTimer = null
  let reconnectDelay = reconnectBaseDelay
  let closed = false

  const emit = (event, data) => {
    const eventListeners = listeners.get(event)
    if (!eventListeners) return
    eventListeners.forEach(handler => handler(data))
  }

  const scheduleReconnect = () => {
    if (closed || reconnectTimer) return
    onStatusChange('reconnecting')
    // 断线重连采用指数退避，避免后端重启或网络抖动时前端疯狂重连。
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)
    reconnectDelay = Math.min(reconnectDelay * 2, reconnectMaxDelay)
  }

  const connect = async () => {
    if (closed) return
    if (abortController) {
      abortController.abort()
    }
    abortController = new AbortController()
    onStatusChange('connecting')

    try {
      const token = tokenProvider()
      // 原生 EventSource 不能自定义 Authorization，所以这里用 fetch 手动解析 event-stream。
      const response = await fetchImpl(url, {
        method: 'GET',
        headers: token ? { Authorization: token } : {},
        signal: abortController.signal
      })

      if (!response.ok || !response.body) {
        throw new Error(`SSE connection failed: ${response.status}`)
      }

      onStatusChange('connected')
      reconnectDelay = reconnectBaseDelay
      await readStream(response.body.getReader())
    } catch (error) {
      if (closed || error.name === 'AbortError') return
      onStatusChange('disconnected')
      scheduleReconnect()
    }
  }

  const readStream = async reader => {
    const decoder = new TextDecoder()
    let buffer = ''

    while (!closed) {
      const { value, done } = await reader.read()
      if (done) break
      // SSE 事件可能被网络分片，buffer 保存上一次未读完的半包。
      const parsed = parseSseMessages(decoder.decode(value, { stream: true }), buffer)
      buffer = parsed.buffer
      parsed.messages.forEach(message => emit(message.event, message.data))
    }

    if (!closed) {
      scheduleReconnect()
    }
  }

  const close = () => {
    closed = true
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    onStatusChange('closed')
  }

  const on = (event, handler) => {
    if (!listeners.has(event)) {
      listeners.set(event, new Set())
    }
    listeners.get(event).add(handler)
    return () => listeners.get(event)?.delete(handler)
  }

  return { connect, close, on }
}

function parseSseBlock(block) {
  const lines = block.split('\n')
  let event = 'message'
  const dataLines = []

  lines.forEach(line => {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  })

  if (!dataLines.length) {
    return null
  }

  const rawData = dataLines.join('\n')
  // 后端通常发送 JSON；解析失败时保留字符串，便于复用到纯文本事件。
  return {
    event,
    data: parseJson(rawData)
  }
}

function parseJson(value) {
  try {
    return JSON.parse(value)
  } catch (error) {
    return value
  }
}
