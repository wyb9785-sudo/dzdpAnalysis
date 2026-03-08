<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>大数据处理一体化平台</h2>
          <p>欢迎登录</p>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
            <el-button type="text" @click="showForgotPassword">忘记密码？</el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              size="large"
              @click="handleLogin"
              :loading="loading"
              style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>

        <el-divider>或者</el-divider>

        <el-form-item>
          <div class="register-link">
            还没有账号？
            <el-button type="text" @click="goToRegister">立即注册</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 注册对话框 -->
    <el-dialog
        v-model="registerDialogVisible"
        title="用户注册"
        width="500px"
        :before-close="handleCloseRegister"
    >
      <RegisterForm @register-success="handleRegisterSuccess" />
    </el-dialog>

    <!-- 忘记密码对话框 -->
    <el-dialog
        v-model="forgotPasswordDialogVisible"
        title="找回密码"
        width="400px"
    >
      <el-form :model="forgotForm" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入注册邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleForgotPassword">发送重置邮件</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/index.js'
import RegisterForm from '../components/auth/RegisterForm.vue'

export default {
  name: 'LoginView',
  components: {
    RegisterForm
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref()
    const loading = ref(false)
    const rememberPassword = ref(false)
    const registerDialogVisible = ref(false)
    const forgotPasswordDialogVisible = ref(false)

    const loginForm = reactive({
      username: '',
      password: ''
    })

    const forgotForm = reactive({
      email: ''
    })

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    // 加载记住的密码
    const loadRememberedPassword = () => {
      const remembered = localStorage.getItem('rememberedLogin')
      if (remembered) {
        const { username, password, remember } = JSON.parse(remembered)
        loginForm.username = username
        loginForm.password = password
        rememberPassword.value = remember
      }
    }

    // 保存记住的密码
    const saveRememberedPassword = () => {
      if (rememberPassword.value) {
        localStorage.setItem('rememberedLogin', JSON.stringify({
          username: loginForm.username,
          password: loginForm.password,
          remember: true
        }))
      } else {
        localStorage.removeItem('rememberedLogin')
      }
    }

    // LoginView.vue 中的 handleLogin 方法修改
    // LoginView.vue - 修改登录成功处理
    const handleLogin = async () => {
      try {
        await loginFormRef.value.validate()
        loading.value = true

        console.log('开始登录...')

        const response = await authApi.login(loginForm)

        console.log('登录响应:', response)

        // 处理响应数据
        const resultData = response.data || response

        if (resultData.token && resultData.user) {
          // 保存到localStorage
          localStorage.setItem('token', resultData.token)
          localStorage.setItem('user', JSON.stringify(resultData.user))

          console.log('登录成功，用户角色:', resultData.user.role)

          ElMessage.success('登录成功')

          // 直接使用window.location确保跳转
          if (resultData.user.role === 'MERCHANT') {
            window.location.href = '/merchant/analysis'
          }else if (resultData.user.role === 'CUSTOMER') {
            window.location.href = '/decision-assistant'
          }
          else {
            window.location.href = '/dashboard'
          }
        } else {
          console.error('响应缺少必要字段')
          ElMessage.error(resultData.message || '登录失败')
        }
      } catch (error) {
        console.error('登录错误:', error)
        if (error.message) {
          ElMessage.error(error.message)
        } else if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message)
        } else {
          ElMessage.error('登录失败')
        }
      } finally {
        loading.value = false
      }
    }

    const goToRegister = () => {
      registerDialogVisible.value = true
    }

    const handleRegisterSuccess = () => {
      registerDialogVisible.value = false
      ElMessage.success('注册成功，请登录')
    }

    const handleCloseRegister = (done) => {
      done()
    }

    const showForgotPassword = () => {
      forgotPasswordDialogVisible.value = true
    }

    const handleForgotPassword = () => {
      ElMessage.info('密码重置功能开发中')
      forgotPasswordDialogVisible.value = false
    }

    onMounted(() => {
      loadRememberedPassword()
    })

    return {
      loginForm,
      forgotForm,
      rules,
      loginFormRef,
      loading,
      rememberPassword,
      registerDialogVisible,
      forgotPasswordDialogVisible,
      handleLogin,
      goToRegister,
      handleRegisterSuccess,
      handleCloseRegister,
      showForgotPassword,
      handleForgotPassword
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #5ec0ac 0%, #e18fad 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
}

.login-header h2 {
  margin: 0;
  color: #93d7aa;
}

.login-header p {
  margin: 5px 0 0 0;
  color: #666;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.register-link {
  text-align: center;
  color: #666;
}
</style>