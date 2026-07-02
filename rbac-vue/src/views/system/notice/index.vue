<template>
  <div class="notice-page">
    <a-card class="search-card slide-in-1" :bordered="false">
      <a-space wrap>
        <a-input v-model:value="query.keyword" allow-clear placeholder="搜索标题或正文" style="width: 260px" @pressEnter="fetchList" />
        <a-select v-model:value="query.status" allow-clear placeholder="状态" style="width: 140px">
          <a-select-option :value="0">草稿</a-select-option>
          <a-select-option :value="1">已发布</a-select-option>
          <a-select-option :value="2">已撤回</a-select-option>
        </a-select>
        <a-button type="primary" @click="fetchList">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </a-space>
    </a-card>

    <a-card class="table-card slide-in-2" :bordered="false">
      <template #title>公告消息管理</template>
      <template #extra>
        <a-button v-hasPerm="['sys:notice:add']" type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增公告
        </a-button>
      </template>

      <a-table :dataSource="noticeList" :columns="columns" rowKey="id" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="notice-title">{{ record.title }}</div>
            <div class="notice-meta">{{ record.noticeType }} · {{ record.priority }}</div>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
          </template>
          <template v-if="column.key === 'stats'">
            <a-space size="small">
              <a-tag>总 {{ record.recipientTotal || 0 }}</a-tag>
              <a-tag color="success">已读 {{ record.readTotal || 0 }}</a-tag>
              <a-tag color="warning">未读 {{ record.unreadTotal || 0 }}</a-tag>
            </a-space>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-hasPerm="['sys:notice:list']" @click="openStats(record)">统计</a>
              <a-divider type="vertical" />
              <a v-if="record.status !== 1" v-hasPerm="['sys:notice:edit']" @click="openEdit(record)">编辑</a>
              <a-divider v-if="record.status !== 1" type="vertical" />
              <a v-if="record.status !== 1" v-hasPerm="['sys:notice:publish']" @click="publishNotice(record)">发布</a>
              <a v-if="record.status === 1" v-hasPerm="['sys:notice:withdraw']" @click="withdrawNotice(record)">撤回</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除该公告吗？" @confirm="deleteNotice(record)">
                <a v-hasPerm="['sys:notice:delete']" class="delete-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer :title="form.id ? '编辑公告' : '新增公告'" :open="drawerVisible" width="760" @close="drawerVisible = false">
      <a-form layout="vertical" :model="form">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="请输入公告标题" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="类型">
              <a-select v-model:value="form.noticeType">
                <a-select-option value="公告">公告</a-select-option>
                <a-select-option value="消息">消息</a-select-option>
                <a-select-option value="警告">警告</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优先级">
              <a-select v-model:value="form.priority">
                <a-select-option value="普通">普通</a-select-option>
                <a-select-option value="重要">重要</a-select-option>
                <a-select-option value="紧急">紧急</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="正文" required>
          <a-textarea v-model:value="form.content" :rows="8" placeholder="请输入公告正文" />
        </a-form-item>

        <div class="target-section">
          <div class="target-header">
            <div>
              <div class="target-title">可见范围</div>
              <div class="target-desc">最终接收人 = 包含范围命中的用户 - 排除范围命中的用户</div>
            </div>
            <a-space>
              <a-button size="small" @click="addTarget('INCLUDE')">添加包含</a-button>
              <a-button size="small" @click="addTarget('EXCLUDE')">添加排除</a-button>
            </a-space>
          </div>

          <div v-for="(target, index) in form.targets" :key="index" class="target-row">
            <a-tag :color="target.effect === 'INCLUDE' ? 'success' : 'error'">
              {{ target.effect === 'INCLUDE' ? '包含' : '排除' }}
            </a-tag>
            <a-select v-model:value="target.targetType" style="width: 120px" @change="target.targetId = null">
              <a-select-option value="ALL">全部</a-select-option>
              <a-select-option value="DEPT">部门</a-select-option>
              <a-select-option value="ROLE">角色</a-select-option>
              <a-select-option value="USER">用户</a-select-option>
            </a-select>
            <a-tree-select
              v-if="target.targetType === 'DEPT'"
              v-model:value="target.targetId"
              :treeData="deptTree"
              treeNodeFilterProp="title"
              placeholder="选择部门"
              style="flex: 1"
            />
            <a-select
              v-else-if="target.targetType === 'ROLE'"
              v-model:value="target.targetId"
              show-search
              optionFilterProp="label"
              placeholder="选择角色"
              style="flex: 1"
              :options="roleOptions"
            />
            <a-select
              v-else-if="target.targetType === 'USER'"
              v-model:value="target.targetId"
              show-search
              optionFilterProp="label"
              placeholder="选择用户"
              style="flex: 1"
              :options="userOptions"
            />
            <a-input v-else value="全部在线及离线用户" disabled style="flex: 1" />
            <a-button danger type="link" @click="form.targets.splice(index, 1)">移除</a-button>
          </div>
        </div>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="submitForm">保存草稿</a-button>
        </a-space>
      </template>
    </a-drawer>

    <a-modal v-model:open="statsModal.open" title="阅读统计" :footer="null">
      <a-row :gutter="16">
        <a-col :span="8"><a-statistic title="接收人数" :value="statsModal.data.recipientTotal || 0" /></a-col>
        <a-col :span="8"><a-statistic title="已读人数" :value="statsModal.data.readTotal || 0" :value-style="{ color: 'var(--color-success)' }" /></a-col>
        <a-col :span="8"><a-statistic title="未读人数" :value="statsModal.data.unreadTotal || 0" :value-style="{ color: 'var(--color-warning)' }" /></a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import request from '@/api/request'

