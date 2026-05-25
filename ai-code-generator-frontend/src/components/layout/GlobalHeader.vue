<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.png" alt="Logo" />
            <h1 class="site-title">坤代码</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  LogoutOutlined,
  HomeOutlined,
  BoldOutlined,
  UserOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})
// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(UserOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: 'others',
    icon: () => h(BoldOutlined),
    label: h(
      'a',
      { href: 'https://zhenzy.asia', target: '_blank' },
      '个人博客',
    ),
    title: '个人博客',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: rgba(248, 251, 255, 0.92);
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 1000;
  width: 100%;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: inherit;
}

.logo {
  height: 48px;
  width: 48px;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.site-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.user-login-status {
  color: #334155;
}

.user-login-status :deep(.ant-space) {
  gap: 10px !important;
}

.user-login-status :deep(.ant-avatar) {
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.ant-menu-horizontal {
  border-bottom: none !important;
  background: transparent;
  color: #475569;
}

.header :deep(.ant-menu-horizontal::before),
.header :deep(.ant-menu-horizontal > .ant-menu-item::after),
.header :deep(.ant-menu-horizontal > .ant-menu-submenu::after) {
  display: none;
}

.header :deep(.ant-menu-horizontal > .ant-menu-item),
.header :deep(.ant-menu-horizontal > .ant-menu-submenu) {
  margin-inline: 8px;
  padding-inline: 10px;
  color: #475569;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.header :deep(.ant-menu-horizontal > .ant-menu-item-selected) {
  color: #2563eb;
  font-weight: 600;
  border-bottom-color: transparent;
}

.header :deep(.ant-menu-horizontal > .ant-menu-item:not(.ant-menu-item-selected):hover),
.header
  :deep(.ant-menu-horizontal > .ant-menu-submenu:not(.ant-menu-submenu-selected):hover) {
  color: #334155;
}

.header :deep(.ant-btn-primary) {
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
}

.header :deep(a) {
  color: inherit;
  text-decoration: none;
}

:deep(.ant-row) {
  align-items: center;
  min-height: 72px;
}
</style>
