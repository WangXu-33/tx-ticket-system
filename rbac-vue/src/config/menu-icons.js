import * as AntIcons from '@ant-design/icons-vue'

const builtInMenuIcons = [
  { key: 'DashboardOutlined', label: '首页仪表盘', type: 'component', component: AntIcons.DashboardOutlined },
  { key: 'AppstoreOutlined', label: '应用中心', type: 'component', component: AntIcons.AppstoreOutlined },
  { key: 'SettingOutlined', label: '系统设置', type: 'component', component: AntIcons.SettingOutlined },
  { key: 'UserOutlined', label: '用户', type: 'component', component: AntIcons.UserOutlined },
  { key: 'TeamOutlined', label: '团队', type: 'component', component: AntIcons.TeamOutlined },
  { key: 'ApartmentOutlined', label: '部门组织', type: 'component', component: AntIcons.ApartmentOutlined },
  { key: 'SafetyCertificateOutlined', label: '权限安全', type: 'component', component: AntIcons.SafetyCertificateOutlined },
  { key: 'MenuOutlined', label: '菜单路由', type: 'component', component: AntIcons.MenuOutlined },
  { key: 'FileTextOutlined', label: '文档内容', type: 'component', component: AntIcons.FileTextOutlined },
  { key: 'FolderOpenOutlined', label: '文件目录', type: 'component', component: AntIcons.FolderOpenOutlined },
  { key: 'DatabaseOutlined', label: '数据存储', type: 'component', component: AntIcons.DatabaseOutlined },
  { key: 'AuditOutlined', label: '审计日志', type: 'component', component: AntIcons.AuditOutlined },
  { key: 'BellOutlined', label: '通知消息', type: 'component', component: AntIcons.BellOutlined },
  { key: 'ToolOutlined', label: '研发工具', type: 'component', component: AntIcons.ToolOutlined },
  { key: 'BuildOutlined', label: '构建配置', type: 'component', component: AntIcons.BuildOutlined },
  { key: 'ContainerOutlined', label: '服务容器', type: 'component', component: AntIcons.ContainerOutlined },
  { key: 'CloudServerOutlined', label: '云服务', type: 'component', component: AntIcons.CloudServerOutlined },
  { key: 'CodeOutlined', label: '代码开发', type: 'component', component: AntIcons.CodeOutlined },
  { key: 'BookOutlined', label: '知识文档', type: 'component', component: AntIcons.BookOutlined },
  { key: 'LinkOutlined', label: '外部链接', type: 'component', component: AntIcons.LinkOutlined }
]

// 后续新增自定义图标时，直接往这里追加即可。
// 示例：
// import customPortalUrl from '@/assets/menu-icons/custom-portal.svg'
// { key: 'CustomPortal', label: '自定义门户', type: 'image', src: customPortalUrl }
const customMenuIcons = []

const iconCatalog = [...builtInMenuIcons, ...customMenuIcons]
const iconMap = new Map(iconCatalog.map(item => [item.key, item]))

export const menuIconOptions = iconCatalog

export function resolveMenuIconMeta(iconKey) {
  return iconMap.get(iconKey) || null
}

export function filterMenuIconOptions(input, option) {
  const keyword = (input || '').trim().toLowerCase()
  if (!keyword) return true
  return [option.label, option.rawKey].some(value => (value || '').toLowerCase().includes(keyword))
}
