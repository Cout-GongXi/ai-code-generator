<template>
  <div class="global-footer">
    AI 代码生成器 by
    <a :href="BLOG_URL" target="_blank" rel="noopener noreferrer">zzy</a>
  </div>
</template>

<script setup lang="ts">
const BLOG_URL = 'https://zhenzy.asia'
</script>

<style scoped>
.global-footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.global-footer a {
  color: #1677ff;
  text-decoration: none;
}

.global-footer a:hover {
  text-decoration: underline;
}
</style>
