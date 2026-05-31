<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <RouterLink to="/" class="back-home" title="返回主页">
          <img class="logo" src="@/assets/logo.png" alt="Logo" />
        </RouterLink>
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button type="default" @click="downloadCode" :loading="downloading">
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
        <!-- 用户头像与菜单 -->
        <a-dropdown
          v-if="loginUserStore.loginUser.id"
          placement="bottomRight"
        >
          <div class="avatar-circle">
            <img
              v-if="loginUserStore.loginUser.userAvatar"
              :src="loginUserStore.loginUser.userAvatar"
              class="avatar-img"
            />
            <span v-else class="avatar-letter">
              {{ (loginUserStore.loginUser.userName ?? '?')[0] }}
            </span>
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item disabled class="menu-username">
                {{ loginUserStore.loginUser.userName ?? '用户' }}
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item @click="goHome">
                <HomeOutlined />
                返回主页
              </a-menu-item>
              <a-menu-item @click="goMyApps">
                <AppstoreOutlined />
                我的应用
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item @click="doLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <RouterLink v-else to="/user/login" class="login-btn">登录</RouterLink>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 首次历史加载中：占位骨架，避免空白 -->
          <div
            v-if="initialHistoryLoading && messages.length === 0"
            class="chat-skeleton"
          >
            <a-skeleton active avatar :paragraph="{ rows: 2 }" />
            <a-skeleton active avatar :paragraph="{ rows: 3 }" />
            <a-skeleton active avatar :paragraph="{ rows: 2 }" />
          </div>
          <!-- 加载更多历史消息 -->
          <div v-if="messages.length > 0" class="load-more-wrapper">
            <a-button
              v-if="hasMoreHistory"
              type="link"
              size="small"
              :loading="historyLoading"
              @click="loadMoreHistory"
            >
              加载更多历史消息
            </a-button>
            <span v-else class="no-more-tip">没有更多历史消息了</span>
          </div>
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-item"
          >
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer
                  v-if="message.content"
                  :content="message.content"
                />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip
              v-if="!isOwner"
              title="无法在别人的作品下对话哦~"
              placement="top"
            >
              <a-textarea
                v-model:value="userInput"
                placeholder="请描述你想生成的网站，越详细效果越好哦"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              placeholder="请描述你想生成的网站，越详细效果越好哦"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistoryByPage } from '@/api/chatHistoryController'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes.ts'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  LogoutOutlined,
  HomeOutlined,
  AppstoreOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string // 数据库加载的历史消息会带有创建时间，用于游标分页
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 历史消息分页相关
const HISTORY_PAGE_SIZE = 10
const historyLoading = ref(false)
const hasMoreHistory = ref(false)
// 首次历史加载状态：仅用于初次进入时展示骨架占位
const initialHistoryLoading = ref(true)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 头像菜单：返回主页
const goHome = () => {
  router.push('/')
}

// 头像菜单：我的应用
const goMyApps = () => {
  router.push('/my/apps')
}

