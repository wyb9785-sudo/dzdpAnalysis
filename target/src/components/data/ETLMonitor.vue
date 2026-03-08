<template>
  <div class="etl-monitor">
    <el-card header="ETL任务监控">
      <!-- 搜索和筛选栏 -->
      <div class="filter-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索任务名称或操作员"
            style="width: 275px; margin-right: 15px"
            @keyup.enter="loadTasks"
        >
          <template #append>
            <el-button @click="loadTasks">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
        <el-select v-model="filterStatus" placeholder="状态筛选" @change="loadTasks">
          <el-option label="全部状态" value=""></el-option>
          <el-option label="成功" value="SUCCESS"></el-option>
          <el-option label="运行中" value="RUNNING"></el-option>
          <el-option label="失败" value="FAILED"></el-option>
        </el-select>
      </div>
      <!-- 使用 tasks 而不是 filteredTasks -->
      <el-table :data="tasks" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="任务ID" width="80"></el-table-column>
        <el-table-column prop="taskName" label="任务名称" width="150"></el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordsProcessed" label="处理记录数" width="100"></el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="scope">
            {{ scope.row.endTime ? formatDate(scope.row.endTime) : '运行中' }}
          </template>
        </el-table-column>
        <el-table-column prop="executionDuration" label="执行时长(秒)" width="120">
          <template #default="scope">
            {{ scope.row.executionDuration || '运行中' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" @click="viewDetails(scope.row)">
              详情
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
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>

    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="70%">
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="HDFS路径">{{ currentTask.hdfsPath }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ currentTask.operator }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTask.status)">
            {{ currentTask.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理记录数">{{ currentTask.recordsProcessed }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(currentTask.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentTask.endTime ? formatDate(currentTask.endTime) : '运行中' }}</el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ currentTask.executionDuration || '运行中' }} 秒</el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentTask.errorMessage">
          <el-alert :title="currentTask.errorMessage" type="error" />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted,computed } from 'vue'
import { ElMessage } from 'element-plus'
import { etlApi } from '../../api/index.js'

export default {
  name: 'ETLMonitor',
  props: {
    autoRefresh: {
      type: Boolean,
      default: true
    }
  },
  setup(props, { emit }) {
    const tasks = ref([])
    const loading = ref(false)
    const detailVisible = ref(false)
    const currentTask = ref(null)

    // 分页参数
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)

    // 筛选参数
    const searchKeyword = ref('')
    const filterStatus = ref('')

    // 生成模拟任务数据
    const generateMockTasks = () => {
      return [
        {
          id: 1,
          taskName: '数据清洗任务',
          status: 'SUCCESS',
          recordsProcessed: 1500,
          startTime: '2024-01-15T10:30:00',
          endTime: '2024-01-15T10:45:00',
          executionDuration: 900,
          hdfsPath: '/dianping/data/raw/file1.csv',
          operator: 'admin'
        },
        {
          id: 2,
          taskName: '商户分析任务',
          status: 'RUNNING',
          recordsProcessed: 800,
          startTime: '2024-01-15T11:00:00',
          endTime: null,
          executionDuration: null,
          hdfsPath: '/dianping/data/raw/file2.csv',
          operator: 'user1'
        },
        {
          id: 3,
          taskName: '评论处理任务',
          status: 'FAILED',
          recordsProcessed: 200,
          startTime: '2024-01-15T09:00:00',
          endTime: '2024-01-15T09:05:00',
          executionDuration: 300,
          hdfsPath: '/dianping/data/raw/file3.csv',
          operator: 'user2',
          errorMessage: 'HDFS文件不存在'
        }
      ]
    }

    const loadTasks = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value,
          sort: 'startTime,desc'
        }

        // 添加筛选条件
        if (filterStatus.value) {
          params.status = filterStatus.value
        }
        if (searchKeyword.value) {
          params.search = searchKeyword.value
        }

        const response = await etlApi.getTasks(params)

        // 处理Spring Data Page响应格式
        if (response && response.content) {
          tasks.value = response.content
          totalItems.value = response.totalElements || 0
        } else if (Array.isArray(response)) {
          tasks.value = response
          totalItems.value = response.length
        } else {
          // 如果没有数据，使用模拟数据
          tasks.value = generateMockTasks()
          totalItems.value = tasks.value.length
        }
      } catch (error) {
        console.error('加载ETL任务失败:', error)
        // 生成模拟数据用于测试
        tasks.value = generateMockTasks()
        totalItems.value = tasks.value.length
        ElMessage.warning('使用模拟数据进行展示')
      } finally {
        loading.value = false
      }
    }

    const handleSizeChange = (newSize) => {
      pageSize.value = newSize
      currentPage.value = 1
      loadTasks()
    }

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage
      loadTasks()
    }

    const getStatusType = (status) => {
      switch (status) {
        case 'SUCCESS':
          return 'success'
        case 'RUNNING':
          return 'warning'
        case 'FAILED':
          return 'danger'
        default:
          return 'info'
      }
    }

    const viewDetails = (task) => {
      currentTask.value = task
      detailVisible.value = true
      emit('taskSelect', task)
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
      loadTasks()
      if (props.autoRefresh) {
        setInterval(loadTasks, 30000)
      }
    })

    return {
      tasks,
      loading,
      currentPage,
      pageSize,
      totalItems,
      searchKeyword,
      filterStatus,
      detailVisible,
      currentTask,
      loadTasks,
      handleSizeChange,
      handleCurrentChange,
      getStatusType,
      viewDetails,
      formatDate
    }
  }
}
</script>

<style scoped>
.etl-monitor {
  padding: 5px;
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