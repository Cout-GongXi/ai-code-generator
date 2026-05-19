<script setup lang="ts">
import { Layout } from 'ant-design-vue'
import GlobalHeader from '@/components/layout/GlobalHeader.vue'
import GlobalFooter from '@/components/layout/GlobalFooter.vue'

const { Header, Content, Footer } = Layout
</script>

<template>
  <Layout class="basic-layout">
    <Header class="basic-layout-header">
      <GlobalHeader />
    </Header>

    <Content class="basic-layout-content">
      <RouterView />
    </Content>

    <Footer class="basic-layout-footer">
      <GlobalFooter />
    </Footer>
  </Layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.basic-layout-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
  height: 64px;
  line-height: 64px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.basic-layout-content {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 16px;
  background: #f5f5f5;
}

.basic-layout-footer {
  flex-shrink: 0;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 768px) {
  .basic-layout-content {
    padding: 16px 12px;
  }

  .basic-layout-footer {
    padding: 12px 16px;
  }
}
</style>
