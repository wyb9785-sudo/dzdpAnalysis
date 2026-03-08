<template>
  <div class="merchant-layout">
    <!-- 商户专属顶部栏 -->
    <div class="merchant-header">
      <div class="header-left">
        <h1 class="logo">大数据处理一体化平台-商户口碑分析中心</h1>
      </div>
      <div class="header-center">
        <span class="merchant-name">{{ merchantName }}</span>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="32" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
            <i class="el-icon-caret-bottom"></i>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="dashboard">返回首页</el-dropdown-item>
              <el-dropdown-item command="refresh">刷新数据</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="merchant-content">
      <router-view />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'MerchantLayout',
  setup() {
    const router = useRouter()
    const merchantName = ref('')
    const userName = ref('')
    const userAvatar = ref('')

    // 从localStorage获取用户信息
    const loadUserInfo = () => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      userName.value = user.username || '商户用户'
      merchantName.value = user.merchantName || user.username || '我的商户'
      userAvatar.value = user.avatar || ''
    }

    const handleCommand = (command) => {
      switch (command) {
        case 'dashboard':
          router.push('/merchant-analysis')
          break
        case 'refresh':
          // 触发刷新事件，由子组件监听
          window.dispatchEvent(new CustomEvent('merchant-refresh'))
          ElMessage.success('刷新数据请求已发送')
          break
        case 'logout':
          handleLogout()
          break
      }
    }

    const handleLogout = () => {
      ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        ElMessage.success('退出成功')
        router.push('/login')
      }).catch(() => {})
    }

    onMounted(() => {
      loadUserInfo()
    })

    return {
      merchantName,
      userName,
      userAvatar,
      handleCommand
    }
  }
}
</script>

<style scoped>
.merchant-layout {
  height: 100vh;
  background: linear-gradient(135deg, #92d5a9 0%, #e18fad 100%);
  display: flex;
  flex-direction: column;
}

.merchant-header {
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-left .logo {
  margin: 0;
  color: #92d5a9;
  font-size: 20px;
  font-weight: bold;
}

.header-center .merchant-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  background: linear-gradient(45deg, #56adb0, #92d5a9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right .el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.header-right .user-name {
  margin: 0 8px;
  color: #606266;
  font-weight: 500;
}

.merchant-content {
  flex: 1;
  overflow: auto;
  padding: 0;
}
</style>