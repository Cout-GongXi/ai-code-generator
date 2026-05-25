/**
 * 环境变量配置
 */

const DEFAULT_DEPLOY_DOMAIN =
  typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8010`
    : 'http://localhost:8010'

// 应用部署域名
export const DEPLOY_DOMAIN =
  (import.meta.env.VITE_DEPLOY_DOMAIN || DEFAULT_DEPLOY_DOMAIN).replace(
    /\/+$/,
    '',
  )

// API 基础地址
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8001/api'

// 静态资源地址
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

// 获取部署应用的完整URL
export const getDeployUrl = (deployKey: string) => {
  const normalizedDeployKey = deployKey.replace(/^\/+|\/+$/g, '')
  return `${DEPLOY_DOMAIN}/${normalizedDeployKey}/`
}

// 获取静态资源预览URL
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  return `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
}
