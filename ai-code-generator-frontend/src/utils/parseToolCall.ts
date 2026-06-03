// 解析 AI 消息中的 [工具调用] 段，把代码块从对话气泡里抽离出来
// 后端 FileWriteTool / FileModifyTool 输出格式：
//   [工具调用] 写入文件 src/pages/Index.jsx
//   ```jsx
//   ...code...
//   ```
//
//   [工具调用] 修改文件 src/pages/Index.jsx
//
//   替换前：
//   ```
//   ...old...
//   ```
//
//   替换后：
//   ```
//   ...new...
//   ```

export type ToolCallAction = 'write' | 'modify' | 'delete' | 'read' | 'unknown'

export interface ToolCallBlock {
  // 稳定 ID，前端用作 key 与 Tab 切换
  id: string
  // 显示名（写入文件 / 修改文件 / 删除文件 ...）
  displayName: string
  // 操作类型
  action: ToolCallAction
  // 文件相对路径
  filePath: string
  // 文件名（路径最后一段）
  fileName: string
  // 语言后缀，未识别为空
  language: string
  // 主体代码内容（write/delete 时是新内容；modify 时是替换后内容）
  content: string
  // 修改场景下的原内容，可空
  oldContent?: string
  // 流式增量时，代码块可能尚未闭合
  streaming: boolean
}

export interface ParsedMessage {
  // 抽离工具调用后的纯文本（含分隔标记，渲染时用 ToolCallCard 占位）
  cleanedText: string
  // 解析出来的工具调用块
  toolCalls: ToolCallBlock[]
}

// 占位符：在 cleanedText 中替代被抽离的 [工具调用] 段，渲染时按 id 替换为卡片
export const TOOL_CALL_PLACEHOLDER = (id: string) => `[[TOOL_CALL:${id}]]`
const PLACEHOLDER_REGEX = /\[\[TOOL_CALL:([^\]]+)\]\]/g

export function isToolCallPlaceholder(text: string): RegExpMatchArray | null {
  return text.match(/^\[\[TOOL_CALL:([^\]]+)\]\]$/)
}

export function splitByToolCalls(cleanedText: string): Array<
  { type: 'text'; value: string } | { type: 'tool'; id: string }
> {
  const parts: Array<{ type: 'text'; value: string } | { type: 'tool'; id: string }> = []
  let lastIndex = 0
  PLACEHOLDER_REGEX.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = PLACEHOLDER_REGEX.exec(cleanedText)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', value: cleanedText.slice(lastIndex, match.index) })
    }
    parts.push({ type: 'tool', id: match[1] })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < cleanedText.length) {
    parts.push({ type: 'text', value: cleanedText.slice(lastIndex) })
  }
  return parts
}

function inferAction(displayName: string): ToolCallAction {
  if (displayName.includes('写入')) return 'write'
  if (displayName.includes('修改')) return 'modify'
  if (displayName.includes('删除')) return 'delete'
  if (displayName.includes('读取')) return 'read'
  return 'unknown'
}

function basename(filePath: string): string {
  const parts = filePath.split(/[\\/]/)
  return parts[parts.length - 1] || filePath
}

function getSuffix(filePath: string): string {
  const name = basename(filePath)
  const dot = name.lastIndexOf('.')
  if (dot <= 0 || dot === name.length - 1) return ''
  return name.slice(dot + 1).toLowerCase()
}

// 行首带可选空白的 [工具调用] 行；尽量宽松地匹配文件名（直到行尾）
const TOOL_HEADER_RE = /(^|\n)[ \t]*\[工具调用\][ \t]+(\S+)[ \t]+([^\n]+?)[ \t]*(?=\n|$)/g

// 在给定起点之后寻找下一个 ``` 代码块（可指定关键字前缀，比如 "替换前"）
function findNextCodeBlock(
  text: string,
  fromIndex: number,
): { langStart: number; bodyStart: number; bodyEnd: number; afterEnd: number; lang: string; closed: boolean } | null {
  const openIdx = text.indexOf('```', fromIndex)
  if (openIdx === -1) return null
  // 读取语言到换行
  const newlineAfterLang = text.indexOf('\n', openIdx + 3)
  if (newlineAfterLang === -1) {
    // 整段未闭合，无内容
    return {
      langStart: openIdx + 3,
      bodyStart: text.length,
      bodyEnd: text.length,
      afterEnd: text.length,
      lang: text.slice(openIdx + 3).trim(),
      closed: false,
    }
  }
  const lang = text.slice(openIdx + 3, newlineAfterLang).trim()
  const bodyStart = newlineAfterLang + 1
  const closeIdx = text.indexOf('\n```', bodyStart - 1)
  if (closeIdx === -1) {
    return {
      langStart: openIdx + 3,
      bodyStart,
      bodyEnd: text.length,
      afterEnd: text.length,
      lang,
      closed: false,
    }
  }
  const bodyEnd = closeIdx
  const afterEnd = closeIdx + 4 // 跨过 "\n```"
  return { langStart: openIdx + 3, bodyStart, bodyEnd, afterEnd, lang, closed: true }
}

