<template>
    <div class="merchant-dashboard">
      <!-- 顶部信息栏 -->
      <div class="dashboard-header">
        <div class="merchant-info">
          <h1>{{ merchantName }} - 商户口碑分析中心</h1>
          <p>最后更新: {{ lastUpdateTime }}</p>
        </div>
        <div class="header-actions">
          <el-button @click="refreshData" :loading="loading">刷新数据</el-button>
          <el-button @click="logout" type="danger">登出系统</el-button>
        </div>
      </div>

      <!-- 第一行：个人分析 -->
      <div class="analysis-row personal-analysis">
        <!-- 雷达图 -->
        <el-card class="chart-card radar-card">
          <template #header>
            <div class="chart-header">
              <span>个人商户口碑分析（近7天）</span>
              <el-tooltip
                  content="数据归一化说明：评分类数据(1-5分)转换为0-100分，好评率(0-100%)保持不变"
                  placement="top"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div v-if="hasRadarData" id="radar-chart" style="width: 100%; height: 300px;"></div>
          <div v-else class="no-data">暂无雷达图数据</div>
        </el-card>

        <!-- 词云图 -->
        <el-card class="chart-card wordcloud-card">
          <template #header>
            <div class="chart-header">
              <span>热门菜品TOP10</span>
              <el-tooltip
                  content="根据用户的提及次数显示，文字大代表该菜品更受欢迎"
                  placement="top"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div v-if="popularFoods.length > 0" id="wordcloud-chart" style="width: 100%; height: 300px;"></div>
          <div v-else class="no-data">暂无热门菜品数据</div>
        </el-card>
      </div>

      <!-- 第二行：气泡图 -->
      <div class="analysis-row">
        <el-card class="chart-card full-width">
          <template #header>
            <div class="chart-header">
              <span>集体商户对比分析（{{ bubbleData.length }}家商户）</span>
              <el-tooltip
                  content="气泡大小由评分决定"
                  placement="top"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div v-if="bubbleData.length > 0" id="bubble-chart" style="width: 100%; height: 400px;"></div>
          <div v-else class="no-data">暂无数据</div>
        </el-card>
      </div>

      <!-- 第三行：堆叠条形图 -->
      <div class="analysis-row">
        <el-card class="chart-card full-width">
          <template #header>
            <div class="chart-header">
              <span>商户情感分析对比-堆叠条形图</span>
              <el-tooltip
                  content="显示top30的商户情感分析对比，包括积极、中性和消极评论数量"
                  placement="top"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div v-if="sentimentData.length > 0" id="stacked-bar-chart" style="width: 100%; height: 800px;"></div>
          <div v-else class="no-data">暂无数据</div>
        </el-card>
      </div>

      <!-- 第四行：玫瑰图（极坐标堆叠柱状图） -->
      <div class="analysis-row">
        <el-card class="chart-card full-width">
          <template #header>
            <span>商户情感分布-玫瑰图</span>
            <el-tooltip
                content="显示top30的商户的情感评论分布，包括积极、中性和消极评论数量"
                placement="top"
            >
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <div v-if="roseChartData.length > 0" id="rose-chart" style="width: 100%; height: 600px;"></div>
          <div v-else class="no-data">暂无数据</div>
        </el-card>
      </div>
    </div>

</template>

