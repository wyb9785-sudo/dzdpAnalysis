<template>
  <div class="merchant-comparison">
    <div v-if="!comparisonData || Object.keys(comparisonData).length === 0" class="no-data">
      <el-empty description="暂无对比数据" />
    </div>

    <div v-else>
      <!-- 评分折线图 -->
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="comparison-card">
            <template #header>
              <h3>评分对比 - 折线图</h3>
            </template>
            <div ref="lineChart" style="height: 400px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 评论柱状图 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <el-card class="comparison-card">
            <template #header>
              <h3>评论数量对比 - 柱状图</h3>
            </template>
            <div ref="barChart" style="height: 400px;"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 详细数据表格 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="12">
          <el-card class="comparison-card">
            <template #header>
              <h3>评分对比</h3>
            </template>
            <el-table :data="ratingTable" style="width: 100%">
              <el-table-column prop="metric" label="评分类型" width="120" />
              <el-table-column
                  v-for="merchant in merchants"
                  :key="merchant.merchantName"
                  :label="merchant.merchantName"
                  width="150"
              >
                <template #default="{ row }">
                  {{ row[merchant.merchantName] || '无数据' }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card class="comparison-card">
            <template #header>
              <h3>评论统计</h3>
            </template>
            <el-table :data="reviewTable" style="width: 100%">
              <el-table-column prop="metric" label="统计类型" width="120" />
              <el-table-column
                  v-for="merchant in merchants"
                  :key="merchant.merchantName"
                  :label="merchant.merchantName"
                  width="150"
              >
                <template #default="{ row }">
                  {{ row[merchant.merchantName] || '无数据' }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>


<script>
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'MerchantComparison',
  props: {
    comparisonData: {
      type: Object,
      default: () => ({})
    },
    merchants: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    const lineChart = ref(null)
    const barChart = ref(null)
    let lineInstance = null
    let barInstance = null
    // 添加监听器，当数据变化时重新初始化图表
    watch(() => props.comparisonData, (newVal) => {
      if (newVal && Object.keys(newVal).length > 0) {
        nextTick(() => {
          initCharts()
        })
      }
    }, { deep: true, immediate: true })
    const initCharts = () => {
      // 销毁现有实例
      if (lineInstance) {
        lineInstance.dispose()
        lineInstance = null
      }
      if (barInstance) {
        barInstance.dispose()
        barInstance = null
      }

      // 确保DOM元素存在且有尺寸
      if (lineChart.value && lineChart.value.clientWidth > 0 && lineChart.value.clientHeight > 0) {
        lineInstance = echarts.init(lineChart.value)
        updateLineChart()
      }

      if (barChart.value && barChart.value.clientWidth > 0 && barChart.value.clientHeight > 0) {
        barInstance = echarts.init(barChart.value)
        updateBarChart()
      }
    }

    // 添加resize监听
    onMounted(() => {
      // 延迟初始化以确保DOM渲染完成
      setTimeout(initCharts, 300)
      window.addEventListener('resize', handleResize)
    })

    const handleResize = () => {
      if (lineInstance) lineInstance.resize()
      if (barInstance) barInstance.resize()
    }
    // 更新折线图
    const updateLineChart = () => {
      if (!lineInstance || !props.comparisonData.ratings || Object.keys(props.comparisonData.ratings).length === 0) return

      const categories = ['综合评分', '口味评分', '服务评分', '环境评分']
      const seriesData = props.merchants.map(merchant => {
        const ratings = props.comparisonData.ratings[merchant.merchantName] || {}
        return {
          name: merchant.merchantName,
          type: 'line',
          data: [
            ratings.avgRating || 0,
            ratings.tasteScore || 0,
            ratings.serviceScore || 0,
            ratings.environmentScore || 0
          ],
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: {
            width: 3
          }
        }
      })

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#ebeef5',
          textStyle: {
            color: '#606266'
          }
        },
        legend: {
          data: props.merchants.map(m => m.merchantName),
          right: 10,
          top: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLine: {
            lineStyle: {
              color: '#616365'
            }
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 5,
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          splitLine: {
            lineStyle: {
              color: '#f0f2f5'
            }
          }
        },
        series: seriesData
      }

      lineInstance.setOption(option)
    }

    // 更新柱状图
    const updateBarChart = () => {
      if (!barInstance || !props.comparisonData.reviewStats || Object.keys(props.comparisonData.reviewStats).length === 0) return

      const categories = props.merchants.map(m => m.merchantName)
      const seriesData = [
        {
          name: '积极评论',
          type: 'bar',
          data: props.merchants.map(merchant =>
              props.comparisonData.reviewStats[merchant.merchantName]?.positiveReviews || 0
          ),
          itemStyle: {
            color: '#93d7aa',
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '25%'
        },
        {
          name: '消极评论',
          type: 'bar',
          data: props.merchants.map(merchant =>
              props.comparisonData.reviewStats[merchant.merchantName]?.negativeReviews || 0
          ),
          itemStyle: {
            color: '#e18fad',
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '25%'
        },
        {
          name: '中性评论',
          type: 'bar',
          data: props.merchants.map(merchant =>
              props.comparisonData.reviewStats[merchant.merchantName]?.neutralReviews || 0
          ),
          itemStyle: {
            color: '#909399',
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '25%'
        }
      ]

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)'
        },
        legend: {
          data: ['积极评论', '消极评论', '中性评论'],
          top: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLine: {
            lineStyle: {
              color: '#616365'
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          splitLine: {
            lineStyle: {
              color: '#f0f2f5'
            }
          }
        },
        series: seriesData
      }

      barInstance.setOption(option)
    }

    // 监听数据变化
    watch(() => props.comparisonData, () => {
      if (lineInstance) updateLineChart()
      if (barInstance) updateBarChart()
    }, { deep: true })

    onMounted(() => {
      initCharts()
    })

    // 计算属性
    const basicInfoTable = computed(() => {
      if (!props.comparisonData.basicInfo || props.merchants.length === 0) {
        return []
      }

      return [
        {
          metric: '商户名称',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.basicInfo[merchant.merchantName]?.name || '无数据'
          }), {})
        }
      ]
    })

    // 计算属性保持不变...
    const ratingTable = computed(() => {
      if (!props.comparisonData.ratings || props.merchants.length === 0) {
        return []
      }

      return [
        {
          metric: '综合评分',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.ratings[merchant.merchantName]?.avgRating?.toFixed(1) || '无数据'
          }), {})
        },
        {
          metric: '口味评分',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.ratings[merchant.merchantName]?.tasteScore?.toFixed(1) || '无数据'
          }), {})
        },
        {
          metric: '服务评分',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.ratings[merchant.merchantName]?.serviceScore?.toFixed(1) || '无数据'
          }), {})
        },
        {
          metric: '环境评分',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.ratings[merchant.merchantName]?.environmentScore?.toFixed(1) || '无数据'
          }), {})
        }
      ]
    })

    const reviewTable = computed(() => {
      if (!props.comparisonData.reviewStats || props.merchants.length === 0) {
        return []
      }

      return [
        {
          metric: '总评论数',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.reviewStats[merchant.merchantName]?.totalReviews || '无数据'
          }), {})
        },
        {
          metric: '积极评论',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.reviewStats[merchant.merchantName]?.positiveReviews || '无数据'
          }), {})
        },
        {
          metric: '消极评论',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.reviewStats[merchant.merchantName]?.negativeReviews || '无数据'
          }), {})
        },
        {
          metric: '中性评论',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.reviewStats[merchant.merchantName]?.neutralReviews || '无数据'
          }), {})
        },
        {
          metric: '好评率',
          ...props.merchants.reduce((acc, merchant) => ({
            ...acc,
            [merchant.merchantName]: props.comparisonData.reviewStats[merchant.merchantName]?.positiveRate?.toFixed(1) + '%' || '无数据'
          }), {})
        }
      ]
    })

    return {
      lineChart,
      barChart,
      ratingTable,
      reviewTable
    }
  }
}
</script>

<style scoped>
.comparison-card {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
}

.no-data {
  text-align: center;
  padding: 60px 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-bottom: 1px solid #e9ecef;
  font-weight: 600;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #f8f9fa !important;
  font-weight: 600;
}
</style>