<template>
  <div id="chatHistoryManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="消息内容">
        <a-input
          v-model:value="searchParams.message"
          placeholder="输入消息内容"
        />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="searchParams.messageType"
          placeholder="选择消息类型"
          style="width: 150px"
          allow-clear
        >
          <a-select-option value="user">用户</a-select-option>
          <a-select-option value="ai">AI</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="应用ID">
        <a-input v-model:value="searchParams.appId" placeholder="输入应用ID" />
      </a-form-item>
      <a-form-item label="用户ID">
        <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" />
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
      @change="doTableChange"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag v-if="record.messageType === 'user'" color="blue">用户</a-tag>
          <a-tag v-else-if="record.messageType === 'ai'" color="green">AI</a-tag>
          <span v-else>{{ record.messageType }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <a-tooltip :title="record.message">
            <div class="message-text">{{ record.message }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="goToApp(record.appId)">
              查看应用
            </a-button>
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
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 100,
    fixed: 'left',
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 100,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 360,
  },
  {
    title: '应用ID',
    dataIndex: 'appId',
    width: 120,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    fixed: 'right',
  },
]

// 数据
const data = ref<API.ChatHistory[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 跳转到应用对话页面
const goToApp = (appId: number | undefined) => {
  if (!appId) return
  router.push(`/app/chat/${appId}`)
}
</script>

<style scoped>
#chatHistoryManagePage {
  padding: 24px;
  margin: 16px 24px 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.message-text {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
