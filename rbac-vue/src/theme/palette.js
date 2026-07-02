export const STATUS_COLORS = {
  success: '#10b981',
  danger: '#ef4444',
  warning: '#f59e0b',
  info: '#3b82f6',
  accent: '#8b5cf6'
}

export const LIGHT_THEME_PALETTE = {
  primary: '#059669',
  primarySoft: 'rgba(5, 150, 105, 0.1)',
  link: '#6366f1',
  ...STATUS_COLORS,
  infoSoft: '#eff6ff',
  successSoft: '#ecfdf5',
  warningSoft: '#fffbeb',
  accentSoft: '#f5f3ff',
  primarySoftSurface: '#ecfdf5'
}

export const DARK_THEME_PALETTE = {
  primary: '#6366f1',
  primarySoft: 'rgba(99, 102, 241, 0.18)',
  link: '#818cf8',
  ...STATUS_COLORS,
  infoSoft: 'rgba(59, 130, 246, 0.16)',
  successSoft: 'rgba(16, 185, 129, 0.16)',
  warningSoft: 'rgba(245, 158, 11, 0.16)',
  accentSoft: 'rgba(139, 92, 246, 0.16)',
  primarySoftSurface: 'rgba(99, 102, 241, 0.16)'
}

export function getThemePalette(isDark) {
  return isDark ? DARK_THEME_PALETTE : LIGHT_THEME_PALETTE
}
