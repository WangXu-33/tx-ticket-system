<template>
  <div class="page-container">
    <a-card class="settings-card slide-in-1" :bordered="false" :bodyStyle="{ padding: 0 }">
      <a-layout class="settings-layout">
        <a-layout-sider width="224" class="settings-sider" theme="light">
          <div class="sider-title">存储引擎设置</div>
          <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="inline"
            class="settings-menu"
          >
            <a-menu-item key="localStorageService">
              <template #icon><hdd-outlined /></template>
              本地存储 (Local)
              <a-badge v-if="configForm['system.storage.active'] === 'localStorageService'" status="processing" style="margin-left: 8px;" />
            </a-menu-item>
            <a-menu-item key="aliyunOssService">
              <template #icon><cloud-outlined /></template>
              阿里云 (OSS)
              <a-badge v-if="configForm['system.storage.active'] === 'aliyunOssService'" status="processing" style="margin-left: 8px;" />
            </a-menu-item>
            <a-menu-item key="minioService">
              <template #icon><database-outlined /></template>
              私有云 (MinIO)
              <a-badge v-if="configForm['system.storage.active'] === 'minioService'" status="processing" style="margin-left: 8px;" />
            </a-menu-item>
          </a-menu>
        </a-layout-sider>

        <a-layout-content class="settings-content">
          <div class="content-header">
            <h2 class="content-title">
              {{ currentMenuTitle }}
              <a-tag v-if="configForm['system.storage.active'] === selectedKeys[0]" color="success" style="margin-left: 12px; font-weight: normal;">当前使用中</a-tag>
            </h2>
            <div class="content-desc">{{ currentMenuDesc }}</div>
          </div>

          <a-form layout="vertical" :model="configForm" class="config-form" hideRequiredMark>
            
            <!-- 本地存储配置 -->
            <div v-show="selectedKeys[0] === 'localStorageService'" class="form-section fade-in">
              <a-row :gutter="32">
                <a-col :span="20">
                  <a-form-item label="物理存储路径">
                    <a-input v-model:value="configForm['local.storage.path']" placeholder="如: D:/tx_ticket_upload/ 或 /usr/local/upload/" size="large" />
                    <div class="tip-text">服务器上的绝对物理路径，需确保应用有读写权限。</div>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="32">
                <a-col :span="20">
                  <a-form-item label="外网访问前缀 (Domain)">
                    <a-input v-model:value="configForm['local.storage.domain']" placeholder="如: /tx_files/" size="large" />
                    <div class="tip-text">本字段用于本地物理映射和兼容旧数据。前端预览与下载统一通过文件 ID 接口访问。</div>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="32">
                <a-col :span="10">
                  <a-form-item label="附件最大上传大小（MB）">
                    <a-input-number v-model:value="configForm['file.security.max-size-mb']" :min="1" :max="1024" style="width: 100%" size="large" />
                    <div class="tip-text">工单、知识库和系统文件上传共用该限制。</div>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="32">
                <a-col :span="20">
                  <a-form-item label="允许上传后缀">
                    <a-textarea v-model:value="configForm['file.security.allowed-suffixes']" :rows="3" placeholder=".jpg,.png,.pdf,.docx,.xlsx,.zip" />
                    <div class="tip-text">英文逗号分隔；留空表示只按禁止后缀拦截。</div>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="32">
                <a-col :span="20">
                  <a-form-item label="禁止上传后缀">
                    <a-textarea v-model:value="configForm['file.security.blocked-suffixes']" :rows="3" placeholder=".exe,.bat,.cmd,.sh,.ps1,.jar" />
                    <div class="tip-text">该配置优先级高于允许后缀，用于拦截脚本、可执行文件和高风险包。</div>
                  </a-form-item>
                </a-col>
              </a-row>
            </div>

            <!-- 阿里云 OSS 配置 -->
            <div v-show="selectedKeys[0] === 'aliyunOssService'" class="form-section fade-in">
              <a-row :gutter="32">
                <a-col :span="12">
                  <a-form-item label="Endpoint API">
                    <a-input v-model:value="configForm['aliyun.oss.endpoint']" placeholder="如: oss-cn-hangzhou.aliyuncs.com" size="large" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="BucketName">
                    <a-input v-model:value="configForm['aliyun.oss.bucketName']" placeholder="如: my-bucket" size="large" />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="32">
                <a-col :span="12">
                  <a-form-item label="AccessKeyId">
                    <a-input v-model:value="configForm['aliyun.oss.accessKeyId']" placeholder="请输入 AccessKeyId" size="large" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="AccessKeySecret">
                    <a-input-password v-model:value="configForm['aliyun.oss.accessKeySecret']" placeholder="********" size="large" />
                    <div class="tip-text">自动128位 AES 加密存储。若不修改请留空或保留掩码。</div>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="32">
                <a-col :span="24">
                  <a-form-item label="自定义访问域名 (可选)">
                    <a-input v-model:value="configForm['aliyun.oss.customDomain']" placeholder="如: https://oss.example.com" size="large" />
                    <div class="tip-text">配置后，返回的 URL 将使用该域名替换默认的 Endpoint 域名。</div>
                  </a-form-item>
                </a-col>
              </a-row>
            </div>

            <!-- MinIO 配置 -->
            <div v-show="selectedKeys[0] === 'minioService'" class="form-section fade-in">
              <a-row :gutter="32">
                <a-col :span="12">
                  <a-form-item label="Endpoint API">
                    <a-input v-model:value="configForm['minio.endpoint']" placeholder="如: 192.168.1.100:9000 或 minio.example.com" size="large" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="启用 HTTPS 协议">
                    <a-switch v-model:checked="configForm['minio.useHttps']" checked-children="开启" un-checked-children="关闭" checkedValue="1" unCheckedValue="0" />
                    <span style="margin-left: 10px; font-size: 13px; color: #64748b;">API 将通过 {{ configForm['minio.useHttps'] === '1' ? 'https://' : 'http://' }} 发起请求</span>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="32">
                <a-col :span="12">
                  <a-form-item label="AccessKey">
                    <a-input v-model:value="configForm['minio.accessKey']" placeholder="请输入 AccessKey" size="large" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="SecretKey">
                    <a-input-password v-model:value="configForm['minio.secretKey']" placeholder="********" size="large" />
                    <div class="tip-text">自动128位 AES 加密存储。</div>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="32">
                <a-col :span="12">
                  <a-form-item label="BucketName">
                    <a-input v-model:value="configForm['minio.bucketName']" placeholder="如: my-bucket" size="large" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="外部映射域名 (可选)">
                    <a-input v-model:value="configForm['minio.customDomain']" placeholder="如: https://img.example.com" size="large" />
                    <div class="tip-text">主要用于 Nginx 代理 MinIO 后的外网图片直显访问。</div>
                  </a-form-item>
                </a-col>
              </a-row>
            </div>

            <!-- 底部操作区 -->
            <div class="form-actions">
              <a-space size="large">
                <a-button 
                  v-if="configForm['system.storage.active'] !== selectedKeys[0]" 
                  v-hasPerm="['sys:config:edit']"
                  type="primary" 
                  ghost 
                  size="large"
                  @click="setActiveEngine"
                >
                  <template #icon><check-circle-outlined /></template>
                  设为系统当前激活引擎
                </a-button>

                <a-button type="primary" size="large" :loading="submittingConfig" @click="submitConfig" v-hasPerm="['sys:config:edit']">
                  <template #icon><save-outlined /></template>
                  保存参数配置
                </a-button>

                <a-button size="large" @click="openConfig">
                  <template #icon><reload-outlined /></template>
                  重置
                </a-button>
              </a-space>
            </div>

          </a-form>
        </a-layout-content>
      </a-layout>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { 
  SaveOutlined, ReloadOutlined, HddOutlined, CloudOutlined, DatabaseOutlined, CheckCircleOutlined 
} from '@ant-design/icons-vue'
import request from '@/api/request'
import { message } from 'ant-design-vue'