// 在两个索引之间是否存在"替换前/替换后"标记，用于识别 modify 段
function findKeywordBetween(text: string, from: number, to: number, keyword: string): number {
  const idx = text.indexOf(keyword, from)
  if (idx === -1 || idx >= to) return -1
  return idx
}

let idCounter = 0
function nextId(filePath: string, displayName: string): string {
  idCounter += 1
  return `tc-${displayName}-${filePath}-${idCounter}`
}

/**
 * 解析一段（可能是流式不完整的）AI 消息文本：
 * - 把所有 [工具调用] 段连同其后跟随的 ``` 代码块抽出来
 * - 抽离位置插入占位符 [[TOOL_CALL:id]]
 */
export function parseToolCalls(raw: string): ParsedMessage {
  if (!raw) return { cleanedText: '', toolCalls: [] }

  const toolCalls: ToolCallBlock[] = []
  const matches: Array<{
    headerStart: number
    blockEnd: number
    block: ToolCallBlock
  }> = []

  TOOL_HEADER_RE.lastIndex = 0
  let header: RegExpExecArray | null
  while ((header = TOOL_HEADER_RE.exec(raw)) !== null) {
    // header[1] 是开头的 \n 或 ''；真正头部从 header.index + header[1].length 开始
    const headerStart = header.index + header[1].length
    const displayName = header[2]
    const filePath = header[3].trim()
    const headerEnd = TOOL_HEADER_RE.lastIndex // 指向行尾换行符之前

    const action = inferAction(displayName)

    // modify：寻找"替换前"和"替换后"两个代码块
    // write / 其它：寻找紧跟的一个代码块
    let blockEnd = headerEnd
    let content = ''
    let oldContent: string | undefined
    let language = getSuffix(filePath)
    let streaming = false

    if (action === 'modify') {
      // 下一个 [工具调用] 头作为本段上界
      TOOL_HEADER_RE.lastIndex = headerEnd
      const peek = TOOL_HEADER_RE.exec(raw)
      const upperBound = peek ? peek.index + peek[1].length : raw.length
      // 回退指针
      TOOL_HEADER_RE.lastIndex = headerEnd

      // 在 [headerEnd, upperBound) 范围内找两个代码块
      const oldKeyIdx = findKeywordBetween(raw, headerEnd, upperBound, '替换前')
      const newKeyIdx = findKeywordBetween(raw, headerEnd, upperBound, '替换后')

      const oldBlock = oldKeyIdx >= 0 ? findNextCodeBlock(raw, oldKeyIdx) : findNextCodeBlock(raw, headerEnd)
      let newBlock = null as ReturnType<typeof findNextCodeBlock> | null
      if (oldBlock) {
        const searchFrom = newKeyIdx >= 0 && newKeyIdx > oldBlock.afterEnd ? newKeyIdx : oldBlock.afterEnd
        newBlock = findNextCodeBlock(raw, searchFrom)
      }

      if (oldBlock) {
        oldContent = raw.slice(oldBlock.bodyStart, oldBlock.bodyEnd)
        if (!oldBlock.closed) streaming = true
      }
      if (newBlock) {
        content = raw.slice(newBlock.bodyStart, newBlock.bodyEnd)
        if (!newBlock.closed) streaming = true
        if (newBlock.lang) language = newBlock.lang
        blockEnd = newBlock.afterEnd
      } else if (oldBlock) {
        // 没有"替换后"代码块——可能仍在流式输出
        blockEnd = oldBlock.afterEnd
        streaming = true
      } else {
        // 没有任何代码块（极端情况），把段落延展到下一个工具头/末尾
        blockEnd = upperBound
        streaming = true
      }
    } else if (action === 'delete') {
      // 删除工具通常没有代码块
      blockEnd = headerEnd
    } else {
      // write / read / unknown：跟随一个代码块
      const codeBlock = findNextCodeBlock(raw, headerEnd)
      // 但要确认该代码块出现在下一个 [工具调用] 之前
      TOOL_HEADER_RE.lastIndex = headerEnd
      const peek = TOOL_HEADER_RE.exec(raw)
      const upperBound = peek ? peek.index + peek[1].length : raw.length
      TOOL_HEADER_RE.lastIndex = headerEnd

      if (codeBlock && codeBlock.langStart - 3 < upperBound) {
        content = raw.slice(codeBlock.bodyStart, codeBlock.bodyEnd)
        if (!codeBlock.closed) streaming = true
        if (codeBlock.lang) language = codeBlock.lang
        blockEnd = codeBlock.afterEnd
      } else {
        // 没有跟随代码块（可能正在流式中尚未到代码块）
        blockEnd = headerEnd
        streaming = true
      }
    }

    const id = nextId(filePath, displayName)
    const block: ToolCallBlock = {
      id,
      displayName,
      action,
      filePath,
      fileName: basename(filePath),
      language,
      content,
      oldContent,
      streaming,
    }
    toolCalls.push(block)
    matches.push({ headerStart, blockEnd, block })

    // 重新对齐 regex 指针，避免在已消费区间再次匹配
    TOOL_HEADER_RE.lastIndex = blockEnd
  }

  // 用占位符替换原文中的工具调用段（从后往前替换，避免索引偏移）
  let cleanedText = raw
  for (let i = matches.length - 1; i >= 0; i--) {
    const m = matches[i]
    const placeholder = TOOL_CALL_PLACEHOLDER(m.block.id)
    cleanedText = cleanedText.slice(0, m.headerStart) + placeholder + cleanedText.slice(m.blockEnd)
  }

  return { cleanedText, toolCalls }
}

