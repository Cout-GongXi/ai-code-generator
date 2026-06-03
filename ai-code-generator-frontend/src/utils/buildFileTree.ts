import type { ToolCallBlock } from './parseToolCall'

export interface FileTreeNode {
  // 节点名（最后一段）
  name: string
  // 完整路径（文件夹也是完整路径，便于折叠状态用路径作为 key）
  path: string
  // 是否文件夹
  isDir: boolean
  // 子节点（文件夹才有）
  children?: FileTreeNode[]
  // 关联的工具调用块（文件才有，便于 UI 展示操作类型 / streaming）
  block?: ToolCallBlock
}

function normalizePath(filePath: string): string[] {
  return filePath
    .replace(/^[./]+/, '') // 去掉开头的 ./ 或 /
    .split(/[\\/]/)
    .filter(Boolean)
}

/**
 * 把扁平的 ToolCallBlock 列表组织成树
 * 文件夹按字典序、文件夹优先于文件
 */
export function buildFileTree(files: ToolCallBlock[]): FileTreeNode[] {
  const root: FileTreeNode = {
    name: '',
    path: '',
    isDir: true,
    children: [],
  }

  for (const file of files) {
    const parts = normalizePath(file.filePath)
    if (parts.length === 0) continue

    let cursor = root
    for (let i = 0; i < parts.length; i++) {
      const segment = parts[i]
      const isLast = i === parts.length - 1
      const segPath = parts.slice(0, i + 1).join('/')

      cursor.children = cursor.children || []
      let next = cursor.children.find((c) => c.name === segment && c.isDir === !isLast)

      if (!next) {
        next = {
          name: segment,
          path: segPath,
          isDir: !isLast,
          children: isLast ? undefined : [],
          block: isLast ? file : undefined,
        }
        cursor.children.push(next)
      } else if (isLast) {
        // 同一文件多次写入：保留最新 block
        next.block = file
      }
      cursor = next
    }
  }

  sortTree(root)
  return root.children || []
}

function sortTree(node: FileTreeNode) {
  if (!node.children) return
  node.children.sort((a, b) => {
    if (a.isDir !== b.isDir) return a.isDir ? -1 : 1
    return a.name.localeCompare(b.name)
  })
  node.children.forEach(sortTree)
}

/**
 * 收集树中所有文件夹路径，用于"默认全部展开"
 */
export function collectDirPaths(nodes: FileTreeNode[]): string[] {
  const paths: string[] = []
  const walk = (n: FileTreeNode) => {
    if (n.isDir) {
      paths.push(n.path)
      n.children?.forEach(walk)
    }
  }
  nodes.forEach(walk)
  return paths
}
