<template>
  <header class="site-header" :class="{ scrolled }">
    <div class="header-inner">
      <!-- 左侧 Logo -->
      <RouterLink to="/" class="logo-wrap">
        <img class="logo" src="@/assets/logo.png" alt="Logo" />
        <span class="site-title">坤代码</span>
      </RouterLink>

      <!-- 中间导航 -->
      <nav class="nav">
        <RouterLink to="/" class="nav-link" :class="{ active: currentPath === '/' }">主页</RouterLink>
        <RouterLink
          v-if="loginUserStore.loginUser.id"
          to="/my/apps"
          class="nav-link"
          :class="{ active: currentPath === '/my/apps' }"
        >我的应用</RouterLink>
        <template v-if="isAdmin">
          <RouterLink to="/admin/userManage" class="nav-link" :class="{ active: currentPath === '/admin/userManage' }">用户管理</RouterLink>
          <RouterLink to="/admin/appManage" class="nav-link" :class="{ active: currentPath === '/admin/appManage' }">应用管理</RouterLink>
          <RouterLink to="/admin/chatHistoryManage" class="nav-link" :class="{ active: currentPath === '/admin/chatHistoryManage' }">对话管理</RouterLink>
        </template>
        <a href="http://38.14.254.239:8090" target="_blank" class="nav-link">个人博客</a>
      </nav>

      <!-- 右侧用户区 -->
      <div class="user-area">
        <template v-if="loginUserStore.loginUser.id">
          <a-dropdown placement="bottomRight">
            <div class="avatar-circle">
              <img
                v-if="loginUserStore.loginUser.userAvatar"
                :src="loginUserStore.loginUser.userAvatar"
                class="avatar-img"
              />
              <span v-else class="avatar-letter">
                {{ (loginUserStore.loginUser.userName ?? '?')[0] }}
              </span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item disabled class="menu-username">
                  {{ loginUserStore.loginUser.userName ?? '用户' }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="doOpenProfile">
                  <UserOutlined />
                  个人资料
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <RouterLink v-else to="/user/login" class="login-btn">登录</RouterLink>
      </div>
    </div>
  </header>

  <!-- 个人资料弹窗 -->
  <a-modal
    v-model:open="profileVisible"
    title="个人资料"
    @ok="doUpdateProfile"
    ok-text="保存"
    cancel-text="取消"
    :width="460"
  >
    <a-form :model="profileForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }" style="margin-top: 20px">
      <a-form-item label="用户名">
        <a-input v-model:value="profileForm.userName" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="头像地址">
        <a-input v-model:value="profileForm.userAvatar" placeholder="请输入头像图片链接" />
      </a-form-item>
      <a-form-item label="个人简介">
        <a-textarea v-model:value="profileForm.userProfile" :rows="3" placeholder="介绍一下自己" />
      </a-form-item>
      <a-form-item label="新密码">
        <a-input-password v-model:value="profileForm.password" placeholder="不修改请留空" />
      </a-form-item>
      <a-form-item label="确认密码">
        <a-input-password v-model:value="profileForm.confirmPassword" placeholder="不修改请留空" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout, updateUserVo } from '@/api/userController.ts'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()

const currentPath = computed(() => route.path)
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const scrolled = ref(false)
const onScroll = () => { scrolled.value = window.scrollY > 10 }
onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))

// 个人资料弹窗
const profileVisible = ref(false)
const profileForm = reactive<API.UserUpdateRequest>({})

const doOpenProfile = () => {
  profileForm.userName = loginUserStore.loginUser.userName
  profileForm.userAvatar = loginUserStore.loginUser.userAvatar
  profileForm.userProfile = loginUserStore.loginUser.userProfile
  profileForm.password = ''
  profileForm.confirmPassword = ''
  profileVisible.value = true
}

const doUpdateProfile = async () => {
  if (profileForm.password && profileForm.password !== profileForm.confirmPassword) {
    message.error('两次密码不一致')
    return
  }
  const params: API.UserUpdateRequest = {
    id: loginUserStore.loginUser.id,
    userName: profileForm.userName,
    userAvatar: profileForm.userAvatar,
    userProfile: profileForm.userProfile,
  }
  if (profileForm.password) {
    params.password = profileForm.password
    params.confirmPassword = profileForm.confirmPassword
  }
  const res = await updateUserVo(params)
  if (res.data.code === 0) {
    message.success('更新成功')
    profileVisible.value = false
    await loginUserStore.fetchLoginUser()
  } else {
    message.error('更新失败，' + res.data.message)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border-bottom: 1px solid transparent;
  box-shadow: none;
  transition: background 0.3s, border-color 0.3s, box-shadow 0.3s, backdrop-filter 0.3s;
}

.site-header.scrolled {
  background: rgba(248, 251, 255, 0.72);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 1px 0 rgba(148, 163, 184, 0.12), 0 4px 20px rgba(15, 23, 42, 0.05);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 40px;
}

/* Logo */
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.site-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.01em;
}

/* 导航 */
.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.nav-link {
  font-size: 14px;
  color: #555;
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 8px;
  transition: color 0.2s, background 0.2s;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.active {
  color: #1a1a1a;
  background: rgba(0, 0, 0, 0.05);
}

/* 用户区 */
.user-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* 登录按钮 */
.login-btn {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  text-decoration: none;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.6);
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.login-btn:hover {
  color: #1a1a1a;
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(148, 163, 184, 0.55);
}

/* 用户头像圆形 */
.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s, transform 0.2s;
}

.avatar-circle:hover {
  opacity: 0.85;
  transform: scale(1.06);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-letter {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1;
}

:global(.menu-username) {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a !important;
  cursor: default !important;
  padding: 8px 16px !important;
}
</style>
