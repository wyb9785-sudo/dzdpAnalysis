<template>
  <div class="data-upload-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据上传</span>
          <el-button type="primary" @click="loadUploadLogs">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      <!-- 使用 FileUpload 组件 -->
      <FileUpload @upload-success="loadUploadLogs" />
    </el-card>

    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>上传记录</span>
          <div>
            <el-button type="danger" @click="deleteSelected" :disabled="selectedLogs.length === 0">
              批量删除
            </el-button>
            <el-button @click="loadUploadLogs">刷新记录</el-button>
          </div>
        </div>
      </template>

      <!-- 上传记录表格 -->
      <el-table
          :data="uploadLogs"
          v-loading="loading"
          @selection-change="handleSelectionChange"
          style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="fileName" label="文件名" width="200" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作员" width="100" />
        <el-table-column prop="uploadStatus" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.uploadStatus === 'SUCCESS' ? 'success' : 'danger'">
              {{ scope.row.uploadStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
                size="small"
                @click="runETL(scope.row.hdfsPath, scope.row.operator || currentOperator)"
                :loading="etlLoading"
            >
              执行ETL
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="deleteLog(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控件 -->
      <div class="pagination">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalItems"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>

      <div v-if="uploadLogs.length === 0 && !loading" class="empty-data">
        暂无上传记录
      </div>
    </el-card>
  </div>
</template>
<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { dataApi, etlApi } from '../api/index.js'
import FileUpload from '../components/data/FileUpload.vue'
import { Refresh } from '@element-plus/icons-vue'

export default {
  name: 'DataUploadView',
  components: {
    FileUpload,
    Refresh
  },
  setup() {
    const router = useRouter()
    const uploadLogs = ref([])
    const loading = ref(false)
    const etlLoading = ref(false) // 添加ETL加载状态
    const selectedLogs = ref([])
    // 分页参数
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)
    // 当前用户
    const currentOperator = ref('admin')

    // 添加缺失的分页方法
    const handleSizeChange = (newSize) => {
      pageSize.value = newSize
      currentPage.value = 1
      loadUploadLogs()
    }

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage
      loadUploadLogs()
    }
    const loadUploadLogs = async () => {
      loading.value = true
      try {
        // 添加分页参数
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value,
          sort: 'uploadTime,desc'
        }

        const response = await dataApi.getUploadLogs(params)

        // 处理响应数据
        if (response.content) {
          // Spring Data JPA 分页响应格式
          uploadLogs.value = response.content
          totalItems.value = response.totalElements
        } else if (Array.isArray(response)) {
          // 普通数组响应格式
          uploadLogs.value = response
          totalItems.value = response.length
        } else {
          uploadLogs.value = []
          totalItems.value = 0
        }
      } catch (error) {
        console.error('加载上传记录失败:', error)
        ElMessage.error('加载上传记录失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }

    const handleSelectionChange = (selection) => {
      selectedLogs.value = selection
    }

    const deleteLog = async (log) => {
      try {
        await ElMessageBox.confirm(
            `确定要删除文件 "${log.fileName}" 吗？此操作也会删除HDFS上的文件。`,
            '确认删除',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        await dataApi.deleteUploadLog(log.id)
        ElMessage.success('删除成功')
        loadUploadLogs()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败: ' + (error.message || '未知错误'))
        }
      }
    }

    const deleteSelected = async () => {
      if (selectedLogs.value.length === 0) {
        ElMessage.warning('请选择要删除的记录')
        return
      }

      try {
        await ElMessageBox.confirm(
            `确定要删除选中的 ${selectedLogs.value.length} 条记录吗？此操作也会删除HDFS上的文件。`,
            '确认批量删除',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        const logIds = selectedLogs.value.map(log => log.id)
        await dataApi.deleteBatchUploadLogs(logIds)

        ElMessage.success('批量删除成功')
        selectedLogs.value = []
        loadUploadLogs()
      } catch (error) {
        if (error !== 'cancel') {
          if (error.response?.status === 403) {
            ElMessage.error('权限不足，需要管理员权限')
          } else {
            ElMessage.error('批量删除失败: ' + (error.message || '未知错误'))
          }
        }
      }
    }

    // 在 DataUploadView.vue 中修改 runETL 方法
    const runETL = async (hdfsPath, operator) => {
      try {
        await ElMessageBox.confirm('确定要执行ETL处理吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        etlLoading.value = true
        ElMessage.info('正在启动ETL任务...')

        // 获取当前用户作为操作员
        const user = JSON.parse(localStorage.getItem('user') || '{}')
        const finalOperator = operator || user.username || 'admin'

        // 执行ETL - 确保传递正确的参数
        const result = await etlApi.run(hdfsPath, finalOperator)

        ElMessage.success('ETL任务已启动，正在处理数据...')

        // 跳转到监控页面
        setTimeout(() => {
          router.push({
            path: '/etl-monitor',
            query: {
              hdfsPath: hdfsPath,
              operator: finalOperator,
              taskId: result.taskId || 'new'
            }
          })
        }, 2000)

      } catch (error) {
        if (error !== 'cancel') {
          console.error('ETL任务启动失败:', error)
          ElMessage.error('ETL任务启动失败: ' + (error.message || '未知错误'))
        }
      } finally {
        etlLoading.value = false
      }
    }

    const viewUploadLogs = () => {
      // 直接重新加载当前页面的数据，而不是跳转
      loadUploadLogs()
    }

    const formatFileSize = (size) => {
      if (!size) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let i = 0
      while (size >= 1024 && i < units.length - 1) {
        size /= 1024
        i++
      }
      return `${size.toFixed(2)} ${units[i]}`
    }

    const formatDate = (dateString) => {
      if (!dateString) return '-'
      try {
        return new Date(dateString).toLocaleString('zh-CN')
      } catch (e) {
        return dateString
      }
    }

    onMounted(() => {
      // 设置当前用户
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      if (user.username) {
        currentOperator.value = user.username
      }

      loadUploadLogs()
    })

    return {
      uploadLogs,
      etlLoading:false,
      loading,
      selectedLogs,
      currentPage,
      pageSize,
      totalItems,
      loadUploadLogs,
      handleSelectionChange,
      handleSizeChange,        // 暴露分页方法
      handleCurrentChange,     // 暴露分页方法
      deleteLog,
      deleteSelected,
      runETL,
      viewUploadLogs,
      formatFileSize,
      formatDate
    }
  }
}
</script>

<style scoped>
.data-upload-view {
  padding: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-4 {
  margin-top: 20px;
}

.empty-data {
  text-align: center;
  padding: 20px;
  color: #999;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>