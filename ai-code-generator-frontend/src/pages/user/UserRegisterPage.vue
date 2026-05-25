<template>
  <div id="userRegisterPage">
    <img class="auth-background" :src="wallpaperUrl" alt="" />
    <main class="auth-shell">
      <section class="form-card">
        <RouterLink class="brand-link" to="/">
          <img class="brand-logo" src="@/assets/logo.png" alt="坤代码" />
          <span>坤代码</span>
        </RouterLink>
        <p class="eyebrow">Create account</p>
        <h1 class="title">注册</h1>
        <p class="subtitle">创建账号，开始用 AI 快速生成完整应用。</p>
        <a-form
          :model="formState"
          name="register"
          autocomplete="off"
          @finish="handleSubmit"
        >
          <a-form-item
            name="userAccount"
            :rules="[{ required: true, message: '请输入账号' }]"
          >
            <a-input
              v-model:value="formState.userAccount"
              class="auth-input"
              placeholder="请输入账号"
            />
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              class="auth-input"
              placeholder="请输入密码"
            />
          </a-form-item>
          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请确认密码' },
              { min: 8, message: '密码不能小于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              class="auth-input"
              placeholder="请再次输入密码"
            />
          </a-form-item>
          <a-form-item class="submit-item">
            <a-button class="submit-button" type="primary" html-type="submit">
              注册
            </a-button>
          </a-form-item>
        </a-form>
        <p class="switch-text">
          已经有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </p>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive, ref } from 'vue'

const router = useRouter()

const wallpaperUrl = ref('https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1920&q=80')

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (
  rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.12), transparent 34%),
    linear-gradient(180deg, #f8fbff 0%, #f4f7fb 42%, #eef3f8 100%);
}

#userRegisterPage,
#userRegisterPage * {
  box-sizing: border-box;
}

.auth-background {
  display: none;
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.form-card {
  width: 100%;
  max-width: 460px;
  padding: 38px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
  text-decoration: none;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  margin: 0;
  color: #0f172a;
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 0;
}

.subtitle {
  margin: 12px 0 30px;
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
}

:deep(.ant-form-item) {
  margin-bottom: 18px;
}

:deep(.auth-input),
:deep(.auth-input .ant-input) {
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

:deep(.auth-input) {
  width: 100%;
  height: 50px;
  padding: 0 18px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #f8fbff;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.04);
}

:deep(.auth-input.ant-input-password) {
  padding: 0 18px;
}

:deep(.auth-input .ant-input) {
  height: 48px;
  background: transparent;
}

:deep(.auth-input::placeholder),
:deep(.auth-input .ant-input::placeholder) {
  color: #94a3b8;
}

:deep(.auth-input:hover),
:deep(.auth-input:focus),
:deep(.auth-input.ant-input-affix-wrapper-focused) {
  border-color: rgba(37, 99, 235, 0.62);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.submit-item {
  margin-top: 8px;
  margin-bottom: 0;
}

.submit-button {
  width: 100%;
  height: 50px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  font-size: 16px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.24);
}

.submit-button:hover,
.submit-button:focus {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
}

.switch-text {
  margin: 24px 0 0;
  color: #475569;
  font-size: 14px;
  text-align: center;
}

.switch-text a {
  color: #2563eb;
  font-weight: 800;
  text-decoration: none;
}

@media (max-width: 900px) {
  .auth-shell {
    padding: 24px 16px;
  }

  .form-card {
    padding: 28px 22px;
  }

  .brand-link {
    margin-bottom: 22px;
  }

  .title {
    font-size: 32px;
  }

  .subtitle {
    margin-bottom: 24px;
  }
}
</style>
