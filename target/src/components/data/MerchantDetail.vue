<template>
  <div class="merchant-detail">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <h3>基本信息</h3>
          </template>
          <div class="detail-item">
            <label>商户名称：</label>
            <span>{{ merchant.merchantName }}</span>
          </div>
          <div class="detail-item">
            <label>平均评分：</label>
            <el-rate :model-value="merchant.avgRating || 0" disabled show-score text-color="#ff9900" />
          </div>
          <div class="detail-item">
            <label>人均价格：</label>
            <span>¥{{ (merchant.avgPrice || 0)?.toFixed(2) }}</span>
          </div>
          <div class="detail-item">
            <label>好评率：</label>
            <span>{{ (merchant.positiveRate || 0).toFixed(1) }}%</span>
          </div>
          <div class="detail-item">
            <label>总评论数：</label>
            <span>{{ merchant.totalReviews || 0 }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <h3>评分详情</h3>
          </template>
          <div ref="scoreChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <h3>评论统计 - 玫瑰图</h3>
          </template>
          <div ref="reviewChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>


<script>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'MerchantDetail',
  props: {
    merchant: {
      type: Object,
      required:true
    }
  },
  setup(props) {
    const scoreChart = ref(null)
    const reviewChart = ref(null)
    let scoreInstance = null
    let reviewInstance = null

    // 添加监听器，当对话框显示时初始化图表
    watch(() => props.merchant, (newVal) => {
      if (newVal) {
        // 使用nextTick确保DOM已更新
        nextTick(() => {
          initCharts()
        })
      }
    }, { immediate: true })

    const initCharts = () => {

      // 销毁现有实例
      if (scoreInstance) {
        scoreInstance.dispose()
        scoreInstance = null
      }
      if (reviewInstance) {
        reviewInstance.dispose()
        reviewInstance = null
      }

      if (scoreChart.value && scoreChart.value.clientWidth > 0) {
        scoreInstance = echarts.init(scoreChart.value)
        updateScoreChart()
      }

      if (reviewChart.value && reviewChart.value.clientWidth > 0) {
        reviewInstance = echarts.init(reviewChart.value)
        updateReviewChart()
      }
    }
    // 添加resize监听
    onMounted(() => {
      window.addEventListener('resize', handleResize)
    })
    const handleResize = () => {
      if (scoreInstance) {
        scoreInstance.resize()
      }
      if (reviewInstance) {
        reviewInstance.resize()
      }
    }
    //评分详情柱状图
    const updateScoreChart = () => {
      if (!scoreInstance) return

      const option = {
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: ['综合评分', '口味', '服务', '环境']
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 5
        },
        series: [{
          type: 'bar',
          data: [
            props.merchant.avgRating || 0,
            props.merchant.tasteScore || 0,
            props.merchant.serviceScore || 0,
            props.merchant.environmentScore || 0
          ],
          itemStyle: {
            color: '#56adb0',
            barWidth: '30%' // 调整柱状图宽度
          }
        }]
      }

      scoreInstance.setOption(option)
    }

    const updateReviewChart = () => {
      if (!reviewInstance) return

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [{
          name: '评论统计',
          type: 'pie',
          radius: ['10%', '70%'],
          center: ['40%', '50%'],
          roseType: 'area',
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}: {c} ({d}%)'
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgb(225,143,173)'
            }
          },
          data: [
            {
              value: props.merchant.positiveReviews || 0,
              name: '积极评论',
              itemStyle: { color: '#93d7aa' }
            },
            {
              value: props.merchant.negativeReviews || 0,
              name: '消极评论',
              itemStyle: { color: '#b6bab8' }
            },
            {
              value: props.merchant.neutralReviews || 0,
              name: '中性评论',
              itemStyle: { color: 'rgba(122,189,216,0.68)' }
            }
          ]
        }]
      }

      reviewInstance.setOption(option)
    }

    onMounted(() => {
      initCharts()
    })

    watch(() => props.merchant, () => {
      initCharts()
    }, { deep: true })

    return {
      scoreChart,
      reviewChart
    }
  }
}
</script>

<style scoped>
.merchant-detail {
  padding: 10px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.detail-item label {
  font-weight: bold;
  min-width: 80px;
  color: #606266;
}

.detail-item span {
  color: #303133;
}
</style>


