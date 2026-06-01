<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
    <a-table :columns="columns" :data-source="data" :pagination="pagination" :loading="loading" @change="doTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <a-select
            :value="record.userRole"
            size="small"
            class="role-select"
            :class="record.userRole === 'admin' ? 'role-admin' : 'role-user'"
            @change="(val: string) => doChangeRole(record.id, val)"
          >
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button class="edit-btn" @click="doOpenEdit(record)">编辑</a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>

  <!-- 编辑用户弹窗 -->
  <a-modal
    v-model:open="editVisible"
    :footer="null"
    :width="460"
    class="edit-user-modal"
    centered
  >
    <div class="modal-body">
      <!-- 头部：头像 + 账号 -->
      <div class="modal-header-area">
        <div class="avatar-preview">
          <img v-if="editForm.userAvatar" :src="editForm.userAvatar" class="avatar-img" />
          <div v-else class="avatar-placeholder">
            {{ (editForm.userName ?? '?')[0] }}
          </div>
        </div>
        <div class="user-account-label">{{ currentAccount }}</div>
      </div>

      <!-- 表单 -->
      <a-form :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" class="edit-form">
        <a-form-item label="用户名">
          <a-input v-model:value="editForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像地址">
          <a-input v-model:value="editForm.userAvatar" placeholder="请输入头像图片链接" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea v-model:value="editForm.userProfile" :rows="3" placeholder="介绍一下自己" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="editForm.password" placeholder="不修改请留空" />
        </a-form-item>
        <a-form-item label="确认密码">
          <a-input-password v-model:value="editForm.confirmPassword" placeholder="不修改请留空" />
        </a-form-item>
      </a-form>

      <!-- 底部按钮 -->
      <div class="modal-footer">
        <a-button @click="editVisible = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="doUpdate">保存</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage, updateUserByAdmin } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  { title: 'id', dataIndex: 'id' },
  { title: '账号', dataIndex: 'userAccount' },
  { title: '用户名', dataIndex: 'userName' },
  { title: '头像', dataIndex: 'userAvatar' },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '用户角色', dataIndex: 'userRole' },
  { title: '创建时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action' },
]

const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const searchParams = reactive<API.UserQueryRequest>({ pageNum: 1, pageSize: 10 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({ ...searchParams })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
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

const doDelete = async (id: string) => {
  if (!id) return
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 行内修改角色
const doChangeRole = async (id: number | undefined, role: string) => {
  const res = await updateUserByAdmin({ id, userRole: role })
  if (res.data.code === 0) {
    message.success('角色已更新')
    fetchData()
  } else {
    message.error('更新失败，' + res.data.message)
  }
}

// 编辑个人信息
const editVisible = ref(false)
const saving = ref(false)
const currentAccount = ref('')
const editForm = reactive<API.UserUpdateByAdminRequest>({})

const doOpenEdit = (record: API.UserVO) => {
  editForm.id = record.id
  editForm.userName = record.userName
  editForm.userAvatar = record.userAvatar
  editForm.userProfile = record.userProfile
  editForm.password = ''
  editForm.confirmPassword = ''
  currentAccount.value = record.userAccount ?? ''
  editVisible.value = true
}

const doUpdate = async () => {
  if (editForm.password && editForm.password !== editForm.confirmPassword) {
    message.error('两次密码不一致')
    return
  }
  saving.value = true
  const params: API.UserUpdateByAdminRequest = {
    id: editForm.id,
    userName: editForm.userName,
    userAvatar: editForm.userAvatar,
    userProfile: editForm.userProfile,
  }
  if (editForm.password) {
    params.password = editForm.password
    params.confirmPassword = editForm.confirmPassword
  }
  const res = await updateUserByAdmin(params)
  saving.value = false
  if (res.data.code === 0) {
    message.success('更新成功')
    editVisible.value = false
    fetchData()
  } else {
    message.error('更新失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#userManagePage {
  padding: 24px;
  margin: 16px 24px 0;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

:deep(.ant-form-inline) { row-gap: 12px; }

:deep(.ant-input),
:deep(.ant-input-affix-wrapper) {
  border-radius: 12px;
  border-color: rgba(148, 163, 184, 0.26);
  background: #f8fbff;
}

:deep(.ant-btn-primary) {
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
}

:deep(.ant-divider) { border-color: rgba(148, 163, 184, 0.18); }

:deep(.ant-table-wrapper) { overflow: hidden; border-radius: 16px; }
:deep(.ant-table) { background: transparent; }
:deep(.ant-table-thead > tr > th) { background: #f8fbff; color: #334155; font-weight: 600; }
:deep(.ant-table-tbody > tr > td) { vertical-align: middle; border-bottom-color: rgba(226, 232, 240, 0.9); }
:deep(.ant-pagination) { margin: 20px 0 0; }

/* 角色下拉 */
.role-select { min-width: 90px; }
.role-admin :deep(.ant-select-selector) {
  border-color: #52c41a !important;
  color: #52c41a !important;
  background: #f6ffed !important;
  border-radius: 6px !important;
}
.role-user :deep(.ant-select-selector) {
  border-color: #1677ff !important;
  color: #1677ff !important;
  background: #e6f4ff !important;
  border-radius: 6px !important;
}

/* 编辑按钮 */
.edit-btn {
  border-radius: 8px;
  border-color: rgba(148, 163, 184, 0.35);
  color: #475569;
}
.edit-btn:hover {
  border-color: #2563eb !important;
  color: #2563eb !important;
}

/* 弹窗内容 */
:global(.edit-user-modal .ant-modal-content) {
  border-radius: 20px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14);
}

:global(.edit-user-modal .ant-modal-close) {
  top: 16px;
  right: 16px;
  color: #94a3b8;
}

.modal-body {
  padding: 28px 28px 24px;
}

/* 头部头像区 */
.modal-header-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(37, 99, 235, 0.12);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.user-account-label {
  font-size: 13px;
  color: #94a3b8;
  letter-spacing: 0.02em;
}

/* 表单 */
.edit-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.edit-form :deep(.ant-form-item-label > label) {
  color: #475569;
  font-size: 13px;
}

.edit-form :deep(.ant-input),
.edit-form :deep(.ant-input-affix-wrapper),
.edit-form :deep(.ant-input-textarea textarea) {
  border-radius: 10px;
  border-color: rgba(148, 163, 184, 0.28);
  background: #f8fbff;
  font-size: 14px;
}

.edit-form :deep(.ant-input:focus),
.edit-form :deep(.ant-input-affix-wrapper:focus),
.edit-form :deep(.ant-input-affix-wrapper-focused) {
  border-color: rgba(37, 99, 235, 0.4);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

/* 底部按钮 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}

.modal-footer :deep(.ant-btn) {
  border-radius: 10px;
  height: 36px;
  padding: 0 20px;
}

.modal-footer :deep(.ant-btn-default) {
  border-color: rgba(148, 163, 184, 0.3);
  color: #64748b;
}

.modal-footer :deep(.ant-btn-primary) {
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.2);
}
</style>
