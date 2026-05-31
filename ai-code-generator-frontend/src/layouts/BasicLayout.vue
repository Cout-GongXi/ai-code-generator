<template>
  <a-layout class="basic-layout" :class="{ 'home-bg': isHome }">
    <!-- 顶部导航栏 -->
    <GlobalHeader v-if="!isStandalonePage" />
    <!-- 主要内容区域 -->
    <a-layout-content
      class="main-content"
      :class="{ 'standalone-content': isStandalonePage }"
    >
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter v-if="!isStandalonePage" />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/layout/GlobalHeader.vue'
import GlobalFooter from '@/components/layout/GlobalFooter.vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isStandalonePage = computed(() => route.meta.standalone === true)
const isHome = computed(() => route.path === '/')
</script>

<style scoped>
.basic-layout {
  background: none;
  min-height: 100vh;
}

.basic-layout.home-bg {
  background: #eef3f8;
}

.main-content {
  width: 100%;
  padding: 64px 0 0;
  background: none;
  margin: 0;
  position: relative;
  z-index: 1;
}

.standalone-content {
  min-height: 100vh;
  padding-top: 0;
}
</style>
