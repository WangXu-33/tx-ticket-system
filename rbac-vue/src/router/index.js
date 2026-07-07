import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/index.vue'
import { hasAnyPermission } from '../utils/permission'

const routes = [
  {
    path: '/login',
    component: () => import('../views/login/index.vue')
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('../views/system/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'ticket',
        redirect: '/ticket/index',
        meta: { title: '工单中心', permission: ['ticket:list', 'ticket:my:list'] }
      },
      {
        path: 'ticket/index',
        component: () => import('../views/ticket/index.vue'),
        meta: { title: '工单首页', permission: ['ticket:list', 'ticket:my:list'] }
      },
      {
        path: 'ticket/create',
        component: () => import('../views/ticket/create.vue'),
        meta: { title: '提交工单', permission: ['ticket:add', 'ticket:my:add'] }
      },
      {
        path: 'ticket/systems',
        component: () => import('../views/ticket/systems.vue'),
        meta: { title: '系统配置', permission: ['ticket:list'] }
      },
      {
        path: 'ticket/process/:id',
        component: () => import('../views/ticket/process.vue'),
        meta: { title: '处理工单', permission: ['ticket:list'] }
      },
      {
        path: 'ticket/pending',
        component: () => import('../views/ticket/pending/index.vue'),
        meta: { title: '待接工单', permission: ['ticket:list'] }
      },
      {
        path: 'ticket/workbench',
        component: () => import('../views/ticket/workbench/index.vue'),
        meta: { title: '工单工作台', permission: ['ticket:list'] }
      },
      {
        path: 'ticket/my',
        component: () => import('../views/ticket/my/index.vue'),
        meta: { title: '我的工单', permission: ['ticket:my:list', 'ticket:add'] }
      },
      {
        path: 'ticket/detail/:id',
        component: () => import('../views/ticket/detail/index.vue'),
        meta: { title: '工单详情', permission: ['ticket:list', 'ticket:my:list'] }
      },
      {
        path: 'ticket/knowledge',
        component: () => import('../views/ticket/knowledge/index.vue'),
        meta: { title: '知识库', permission: ['knowledge:list'] }
      },
      {
        path: 'ticket/knowledge/detail/:id?',
        component: () => import('../views/ticket/knowledge/detail.vue'),
        meta: { title: '知识详情', permission: ['knowledge:list', 'knowledge:edit'] }
      },
      {
        path: 'system/user',
        component: () => import('../views/rbac/user/index.vue'),
        meta: { title: '用户管理', permission: ['sys:user:list'] }
      },
      {
        path: 'system/user/detail/:id?',
        component: () => import('../views/rbac/user/detail.vue'),
        meta: { title: '用户资料', permission: ['sys:user:list', 'sys:user:add', 'sys:user:edit'] }
      },
      {
        path: 'user/profile',
        component: () => import('../views/rbac/user/profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'system/role',
        component: () => import('../views/rbac/role/index.vue'),
        meta: { title: '角色管理', permission: ['sys:role:list'] }
      },
      {
        path: 'rbac/analytics',
        component: () => import('../views/rbac/analytics/index.vue'),
        meta: { title: '权限分析', permission: ['sys:analytics:list'] }
      },
      {
        path: 'system/menu',
        component: () => import('../views/rbac/menu/index.vue'),
        meta: { title: '菜单管理', permission: ['sys:menu:list'] }
      },
      {
        path: 'system/dept',
        component: () => import('../views/rbac/dept/index.vue'),
        meta: { title: '部门管理', permission: ['sys:dept:list'] }
      },
      {
        path: 'system/dict',
        component: () => import('../views/system/dict/index.vue'),
        meta: { title: '字典管理', permission: ['sys:dict:list'] }
      },
      {
        path: 'system/file',
        component: () => import('../views/system/file/index.vue'),
        meta: { title: '文件管理', permission: ['sys:file:list'] }
      },
      {
        path: 'system/config',
        component: () => import('../views/system/config/index.vue'),
        meta: { title: '存储配置', permission: ['sys:config:list'] }
      },
      {
        path: 'system/notice',
        component: () => import('../views/system/notice/index.vue'),
        meta: { title: '公告消息', permission: ['sys:notice:list'] }
      },
      {
        path: 'notice/my',
        component: () => import('../views/notice/my.vue'),
        meta: { title: '我的公告', permission: ['sys:notice:read'] }
      },
      {
        path: 'log/operlog',
        component: () => import('../views/log/operlog/index.vue'),
        meta: { title: '操作日志', permission: ['sys:operlog:list'] }
      },
      {
        path: 'log/loginlog',
        component: () => import('../views/log/loginlog/index.vue'),
        meta: { title: '登录日志', permission: ['sys:loginlog:list'] }
      },
      {
        path: 'tool/gen',
        component: () => import('../views/tool/gen/index.vue'),
        meta: { title: '代码生成', permission: ['tool:gen:list'] }
      },
      {
        path: 'external/frame',
        component: () => import('../views/system/external-frame/index.vue'),
        meta: { title: '外部系统' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.meta.permission && !hasAnyPermission(to.meta.permission)) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
