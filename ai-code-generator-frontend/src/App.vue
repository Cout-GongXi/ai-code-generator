<template>
  <BasicLayout />
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import {onMounted} from 'vue';
const loginUserStore = useLoginUserStore()
// 应用启动时获取登录用户信息
onMounted(() => {
  loginUserStore.fetchLoginUser()
})
</script>
<style scoped>
body {
  height: 100%;
  margin: 0;
}
</style>
