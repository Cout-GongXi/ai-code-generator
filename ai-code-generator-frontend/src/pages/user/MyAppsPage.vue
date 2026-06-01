<template>
  <div id="myAppsPage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input
          v-model:value="searchParams.appName"
          placeholder="输入应用名称"
        />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          placeholder="选择生成类型"
          style="width: 150px"
          allow-clear
        >
          <a-select-option
            v-for="option in CODE_GEN_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      @change="doTableChange"
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image
            v-if="record.cover"
            :src="record.cover"
            :width="80"
            :height="60"
          />
          <div v-else class="no-cover">无封面</div>
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <div class="prompt-text">{{ record.initPrompt }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          {{ formatCodeGenType(record.codeGenType) }}
        </template>
        <template v-else-if="column.dataIndex === 'deployedTime'">
          <span v-if="record.deployedTime">
            {{ formatTime(record.deployedTime) }}
          </span>
          <span v-else class="text-gray">未部署</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button
              type="primary"
              size="small"
              @click="viewChat(record.id)"
            >
              查看对话
            </a-button>
            <a-button
              v-if="record.deployKey"
              type="default"
              size="small"
              @click="viewWork(record)"
            >
              查看作品
            </a-button>
            <a-button
              type="default"
              size="small"
              @click="editApp(record)"
            >
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除这个应用吗？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="deleteAppById(record.id)"
            >
              <a-button danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listUserAppsVoByPage, deleteApp } from '@/api/appController'
import {
  CODE_GEN_TYPE_OPTIONS,
  formatCodeGenType,
} from '../../utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import { getDeployUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const columns = [
  { title: '应用名称', dataIndex: 'appName', width: 160, fixed: 'left' },
  { title: '封面', dataIndex: 'cover', width: 100 },
  { title: '初始提示词', dataIndex: 'initPrompt', width: 240 },
  { title: '生成类型', dataIndex: 'codeGenType', width: 110 },
  { title: '部署时间', dataIndex: 'deployedTime', width: 170 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 320, fixed: 'right' },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'desc',
})

// 加载数据
const fetchData = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  loading.value = true
  try {
    const res = await listUserAppsVoByPage({ ...searchParams })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = Number(res.data.data.totalRow) || 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 个应用`,
}))

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 查看对话历史
const viewChat = (appId: number | undefined) => {
  if (!appId) return
  router.push(`/app/chat/${appId}`)
}

// 查看部署作品
const viewWork = (app: API.AppVO) => {
  if (!app.deployKey) {
    message.warning('该应用尚未部署')
    return
  }
  window.open(getDeployUrl(app.deployKey), '_blank')
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  if (!app.id) return
  router.push(`/app/edit/${app.id}`)
}

// 删除应用
const deleteAppById = async (id: number | undefined) => {
  if (!id) return
  try {
    const res = await deleteApp({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}
</script>

<style scoped>
#myAppsPage {
  padding: 24px;
  margin: 16px 24px 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.no-cover {
  width: 80px;
  height: 60px;
  background: #f8fbff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 12px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.prompt-text {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-gray {
  color: #94a3b8;
}

:deep(.ant-form-inline) {
  row-gap: 12px;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-select-selector) {
  border-radius: 12px !important;
  border-color: rgba(148, 163, 184, 0.26) !important;
  background: #f8fbff !important;
}

:deep(.ant-btn-primary) {
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
}

:deep(.ant-divider) {
  border-color: rgba(148, 163, 184, 0.18);
}

:deep(.ant-table-wrapper) {
  overflow: hidden;
  border-radius: 16px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #334155;
  font-weight: 600;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  border-bottom-color: rgba(226, 232, 240, 0.9);
}
</style>
