<template>
  <div class="decision-assistant">
    <!-- 顶部导航栏 -->
    <div class="customer-header">
      <div class="header-left">
        <h1 class="logo">大数据处理一体化平台-寻味助手</h1>
      </div>
      <div class="header-center">
        <span class="customer-name">{{ customerName }}</span>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="32" :src="userAvatar" />
            <span class="user-name">{{ userName }}</span>
            <i class="el-icon-caret-bottom"></i>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="refresh">刷新数据</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <el-tabs v-model="activeTab" type="border-card" >
      <!-- 智能筛选标签页 -->
      <el-tab-pane label="智能筛选" name="search">
        <div class="search-panel">
          <el-form :model="searchForm" label-width="120px">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="商户名称">
                  <el-select v-model="searchForm.merchantName" placeholder="选择商户" filterable clearable>
                    <el-option
                        v-for="name in merchantNames"
                        :key="name"
                        :label="name"
                        :value="name"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最低评分">
                  <el-slider
                      v-model="searchForm.minRating"
                      :min="0"
                      :max="5"
                      :step="0.1"
                      show-input
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最高价格">
                  <el-input-number
                      v-model="searchForm.maxPrice"
                      :min="0"
                      :max="60000"
                      :step="20"
                      controls-position="right"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="最低好评率">
                  <el-slider
                      v-model="searchForm.minPositiveRate"
                      :min="0"
                      :max="100"
                      :step="1"
                      show-input
                      :format-tooltip="value => `${value}%`"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item>
                  <el-button type="primary" @click="handleSearch">查询</el-button>
                  <el-button @click="resetSearch">重置</el-button>
                  <el-button
                      type="success"
                      :disabled="selectedMerchants.length < 2 || selectedMerchants.length > 3"
                      @click="goToComparison"
                  >
                    对比选中的商户 ({{ selectedMerchants.length }})
                  </el-button>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <!-- 搜索结果表格 -->
        <el-table
            :data="searchResults"
            style="width: 100%"
            @selection-change="handleSelectionChange"
            v-loading="loading"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="merchantName" label="商户名称" width="220" />
          <el-table-column label="评分" width="180">
            <template #default="{ row }">
              <el-rate :model-value="row.avgRating || 0" disabled show-score text-color="#ff9900" />
            </template>
          </el-table-column>
          <el-table-column label="人均价格" width="120">
            <template #default="{ row }">¥{{ (row.avgPrice || 0)?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="好评率" width="150">
            <template #default="{ row }">
              <el-progress
                  :percentage="(row.positiveRate || 0)"
                  :format="() => `${(row.positiveRate || 0).toFixed(1)}%`"
              />
            </template>
          </el-table-column>
          <el-table-column label="评价数量" width="130">
            <template #default="{ row }">
              {{ row.totalReviews || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="viewDetails(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页控件 -->
        <el-pagination
            v-if="searchResults.length > 0"
            style="margin-top: 20px; justify-content: center;"
            :current-page="searchPagination.currentPage"
            :page-size="searchPagination.pageSize"
            :total="searchPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSearchSizeChange"
            @current-change="handleSearchCurrentChange"
        />
      </el-tab-pane>

      <!-- 排行榜+推荐标签页 -->
      <el-tab-pane label="智能推荐" name="rankings">
        <div class="ranking-layout">
          <!-- 左侧排行榜 -->
          <div class="ranking-left"style="width: 70px; margin-right: 35px">
            <div class="ranking-controls">
              <span style="margin-right: 20px">综合排名</span>
              <el-select v-model="rankingSortField" @change="loadRankings" style="width: 120px; margin-right: 10px">
                <el-option label="默认排序" value="default" />
                <el-option label="人均价格" value="avgPrice" />
                <el-option label="好评率" value="positiveRate" />
              </el-select>
              <el-select v-model="rankingSortOrder" @change="loadRankings" style="width: 100px; margin-right: 20px">
                <el-option label="升序" value="asc" />
                <el-option label="降序" value="desc" />
              </el-select>
              <el-select v-model="rankingLimit" @change="loadRankings" style="width: 120px">
                <el-option label="TOP 10" value="10" />
                <el-option label="TOP 20" value="20" />
                <el-option label="TOP 50" value="50" />
              </el-select>
            </div>

            <el-table :data="rankings" style="width: 100%" v-loading="loadingRankings" height="500">
              <el-table-column prop="rankPosition" label="排名" width="70" />
              <el-table-column prop="merchantName" label="商户名称" width="190" />
              <el-table-column label="综合评分" width="152">
                <template #default="{ row }">
                  <el-rate :model-value="(row.compositeScore || 0) / 10" disabled show-score text-color="#ff9900" />
<!--                  <div style="font-size: 12px; color: #666;">
                    {{ (row.compositeScore || 0).toFixed(1) }}
                  </div>-->
                </template>
              </el-table-column>
              <el-table-column label="人均价格" width="100">
                <template #default="{ row }">¥{{ (row.avgPrice || 0)?.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="好评率" width="130">
                <template #default="{ row }">
                  <el-progress
                      :percentage="(row.positiveRate || 0)"
                      :format="() => `${(row.positiveRate || 0).toFixed(1)}%`"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button size="small" @click="selectForCompare(row)">对比</el-button>
                  <el-button size="small" type="primary" @click="viewFoodKeywords(row)">菜品</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 右侧词云图 -->
          <div class="ranking-right"style="width: 180px; margin-right: 35px">
            <div v-if="showWordCloud" class="word-cloud-card">
              <el-card>
                <template #header>
                  <h3>{{ selectedMerchantForKeywords }} - 热门菜品推荐</h3>
                </template>
                <div ref="wordCloudChart" style="height: 450px; width: 110%;"></div>
              </el-card>
            </div>
            <div v-else class="word-cloud-placeholder">
              <el-empty description="点击'菜品'查看热门推荐" />
            </div>
          </div>
        </div>
      </el-tab-pane>
      <!-- 商户对比标签页 -->
      <el-tab-pane label="商户对比" name="comparison">
        <div v-if="selectedMerchants.length >= 2">
          <MerchantComparison
              :comparisonData="comparisonData"
              :merchants="selectedMerchants"
          />
        </div>
        <div v-else class="no-comparison">
          <el-empty description="请选择2-3个商户进行对比" />
          <el-button type="primary" @click="activeTab = 'search'">返回筛选</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 商户详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        :title="`${selectedMerchant?.merchantName} - 详细信息`"
        width="80%"
    >
      <MerchantDetail :merchant="selectedMerchant" v-if="detailDialogVisible && selectedMerchant" />
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { decisionApi } from '../api/index.js'
import MerchantComparison from '../components/layout/MerchantComparison.vue'
import MerchantDetail from '../components/data/MerchantDetail.vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'   // 2.x 官方入口
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'DecisionAssistantView',
  components: {
    MerchantComparison,
    MerchantDetail
  },
  setup() {
    const router = useRouter()

    // 状态变量
    const activeTab = ref('search')
    const detailDialogVisible = ref(false)
    const showWordCloud = ref(false)
    const loading = ref(false)
    const loadingRankings = ref(false)

    // 用户信息
    const customerName = ref('')
    const userName = ref('')
    const userAvatar = ref('')

    // 分页信息
    const searchPagination = ref({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    const rankingPagination = ref({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    // 排序控制
    const rankingSortField = ref('default')
    const rankingSortOrder = ref('desc')

    // 表单数据
    const searchForm = ref({
      merchantName: '',
      minRating: 0,
      maxPrice: null,
      minPositiveRate: 0
    })

    // 数据列表
    const merchantNames = ref([])
    const searchResults = ref([])
    const selectedMerchants = ref([])
    const rankings = ref([])
    const selectedMerchant = ref(null)
    const selectedMerchantForKeywords = ref('')
    const foodKeywords = ref([])

    // API数据
    const comparisonData = ref({})
    const rankingLimit = ref('10')

    // 图表引用
    const wordCloudChart = ref(null)
    let wordCloudInstance = null

    // 加载用户信息
    const loadUserInfo = () => {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      userName.value = user.username || '顾客用户'
      customerName.value = user.username || '顾客用户'
      userAvatar.value = user.avatar || ''
    }

    // 退出登录处理
    const handleCommand = (command) => {
      switch (command) {
        case 'refresh':
          window.location.reload()
          break
        case 'logout':
          handleLogout()
          break
      }
    }

    const handleLogout = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.success('退出成功')
      router.push('/login')
    }

    // 加载商户名称
    const loadMerchantNames = async () => {
      try {
        const response = await decisionApi.getMerchantNames()
        if (response && Array.isArray(response)) {
          merchantNames.value = response
        }
      } catch (error) {
        console.error('加载商户名称失败:', error)
        ElMessage.error('加载商户列表失败，请检查网络连接')
      }
    }

    // 搜索处理
    const handleSearch = async () => {
      try {
        loading.value = true
        const params = {
          merchantName: searchForm.value.merchantName || undefined,
          minRating: searchForm.value.minRating > 0 ? searchForm.value.minRating : undefined,
          maxPrice: searchForm.value.maxPrice,
          minPositiveRate: searchForm.value.minPositiveRate > 0 ? searchForm.value.minPositiveRate : undefined,
          page: searchPagination.value.currentPage - 1,
          size: searchPagination.value.pageSize
        }

        // 移除undefined参数
        Object.keys(params).forEach(key => {
          if (params[key] === undefined) {
            delete params[key]
          }
        })

        const response = await decisionApi.searchRestaurants(params)

        if (response && Array.isArray(response.content)) {
          searchResults.value = response.content
          searchPagination.value.total = response.totalElements || response.content.length
        } else if (Array.isArray(response)) {
          searchResults.value = response
          searchPagination.value.total = response.length
        } else {
          searchResults.value = []
          searchPagination.value.total = 0
        }

        ElMessage.success(`找到 ${searchResults.value.length} 个结果`)
      } catch (error) {
        console.error('搜索失败:', error)
        ElMessage.error('搜索失败，请稍后重试')
        searchResults.value = []
        searchPagination.value.total = 0
      } finally {
        loading.value = false
      }
    }

    // 分页处理
    const handleSearchSizeChange = (size) => {
      searchPagination.value.pageSize = size
      searchPagination.value.currentPage = 1
      handleSearch()
    }

    const handleSearchCurrentChange = (page) => {
      searchPagination.value.currentPage = page
      handleSearch()
    }

    const handleRankingSizeChange = (size) => {
      rankingPagination.value.pageSize = size
      rankingPagination.value.currentPage = 1
      loadRankings()
    }

    const handleRankingCurrentChange = (page) => {
      rankingPagination.value.currentPage = page
      loadRankings()
    }

    const resetSearch = () => {
      searchForm.value = {
        merchantName: '',
        minRating: 0,
        maxPrice: null,
        minPositiveRate: 0
      }
      searchPagination.value.currentPage = 1
      searchResults.value = []
      selectedMerchants.value = []
    }

    const handleSelectionChange = (selection) => {
      selectedMerchants.value = selection
    }

    // 加载排行榜
    const loadRankings = async () => {
      try {
        loadingRankings.value = true
        const params = {
          rankType: '综合排名',
          limit: parseInt(rankingLimit.value),
          page: rankingPagination.value.currentPage - 1,
          size: rankingPagination.value.pageSize,
          sortField: rankingSortField.value !== 'default' ? rankingSortField.value : undefined,
          sortOrder: rankingSortField.value !== 'default' ? rankingSortOrder.value : undefined
        }

        // 移除undefined参数
        Object.keys(params).forEach(key => {
          if (params[key] === undefined) {
            delete params[key]
          }
        })

        const response = await decisionApi.getRankings(params)

        if (response && Array.isArray(response.content)) {
          rankings.value = response.content
          rankingPagination.value.total = response.totalElements || response.content.length
        } else if (Array.isArray(response)) {
          rankings.value = response
          rankingPagination.value.total = response.length
        } else {
          rankings.value = []
          rankingPagination.value.total = 0
        }
      } catch (error) {
        console.error('加载排行榜失败:', error)
        ElMessage.error('加载排行榜失败，请检查网络连接')
        rankings.value = []
        rankingPagination.value.total = 0
      } finally {
        loadingRankings.value = false
      }
    }

    // 查看详情
    const viewDetails = (merchant) => {
      selectedMerchant.value = merchant
      detailDialogVisible.value = true
    }

    // 选择对比
    const selectForCompare = (merchant) => {
      if (selectedMerchants.value.length >= 3) {
        ElMessage.warning('最多只能选择3个商户进行对比')
        return
      }

      if (!selectedMerchants.value.some(m => m.merchantName === merchant.merchantName)) {
        selectedMerchants.value.push(merchant)
        ElMessage.success(`已添加 ${merchant.merchantName} 到对比列表`)
      }
    }

    // 跳转到对比页面
    const goToComparison = async () => {
      if (selectedMerchants.value.length < 2) {
        ElMessage.warning('请至少选择2个商户进行对比')
        return
      }

      try {
        // 获取商户名称列表
        const merchantNames = selectedMerchants.value.map(m => m.merchantName)

        // 使用decisionApi获取对比数据
        const response = await decisionApi.compareMerchants(merchantNames)

        if (response) {
          comparisonData.value = response
          activeTab.value = 'comparison'
          ElMessage.success('对比数据加载成功')
        } else {
          ElMessage.warning('获取对比数据失败')
        }
      } catch (error) {
        console.error('获取对比数据失败:', error)
        ElMessage.error('获取对比数据失败')
      }
    }

    // 查看菜品关键词
    const viewFoodKeywords = async (merchant) => {
      try {
        selectedMerchantForKeywords.value = merchant.merchantName
        const response = await decisionApi.getKeywords(merchant.merchantName, 20)

        if (response && Array.isArray(response)) {
          foodKeywords.value = response
          showWordCloud.value = true
          nextTick(() => {
            initWordCloud()
          })
        } else {
          ElMessage.warning('暂无菜品数据')
        }
      } catch (error) {
        console.error('获取菜品关键词失败:', error)
        ElMessage.error('获取菜品数据失败')
      }
    }

    // 初始化词云图
    const initWordCloud = () => {
      if (!wordCloudChart.value || foodKeywords.value.length === 0) return

      if (wordCloudInstance) {
        wordCloudInstance.dispose()
      }

      wordCloudInstance = echarts.init(wordCloudChart.value)

      const data = foodKeywords.value.map(item => ({
        name: item.keyword,
        value: item.mentionCount
      }))

      const option = {
        tooltip: {
          show: true,
          formatter: function (params) {
            return `${params.name}: ${params.value}次提及`
          }
        },
        series: [{
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '80%',
          height: '80%',
          sizeRange: [12, 60],
          rotationRange: [-90, 90],
          rotationStep: 45,
          gridSize: 8,
          drawOutOfBound: false,
          textStyle: {
            color: function () {
              return 'rgb(' + [
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160)
              ].join(',') + ')'
            }
          },
          emphasis: {
            textStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(2,179,255,0.68)'
            }
          },
          data: data
        }]
      }

      wordCloudInstance.setOption(option)

      // 响应窗口大小变化
      window.addEventListener('resize', () => {
        if (wordCloudInstance) {
          wordCloudInstance.resize()
        }
      })
    }

    // 初始化
    onMounted(() => {
      loadUserInfo()
      loadMerchantNames()
      loadRankings()
      // 初始加载一次数据
      handleSearch()
    })

    return {
      activeTab,
      detailDialogVisible,
      showWordCloud,
      loading,
      loadingRankings,
      customerName,
      userName,
      userAvatar,
      searchPagination,
      rankingPagination,
      rankingSortField,
      rankingSortOrder,
      searchForm,
      merchantNames,
      searchResults,
      selectedMerchants,
      rankings,
      comparisonData,
      rankingLimit,
      selectedMerchant,
      selectedMerchantForKeywords,
      foodKeywords,
      wordCloudChart,
      handleCommand,
      handleSearch,
      handleSearchSizeChange,
      handleSearchCurrentChange,
      handleRankingSizeChange,
      handleRankingCurrentChange,
      resetSearch,
      handleSelectionChange,
      loadRankings,
      viewDetails,
      selectForCompare,
      goToComparison,
      viewFoodKeywords
    }
  }
}
</script>

<style scoped>
.decision-assistant {
  padding: 20px;
  background: linear-gradient(135deg, #cbddd6 0%, rgba(173, 232, 185, 0.65) 100%);
  min-height: 100vh;
}

.customer-header {
  height: 80px;
  background: linear-gradient(135deg, #93d7aa 0%, #e18fad 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.header-left .logo {
  margin: 0;
  color: white;
  font-size: 20px;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.header-center .customer-name {
  font-size: 18px;
  font-weight: bold;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.header-right .el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
}

.header-right .user-name {
  margin: 0 12px;
  color: #cbddd6;
  font-weight: 500;
}

.search-panel {
  margin-bottom: 24px;
  padding: 24px;
  background: rgba(203, 221, 214, 0.6);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.ranking-layout {
  display: flex;
  gap: 24px;
  min-height: 600px;
}

.ranking-left {
  flex: 1;
  min-width: 0;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.ranking-right {
  width: 450px;
  min-width: 450px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.ranking-controls {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.word-cloud-card {
  height: 100%;
}

.word-cloud-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
}

.no-comparison {
  text-align: center;
  padding: 60px 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 表格样式优化 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #f8f9fa !important;
  font-weight: 600;
}

:deep(.el-table .el-button) {
  margin: 2px;
}

/* 卡片样式优化 */
:deep(.el-card) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-bottom: 1px solid #e9ecef;
  font-weight: 600;
}

/* 标签页样式优化 */
:deep(.el-tabs__content) {
  padding: 0;
}

:deep(.el-tabs--border-card) {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-bottom: 1px solid #e9ecef;
}

/* 进度条样式优化 */
:deep(.el-progress-bar) {
  padding-right: 0;
}

:deep(.el-progress__text) {
  font-size: 12px;
  min-width: 40px;
}

/* 分页样式优化 */
:deep(.el-pagination) {
  padding: 20px 0;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .ranking-layout {
    flex-direction: column;
  }

  .ranking-right {
    width: 100%;
    min-width: auto;
  }
}

@media (max-width: 768px) {
  .customer-header {
    flex-direction: column;
    height: auto;
    padding: 15px;
    gap: 10px;
  }

  .search-panel {
    padding: 16px;
  }

  .ranking-controls {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>