<script>
import { onMounted, ref, computed, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantAnalysisApi } from '../api/index.js'
import { InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'MerchantAnalysisView',

  setup() {
    const router = useRouter()
    const loading = ref(false)
    const merchantName = ref('')
    const lastUpdateTime = ref('')

    const radarData = ref({})
    const bubbleData = ref([])
    const sentimentData = ref([])
    const roseChartData = ref([])
    const popularFoods = ref([])
    const chartInstances = ref([])
    // 计算是否有雷达图数据
    const hasRadarData = computed(() => {
      return radarData.value && Object.keys(radarData.value).length > 0 &&
          radarData.value.avgRating7d !== undefined;
    })
    // 格式化商户名称显示
    const formatMerchantName = (name) => {
      if (!name) return ''
      return name.length > 10 ? name.substring(0, 8) + '...' : name
    }

    // 创建模拟仪表板数据
    const createMockDashboardData = (merchantName) => {
      return {
        merchantName: merchantName,
        avgRating7d: 4.5,
        avgTaste7d: 4.3,
        avgService7d: 4.2,
        avgEnvironment7d: 4.4,
        positiveRate7d: 85.5
      }
    }

    // 创建模拟菜品数据
    const createMockFoodsData = (merchantName) => {
      return [
        { keyword: "猪肚鸡", mentionCount: 120 },
        { keyword: "椰子鸡", mentionCount: 98 },
        { keyword: "煲仔饭", mentionCount: 76 },
        { keyword: "烤鱼", mentionCount: 65 },
        { keyword: "奶茶", mentionCount: 54 },
        { keyword: "烧鹅", mentionCount: 43 },
        { keyword: "叉烧", mentionCount: 32 },
        { keyword: "肠粉", mentionCount: 28 },
        { keyword: "虾饺", mentionCount: 25 },
        { keyword: "炒牛河", mentionCount: 22 }
      ]
    }

    // 创建模拟气泡图数据
    const createMockBubbleData = () => {
      return [
        { merchantName: "商户1", totalReviews: 1000, positiveRate: 85.5, avgRating: 4.5 },
        { merchantName: "商户2", totalReviews: 800, positiveRate: 82.3, avgRating: 4.3 },
        { merchantName: "商户3", totalReviews: 1200, positiveRate: 88.7, avgRating: 4.7 },
        { merchantName: "商户4", totalReviews: 600, positiveRate: 79.2, avgRating: 4.1 },
        { merchantName: "商户5", totalReviews: 1500, positiveRate: 90.1, avgRating: 4.8 }
      ]
    }

    // 创建模拟情感趋势数据
    const createMockSentimentData = () => {
      return [
        {
          merchantName: merchantName.value,
          positiveCount: 120,
          negativeCount: 15,
          neutralCount: 35,
          totalCount: 170
        }
      ]
    }

    // 创建模拟玫瑰图数据
    const createMockRoseData = () => {
      return [
        { merchant: "聚汇坊", positive: 120, negative: 15, neutral: 35 },
        { merchant: "海底捞", positive: 200, negative: 20, neutral: 50 },
        { merchant: "麦当劳", positive: 150, negative: 25, neutral: 45 },
        { merchant: "肯德基", positive: 180, negative: 18, neutral: 42 },
        { merchant: "星巴克", positive: 90, negative: 10, neutral: 30 }
      ]
    }

    // 刷新数据
    const refreshData = async () => {
      loading.value = true
      try {
        // 清理现有图表
        chartInstances.value.forEach(chart => chart.dispose())
        chartInstances.value = []

        // 重新获取数据
        await fetchCurrentMerchantData()
        await fetchCollectiveData()
        ElMessage.success('数据刷新成功')
      } catch (error) {
        ElMessage.error('数据刷新失败')
      } finally {
        loading.value = false
      }
    }

    // 登出系统
    const logout = async () => {
      try {
        await ElMessageBox.confirm('确定要退出系统吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 清除本地存储
        localStorage.removeItem('token')
        localStorage.removeItem('user')

        // 跳转到登录页
        router.push('/login')
        ElMessage.success('已成功退出系统')
      } catch (error) {
        // 用户取消登出
      }
    }
    // 获取当前商户数据
    const fetchCurrentMerchantData = async () => {
      try {
        loading.value = true;

        const userStr = localStorage.getItem('user');
        let currentMerchantName = '当前商户';

        if (userStr) {
          try {
            const user = JSON.parse(userStr);
            currentMerchantName = user.merchantName || user.username || '当前商户';
            merchantName.value = currentMerchantName;
            console.log('当前商户名:', currentMerchantName);
          } catch (e) {
            console.error('解析用户信息失败:', e);
          }
        }

        const [dashboardResponse, foodsResponse] = await Promise.allSettled([
          merchantAnalysisApi.getDashboardData(),
          merchantAnalysisApi.getFoods(10)
        ]);

        console.log('API响应详情:', {
          dashboard: dashboardResponse,
          foods: foodsResponse
        });

        // 处理仪表板数据 - 更严格的检查
        if (dashboardResponse.status === 'fulfilled' && dashboardResponse.value) {
          const data = dashboardResponse.value;
          console.log('仪表板原始数据详情:', JSON.stringify(data, null, 2));

          // 检查数据是否有效（至少有一个评分字段）
          const hasValidData = data && Object.keys(data).length > 0 &&
              (data.avgRating !== undefined || data.avgRating!== undefined ||
                  data.avgTaste7d !== undefined || data.positiveRate7d !== undefined);

          if (hasValidData) {
            console.log('✅ 仪表板数据有效');
            radarData.value = data;
            if (data.merchantName) {
              merchantName.value = data.merchantName;
            }
            nextTick(() => initRadarChart());
          } else {
            console.warn('❌ 仪表板数据无效，使用模拟数据');
            radarData.value = createMockDashboardData(currentMerchantName);
            nextTick(() => initRadarChart());
          }
        } else {
          console.warn('仪表板数据获取失败，使用模拟数据');
          radarData.value = createMockDashboardData(currentMerchantName);
          nextTick(() => initRadarChart());
        }

        // 处理菜品数据
        if (foodsResponse.status === 'fulfilled') {
          if (Array.isArray(foodsResponse.value) && foodsResponse.value.length > 0) {
            popularFoods.value = foodsResponse.value.filter(item =>
                item.keyword && item.keyword !== '无' && item.keyword !== '无关键词'
            );
            console.log('获取到菜品数据:', popularFoods.value.length, '条');
          } else {
            console.warn('菜品数据为空数组，使用模拟数据');
            popularFoods.value = createMockFoodsData(currentMerchantName);
          }
          nextTick(() => initWordcloudChart());
        } else {
          console.warn('菜品数据获取失败，使用模拟数据');
          popularFoods.value = createMockFoodsData(currentMerchantName);
          nextTick(() => initWordcloudChart());
        }

      } catch (error) {
        console.error('获取当前商户数据失败:', error);
        const userStr = localStorage.getItem('user');
        const currentMerchantName = userStr ? (JSON.parse(userStr).merchantName || JSON.parse(userStr).username) : '当前商户';

        merchantName.value = currentMerchantName;
        radarData.value = createMockDashboardData(currentMerchantName);
        popularFoods.value = createMockFoodsData(currentMerchantName);

        nextTick(() => {
          initRadarChart();
          initWordcloudChart();
        });
      }
    };

    const fetchCollectiveData = async () => {
      try {
        const [bubbleResponse, sentimentResponse, roseResponse, allSentimentResponse] = await Promise.allSettled([
          merchantAnalysisApi.getBubbleData(),
          merchantAnalysisApi.getSentimentTrend(), // 当前商户的情感趋势
          merchantAnalysisApi.getRoseChart(),
          merchantAnalysisApi.getAllSentimentData() // 所有商户的情感数据（堆叠图用）
        ]);

        // 处理气泡图数据
        if (bubbleResponse.status === 'fulfilled' && Array.isArray(bubbleResponse.value)) {
          bubbleData.value = bubbleResponse.value;
          console.log('气泡图数据:', bubbleData.value.length, '条');
        } else {
          console.warn('气泡图数据获取失败，使用模拟数据');
          bubbleData.value = createMockBubbleData();
        }

        // 处理堆叠图数据（使用所有商户的情感数据）
        if (allSentimentResponse.status === 'fulfilled' && Array.isArray(allSentimentResponse.value)) {
          if (allSentimentResponse.value.length === 0) {
            console.warn('所有商户情感数据为空，使用玫瑰图数据');
            // 如果没有情感数据，可以使用玫瑰图数据转换
            sentimentData.value = convertRoseToSentimentData(roseResponse.value || []);
          } else {
            sentimentData.value = allSentimentResponse.value;
            console.log('堆叠图数据（所有商户）:', sentimentData.value.length, '条');
          }
        } else {
          console.warn('所有商户情感数据获取失败，使用玫瑰图数据');
          sentimentData.value = convertRoseToSentimentData(roseResponse.value || []);
        }

        // 处理玫瑰图数据
        if (roseResponse.status === 'fulfilled' && Array.isArray(roseResponse.value)) {
          roseChartData.value = roseResponse.value;
          console.log('玫瑰图数据:', roseChartData.value.length, '条');
        } else {
          console.warn('玫瑰图数据获取失败，使用模拟数据');
          roseChartData.value = createMockRoseData();
        }

        // 延迟初始化图表
        nextTick(() => {
          setTimeout(() => {
            initBubbleChart();
            initStackedBarChart();
            initRoseChart();
          }, 100);
        });

      } catch (error) {
        console.error('获取集体数据失败:', error);
        ElMessage.error('获取集体数据失败');
      } finally {
        loading.value = false;
      }
    }
    // 将玫瑰图数据转换为情感数据格式
    const convertRoseToSentimentData = (roseData) => {
      return roseData.map(item => ({
        merchantName: item.merchantName,
        positiveCount: item.positiveCount || 0,
        negativeCount: item.negativeCount || 0,
        neutralCount: item.neutralCount || 0,
        totalCount: (item.positiveCount || 0) + (item.negativeCount || 0) + (item.neutralCount || 0)
      }));
    };

// 创建所有商户的模拟情感数据
    const createMockAllSentimentData = () => {
      const mockData = [];
      const merchants = ['商户A', '商户B', '商户C', '商户D', '商户E', '商户F', '商户G'];

      merchants.forEach(merchant => {
        mockData.push({
          merchantName: merchant,
          positiveCount: Math.floor(Math.random() * 200) + 50,
          negativeCount: Math.floor(Math.random() * 50) + 5,
          neutralCount: Math.floor(Math.random() * 100) + 20,
          totalCount: 0 // 会在计算后设置
        });

        const lastItem = mockData[mockData.length - 1];
        lastItem.totalCount = lastItem.positiveCount + lastItem.negativeCount + lastItem.neutralCount;
      });

      return mockData;
    };

    // 初始化雷达图
    const initRadarChart = () => {
      try {
        const dom = document.getElementById('radar-chart');
        if (!dom || !hasRadarData.value) {
          console.log('雷达图DOM元素或数据未就绪');
          return;
        }

        const existingChart = echarts.getInstanceByDom(dom);
        if (existingChart) {
          existingChart.dispose();
        }

        const chart = echarts.init(dom);
        chartInstances.value.push(chart);

        /**
         * 数据归一化处理
         * 评分类数据（1-5分）归一化公式: (原始分数 - 1) / 4 * 100
         * 好评率数据（0-100）保持不变
         */
        const normalizeScore = (score) => {
          if (score === undefined || score === null) return 50; // 默认值
          return Number((((score - 1) / 4) * 100).toFixed(1)); // 保留一位小数
        };

        const rating = normalizeScore(radarData.value.avgRating7d);
        const taste = normalizeScore(radarData.value.avgTaste7d);
        const service = normalizeScore(radarData.value.avgService7d);
        const environment = normalizeScore(radarData.value.avgEnvironment7d);
        const positiveRate = radarData.value.positiveRate7d ?
            Number(radarData.value.positiveRate7d.toFixed(1)) : 70;

        console.log('雷达图归一化数据:', {
          rating, taste, service, environment, positiveRate
        });

        const option = {
          title: {
            subtextStyle: {
              fontSize: 12,
              color: '#666'
            }
          },
          tooltip: {
            trigger: 'item',
          //   formatter: function(params) {
          //     return `
          //   <b>${params.name}</b><br/>
          //   得分: ${params.value}分<br/>
          //   <small>${getNormalizationDescription(params.name)}</small>
          // `;
          //   }
          },
          radar: {
            indicator: [
              { name: '综合评分', max: 100 },
              { name: '口味', max: 100 },
              { name: '服务', max: 100 },
              { name: '环境', max: 100 },
              { name: '好评率', max: 100 }
            ],
            splitArea: {
              show: true,
              areaStyle: {
                color: ['rgba(255, 255, 255, 0.1)', 'rgba(255, 255, 255, 0.05)']
              }
            }
          },
          series: [{
            type: 'radar',
            data: [{
              value: [rating, taste, service, environment, positiveRate],
              name: merchantName.value,
              areaStyle: {
                color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                  { offset: 0, color: 'rgba(146, 213, 169, 0.6)' },
                  { offset: 1, color: 'rgba(146, 213, 169, 0.1)' }
                ])
              },
              lineStyle: {
                width: 2,
                color: '#92d5a9'
              },
              itemStyle: {
                color: '#92d5a9'
              }
            }]
          }]
        };

        chart.setOption(option);

        // 添加归一化说明的辅助函数
        function getNormalizationDescription(dimension) {
          const descriptions = {
            '综合评分': '1-5分归一化到0-100分：(原始分-1)/4×100',
            '口味': '1-5分归一化到0-100分：(原始分-1)/4×100',
            '服务': '1-5分归一化到0-100分：(原始分-1)/4×100',
            '环境': '1-5分归一化到0-100分：(原始分-1)/4×100',
            '好评率': '0-100%直接显示，无需归一化'
          };
          return descriptions[dimension] || '数据维度';
        }

      } catch (error) {
        console.error('初始化雷达图失败:', error);
      }
    };
    // 初始化气泡图
    const initBubbleChart = () => {
      try {
        const dom = document.getElementById('bubble-chart');
        if (!dom || bubbleData.value.length === 0) return;

        const existingChart = echarts.getInstanceByDom(dom);
        if (existingChart) {
          existingChart.dispose();
        }

        const chart = echarts.init(dom);
        chartInstances.value.push(chart);

        const data = bubbleData.value.map(item => ({
          name: item.merchantName,
          value: [item.totalReviews || 0, item.positiveRate || 0, item.avgRating || 0],
        }));

        const option = {
          tooltip: {
            formatter: function (params) {
              return `${params.data.name}<br/>评论数: ${params.data.value[0].toFixed(1)}<br/>好评率: ${params.data.value[1].toFixed(1)}%<br/>评分: ${params.data.value[2]}`
            }
          },
          xAxis: { type: 'value', name: '评论数' },
          yAxis: { type: 'value', name: '好评率(%)', min: 0, max: 100 },
          series: [{
            type: 'scatter',
            symbolSize: function (data) {
              const size = Math.sqrt(data[2]) / 20;
              return Math.min(Math.max(size, 15), 80);
            },
            itemStyle: {
              color: function(params) {
                const colors = ['#92d5a9', '#e18fad', '#56adb0', '#ffad33', '#F56C6C'];
                return colors[params.dataIndex % colors.length];
              }
            },
            data: data
          }]
        };

        chart.setOption(option);
      } catch (error) {
        console.error('初始化气泡图失败:', error);
      }
    }

    // 初始化堆叠条形图
    const initStackedBarChart = () => {
      try {
        const dom = document.getElementById('stacked-bar-chart');
        if (!dom || sentimentData.value.length === 0) {
          console.log('堆叠图数据为空或DOM未就绪');
          return;
        }

        const existingChart = echarts.getInstanceByDom(dom);
        if (existingChart) {
          existingChart.dispose();
        }

        const chart = echarts.init(dom);
        chartInstances.value.push(chart);

        console.log('堆叠图数据（所有商户）:', sentimentData.value);

        // 只显示前30家商户避免过度拥挤
        const displayData = sentimentData.value.slice(0, 30);
        const merchants = displayData.map(item => formatMerchantName(item.merchantName));
        const positiveData = displayData.map(item => item.positiveCount || 0);
        const negativeData = displayData.map(item => item.negativeCount || 0);
        const neutralData = displayData.map(item => item.neutralCount || 0);

        const option = {
          title: {
            text: '商户情感分析对比（Top 30）',
            left: 'center',
            textStyle: {
              fontSize: 16,
              fontWeight: 'bold'
            }
          },
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: function(params) {
              const dataIndex = params[0].dataIndex;
              const total = positiveData[dataIndex] + negativeData[dataIndex] + neutralData[dataIndex];
              let result = `<b>${merchants[dataIndex]}</b><br/>`;

              params.forEach(param => {
                const percentage = total > 0 ? ((param.value / total) * 100).toFixed(1) : 0;
                result += `${param.seriesName}: ${param.value} (${percentage}%)<br/>`;
              });

              result += `<b>总计: ${total}</b>`;
              return result;
            }
          },
          legend: {
            data: ['积极评论', '中性评论', '消极评论'],
            bottom: 10
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            top: '15%',
            containLabel: true
          },
          xAxis: {
            type: 'value',
            name: '评论数量'
          },
          yAxis: {
            type: 'category',
            data: merchants,
            axisLabel: {
              interval: 0,
              rotate: 30,
              fontSize: 10,
              margin: 8
            }
          },
          series: [
            {
              name: '积极评论',
              type: 'bar',
              stack: 'total',
              emphasis: { focus: 'series' },
              itemStyle: { color: '#92d5a9' },
              data: positiveData
            },
            {
              name: '中性评论',
              type: 'bar',
              stack: 'total',
              emphasis: { focus: 'series' },
              itemStyle: { color: '#56adb0' },
              data: neutralData
            },
            {
              name: '消极评论',
              type: 'bar',
              stack: 'total',
              emphasis: { focus: 'series' },
              itemStyle: { color: '#e18fad' },
              data: negativeData
            }
          ]
        };

        chart.setOption(option);
        console.log('堆叠图初始化完成');

      } catch (error) {
        console.error('初始化堆叠条形图失败:', error);
      }
    }

    // 初始化词云图
    const initWordcloudChart = () => {
      try {
        const dom = document.getElementById('wordcloud-chart');
        if (!dom || popularFoods.value.length === 0) return;

        const existingChart = echarts.getInstanceByDom(dom);
        if (existingChart) {
          existingChart.dispose();
        }

        const chart = echarts.init(dom);
        chartInstances.value.push(chart);

        const wordcloudData = popularFoods.value.map(item => ({
          name: item.keyword,
          value: item.mentionCount || 1
        }));

        const option = {
          tooltip: {
            show: true,
            formatter: function (params) {
              return `${params.data.name}: ${params.data.value}次提及`;
            }
          },
          series: [{
            type: 'wordCloud',
            shape: 'circle',
            sizeRange: [20, 80],
            rotationRange: [0, 0],
            gridSize: 8,
            drawOutOfBound: false,
            textStyle: {
              color: function () {
                return 'rgb(' + [
                  Math.round(Math.random() * 160 + 50),
                  Math.round(Math.random() * 160 + 50),
                  Math.round(Math.random() * 160 + 50)
                ].join(',') + ')';
              }
            },
            emphasis: {
              focus: 'self',
              textStyle: {
                shadowBlur: 10,
                shadowColor: '#333'
              }
            },
            data: wordcloudData
          }]
        };

        chart.setOption(option);
      } catch (error) {
        console.error('初始化词云图失败:', error);
      }
    };

    // 初始化玫瑰图（极坐标堆叠柱状图）
    const initRoseChart = () => {
      try {
        const dom = document.getElementById('rose-chart');
        if (!dom || roseChartData.value.length === 0) return;

        const existingChart = echarts.getInstanceByDom(dom);
        if (existingChart) {
          existingChart.dispose();
        }

        const chart = echarts.init(dom);
        chartInstances.value.push(chart);

        // 只取前30家商户
        const displayData = roseChartData.value.slice(0, 30);
        const merchants = displayData.map(d => formatMerchantName(d.merchant || d.merchantName));
        const pos = displayData.map(d => d.positive || d.positiveCount || 0);
        const neg = displayData.map(d => d.negative || d.negativeCount || 0);
        const neu = displayData.map(d => d.neutral || d.neutralCount || 0);

        const option = {
          title: {
            text: `商户情感玫瑰图（Top ${displayData.length}）`,
            left: 'center',
            textStyle: {
              fontSize: 16,
              fontWeight: 'bold'
            }
          },
          tooltip: {
            trigger: 'item',
            formatter: (p) => {
              const idx = p.dataIndex;
              const total = pos[idx] + neg[idx] + neu[idx];
              return `${merchants[idx]}<br/>
                      积极 ${pos[idx]} (${(pos[idx]/total*100).toFixed(1)}%)<br/>
                      中性 ${neu[idx]} (${(neu[idx]/total*100).toFixed(1)}%)<br/>
                      消极 ${neg[idx]} (${(neg[idx]/total*100).toFixed(1)}%)`;
            }
          },
          angleAxis: {
            type: 'category',
            data: merchants,
            startAngle: 90,
            axisLabel: {
              interval: 1,
              fontSize: 10,
              rotate: 45
            }
          },
          radiusAxis: {
            min: 0
          },
          polar: {},
          series: [
            {
              name: '积极',
              type: 'bar',
              coordinateSystem: 'polar',
              stack: '情感',
              color: '#92d5a9',
              data: pos
            },
            {
              name: '中性',
              type: 'bar',
              coordinateSystem: 'polar',
              stack: '情感',
              color: '#ffad33',
              data: neu
            },
            {
              name: '消极',
              type: 'bar',
              coordinateSystem: 'polar',
              stack: '情感',
              color: '#e18fad',
              data: neg
            }
          ],
          legend: {
            orient: 'horizontal',
            bottom: 10,
            data: ['积极', '中性', '消极']
          }
        };

        chart.setOption(option);
      } catch (error) {
        console.error('初始化玫瑰图失败:', error);
      }
    }

    // 查看全部玫瑰图
    const showAllRoses = () => {
      ElMessage.info(`功能开发中，将显示全部${roseChartData.value.length}家商户的玫瑰图`);
    }

    // 组件挂载
    onMounted(async () => {
      console.log('组件挂载，开始获取数据');

      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          const user = JSON.parse(userStr);
          merchantName.value = user.merchantName || user.username || '当前商户';
          console.log('当前用户:', user);
        } catch (e) {
          console.error('解析用户信息错误:', e);
        }
      }

      lastUpdateTime.value = new Date().toLocaleString();
      await fetchCurrentMerchantData();
      await fetchCollectiveData();

      // 添加延迟确保DOM渲染完成
      setTimeout(() => {
        chartInstances.value.forEach(chart => {
          try {
            chart.resize();
          } catch (e) {
            console.error('图表resize失败:', e);
          }
        });
      }, 1000);

      // 窗口resize监听
      window.addEventListener('resize', () => {
        chartInstances.value.forEach(chart => {
          try {
            chart.resize();
          } catch (e) {
            console.error('图表resize失败:', e);
          }
        });
      });
    });

    // 组件卸载时的清理
    onUnmounted(() => {
      chartInstances.value.forEach(chart => {
        try {
          chart.dispose();
        } catch (e) {
          console.error('图表销毁失败:', e);
        }
      });
      chartInstances.value = [];

      window.removeEventListener('resize', () => {});
    });

    return {
      loading,
      merchantName,
      InfoFilled,
      lastUpdateTime,
      radarData,
      bubbleData,
      sentimentData,
      hasRadarData,
      roseChartData,
      popularFoods,
      refreshData,
      logout,
      showAllRoses
    }
  }
}
</script>

<style scoped>
.merchant-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.dashboard-header {
  background: linear-gradient(135deg, #92d5a9 0%, #e18fad 100%);
  color: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.merchant-info h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: bold;
}

.merchant-info p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.analysis-row {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}

.personal-analysis {
  grid-template-columns: 1fr 1fr;
}

.chart-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: none;
  background: white;
}

.chart-card ::v-deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-bottom: 1px solid #dee2e6;
  padding: 15px 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #2c3e50;
}

.full-width {
  grid-column: 1 / -1;
}

.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #6c757d;
  font-size: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin: 10px;
}

/* 雷达图和词云图卡片特定样式 */
.radar-card, .wordcloud-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.radar-card:hover, .wordcloud-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .personal-analysis {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: center;
  }

  .merchant-info h1 {
    font-size: 20px;
  }
}

/* 确保图表容器可见 */
#radar-chart, #wordcloud-chart, #bubble-chart, #stacked-bar-chart, #rose-chart {
  background: white;
  border-radius: 8px;
}
</style>