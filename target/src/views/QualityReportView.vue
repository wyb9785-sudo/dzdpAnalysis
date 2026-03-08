<template>
  <div class="quality-report-view">
    <el-row :gutter="20">
      <!-- ETL任务列表 -->
      <el-col :span="8">
        <el-card class="task-list-card">
          <template #header>
            <div class="card-header">
              <span>ETL任务列表</span>
              <el-button type="primary" @click="refreshReports">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-table
              :data="qualityReports"
              highlight-current-row
              @current-change="handleTaskSelect"
              style="width: 100%"
              height="250"
              v-loading="loading">
            <el-table-column prop="taskName" label="任务名称" width="180" />
            <el-table-column prop="totalRecords" label="总记录数" width="90" />
            <el-table-column prop="qualityScore" label="质量评分" width="90">
              <template #default="scope">
                <el-tag :type="getScoreType(scope.row.qualityScore)" size="small">
                  {{ scope.row.qualityScore }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
              small
              :current-page="pagination.currentPage"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              @current-change="handlePageChange"
              layout="prev, pager, next"
              class="mt-3"
          />

          <!-- 数据质量评分卡片 - 移动到下方 -->
          <el-card class="quality-score-card mt-3" v-if="selectedReport">
            <template #header>
              <span>数据质量评分</span>
            </template>
            <div class="score-container">
              <el-progress
                  type="dashboard"
                  :percentage="selectedReport.qualityScore || 0"
                  :color="getQualityColor(selectedReport.qualityScore)"
                  :width="90"
              >
                <template #default>
                  <span class="percentage-value">{{ selectedReport.qualityScore || 0 }}%</span>
                </template>
              </el-progress>
              <div class="score-details">
                <div class="score-item">
                  <span class="label">核心字段:</span>
                  <span class="value">{{ selectedReport.coreFieldScore || 0 }}%</span>
                </div>
                <div class="score-item">
                  <span class="label">内容完整:</span>
                  <span class="value">{{ selectedReport.contentScore || 0 }}%</span>
                </div>
                <div class="score-item">
                  <span class="label">价格格式:</span>
                  <span class="value">{{ selectedReport.priceScore || 0 }}%</span>
                </div>
                <div class="score-item">
                  <span class="label">评分合理:</span>
                  <span class="value">{{ selectedReport.ratingScore || 0 }}%</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-card>
      </el-col>

      <!-- 数据质量报告详情 -->
      <el-col :span="16">
        <el-card v-if="selectedReport" class="report-detail-card">
          <template #header>
            <div class="card-header">
              <span>数据质量报告 - {{ selectedReport.taskName }}</span>
              <el-tag type="success">任务ID: {{ selectedReport.taskId }}</el-tag>
            </div>
          </template>

          <!-- 数据验证提示 -->
          <el-alert
              v-if="selectedReport.totalRecords === selectedReport.validRecords"
              title="数据质量优秀：所有记录均为有效记录"
              type="success"
              :closable="false"
              class="mb-3"
          />
          <el-alert
              v-else
              :title="`数据质量良好：${selectedReport.validRecords}/${selectedReport.totalRecords} 有效记录`"
              type="info"
              :closable="false"
              class="mb-3"
          />

          <!-- 基本统计信息 -->
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="总记录数" :value="selectedReport.totalRecords || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="有效记录数" :value="selectedReport.validRecords || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="核心字段缺失数" :value="selectedReport.coreFieldMissingCount || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="评分异常数" :value="selectedReport.ratingAnomalyCount || 0" />
            </el-col>
          </el-row>

          <el-row :gutter="20" class="mt-4">
            <!-- 数据分布图表 - 放在左边 -->
            <el-col :span="12">
              <el-card class="chart-card">
                <template #header>
                  <span>数据分布</span>
                </template>
                <div id="qualityChart" style="width: 100%; height: 417px;"></div>
              </el-card>
            </el-col>

            <!-- 详细评分和业务指标 - 放在右边，上下排放 -->
            <el-col :span="12">
              <!-- 详细评分分析 -->
              <el-card class="detail-card mb-3">
                <template #header>
                  <span>详细评分分析</span>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="核心字段完整率">
                    <div class="score-progress">
                      <el-progress
                          :percentage="selectedReport.coreFieldScore || 0"
                          :color="getQualityColor(selectedReport.coreFieldScore)"
                          :show-text="false"
                      />
                      <span class="score-value">{{ selectedReport.coreFieldScore || 0 }}%</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="内容完整度">
                    <div class="score-progress">
                      <el-progress
                          :percentage="selectedReport.contentScore || 0"
                          :color="getQualityColor(selectedReport.contentScore)"
                          :show-text="false"
                      />
                      <span class="score-value">{{ selectedReport.contentScore || 0 }}%</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="价格格式合法率">
                    <div class="score-progress">
                      <el-progress
                          :percentage="selectedReport.priceScore || 0"
                          :color="getQualityColor(selectedReport.priceScore)"
                          :show-text="false"
                      />
                      <span class="score-value">{{ selectedReport.priceScore || 0 }}%</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="评分合理性">
                    <div class="score-progress">
                      <el-progress
                          :percentage="selectedReport.ratingScore || 0"
                          :color="getQualityColor(selectedReport.ratingScore)"
                          :show-text="false"
                      />
                      <span class="score-value">{{ selectedReport.ratingScore || 0 }}%</span>
                    </div>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>

              <!-- 业务指标 -->
              <el-card class="detail-card">
                <template #header>
                  <span>业务指标</span>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="商户数量">
                    {{ selectedReport.merchantCount || 0 }}
                  </el-descriptions-item>
                  <el-descriptions-item label="平均评分">
                    {{ formatRating(selectedReport.avgRating) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="正面评价率">
                    <div class="score-progress">
                      <el-progress
                          :percentage="selectedReport.positiveRate || 0"
                          color="#67c23a"
                          :show-text="false"
                      />
                      <span class="score-value">{{ formatPositiveRate(selectedReport.positiveRate) }}%</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="处理时间">
                    {{ formatDate(selectedReport.createTime) }}
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>
        </el-card>

        <el-card v-else class="empty-card">
          <template #header>
            <span>数据质量报告</span>
          </template>
          <div class="empty-state">
            <el-icon size="50"><Document /></el-icon>
            <p>请从左侧选择一个ETL任务查看详细的质量报告</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { qualityApi } from '@/api/index'
import { Refresh, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'QualityReportView',
  components: {
    Refresh,
    Document
  },
  setup() {
    const qualityReports = ref([])
    const selectedReport = ref(null)
    const chart = ref(null)
    const loading = ref(false)
    const usingMockData = ref(false)
    const pagination = ref({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    // 修改加载质量报告方法
    const loadQualityReports = async (page = 1) => {
      loading.value = true
      usingMockData.value = false

      try {
        console.log('开始加载质量报告，页码:', page)

        // 使用qualityApi获取数据
        const response = await qualityApi.getQualityReportsList({
          page: page - 1, // 后端从0开始
          size: pagination.value.pageSize
        })

        console.log('API响应:', response)

        console.log('响应内容:', response.content)
        console.log('总记录数:', response.totalElements)

        // 检查是否为分页响应格式
        if (response && response.content && Array.isArray(response.content) && response.content.length > 0) {
          qualityReports.value = response.content
          pagination.value.total = response.totalElements || 0
          pagination.value.currentPage = page

          console.log('成功加载', qualityReports.value.length, '条质量报告')
          console.log('第一条记录:', qualityReports.value[0])

          // 默认选择第一个报告
          if (qualityReports.value.length > 0 && !selectedReport.value) {
            selectedReport.value = qualityReports.value[0]
            nextTick(() => {
              renderChart()
            })
          }
        }
        // 检查是否为数组格式（没有分页信息）
        else if (response && Array.isArray(response) && response.length > 0) {
          qualityReports.value = response
          pagination.value.total = response.length
          pagination.value.currentPage = page

          console.log('成功加载数组格式数据:', qualityReports.value.length, '条质量报告')

          // 默认选择第一个报告
          if (qualityReports.value.length > 0 && !selectedReport.value) {
            selectedReport.value = qualityReports.value[0]
            nextTick(() => {
              renderChart()
            })
          }
        }
        else {
          console.warn('API返回空数据，使用模拟数据')
          useMockData(page)
        }
      } catch (error) {
        console.error('加载质量报告失败:', error)
        console.error('错误详情:', error.response ? error.response.data : error.message)
        useMockData(page)
        ElMessage.warning('使用模拟数据进行展示')
      } finally {
        loading.value = false
      }
    }
    // 使用模拟数据的辅助方法
    const useMockData = (page) => {
      usingMockData.value = true
      const mockData = generateMockReports()
      const startIndex = (page - 1) * pagination.value.pageSize
      const endIndex = startIndex + pagination.value.pageSize

      qualityReports.value = mockData.slice(startIndex, endIndex)
      pagination.value.total = mockData.length
      pagination.value.currentPage = page

      // 默认选择第一个报告
      if (qualityReports.value.length > 0 && !selectedReport.value) {
        selectedReport.value = qualityReports.value[0]
        nextTick(() => {
          renderChart()
        })
      }
    }

    // 生成模拟数据
    const generateMockReports = () => {
      const reports = []
      for (let i = 1; i <= 15; i++) {
        reports.push({
          id: i,
          taskId: i,
          taskName: `ETL_PROCESS_20250909_14023${i}`,
          uploadDate: '20250909',
          totalRecords: 467455,
          validRecords: 467455,
          nullCommentCount: 8,
          priceErrorCount: 0,
          coreFieldMissingCount: 0,
          ratingAnomalyCount: 0,
          qualityScore: 100,
          coreFieldScore: 100,
          contentScore: 100,
          priceScore: 100,
          ratingScore: 100,
          merchantCount: 11564,
          avgRating: 4.22,
          positiveRate: 80.53,
          createTime: new Date().toISOString()
        })
      }
      return reports
    }

    // 安全格式化评分
    const formatRating = (rating) => {
      if (rating === null || rating === undefined) return '0.00'
      const num = typeof rating === 'string' ? parseFloat(rating) : rating
      return isNaN(num) ? '0.00' : num.toFixed(2)
    }

    // 安全格式化正面评价率
    const formatPositiveRate = (rate) => {
      if (rate === null || rate === undefined) return '0.00'
      const num = typeof rate === 'string' ? parseFloat(rate) : rate
      return isNaN(num) ? '0.00' : num.toFixed(2)
    }


    const handleTaskSelect = (report) => {
      if (report) {
        selectedReport.value = report
        nextTick(() => {
          renderChart()
        })
      }
    }

    const handlePageChange = (page) => {
      loadQualityReports(page)
    }

    const refreshReports = () => {
      loadQualityReports(pagination.value.currentPage)
      ElMessage.success('数据已刷新')
    }

    const renderChart = () => {
      if (!selectedReport.value) return

      const chartDom = document.getElementById('qualityChart')
      if (!chartDom) return

      if (chart.value) {
        chart.value.dispose()
      }

      chart.value = echarts.init(chartDom)
      const report = selectedReport.value
      const validRecords = report.validRecords || 0
      const coreFieldMissing = report.coreFieldMissingCount || 0
      const ratingAnomaly = report.ratingAnomalyCount || 0
      const otherErrors = (report.nullCommentCount || 0) + (report.priceErrorCount || 0)
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: 0,
          data: ['有效数据', '核心字段缺失', '评分异常', '其他异常']
        },
        series: [
          {
            name: '数据分布',
            type: 'pie',
            radius: ['50%', '70%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              {
                value: validRecords,
                name: '有效数据',
                itemStyle: { color: '#92d5a9' }
              },
              {
                value: coreFieldMissing,
                name: '核心字段缺失',
                itemStyle: { color: '#ffad33' }
              },
              {
                value: ratingAnomaly,
                name: '评分异常',
                itemStyle: { color: '#e18fad' }
              },
              {
                value: otherErrors,
                name: '其他异常',
                itemStyle: { color: '#909399' }
              }
            ]
          }
        ]
      }

      chart.value.setOption(option)
    }

    const getQualityColor = (score) => {
      if (!score) return '#ffad33'
      if (score >= 90) return '#92d5a9'
      if (score >= 70) return 'rgba(255,214,51,0.92)'
      return '#e18fad'
    }

    const getScoreType = (score) => {
      if (!score) return 'info'
      if (score >= 90) return 'success'
      if (score >= 70) return 'warning'
      return 'danger'
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
      loadQualityReports(1)

      window.addEventListener('resize', () => {
        if (chart.value) {
          chart.value.resize()
        }
      })
    })

    return {
      qualityReports,
      selectedReport,
      pagination,
      loading,
      usingMockData,
      loadQualityReports,
      handleTaskSelect,
      handlePageChange,
      refreshReports,
      getQualityColor,
      getScoreType,
      formatDate,
      formatRating,
      formatPositiveRate
    }
  }
}
</script>

<style scoped>
.quality-report-view {
  padding: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-3 {
  margin-top: 12px;
}

.mb-3 {
  margin-bottom: 12px;
}

.mt-4 {
  margin-top: 20px;
}

.percentage-value {
  font-size: 18px;
  font-weight: bold;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state .el-icon {
  margin-bottom: 16px;
}

.task-list-card {
  height: 750px;
  display: flex;
  flex-direction: column;
}

.quality-score-card {
  flex: 1;
  margin-top: 12px;
}

.report-detail-card {
  height: 750px;
  overflow-y: auto;
}

.empty-card {
  height: 750px;
}

.chart-card {
  height: 700px;
}

.detail-card {
  height: 350px;
}

.score-container {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 180px;
}

.score-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.score-item .label {
  font-size: 12px;
  color: #606266;
}

.score-item .value {
  font-weight: bold;
  color: #409EFF;
}

.score-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.score-value {
  min-width: 40px;
  font-weight: bold;
  color: #409EFF;
  text-align: right;
}

:deep(.el-progress--dashboard) {
  width: 100px;
}

:deep(.el-descriptions__body) {
  padding: 12px;
}

:deep(.el-descriptions__label) {
  width: 100px;
}

:deep(.el-progress-bar) {
  flex: 1;
}

/* 确保卡片高度一致 */
:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>