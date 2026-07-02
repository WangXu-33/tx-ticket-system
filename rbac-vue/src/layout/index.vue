<template>
  <a-layout class="admin-layout" :class="{ 'dark-mode': appStore.isDark }">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      width="240"
      class="sider-wrapper"
      :theme="appStore.isDark ? 'dark' : 'light'"
    >
      <div class="logo-area">
        <div class="logo-box">TX</div>
        <span v-if="!collapsed" class="logo-text">头绪工单系统</span>
      </div>
      
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        class="main-menu"
        :theme="appStore.isDark ? 'dark' : 'light'"
      >
        <template v-for="item in renderMenuList" :key="item.id">
          <!-- 包含子菜单的目录 -->
          <a-sub-menu v-if="item.children && item.children.length" :key="item.path">
            <template #icon><MenuIconRenderer v-if="item.icon" :icon-key="item.icon" :size="16" /></template>
            <template #title>{{ item.title }}</template>
            <a-menu-item 
              v-for="sub in item.children" 
              :key="sub.path" 
              @click="handleMenuClick(sub)"
            >
              {{ sub.title }}
            </a-menu-item>
          </a-sub-menu>
          
          <!-- 单个菜单项 -->
          <a-menu-item v-else :key="item.path" @click="handleMenuClick(item)">
            <template #icon><MenuIconRenderer v-if="item.icon" :icon-key="item.icon" :size="16" /></template>
            <span>{{ item.title }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶栏 -->
      <a-layout-header class="header-wrapper">
        <div class="header-left">
          <component
            :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            class="trigger-icon"
            @click="collapsed = !collapsed"
          />
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>首页</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentRouteName }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-badge :count="unreadNoticeCount" :offset="[-4, 4]">
            <div class="action-item" title="我的公告" @click="router.push('/notice/my')">
              <BellOutlined />
            </div>
          </a-badge>

          <!-- 主题切换 -->
          <div class="action-item theme-switch" @click="appStore.toggleTheme">
            <bulb-outlined v-if="!appStore.isDark" />
            <bulb-filled v-else />
          </div>

          <!-- 用户信息 -->
          <a-dropdown>
            <div class="user-info">
              <a-avatar :size="32" class="avatar">
                {{ user.nickname?.charAt(0) || 'A' }}
              </a-avatar>
              <span class="nickname">{{ user.nickname }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/user/profile')">
                  <template #icon><UserOutlined /></template>
                  个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="handleLogout">
                  <template #icon><LogoutOutlined /></template>
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 主内容区 -->
      <a-layout-content class="content-wrapper">
        <div class="page-inner">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '../store/app'
import request from '@/api/request'
import { createSseClient } from '@/utils/sseClient'
import { 
  DashboardOutlined, MenuUnfoldOutlined,
  MenuFoldOutlined, LogoutOutlined, BulbOutlined, BulbFilled, UserOutlined, BellOutlined
} from '@ant-design/icons-vue'
import { notification } from 'ant-design-vue'
import MenuIconRenderer from '@/components/menu-icon-renderer/index.vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const collapsed = ref(false)
const selectedKeys = ref([])
const openKeys = ref([])
const menuList = ref([]) // 动态菜单列表
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const unreadNoticeCount = ref(0)
let noticeClient = null

const currentRouteName = computed(() => {
  if (route.path === '/external/frame' && route.query.title) {
    return route.query.title
  }
  return route.meta.title || '当前页'
})

const isExternalUrl = (path) => /^(https?:)?\/\//i.test(path || '')

const normalizePath = (path) => {
  if (!path) return ''
  if (isExternalUrl(path)) return path
  return path.startsWith('/') ? path : `/${path}`
}

const normalizeMenuTree = (list) => {
  return list.map(item => ({
    ...item,
    path: normalizePath(item.path),
    children: item.children ? normalizeMenuTree(item.children) : []
  }))
}

const renderMenuList = computed(() => {
  const homePath = '/dashboard'
  const hasHome = menuList.value.some(item => item.path === homePath)
  if (hasHome) {
    return menuList.value
  }
  return [
    {
      id: 'dashboard-static',
      title: '首页',
      path: homePath,
      icon: 'DashboardOutlined',
      children: []
    },
    ...menuList.value
  ]
})

const collectParentKeys = (list, targetPath, parents = []) => {
  for (const item of list) {
    const nextParents = item.children?.length ? [...parents, item.path] : parents
    if (item.path === targetPath) {
      return parents
    }
    if (item.children?.length) {
      const matchedParents = collectParentKeys(item.children, targetPath, nextParents)
      if (matchedParents.length || item.children.some(child => child.path === targetPath)) {
        return matchedParents
      }
    }
  }
  return []
}

const syncMenuState = () => {
  const externalMenuPath = typeof route.query.menuPath === 'string' ? route.query.menuPath : ''
  const currentPath = externalMenuPath || normalizePath(route.path)
  selectedKeys.value = [currentPath]
  openKeys.value = collectParentKeys(renderMenuList.value, currentPath)
}

const fetchMenuList = async () => {
  const res = await request.get('/auth/menus')
  // 将扁平数组转为树形结构供菜单渲染
  const map = {}
  res.data.forEach(item => map[item.id] = { ...item, children: [] })
  const tree = []
  res.data.forEach(item => {
    if (item.parentId !== 0 && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else {
      tree.push(map[item.id])
    }
  })
  menuList.value = normalizeMenuTree(tree)
  syncMenuState()
}

const fetchUnreadNoticeCount = async () => {
  try {
    const res = await request.get('/system/notice/my/unread-count')
    unreadNoticeCount.value = res.data || 0
  } catch (e) {
    unreadNoticeCount.value = 0
  }
}

const connectNoticeStream = () => {
  noticeClient = createSseClient()
  noticeClient.on('notice-new', data => {
    fetchUnreadNoticeCount()
    notification.info({
      message: '新公告',
      description: data?.title || '你收到一条新公告消息',
      duration: 4
    })
  })
  noticeClient.connect()
}

const handleLogout = async () => {
  await request.post('/auth/logout')
  localStorage.clear()
  router.push('/login')
}

const handleMenuClick = (menu) => {
  const path = typeof menu === 'string' ? menu : menu?.path
  const title = typeof menu === 'string' ? '' : menu?.title
  if (isExternalUrl(path)) {
    router.push({
      path: '/external/frame',
      query: {
        url: path,
        title: title || '外部系统',
        menuPath: path
      }
    })
    return
  }
  const nextPath = normalizePath(path)
  if (route.path !== nextPath) {
    router.push(nextPath)
  }
}

onMounted(() => {
  fetchMenuList()
  fetchUnreadNoticeCount()
  connectNoticeStream()
})

onUnmounted(() => {
  if (noticeClient) {
    noticeClient.close()
    noticeClient = null
  }
})

watch(() => route.path, () => {
  syncMenuState()
}, { immediate: true })
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  transition: all 0.5s;
}

/* 侧边栏样式 */
.sider-wrapper {
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.02);
  z-index: 100;
  transition: all 0.3s;
}
.light-theme .sider-wrapper { background: #fff !important; }
.dark-mode .sider-wrapper { background: #0f172a !important; border-right: 1px solid rgba(255,255,255,0.05); }

.logo-area {
  height: 64px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}
.logo-box {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: white;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 900;
  font-size: 12px;
  flex-shrink: 0;
}
.dark-mode .logo-box { background: var(--color-primary); }
.logo-text {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
  white-space: nowrap;
}

/* 顶栏样式 */
.header-wrapper {
  background: var(--bg-card) !important;
  backdrop-filter: blur(10px);
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 99;
}

.header-left { display: flex; align-items: center; gap: 20px; }
.trigger-icon { font-size: 18px; cursor: pointer; color: var(--text-sub); transition: color 0.3s; }
.trigger-icon:hover { color: var(--text-main); }
:deep(.ant-breadcrumb-link), :deep(.ant-breadcrumb-separator) { color: var(--text-main) !important; }

.header-right { display: flex; align-items: center; gap: 16px; }
.action-item {
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--text-main);
}
.action-item:hover { background: var(--bg-primary); color: var(--color-primary); }
.dark-mode .action-item:hover { color: var(--color-primary); }

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 10px;
  transition: background 0.3s;
}
.user-info:hover { background: var(--bg-primary); }
.nickname { font-weight: 600; color: var(--text-main); }

