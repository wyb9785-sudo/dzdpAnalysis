<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <!-- 搜索和筛选 -->
      <div class="filter-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名或邮箱"
            style="width: 450px; margin-right: 15px"
            @keyup.enter="loadUsers"
        >
          <template #append>
            <el-button @click="loadUsers">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <el-select v-model="filterRole" placeholder="角色筛选" @change="loadUsers">
          <el-option label="全部角色" value=""></el-option>
          <el-option label="管理员" value="ADMIN"></el-option>
          <el-option label="商户" value="MERCHANT"></el-option>
          <el-option label="顾客" value="CUSTOMER"></el-option>
        </el-select>

        <el-select v-model="filterStatus" placeholder="状态筛选" @change="loadUsers" style="margin-left: 10px">
          <el-option label="全部状态" value=""></el-option>
          <el-option label="活跃" value="ACTIVE"></el-option>
          <el-option label="禁用" value="INACTIVE"></el-option>
          <el-option label="锁定" value="LOCKED"></el-option>
        </el-select>
      </div>

      <!-- 用户表格 -->
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)">
              {{ scope.row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.lastLoginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="showEditDialog(scope.row)">
              编辑
            </el-button>
            <el-button
                size="small"
                :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'"
                @click="toggleUserStatus(scope.row)"
            >
              {{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleDelete(scope.row)"
                v-if="scope.row.role !== 'ADMIN'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalItems"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadUsers"
            @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog
        :title="isEditing ? '编辑用户' : '新增用户'"
        v-model="dialogVisible"
        width="500px"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!isEditing">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" type="email" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="商户" value="MERCHANT" />
            <el-option label="顾客" value="CUSTOMER" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" placeholder="请选择状态">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>

        <el-form-item label="商户名称" prop="merchantName" v-if="userForm.role === 'MERCHANT'">
          <el-input v-model="userForm.merchantName" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUserForm" :loading="submitting">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi,qualityApi } from '../api/index.js'
import { useRouter } from 'vue-router'

export default {
  name: 'UserManagementView',
  setup() {
    const router = useRouter()
    const users = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const isEditing = ref(false)
    const submitting = ref(false)
    const userFormRef = ref()

    // 分页参数
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)

    // 筛选参数
    const searchKeyword = ref('')
    const filterRole = ref('')
    const filterStatus = ref('')

    const userForm = reactive({
      userId: null,
      username: '',
      password: '',
      email: '',
      phone: '',
      role: 'CUSTOMER',
      status: 'ACTIVE',
      merchantName: ''
    })

    const userRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 100, message: '用户名长度在 3 到 100 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ],
      role: [
        { required: true, message: '请选择角色', trigger: 'change' }
      ],
      status: [
        { required: true, message: '请选择状态', trigger: 'change' }
      ],
      merchantName: [
        { required: true, message: '请输入商户名称', trigger: 'blur', when: (form) => form.role === 'MERCHANT' }
      ]
    }

    const loadUsers = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value,
          sortField: 'createTime',
          sortDirection: 'desc'
        }

        if (searchKeyword.value) params.search = searchKeyword.value
        if (filterRole.value) params.role = filterRole.value
        if (filterStatus.value) params.status = filterStatus.value

        const response = await authApi.getUsers(params)

        // 统一处理响应格式
        if (response && response.users) {
          users.value = response.users
          totalItems.value = response.totalItems || 0
        } else if (Array.isArray(response)) {
          users.value = response
          totalItems.value = response.length
        } else if (response && response.data && Array.isArray(response.data)) {
          users.value = response.data
          totalItems.value = response.data.length
        } else {
          users.value = getMockUsers()
          totalItems.value = users.value.length
          ElMessage.warning('使用模拟数据，后端API返回格式异常')
        }
      } catch (error) {
        console.error('加载用户列表失败详情:', error)
        console.error('错误响应:', error.response)
        if (error.response?.status === 500) {
          ElMessage.error('服务器内部错误，请检查后端日志')
        } else if (error.response?.status === 403) {
          ElMessage.error('权限不足，需要管理员权限')
          router.push('/dashboard')
        } else {
          ElMessage.error('加载用户列表失败: ' + (error.message || '未知错误'))
        }
      } finally {
        loading.value = false
      }
    }

    const getMockUsers = () => {
      return [
        {
          userId: 1,
          username: 'admin',
          email: 'admin@example.com',
          phone: '13800138000',
          role: 'ADMIN',
          status: 'ACTIVE',
          lastLoginTime: '2023-12-01T10:30:00',
          createTime: '2023-01-01T00:00:00'
        },
        {
          userId: 2,
          username: 'merchant1',
          email: 'merchant1@example.com',
          phone: '13900139000',
          role: 'MERCHANT',
          status: 'ACTIVE',
          merchantName: '测试商户1',
          lastLoginTime: '2023-12-01T09:15:00',
          createTime: '2023-02-01T00:00:00'
        },
        {
          userId: 3,
          username: 'customer1',
          email: 'customer1@example.com',
          phone: '13700137000',
          role: 'CUSTOMER',
          status: 'ACTIVE',
          lastLoginTime: '2023-11-30T16:45:00',
          createTime: '2023-03-01T00:00:00'
        }
      ]
    }

    const showCreateDialog = () => {
      isEditing.value = false
      resetUserForm()
      dialogVisible.value = true
    }

    const showEditDialog = (user) => {
      isEditing.value = true
      Object.assign(userForm, user)
      dialogVisible.value = true
    }

    const resetUserForm = () => {
      Object.assign(userForm, {
        userId: null,
        username: '',
        password: '',
        email: '',
        phone: '',
        role: 'CUSTOMER',
        status: 'ACTIVE',
        merchantName: ''
      })
    }

    const submitUserForm = async () => {
      if (!userFormRef.value) return

      try {
        await userFormRef.value.validate()
        submitting.value = true

        if (isEditing.value) {
          await authApi.updateUser(userForm.userId, userForm)
          ElMessage.success('用户更新成功')
        } else {
          await authApi.createUser(userForm)
          ElMessage.success('用户创建成功')
        }

        dialogVisible.value = false
        loadUsers()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败: ' + (error.message || '未知错误'))
      } finally {
        submitting.value = false
      }
    }

    const toggleUserStatus = async (user) => {
      try {
        const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
        console.log('更新用户状态请求:', {
          userId: user.userId,
          newStatus,
          url: `/api/admin/users/${user.userId}/status?status=${newStatus}`
        })

        const response = await authApi.updateUserStatus(user.userId, newStatus)
        console.log('更新响应:', response)

        ElMessage.success('用户状态更新成功')
        loadUsers()
      } catch (error) {
        console.error('更新用户状态失败详情:', error)
        console.error('错误配置:', error.config) // 查看请求配置
        ElMessage.error('更新用户状态失败: ' + (error.message || '未知错误'))
      }
    }

    const handleDelete = async (user) => {
      try {
        await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await authApi.deleteUser(user.userId)
        ElMessage.success('用户删除成功')
        loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除用户失败:', error)
          ElMessage.error('删除用户失败: ' + (error.message || '未知错误'))
        }
      }
    }

    const getRoleType = (role) => {
      switch (role) {
        case 'ADMIN':
          return 'danger'
        case 'MERCHANT':
          return 'warning'
        case 'CUSTOMER':
          return 'success'
        default:
          return 'info'
      }
    }

    const getStatusType = (status) => {
      switch (status) {
        case 'ACTIVE':
          return 'success'
        case 'INACTIVE':
          return 'warning'
        case 'LOCKED':
          return 'danger'
        default:
          return 'info'
      }
    }

    const formatDate = (dateString) => {
      if (!dateString) return ''
      try {
        return new Date(dateString).toLocaleString()
      } catch (e) {
        return dateString
      }
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      users,
      loading,
      dialogVisible,
      isEditing,
      submitting,
      userFormRef,
      currentPage,
      pageSize,
      totalItems,
      searchKeyword,
      filterRole,
      filterStatus,
      userForm,
      userRules,
      loadUsers,
      showCreateDialog,
      showEditDialog,
      submitUserForm,
      toggleUserStatus,
      handleDelete,
      getRoleType,
      getStatusType,
      formatDate
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>