<template>
  <div
    class="tool-call-card"
    :class="[`action-${block.action}`, { clickable: block.action !== 'delete' && block.action !== 'read' }]"
    @click="onClick"
  >
    <div class="card-icon">
      <component :is="iconComponent" />
    </div>
    <div class="card-body">
      <div class="card-title">
        <span class="file-name">{{ block.fileName }}</span>
        <span v-if="actionLabel" class="action-label">{{ actionLabel }}</span>
        <a-spin v-if="block.streaming" size="small" class="streaming-spin" />
      </div>
      <div class="card-subtitle">{{ block.filePath }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FileAddOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import type { ToolCallBlock } from '@/utils/parseToolCall'

interface Props {
  block: ToolCallBlock
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'open', filePath: string): void
}>()

const iconComponent = computed(() => {
  switch (props.block.action) {
    case 'write':
      return FileAddOutlined
    case 'modify':
      return EditOutlined
    case 'delete':
      return DeleteOutlined
    default:
      return FileTextOutlined
  }
})

const actionLabel = computed(() => {
  switch (props.block.action) {
    case 'write':
      return '新建'
    case 'modify':
      return '修改'
    case 'delete':
      return '删除'
    case 'read':
      return '读取'
    default:
      return ''
  }
})

function onClick() {
  if (props.block.action === 'delete' || props.block.action === 'read') return
  emit('open', props.block.filePath)
}
</script>

<style scoped>
.tool-call-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 6px 0;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: #ffffff;
  max-width: 100%;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.05s;
}

.tool-call-card.clickable {
  cursor: pointer;
}

.tool-call-card.clickable:hover {
  border-color: rgba(37, 99, 235, 0.4);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
}

.tool-call-card.clickable:active {
  transform: translateY(1px);
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #ffffff;
  flex-shrink: 0;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.action-modify .card-icon {
  background: linear-gradient(135deg, #f59e0b, #fb923c);
}

.action-delete .card-icon {
  background: linear-gradient(135deg, #ef4444, #f87171);
}

.action-read .card-icon,
.action-unknown .card-icon {
  background: linear-gradient(135deg, #64748b, #94a3b8);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.2;
}

.file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 240px;
}

.action-label {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.12);
}

.streaming-spin {
  margin-left: 2px;
}

.card-subtitle {
  font-size: 11px;
  color: #94a3b8;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}
</style>
