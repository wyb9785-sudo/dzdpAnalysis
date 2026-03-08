<script setup>
import { ref, reactive, onMounted, defineEmits, watch } from 'vue'  // 添加watch导入
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { dataApi ,api, etlApi} from '../../api/index.js'
import axios from 'axios' // 添加axios导入


const router = useRouter()
const uploadRef = ref()
const loading = ref(false)
// 添加缺失的变量
const etlLoading = ref(false)
const currentOperator = ref('')
const uploadResult = ref(null)

// 修复：使用单独的文件引用
const selectedFile = ref(null)
// 分页相关变量
const pageSize = ref(10)
const currentPage = ref(1)

const form = reactive({
  operator: ''
})

const fileList = ref([])

const emit = defineEmits(['upload-success'])

// 添加文件选择处理 - 修复参数名错误
const handleFileChange = (file, files) => {
  console.log('文件选择:', file)
  selectedFile.value = file.raw // 保存文件对象
  fileList.value = files // 更新文件列表
}

const beforeUpload = (file) => {
  const isCSV = file.name.endsWith('.csv') || file.name.endsWith('.CSV')
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.XLSX')

  if (!isCSV && !isExcel) {
    ElMessage.error('只支持CSV和Excel格式的文件')
    return false
  }

  if (file.size > 10 * 1024 * 1024 * 1024) { // 10GB
    ElMessage.error('文件大小不能超过10GB')
    return false
  }

  return true
}

const submitUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  if (!form.operator) {
    ElMessage.warning('请输入操作员名称')
    return
  }

  loading.value = true

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('operator', form.operator)

    console.log('开始上传文件:', selectedFile.value.name)
    console.log('FormData内容检查:')
    for (let [key, value] of formData.entries()) {
      console.log(key, value)
    }

    const uploadApi = axios.create({
      baseURL: 'http://localhost:8080',
      timeout: 30000
    })

    // 添加token到新实例
    const token = localStorage.getItem('token')
    if (token) {
      uploadApi.defaults.headers.common['Authorization'] = `Bearer ${token}`
    }

    const result = await uploadApi.post('/api/data/upload', formData)
    uploadResult.value = result.data
    ElMessage.success('文件上传成功')
    emit('upload-success')
    // 上传成功后询问是否执行ETL
    ElMessageBox.confirm('文件上传成功，是否立即执行ETL处理？', '提示', {
      confirmButtonText: '立即执行',
      cancelButtonText: '稍后处理',
      type: 'success'
    }).then(() => {
      triggerETL(result.data.hdfsPath, form.operator)
    }).catch(() => {
      // 用户取消
    })
  } catch (error) {
    console.error('上传失败详情:', error)
    ElMessage.error('上传失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  fileList.value = []
  selectedFile.value = null
  form.operator = currentOperator.value
  uploadResult.value = null
}

// 添加ETL任务状态检查相关代码
const checkEtlTaskStatus = async (taskId) => {
  const maxChecks = 120; // 最大检查次数（10分钟）
  const checkInterval = 5000; // 每5秒检查一次

  for (let i = 0; i < maxChecks; i++) {
    try {
      const status = await etlApi.getTaskStatus(taskId);

      if (status === 'SUCCESS') {
        ElMessage.success('ETL任务处理完成');
        return true;
      } else if (status === 'FAILED') {
        ElMessage.error('ETL任务处理失败');
        return false;
      }

      // 任务还在运行，继续等待
      await new Promise(resolve => setTimeout(resolve, checkInterval));

    } catch (error) {
      console.error('检查ETL任务状态失败:', error);
      // 网络错误不影响继续检查
      await new Promise(resolve => setTimeout(resolve, checkInterval));
    }
  }

  ElMessage.warning('ETL任务处理超时，请稍后在监控页面查看状态');
  return false;
}

// 修改triggerETL方法
const triggerETL = async (hdfsPath, operator) => {
  etlLoading.value = true;
  try {
    ElMessage.info('正在启动ETL任务，请稍后...');

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const finalOperator = operator || user.username || 'admin';
    // 确保参数正确传递
    console.log('ETL参数:', { hdfsPath, operator: finalOperator });
    // 启动ETL任务
    const result = await etlApi.run(hdfsPath, finalOperator);

    ElMessage.success('ETL任务已启动，正在处理数据...');

    // 启动状态检查
    const taskSuccess = await checkEtlTaskStatus(result.taskId);

    if (taskSuccess) {
      // 任务成功，跳转到监控页面
      router.push({
        path: '/etl-monitor',
        query: {
          hdfsPath: hdfsPath,
          operator: finalOperator,
          taskId: result.taskId
        }
      });
    } else {
      // 任务失败或超时，仍然跳转但显示警告
      router.push({
        path: '/etl-monitor',
        query: {
          hdfsPath: hdfsPath,
          operator: finalOperator,
          taskId: result.taskId,
          warning: '任务处理时间较长，请稍后查看结果'
        }
      });
    }

  } catch (error) {
    console.error('ETL任务启动失败:', error);
    ElMessage.error('ETL任务启动失败: ' + (error.message || '未知错误'));
  } finally {
    etlLoading.value = false;
  }
}

const viewUploadLogs = () => {
  router.push('/data-upload')
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
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  // 设置当前用户为默认操作员
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.username) {
    currentOperator.value = user.username
    form.operator = user.username
  }
})

// 暴露给模板
defineExpose({
  form,
  fileList,
  uploadRef,
  loading,
  //etlLoading,
  currentOperator,
  uploadResult,
  beforeUpload,
  handleFileChange,
  submitUpload,
  resetForm,
  triggerETL,
  viewUploadLogs,
  formatFileSize,
  formatDate
})
</script>

<template>
  <div class="file-upload">
    <el-card>
      <template #header>
        <span>数据文件上传</span>
      </template>

      <el-form :model="form" label-width="100px">
        <el-form-item label="选择文件" required>
          <el-upload
              class="upload-demo"
              :on-change="handleFileChange"
              :before-upload="beforeUpload"
              :limit="1"
              :file-list="fileList"
              :auto-upload="false"
              ref="uploadRef"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">
                支持CSV和Excel格式，最大10GB。请先选择文件，然后填写操作员信息，最后点击开始上传。
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="操作员" required>
          <el-input
              v-model="form.operator"
              placeholder="请输入操作员用户名"
              :disabled="!!currentOperator"
          />
          <div v-if="currentOperator" class="operator-info">
            当前用户: {{ currentOperator }}
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              @click="submitUpload"
              :loading="loading"
              :disabled="!form.operator || !selectedFile"
          >
            开始上传
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 上传结果展示 -->
    <el-card v-if="uploadResult" class="mt-4">
      <template #header>
        <span>上传结果</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件名称">{{ uploadResult.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(uploadResult.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="HDFS路径">{{ uploadResult.hdfsPath }}</el-descriptions-item>
        <el-descriptions-item label="记录数量">{{ uploadResult.recordCount }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatDate(uploadResult.uploadTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ uploadResult.operator }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-buttons mt-4">
        <el-button
            type="primary"
            @click="triggerETL(uploadResult.hdfsPath, form.operator)"
            :loading="etlLoading"
        >
          立即执行ETL处理
        </el-button>
        <el-button @click="viewUploadLogs">查看上传日志</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.file-upload {
  padding: 20px;
}

.upload-demo {
  width: 100%;
}

.operator-info {
  margin-top: 5px;
  color: #666;
  font-size: 12px;
}

.mt-4 {
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}
</style>