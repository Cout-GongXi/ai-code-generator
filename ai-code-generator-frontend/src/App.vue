<template>
  <BasicLayout />
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>
<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu',
    'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

html {
  overflow-x: hidden;
}

/* 全局隐藏滚动条（保留滚动功能） */
* {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

*::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Edge, Opera */
  width: 0 !important;
  height: 0 !important;
}

*::-webkit-scrollbar-track {
  display: none;
}

*::-webkit-scrollbar-thumb {
  display: none;
}
</style>
