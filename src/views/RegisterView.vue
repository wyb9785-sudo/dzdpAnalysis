<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="register-header">
          <h2>用户注册</h2>
          <p>创建您的账户</p>
        </div>
      </template>

      <RegisterForm @register-success="handleRegisterSuccess" @switch-to-login="goToLogin" />

      <el-divider>或者</el-divider>

      <div class="login-link">
        已有账号？
        <el-button type="text" @click="goToLogin">立即登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RegisterForm from '../components/auth/RegisterForm.vue'

export default {
  name: 'RegisterView',
  components: {
    RegisterForm
  },
  setup() {
    const router = useRouter()

    const handleRegisterSuccess = () => {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }

    const goToLogin = () => {
      router.push('/login')
    }

    return {
      handleRegisterSuccess,
      goToLogin
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #5ec0ac 0%, #e18fad 100%);
}

.register-card {
  width: 500px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
}

.register-header h2 {
  margin: 0;
  color: #93d7aa;
}

.register-header p {
  margin: 5px 0 0 0;
  color: #666;
}

.login-link {
  text-align: center;
  color: #666;
}
</style>