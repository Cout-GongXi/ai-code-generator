<template>
  <div class="user-register-page">
    <a-card class="user-register-card" :title="`${SITE_TITLE} - 注册`">
      <p class="user-register-slogan">不写一行代码，生成完整应用</p>
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

        <a-form-item name="checkPassword">
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" block :loading="loading">
            注册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="tips">
        已有账号？
        <a @click="goLogin">去登录</a>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { register } from '@/api/userController.ts'
import { SITE_TITLE } from '@/config/menu'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = async (_rule: Rule, value: string) => {
  if (!value) {
    return Promise.reject('请再次输入密码')
  }
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const rules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少 4 位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, validator: validateCheckPassword, trigger: 'blur' },
  ],
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  loading.value = true
  try {
    const res = await register(values)
    if (res.data.code === 0) {
      message.success('注册成功')
      await router.replace('/user/login')
    } else {
      message.error(res.data.message ?? '注册失败')
    }
  } catch {
    message.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/user/login')
}
</script>

<style scoped>
.user-register-page {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.user-register-card {
  width: 100%;
  max-width: 420px;
}

.user-register-slogan {
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
