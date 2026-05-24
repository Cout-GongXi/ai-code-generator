<template>
  <div class="user-login-page">
    <a-card class="user-login-card" :title="`${SITE_TITLE} - 登录`">
      <p class="user-login-slogan">不写一行代码，生成完整应用</p>
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
        autocomplete="off"
        @finish="handleSubmit"
      >
        <a-form-item name="userAccount">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号"
            allow-clear
          />
        </a-form-item>

        <a-form-item name="userPassword">
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" block :loading="loading">
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="tips">
        还没有账号？
        <a @click="goRegister">立即注册</a>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { useLoginUserStore } from '@/stores/loginUser'
import { SITE_TITLE } from '@/config/menu'
import { userLogin } from '@/api/userController.ts'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const rules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少 4 位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

function getRedirectPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return '/'
}

const handleSubmit = async (values: any) => {
  loading.value = true
  try {
    const res = await userLogin   (values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      loginUserStore.setLoginUser(res.data.data)
      message.success('登录成功')
      await router.replace(getRedirectPath())
    } else {
      message.error(res.data.message ?? '登录失败')
    }
  } catch {
    message.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/user/register')
}
</script>

<style scoped>
.user-login-page {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.user-login-card {
  width: 100%;
  max-width: 420px;
}

.user-login-slogan {
  margin: -8px 0 16px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.tips {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
}

.tips a {
  color: #1677ff;
  cursor: pointer;
}
</style>
