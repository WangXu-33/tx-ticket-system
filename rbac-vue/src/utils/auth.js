const parseStorage = (key, fallback) => {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

/**
 * 修改时间：2026-07-02
 * 功能说明：读取当前登录用户缓存，供工单按钮权限与归属判断使用。
 * 入参：无。
 * 出参：用户对象或 null。
 * 异常场景：缓存损坏时返回 null，避免页面脚本中断。
 */
export const getCurrentUser = () => parseStorage('user', null)

export const getCurrentUserId = () => getCurrentUser()?.id ?? null

export const getStoredRoles = () => parseStorage('roles', [])

export const hasAnyRole = (roles = []) => {
  const required = Array.isArray(roles) ? roles : [roles]
  if (required.length === 0) {
    return true
  }
  const currentRoles = new Set(getStoredRoles())
  return required.some(role => currentRoles.has(role))
}
