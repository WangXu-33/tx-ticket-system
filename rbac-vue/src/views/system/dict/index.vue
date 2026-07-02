<template>
  <div class="page-container">
    <!-- 轻量级字典数据统计摘要 -->
    <a-card class="stat-card slide-in-1" :bordered="false" style="margin-bottom: 16px;">
      <a-row :gutter="24">
        <a-col :span="4">
          <a-statistic title="字典分类总数" :value="statsSummary.typeTotal || 0" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="已启用分类" :value="statsSummary.enabledTypeTotal || 0" :value-style="{ color: 'var(--color-success)' }" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="已禁用分类" :value="statsSummary.disabledTypeTotal || 0" :value-style="{ color: 'var(--color-danger)' }" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="字典数据总项数" :value="statsSummary.dataItemTotal || 0" />
        </a-col>
        <a-col :span="8">
          <div style="font-size: 14px; color: #64748b; margin-bottom: 8px;">热门数据项 Top 3</div>
          <div v-if="statsSummary.dataItemTop && statsSummary.dataItemTop.length > 0">
            <a-tag v-for="item in statsSummary.dataItemTop.slice(0, 3)" :key="item.name" color="blue">
              {{ item.name }} ({{ item.value }}项)
            </a-tag>
          </div>
          <div v-else style="font-size: 12px; color: #94a3b8;">暂无数据</div>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="16" class="slide-in-2">
      <!-- 左侧：字典分类树 (前端实时搜索) -->
      <a-col :span="6">
        <a-card title="字典分类" :bordered="false" class="dict-tree-card">
          <template #extra>
            <a-button v-hasPerm="['sys:dict:add']" type="primary" size="small" @click="handleTypeAdd">
              <template #icon><plus-outlined /></template>
            </a-button>
          </template>
          <div class="search-wrapper">
            <a-input
              v-model:value="typeKeyword"
              placeholder="搜索名称或编码..."
              allow-clear
            >
              <template #prefix><search-outlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input>
          </div>
          <a-menu
            v-model:selectedKeys="selectedTypeKeys"
            mode="vertical"
            class="dict-type-menu"
            @select="onTypeSelect"
          >
            <a-menu-item v-for="type in filteredTypeList" :key="type.code">
              <div class="type-item">
                <span class="type-label">{{ type.name }}</span>
                <span class="type-code">{{ type.code }}</span>
                <div class="type-actions">
                  <edit-outlined v-hasPerm="['sys:dict:edit']" @click.stop="handleTypeEdit(type)" />
                  <a-popconfirm title="确定删除吗？" @confirm="handleTypeDelete(type.id)">
                    <delete-outlined v-hasPerm="['sys:dict:delete']" @click.stop />
                  </a-popconfirm>
                </div>
              </div>
            </a-menu-item>
            <a-empty v-if="filteredTypeList.length === 0" description="未找到匹配分类" :image="false" />
          </a-menu>
        </a-card>
      </a-col>

      <!-- 右侧：字典数据列表 (树形表格) -->
      <a-col :span="18">
        <a-card :title="`[${activeTypeCode}] 字典项明细`" :bordered="false" class="dict-data-card">
          <template #extra>
            <a-button v-hasPerm="['sys:dict:add']" type="primary" @click="handleDataAdd" :disabled="!activeTypeCode">
              <template #icon><plus-outlined /></template>新增字典项
            </a-button>
          </template>

          <a-table
            :dataSource="dataList"
            :columns="dataColumns"
            rowKey="id"
            :pagination="false"
            :loading="dataLoading"
            childrenColumnName="children"
            :defaultExpandAllRows="true"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'error'">
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a v-hasPerm="['sys:dict:add']" class="table-action-link" @click="handleDataAdd(record)">新增子项</a>
                  <a-divider type="vertical" />
                  <a v-hasPerm="['sys:dict:edit']" class="edit-link" @click="handleDataEdit(record)">编辑</a>
                  <a-divider type="vertical" />
                  <a-popconfirm title="确定删除吗？" @confirm="handleDataDelete(record.id)">
                    <a v-hasPerm="['sys:dict:delete']" class="delete-link">删除</a>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- 字典项弹窗 -->
    <a-modal v-model:open="dataModal.open" :title="dataModal.title" @ok="handleDataSubmit">
      <a-form :model="dataForm" layout="vertical">
        <a-form-item label="上级项ID" v-if="dataForm.parentId !== 0">
          <a-input :value="dataForm.parentId" disabled />
        </a-form-item>
        <a-form-item label="字典标签" required><a-input v-model:value="dataForm.label" /></a-form-item>
        <a-form-item label="字典键值" required><a-input v-model:value="dataForm.value" /></a-form-item>
        <a-form-item label="显示排序"><a-input-number v-model:value="dataForm.sort" style="width: 100%" /></a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="dataForm.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 字典分类弹窗 -->
    <a-modal v-model:open="typeModal.open" :title="typeModal.title" @ok="handleTypeSubmit">
       <a-form :model="typeForm" layout="vertical">
         <a-form-item label="分类名称" required><a-input v-model:value="typeForm.name" /></a-form-item>
         <a-form-item label="分类编码" required><a-input v-model:value="typeForm.code" :disabled="!!typeForm.id" /></a-form-item>
       </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'

