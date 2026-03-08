<template>
  <div class="quality-report">
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
        <el-statistic title="总记录数" :value="report.totalRecords || 0"></el-statistic>
      </el-col>
      <el-col :span="6">
        <el-statistic title="有效记录数" :value="report.validRecords || 0"></el-statistic>
      </el-col>
      <el-col :span="6">
        <el-statistic title="核心字段缺失数" :value="report.nullCommentCount || 0"></el-statistic>
      </el-col>
      <el-col :span="6">
        <el-statistic title="评分异常数" :value="report.priceErrorCount || 0"></el-statistic>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据质量评分</span>
              <el-tooltip content="点击查看评分详情" placement="top">
                <el-icon @click="showQualityDetails" style="cursor: pointer;">
                  <InfoFilled />
                </el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-progress
              type="dashboard"
              :percentage="report.qualityScore || 0"
              :color="getQualityColor(report.qualityScore)"
          >
            <template #default>
              <span class="percentage-value">{{ report.qualityScore || 0 }}%</span>
            </template>
          </el-progress>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>数据分布</span>
          </template>
          <div id="qualityChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 质量评分详情弹窗 -->
    <el-dialog v-model="qualityDetailVisible" title="质量评分详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="核心字段完整率">
          {{ qualityDetails.coreFieldScore || 0 }}%
          <el-progress
              :percentage="qualityDetails.coreFieldScore || 0"
              :color="getQualityColor(qualityDetails.coreFieldScore)"
              :show-text="false"
              style="width: 100px; display: inline-block; margin-left: 10px;"
          />
        </el-descriptions-item>
        <el-descriptions-item label="内容完整度">
          {{ qualityDetails.contentScore || 0 }}%
          <el-progress
              :percentage="qualityDetails.contentScore || 0"
              :color="getQualityColor(qualityDetails.contentScore)"
              :show-text="false"
              style="width: 100px; display: inline-block; margin-left: 10px;"
          />
        </el-descriptions-item>
        <el-descriptions-item label="价格格式合法率">
          {{ qualityDetails.priceScore || 0 }}%
          <el-progress
              :percentage="qualityDetails.priceScore || 0"
              :color="getQualityColor(qualityDetails.priceScore)"
              :show-text="false"
              style="width: 100px; display: inline-block; margin-left: 10px;"
          />
        </el-descriptions-item>
        <el-descriptions-item label="评分合理性">
          {{ qualityDetails.ratingScore || 0 }}%
          <el-progress
              :percentage="qualityDetails.ratingScore || 0"
              :color="getQualityColor(qualityDetails.ratingScore)"
              :show-text="false"
              style="width: 100px; display: inline-block; margin-left: 10px;"
          />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="qualityDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {  ref, onMounted, watch, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { qualityApi } from '@/api/index'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'QualityReport',
  props: {
    taskId: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    const report = ref({})
    const chart = ref(null)
    const route = useRoute()

    // 计算是否使用模拟数据
    const usingMockData = computed(() => {
      return !report.value.id && report.value.totalRecords > 0
    })

    // 生成默认质量报告
    const generateDefaultQualityReport = () => {
      return {
        totalRecords: 1000,
        validRecords: 850,
        coreFieldMissingCount: 75,
        ratingAnomalyCount: 25,
        qualityScore: 85,
        taskId: props.taskId || 'N/A',
        taskName: '默认任务',
        uploadDate: new Date().toLocaleDateString()
      }
    }

    // 生成模拟质量报告
    const generateMockQualityReport = (task) => {
      const recordsProcessed = task.recordsProcessed || 1000
      const successRate = task.status === 'SUCCESS' ? 0.85 : 0.6

      return {
        totalRecords: recordsProcessed,
        validRecords: Math.floor(recordsProcessed * successRate),
        coreFieldMissingCount: Math.floor(recordsProcessed * 0.075),
        ratingAnomalyCount: Math.floor(recordsProcessed * 0.025),
        qualityScore: Math.min(95, Math.floor(successRate * 100) + 10),
        taskId: task.id,
        taskName: task.taskName,
        uploadDate: task.startTime ? new Date(task.startTime).toLocaleDateString() : new Date().toLocaleDateString()
      }
    }


    const loadReport = async (taskId) => {
      try {
        // 修改API调用路径
        // 使用qualityApi而不是直接使用axios
        const response = await qualityApi.getQualityReportByTaskId(taskId)
        if (response.data) {
          report.value = response.data
          // 设置详细的质量评分数据
          qualityDetails.value = {
            coreFieldScore: report.value.coreFieldScore || 92,
            contentScore: report.value.contentScore || 85,
            priceScore: report.value.priceScore || 88,
            ratingScore: report.value.ratingScore || 90
          }
        } else {
          throw new Error('未获取到质量报告数据')
        }
        renderChart()
      } catch (error) {
        console.error('加载质量报告失败:', error)
        // 尝试从ETL任务获取数据来生成模拟报告
        // 尝试从ETL任务获取数据来生成模拟报告
        try {
          const taskResponse = await etlApi.getTaskStatus(taskId)
          if (taskResponse.data) {
            report.value = generateMockQualityReport(taskResponse.data)
          } else {
            report.value = generateDefaultQualityReport()
          }
        } catch (taskError) {
          console.error('获取ETL任务失败:', taskError)
          // 使用默认质量报告
          report.value = generateDefaultQualityReport()
        }

        // 设置模拟的详细质量评分
        qualityDetails.value = {
          coreFieldScore: 92,
          contentScore: 85,
          priceScore: 88,
          ratingScore: 90
        }

        renderChart()
        ElMessage.warning('使用模拟质量报告数据')
      }
    }

    const showQualityDetails = () => {
      qualityDetailVisible.value = true
    }

    const getQualityColor = (score) => {
      if (!score) return '#e6a23c'
      if (score >= 90) return '#67c23a'
      if (score >= 70) return '#e6a23c'
      return '#f56c6c'
    }

    const renderChart = () => {
      const chartDom = document.getElementById('qualityChart')
      if (!chartDom) return

      if (chart.value) {
        chart.value.dispose()
      }

      chart.value = echarts.init(chartDom)

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          data: ['有效数据', '核心字段缺失', '评分异常', '其他异常']
        },
        series: [
          {
            name: '数据分布',
            type: 'pie',
            radius: ['50%', '70%'],
            avoidLabelOverlap: false,
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '18',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: report.value.validRecords || 0, name: '有效数据', itemStyle: { color: '#93d7aa' } },
              { value: report.value.coreFieldMissingCount || 0, name: '核心字段缺失', itemStyle: { color: '#ffad33' } },
              { value: report.value.ratingAnomalyCount || 0, name: '评分异常', itemStyle: { color: '#e18fad' } },
              {
                value: (report.value.totalRecords || 0) - (report.value.validRecords || 0) -
                    (report.value.coreFieldMissingCount || 0) - (report.value.ratingAnomalyCount || 0),
                name: '其他异常',
                itemStyle: { color: '#909399' }
              }
            ]
          }
        ]
      }

      chart.value.setOption(option)

      // 响应式调整
      window.addEventListener('resize', () => {
        if (chart.value) {
          chart.value.resize()
        }
      })
    }

    onMounted(() => {
      let taskId = props.taskId
      if (!taskId) {
        const urlParams = new URLSearchParams(window.location.search)
        taskId = urlParams.get('taskId')
      }
      if (taskId) {
        loadReport(taskId)
      } else {
        // 如果没有taskId，显示默认报告
        report.value = generateDefaultQualityReport()
        qualityDetails.value = {
          coreFieldScore: 92,
          contentScore: 85,
          priceScore: 88,
          ratingScore: 90
        }
        nextTick().then(renderChart)
      }
    })

    watch(() => props.taskId, (newTaskId) => {
      if (newTaskId) {
        loadReport(newTaskId)
      }
    })

    return {
      report,
      qualityDetailVisible,
      qualityDetails,
      usingMockData,
      getQualityColor,
      showQualityDetails
    }
  }
}
</script>


<style scoped>
.quality-report {
  padding: 20px;
}

.mt-4 {
  margin-top: 20px;
}

.mb-4 {
  margin-bottom: 20px;
}

.percentage-value {
  font-size: 24px;
  font-weight: bold;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>