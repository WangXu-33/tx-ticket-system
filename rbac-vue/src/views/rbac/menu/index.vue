<template>
  <div class="page-container">
    <a-card class="search-card slide-in-1" :bordered="false">
      <TreeSearchToolbar
        v-model="searchKeyword"
        placeholder="请输入菜单名称、路由或组件路径"
        @search="handleSearch"
        @reset="resetSearch"
      />
    </a-card>

    <a-card class="table-card slide-in-2" :bordered="false">
      <template #title>
        <span class="card-title">菜单与路由管理</span>
      </template>
      <template #extra>
        <a-button v-hasPerm="['sys:menu:add']" type="primary" @click="handleAdd(0)">
          <template #icon><PlusOutlined /></template>新增根菜单
        </a-button>
      </template>

      <a-table 
        :dataSource="filteredMenuTree"
        :columns="columns" 
        rowKey="id" 
        :loading="loading"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <a-tag :color="menuTypeMeta[getMenuType(record)].color">
              {{ menuTypeMeta[getMenuType(record)].label }}
            </a-tag>
          </template>
          <template v-if="column.key === 'icon'">
            <a-tooltip v-if="record.icon" :title="resolveMenuIconMeta(record.icon)?.label || record.icon">
              <MenuIconRenderer :icon-key="record.icon" :size="18" :fallback-text="record.icon" />
            </a-tooltip>
            <span v-else class="empty-icon-text">未设置</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-hasPerm="['sys:menu:edit']" class="edit-link" @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a v-hasPerm="['sys:menu:edit']" class="perm-link" @click="handleConfigPerms(record)">按钮权限</a>
              <a-divider type="vertical" />
              <a v-if="!record.component" v-hasPerm="['sys:menu:add']" class="table-action-link" @click="handleAdd(record.id)">下级</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除吗？" @confirm="handleDelete(record.id)">
                <a v-hasPerm="['sys:menu:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 按钮权限配置抽屉 (符合剥离逻辑规约) -->
    <a-drawer
      v-model:open="permDrawerVisible"
      :title="`按钮权限配置 - ${currentMenu?.title}`"
      width="600"
      destroyOnClose
    >
      <div class="perm-toolbar">
        <a-button v-hasPerm="['sys:menu:edit']" type="primary" size="small" @click="handleAddPerm">
          <template #icon><plus-outlined /></template>添加按钮
        </a-button>
      </div>
      <a-table :dataSource="permList" :columns="permColumns" rowKey="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permKey'">
            <a-tag color="purple">{{ record.permKey }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-popconfirm title="确定删除权限吗？" @confirm="handleDeletePerm(record.id)">
              <a v-hasPerm="['sys:menu:edit']" class="delete-link">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <!-- 菜单编辑抽屉 -->
    <a-drawer
      :title="form.id ? '编辑菜单' : '新增菜单'"
      :open="drawerVisible"
      width="620"
      @close="drawerVisible = false"
    >
      <a-form :model="form" layout="vertical">
        <a-alert
          type="info"
          show-icon
          class="menu-type-alert"
          :message="menuTypeMeta[form.menuType].title"
          :description="menuTypeMeta[form.menuType].description"
        />
        <a-form-item label="菜单类型">
          <a-radio-group v-model:value="form.menuType" button-style="solid" class="menu-type-group" @change="handleMenuTypeChange">
            <a-radio-button value="directory">目录</a-radio-button>
            <a-radio-button value="menu">页面</a-radio-button>
            <a-radio-button value="external">外链</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="上级菜单">
          <a-input :value="parentMenuLabel" disabled />
        </a-form-item>
        <a-form-item label="菜单名称" :rules="[{ required: true }]">
          <a-input v-model:value="form.title" :placeholder="menuTypeMeta[form.menuType].namePlaceholder" />
        </a-form-item>
        <a-form-item label="路由路径">
          <a-input v-model:value="form.path" :placeholder="menuTypeMeta[form.menuType].pathPlaceholder" />
          <div class="field-hint">{{ menuTypeMeta[form.menuType].pathHint }}</div>
        </a-form-item>
        <a-form-item v-if="form.menuType === 'menu'" label="组件路径">
          <a-input v-model:value="form.component" placeholder="如: /rbac/user/index" />
          <div class="field-hint">只有“页面”类型需要组件路径，目录和外链会自动忽略该字段。</div>
        </a-form-item>
        <a-form-item label="图标">
          <a-select
            v-model:value="form.icon"
            show-search
            allow-clear
            option-filter-prop="label"
            :filter-option="filterMenuIconOptions"
            placeholder="请选择图标，也可以搜索中文名称"
          >
            <a-select-option
              v-for="icon in menuIconOptions"
              :key="icon.key"
              :value="icon.key"
              :label="icon.label"
              :raw-key="icon.key"
            >
              <a-space>
                <MenuIconRenderer :icon-key="icon.key" :size="18" />
                <span>{{ icon.label }}</span>
              </a-space>
            </a-select-option>
          </a-select>
          <div v-if="selectedIconMeta" class="field-hint">当前图标：{{ selectedIconMeta.label }}</div>
          <div class="icon-preview-row">
            <span class="field-hint">常用图标：</span>
            <div class="icon-chip-list">
              <button
                v-for="icon in quickMenuIcons"
                :key="icon.key"
                type="button"
                class="icon-chip"
                :class="{ active: form.icon === icon.key }"
                :title="icon.label"
                @click="form.icon = icon.key"
              >
                <MenuIconRenderer :icon-key="icon.key" :size="18" />
              </button>
            </div>
          </div>
          <div class="field-hint">后续如果要加自定义图标，直接在本地 `src/config/menu-icons.js` 注册，并把 svg/png 放到 `src/assets/menu-icons/`。</div>
        </a-form-item>
        <a-form-item label="显示排序">
          <a-input-number v-model:value="form.sort" style="width: 100%" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button v-hasPerm="form.id ? ['sys:menu:edit'] : ['sys:menu:add']" type="primary" :loading="submitting" @click="submitForm">提交</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 按钮权限表单弹窗 -->
    <a-modal
      v-model:open="permVisible"
      title="新增按钮权限"
      @ok="submitPerm"
      :confirmLoading="permSubmitting"
      destroyOnClose
    >
      <a-form :model="permForm" layout="vertical">
        <a-form-item label="按钮名称" :rules="[{ required: true }]">
          <a-input v-model:value="permForm.name" placeholder="如: 新增用户" />
        </a-form-item>
        <a-form-item label="权限标识" :rules="[{ required: true }]">
          <a-input v-model:value="permForm.permKey" placeholder="如: sys:user:add" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'
import TreeSearchToolbar from '@/components/tree-search-toolbar/index.vue'
import { filterTreeWithLinkage } from '@/utils/tree'
import MenuIconRenderer from '@/components/menu-icon-renderer/index.vue'
import { filterMenuIconOptions, menuIconOptions, resolveMenuIconMeta } from '@/config/menu-icons'

const loading = ref(false)
const submitting = ref(false)
const menuTree = ref([])
const currentMenu = ref(null)
const searchKeyword = ref('')
const activeKeyword = ref('')

const drawerVisible = ref(false)
const form = reactive({ id: null, parentId: 0, title: '', path: '', component: '', icon: '', sort: 0, menuType: 'directory' })

const permDrawerVisible = ref(false)
const permList = ref([])

const permVisible = ref(false)
const permSubmitting = ref(false)
const permForm = reactive({ id: null, menuId: null, name: '', permKey: '', status: 1 })

const menuTypeMeta = {
  directory: {
    label: '目录',
    title: '目录用于组织导航，不直接绑定页面组件。',
    description: '适合系统管理、日志管理这类分组。填写一个路由前缀即可，下面再挂具体页面。',
    namePlaceholder: '如：系统管理',
    pathPlaceholder: '如：system',
    pathHint: '目录建议填写短路径前缀，不需要组件路径。',
    color: 'blue'
  },
  menu: {
    label: '页面',
    title: '页面会直接跳转到一个业务页面。',
    description: '适合用户管理、角色管理这类可点击进入的页面。需要同时配置路由路径和组件路径。',
    namePlaceholder: '如：用户管理',
    pathPlaceholder: '如：system/user',
    pathHint: '页面路径建议与实际路由一致，组件路径填写 views 下的相对路径。',
    color: 'green'
  },
  external: {
    label: '外链',
    title: '外链会在新标签页打开外部地址。',
    description: '适合跳转到文档系统、报表平台、代码生成器等独立服务。当前导航已支持外链点击打开。',
    namePlaceholder: '如：接口文档',
    pathPlaceholder: '如：https://docs.example.com',
    pathHint: '外链必须填写完整的 http:// 或 https:// 地址。',
    color: 'orange'
  }
}

const quickMenuIcons = computed(() => menuIconOptions.slice(0, 12))

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '类型', key: 'menuType', width: 100 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 80 },
  { title: '路由路径', dataIndex: 'path', key: 'path' },
  { title: '组件路径', dataIndex: 'component', key: 'component' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '操作', key: 'action', width: 300 }
]