const selectedKeys = ref(['localStorageService'])
const submittingConfig = ref(false)

const configForm = reactive({
  'system.storage.active': 'localStorageService',
  'local.storage.path': '',
  'local.storage.domain': '',
  'file.security.max-size-mb': '20',
  'file.security.allowed-suffixes': '.jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar',
  'file.security.blocked-suffixes': '.exe,.bat,.cmd,.sh,.ps1,.jar,.war,.msi,.dll,.com,.scr',
  'aliyun.oss.endpoint': '',
  'aliyun.oss.bucketName': '',
  'aliyun.oss.accessKeyId': '',
  'aliyun.oss.accessKeySecret': '',
  'aliyun.oss.customDomain': '',
  'minio.endpoint': '',
  'minio.useHttps': '0',
  'minio.accessKey': '',
  'minio.secretKey': '',
  'minio.bucketName': '',
  'minio.customDomain': ''
})

const currentMenuTitle = computed(() => {
  const map = {
    localStorageService: '本地存储 (Local)',
    aliyunOssService: '阿里云 (OSS)',
    minioService: '私有云 (MinIO)'
  }
  return map[selectedKeys.value[0]] || '存储设置'
})

const currentMenuDesc = computed(() => {
  const map = {
    localStorageService: '将文件存储在应用服务器的物理磁盘中。适用于单体架构或拥有独立大容量存储池的轻量级应用。',
    aliyunOssService: '将文件存储至阿里云对象存储 OSS，具备高可用性和海量并发支撑，适用于大型互联网应用。',
    minioService: '使用自建的私有云对象存储 MinIO，兼顾了成本控制与 S3 协议标准兼容，满足内网隔离要求。'
  }
  return map[selectedKeys.value[0]] || ''
})

