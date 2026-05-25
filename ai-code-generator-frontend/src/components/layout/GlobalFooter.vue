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
  padding: 18px 24px 24px;
  background: rgba(248, 251, 255, 0.92);
  backdrop-filter: blur(14px);
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 -10px 30px rgba(15, 23, 42, 0.04);
  font-size: 14px;
  color: #64748b;
  letter-spacing: 0.02em;
}

.global-footer a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.global-footer a:hover {
  text-decoration: underline;
}
</style>
