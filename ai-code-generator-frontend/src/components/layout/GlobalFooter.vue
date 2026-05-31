<template>
  <footer v-if="isHome" class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <img class="footer-logo" src="@/assets/logo.png" alt="Logo" />
        <span class="footer-title">坤代码</span>
      </div>

      <div class="footer-meta">
        <span>AI 代码生成器 by</span>
        <a :href="BLOG_URL" target="_blank" rel="noopener noreferrer">zzy</a>
        <span class="dot">·</span>
        <span>© {{ year }}</span>
      </div>

      <div class="footer-links">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
          京ICP备XXXXXXXX号
        </a>
        <span class="dot">·</span>
        <a
          href="http://www.beian.gov.cn/portal/registerSystemInfo"
          target="_blank"
          rel="noopener noreferrer"
        >
          京公网安备 XXXXXXXXXXXX号
        </a>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const BLOG_URL = 'https://zhenzy.asia'
const route = useRoute()
const isHome = computed(() => route.path === '/')
const year = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  background: transparent;
  border-top: none;
  margin-top: 0;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.01em;
  line-height: 1.4;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.footer-logo {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.footer-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.footer-meta,
.footer-links {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.footer-meta a,
.footer-links a {
  color: #64748b;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-meta a:hover,
.footer-links a:hover {
  color: #2563eb;
}

.dot {
  color: rgba(148, 163, 184, 0.5);
}

@media (max-width: 720px) {
  .footer-inner {
    padding: 8px 16px;
    font-size: 11px;
  }
}
</style>
