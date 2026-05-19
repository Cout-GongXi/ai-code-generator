export interface AppMenuItem {
  key: string
  label: string
  path: string
}

export const SITE_TITLE = 'AI 代码生成器'

export const appMenuItems: AppMenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'about', label: '关于', path: '/about' },
]
