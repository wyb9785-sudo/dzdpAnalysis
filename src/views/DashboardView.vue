<template>
  <div class="dashboard">
    <el-alert
        v-if="usingMockData"
        title="提示：当前使用模拟数据进行展示"
        type="info"
        :closable="false"
        show-icon
        class="mb-4"
    />
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据统计</span>
              <el-tooltip content="所有上传文件的总数量" placement="top">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-statistic
              title="总上传文件数"
              :value="stats.totalUploads || 0"
              :precision="0"
          />
          <el-statistic
              title="总处理记录数"
              :value="stats.totalRecords || 0"
              :precision="0"
              class="mt-3"
          />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>任务状态</span>
            </div>
          </template>
          <el-statistic
              title="成功任务数"
              :value="stats.successTasks || 0"
              :precision="0"
          />
          <el-statistic
              title="失败任务数"
              :value="stats.failedTasks || 0"
              :precision="0"
              class="mt-3"
          />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据质量</span>
              <el-tooltip content="基于核心字段完整率、内容完整度、价格格式合法率和评分合理性计算" placement="top">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-statistic
              title="平均质量评分"
              :value="stats.avgQuality || 0"
              suffix="%"
              :precision="1"
          />
          <div class="mt-3">
            <el-progress
                :percentage="stats.avgQuality || 0"
                :color="getQualityColor(stats.avgQuality)"
                :show-text="true"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
            </div>
          </template>
          <el-tag :type="clusterStatus.hdfsConnection === 'SUCCESS' ? 'success' : 'danger'" size="large">
            HDFS: {{ clusterStatus.hdfsConnection === 'SUCCESS' ? '正常' : '异常' }}
          </el-tag>
          <el-tag :type="clusterStatus.hiveConnection === 'SUCCESS' ? 'success' : 'danger'" size="large" class="ml-2">
            Hive: {{ clusterStatus.hiveConnection === 'SUCCESS' ? '正常' : '异常' }}
          </el-tag>
          <div class="mt-3">
            <el-button type="primary" @click="refreshStats" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <el-card header="最近上传文件">
          <el-table :data="recentUploads" height="250" v-loading="loading">
            <el-table-column prop="fileName" label="文件名" width="180" />
            <el-table-column prop="fileSize" label="大小" width="100">
              <template #default="scope">
                {{ formatFileSize(scope.row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="uploadTime" label="上传时间" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.uploadTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="操作员" width="80" />
            <el-table-column prop="recordCount" label="记录数" width="80">
              <template #default="scope">
                {{ scope.row.recordCount || 'N/A' }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="recentUploads.length === 0 && !loading" class="empty-data">
            暂无上传记录
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="最近ETL任务">
          <el-table :data="recentTasks" height="250" v-loading="loading">
            <el-table-column prop="taskName" label="任务名" width="180" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" size="small">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recordsProcessed" label="处理数" width="80" />
            <el-table-column prop="startTime" label="开始时间" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.startTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="executionDuration" label="耗时(秒)" width="80">
              <template #default="scope">
                {{ scope.row.executionDuration || 'N/A' }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="recentTasks.length === 0 && !loading" class="empty-data">
            暂无ETL任务
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, InfoFilled } from '@element-plus/icons-vue'
import { dashboardApi, etlApi, clusterApi  } from '../api/index.js'

export default {
  name: 'DashboardView',
  components: {
    Refresh,
    InfoFilled
  },
  setup() {
    const stats = ref({
      totalUploads: 0,
      totalRecords: 0,
      successTasks: 0,
      failedTasks: 0,
      runningTasks: 0,
      avgQuality: 0
    })
    const recentUploads = ref([])
    const recentTasks = ref([])
    const loading = ref(false)
    const clusterStatus = ref({
      hdfsConnection: 'UNKNOWN',
      hiveConnection: 'UNKNOWN'
    })

    // 计算是否使用模拟数据（仅质量评分可能使用模拟数据）
    const usingMockData = computed(() => {
      return stats.value.avgQuality > 0 &&
          (stats.value.totalUploads === 0 ||
              stats.value.totalRecords === 0)
    })

    // DashboardView.vue - 修改 setup 函数中的 loadDashboardData
    const loadDashboardData = async () => {
      loading.value = true
      try {
        // 使用 Promise.allSettled 来处理多个请求
        const [statsResponse, uploadsResponse, tasksResponse, clusterResponse] =
            await Promise.allSettled([
              dashboardApi.getStats(),
              dashboardApi.getRecentUploads(),
              etlApi.getTasks({ page: 0, size: 5, sort: 'startTime,desc' }),
              clusterApi.health()
            ])

        // 处理每个响应
        if (statsResponse.status === 'fulfilled' && statsResponse.value) {
          stats.value = statsResponse.value
        } else {
          console.error('获取统计信息失败:', statsResponse.reason)
          stats.value = {
            totalUploads: 0,
            totalRecords: 0,
            successTasks: 0,
            failedTasks: 0,
            runningTasks: 0,
            avgQuality: 0
          }
        }

        if (uploadsResponse.status === 'fulfilled' && uploadsResponse.value) {
          recentUploads.value = Array.isArray(uploadsResponse.value) ?
              uploadsResponse.value.slice(0, 5) : []
        } else {
          console.error('获取最近上传失败:', uploadsResponse.reason)
          recentUploads.value = []
        }

        if (tasksResponse.status === 'fulfilled' && tasksResponse.value) {
          if (tasksResponse.value.content) {
            recentTasks.value = tasksResponse.value.content.slice(0, 5)
          } else if (Array.isArray(tasksResponse.value)) {
            recentTasks.value = tasksResponse.value.slice(0, 5)
          }
        } else {
          console.error('获取最近任务失败:', tasksResponse.reason)
          recentTasks.value = []
        }

        if (clusterResponse.status === 'fulfilled' && clusterResponse.value) {
          clusterStatus.value = clusterResponse.value
        } else {
          console.error('获取集群状态失败:', clusterResponse.reason)
          clusterStatus.value = {
            hdfsConnection: 'UNKNOWN',
            hiveConnection: 'UNKNOWN'
          }
        }

      } catch (error) {
        console.error('加载仪表盘数据失败:', error)
        ElMessage.error('加载数据失败')

        // 重置为初始状态
        stats.value = {
          totalUploads: 0,
          totalRecords: 0,
          successTasks: 0,
          failedTasks: 0,
          runningTasks: 0,
          avgQuality: 0
        }
        recentUploads.value = []
        recentTasks.value = []
        clusterStatus.value = {
          hdfsConnection: 'UNKNOWN',
          hiveConnection: 'UNKNOWN'
        }
      } finally {
        loading.value = false
      }
    }

    const refreshStats = () => {
      loadDashboardData()
      ElMessage.success('数据已刷新')
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

    const getStatusType = (status) => {
      switch (status) {
        case 'SUCCESS': return 'success'
        case 'RUNNING': return 'warning'
        case 'FAILED': return 'danger'
        default: return 'info'
      }
    }

    const getQualityColor = (score) => {
      if (!score) return 'rgba(248,171,51,0.86)'
      if (score >= 90) return '#5ec0ac'
      if (score >= 70) return '#e18fad'
      return '#e6b1b1'
    }

    onMounted(() => {
      loadDashboardData()
    })

    return {
      stats,
      recentUploads,
      recentTasks,
      loading,
      clusterStatus,
      usingMockData,
      refreshStats,
      formatFileSize,
      formatDate,
      getStatusType,
      getQualityColor
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 5px;
}

.mt-3 {
  margin-top: 12px;
}

.mt-4 {
  margin-top: 20px;
}

.ml-2 {
  margin-left: 8px;
}

.empty-data {
  text-align: center;
  padding: 20px;
  color: #999;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>