/* 内容容器 */
.content-wrapper {
  padding: 24px;
  background: var(--bg-primary);
  min-height: calc(100vh - 64px);
}

.page-inner {
  max-width: 1600px;
  margin: 0 auto;
}

/* 动效 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }

/* Ant Design 覆盖 */
:deep(.ant-menu) { border-inline-end: none !important; background: transparent !important; }
:deep(.ant-menu-item), :deep(.ant-menu-submenu-title), :deep(.ant-menu-item-icon) { color: var(--text-main) !important; }
:deep(.ant-menu-submenu-arrow) { color: var(--text-main) !important; }
:deep(.ant-menu-item-selected) {
  background: linear-gradient(90deg, var(--color-primary-soft-surface) 0%, rgba(255,255,255,0.82) 100%) !important;
  color: var(--color-primary) !important;
  box-shadow: inset 4px 0 0 var(--color-primary), 0 8px 18px rgba(5, 150, 105, 0.12);
}
.dark-mode :deep(.ant-menu-item-selected) {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.22) 0%, rgba(30,41,59,0.96) 100%) !important;
  color: var(--color-primary) !important;
  box-shadow: inset 4px 0 0 var(--color-primary), 0 10px 22px rgba(2, 6, 23, 0.32);
}
:deep(.ant-menu-item) { border-radius: 10px !important; margin: 4px 8px !important; width: calc(100% - 16px) !important; }
:deep(.ant-menu-item-selected .ant-menu-title-content),
:deep(.ant-menu-item-selected .ant-menu-item-icon),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title .ant-menu-title-content),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title .ant-menu-item-icon) {
  color: var(--color-primary) !important;
  font-weight: 700;
}
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  background: rgba(5, 150, 105, 0.08) !important;
  border-radius: 10px !important;
}
.dark-mode :deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  background: rgba(99, 102, 241, 0.14) !important;
}
:deep(.ant-menu-item::after) {
  border-inline-end-color: var(--color-primary) !important;
  border-inline-end-width: 4px !important;
}
</style>
