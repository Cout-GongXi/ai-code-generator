<template>
  <div id="userManagePage" class="user-manage-page">
    <a-card title="用户管理">
      <!-- 搜索表单 -->
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input
            v-model:value="searchParams.userAccount"
            placeholder="输入账号"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input
            v-model:value="searchParams.userName"
            placeholder="输入用户名"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>

      <div class="table-toolbar">
        <a-button type="primary" @click="doAdd">创建用户</a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar v-if="record.userAvatar" :src="record.userAvatar" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
            <a-tag v-else color="blue">普通用户</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="doEdit(record)">编辑</a-button>
              <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建 / 编辑用户 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="modalFormRef"
        :model="modalForm"
        :rules="modalRules"
        layout="vertical"
        autocomplete="off"
      >
        <a-form-item v-if="isEdit" label="id">
          <a-input v-model:value="modalForm.id" disabled />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="账号" name="userAccount">
          <a-input
            v-model:value="modalForm.userAccount"
            placeholder="请输入账号"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input
            v-model:value="modalForm.userName"
            placeholder="请输入用户名"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="头像" name="userAvatar">
          <a-input
            v-model:value="modalForm.userAvatar"
            placeholder="请输入头像 URL"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="简介" name="userProfile">
          <a-textarea
            v-model:value="modalForm.userProfile"
            placeholder="请输入简介"
            :rows="3"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="角色" name="userRole">
          <a-select v-model:value="modalForm.userRole" placeholder="请选择角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'
import {
  addUser,
  deleteUser,
  listUserVoByPage,
  updateUser,
} from '@/api/userController.ts'

const columns = [
  { title: 'id', dataIndex: 'id', width: 120 },
  { title: '账号', dataIndex: 'userAccount',width: 150 },
  { title: '用户名', dataIndex: 'userName',width: 150 },
  { title: '头像', dataIndex: 'userAvatar', width: 80 },
  { title: '简介', dataIndex: 'userProfile',  ellipsis: true },
  { title: '用户角色', dataIndex: 'userRole', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140 },
]

const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => `共 ${count} 页`,
}))

const modalOpen = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalFormRef = ref<FormInstance>()
// 表单
const modalForm = reactive<API.UserAddRequest & API.UserUpdateRequest>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const modalRules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少 4 位', trigger: 'blur' },
  ],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const modalTitle = computed(() => (isEdit.value ? '编辑用户' : '创建用户'))

function formatTime(time?: string) {
  if (!time) {
    return '-'
  }
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) {
    return time
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({ ...searchParams })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '获取数据失败')
    }
  } catch {
    message.error('获取数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
// 表格分页变化时的操作
const doTableChange = (page: TablePaginationConfig) => {
  searchParams.pageNum = page.current ?? 1
  searchParams.pageSize = page.pageSize ?? 10
  fetchData()
}
// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const resetModalForm = () => {
  modalForm.id = undefined
  modalForm.userAccount = ''
  modalForm.userName = ''
  modalForm.userAvatar = ''
  modalForm.userProfile = ''
  modalForm.userRole = 'user'
}

const doAdd = () => {
  isEdit.value = false
  resetModalForm()
  modalOpen.value = true
}

const doEdit = (record: API.UserVO) => {
  isEdit.value = true
  modalForm.id = record.id
  modalForm.userName = record.userName ?? ''
  modalForm.userAvatar = record.userAvatar ?? ''
  modalForm.userProfile = record.userProfile ?? ''
  modalForm.userRole = record.userRole ?? 'user'
  modalOpen.value = true
}

const handleModalCancel = () => {
  modalOpen.value = false
  modalFormRef.value?.resetFields()
}

const handleModalOk = async () => {
  try {
    await modalFormRef.value?.validate()
  } catch {
    return
  }

  modalLoading.value = true
  try {
    if (isEdit.value) {
      const res = await updateUser({
        id: modalForm.id,
        userName: modalForm.userName,
        userAvatar: modalForm.userAvatar,
        userProfile: modalForm.userProfile,
        userRole: modalForm.userRole,
      })
      if (res.data.code === 0) {
        message.success('更新成功')
        modalOpen.value = false
        await fetchData()
      } else {
        message.error(res.data.message ?? '更新失败')
      }
    } else {
      const res = await addUser({
        userAccount: modalForm.userAccount,
        userName: modalForm.userName,
        userAvatar: modalForm.userAvatar,
        userProfile: modalForm.userProfile,
        userRole: modalForm.userRole,
      })
      if (res.data.code === 0) {
        message.success('创建成功，默认密码 12345678')
        modalOpen.value = false
        await fetchData()
      } else {
        message.error(res.data.message ?? '创建失败')
      }
    }
  } catch {
    message.error(isEdit.value ? '更新失败，请稍后重试' : '创建失败，请稍后重试')
  } finally {
    modalLoading.value = false
  }
}

const doDelete = async (id?: number) => {
  if (!id) {
    return
  }
  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      await fetchData()
    } else {
      message.error(res.data.message ?? '删除失败')
    }
  } catch {
    message.error('删除失败，请稍后重试')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-manage-page {
  width: 100%;
}

.table-toolbar {
  margin: 16px 0;
}
</style>
