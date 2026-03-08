<template>
  <div class="register-form">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="用户名" prop="username">
        <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            @blur="checkUsername"
        />
        <div v-if="usernameExists" class="error-tip">用户名已存在</div>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
        />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            @blur="checkEmail"
        />
        <div v-if="emailExists" class="error-tip">邮箱已存在</div>
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
        />
      </el-form-item>

      <el-form-item label="用户角色" prop="role">
        <el-radio-group v-model="form.role">
          <el-radio label="MERCHANT">商家</el-radio>
          <el-radio label="CUSTOMER">顾客</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
          label="商户名称"
          prop="merchantName"
          v-if="form.role === 'MERCHANT'"
      >
        <el-input
            v-model="form.merchantName"
            placeholder="请输入商户名称"
        />
      </el-form-item>

      <el-form-item>
        <el-button
            type="primary"
            @click="handleRegister"
            :loading="loading"
            style="width: 100%"
        >
          注册
        </el-button>
      </el-form-item>

      <el-form-item>
        <div class="login-link">
          已有账号？
          <el-button type="text" @click="$emit('switch-to-login')">立即登录</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../../api/index.js'

export default {
  name: 'RegisterForm',
  emits: ['register-success', 'switch-to-login'],
  setup(props, { emit }) {
    const formRef = ref()
    const loading = ref(false)
    const usernameExists = ref(false)
    const emailExists = ref(false)

    const form = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: '',
      role: 'CUSTOMER',
      merchantName: ''
    })

    const validateUsername = async (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入用户名'))
      }
      if (value.length < 3) {
        return callback(new Error('用户名长度不能少于3个字符'))
      }
      if (usernameExists.value) {
        return callback(new Error('用户名已存在'))
      }
      callback()
    }

    const validatePassword = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入密码'))
      }
      if (value.length < 6) {
        return callback(new Error('密码长度不能少于6个字符'))
      }
      callback()
    }

    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请确认密码'))
      }
      if (value !== form.password) {
        return callback(new Error('两次输入密码不一致'))
      }
      callback()
    }

    const validateEmail = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入邮箱'))
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(value)) {
        return callback(new Error('请输入有效的邮箱地址'))
      }
      if (emailExists.value) {
        return callback(new Error('邮箱已存在'))
      }
      callback()
    }

    const rules = {
      username: [{ validator: validateUsername, trigger: 'blur' }],
      password: [{ validator: validatePassword, trigger: 'blur' }],
      confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
      email: [{ validator: validateEmail, trigger: 'blur' }],
      merchantName: [
        {
          required: true,
          message: '请输入商户名称',
          trigger: 'blur',
          validator: (rule, value, callback) => {
            if (form.role === 'MERCHANT' && !value) {
              callback(new Error('商户名称不能为空'))
            } else {
              callback()
            }
          }
        }
      ]
    }

    const checkUsername = async () => {
      if (!form.username) return
      try {
        const result = await authApi.checkUsername(form.username)
        usernameExists.value = result.exists
      } catch (error) {
        console.error('检查用户名失败')
      }
    }

    const checkEmail = async () => {
      if (!form.email) return
      try {
        const result = await authApi.checkEmail(form.email)
        emailExists.value = result.exists
      } catch (error) {
        console.error('检查邮箱失败')
      }
    }

    const handleRegister = async () => {
      if (!formRef.value) return

      try {
        await formRef.value.validate()
        loading.value = true

        const registerData = { ...form }
        // 清理确认密码字段
        delete registerData.confirmPassword

        const result = await authApi.register(registerData)

        if (result.success) {
          ElMessage.success('注册成功')
          emit('register-success')
          resetForm()
        } else {
          ElMessage.error(result.message)
        }
      } catch (error) {
        if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message)
        } else if (error.name !== 'Error') {
          ElMessage.error('注册失败，请检查表单')
        }
      } finally {
        loading.value = false
      }
    }

    const resetForm = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      usernameExists.value = false
      emailExists.value = false
    }

    // 监听角色变化，重置商户名称
    watch(() => form.role, (newRole) => {
      if (newRole !== 'MERCHANT') {
        form.merchantName = ''
      }
    })

    return {
      form,
      rules,
      formRef,
      loading,
      usernameExists,
      emailExists,
      checkUsername,
      checkEmail,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-form {
  padding: 10px;
}

.error-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.login-link {
  text-align: center;
  color: #666;
  width: 100%;
}
</style>