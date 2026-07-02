<template>
  <a-config-provider :theme="antdTheme" :locale="zhCN">
    <div class="app-root" :style="themeVars">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { useAppStore } from './store/app'
import { DARK_THEME_PALETTE, LIGHT_THEME_PALETTE, getThemePalette } from './theme/palette'

// 设置 dayjs 语言
dayjs.locale('zh-cn')

const appStore = useAppStore()
const currentPalette = computed(() => getThemePalette(appStore.isDark))

const themeVars = computed(() => {
  const palette = currentPalette.value

  return {
    '--color-primary': palette.primary,
    '--color-primary-soft': palette.primarySoft,
    '--color-link': palette.link,
    '--color-success': palette.success,
    '--color-danger': palette.danger,
    '--color-warning': palette.warning,
    '--color-info': palette.info,
    '--color-accent': palette.accent,
    '--color-info-soft': palette.infoSoft,
    '--color-success-soft': palette.successSoft,
    '--color-warning-soft': palette.warningSoft,
    '--color-accent-soft': palette.accentSoft,
    '--color-primary-soft-surface': palette.primarySoftSurface,
    '--table-action-color': palette.primary,
    '--table-action-hover-color': palette.link,
    '--table-action-danger-color': palette.danger
  }
})

// 动态计算 Ant Design 的主题配置，确保组件内部颜色也同步
const antdTheme = computed(() => ({
  algorithm: appStore.isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: currentPalette.value.primary,
    borderRadius: 12,
  },
}))
</script>

<style>
/* 注入规约中定义的全局语义化变量 */
:root {
  --bg-primary: #f1f5f9;
  --bg-card: #ffffff;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --border-color: #e2e8f0;
  --color-primary: #059669;
  --color-primary-soft: rgba(5, 150, 105, 0.1);
  --color-link: #6366f1;
  --color-success: #10b981;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  --color-info: #3b82f6;
  --color-accent: #8b5cf6;
  --color-info-soft: #eff6ff;
  --color-success-soft: #ecfdf5;
  --color-warning-soft: #fffbeb;
  --color-accent-soft: #f5f3ff;
  --color-primary-soft-surface: #ecfdf5;
  --table-action-color: var(--color-primary);
  --table-action-hover-color: var(--color-link);
  --table-action-danger-color: var(--color-danger);
  transition: all 0.5s ease;
}

.dark {
  --bg-primary: #020617;
  --bg-card: #0f172a;
  --text-main: #f8fafc;
  --text-sub: #94a3b8;
  --border-color: #1e293b;
  --color-primary: #6366f1;
  --color-primary-soft: rgba(99, 102, 241, 0.18);
  --color-link: #818cf8;
  --color-success: #10b981;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  --color-info: #3b82f6;
  --color-accent: #8b5cf6;
  --color-info-soft: rgba(59, 130, 246, 0.16);
  --color-success-soft: rgba(16, 185, 129, 0.16);
  --color-warning-soft: rgba(245, 158, 11, 0.16);
  --color-accent-soft: rgba(139, 92, 246, 0.16);
  --color-primary-soft-surface: rgba(99, 102, 241, 0.16);
}

/* 强制应用全局文字与背景色，解决“看不见字体”的问题 */
body {
  margin: 0;
  padding: 0;
  background-color: var(--bg-primary);
  color: var(--text-main);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.app-root {
  min-height: 100vh;
}

/* 确保所有卡片、面板遵循变量 */
.ant-card {
  background-color: var(--bg-card) !important;
  color: var(--text-main) !important;
}

h1, h2, h3, h4, h5, h6, span, p, div {
  color: inherit; /* 继承父级（即 body 的变量色） */
}

/* 默认链接不抢业务组件颜色，表格操作色在下方单独声明 */
a {
  color: inherit;
  text-decoration: none;
}

/* 表格背景修正 */
.ant-table {
  background: transparent !important;
}
.ant-table-thead > tr > th {
  background: var(--bg-primary) !important;
  color: var(--text-main) !important;
}
.ant-table-tbody > tr > td {
  color: var(--text-main) !important;
}

/* 查询表单和结果表格保持统一留白，避免表单控件贴住表头 */
.ant-form.ant-form-inline {
  row-gap: 12px;
}

.ant-form.ant-form-inline + .ant-table-wrapper {
  margin-top: 16px;
}

/* 统一表格操作列链接颜色：覆盖插槽、Ant Link 按钮及内部图标文字 */
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-action-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.edit-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.auth-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.perm-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.url-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.resetPwd-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.dataPerm-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.preview-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link:not(.ant-btn-dangerous) {
  color: var(--table-action-color) !important;
  cursor: pointer;
  font-weight: 600;
  transition: color 0.2s ease, filter 0.2s ease;
}

.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-action-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.edit-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.auth-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.perm-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.url-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.resetPwd-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.dataPerm-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.preview-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link:not(.ant-btn-dangerous):hover {
  color: var(--table-action-hover-color) !important;
  text-decoration: underline;
  filter: brightness(0.92);
}

.ant-table-wrapper .ant-table-tbody .ant-table-cell a.delete-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-danger-link,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link.ant-btn-dangerous {
  color: var(--table-action-danger-color) !important;
  cursor: pointer;
  font-weight: 600;
  transition: color 0.2s ease, filter 0.2s ease;
}

.ant-table-wrapper .ant-table-tbody .ant-table-cell a.delete-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-danger-link:hover,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link.ant-btn-dangerous:hover {
  color: var(--table-action-danger-color) !important;
  text-decoration: underline;
  filter: brightness(0.9);
}

.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-action-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.edit-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.auth-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.perm-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.url-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.resetPwd-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.dataPerm-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.preview-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.delete-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell a.table-danger-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link span,
.ant-table-wrapper .ant-table-tbody .ant-table-cell .ant-btn-link svg {
  color: inherit !important;
  fill: currentColor;
}
</style>
