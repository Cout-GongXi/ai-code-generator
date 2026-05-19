<template>
  <div class="global-header">
    <div class="header-brand">
      <img :src="logoUrl" alt="logo" class="header-logo" />
      <span class="site-title">{{ SITE_TITLE }}</span>
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

    <div class="header-actions">
      <a-button type="primary" @click="handleLogin">登录</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { Menu } from 'ant-design-vue'
import logoUrl from '@/assets/logo.png'
import { SITE_TITLE, appMenuItems } from '@/config/menu'

const router = useRouter()
const route = useRoute()

const selectedKeys = ref<string[]>([])

const menuItems = computed<MenuProps['items']>(() =>
  appMenuItems.map((item) => ({
    key: item.key,
    label: item.label,
  })),
)

function syncSelectedKeys() {
  const matched = appMenuItems.find((item) => item.path === route.path)
  selectedKeys.value = matched ? [matched.key] : []
}

watch(() => route.path, syncSelectedKeys, { immediate: true })

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  const target = appMenuItems.find((item) => item.key === key)
  if (target) {
    router.push(target.path)
  }
}

function handleLogin() {
  // TODO: 接入登录流程
}
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

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
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

.header-actions {
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
