<template>
  <ul class="file-tree" :class="{ 'is-root': depth === 0 }">
    <li v-for="node in nodes" :key="node.path" class="tree-node">
      <div
        class="tree-row"
        :class="{
          'is-dir': node.isDir,
          'is-file': !node.isDir,
          active: !node.isDir && node.path === activePath,
        }"
        :style="{ paddingLeft: depth * 14 + 8 + 'px' }"
        @click="onClickNode(node)"
      >
        <span class="tree-toggle">
          <RightOutlined
            v-if="node.isDir"
            class="toggle-icon"
            :class="{ open: isExpanded(node.path) }"
          />
        </span>
        <span class="tree-icon">
          <FolderOpenOutlined
            v-if="node.isDir && isExpanded(node.path)"
            class="dir-icon open"
          />
          <FolderOutlined v-else-if="node.isDir" class="dir-icon" />
          <FileTextOutlined v-else class="file-icon" />
        </span>
        <span class="tree-name">{{ node.name }}</span>
        <span v-if="!node.isDir && node.block" class="tree-badge" :class="`badge-${node.block.action}`">
          {{ badgeLabel(node.block.action) }}
        </span>
        <a-spin v-if="!node.isDir && node.block?.streaming" size="small" class="tree-spin" />
      </div>
      <FileTree
        v-if="node.isDir && node.children && isExpanded(node.path)"
        :nodes="node.children"
        :active-path="activePath"
        :expanded="expanded"
        :depth="depth + 1"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import {
  RightOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import type { FileTreeNode } from '@/utils/buildFileTree'
import type { ToolCallAction } from '@/utils/parseToolCall'

interface Props {
  nodes: FileTreeNode[]
  activePath: string | null
  expanded: Set<string>
  depth?: number
}

const props = withDefaults(defineProps<Props>(), { depth: 0 })

const emit = defineEmits<{
  (e: 'select', filePath: string): void
  (e: 'toggle', dirPath: string): void
}>()

function isExpanded(path: string): boolean {
  return props.expanded.has(path)
}

function onClickNode(node: FileTreeNode) {
  if (node.isDir) {
    emit('toggle', node.path)
  } else {
    emit('select', node.path)
  }
}

function badgeLabel(action: ToolCallAction): string {
  switch (action) {
    case 'write':
      return '新'
    case 'modify':
      return '改'
    case 'delete':
      return '删'
    default:
      return ''
  }
}
</script>

<style scoped>
.file-tree {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 13px;
  color: #334155;
  user-select: none;
}

.file-tree.is-root {
  padding: 4px 0;
}

.tree-node {
  line-height: 1.4;
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px 4px 0;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.12s;
  white-space: nowrap;
  overflow: hidden;
}

.tree-row:hover {
  background: rgba(148, 163, 184, 0.12);
}

.tree-row.active {
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  font-weight: 600;
}

.tree-toggle {
  width: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.toggle-icon {
  font-size: 10px;
  color: #64748b;
  transition: transform 0.15s;
}

.toggle-icon.open {
  transform: rotate(90deg);
}

.tree-icon {
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  flex-shrink: 0;
}

.dir-icon {
  color: #f59e0b;
}

.dir-icon.open {
  color: #fbbf24;
}

.file-icon {
  color: #64748b;
}

.tree-row.active .file-icon {
  color: #2563eb;
}

.tree-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.tree-badge {
  font-size: 10px;
  padding: 0 5px;
  border-radius: 4px;
  font-weight: 600;
  flex-shrink: 0;
}

.badge-write {
  color: #15803d;
  background: #dcfce7;
}

.badge-modify {
  color: #b45309;
  background: #fef3c7;
}

.badge-delete {
  color: #b91c1c;
  background: #fee2e2;
}

.tree-spin {
  margin-left: 2px;
}
</style>
