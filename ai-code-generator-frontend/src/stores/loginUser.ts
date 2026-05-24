import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController.ts'

/**
 * 用户登录状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  // 默认值
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })
  // 获取用户信息
  async function fetchLoginUser() {
      try {
        const res = await getLoginUser()
        if (res.data.code === 0 && res.data.data) {

          loginUser.value = res.data.data
        } else {
          // 未登录，清空用户信息
          loginUser.value = {}
        }
      } catch (error) {
        console.error('获取用户信息失败', error)
        loginUser.value = {}
      }
  }
  // 更新登录用户信息
  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }
  return { loginUser, fetchLoginUser, setLoginUser }
})