const typeKeyword = ref('')
const typeList = ref([])
const selectedTypeKeys = ref([])
const activeTypeCode = ref('')

const statsSummary = ref({})

// 前端实时过滤逻辑
const filteredTypeList = computed(() => {
  const keyword = typeKeyword.value.trim().toLowerCase()
  if (!keyword) return typeList.value
  return typeList.value.filter(item => 
    item.name.toLowerCase().includes(keyword) || 
    item.code.toLowerCase().includes(keyword)
  )
})

const dataList = ref([])
const dataLoading = ref(false)
const dataColumns = [
  { title: '字典标签', dataIndex: 'label', key: 'label' },
  { title: '字典键值', dataIndex: 'value', key: 'value' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220 }
]

const fetchStats = async () => {
  try {
    const res = await request.get('/system/dict/stats/summary')
    statsSummary.value = res.data || {}
  } catch (e) {
    console.error("Fetch dict stats failed", e)
  }
}

const fetchTypeList = async () => {
  const res = await request.get('/system/dict/type/list')
  typeList.value = res.data
  if (res.data.length > 0 && !activeTypeCode.value) {
    selectedTypeKeys.value = [res.data[0].code]
    activeTypeCode.value = res.data[0].code
    fetchDataList()
  }
}

const onTypeSelect = ({ key }) => {
  activeTypeCode.value = key
  fetchDataList()
}

const fetchDataList = async () => {
  if (!activeTypeCode.value) return
  dataLoading.value = true
  try {
    const res = await request.get('/system/dict/data/list', { params: { typeCode: activeTypeCode.value } })
    dataList.value = res.data
  } finally {
    dataLoading.value = false
  }
}

const dataModal = reactive({ open: false, title: '', isEdit: false })
const dataForm = reactive({ id: null, typeCode: '', parentId: 0, label: '', value: '', sort: 1, status: 1 })
const handleDataAdd = (record) => {
  Object.assign(dataForm, { id: null, typeCode: activeTypeCode.value, parentId: record?.id || 0, label: '', value: '', sort: 1, status: 1 })
  dataModal.title = record ? '新增子项' : '新增字典项'
  dataModal.isEdit = false; dataModal.open = true;
}
const handleDataEdit = (record) => { Object.assign(dataForm, record); dataModal.title = '编辑字典项'; dataModal.isEdit = true; dataModal.open = true; }
const handleDataSubmit = async () => {
  await request.post(dataModal.isEdit ? '/system/dict/data/edit' : '/system/dict/data/add', dataForm)
  message.success('操作成功'); dataModal.open = false; fetchDataList(); fetchStats();
}
const handleDataDelete = async (id) => { await request.delete(`/system/dict/data/delete/${id}`); message.success('删除成功'); fetchDataList(); fetchStats(); }

const typeModal = reactive({ open: false, title: '', isEdit: false })
const typeForm = reactive({ id: null, name: '', code: '' })
const handleTypeAdd = () => { Object.assign(typeForm, { id: null, name: '', code: '' }); typeModal.open = true; typeModal.isEdit = false; typeModal.title = '新增分类'; }
const handleTypeEdit = (type) => { Object.assign(typeForm, type); typeModal.open = true; typeModal.isEdit = true; typeModal.title = '编辑分类'; }
const handleTypeSubmit = async () => {
  await request.post(typeModal.isEdit ? '/system/dict/type/edit' : '/system/dict/type/add', typeForm)
  message.success('操作成功'); typeModal.open = false; fetchTypeList(); fetchStats();
}
const handleTypeDelete = async (id) => { await request.delete(`/system/dict/type/delete/${id}`); message.success('删除成功'); fetchTypeList(); fetchStats(); }

onMounted(() => {
  fetchStats()
  fetchTypeList()
})
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; }
.search-wrapper { padding: 10px; margin-bottom: 10px; }
.dict-type-menu { border-inline-end: none !important; }
.type-item { display: flex; align-items: center; width: 100%; position: relative; }
.type-label { font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; }
.type-code { font-size: 11px; color: #94a3b8; margin-left: 8px; }
.type-actions { display: none; margin-left: 10px; gap: 8px; color: #94a3b8; }
.dict-type-menu :deep(.ant-menu-item-selected) .type-actions { display: flex; }
.dict-tree-card, .dict-data-card { border-radius: 12px; height: calc(100vh - 120px); overflow-y: auto; }
.delete-link { color: var(--color-danger); }

.stat-card { border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); background: var(--bg-card); }
.slide-in-1 { animation: slide-up 0.6s both; }
.slide-in-2 { animation: slide-up 0.6s both 0.1s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

:deep(.ant-card) { transition: all 0.3s; }
.dark-mode :deep(.ant-card) { background: #0f172a; border: 1px solid rgba(255,255,255,0.05); }
</style>