const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const noticeList = ref([])
const roleList = ref([])
const userList = ref([])
const deptTreeRaw = ref([])
const query = reactive({ keyword: '', status: undefined })
const form = reactive({ id: null, title: '', content: '', noticeType: '公告', priority: '普通', targets: [{ targetType: 'ALL', targetId: null, effect: 'INCLUDE' }] })
const statsModal = reactive({ open: false, data: {} })

const statusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已发布', color: 'success' },
  2: { text: '已撤回', color: 'warning' }
}

const columns = [
  { title: '公告', key: 'title' },
  { title: '状态', key: 'status', width: 110 },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 180 },
  { title: '阅读统计', key: 'stats', width: 260 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 260 }
]

const roleOptions = computed(() => roleList.value.map(item => ({ value: item.id, label: `${item.roleName} (${item.roleKey})` })))
const userOptions = computed(() => userList.value.map(item => ({ value: item.id, label: `${item.nickname || item.username} (${item.username})` })))
const deptTree = computed(() => mapDeptTree(deptTreeRaw.value))

const mapDeptTree = list => list.map(item => ({
  title: item.deptName,
  value: item.id,
  key: item.id,
  children: item.children ? mapDeptTree(item.children) : []
}))

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/notice/list', { params: query })
    noticeList.value = res.data || []
  } finally {
    loading.value = false
  }
}

const fetchOptions = async () => {
  const [roles, users, depts] = await Promise.all([
    request.get('/system/role/list'),
    request.get('/system/user/list'),
    request.get('/system/dept/tree')
  ])
  roleList.value = roles.data || []
  userList.value = users.data || []
  deptTreeRaw.value = depts.data || []
}

const resetQuery = () => {
  query.keyword = ''
  query.status = undefined
  fetchList()
}

const resetForm = () => {
  Object.assign(form, { id: null, title: '', content: '', noticeType: '公告', priority: '普通', targets: [{ targetType: 'ALL', targetId: null, effect: 'INCLUDE' }] })
}

const openCreate = () => {
  resetForm()
  drawerVisible.value = true
}

const openEdit = async record => {
  resetForm()
  const [detail, targets] = await Promise.all([
    request.get(`/system/notice/detail/${record.id}`),
    request.get(`/system/notice/targets/${record.id}`)
  ])
  Object.assign(form, {
    id: detail.data.id,
    title: detail.data.title,
    content: detail.data.content,
    noticeType: detail.data.noticeType || '公告',
    priority: detail.data.priority || '普通',
    targets: targets.data?.length ? targets.data.map(item => ({
      targetType: item.targetType,
      targetId: item.targetId,
      effect: item.effect
    })) : [{ targetType: 'ALL', targetId: null, effect: 'INCLUDE' }]
  })
  drawerVisible.value = true
}

const addTarget = effect => {
  form.targets.push({ targetType: effect === 'INCLUDE' ? 'ALL' : 'USER', targetId: null, effect })
}

const submitForm = async () => {
  submitting.value = true
  try {
    await request.post(form.id ? '/system/notice/edit' : '/system/notice/add', form)
    message.success('保存成功')
    drawerVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

const publishNotice = async record => {
  await request.post(`/system/notice/publish/${record.id}`)
  message.success('已发布并推送')
  fetchList()
}

const withdrawNotice = async record => {
  await request.post(`/system/notice/withdraw/${record.id}`)
  message.success('已撤回')
  fetchList()
}

const deleteNotice = async record => {
  await request.delete(`/system/notice/delete/${record.id}`)
  message.success('已删除')
  fetchList()
}

const openStats = async record => {
  const res = await request.get(`/system/notice/stats/${record.id}`)
  statsModal.data = res.data || {}
  statsModal.open = true
}

onMounted(() => {
  fetchOptions()
  fetchList()
})
</script>

<style scoped>
.notice-page { display: flex; flex-direction: column; gap: 16px; }
.search-card, .table-card { border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.notice-title { font-weight: 700; color: var(--text-main); }
.notice-meta { color: var(--text-sub); font-size: 12px; margin-top: 4px; }
.delete-link { color: var(--color-danger); }
.target-section { border: 1px solid var(--border-color); border-radius: 8px; padding: 14px; }
.target-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.target-title { font-weight: 700; color: var(--text-main); }
.target-desc { font-size: 12px; color: var(--text-sub); margin-top: 4px; }
.target-row { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.slide-in-1 { animation: slide-up 0.5s both; }
.slide-in-2 { animation: slide-up 0.5s both 0.08s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.dark-mode .search-card, .dark-mode .table-card { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
</style>
