<template>
  <div class="code-tabs-panel">
    <div v-if="files.length === 0" class="empty-state">
      <div class="empty-icon">📄</div>
      <p>暂无生成的代码文件</p>
      <p class="empty-tip">AI 写入或修改文件时，会自动在这里以标签页展示</p>
    </div>
    <template v-else>
      <!-- 左侧文件树 -->
      <aside class="tree-pane" :style="{ width: treeWidth + 'px' }">
        <div class="tree-header">
          <FolderOpenOutlined />
          <span class="tree-header-title">文件</span>
          <span class="tree-header-count">{{ files.length }}</span>
        </div>
        <div class="tree-scroll">
          <FileTree
            :nodes="treeNodes"
            :active-path="activePath"
            :expanded="expanded"
            @select="$emit('select', $event)"
            @toggle="toggleDir"
          />
        </div>
      </aside>
      <!-- 拖动手柄 -->
      <div class="tree-resizer" @mousedown="onResizerDown"></div>
      <!-- 右侧 Tab + 代码 -->
      <section class="code-pane">
        <div v-if="openedFiles.length > 0" class="tab-bar" ref="tabBarRef">
          <div
            v-for="file in openedFiles"
            :key="file.filePath"
            class="tab-item"
            :class="{ active: file.filePath === activePath, streaming: file.streaming }"
            :title="file.filePath"
            @click="$emit('select', file.filePath)"
            @mousedown.middle.prevent="closeTab(file.filePath)"
          >
            <component :is="iconFor(file)" class="tab-icon" />
            <span class="tab-name">{{ file.fileName }}</span>
            <span v-if="file.action === 'modify'" class="tab-badge modify">修改</span>
            <span v-else-if="file.action === 'delete'" class="tab-badge delete">删除</span>
            <a-spin v-if="file.streaming" size="small" class="tab-spin" />
            <button
              class="tab-close"
              :title="`关闭 ${file.fileName}`"
              @click.stop="closeTab(file.filePath)"
            >
              <CloseOutlined />
            </button>
          </div>
        </div>
        <div class="tab-body">
          <div v-if="activeFile" class="file-meta">
            <span class="file-meta-path">{{ activeFile.filePath }}</span>
            <span v-if="activeFile.language" class="file-meta-lang">{{ activeFile.language }}</span>
          </div>
          <pre v-if="activeFile" class="code-block hljs"><code v-html="highlightedHtml"></code></pre>
          <div v-else class="no-tab-state">
            <div class="no-tab-icon">📂</div>
            <p>没有打开的文件</p>
            <p class="no-tab-tip">在左侧文件树中点击文件以预览</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick, onUnmounted } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {
  FileAddOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'
import FileTree from './FileTree.vue'
import type { ToolCallBlock } from '@/utils/parseToolCall'
import { buildFileTree, collectDirPaths } from '@/utils/buildFileTree'

interface Props {
  files: ToolCallBlock[]
  activePath: string | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'select', filePath: string | null): void
}>()

const tabBarRef = ref<HTMLElement>()

// 文件树宽度（像素），支持拖动
const treeWidth = ref(220)

const treeNodes = computed(() => buildFileTree(props.files))

// 已打开 Tab 的路径列表，保持插入顺序
const openedPaths = ref<string[]>([])

// 渲染用：根据 openedPaths 顺序映射到当前的 ToolCallBlock
const openedFiles = computed<ToolCallBlock[]>(() => {
  return openedPaths.value
    .map((p) => props.files.find((f) => f.filePath === p))
    .filter((f): f is ToolCallBlock => !!f)
})

// activePath 变化时：自动把它加入已打开 Tab（追加到末尾）
watch(
  () => props.activePath,
  (path) => {
    if (path && !openedPaths.value.includes(path)) {
      openedPaths.value = [...openedPaths.value, path]
    }
  },
  { immediate: true },
)

// 当 props.files 变化时清理：移除已打开但已不存在的路径
watch(
  () => props.files,
  (files) => {
    const exists = new Set(files.map((f) => f.filePath))
    openedPaths.value = openedPaths.value.filter((p) => exists.has(p))
  },
)

function closeTab(filePath: string) {
  const idx = openedPaths.value.indexOf(filePath)
  if (idx === -1) return

  const next = [...openedPaths.value]
  next.splice(idx, 1)
  openedPaths.value = next

  // 关闭的不是当前 active，无需切换
  if (props.activePath !== filePath) return

  // 选相邻 Tab 作为新 active：优先右侧，否则左侧；都没有就清空
  if (next.length === 0) {
    emit('select', null)
  } else {
    const newIdx = Math.min(idx, next.length - 1)
    emit('select', next[newIdx])
  }
}

// 展开状态：默认全部展开
const expanded = ref<Set<string>>(new Set())
watch(
  treeNodes,
  (nodes) => {
    // 把新出现的文件夹默认设为展开（保留用户已折叠的状态）
    const current = expanded.value
    const dirs = collectDirPaths(nodes)
    const next = new Set(current)
    for (const d of dirs) {
      if (!current.has(d) && !manuallyCollapsed.value.has(d)) {
        next.add(d)
      }
    }
    expanded.value = next
  },
  { immediate: true },
)