const openConfig = async () => {
  try {
    const res = await request.get('/system/config/getOssConfig')
    configForm['minio.useHttps'] = '0'
    Object.assign(configForm, res.data)
    
    // 如果获取到的激活引擎存在，初始化选中激活引擎对应的左侧菜单
    if (configForm['system.storage.active']) {
      selectedKeys.value = [configForm['system.storage.active']]
    }
  } catch (e) {
    message.error('获取配置失败')
  }
}

const setActiveEngine = () => {
  configForm['system.storage.active'] = selectedKeys.value[0]
}

const submitConfig = async () => {
  submittingConfig.value = true
  try {
    const payload = Object.fromEntries(Object.entries(configForm).map(([key, value]) => [key, value == null ? '' : String(value)]))
    await request.post('/system/config/saveOssConfig', payload)
    message.success('配置已保存并实时生效')
  } finally {
    submittingConfig.value = false
  }
}

onMounted(() => {
  openConfig()
})
</script>

<style scoped>
.page-container { 
  display: flex; 
  flex-direction: column; 
  height: 100%;
}
.settings-card { 
  border-radius: 12px; 
  box-shadow: 0 4px 24px rgba(0,0,0,0.04); 
  flex: 1;
  overflow: hidden;
}
.settings-layout {
  background: transparent;
  min-height: 600px;
}
.settings-sider {
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
}
.sider-title {
  padding: 24px 24px 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.settings-menu {
  background: transparent;
  border-right: none;
}
.settings-menu :deep(.ant-menu-item) {
  margin-top: 8px;
  margin-bottom: 8px;
  border-radius: 0;
  width: 100%;
  margin-left: 0;
}
.settings-menu :deep(.ant-menu-item-selected) {
  background-color: #e6f4ff;
  border-right: 3px solid #1677ff;
}
.settings-content {
  padding: 32px 48px;
  background: #ffffff;
}
.content-header {
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.content-title {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}
.content-desc {
  color: #64748b;
  font-size: 14px;
}
.config-form {
  max-width: 800px;
}
.tip-text {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 6px;
  line-height: 1.5;
}
.form-actions {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px dashed #e2e8f0;
}
.fade-in { animation: fade-in 0.4s ease-out; }
@keyframes fade-in { from { opacity: 0; transform: translateX(10px); } to { opacity: 1; transform: translateX(0); } }
.slide-in-1 { animation: slide-up 0.6s both 0.1s; }
@keyframes slide-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* 暗黑模式适配 */
.dark-mode .settings-sider { background: #0f172a; border-right-color: #334155; }
.dark-mode .settings-content { background: #020617; }
.dark-mode .sider-title { color: #f8fafc; }
.dark-mode .content-title { color: #f8fafc; }
.dark-mode .content-header { border-bottom-color: #334155; }
.dark-mode .form-actions { border-top-color: #334155; }
.dark-mode .settings-menu :deep(.ant-menu-item-selected) { background-color: rgba(99, 102, 241, 0.15); border-right-color: #818cf8; color: #818cf8; }
.dark-mode .settings-menu :deep(.ant-menu-item-selected .anticon) { color: #818cf8; }
</style>