// 同一文件可能被多次写入/修改，UI 上一般展示"最新版本"；这里给一个去重 helper
export function dedupeByFile(blocks: ToolCallBlock[]): ToolCallBlock[] {
  const map = new Map<string, ToolCallBlock>()
  for (const b of blocks) {
    if (b.action === 'read') continue // 读文件不进 Tab
    map.set(b.filePath, b)
  }
  return Array.from(map.values())
}

// 原生 HTML / 多文件模式：AI 直接输出裸代码块，没有 [工具调用] 头
// 按语言把裸代码块映射到虚拟文件，使其同样能进入代码 Tab 与文件树
export type CodeGenMode = 'html' | 'multi_file' | 'vue_project' | string | undefined

interface VirtualFileMapping {
  filePath: string
  language: string
  displayName: string
}

// 语言（小写）→ 虚拟文件信息
function getVirtualFileMapping(mode: CodeGenMode, lang: string): VirtualFileMapping | null {
  const normalized = lang.toLowerCase()
  if (mode === 'html') {
    if (normalized === 'html') {
      return { filePath: 'index.html', language: 'html', displayName: '生成代码' }
    }
    return null
  }
  if (mode === 'multi_file') {
    if (normalized === 'html') {
      return { filePath: 'index.html', language: 'html', displayName: '生成代码' }
    }
    if (normalized === 'css') {
      return { filePath: 'style.css', language: 'css', displayName: '生成代码' }
    }
    if (normalized === 'javascript' || normalized === 'js') {
      return { filePath: 'script.js', language: 'javascript', displayName: '生成代码' }
    }
    return null
  }
  return null
}

/**
 * 在 cleanedText（已剔除 [工具调用] 段）中扫描剩余的裸代码块，
 * 按 mode 映射成虚拟文件块，并在原位置替换为占位符
 */
function extractBareCodeBlocks(
  cleanedText: string,
  mode: CodeGenMode,
): { cleanedText: string; toolCalls: ToolCallBlock[] } {
  if (mode !== 'html' && mode !== 'multi_file') {
    return { cleanedText, toolCalls: [] }
  }

  const blocks: ToolCallBlock[] = []
  const matches: Array<{ start: number; end: number; placeholderId: string }> = []

  let cursor = 0
  while (cursor < cleanedText.length) {
    const cb = findNextCodeBlock(cleanedText, cursor)
    if (!cb) break

    const openIdx = cleanedText.indexOf('```', cursor)
    const mapping = getVirtualFileMapping(mode, cb.lang)

    if (mapping) {
      const content = cleanedText.slice(cb.bodyStart, cb.bodyEnd)
      const id = nextId(mapping.filePath, mapping.displayName)
      blocks.push({
        id,
        displayName: mapping.displayName,
        action: 'write',
        filePath: mapping.filePath,
        fileName: basename(mapping.filePath),
        language: mapping.language,
        content,
        streaming: !cb.closed,
      })
      matches.push({ start: openIdx, end: cb.afterEnd, placeholderId: id })
    }

    cursor = cb.afterEnd
    // 若代码块未闭合，再继续也找不到下一个，退出
    if (!cb.closed) break
  }

  // 从后往前替换为占位符
  let result = cleanedText
  for (let i = matches.length - 1; i >= 0; i--) {
    const m = matches[i]
    result = result.slice(0, m.start) + TOOL_CALL_PLACEHOLDER(m.placeholderId) + result.slice(m.end)
  }

  return { cleanedText: result, toolCalls: blocks }
}

/**
 * 顶层入口：parseToolCalls 的包装，额外处理裸代码块
 */
export function parseAiMessage(raw: string, mode: CodeGenMode): ParsedMessage {
  const first = parseToolCalls(raw)
  if (mode !== 'html' && mode !== 'multi_file') {
    return first
  }
  const second = extractBareCodeBlocks(first.cleanedText, mode)
  return {
    cleanedText: second.cleanedText,
    toolCalls: [...first.toolCalls, ...second.toolCalls],
  }
}
