<template>
  <div class="main-layout">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>大数据处理一体化平台</h1>
          <div class="header-actions">
            <span class="user-info">欢迎，{{ currentUser.username }} [{{ currentUser.role }}]</span>
            <el-button type="primary" @click="checkClusterHealth" size="small">
              集群状态
            </el-button>
            <el-button @click="handleLogout" size="small">
              退出登录
            </el-button>
          </div>
        </div>
      </el-header>
      <el-container>
        <el-aside width="180px">
          <el-menu
              router
              :default-active="$route.path"
              class="sidebar-menu"
          >
            <el-menu-item index="/dashboard">
              <el-icon><Monitor /></el-icon>
              <span>系统仪表盘</span>
            </el-menu-item>

<!--            <el-sub-menu index="admin-menu" v-if="currentUser.role === 'ADMIN'">-->
<!--              <template #title>-->
<!--                <el-icon><Setting /></el-icon>-->
<!--                <span>管理员功能</span>-->
<!--              </template>-->
              <el-menu-item index="/data-upload">
                <el-icon><Upload /></el-icon>
                <span>数据上传</span>
              </el-menu-item>
              <el-menu-item index="/etl-monitor">
                <el-icon><DataLine /></el-icon>
                <span>ETL任务监控</span>
              </el-menu-item>
              <el-menu-item index="/user-management">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </el-menu-item>

            <el-menu-item index="/quality-report">
              <el-icon><Document /></el-icon>
              <span>数据质量报告</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { Monitor, Upload, Setting, Document, User, DataLine } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { clusterApi } from '../../api/index.js'

export default {
  name: 'MainLayout',
  components: {
    Monitor,
    Upload,
    Setting,
    Document,
    User,
    DataLine
  },
  setup() {
    const router = useRouter()
    const currentUser = ref({
      username: '',
      role: ''
    })

    const loadUserInfo = () => {
      const userData = localStorage.getItem('user')
      if (userData) {
        currentUser.value = JSON.parse(userData)
      }
    }

    const checkClusterHealth = async () => {
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          ElMessage.error('请先登录系统')
          window.location.href = '/login'
          return
        }

        const result = await clusterApi.health()
        const status = result.overallStatus || result.status

        if (status === 'READY') {
          ElMessage.success('集群状态正常')
        } else if (status === 'DEGRADED') {
          ElMessage.warning('集群状态: 部分服务异常')
        } else {
          ElMessage.warning(`集群状态: ${status}`)
        }

      } catch (error) {
        console.error('集群状态检查失败:', error)
        if (error.response?.status === 403) {
          ElMessage.error('权限不足，无法检查集群状态')
        } else {
          ElMessage.error('集群状态检查失败: ' + (error.response?.data?.message || error.message))
        }
      }
    }

    const handleLogout = async () => {
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '确认退出', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 清除本地存储
        localStorage.removeItem('token')
        localStorage.removeItem('user')

        ElMessage.success('退出登录成功')
        router.push('/login')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('退出登录失败')
        }
      }
    }

    onMounted(() => {
      loadUserInfo()
    })

    return {
      currentUser,
      checkClusterHealth,
      handleLogout
    }
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.el-header {
  background-color: #5ec0ac;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 30px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-content h1 {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  margin-right: 15px;
  font-size: 14px;
}

.sidebar-menu {
  height: 100%;
  border-right: solid;
  border-right-color: white;
  background-color: #cbddd6;
}

.el-main {
  padding: 20px;
  background-color: #cbddd6;
}
</style>