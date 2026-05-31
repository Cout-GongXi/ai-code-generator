<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <a-space v-if="!featured">
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app.deployKey" type="default" @click="handleViewWork"
            >查看作品</a-button
          >
        </a-space>
        <button
          v-else-if="app.deployKey"
          class="featured-view-btn"
          @click.stop="handleViewWork"
        >
          查看作品
        </button>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="52">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <div class="app-subtitle">
          <p class="app-author">
            {{ app.user?.userName || (featured ? '官方' : '未知用户') }}
          </p>
          <span v-if="app.createTime" class="app-date">
            {{ formatDate(app.createTime) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { formatDate } from '@/utils/time'

interface Props {
  app: API.AppVO
  featured?: boolean
}
interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}
const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(148, 163, 184, 0.16);
  transition:
    transform 0.3s,
    box-shadow 0.3s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 24px 54px rgba(37, 99, 235, 0.16);
}

.app-preview {
  height: 180px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-placeholder {
  font-size: 48px;
  color: #d9d9d9;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-card--featured .app-overlay {
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.55) 0%,
    rgba(0, 0, 0, 0.15) 60%,
    rgba(0, 0, 0, 0) 100%
  );
  align-items: flex-end;
  padding: 14px;
}

.featured-view-btn {
  width: 100%;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
  font-size: 15px;
  font-weight: 500;
  padding: 10px 0;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.2s;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.18);
}

.featured-view-btn:hover {
  background: #ffffff;
  transform: translateY(-1px);
}

.app-info {
  padding: 16px 18px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 16px;
  font-weight: 450;
  margin: 0 0 4px;
  color: #0f172a;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-subtitle {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 14px;
  min-width: 0;
}

.app-author {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
}

.app-date {
  white-space: nowrap;
  color: #64748b;
  flex-shrink: 0;
}
</style>
