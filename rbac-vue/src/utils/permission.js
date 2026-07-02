let cachedRaw = null
let cachedSet = new Set()

export const getPermissionSet = () => {
  const raw = localStorage.getItem('permissions') || '[]'
  if (raw !== cachedRaw) {
    cachedRaw = raw
    try {
      cachedSet = new Set(JSON.parse(raw))
    } catch {
      cachedSet = new Set()
    }
  }
  return cachedSet
}

const normalizePermissions = (requiredPerms = []) => {
  if (!requiredPerms) {
    return []
  }
  return Array.isArray(requiredPerms) ? requiredPerms : [requiredPerms]
}

export const hasAnyPermission = (requiredPerms = []) => {
  const perms = normalizePermissions(requiredPerms)
  if (perms.length === 0) {
    return true
  }
  const permissionSet = getPermissionSet()
  if (permissionSet.has('*')) {
    return true
  }
  return perms.some(perm => permissionSet.has(perm))
}

export const hasAllPermissions = (requiredPerms = []) => {
  const perms = normalizePermissions(requiredPerms)
  if (perms.length === 0) {
    return true
  }
  const permissionSet = getPermissionSet()
  if (permissionSet.has('*')) {
    return true
  }
  return perms.every(perm => permissionSet.has(perm))
}

export const refreshPermissionCache = () => {
  cachedRaw = null
  return getPermissionSet()
}
