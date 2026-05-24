<template>
  <div class="global-header">
    <div class="header-brand">
      <router-link to="/">
        <img :src="logoUrl" alt="logo" class="header-logo" />
        <span class="site-title">{{ SITE_TITLE }}</span>
      </router-link>
    </div>

    <div class="header-menu">
      <Menu
        v-model:selected-keys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        :overflowed-indicator="null"
        @click="handleMenuClick"
      />
    </div>

    <div class="user-login-status">
      <div v-if="loginUserStore.loginUser.id">
        <a-dropdown>
          <a-space>
            <a-avatar :src="loginUserStore.loginUser.userAvatar" />
            {{ loginUserStore.loginUser.userName ?? '无名' }}
          </a-space>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handleLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div v-else>
        <a-button type="primary" @click="handleLogin">登录</a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { Menu } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
import logoUrl from '@/assets/logo.png'
import { SITE_TITLE, appMenuItems } from '@/config/menu'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()

const selectedKeys = ref<string[]>([])

const originMenuItems: MenuProps['items'] = appMenuItems.map((item) => ({
  key: item.path,
  label: item.label,
}))

/**
 * 根据权限过滤菜单项
 */
const filterMenus = (menus: MenuProps['items'] = []) => {
  return menus?.filter((menu) => {
    const menuKey = String(menu?.key ?? '')
    // 管理员才能看到 /admin 开头的菜单
    if (menuKey.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const menuItems = computed(() => filterMenus(originMenuItems))

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}

const handleLogin = () => {
  router.push('/user/login')
}

const handleLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({
        userName: '未登录',
      })
      message.success('退出成功')
      await router.replace('/user/login')
    } else {
      message.error(res.data.message ?? '退出失败')
    }
  } catch {
    message.error('退出失败，请稍后重试')
  }
}

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 16px;
}

.header-brand a {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.header-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.site-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.header-menu :deep(.ant-menu) {
  border-bottom: none;
  background: transparent;
  line-height: 64px;
}

.user-login-status {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .site-title {
    font-size: 16px;
  }

  .header-logo {
    width: 28px;
    height: 28px;
  }

  .global-header {
    gap: 8px;
    padding: 0 12px;
  }
}

@media (max-width: 576px) {
  .site-title {
    display: none;
  }
}
</style>
