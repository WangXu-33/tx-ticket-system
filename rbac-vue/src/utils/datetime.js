/**
 * 修改时间：2026-07-02
 * 功能说明：统一格式化后端返回的日期时间，前端一律显示为 yyyy-MM-dd HH:mm:ss。
 * 入参：日期字符串、数组时间、时间戳或空值。
 * 出参：格式化后的日期时间字符串；空值返回 `-`。
 * 异常场景：遇到非标准日期内容时按原值返回，避免页面出现空白。
 */
export const formatDateTime = value => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = num => String(num).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  const text = String(value)
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(text)) {
    return text.replace('T', ' ').slice(0, 19)
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/.test(text)) {
    return text.slice(0, 19)
  }
  return text
}