// 用户手动折叠过的目录，避免新文件出现时被强制再次展开
const manuallyCollapsed = ref<Set<string>>(new Set())

function toggleDir(dirPath: string) {
  const next = new Set(expanded.value)
  if (next.has(dirPath)) {
    next.delete(dirPath)
    manuallyCollapsed.value.add(dirPath)
  } else {
    next.add(dirPath)
    manuallyCollapsed.value.delete(dirPath)
  }
  expanded.value = next
}

const activeFile = computed(() =>
  props.files.find((f) => f.filePath === props.activePath) || null,
)

const highlightedHtml = computed(() => {
  if (!activeFile.value) return ''
  const code = activeFile.value.content || ''
  const lang = activeFile.value.language
  if (lang && hljs.getLanguage(lang)) {
    try {
      return hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    } catch {
      // 忽略
    }
  }
  return escapeHtml(code)
})

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function iconFor(file: ToolCallBlock) {
  switch (file.action) {
    case 'write':
      return FileAddOutlined
    case 'modify':
      return EditOutlined
    case 'delete':
      return DeleteOutlined
    default:
      return FileTextOutlined
  }
}

// 拖拽改变文件树宽度
let dragStartX = 0
let dragStartWidth = 0
function onResizerDown(e: MouseEvent) {
  dragStartX = e.clientX
  dragStartWidth = treeWidth.value
  document.addEventListener('mousemove', onResizerMove)
  document.addEventListener('mouseup', onResizerUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
function onResizerMove(e: MouseEvent) {
  const delta = e.clientX - dragStartX
  const w = dragStartWidth + delta
  treeWidth.value = Math.max(140, Math.min(420, w))
}
function onResizerUp() {
  document.removeEventListener('mousemove', onResizerMove)
  document.removeEventListener('mouseup', onResizerUp)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}
onUnmounted(() => {
  document.removeEventListener('mousemove', onResizerMove)
  document.removeEventListener('mouseup', onResizerUp)
})

// 当 active 文件变化时，自动把对应 Tab 滚到可视区
watch(
  () => props.activePath,
  async () => {
    await nextTick()
    if (!tabBarRef.value) return
    const el = tabBarRef.value.querySelector('.tab-item.active') as HTMLElement | null
    if (el) {
      el.scrollIntoView({ inline: 'nearest', block: 'nearest', behavior: 'smooth' })
    }
  },
)
</script>

<style scoped>
.code-tabs-panel {
  height: 100%;
  display: flex;
  flex-direction: row;
  background: #ffffff;
  min-height: 0;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.empty-tip {
  margin-top: 6px !important;
  font-size: 12px;
  color: #cbd5e1;
}

/* 文件树面板 */
.tree-pane {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  min-width: 140px;
  overflow: hidden;
}

.tree-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tree-header-title {
  flex: 1;
}

.tree-header-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(148, 163, 184, 0.2);
  color: #475569;
  letter-spacing: 0;
}

.tree-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  padding: 4px 6px 8px;
}

.tree-resizer {
  width: 4px;
  flex-shrink: 0;
  cursor: col-resize;
  background: transparent;
  transition: background 0.15s;
}

.tree-resizer:hover {
  background: rgba(37, 99, 235, 0.3);
}

/* 代码面板 */
.code-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.tab-bar {
  display: flex;
  gap: 4px;
  padding: 8px 8px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fbff;
  overflow-x: auto;
  scrollbar-width: thin;
}

.tab-bar::-webkit-scrollbar {
  height: 6px;
}

.tab-bar::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.4);
  border-radius: 3px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  border-radius: 8px 8px 0 0;
  border: 1px solid transparent;
  border-bottom: none;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.6);
  color: #0f172a;
}

.tab-item.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 600;
  border-color: rgba(148, 163, 184, 0.24);
  position: relative;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: #ffffff;
}

.tab-icon {
  font-size: 13px;
  color: #3b82f6;
}

.tab-item.active .tab-icon {
  color: #2563eb;
}

.tab-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-badge {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 500;
}

.tab-badge.modify {
  color: #b45309;
  background: #fef3c7;
}

.tab-badge.delete {
  color: #b91c1c;
  background: #fee2e2;
}

.tab-spin {
  margin-left: 2px;
}

.tab-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-left: 2px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
}

.tab-item:hover .tab-close,
.tab-item.active .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: rgba(148, 163, 184, 0.25);
  color: #0f172a;
}

.tab-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.no-tab-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  gap: 4px;
}

.no-tab-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.no-tab-state p {
  margin: 0;
  font-size: 13px;
}

.no-tab-tip {
  font-size: 12px;
  color: #cbd5e1;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  font-size: 12px;
  color: #64748b;
  background: #fafbfc;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  flex-shrink: 0;
}

.file-meta-path {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.file-meta-lang {
  margin-left: auto;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 16px;
  overflow: auto;
  background: #f8f8f8;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.55;
  min-height: 0;
}

.code-block code {
  white-space: pre;
}

/* 小屏适配：文件树窄一些 */
@media (max-width: 1280px) {
  .tree-pane {
    width: 180px !important;
  }
}
</style>