// 头像菜单：退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 加载对话历史（首次加载 / 加载更多）
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || historyLoading.value) return

  historyLoading.value = true
  try {
    const params: API.listAppChatHistoryByPageParams = {
      appId: appId.value as unknown as number,
      pageSize: HISTORY_PAGE_SIZE,
    }
    // 加载更多时使用最早一条历史消息的创建时间作为游标
    if (isLoadMore) {
      const earliestHistory = messages.value.find((m) => m.createTime)
      if (earliestHistory?.createTime) {
        params.lastCreateTime = earliestHistory.createTime
      }
    }

    const res = await listAppChatHistoryByPage(params)
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records ?? []
      // 后端返回按创建时间倒序，反转后按时间升序展示
      const newMessages: Message[] = records
        .slice()
        .reverse()
        .map((record) => ({
          type: record.messageType === 'user' ? 'user' : 'ai',
          content: record.message || '',
          createTime: record.createTime,
        }))

      // 是否仍有更多（返回数量等于页大小则可能还有）
      hasMoreHistory.value = records.length >= HISTORY_PAGE_SIZE

      if (isLoadMore) {
        // 保留当前滚动位置
        const container = messagesContainer.value
        const previousHeight = container?.scrollHeight ?? 0
        messages.value = [...newMessages, ...messages.value]
        await nextTick()
        if (container) {
          container.scrollTop = container.scrollHeight - previousHeight
        }
      } else {
        messages.value = newMessages
        await nextTick()
        scrollToBottom()
      }
    } else {
      message.error('加载对话历史失败：' + res.data.message)
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    historyLoading.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = () => loadChatHistory(true)

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  const message = userInput.value.trim()
  userInput.value = ''

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content =
    '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const url = `${baseURL}/app/download/${appId.value}`

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }

    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`

    // 下载文件
    const blob = await response.blob()

    // 检查 blob 类型和大小
    console.log('下载的文件信息:', {
      size: blob.size,
      type: blob.type,
      fileName: fileName
    })

    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 页面加载初始化：并行加载，立即渲染骨架，不阻塞首屏
onMounted(() => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }
  // 同步先设置 appId，让两个请求都能并行发起
  appId.value = id

  // 并行加载应用信息与历史对话；任一完成后立即更新对应区域
  const appInfoPromise = fetchAppInfo()
  const chatHistoryPromise = loadChatHistory(false).finally(() => {
    initialHistoryLoading.value = false
  })

  // 历史对话一旦加载完成，若已有内容就先把右侧预览顶上去
  chatHistoryPromise.then(() => {
    if (messages.value.length >= 2 && appInfo.value?.codeGenType) {
      updatePreview()
    }
  })

  // 应用信息回来后，再尝试一次预览（覆盖 codeGenType 之前未就绪的情况）
  appInfoPromise.then(() => {
    if (appInfo.value && messages.value.length >= 2) {
      updatePreview()
    }
  })

  // 两者都就绪后决定是否自动发送初始提示词
  Promise.all([appInfoPromise, chatHistoryPromise]).then(async () => {
    if (!appInfo.value) return
    if (
      !route.query.view &&
      isOwner.value &&
      messages.value.length === 0 &&
      appInfo.value.initPrompt
    ) {
      await sendInitialMessage(appInfo.value.initPrompt)
    }
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.1), transparent 30%),
    linear-gradient(180deg, #f8fbff 0%, #f4f7fb 45%, #eef3f8 100%);
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 8px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-home {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  border-radius: 50%;
  transition: opacity 0.2s, transform 0.2s;
}

.back-home:hover {
  opacity: 0.85;
  transform: scale(1.06);
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.app-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 用户头像 */
.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s, transform 0.2s;
  flex-shrink: 0;
}

.avatar-circle:hover {
  opacity: 0.85;
  transform: scale(1.06);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-letter {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1;
}

.login-btn {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  text-decoration: none;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.6);
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.login-btn:hover {
  color: #1a1a1a;
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(148, 163, 184, 0.55);
}

:global(.menu-username) {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a !important;
  cursor: default !important;
  padding: 8px 16px !important;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 0;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.load-more-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0 16px;
}

.chat-skeleton {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 12px 4px 4px;
}

.no-more-tip {
  font-size: 12px;
  color: #94a3b8;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
}

.ai-message .message-content {
  background: #f8fbff;
  color: #1e293b;
  border: 1px solid rgba(148, 163, 184, 0.16);
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
}

/* 输入区域 */
.input-container {
  padding: 16px 20px 20px;
  background: rgba(255, 255, 255, 0.7);
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 251, 255, 0.92);
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

:deep(.ant-btn) {
  border-radius: 12px;
}

:deep(.ant-btn-primary) {
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.16);
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-input-textarea textarea) {
  border-radius: 14px;
  border-color: rgba(148, 163, 184, 0.24);
  background: #f8fbff;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 14px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
