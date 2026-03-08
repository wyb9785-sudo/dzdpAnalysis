<template>
  <div class="etl-monitor-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>ETL任务监控</span>
          <el-button type="primary" @click="refreshTasks">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <ETLMonitor @taskSelect="handleTaskSelect" />
    </el-card>

    <el-card class="mt-4" v-if="selectedTask">
      <template #header>
        <span>任务详情 - {{ selectedTask.taskName }}</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ selectedTask.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ selectedTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="HDFS路径">{{ selectedTask.hdfsPath }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ selectedTask.operator }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(selectedTask.status)">
            {{ selectedTask.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理记录数">{{ selectedTask.recordsProcessed }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ selectedTask.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ selectedTask.endTime || '运行中' }}</el-descriptions-item>
        <el-descriptions-item label="执行时长">
          {{ selectedTask.executionDuration ? selectedTask.executionDuration + ' 秒' : '运行中' }}
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="selectedTask.errorMessage">
          <el-alert :title="selectedTask.errorMessage" type="error" />
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-4" v-if="selectedTask.status === 'SUCCESS'">
        <el-button type="primary" @click="viewQualityReport(selectedTask.id)">
          查看质量报告
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ETLMonitor from '../components/data/ETLMonitor.vue'

export default {
  name: 'ETLMonitorView',
  components: {
    ETLMonitor
  },
  setup() {
    const router = useRouter()
    const selectedTask = ref(null)

    const refreshTasks = () => {
      ElMessage.success('数据已刷新')
      window.location.reload()
    }

    const viewQualityReport = (taskId) => {
      router.push(`/quality-report/${taskId}`)
    }

    const getStatusType = (status) => {
      switch (status) {
        case 'SUCCESS': return 'success'
        case 'RUNNING': return 'warning'
        case 'FAILED': return 'danger'
        default: return 'info'
      }
    }

    const handleTaskSelect = (task) => {
      selectedTask.value = task
    }

    return {
      selectedTask,
      refreshTasks,
      viewQualityReport,
      getStatusType,
      handleTaskSelect
    }
  }
}
</script>

<style scoped>
.etl-monitor-view {
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
</style>