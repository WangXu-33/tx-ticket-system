import axios from 'axios'
import { message } from 'ant-design-vue'

const service = axios.create({
  baseURL: '/api',
  timeout: 5000
})

/**
 * 修改时间：2026-07-02
 * 功能说明：收敛后端异常提示，避免 SQL、堆栈、超长错误直接展示到页面。
 * 入参：后端返回的错误信息。
 * 出参：适合前端展示的错误文案。
 * 异常场景：无。
 */
const normalizeErrorMessage = msg => {
  if (!msg) {
    return '请求失败，请稍后重试'
  }
  const text = String(msg)
  const riskyPatterns = ['###', 'SQLSyntaxErrorException', 'BadSqlGrammarException', 'java.', 'org.springframework']
  if (text.length > 120 || riskyPatterns.some(pattern => text.includes(pattern))) {
    return '系统处理失败，请稍后重试或联系管理员'
  }
  return text
}

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      const safeMessage = normalizeErrorMessage(res.msg || 'Error')
      message.error(safeMessage)
      if (res.code === 401) {
        localStorage.removeItem('token')
        location.href = '/login'
      }
      return Promise.reject(new Error(safeMessage))
    }
    return res
  },
  error => {
    const responseMessage = error.response?.data?.msg || error.message
    message.error(normalizeErrorMessage(responseMessage))
    return Promise.reject(error)
  }
)

export default service