const permColumns = [
  { title: '权限名称', dataIndex: 'name' },
  { title: '权限标识', dataIndex: 'permKey', key: 'permKey' },
  { title: '操作', key: 'action', width: 100 }
]

const filteredMenuTree = computed(() => {
  return filterTreeWithLinkage(menuTree.value, activeKeyword.value, ['title', 'path', 'component'])
})

const parentMenuMap = computed(() => {
  const map = new Map([[0, '根节点']])
  const walk = (nodes) => {
    nodes.forEach(node => {
      map.set(node.id, node.title)
      if (node.children?.length) {
        walk(node.children)
      }
    })
  }
  walk(menuTree.value)
  return map
})

const parentMenuLabel = computed(() => {
  return `${parentMenuMap.value.get(form.parentId) || '未找到上级菜单'}（ID: ${form.parentId}）`
})

const selectedIconMeta = computed(() => resolveMenuIconMeta(form.icon))

const isExternalUrl = (value) => /^https?:\/\//i.test((value || '').trim())

const getMenuType = (record) => {
  if (isExternalUrl(record.path)) return 'external'
  if (record.component) return 'menu'
  return 'directory'
}

const fetchMenus = async () => {
  loading.value = true
  try {
    const treeRes = await request.get('/system/menu/tree')
    menuTree.value = treeRes.data
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  activeKeyword.value = searchKeyword.value.trim()
}

const resetSearch = () => {
  searchKeyword.value = ''
  activeKeyword.value = ''
}

const handleAdd = (parentId) => {
  Object.assign(form, {
    id: null,
    parentId,
    title: '',
    path: '',
    component: '',
    icon: '',
    sort: 0,
    menuType: parentId === 0 ? 'directory' : 'menu'
  })
  drawerVisible.value = true
}

const handleEdit = (record) => {
  Object.assign(form, { ...record, menuType: getMenuType(record) })
  drawerVisible.value = true
}

const handleMenuTypeChange = ({ target }) => {
  const nextType = target?.value || form.menuType
  if (nextType !== 'menu') {
    form.component = ''
  }
  if (nextType === 'external' && !isExternalUrl(form.path)) {
    form.path = ''
  }
}

const handleDelete = async (id) => {
  await request.delete(`/system/menu/delete/${id}`)
  message.success('删除成功')
  fetchMenus()
}

const submitForm = async () => {
  if (submitting.value) return
  if (!form.title.trim()) {
    message.warning('请输入菜单名称')
    return
  }
  if (!form.path.trim()) {
    message.warning('请填写路由路径或外链地址')
    return
  }
  if (form.menuType === 'menu' && !form.component.trim()) {
    message.warning('页面类型必须填写组件路径')
    return
  }
  if (form.menuType === 'external' && !isExternalUrl(form.path)) {
    message.warning('外链必须以 http:// 或 https:// 开头')
    return
  }
  submitting.value = true
  try {
    const url = form.id ? '/system/menu/edit' : '/system/menu/add'
    const payload = {
      id: form.id,
      parentId: form.parentId,
      title: form.title.trim(),
      path: form.path.trim(),
      component: form.menuType === 'menu' ? form.component.trim() : '',
      icon: form.icon || '',
      sort: form.sort ?? 0
    }
    await request.post(url, payload)
    message.success('操作成功')
    drawerVisible.value = false
    fetchMenus()
  } finally {
    submitting.value = false
  }
}

const handleConfigPerms = async (menu) => {
  currentMenu.value = menu
  const res = await request.get('/system/permission/list', { params: { menuId: menu.id } })
  permList.value = res.data
  permDrawerVisible.value = true
}

const handleAddPerm = () => {
  Object.assign(permForm, { id: null, menuId: currentMenu.value.id, name: '', permKey: '', status: 1 })
  permVisible.value = true
}

const submitPerm = async () => {
  if (permSubmitting.value) return
  permSubmitting.value = true
  try {
    await request.post('/system/permission/add', permForm)
    message.success('添加成功')
    permVisible.value = false
    await handleConfigPerms(currentMenu.value)
  } finally {
    permSubmitting.value = false
  }
}

const handleDeletePerm = async (id) => {
  await request.delete(`/system/permission/delete/${id}`, { params: { menuId: currentMenu.value.id } })
  message.success('已从当前菜单移除权限')
  await handleConfigPerms(currentMenu.value)
}

onMounted(fetchMenus)
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.edit-link { color: var(--color-primary); font-weight: 600; }
.perm-link { color: var(--color-link); font-weight: 600; }
.delete-link { color: var(--color-danger); }
.perm-toolbar { margin-bottom: 16px; text-align: right; }
.table-card { border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.03); }
.empty-icon-text {
  color: var(--text-secondary);
  font-size: 12px;
}
.menu-type-alert { margin-bottom: 16px; }
.menu-type-group { width: 100%; display: flex; }
.menu-type-group :deep(.ant-radio-button-wrapper) {
  flex: 1 1 0;
  text-align: center;
}
.field-hint {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.icon-preview-row {
  margin-top: 10px;
}
.icon-chip-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}
.icon-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.2s ease;
}
.icon-chip:hover,
.icon-chip.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.08);
}
.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }

@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .icon-chip-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
